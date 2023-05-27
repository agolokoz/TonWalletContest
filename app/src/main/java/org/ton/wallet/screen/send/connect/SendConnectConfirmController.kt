package org.ton.wallet.screen.send.connect

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import org.ton.lib.rlottie.RLottieDrawable
import org.ton.lib.rlottie.RLottieResourceLoader
import org.ton.wallet.R
import org.ton.wallet.lib.core.Formatter
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.core.ext.setOnClickListenerWithLock
import org.ton.wallet.lib.screen.controller.BaseViewModelBottomSheetController
import org.ton.wallet.lib.screen.viewmodel.viewModels
import org.ton.wallet.uikit.drawable.EmptyDrawable
import org.ton.wallet.uikit.drawable.IndeterminateProgressDrawable

class SendConnectConfirmController(args: Bundle) : BaseViewModelBottomSheetController<SendConnectConfirmViewModel>(args) {

    override val viewModel by viewModels { SendConnectConfirmViewModel(args) }

    private val drawableSize = Res.dp(20)
    private val emptyDrawable = EmptyDrawable(drawableSize, drawableSize)
    private val loadingDrawable = IndeterminateProgressDrawable(drawableSize)

    private lateinit var rootView: View
    private lateinit var amountText: TextView
    private lateinit var receiverTitleText: TextView
    private lateinit var feeTitleText: TextView
    private lateinit var receiverText: TextView
    private lateinit var feeText: TextView
    private lateinit var cancelButton: TextView
    private lateinit var confirmButton: TextView
    private lateinit var buttonsLayout: ViewGroup
    private lateinit var feeLoadingView: View
    private lateinit var doneImage: ImageView

    private var prevIsSent = false

    override fun createBottomSheetView(inflater: LayoutInflater, container: ViewGroup?, savedViewState: Bundle?): View {
        val view = inflater.inflate(R.layout.screen_send_confirm_connect, container, false)

        amountText = view.findViewById(R.id.sendConfirmConnectAmountText)
        RLottieResourceLoader.readRawResourceAsync(context, R.raw.lottie_main) { json, _, _ ->
            val animationDrawable = RLottieDrawable(json, "" + R.raw.lottie_main, Res.dp(44), Res.dp(44), true)
            animationDrawable.setAutoRepeat(1)
            animationDrawable.start()
            amountText.setCompoundDrawablesRelativeWithIntrinsicBounds(animationDrawable, null, null, null)
        }

        receiverText = view.findViewById(R.id.sendConfirmConnectReceiverText)
        receiverTitleText = view.findViewById(R.id.sendConfirmConnectReceiverTitleText)
        feeTitleText = view.findViewById(R.id.sendConfirmConnectFeeTitleText)
        feeText = view.findViewById(R.id.sendConfirmConnectFeeText)

        cancelButton = view.findViewById(R.id.sendConfirmConnectCancelButton)
        cancelButton.setOnClickListenerWithLock(viewModel::onCancelClicked)
        confirmButton = view.findViewById(R.id.sendConfirmConnectConfirmButton)
        confirmButton.setOnClickListenerWithLock(viewModel::onConfirmClicked)
        confirmButton.setCompoundDrawablesWithIntrinsicBounds(emptyDrawable, null, emptyDrawable, null)
        buttonsLayout = view.findViewById(R.id.sendConfirmConnectButtonsLayout)
        doneImage = view.findViewById(R.id.sendConfirmConnectDoneImage)

        feeLoadingView = view.findViewById(R.id.sendConfirmConnectFeeLoaderView)
        val feeDrawable = IndeterminateProgressDrawable(null)
        feeDrawable.setColor(Res.color(R.color.blue))
        feeLoadingView.background = feeDrawable

        rootView = view
        return view
    }

    override fun onPostCreateView(view: View) {
        super.onPostCreateView(view)
        viewModel.state.launchInViewScope(::setState)
    }

    override fun onAnimationFinished() {
        super.onAnimationFinished()
        rootView.requestLayout()
        receiverTitleText.requestLayout()
        feeTitleText.requestLayout()
    }

    private fun setState(state: SendConnectConfirmState) {
        val amountString = Formatter.getFormattedAmount(state.amount)
        amountText.text = Formatter.getBeautifiedAmount(amountString)
        receiverText.text = Formatter.getBeautifiedShortStringSafe(Formatter.getShortAddress(state.receiver))
        feeLoadingView.isVisible = state.feeString == null
        feeText.text = state.feeString
        val continueButton = if (state.isSending) loadingDrawable else emptyDrawable
        confirmButton.setCompoundDrawablesWithIntrinsicBounds(emptyDrawable, null, continueButton, null)

        if (prevIsSent != state.isSent && state.isSent) {
            buttonsLayout.animate().cancel()
            buttonsLayout.animate()
                .alpha(0f)
                .scaleX(0f)
                .scaleY(0f)
                .setDuration(150L)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        doneImage.alpha = 0f
                        doneImage.scaleX = 0f
                        doneImage.scaleY = 0f
                        doneImage.isVisible = true

                        doneImage.animate().cancel()
                        doneImage.animate()
                            .alpha(1f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(150L)
                            .start()
                    }
                })
                .start()
        }
        prevIsSent = state.isSent
    }

    companion object {

        const val ArgumentKeyTransfer = "transfer"
    }
}