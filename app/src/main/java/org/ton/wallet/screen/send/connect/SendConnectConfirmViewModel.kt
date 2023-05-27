package org.ton.wallet.screen.send.connect

import android.os.Bundle
import com.bluelinelabs.conductor.ControllerChangeType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.ton.wallet.R
import org.ton.wallet.data.model.TonConnect
import org.ton.wallet.data.model.TonConnectEvent
import org.ton.wallet.domain.UseCases
import org.ton.wallet.lib.core.Formatter
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.core.ThreadUtils
import org.ton.wallet.lib.navigator.Screen
import org.ton.wallet.lib.navigator.ScreenParams
import org.ton.wallet.lib.screen.changehandler.SlideChangeHandlerNoRemoveView
import org.ton.wallet.lib.screen.viewmodel.BaseViewModel
import org.ton.wallet.screen.pass.base.BasePassCodeController
import org.ton.wallet.screen.pass.enter.PassCodeEnterController
import org.ton.wallet.util.ErrorHandler
import org.ton.wallet.util.FlowBus
import org.ton.wallet.util.FlowBusEvent

class SendConnectConfirmViewModel(args: Bundle) : BaseViewModel() {

    private val transfer = args.getParcelable<TonConnectEvent.Transfer>(SendConnectConfirmController.ArgumentKeyTransfer)
        ?: throw IllegalArgumentException("Transfer is null")

    private var ufAddress = ""
    private var passCodeEntered = false
    private var sendJob: Job? = null

    val state = MutableStateFlow(SendConnectConfirmState(transfer.amount, "", null, false, false))

    init {
        viewModelScope.launch(Dispatchers.IO) {
            ufAddress = UseCases.getUfAddress(transfer.rawAddress) ?: transfer.rawAddress
            state.value = state.value.copy(receiver = ufAddress)

            val fee = UseCases.getSendFee(ufAddress, transfer.amount, "")
            val feeString = Formatter.getFormattedAmount(fee)
            state.value = state.value.copy(feeString = "≈ $feeString TON")
        }
    }

    override fun onResult(code: String, data: Bundle) {
        super.onResult(code, data)
        if (code == PassCodeEnterController.ResultKeyCorrectPassCodeEntered) {
            val purpose = data.getString(PassCodeEnterController.ArgumentKeyPurpose)
            if (purpose == PassCodeEnterPurposeConfirmSend) {
                passCodeEntered = true
            }
        }
    }

    override fun onDestroy() {
        sendJob?.cancel()
        super.onDestroy()
    }

    override fun onScreenChange(isStarted: Boolean, changeType: ControllerChangeType) {
        super.onScreenChange(isStarted, changeType)
        if (!isStarted && !changeType.isPush && changeType.isEnter && passCodeEntered) {
            performSend()
        }
    }

    fun onCancelClicked() {
        ThreadUtils.appCoroutineScope.launch(Dispatchers.IO) {
            val error = TonConnect.SendTransactionResponse.Error(
                id = transfer.requestId.toString(),
                error = TonConnect.Error(
                    code = 300,
                    message = "User declined the transaction"
                )
            )
            UseCases.tonConnectSendTransactionError(transfer.clientId, error)
        }
        navigator.onBackPressed()
    }

    fun onConfirmClicked() {
        if (sendJob?.isActive == true || state.value.feeString == null) {
            return
        }
        passCodeEntered = false
        val args = Bundle()
        args.putBoolean(BasePassCodeController.ArgumentKeyBackVisible, false)
        args.putBoolean(BasePassCodeController.ArgumentKeyDark, false)
        args.putBoolean(PassCodeEnterController.ArgumentKeyPopOnSuccess, true)
        args.putString(PassCodeEnterController.ArgumentKeyPurpose, PassCodeEnterPurposeConfirmSend)
        navigator.add(ScreenParams(Screen.PassCodeEnter, args, SlideChangeHandlerNoRemoveView(), SlideChangeHandlerNoRemoveView(), this))
    }

    private fun performSend() {
        if (sendJob?.isActive == true) {
            return
        }
        state.value = state.value.copy(isSending = true)
        sendJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                UseCases.performSend(ufAddress, transfer.amount, "")
                state.value = state.value.copy(isSent = true)
                ThreadUtils.appCoroutineScope.launch(Dispatchers.IO) {
                    val success = TonConnect.SendTransactionResponse.Success(
                        id = transfer.requestId.toString(),
                    )
                    UseCases.tonConnectSendTransactionResult(transfer.clientId, success)
                }
                withContext(Dispatchers.Main) {
                    delay(1000L)
                    navigator.onBackPressed()
                }
            } catch (e: Exception) {
                FlowBus.common.dispatch(FlowBusEvent.ShowSnackBar(
                    message = ErrorHandler.getErrorMessage(e),
                    drawable = Res.drawable(R.drawable.ic_warning_32)
                ))
            } finally {
                state.value = state.value.copy(isSending = false)
            }
        }
    }

    private companion object {

        private const val PassCodeEnterPurposeConfirmSend = "confirmSend"
    }
}