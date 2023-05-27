package org.ton.wallet.screen.transaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import org.ton.wallet.R
import org.ton.wallet.data.db.transaction.TransactionDto
import org.ton.wallet.data.db.transaction.TransactionStatus
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.core.ext.setOnClickListener
import org.ton.wallet.lib.core.ext.setOnClickListenerWithLock
import org.ton.wallet.lib.screen.controller.BaseViewModelBottomSheetController
import org.ton.wallet.lib.screen.viewmodel.viewModels
import org.ton.wallet.uikit.drawable.IndeterminateProgressDrawable
import org.ton.wallet.util.ViewUtils

class TransactionDetailsController(args: Bundle) : BaseViewModelBottomSheetController<TransactionDetailsViewModel>(args) {

    override val viewModel by viewModels { TransactionDetailsViewModel(args) }

    private var _binding: TransactionDetailsBinding? = null
    private val binding get() = _binding!!

    override fun createBottomSheetView(inflater: LayoutInflater, container: ViewGroup?, savedViewState: Bundle?): View {
        _binding = TransactionDetailsBinding(inflater, container)
        ViewUtils.connectAppToolbarWithScrollableView(binding.toolbar, binding.scrollView)

        binding.explorerButton.setOnClickListenerWithLock { viewModel.onViewExplorerClicked(activity!!) }
        binding.hashLayout.setOnClickListener(viewModel::onHashClicked)
        binding.peerAddressLayout.setOnClickListener(viewModel::onPeerAddressClicked)
        binding.button.setOnClickListenerWithLock(viewModel::onButtonClicked)

        return binding.root
    }

    override fun onPostCreateView(view: View) {
        super.onPostCreateView(view)
        viewModel.stateFlow.launchInViewScope(::onStateChanged)
    }

    override fun onDestroyView(view: View) {
        _binding = null
        super.onDestroyView(view)
    }

    override fun onChangeEnded(changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) {
        super.onChangeEnded(changeHandler, changeType)
        if (changeType.isEnter) {
            binding.animationView.playAnimation()
        }
    }

    private fun onStateChanged(state: TransactionDetailsState?) {
        if (state == null) {
            return
        }
        binding.amountText.text = state.amount
        binding.feeText.text = state.fee
        binding.feeText.isVisible = state.fee != null
        binding.dateText.text = state.date
        binding.dateText.isVisible = state.date != null
        binding.messageText.text = state.message
        binding.messageText.isVisible = !state.message.isNullOrEmpty()
        binding.recipientLayout.isVisible = state.peerDns != null
        binding.recipientAddressText.text = state.peerDns
        binding.button.text = state.buttonText

        binding.peerAddressTitleText.text = when (state.type) {
            TransactionDto.Type.In -> Res.str(R.string.sender_address)
            TransactionDto.Type.Out -> Res.str(R.string.recipient_address)
            TransactionDto.Type.Unknown -> null
        }
        binding.peerAddressValueText.text = state.peerShortAddress
        binding.peerAddressLayout.isVisible = state.type != TransactionDto.Type.Unknown
        binding.hashValueText.text = state.hashShort
        binding.hashLayout.isVisible = state.hashShort != null
        binding.explorerButton.isVisible = state.hashShort != null

        when (state.status) {
            TransactionStatus.Executed -> Unit
            TransactionStatus.Pending -> {
                binding.statusText.text = Res.str(R.string.pending)
                binding.statusText.setTextColor(Res.color(R.color.blue))
            }
            TransactionStatus.Cancelled -> {
                binding.statusText.text = Res.str(R.string.cancelled)
                binding.statusText.setTextColor(Res.color(R.color.text_error))
            }
        }
        val startDrawable =
            if (state.status != TransactionStatus.Pending) {
                null
            } else {
                IndeterminateProgressDrawable(Res.dp(13)).apply {
                    setStrokeWidth(Res.dp(1.5f))
                    setColor(Res.color(R.color.blue))
                }
            }
        binding.statusText.setCompoundDrawablesRelativeWithIntrinsicBounds(startDrawable, null, null, null)
        binding.statusText.isVisible = state.status != TransactionStatus.Executed
    }

    companion object {

        const val ArgumentKeyTransactionInternalId = "internalId"
    }
}