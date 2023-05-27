package org.ton.wallet.util

import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener

class EventSourceProxy(
    private val listener: EventSourceListener
) : EventSourceListener() {

    override fun onOpen(eventSource: EventSource, response: Response) {
        super.onOpen(eventSource, response)
        listener.onOpen(eventSource, response)
    }

    override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
        super.onEvent(eventSource, id, type, data)
        listener.onEvent(eventSource, id, type, data)
    }

    override fun onClosed(eventSource: EventSource) {
        super.onClosed(eventSource)
        listener.onClosed(eventSource)
    }

    override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
        super.onFailure(eventSource, t, response)
        listener.onFailure(eventSource, t, response)
    }
}