package org.ton.wallet.util

import org.ton.wallet.R
import org.ton.wallet.data.ton.TonApiException
import org.ton.wallet.lib.core.Res
import java.net.UnknownHostException

object ErrorHandler {

    fun getErrorMessage(throwable: Throwable): String? {
        return when (val cause = getException(throwable)) {
            is UnknownHostException -> {
                Res.str(R.string.network_error)
            }
            is TonApiException -> {
                if (cause.error.message.startsWith("LITE_SERVER_NOTREADY")) {
                    null
                } else {
                    Res.str(R.string.unknown_error)
                }
            }
            else -> {
                Res.str(R.string.unknown_error)
            }
        }
    }

    private fun getException(throwable: Throwable): Throwable? {
        return when (throwable) {
            is UnknownHostException -> throwable
            is TonApiException -> throwable
            else -> throwable.cause?.let(::getException)
        }
    }
}