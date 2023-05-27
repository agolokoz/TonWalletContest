package org.ton.wallet.lib.screen.changehandler

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import com.bluelinelabs.conductor.changehandler.AnimatorChangeHandler

class BottomSheetSlideAnimator : AnimatorChangeHandler(500, false) {

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
        set.interpolator = DecelerateInterpolator(2.0f)
        return set
    }

    override fun resetFromView(from: View) {
        from.translationX = 0f
    }
}