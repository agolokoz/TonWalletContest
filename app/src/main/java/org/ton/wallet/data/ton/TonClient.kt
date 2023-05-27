package org.ton.wallet.data.ton

import androidx.annotation.WorkerThread
import drinkless.org.ton.Client
import drinkless.org.ton.Client.ExceptionHandler
import drinkless.org.ton.Client.ResultHandler
import drinkless.org.ton.TonApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.ton.lite.api.LiteApiClient
import org.ton.lite.client.LiteClient
import org.ton.wallet.lib.core.FileUtils
import org.ton.wallet.lib.core.L
import org.ton.wallet.lib.core.ext.await
import java.io.File
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.roundToInt

class TonClient(
    private val okHttpClient: OkHttpClient,
    private val json: Json
) {

    private val ton = Client.create(null, null, null)

    lateinit var liteClient: LiteClient
        private set

    val liteApi: LiteApiClient
        get() = liteClient.liteApi

    private val initMutex = Mutex()
    @Volatile
    private var isClientInitialized = false
    private var defaultRwalletInitPublicKey: String? = null
    private var defaultWalletId = 0L

    val syncProgressFlow: StateFlow<Int> = MutableStateFlow(100)

    init {
        ton.setUpdatesHandler(::handleUpdate)
    }

    @Throws(Exception::class)
    suspend fun sendRequest(request: TonApi.Function): TonApi.Object {
        initClient()
        return sendRequestInternal(request)
    }

    @Throws(Exception::class)
    suspend inline fun <reified T : TonApi.Object> sendRequestTyped(request: TonApi.Function): T {
        val result = sendRequest(request)
        if (result is T) {
            return result
        } else {
            throw TypeCastException("$result could not be casted to ${T::class}")
        }
    }

    @Throws(Exception::class)
    suspend fun getDefaultWalletId(): Long {
        if (defaultWalletId == 0L) {
            initClient()
        }
        return defaultWalletId
    }

    @Throws(Exception::class)
    suspend fun getDefaultRwalletInitPublicKey(): String {
        if (defaultRwalletInitPublicKey == null) {
            initClient()
        }
        return defaultRwalletInitPublicKey!!
    }

    @Throws(Exception::class)
    private suspend fun sendRequestInternal(request: TonApi.Function): TonApi.Object {
        return suspendCoroutine { cont ->
            val exceptionHandler = ExceptionHandler(cont::resumeWithException)
            val resultHandler = object : ResultHandler {
                override fun onResult(result: TonApi.Object) {
                    if (result is TonApi.Error) {
                        L.e("TonApi.Error ${result.code}: ${result.message}")
                        val retryRequest = result.message.startsWith("LITE_SERVER_NOTREADY")
                                || result.message.contains("LITE_SERVER_UNKNOWN: block is not applied")
                        if (retryRequest) {
                            L.d("Retry send request")
                            trySendRequest(cont, request, this, exceptionHandler)
                        } else {
                            cont.resumeWithException(TonApiException(result))
                        }
                    } else {
                        cont.resume(result)
                    }
                }
            }
            trySendRequest(cont, request, resultHandler, exceptionHandler)
        }
    }

    private fun trySendRequest(
        continuation: Continuation<TonApi.Object>,
        request: TonApi.Function,
        resultHandler: ResultHandler,
        exceptionHandler: ExceptionHandler
    ) {
        try {
            ton.send(request, resultHandler, exceptionHandler)
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }

    @Throws(Exception::class)
    private suspend fun initClient() {
        if (isClientInitialized) {
            return
        }
        initMutex.withLock {
            if (isClientInitialized) {
                return@withLock
            }

            val request = Request.Builder().url(ConfigUrl).build()
            val response = okHttpClient.newCall(request).await()
            val configJson = response.body?.string() ?: ""
            val tonApiConfig = TonApi.Config(configJson, BlockChainName, false, false)
            liteClient = LiteClient(Dispatchers.Default, liteClientConfigGlobal = json.decodeFromString(configJson))

            val keyDirectory = File(FileUtils.getFilesDir(), "k")
            keyDirectory.mkdirs()
            val keyStoreType = TonApi.KeyStoreTypeDirectory(keyDirectory.absolutePath)
            val tonApiOptions = TonApi.Options(tonApiConfig, keyStoreType)
            val result = sendRequestInternal(TonApi.Init(tonApiOptions))
            if (result is TonApi.OptionsInfo) {
                defaultRwalletInitPublicKey = result.configInfo.defaultRwalletInitPublicKey
                defaultWalletId = result.configInfo.defaultWalletId
                isClientInitialized = true
            }
        }
    }

    @WorkerThread
    private fun handleUpdate(obj: TonApi.Object) {
        if (obj is TonApi.UpdateSyncState) {
            if (obj.syncState is TonApi.SyncStateInProgress) {
                val syncState = obj.syncState as TonApi.SyncStateInProgress
                val progress = (syncState.currentSeqno - syncState.fromSeqno).toFloat() / (syncState.toSeqno - syncState.fromSeqno) * 100f
                (syncProgressFlow as MutableStateFlow).tryEmit(progress.roundToInt())
            } else if (obj.syncState is TonApi.SyncStateDone) {
                (syncProgressFlow as MutableStateFlow).tryEmit(100)
            }
        }
    }

    private companion object {
        private const val BlockChainName = "mainnet"
        private const val ConfigUrl = "https://ton.org/global-config-wallet.json"
    }
}