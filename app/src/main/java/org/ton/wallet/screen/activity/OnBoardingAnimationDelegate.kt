package org.ton.wallet.screen.activity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.widget.FrameLayout
import org.ton.wallet.R
import org.ton.wallet.lib.core.Res
import org.ton.wallet.uikit.drawable.TopRoundRectDrawable
import org.ton.wallet.util.CubicBezierInterpolator

class OnBoardingAnimationDelegate(
    private val rootView: FrameLayout,
    val animationType: MainActivityAnimationType
) {

    private val bottomSheetTranslation = Res.dimen(R.dimen.splash_bottom_sheet_top)
    private val bottomSheetRadius = Res.dimen(R.dimen.bottom_sheet_radius)
    private val bottomSheetDrawable = TopRoundRectDrawable()

    private var offsetAnimator: ValueAnimator? = null
    private var isOpenAnimationPlayed = false

    init {
        bottomSheetDrawable.setColor(Res.color(R.color.common_white))
        bottomSheetDrawable.setTopRadius(bottomSheetRadius)
        rootView.foreground = bottomSheetDrawable
    }

    fun startOpenAnimation(onAnimationEnd: () -> Unit) {
        if (isOpenAnimationPlayed) {
            return
        }
        isOpenAnimationPlayed = true
        bottomSheetDrawable.setTopOffset(bottomSheetTranslation)
        var targetTranslation: Float = bottomSheetTranslation
        var duration: Long = 0
        if (animationType == MainActivityAnimationType.BottomSheetUp) {
            targetTranslation = 0f
            duration = AnimationDurationMs
        } else if (animationType == MainActivityAnimationType.BottomSheetDown) {
            targetTranslation = Res.screenHeight.toFloat()
            duration = 250L
        }
        animate(targetTranslation, duration, onAnimationEnd)
    }

    private fun animate(targetTranslation: Float, duration: Long, onAnimationEnd: () -> Unit) {
        offsetAnimator?.cancel()
        offsetAnimator = ValueAnimator.ofFloat(bottomSheetDrawable.topOffset, targetTranslation).apply {
            addUpdateListener { animator ->
                val offset = animator.animatedValue as Float
                bottomSheetDrawable.setTopOffset(offset)
                val radiusFactor =
                    if (offset > bottomSheetRadius * 4) 1f
                    else offset / (bottomSheetRadius * 4)
                bottomSheetDrawable.setTopRadius(bottomSheetRadius * radiusFactor)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    rootView.foreground = null
                    onAnimationEnd.invoke()
                }
            })
            applyAnimatorProperties(this)
            setDuration(duration)
            start()
        }
    }

    companion object {

        private const val AnimationDurationMs = 500L

        fun applyAnimatorProperties(animator: Animator) {
            animator.duration = AnimationDurationMs
            animator.interpolator = CubicBezierInterpolator.EaseOutQuint
        }
    }
}