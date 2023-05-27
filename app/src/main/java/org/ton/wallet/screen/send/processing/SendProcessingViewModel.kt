package org.ton.wallet.screen.send.processing

import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.ton.wallet.R
import org.ton.wallet.domain.UseCases
import org.ton.wallet.lib.core.Formatter
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.navigator.Screen
import org.ton.wallet.lib.navigator.ScreenParams
import org.ton.wallet.lib.screen.viewmodel.BaseViewModel
import org.ton.wallet.util.FlowBus
import org.ton.wallet.util.FlowBusEvent
import kotlin.math.abs

class SendProcessingViewModel(args: Bundle) : BaseViewModel() {

    private val message = args.getString(SendProcessingController.ArgumentKeyMessage, "")

    val address = args.getString(SendProcessingController.ArgumentKeyAddress)
        ?: throw IllegalArgumentException("${SendProcessingController.ArgumentKeyAddress} must be not null")

    private val presetFee = args.getLong(SendProcessingController.ArgumentKeyFee)

    private var isPoppedToMain = false

    private val _amountFlow = MutableStateFlow(args.getLong(SendProcessingController.ArgumentKeyAmount))
    val amountTextFlow: Flow<String> = _amountFlow.map { amount ->
        val amountText = Formatter.getFormattedAmount(amount)
        Res.str(R.string.toncoin_have_been_sent, amountText)
    }

    private val _showCompletedFlow = MutableSharedFlow<Unit>(replay = 1)
    val showCompletedFlow: Flow<Unit> = _showCompletedFlow

    init {
        assert(args.containsKey(SendProcessingController.ArgumentKeyAmount)) { "${SendProcessingController.ArgumentKeyAmount} must be not null" }
        assert(args.containsKey(SendProcessingController.ArgumentKeyFee)) { "${SendProcessingController.ArgumentKeyFee} must be not null" }

        launch(Dispatchers.IO) {
            try {
                val actualFee = UseCases.getSendFee(address, _amountFlow.value, message)
                val feeDiff = abs(actualFee - presetFee)
                if (feeDiff.toDouble() / presetFee > 0.01) {
                    FlowBus.common.dispatch(FlowBusEvent.ShowSnackBar(
                        title = Res.str(R.string.fee_changed),
                        message = Res.str(R.string.fee_changed_check),
                        drawable = Res.drawable(R.drawable.ic_warning_32)
                    ))
                    withContext(Dispatchers.Main) {
                        setResult(SendProcessingController.ResultKeyFeeChanged, Bundle.EMPTY)
                        navigator.onBackPressed()
                    }
                    return@launch
                }

                val resultAmount = UseCases.performSend(address, _amountFlow.value, message)
                _amountFlow.value = resultAmount
                onSendCompleted()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    navigator.onBackPressed()
                }
                throw e
            }
        }
    }

    fun onCloseClicked() {
        navigator.onBackPressed()
    }

    fun onViewWalletClicked() {
        popUntilMain()
        navigator.onBackPressed()
    }

    private fun onSendCompleted() {
        _showCompletedFlow.tryEmit(Unit)
        popUntilMain()
    }

    private fun popUntilMain() {
        if (!isPoppedToMain) {
            navigator.popTo(ScreenParams(Screen.Main), true)
        }
        isPoppedToMain = true
    }
}