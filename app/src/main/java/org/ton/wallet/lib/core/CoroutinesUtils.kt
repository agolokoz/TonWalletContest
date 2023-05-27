package org.ton.wallet.lib.core

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

object CoroutinesUtils {

    val appCoroutinesScope by lazy {
        CoroutineScope(SupervisorJob() + getCoroutineExceptionHandler("AppScope"))
    }

    fun getCoroutineExceptionHandler(tag: String?): CoroutineExceptionHandler {
        return CoroutineExceptionHandler { _, throwable ->
            L.e(tag, throwable)
        }
    }
}