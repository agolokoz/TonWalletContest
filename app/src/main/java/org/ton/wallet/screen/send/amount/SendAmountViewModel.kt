package org.ton.wallet.screen.send.amount

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import org.ton.lib.tonapi.TonCoin
import org.ton.wallet.R
import org.ton.wallet.domain.UseCases
import org.ton.wallet.lib.core.Formatter
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.core.ThreadUtils
import org.ton.wallet.lib.core.ext.addSpans
import org.ton.wallet.lib.navigator.Screen
import org.ton.wallet.lib.navigator.ScreenParams
import org.ton.wallet.lib.screen.viewmodel.BaseViewModel
import org.ton.wallet.screen.send.confirm.SendConfirmController
import org.ton.wallet.uikit.span.FontSpan
import org.ton.wallet.uikit.view.NumPadView
import org.ton.wallet.util.FlowBus
import org.ton.wallet.util.FlowBusEvent
import java.math.BigDecimal

class SendAmountViewModel(args: Bundle) : BaseViewModel(), NumPadView.NumPadViewListener {

    private val address: String = args.getString(SendAmountController.ArgumentKeyAddress)
        ?: throw IllegalArgumentException("${SendAmountController.ArgumentKeyAddress} must be not null")
    private val comment: String? = args.getString(SendAmountController.ArgumentKeyComment)
    private val dns: String? = args.getString(SendAmountController.ArgumentKeyDns)

    private var selectionChangedAfterText = false

    val balanceFlow: Flow<Long?> = UseCases.currentAccountBalanceFlow.flowOn(Dispatchers.IO)

    private val balanceDecimalFlow: StateFlow<BigDecimal?> = balanceFlow.map { balanceLong ->
        if (balanceLong == null) null
        else BigDecimal(balanceLong).movePointLeft(TonCoin.Decimals)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _amountStringFlow = MutableStateFlow<CharSequence>("")
    val amountFlow: Flow<CharSequence> = _amountStringFlow

    private val _amountSelectionFlow = MutableStateFlow(0)
    val amountSelectionFlow: Flow<Int> = _amountSelectionFlow

    private val _isInsufficientFundsFlow = MutableStateFlow(false)
    val isInsufficientFundsFlow: Flow<Boolean> = _isInsufficientFundsFlow

    private val _isSendAllCheckedFlow = MutableStateFlow(false)
    val sendAllCheckedFlow: Flow<Boolean> = _isSendAllCheckedFlow

    val isAddressEditable = args.getBoolean(SendAmountController.ArgumentKeyAddressEditable, true)
    val sendToString: CharSequence

    init {
        val shortAddress = Formatter.getShortAddressSafe(address) ?: ""
        val stringBuilder = SpannableStringBuilder(Res.str(R.string.send_to, shortAddress, dns ?: ""))
        val index = stringBuilder.indexOf(shortAddress)
        if (index >= 0) {
            val spans = listOf(ForegroundColorSpan(Res.color(R.color.text_primary)), FontSpan(Res.font(R.font.inter_regular)))
            stringBuilder.addSpans(spans, index, index + shortAddress.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        }
        sendToString = stringBuilder

        if (args.containsKey(SendAmountController.ArgumentKeyAmount)) {
            val presetAmount = args.getLong(SendAmountController.ArgumentKeyAmount)
            val formattedAmount = Formatter.getFormattedAmount(presetAmount)
            setNewAmount(formattedAmount)
            _amountSelectionFlow.value = formattedAmount.length
        }
    }

    override fun onNumberClicked(number: Int) {
        val selection = _amountSelectionFlow.value
        val newAmount = _amountStringFlow.value.replaceRange(selection, selection, number.toString())
        if (!newAmount.startsWith("00") && _amountStringFlow.value.length < MAX_LENGTH) {
            setNewAmount(newAmount)
            _amountSelectionFlow.value = selection + 1
        }
    }

    override fun onBackSpaceClicked() {
        val selection = _amountSelectionFlow.value
        if (selection > 0) {
            setNewAmount(_amountStringFlow.value.removeRange(selection - 1, selection))
            _amountSelectionFlow.value = selection - 1
        }
    }

    override fun onBackSpaceLongClicked() {
        setNewAmount("")
        _amountSelectionFlow.value = _amountStringFlow.value.length
    }

    override fun onDotClicked() {
        if (!_amountStringFlow.value.contains(Formatter.decimalSeparator)
            && _amountSelectionFlow.value > 0
            && _amountStringFlow.value.length < MAX_LENGTH
        ) {
            val selection = _amountSelectionFlow.value
            setNewAmount(_amountStringFlow.value.replaceRange(selection, selection, Formatter.decimalSeparator.toString()))
            _amountSelectionFlow.value = selection + 1
        }
    }

    fun onEditAddressClicked() {
        navigator.onBackPressed()
    }

    fun onContinueClicked() {
        if (_isInsufficientFundsFlow.value) {
            return
        }
        val amountBigDecimal = _amountStringFlow.value.toString().toBigDecimalOrNull()?.movePointRight(TonCoin.Decimals)
        val minAmount = BigDecimal.ONE
        val maxAmount = BigDecimal.valueOf(Long.MAX_VALUE)
        if (amountBigDecimal != null && minAmount <= amountBigDecimal && amountBigDecimal <= maxAmount) {
            val args = Bundle()
            args.putString(SendConfirmController.ArgumentKeyRecipientAddress, address)
            args.putLong(SendConfirmController.ArgumentKeyAmount, amountBigDecimal.longValueExact())
            comment?.let { args.putString(SendConfirmController.ArgumentKeyComment, it) }
            ThreadUtils.postOnMain { navigator.add(ScreenParams(Screen.SendConfirm, args)) }
        } else {
            FlowBus.common.dispatch(FlowBusEvent.ShowSnackBar(
                Res.str(R.string.error),
                Res.str(R.string.wrong_amount),
                Res.drawable(R.drawable.ic_warning_32)
            ))
        }
    }

    fun onSendAllClicked() {
        _isSendAllCheckedFlow.value = !_isSendAllCheckedFlow.value
        if (_isSendAllCheckedFlow.value) {
            val balanceDecimal = balanceDecimalFlow.value
            if (balanceDecimal != null) {
                setNewAmount(balanceDecimal.toPlainString())
                _amountSelectionFlow.value = _amountStringFlow.value.length
            }
        }
    }

    fun onTextSelectionChanged(start: Int, end: Int) {
        if (!selectionChangedAfterText && start == end) {
            _amountSelectionFlow.tryEmit(start)
        }
        selectionChangedAfterText = false
    }

    private fun setNewAmount(amount: CharSequence?) {
        selectionChangedAfterText = true
        val stringBuilder: SpannableStringBuilder? = Formatter.getBeautifiedAmount(amount)
        if (amount != null && amount.length <= 19 && stringBuilder != null) {
            val amountDecimal = try {
                if (amount.isEmpty()) BigDecimal.ZERO
                else BigDecimal(amount.toString())
            } catch (e: Exception) {
                null
            }
            val balanceDecimal = balanceDecimalFlow.value
            if (amountDecimal != null && balanceDecimal != null) {
                if (amountDecimal != balanceDecimal) {
                    _isSendAllCheckedFlow.value = false
                }
                _isInsufficientFundsFlow.value = amountDecimal > balanceDecimal
                val color = Res.color(if (_isInsufficientFundsFlow.value) R.color.text_error else R.color.text_primary)
                stringBuilder.setSpan(ForegroundColorSpan(color), 0, stringBuilder.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            }
            _amountStringFlow.value = stringBuilder
        }
    }

    private companion object {
        private const val MAX_LENGTH = 19
    }
}