package org.ton.wallet.lib.screen.controller

import android.os.Bundle

interface ResultListener {

    fun onResult(code: String, data: Bundle)
}