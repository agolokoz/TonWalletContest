package org.ton.wallet.domain

import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import org.ton.crypto.hex
import org.ton.lib.tonapi.TonAccount
import org.ton.wallet.AppComponentsProvider
import org.ton.wallet.BuildConfig
import org.ton.wallet.data.db.transaction.TransactionDto
import org.ton.wallet.data.model.*
import org.ton.wallet.data.repo.AccountsRepository
import org.ton.wallet.data.repo.SettingsRepository
import org.ton.wallet.data.repo.TonConnectRepository
import org.ton.wallet.data.repo.TransactionsRepository
import org.ton.wallet.data.ton.TonApiException
import org.ton.wallet.lib.core.ThreadUtils
import org.ton.wallet.lib.core.ext.clear
import org.ton.wallet.util.LinkAction
import kotlin.math.max

object UseCases {

    private val accounts: AccountsRepository get() = AppComponentsProvider.accountsRepository
    private val settings: SettingsRepository get() = AppComponentsProvider.settingsRepository
    private val transactions: TransactionsRepository get() = AppComponentsProvider.transactionsRepository
    private val tonConnect: TonConnectRepository get() = AppComponentsProvider.tonConnectRepository

    private val tonConnectListenerScope = CoroutineScope(Dispatchers.Default)

    private val currentAccountIdStateFlow: StateFlow<Int> by lazy {
        settings.accountTypeFlow.map(accounts::getAccountId)
            .filterNotNull()
            .stateIn(ThreadUtils.appCoroutineScope, SharingStarted.Eagerly, -1)
    }

    val currentAccountAddressFlow: Flow<String> by lazy {
        settings.accountTypeFlow.map(accounts::getAccountAddress)
    }

    val currentAccountBalanceFlow: Flow<Long?> by lazy {
        currentAccountAddressFlow.flatMapLatest(accounts::getAccountBalanceFlow)
            .map { balance -> balance?.let { max(it, 0) } }
    }

    val currentAccountTonConnectEventsFlow: Flow<TonConnectEvent> by lazy {
        currentAccountIdStateFlow.flatMapLatest(tonConnect::getEventsFlow)
    }

    init {
        currentAccountIdStateFlow.onEach { accountId ->
            tonConnect.checkExistingConnections(accountId)
        }.launchIn(tonConnectListenerScope)
    }

    suspend fun getUfAddress(rawAddress: String): String? {
        return accounts.getUfAddress(rawAddress)
    }

    suspend fun getTransactions(reload: Boolean): List<TransactionDto>? {
        val accountType = settings.accountTypeFlow.value
        val accountAddress = accounts.getAccountAddress(settings.accountTypeFlow.value)
        val loadType = if (reload) LoadType.OnlyApi else LoadType.CacheOrApi
        val account = accounts.getAccountState(accountAddress, accountType, loadType)
        return if (account == null) {
            null
        } else {
            transactions.getTransactions(account.id, loadType)
        }
    }

    @Throws(Exception::class)
    suspend fun getSendFee(toAddress: String, amount: Long, message: String): Long {
        val fromAddress = currentAccountAddressFlow.first()
        val sendParams = SendParams(fromAddress, toAddress, amount, message)
        return try {
            transactions.getSendFee(settings.publicKey, sendParams)
        } catch (e: TonApiException) {
            if (e.error.message == "NOT_ENOUGH_FUNDS") {
                0
            } else {
                throw e
            }
        }
    }

    @Throws(Exception::class)
    suspend fun performSend(toAddress: String, amount: Long, message: String): Long {
        val fromAddress = currentAccountAddressFlow.first()
        val secret = settings.secret
        val password = settings.password
        val seed = settings.seed
        val sendParams = SendParams(fromAddress, toAddress, amount, message)
        val result = transactions.performSend(settings.publicKey, secret, password, seed, sendParams)
        secret.clear()
        password.clear()
        seed.clear()
        return result
    }

    suspend fun tonConnectOpenConnection(connectAction: LinkAction.TonConnectAction) {
        val accountType = settings.accountTypeFlow.value
        val accountId = accounts.getAccountId(accountType) ?: return
        tonConnect.connect(accountId, connectAction.clientId)

        val accountUfAddress = accounts.getAccountAddress(settings.accountTypeFlow.value)
        val rawAddress = accounts.getRawAddress(accountUfAddress) ?: ""

        val account = TonAccount(settings.publicKey, accountType.version, accountType.revision)
        val walletStateInit = Base64.encodeToString(account.getStateInitBytes(), Base64.NO_WRAP)
        val payload = TonConnect.ConnectEvent.Success.Payload(
            items = listOf(
                TonConnect.TonAddress(
                    address = rawAddress,
                    publicKey = hex(account.publicKey),
                    network = TonConnect.NetworkMainNet,
                    walletStateInit = walletStateInit
                )
            ),
            device = TonConnect.DeviceInfo(
                platform = TonConnect.PlatformAndroid,
                appVersion = BuildConfig.VERSION_NAME,
            )
        )
        val event = TonConnect.ConnectEvent.Success(0, payload)
        val connectEventJson = AppComponentsProvider.json.encodeToString(event)
        tonConnect.sendMessage(accountId, connectAction.clientId, connectEventJson.toByteArray())
    }

    suspend fun tonConnectSendTransactionResult(clientId: String, success: TonConnect.SendTransactionResponse.Success) {
        val responseJson = AppComponentsProvider.json.encodeToString(success)
        tonConnect.sendMessage(currentAccountIdStateFlow.value, clientId, responseJson.toByteArray())
    }

    suspend fun tonConnectSendTransactionError(clientId: String, error: TonConnect.SendTransactionResponse.Error) {
        val responseJson = AppComponentsProvider.json.encodeToString(error)
        tonConnect.sendMessage(currentAccountIdStateFlow.value, clientId, responseJson.toByteArray())
    }

    suspend fun getRecentSendTransactions(): List<RecentTransactionDto> {
        val accountId = accounts.getAccountId(settings.accountTypeFlow.value) ?: return emptyList()
        return transactions.getLocalRecentSendTransactions(accountId)
    }

    suspend fun deleteWallet() {
        AppComponentsProvider.baseRepositories.forEach { it.deleteWallet() }
        AppComponentsProvider.defaultPreferences.edit().clear().apply()
        AppComponentsProvider.securedPreferences.edit().clear().apply()
    }
}