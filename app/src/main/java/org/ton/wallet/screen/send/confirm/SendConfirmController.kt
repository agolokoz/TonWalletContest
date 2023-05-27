package org.ton.wallet.screen.send.confirm

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import org.ton.wallet.R
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.core.ext.addSpans
import org.ton.wallet.lib.core.ext.setOnClickListenerWithLock
import org.ton.wallet.lib.core.ext.setTextWithSelection
import org.ton.wallet.lib.screen.controller.BaseViewModelBottomSheetController
import org.ton.wallet.lib.screen.viewmodel.viewModels
import org.ton.wallet.uikit.drawable.IndeterminateProgressDrawable
import org.ton.wallet.uikit.view.AppEditText
import org.ton.wallet.util.ViewUtils
import kotlin.math.abs

class SendConfirmController(args: Bundle) : BaseViewModelBottomSheetController<SendConfirmViewModel>(args) {

    override val viewModel by viewModels { SendConfirmViewModel(args) }

    override val isFullHeight = true

    private lateinit var messageEditText: AppEditText
    private lateinit var recipientText: TextView
    private lateinit var amountText: TextView
    private lateinit var feeText: TextView
    private lateinit var feeLoadingView: View
    private lateinit var messageCharsText: TextView

    override fun createBottomSheetView(inflater: LayoutInflater, container: ViewGroup?, savedViewState: Bundle?): View {
        val view = inflater.inflate(R.layout.screen_send_confirm, container, false)
        ViewUtils.connectAppToolbarWithScrollableView(view.findViewById(R.id.sendConfirmToolbar), view.findViewById(R.id.sendConfirmScrollView))
        view.findViewById<View>(R.id.sendConfirmContinueButton).setOnClickListenerWithLock { viewModel.onConfirmClicked(activity!!) }

        messageCharsText = view.findViewById(R.id.sendConfirmMessageCharactersText)
        amountText = view.findViewById(R.id.sendConfirmAmountValue)
        feeText = view.findViewById(R.id.sendConfirmFeeValue)

        messageEditText = view.findViewById(R.id.sendConfirmDescriptionEditText)
        messageEditText.addTextChangedListener { viewModel.onTextChanged(it?.toString() ?: "") }
        messageEditText.setText(viewModel.presetMessage ?: "")

        recipientText = view.findViewById(R.id.sendConfirmRecipientValue)
        recipientText.text = viewModel.recipient

        feeLoadingView = view.findViewById(R.id.sendConfirmFeeLoadingView)
        val feeDrawable = IndeterminateProgressDrawable(null)
        feeDrawable.setColor(Res.color(R.color.blue))
        feeLoadingView.background = feeDrawable

        return view
    }

    override fun onPostCreateView(view: View) {
        super.onPostCreateView(view)
        viewModel.amountStringFlow.launchInViewScope(amountText::setText)
        viewModel.feeStateFlow.launchInViewScope(::onFeeStateChanged)
        viewModel.messageLeftSymbolsFlow.launchInViewScope(::onMessageCharactersLeftChanged)
    }

    private fun onFeeStateChanged(feeState: SendConfirmFeeState) {
        if (feeState is SendConfirmFeeState.Value) {
            feeText.text = feeState.fee
        }
        feeText.isVisible = feeState is SendConfirmFeeState.Value
        feeLoadingView.isVisible = feeState is SendConfirmFeeState.Loading
    }

    private fun onMessageCharactersLeftChanged(count: Int) {
        messageCharsText.isVisible = count <= 24
        if (messageCharsText.isVisible) {
            if (count >= 0) {
                messageCharsText.text = Res.str(R.string.characters_left, count)
                messageCharsText.setTextColor(Res.color(R.color.orange))
            } else {
                messageCharsText.text = Res.str(R.string.characters_exceeded, abs(count))
                messageCharsText.setTextColor(Res.color(R.color.text_error))

                val messageText = messageEditText.text?.toString() ?: ""
                val spannableBuilder = SpannableStringBuilder(messageText)
                val color = Res.color(R.color.text_error)
                spannableBuilder.addSpans(listOf(
                    ForegroundColorSpan(color),
                    BackgroundColorSpan(ColorUtils.setAlphaComponent(color, 31))
                ), messageText.length - abs(count), messageText.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                messageEditText.setTextWithSelection(spannableBuilder, messageEditText.selectionStart)
            }
        }
    }

    companion object {
        const val ArgumentKeyAmount = "amount"
        const val ArgumentKeyComment = "comment"
        const val ArgumentKeyRecipientAddress = "recipient"
    }
}