package org.ton.wallet.lib.core.ext

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import org.ton.wallet.R
import org.ton.wallet.lib.core.L
import org.ton.wallet.lib.core.Res
import org.ton.wallet.util.FlowBus
import org.ton.wallet.util.FlowBusEvent

fun Context.copyToClipboard(content: String, message: String?, withVibrate: Boolean = true) {
    try {
        val clipboardManager = getSystemService(ClipboardManager::class.java)
        clipboardManager.setPrimaryClip(ClipData.newPlainText("clipboard", content))
    } catch (e: Exception) {
        L.e(e)
    }
    if (withVibrate) {
        vibrate()
    }
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
        message?.let { msg ->
            FlowBus.common.dispatch(FlowBusEvent.ShowSnackBar(
                message = msg,
                drawable = Res.drawableColored(R.drawable.ic_copy_28, Res.color(R.color.common_white))
            ))
        }
    }
}

fun Context.toActivitySafe(): Activity? {
    var context = this
    while (context !is Activity && context is ContextWrapper) {
        context = context.baseContext
    }
    return context as? Activity
}

fun Context.vibrate(durationMs: Long = 200) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val effect = VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
        getSystemService(Vibrator::class.java).vibrate(effect)
    } else {
        getSystemService(Vibrator::class.java).vibrate(durationMs)
    }
}