package org.ton.wallet.screen.send.confirm

import android.app.Activity
import android.os.Bundle
import com.bluelinelabs.conductor.ControllerChangeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import org.ton.wallet.R
import org.ton.wallet.data.ton.TonApiException
import org.ton.wallet.domain.UseCases
import org.ton.wallet.lib.core.Formatter
import org.ton.wallet.lib.core.KeyboardUtils
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.navigator.Screen
import org.ton.wallet.lib.navigator.ScreenParams
import org.ton.wallet.lib.screen.changehandler.SlideChangeHandlerNoRemoveView
import org.ton.wallet.lib.screen.controller.ResultListener
import org.ton.wallet.lib.screen.viewmodel.BaseViewModel
import org.ton.wallet.screen.pass.base.BasePassCodeController
import org.ton.wallet.screen.pass.enter.PassCodeEnterController
import org.ton.wallet.screen.send.amount.SendAmountController
import org.ton.wallet.screen.send.processing.SendProcessingController
import org.ton.wallet.util.FlowBus
import org.ton.wallet.util.FlowBusEvent

class SendConfirmViewModel(args: Bundle) : BaseViewModel(), ResultListener {

    private val recipientAddress = args.getString(SendConfirmController.ArgumentKeyRecipientAddress)
        ?: throw IllegalArgumentException("${SendAmountController.ArgumentKeyAddress} must be not null")

    private val sourceAmount = args.getLong(SendConfirmController.ArgumentKeyAmount)
    private var calculateFeeJob: Job? = null
    private var passCodeEntered = false

    private val _amountFlow = MutableStateFlow(sourceAmount)
    private val _isFeeLoadingFlow = MutableStateFlow(false)
    private val _feeFlow = MutableStateFlow(-1L)
    private val _messageFlow = MutableStateFlow("")

    val amountStringFlow: Flow<String> = _amountFlow.map(Formatter::getFormattedAmount)

    val feeStateFlow: Flow<SendConfirmFeeState> = combine(_feeFlow, _isFeeLoadingFlow) { fee, isLoading ->
        if (isLoading) {
            SendConfirmFeeState.Loading
        } else if (fee < 0) {
            SendConfirmFeeState.Error
        } else {
            SendConfirmFeeState.Value(Formatter.getFormattedAmount(fee))
        }
    }

    val messageLeftSymbolsFlow: StateFlow<Int> = _messageFlow.map { MessageMaxLength - it.length }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MessageMaxLength)

    val recipient = Formatter.getBeautifiedShortString(Formatter.getShortAddress(recipientAddress))

    val presetMessage: String? = args.getString(SendConfirmController.ArgumentKeyComment)

    init {
        _messageFlow.debounce(300L)
            .drop(1)
            .onEach(::calculateFee)
            .launchIn(viewModelScope)
        calculateFee(_messageFlow.value)
    }

    override fun onResult(code: String, data: Bundle) {
        super.onResult(code, data)
        if (code == PassCodeEnterController.ResultKeyCorrectPassCodeEntered) {
            val purpose = data.getString(PassCodeEnterController.ArgumentKeyPurpose)
            if (purpose == PassCodeEnterPurposeConfirmSend) {
                passCodeEntered = true
            }
        } else if (code == SendProcessingController.ResultKeyFeeChanged) {
            calculateFee(_messageFlow.value)
        }
    }

    override fun onScreenChange(isStarted: Boolean, changeType: ControllerChangeType) {
        super.onScreenChange(isStarted, changeType)
        if (!isStarted && !changeType.isPush && changeType.isEnter && passCodeEntered) {
            val args = Bundle()
            args.putLong(SendProcessingController.ArgumentKeyAmount, _amountFlow.value)
            args.putLong(SendProcessingController.ArgumentKeyFee, _feeFlow.value)
            args.putString(SendProcessingController.ArgumentKeyAddress, recipientAddress)
            args.putString(SendProcessingController.ArgumentKeyMessage, _messageFlow.value)
            navigator.add(ScreenParams(Screen.SendProcessing, args))
            passCodeEntered = false
        }
    }

    fun onTextChanged(text: String) {
        _messageFlow.value = text
    }

    fun onConfirmClicked(activity: Activity) {
        if (_feeFlow.value < 0 || messageLeftSymbolsFlow.value < 0) {
            return
        }
        passCodeEntered = false
        val args = Bundle()
        args.putBoolean(BasePassCodeController.ArgumentKeyBackVisible, false)
        args.putBoolean(BasePassCodeController.ArgumentKeyDark, false)
        args.putBoolean(PassCodeEnterController.ArgumentKeyPopOnSuccess, true)
        args.putString(PassCodeEnterController.ArgumentKeyPurpose, PassCodeEnterPurposeConfirmSend)
        KeyboardUtils.hideKeyboard(activity.window) {
            navigator.add(ScreenParams(Screen.PassCodeEnter, args, SlideChangeHandlerNoRemoveView(), SlideChangeHandlerNoRemoveView(), this))
        }
    }

    private fun calculateFee(message: String) {
        calculateFeeJob?.cancel()
        _isFeeLoadingFlow.tryEmit(true)
        calculateFeeJob = launch(Dispatchers.IO) {
            val amount = _amountFlow.value
            try {
                val fee = UseCases.getSendFee(recipientAddress, amount, message)
                _feeFlow.emit(fee)
            } catch (e: TonApiException) {
                if (e.error.message == "NOT_ENOUGH_FUNDS") {
                    var zeroAmountFee: Long? = null
                    try {
                        zeroAmountFee = UseCases.getSendFee(recipientAddress, 0, message)
                    } catch (e: TonApiException) {
                        if (e.error.message == "NOT_ENOUGH_FUNDS") {
                            showNotEnoughFundsError()
                            _feeFlow.emit(-1)
                        } else {
                            throw e
                        }
                    }

                    if (zeroAmountFee != null) {
                        val decreasedAmount = amount - zeroAmountFee
                        try {
                            val decreasedAmountFee = UseCases.getSendFee(recipientAddress, decreasedAmount, message)
                            _feeFlow.emit(decreasedAmountFee)
                            _amountFlow.emit(decreasedAmount)
                            FlowBus.common.dispatch(FlowBusEvent.ShowSnackBar(
                                message = Res.str(R.string.decreased_amount),
                                drawable = Res.drawable(R.drawable.ic_warning_32),
                            ))
                        } catch (e: TonApiException) {
                            if (e.error.message == "NOT_ENOUGH_FUNDS") {
                                showNotEnoughFundsError()
                                _feeFlow.emit(-1)
                            } else {
                                throw e
                            }
                        }
                    }
                } else {
                    throw e
                }
            } finally {
                _isFeeLoadingFlow.emit(false)
            }
        }
    }

    private fun showNotEnoughFundsError() {
        FlowBus.common.dispatch(FlowBusEvent.ShowSnackBar(
            message = Res.str(R.string.not_enough_funds),
            drawable = Res.drawable(R.drawable.ic_warning_32),
            durationMs = 5000L
        ))
    }

    private companion object {
        private const val MessageMaxLength = 122
        private const val PassCodeEnterPurposeConfirmSend = "confirmSend"
    }
}