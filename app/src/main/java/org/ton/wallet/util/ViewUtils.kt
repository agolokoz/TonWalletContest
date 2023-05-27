package org.ton.wallet.util

import android.view.View
import android.view.View.OnScrollChangeListener
import androidx.core.math.MathUtils.clamp
import org.ton.wallet.lib.core.Res
import org.ton.wallet.uikit.view.AppToolbar

object ViewUtils {

    private val threshold = Res.dp(16)

    fun connectAppToolbarWithScrollableView(toolbar: AppToolbar, view: View) {
        val scrollChangeListener = OnScrollChangeListener { _, _, scrollY, _, _ ->
            toolbar.setShadowAlpha(clamp(scrollY.toFloat() / threshold, 0f, 1f))
        }
        scrollChangeListener.onScrollChange(view, 0, view.scrollY, 0, 0)
        view.setOnScrollChangeListener(scrollChangeListener)
    }
}