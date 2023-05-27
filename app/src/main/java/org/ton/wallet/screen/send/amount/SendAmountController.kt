package org.ton.wallet.screen.send.amount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.core.view.isVisible
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import org.ton.lib.rlottie.RLottieImageView
import org.ton.wallet.R
import org.ton.wallet.lib.core.Formatter
import org.ton.wallet.lib.core.ext.setOnClickListener
import org.ton.wallet.lib.core.ext.setOnClickListenerWithLock
import org.ton.wallet.lib.core.ext.setSelectionSafe
import org.ton.wallet.lib.screen.controller.BaseViewModelBottomSheetController
import org.ton.wallet.lib.screen.viewmodel.viewModels
import org.ton.wallet.uikit.view.AppEditText
import org.ton.wallet.uikit.view.NumPadView
import org.ton.wallet.uikit.view.SwitchView
import org.ton.wallet.util.ViewUtils

class SendAmountController(args: Bundle) : BaseViewModelBottomSheetController<SendAmountViewModel>(args) {

    override val viewModel by viewModels { SendAmountViewModel(args) }
    override val isFullHeight = true

    private lateinit var animationView: RLottieImageView
    private lateinit var amountEditText: AppEditText
    private lateinit var sendAllAmountText: TextView
    private lateinit var sendAllAmountSwitch: SwitchView
    private lateinit var insufficientFundsText: TextView
    private lateinit var continueButton: View

    override fun createBottomSheetView(inflater: LayoutInflater, container: ViewGroup?, savedViewState: Bundle?): View {
        val view = inflater.inflate(R.layout.screen_send_amount, container, false)
        ViewUtils.connectAppToolbarWithScrollableView(view.findViewById(R.id.sendAmountToolbar), view.findViewById(R.id.sendAmountScrollView))
        view.findViewById<TextView>(R.id.sendAmountToText).text = viewModel.sendToString
        view.findViewById<View>(R.id.sendAmountSendAllBackground).setOnClickListener(viewModel::onSendAllClicked)
        view.findViewById<NumPadView>(R.id.sendAmountNumPadView).setNumPadViewListener(viewModel)

        continueButton = view.findViewById(R.id.sendAmountContinueButton)
        continueButton.setOnClickListenerWithLock(viewModel::onContinueClicked)

        view.findViewById<TextView>(R.id.sendAmountToEdit).isVisible = viewModel.isAddressEditable
        if (viewModel.isAddressEditable) {
            view.findViewById<View>(R.id.sendAmountToBackground).setOnClickListener(viewModel::onEditAddressClicked)
        }

        amountEditText = view.findViewById(R.id.sendAmountEditText)
        amountEditText.addSelectionChangedListener(viewModel::onTextSelectionChanged)

        animationView = view.findViewById(R.id.sendAmountAnimationView)
        insufficientFundsText = view.findViewById(R.id.sendAmountInsufficientFundsText)
        sendAllAmountText = view.findViewById(R.id.sendAmountSendAllValue)
        sendAllAmountSwitch = view.findViewById(R.id.sendAmountSendAllSwitch)

        return view
    }

    override fun onPostCreateView(view: View) {
        super.onPostCreateView(view)
        viewModel.balanceFlow.launchInViewScope(::onBalanceChanged)
        viewModel.amountFlow.launchInViewScope(amountEditText::setText)
        viewModel.amountSelectionFlow.launchInViewScope(amountEditText::setSelectionSafe)
        viewModel.sendAllCheckedFlow.launchInViewScope { isChecked -> sendAllAmountSwitch.setChecked(isChecked, true) }
        viewModel.isInsufficientFundsFlow.launchInViewScope { isInsufficientFunds ->
            insufficientFundsText.isVisible = isInsufficientFunds
        }
    }

    override fun onChangeStarted(changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) {
        super.onChangeStarted(changeHandler, changeType)
        if (changeType.isPush && !changeType.isEnter) {
            activity!!.window.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            amountEditText.clearFocus()
        }
    }

    override fun onChangeEnded(changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) {
        super.onChangeEnded(changeHandler, changeType)
        if (changeType.isEnter) {
            animationView.playAnimation()
            activity!!.window.setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM, WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            amountEditText.requestFocus()
        }
    }

    override fun setBottomSheetTranslation(translation: Float) {
        super.setBottomSheetTranslation(translation)
        continueButton.invalidate()
    }

    private fun onBalanceChanged(balance: Long?) {
        sendAllAmountText.text = balance?.let(Formatter::getFormattedAmount)
        sendAllAmountText.isVisible = balance != null
    }

    companion object {

        const val ArgumentKeyAddress = "address"
        const val ArgumentKeyAddressEditable = "addressEditable"
        const val ArgumentKeyAmount = "amount"
        const val ArgumentKeyComment = "comment"
        const val ArgumentKeyDns = "dns"
    }
}