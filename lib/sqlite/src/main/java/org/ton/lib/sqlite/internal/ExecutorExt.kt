package org.ton.lib.sqlite.internal

import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import kotlin.coroutines.ContinuationInterceptor

internal suspend fun Executor.acquireTransactionThread(controlJob: Job): ContinuationInterceptor {
    return suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation {
            controlJob.cancel()
        }
        try {
            execute {
                runBlocking {
                    continuation.resume(coroutineContext[ContinuationInterceptor]!!, null)
                    controlJob.join()
                }
            }
        } catch (e: RejectedExecutionException) {
            continuation.cancel(IllegalStateException("Could not acquire thread to perform transaction", e))
        }
    }
}