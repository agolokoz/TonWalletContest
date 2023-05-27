package org.ton.wallet.data.repo

import android.content.SharedPreferences
import drinkless.org.ton.TonApi
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.ton.lib.tonapi.TonAccount
import org.ton.lib.tonapi.TonAccountType
import org.ton.wallet.data.DefaultPrefsKeys
import org.ton.wallet.data.db.account.AccountDto
import org.ton.wallet.data.db.account.AccountsDao
import org.ton.wallet.data.model.LoadType
import org.ton.wallet.data.ton.TonApiException
import org.ton.wallet.data.ton.TonClient
import org.ton.wallet.lib.core.CoroutinesUtils
import java.util.concurrent.ConcurrentHashMap

interface AccountsRepository : BaseRepository {

    // flows
    fun getAccountBalanceFlow(address: String): StateFlow<Long?>

    // methods
    suspend fun getAccountsCount(): Int

    suspend fun getAccountAddress(type: TonAccountType): String

    suspend fun getAccountId(type: TonAccountType): Int?

    suspend fun getAccountState(address: String, accountType: TonAccountType, loadType: LoadType): AccountDto?

    @Throws(Exception::class)
    suspend fun isValidUfAddress(address: String): Boolean

    @Throws(Exception::class)
    suspend fun getUfAddress(rawAddress: String): String?

    @Throws(Exception::class)
    suspend fun getRawAddress(ufAddress: String): String?

    @Throws(Exception::class)
    suspend fun resolveDnsName(dnsName: String): String?
}

internal class AccountsRepositoryImpl(
    private val client: TonClient,
    private val accountsDao: AccountsDao,
    private val securedPreferences: SharedPreferences
) : AccountsRepository {

    private val accountBalanceFlows = ConcurrentHashMap<String, MutableStateFlow<Long?>>()

    private val publicKey: String
        get() = securedPreferences.getString(DefaultPrefsKeys.PublicKey, "") ?: ""

    init {
        CoroutinesUtils.appCoroutinesScope.launch(Dispatchers.IO) {
            accountsDao.getAll().forEach { account ->
                setAccountBalance(account.address, account.balance)
            }
        }
    }

    override fun getAccountBalanceFlow(address: String): StateFlow<Long?> {
        return accountBalanceFlows.getOrPut(address) { MutableStateFlow(null) }
    }


    override suspend fun getAccountsCount(): Int {
        return accountsDao.getCount()
    }

    override suspend fun getAccountAddress(type: TonAccountType): String {
        var address: String? = accountsDao.getAddress(0, type)
        if (address == null) {
            val wallet = TonAccount(publicKey, type.version, type.revision)
            val initialState = TonApi.RawInitialAccountState(wallet.getCode(), wallet.getData())
            val addressRequest = TonApi.GetAccountAddress(initialState, wallet.revision, 0)
            val addressResponse = client.sendRequestTyped<TonApi.AccountAddress>(addressRequest)
            address = addressResponse.accountAddress!!
            accountsDao.add(0, address, type)
        }
        return address
    }

    override suspend fun getAccountId(type: TonAccountType): Int? {
        return accountsDao.getId(type)
    }

    override suspend fun getAccountState(address: String, accountType: TonAccountType, loadType: LoadType): AccountDto? {
        var account = accountsDao.get(address)
        if (loadType == LoadType.OnlyCache || loadType == LoadType.CacheOrApi && account != null) {
            return account
        }

        if (account == null) {
            val id = accountsDao.add(0, address, accountType)
            account = accountsDao.get(id)
        }
        val accountRequest = TonApi.GetAccountState(TonApi.AccountAddress(address))
        val accountResponse = client.sendRequestTyped<TonApi.FullAccountState>(accountRequest)

        setAccountBalance(address, accountResponse.balance)
        accountsDao.setLastTransaction(address, accountResponse.lastTransactionId.lt, accountResponse.lastTransactionId.hash)
        account!!.balance = accountResponse.balance
        account.lastTransactionId = accountResponse.lastTransactionId.lt
        account.lastTransactionHash = accountResponse.lastTransactionId.hash

        return account
    }

    override suspend fun isValidUfAddress(address: String): Boolean {
        return try {
            val response = client.sendRequest(TonApi.UnpackAccountAddress(address))
            response is TonApi.UnpackedAccountAddress
        } catch (e: TonApiException) {
            if (e.error.message.startsWith("INVALID_ACCOUNT_ADDRESS")) {
                false
            } else {
                throw e
            }
        }
    }

    override suspend fun getUfAddress(rawAddress: String): String? {
        return try {
            val splits = rawAddress.split(':')
            val workChainId = splits[0].toIntOrNull() ?: return null
            val addressBytes = hex(splits[1])
            val unpackedAddress = TonApi.UnpackedAccountAddress(workChainId, true, false, addressBytes)
            val response = client.sendRequestTyped<TonApi.AccountAddress>(TonApi.PackAccountAddress(unpackedAddress))
            return response.accountAddress
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getRawAddress(ufAddress: String): String? {
        return try {
            val unpackedAddress = client.sendRequestTyped<TonApi.UnpackedAccountAddress>(TonApi.UnpackAccountAddress(ufAddress))
            return "${unpackedAddress.workchainId}:${hex(unpackedAddress.addr)}"
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun resolveDnsName(dnsName: String): String? {
        val request = TonApi.DnsResolve(null, dnsName, CategoryEmpty, 5)
        val response = client.sendRequestTyped<TonApi.DnsResolved>(request)
        val entryData = response.entries.firstOrNull { it.category.contentEquals(CategoryWallet) }?.entry
        if (entryData is TonApi.DnsEntryDataSmcAddress) {
            return entryData.smcAddress.accountAddress
        }
        return null
    }

    override suspend fun deleteWallet() {
        accountsDao.remove(0)
    }

    private suspend fun setAccountBalance(address: String, balance: Long) {
        accountsDao.setBalance(address, balance)
        accountBalanceFlows.getOrPut(address) { MutableStateFlow(0) }.emit(balance)
    }

    private companion object {

        private val CategoryEmpty = ByteArray(32) { 0 }
        private val CategoryWallet = byteArrayOf(-24, -44, 64, 80, -121, 61, -70, -122, 90, -89, -63, 112, -85, 76, -50, 100, -39, 8, 57, -93, 77, -49, -42, -49, 113, -47, 78, 2, 5, 68, 59, 27)
    }
}