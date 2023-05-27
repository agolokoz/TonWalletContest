package org.ton.wallet.lib.core.ext

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.EditText
import org.ton.wallet.lib.core.L
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.core.TimeoutLock
import kotlin.math.sin

private val clickTimeoutLock = TimeoutLock(350)

fun View.animateShake(
    shakesCount: Int,
    amplitude: Float = Res.dp(10f),
    durationMs: Long = 300L,
    scaleToOriginal: Boolean = false
) {
    this.animate().cancel()
    val animator = this.animate()
        .setUpdateListener { animator ->
            val value = animator.animatedValue as Float
            this.translationX = (sin(value * Math.PI * shakesCount) * amplitude).toFloat()
        }
        .setDuration(durationMs)
    if (scaleToOriginal) {
        animator.scaleX(1f).scaleY(1f)
    }
    animator.start()
}

fun View.animateBounce(
    bouncesCount: Int,
    amplitudeScale: Float = 1.1f,
    durationMs: Long = 300L
) {
    val scaleFactor = amplitudeScale - 1f
    this.animate().cancel()
    this.animate()
        .setUpdateListener { animator ->
            val value = animator.animatedFraction
            val scale = 1f + (sin(value * Math.PI * bouncesCount) * scaleFactor).toFloat()
            this.scaleX = scale
            this.scaleY = scale
        }
        .setDuration(durationMs)
        .setInterpolator(LinearInterpolator())
        .start()
}

fun View.containsMotionEvent(touchParentView: View, event: MotionEvent): Boolean {
    var viewLeft = left
    var viewTop = top

    var currentParentView = parent as? ViewGroup
    while (currentParentView != null) {
        viewLeft += currentParentView.left - currentParentView.scrollX
        viewTop += currentParentView.top - currentParentView.scrollY
        currentParentView =
            if (currentParentView == touchParentView) null
            else currentParentView.parent as? ViewGroup
    }

    return viewLeft + translationX <= event.x && event.x <= viewLeft + width + translationX
            && viewTop + translationY <= event.y && event.y <= viewTop + height + translationY
}

fun EditText.setSelectionSafe(position: Int) {
    try {
        setSelection(position)
    } catch (e: Exception) {
        L.e(e)
    }
}

fun EditText.setTextWithSelection(text: CharSequence, selection: Int = text.length) {
    setText(text)
    setSelectionSafe(selection)
}

fun View.setOnClickListener(action: () -> Unit) {
    setOnClickListener { action() }
}

fun View.setOnClickListenerWithLock(clickListener: () -> Unit) {
    setOnClickListener(withLock(clickListener))
}

fun View.setOnClickListenerWithLock(clickListener: () -> Unit, lockTimeMs: Long) {
    setOnClickListener(withLock(clickListener, lockTimeMs))
}

private fun withLock(clickListener: () -> Unit, lockTimeMs: Long = 0): View.OnClickListener {
    return View.OnClickListener {
        if (!clickTimeoutLock.checkAndMaybeLock(lockTimeMs)) {
            clickListener.invoke()
        }
    }
}