package org.ton.wallet.screen.activity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.PopupWindow
import androidx.annotation.UiThread
import androidx.core.view.setMargins
import org.ton.wallet.lib.core.Res
import org.ton.wallet.uikit.view.SnackBarView
import org.ton.wallet.util.FlowBusEvent
import kotlin.math.min

class SnackBarDelegate(
    private val rootLayout: FrameLayout
) {

    private val handler = Handler(Looper.getMainLooper())

    private val snackBarPopupWindow by lazy {
        SnackBarPopupWindow(rootLayout.context)
    }

    @UiThread
    fun showMessage(message: FlowBusEvent.ShowSnackBar) {
        if (snackBarPopupWindow.isShowing) {
            handler.removeCallbacksAndMessages(null)
            snackBarPopupWindow.close {
                handler.post {
                    showMessageInternal(message)
                }
            }
        } else {
            showMessageInternal(message)
        }
    }

    @UiThread
    fun hideMessage() {
        if (snackBarPopupWindow.isShowing) {
            handler.removeCallbacksAndMessages(null)
            snackBarPopupWindow.close()
        }
    }

    fun onDestroy() {
        if (snackBarPopupWindow.isShowing) {
            snackBarPopupWindow.dismiss()
        }
    }

    private fun showMessageInternal(message: FlowBusEvent.ShowSnackBar) {
        snackBarPopupWindow.setMessage(message)
        snackBarPopupWindow.show(rootLayout) {
            handler.postDelayed(hideSnackBarViewRunnable, message.durationMs)
        }
    }

    private val hideSnackBarViewRunnable = Runnable {
        snackBarPopupWindow.close()
    }

    private class SnackBarPopupWindow(context: Context) : PopupWindow(),
        PopupWindow.OnDismissListener {

        private val snackBarView = SnackBarView(context)
        private val margin = Res.dp(8)
        private var actionOnDismiss: (() -> Unit)? = null

        init {
            val rootLayout = FrameLayout(context)
            contentView = rootLayout
            width = min(Res.screenWidth, Res.dp(400))
            height = WindowManager.LayoutParams.WRAP_CONTENT

            snackBarView.setOnClickListener { close() }
            val snackBarLayoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            snackBarLayoutParams.setMargins(margin)
            rootLayout.addView(snackBarView, snackBarLayoutParams)
            setOnDismissListener(this)
        }

        override fun onDismiss() {
            actionOnDismiss?.invoke()
            actionOnDismiss = null
        }

        fun setMessage(message: FlowBusEvent.ShowSnackBar) {
            snackBarView.setTitle(message.title)
            snackBarView.setMessage(message.message)
            snackBarView.setImage(message.drawable)
            snackBarView.prepare()
        }

        fun show(view: View, actionOnEnd: (() -> Unit)? = null) {
            val widthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
            val heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            snackBarView.measure(widthSpec, heightSpec)
            snackBarView.translationY = snackBarView.measuredHeight.toFloat() + margin

            showAtLocation(view, Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM, 0, 0)
            animateSnackBar(0f, actionOnEnd)
        }

        fun close(actionOnDismiss: (() -> Unit)? = null) {
            this.actionOnDismiss = actionOnDismiss
            animateSnackBar(snackBarView.measuredHeight.toFloat() + margin, ::dismiss)
        }

        private fun animateSnackBar(translation: Float, actionOnEnd: (() -> Unit)?) {
            snackBarView.animate().cancel()
            snackBarView.animate()
                .translationY(translation)
                .setInterpolator(DecelerateInterpolator(2.0f))
                .setDuration(200L)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        actionOnEnd?.invoke()
                    }
                })
                .start()
        }
    }
}