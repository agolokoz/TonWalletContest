package org.ton.wallet.screen.scanqr

import android.app.Activity
import android.os.Bundle
import org.ton.wallet.lib.core.AndroidUtils
import org.ton.wallet.lib.screen.viewmodel.BaseViewModel

class ScanQrViewModel : BaseViewModel() {

    private val resultBundle = Bundle()

    fun onBackClicked() {
        navigator.back()
    }

    fun onOpenSettingsClicked(activity: Activity) {
        AndroidUtils.openAppSettings(activity)
    }

    fun onQrDetected(value: String) {
        resultBundle.putString(ScanQrController.ArgumentKeyQrValue, value)
        setResult(ScanQrController.ResultCodeQrDetected, resultBundle)
    }
}