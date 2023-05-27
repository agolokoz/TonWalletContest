package org.ton.wallet.lib.screen.changehandler

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import com.bluelinelabs.conductor.changehandler.AnimatorChangeHandler
import org.ton.wallet.R
import org.ton.wallet.lib.core.Res

abstract class BaseSlideChangeHandler(removesViewOnPush: Boolean) : AnimatorChangeHandler(500, removesViewOnPush) {

    override fun getAnimator(container: ViewGroup, from: View?, to: View?, isPush: Boolean, toAddedToContainer: Boolean): Animator {
        val set = AnimatorSet()

        val topControllerView = if (isPush) to else from
        topControllerView?.let { controller ->
            val translationStart =
                if (isPush && toAddedToContainer) controller.width.toFloat()
                else controller.translationX
            val translationEnd = if (isPush) 0f else controller.width.toFloat()
            val translationAnimator = ObjectAnimator.ofFloat(controller, View.TRANSLATION_X, translationStart, translationEnd)
            set.play(translationAnimator)
        }

        val bottomController = if (isPush) from else to
        bottomController?.let { controller ->
            val translationStart =
                if (isPush && toAddedToContainer) 0f
                else -controller.width * 0.5f
            val translationEnd =
                if (isPush) -controller.width * 0.5f
                else 0f
            val translationAnimator = ObjectAnimator.ofFloat(controller, View.TRANSLATION_X, translationStart, translationEnd)
            set.play(translationAnimator)

            val dimColor = Res.color(R.color.screen_dim_color)
            val startColor = if (isPush) Color.TRANSPARENT else dimColor
            val endColor = if (isPush) dimColor else Color.TRANSPARENT
            val colorDrawable = ColorDrawable(startColor)
            controller.foreground = colorDrawable

            val dimAnimator = ValueAnimator.ofArgb(startColor, endColor)
            dimAnimator.addUpdateListener { animator ->
                colorDrawable.color = animator.animatedValue as Int
            }
            set.play(dimAnimator)
        }

        set.interpolator = DecelerateInterpolator(2.0f)
        return set
    }

    override fun resetFromView(from: View) {
        from.translationX = 0f
    }
}

class SlideChangeHandler : BaseSlideChangeHandler(true)

class SlideChangeHandlerNoRemoveView : BaseSlideChangeHandler(false)