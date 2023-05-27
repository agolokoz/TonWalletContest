package org.ton.wallet.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.ton.wallet.lib.core.CoroutinesUtils

class FlowBus<T> {

    private val scope = CoroutineScope(SupervisorJob() + CoroutinesUtils.getCoroutineExceptionHandler("FlowBus"))

    private val _eventsFlow = MutableSharedFlow<T>()
    val eventsFlow: SharedFlow<T> = _eventsFlow.asSharedFlow()

    fun dispatch(event: T) {
        scope.launch { _eventsFlow.emit(event) }
    }

    companion object {

        val common by lazy { FlowBus<Any>() }
    }
}