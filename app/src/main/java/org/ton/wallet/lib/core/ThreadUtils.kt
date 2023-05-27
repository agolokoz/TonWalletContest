package org.ton.wallet.lib.core

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

object ThreadUtils {

    private val handler = Handler(Looper.getMainLooper())

    private val defaultExecutor = Dispatchers.Default.asExecutor()

    val ioExecutor = Dispatchers.IO.asExecutor()

    val appCoroutineScope = CoroutineScope(Dispatchers.Default)

    fun postOnMain(runnable: Runnable) {
        postOnMain(runnable, 0L)
    }

    fun postOnMain(runnable: Runnable, delayMs: Long) {
        handler.postDelayed(runnable, delayMs)
    }

    fun postOnDefault(runnable: Runnable) {
        defaultExecutor.execute(runnable)
    }

    fun cancelOnMain(runnable: Runnable) {
        handler.removeCallbacks(runnable)
    }
}