package org.ton.wallet.screen.send.processing

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.math.MathUtils.clamp
import androidx.core.view.isVisible
import org.ton.lib.rlottie.RLottieImageView
import org.ton.wallet.R
import org.ton.wallet.lib.core.ext.setOnClickListener
import org.ton.wallet.lib.screen.controller.BaseViewModelBottomSheetController
import org.ton.wallet.lib.screen.viewmodel.viewModels
import org.ton.wallet.util.CubicBezierInterpolator

class SendProcessingController(args: Bundle) : BaseViewModelBottomSheetController<SendProcessingViewModel>(args) {

    override val viewModel by viewModels { SendProcessingViewModel(args) }

    override val isFullHeight = true

    private lateinit var progressLayout: ViewGroup
    private lateinit var processingAnimationView: RLottieImageView
    private lateinit var completedLayout: ViewGroup
    private lateinit var completedAnimationView: RLottieImageView
    private lateinit var completedSentText: TextView

    override fun createBottomSheetView(inflater: LayoutInflater, container: ViewGroup?, savedViewState: Bundle?): View {
        val view = inflater.inflate(R.layout.screen_send_processing, container, false)
        view.findViewById<View>(R.id.sendProcessingCloseButton).setOnClickListener(viewModel::onCloseClicked)
        view.findViewById<View>(R.id.sendProcessingWalletButton).setOnClickListener(viewModel::onViewWalletClicked)

        var address = viewModel.address
        address = address.replaceRange(address.length / 2, address.length / 2, "\n")
        view.findViewById<TextView>(R.id.sendProcessingAddressText).text = address

        progressLayout = view.findViewById(R.id.sendProcessingProgressLayout)
        processingAnimationView = view.findViewById(R.id.sendProcessingProgressAnimationView)

        completedLayout = view.findViewById(R.id.sendProcessingCompletedLayout)
        completedAnimationView = view.findViewById(R.id.sendProcessingCompletedAnimationView)
        completedSentText = view.findViewById(R.id.sendProcessingSentText)

        return view
    }

    override fun onPostCreateView(view: View) {
        super.onPostCreateView(view)
        viewModel.showCompletedFlow.launchInViewScope(::setShowCompleted)
        viewModel.amountTextFlow.launchInViewScope(completedSentText::setText)
    }

    private fun setShowCompleted(unit: Unit) {
        completedAnimationView.playAnimation()
        completedLayout.isVisible = true
        ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener { animator ->
                val value = animator.animatedValue as Float
                val progressValue = clamp((1f - value) * 2f, 0f, 1f)
                progressLayout.alpha = progressValue
                progressLayout.scaleX = progressValue
                progressLayout.scaleY = progressValue

                val completedValue = clamp((value - 0.5f) * 2f, 0f, 1f)
                completedLayout.alpha = completedValue
                completedLayout.scaleX = completedValue
                completedLayout.scaleY = completedValue
            }
            interpolator = CubicBezierInterpolator.Default
            duration = 400
            start()
        }
    }

    companion object {

        const val ArgumentKeyAddress = "address"
        const val ArgumentKeyAmount = "amount"
        const val ArgumentKeyFee = "fee"
        const val ArgumentKeyMessage = "message"

        const val ResultKeyFeeChanged = "feeChanged"
    }
}