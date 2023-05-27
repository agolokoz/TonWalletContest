package org.ton.wallet.data.repo

import android.util.Base64
import drinkless.org.ton.TonApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.ton.block.*
import org.ton.contract.wallet.WalletV4R2Contract
import org.ton.lib.tonapi.TonAccount
import org.ton.lite.api.liteserver.LiteServerAccountId
import org.ton.wallet.BuildConfig
import org.ton.wallet.data.db.account.AccountsDao
import org.ton.wallet.data.db.transaction.TransactionDto
import org.ton.wallet.data.db.transaction.TransactionStatus
import org.ton.wallet.data.db.transaction.TransactionsDao
import org.ton.wallet.data.model.LoadType
import org.ton.wallet.data.model.RecentTransactionDto
import org.ton.wallet.data.model.SendParams
import org.ton.wallet.data.ton.TonClient
import org.ton.wallet.lib.core.L
import java.nio.charset.Charset

interface TransactionsRepository : BaseRepository {

    val transactionsAddedFlow: Flow<TransactionDto?>

    suspend fun getTransaction(internalId: Long): TransactionDto?

    suspend fun getTransactions(accountId: Int, loadType: LoadType): List<TransactionDto>?

    suspend fun getLocalRecentSendTransactions(accountId: Int): List<RecentTransactionDto>

    @Throws(Exception::class)
    suspend fun getSendFee(publicKey: String, sendParams: SendParams): Long

    @Throws(Exception::class)
    suspend fun performSend(publicKey: String, secret: ByteArray, password: ByteArray, seed: ByteArray, sendParams: SendParams): Long
}

class TransactionsRepositoryImpl(
    private val tonClient: TonClient,
    private val accountsDao: AccountsDao,
    private val transactionsDao: TransactionsDao
) : TransactionsRepository {

    override val transactionsAddedFlow: Flow<TransactionDto?> = MutableSharedFlow()

    override suspend fun getTransaction(internalId: Long): TransactionDto? {
        return transactionsDao.get(internalId)
    }

    override suspend fun getTransactions(
        accountId: Int,
        loadType: LoadType
    ): List<TransactionDto>? {
        // load transactions from db
        var transactionsDtoList: List<TransactionDto>? = null
        if (loadType.useCache) {
            transactionsDtoList = transactionsDao.getAll(accountId)
        }
        if (loadType == LoadType.OnlyCache || loadType == LoadType.CacheOrApi && !transactionsDtoList.isNullOrEmpty()) {
            return transactionsDtoList ?: emptyList()
        }

        // check account
        val account = accountsDao.get(accountId)
        val lastTransactionId = account?.lastTransactionId
        val lastTransactionHash = account?.lastTransactionHash
        if (lastTransactionId == null || lastTransactionHash == null) {
            return null
        }

        val lastHashes = transactionsDao.getLastExecutedTransactionHashes(accountId)
        val nonExecutedTransactions = transactionsDao.getAllNonExecuted(accountId)
        val pendingTransactionsHashes = hashSetOf<String>()
        nonExecutedTransactions.forEach { dto ->
            if (dto.status == TransactionStatus.Pending) {
                pendingTransactionsHashes.add(dto.hash)
            }
        }

        // load transactions from api
        var fromId: TonApi.InternalTransactionId? =
            TonApi.InternalTransactionId(lastTransactionId, lastTransactionHash)
        val apiTransactions = mutableListOf<TonApi.RawTransaction>()
        while (fromId != null) {
            val transactionsRequest =
                TonApi.RawGetTransactions(null, TonApi.AccountAddress(account.address), fromId)
            val transactionsResponse = try {
                tonClient.sendRequestTyped<TonApi.RawTransactions>(transactionsRequest)
            } catch (e: Exception) {
                null
            } ?: break

            // check if we reached existing hash
            var stopPaging = false
            if (lastHashes.isNotEmpty()) {
                for (rawTransaction in transactionsResponse.transactions) {
                    val rawTransactionHash =
                        Base64.encodeToString(rawTransaction.transactionId.hash, Base64.NO_WRAP)
                    val rawTransactionInMsgHash = Base64.encodeToString(
                        rawTransaction.inMsg?.bodyHash ?: byteArrayOf(),
                        Base64.NO_WRAP
                    )
                    if (pendingTransactionsHashes.contains(rawTransactionHash) || pendingTransactionsHashes.contains(
                            rawTransactionInMsgHash
                        )
                    ) {
                        pendingTransactionsHashes.remove(rawTransactionHash)
                        pendingTransactionsHashes.remove(rawTransactionInMsgHash)
                    }

                    if (lastHashes.contains(rawTransactionHash)) {
                        // if we have pending transactions, we need to continue search for them
                        stopPaging = pendingTransactionsHashes.isEmpty()
                        break
                    }
                    apiTransactions.add(rawTransaction)
                }
            } else {
                apiTransactions.addAll(transactionsResponse.transactions)
            }

            fromId = if (stopPaging) {
                null
            } else {
                if (transactionsResponse.previousTransactionId.lt == 0L) null
                else transactionsResponse.previousTransactionId
            }
        }

        // prepare new dto list
        val newDtoList = mutableListOf<TransactionDto>()
        apiTransactions.forEach { rawTransaction ->
            val dto = mapRawTransactionToDto(rawTransaction, accountId)

            // check if this transaction was non-executed locally
            var foundNonExecuted = false
            for (nonExecutedTransaction in nonExecutedTransactions) {
                if (nonExecutedTransaction.hash == dto.hash || nonExecutedTransaction.hash == dto.inMsgBodyHash) {
                    transactionsDao.update(nonExecutedTransaction.internalId, dto)
                    foundNonExecuted = true
                }
            }

            // if this transaction is new then add to new dto list
            if (!foundNonExecuted) {
                newDtoList.add(dto)
            }
        }

        // put new transactions into db
        if (BuildConfig.DEBUG) {
            transactionsDao.add(accountId, newDtoList)
        } else {
            try {
                transactionsDao.add(accountId, newDtoList)
            } catch (e: Exception) {
                L.e(e)
            }
        }

        return transactionsDao.getAll(accountId)
    }

    override suspend fun getLocalRecentSendTransactions(accountId: Int): List<RecentTransactionDto> {
        return transactionsDao.getAllSendUnique(accountId)
    }

    override suspend fun getSendFee(publicKey: String, sendParams: SendParams): Long {
        val inputKey = TonApi.InputKeyFake()
        val account = accountsDao.get(sendParams.fromAddress)
            ?: throw IllegalArgumentException("Could not find account with address ${sendParams.fromAddress} in database")
        return try {
            val queryInfo = getQueryInfo(inputKey, null, publicKey, sendParams)
            val feeRequest = TonApi.QueryEstimateFees(queryInfo.id, true)
            val feeResponse = tonClient.sendRequestTyped<TonApi.QueryFees>(feeRequest)
            feeResponse.sourceFees.gasFee + feeResponse.sourceFees.storageFee + feeResponse.sourceFees.fwdFee + feeResponse.sourceFees.inFwdFee
        } catch (e: Exception) {
            if (account.version == 4) {
                0L
            } else {
                throw e
            }
        }
    }

    override suspend fun performSend(publicKey: String, secret: ByteArray, password: ByteArray, seed: ByteArray, sendParams: SendParams): Long {
        val inputKey = TonApi.InputKeyRegular(TonApi.Key(publicKey, secret), password)
        val account = accountsDao.get(sendParams.fromAddress)
            ?: throw IllegalArgumentException("Could not find account with address ${sendParams.fromAddress} in database")

        val queryInfo = getQueryInfo(inputKey, seed, publicKey, sendParams)
        val sendQuery = TonApi.QuerySend(queryInfo.id)
        tonClient.sendRequestTyped<TonApi.Ok>(sendQuery)
        val bodyHash = queryInfo.bodyHash
        val validUntil = queryInfo.validUntil

        val transaction = TransactionDto(
            hash = Base64.encodeToString(bodyHash, Base64.NO_WRAP),
            accountId = account.id,
            status = TransactionStatus.Pending,
            timestampSec = System.currentTimeMillis() / 1000,
            amount = -sendParams.amount,
            peerAddress = sendParams.toAddress,
            message = sendParams.message,
            validUntilSec = validUntil,
        )
        val internalId = transactionsDao.add(account.id, transaction)
        if (internalId != null) {
            transaction.internalId = internalId
            (transactionsAddedFlow as MutableSharedFlow).emit(transaction)
        }

        return sendParams.amount
    }

    override suspend fun deleteWallet() = Unit

    private suspend fun getQueryInfo(inputKey: TonApi.InputKey, seed: ByteArray?, publicKey: String, sendParams: SendParams): TonApi.QueryInfo {
        val accountDto = accountsDao.get(sendParams.fromAddress)
            ?: throw IllegalArgumentException("Could not find account with address ${sendParams.fromAddress} in database")

        val request: TonApi.Function
        if (accountDto.version == 4) {
            val unpackedFromAddress = tonClient.sendRequestTyped<TonApi.UnpackedAccountAddress>(TonApi.UnpackAccountAddress(sendParams.fromAddress))
            val fromAddrStd = AddrStd(unpackedFromAddress.workchainId, unpackedFromAddress.addr)
            val blockId = tonClient.liteClient.getLastBlockId()
            val accountInfo = tonClient.liteClient.getAccount(LiteServerAccountId(fromAddrStd.workchainId, fromAddrStd.address), blockId)!!
            val walletContract = WalletV4R2Contract(accountInfo)
            val account = TonAccount(publicKey, accountDto.version, accountDto.revision, seqNo = walletContract.getSeqno())
            val unpackedToAddress = tonClient.sendRequestTyped<TonApi.UnpackedAccountAddress>(TonApi.UnpackAccountAddress(sendParams.toAddress))
            val requestBody = account.getTransferMessageBody(unpackedToAddress.workchainId, unpackedToAddress.addr, sendParams.amount, sendParams.message, seed)
            request = TonApi.RawCreateQuery(TonApi.AccountAddress(sendParams.toAddress), account.getCode(), account.getData(), requestBody)
        } else {
            val account = TonAccount(publicKey, accountDto.version, accountDto.revision)
            val initialAccountState = TonApi.RawInitialAccountState(account.getCode(), account.getData())
            val messageData =
                if (sendParams.message.isEmpty()) TonApi.MsgDataText(null)
                else TonApi.MsgDataText(sendParams.message.toByteArray(Charset.defaultCharset()))
            val actionMessage = TonApi.ActionMsg(arrayOf(TonApi.MsgMessage(TonApi.AccountAddress(sendParams.toAddress), publicKey, sendParams.amount, messageData, 3)), true)
            request = TonApi.CreateQuery(inputKey, TonApi.AccountAddress(sendParams.fromAddress), 0, actionMessage, initialAccountState)
        }
        return tonClient.sendRequestTyped(request)
    }

    private fun mapRawTransactionToDto(raw: TonApi.RawTransaction, accountId: Int): TransactionDto {
        var value = 0L
        var message: String? = null
        var inMsgBodyHash: String? = null
        raw.inMsg?.let { msg ->
            value += msg.value
            message = getMessage(msg.msgData)
            inMsgBodyHash = Base64.encodeToString(msg.bodyHash, Base64.NO_WRAP)
        }
        val outMessages = raw.outMsgs ?: emptyArray<TonApi.RawMessage>()
        outMessages.forEach { msg ->
            value -= msg.value
            if (message.isNullOrEmpty()) {
                message = getMessage(msg.msgData)
            }
        }

        val peerAddress: String? =
            if (value > 0) {
                raw.inMsg?.source?.accountAddress
            } else {
                if (raw.transactionId.lt == 0L) raw.inMsg?.destination?.accountAddress
                else raw.outMsgs?.firstOrNull()?.destination?.accountAddress
            }

        return TransactionDto(
            hash = Base64.encodeToString(raw.transactionId.hash, Base64.NO_WRAP),
            accountId = accountId,
            timestampSec = raw.utime,
            status = TransactionStatus.Executed,
            amount = value,
            fee = raw.fee,
            storageFee = raw.storageFee,
            peerAddress = peerAddress,
            message = message,
            inMsgBodyHash = inMsgBodyHash
        )
    }

    private fun getMessage(msgData: TonApi.MsgData): String? {
        return when (msgData) {
            is TonApi.MsgDataText -> String(msgData.text)
            is TonApi.MsgDataDecryptedText -> String(msgData.text)
            else -> null
        }
    }
}