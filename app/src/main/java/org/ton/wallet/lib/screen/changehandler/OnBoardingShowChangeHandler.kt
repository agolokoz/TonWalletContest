package org.ton.wallet.lib.screen.changehandler

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.changehandler.AnimatorChangeHandler

class OnBoardingShowChangeHandler(duration: Long = 200L) : AnimatorChangeHandler(duration, true) {

    override fun getAnimator(container: ViewGroup, from: View?, to: View?, isPush: Boolean, toAddedToContainer: Boolean): Animator {
        val topControllerView = if (isPush) to else from
        if (topControllerView != null) {
            return ObjectAnimator.ofFloat(topControllerView, View.ALPHA, 0f, 1f)
        }
        return AnimatorSet()
    }

    override fun resetFromView(from: View) {
        from.alpha = 1f
    }
}