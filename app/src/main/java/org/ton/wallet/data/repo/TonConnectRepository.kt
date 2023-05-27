package org.ton.wallet.data.repo

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.ton.crypto.hex
import org.ton.lib.security.TonSecurity
import org.ton.wallet.data.db.connect.ConnectDao
import org.ton.wallet.data.db.connect.ConnectDto
import org.ton.wallet.data.model.TonConnect
import org.ton.wallet.data.model.TonConnectEvent
import org.ton.wallet.lib.core.L
import org.ton.wallet.lib.core.ThreadUtils
import org.ton.wallet.lib.core.ext.await
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

interface TonConnectRepository : BaseRepository {

    @Throws(Exception::class)
    suspend fun getManifestInfo(manifestUrl: String): TonConnect.Manifest?

    suspend fun checkExistingConnections(accountId: Int)

    @Throws(Exception::class)
    suspend fun connect(accountId: Int, clientId: String)

    suspend fun sendMessage(accountId: Int, clientId: String, body: ByteArray)

    fun getEventsFlow(accountId: Int): Flow<TonConnectEvent>
}

class TonConnectRepositoryImpl(
    private val okHttpClient: OkHttpClient,
    private val sseHttpClient: OkHttpClient,
    private val json: Json,
    private val dao: ConnectDao
) : TonConnectRepository {

    private val eventSourceConnectionMap = ConcurrentHashMap<EventSource, ConnectDto>()
    private val eventSourceConnectContMap = ConcurrentHashMap<EventSource, Continuation<Unit>>()
    private val accountIdEventsMap = ConcurrentHashMap<Int, MutableSharedFlow<TonConnectEvent>>()

    private val bridgeHttpUrl = "https://bridge.tonapi.io/bridge".toHttpUrl()

    override suspend fun getManifestInfo(manifestUrl: String): TonConnect.Manifest? {
        val httpUrl = manifestUrl.toHttpUrlOrNull() ?: return null
        val request = Request.Builder().url(httpUrl).build()
        val response = okHttpClient.newCall(request).await()
        val jsonResponse = response.body?.string() ?: ""
        return json.decodeFromString<TonConnect.Manifest>(jsonResponse)
    }

    override suspend fun checkExistingConnections(accountId: Int) {
        val dbConnections = dao.getConnections(accountId)
        dbConnections.forEach { connection ->
            connect(connection.accountId, connection.clientId)
        }
    }

    override suspend fun connect(accountId: Int, clientId: String) {
        var connection = dao.getConnection(accountId, clientId)
        if (connection == null) {
            val cryptoBoxKeys = TonSecurity.nativeCryptoBoxInitKeys() ?: return
            connection = ConnectDto(
                accountId = accountId,
                clientId = clientId,
                publicKey = hex(cryptoBoxKeys.copyOfRange(0, 32)),
                secretKey = hex(cryptoBoxKeys.copyOfRange(32, 64)),
                requestId = -1
            )
        }

        val httpUrl = bridgeHttpUrl.newBuilder()
            .addPathSegment("events")
            .addQueryParameter("client_id", connection.publicKey)
            .build()
        val request = Request.Builder()
            .addHeader("Accept", "text/event-stream")
            .addHeader("Cache-Control", "no-cache")
            .addHeader("Connection", "keep-alive")
            .url(httpUrl)
            .build()

        return suspendCoroutine { cont ->
            val eventSource = EventSources.createFactory(sseHttpClient)
                .newEventSource(request, eventSourceListener)
            eventSourceConnectContMap[eventSource] = cont
            eventSourceConnectionMap[eventSource] = connection
        }
    }

    override suspend fun sendMessage(accountId: Int, clientId: String, body: ByteArray) {
        val connection = dao.getConnection(accountId, clientId) ?: return
        val encryptedBody = TonSecurity.nativeCryptoBox(body, hex(connection.clientId), hex(connection.secretKey)) ?: return
        val encryptedBodyBase64 = Base64.encodeToString(encryptedBody, Base64.NO_WRAP)
        val httpUrl = bridgeHttpUrl.newBuilder()
            .addPathSegment("message")
            .addQueryParameter("client_id", connection.publicKey)
            .addQueryParameter("to", clientId)
            .addQueryParameter("ttl", "300")
            .build()
        val request = Request.Builder()
            .post(encryptedBodyBase64.toRequestBody())
            .url(httpUrl)
            .build()
        okHttpClient.newCall(request).await()
    }

    override suspend fun deleteWallet() = Unit

    override fun getEventsFlow(accountId: Int): Flow<TonConnectEvent> {
        return accountIdEventsMap.getOrPut(accountId) { MutableSharedFlow(replay = 0, extraBufferCapacity = 1) }
    }


    private suspend fun onSseConnectionOpened(eventSource: EventSource, response: Response) {
        L.d("onSseConnectionOpened")
        val connection = eventSourceConnectionMap[eventSource] ?: return
        if (!dao.hasConnection(connection.accountId, connection.clientId)) {
            dao.addConnection(connection)
        }
        eventSourceConnectContMap[eventSource]?.let { continuation ->
            continuation.resume(Unit)
            eventSourceConnectContMap.remove(eventSource)
        }
    }

    private suspend fun onSseConnectionEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
        L.d("onSseConnectionEvent: ${id}, ${type}, $data")
        val connection = eventSourceConnectionMap[eventSource] ?: return
        val bridgeMessage = json.decodeFromString<TonConnect.BridgeMessage>(data)
        val encodedData = Base64.decode(bridgeMessage.message, Base64.NO_WRAP)
        val decodedData = TonSecurity.nativeCryptoBoxOpen(encodedData, hex(connection.clientId), hex(connection.secretKey)) ?: return
        val decodedString = String(decodedData)
        L.d("onSseConnectionEvent: $decodedString")
        val appRawRequest = json.decodeFromString<TonConnect.AppRawRequest>(decodedString)
        if (appRawRequest.id <= connection.requestId) {
            return
        }

        eventSourceConnectionMap[eventSource] = connection.copy(requestId = appRawRequest.id)
        dao.updateRequestId(connection.accountId, connection.clientId, appRawRequest.id)
        if (appRawRequest.method != "sendTransaction") {
            return
        }

        val payloads = appRawRequest.params.map { param ->
            json.decodeFromString<TonConnect.AppRawRequest.Payload>(param)
        }
        val payloadMsg = payloads.firstOrNull()?.messages?.firstOrNull() ?: return

        val event = TonConnectEvent.Transfer(
            clientId = connection.clientId,
            requestId = appRawRequest.id,
            rawAddress = payloadMsg.address,
            amount = payloadMsg.amount,
            stateInit = payloadMsg.stateInit?.let { Base64.decode(it, Base64.NO_WRAP) },
        )
        accountIdEventsMap[connection.accountId]?.emit(event)
    }

    private suspend fun onSseConnectionClosed(eventSource: EventSource) {
        L.d("onSseConnectionClosed")
    }

    private suspend fun onSseConnectionFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
        L.d("onSseConnectionFailure: ${t?.message}")
        eventSourceConnectContMap[eventSource]?.let { continuation ->
            continuation.resumeWithException(t ?: RuntimeException("onFailure"))
            eventSourceConnectContMap.remove(eventSource)
        }
    }


    private val eventSourceListener = object : EventSourceListener() {

        override fun onOpen(eventSource: EventSource, response: Response) {
            super.onOpen(eventSource, response)
            ThreadUtils.appCoroutineScope.launch(Dispatchers.IO) {
                onSseConnectionOpened(eventSource, response)
            }
        }

        override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
            super.onEvent(eventSource, id, type, data)
            ThreadUtils.appCoroutineScope.launch(Dispatchers.IO) {
                onSseConnectionEvent(eventSource, id, type, data)
            }
        }

        override fun onClosed(eventSource: EventSource) {
            super.onClosed(eventSource)
            ThreadUtils.appCoroutineScope.launch(Dispatchers.IO) {
                onSseConnectionClosed(eventSource)
            }
        }

        override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
            super.onFailure(eventSource, t, response)
            ThreadUtils.appCoroutineScope.launch(Dispatchers.IO) {
                onSseConnectionFailure(eventSource, t, response)
            }
        }
    }
}