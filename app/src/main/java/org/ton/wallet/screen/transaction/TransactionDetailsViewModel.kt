package org.ton.wallet.screen.transaction

import android.app.Activity
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.ton.wallet.R
import org.ton.wallet.data.db.transaction.TransactionDto
import org.ton.wallet.data.db.transaction.TransactionStatus
import org.ton.wallet.lib.core.BrowserUtils
import org.ton.wallet.lib.core.Formatter
import org.ton.wallet.lib.core.L
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.core.ext.copyToClipboard
import org.ton.wallet.lib.navigator.Screen
import org.ton.wallet.lib.navigator.ScreenParams
import org.ton.wallet.lib.screen.viewmodel.BaseViewModel
import org.ton.wallet.screen.send.amount.SendAmountController

class TransactionDetailsViewModel(args: Bundle) : BaseViewModel() {

    private val transactionInternalId = args.getLong(TransactionDetailsController.ArgumentKeyTransactionInternalId)
    private var transactionDto: TransactionDto? = null

    private val _stateFlow = MutableStateFlow<TransactionDetailsState?>(null)
    val stateFlow: Flow<TransactionDetailsState?> = _stateFlow

    init {
        launch(Dispatchers.IO) {
            val transaction = try {
                transactions.getTransaction(transactionInternalId)
            } catch (e: Exception) {
                null
            }
            if (transaction == null) {
                L.e(IllegalArgumentException("Could not load transaction with internal id $transactionInternalId"))
                navigator.back()
            } else {
                onTransactionLoaded(transaction)
            }
        }
    }

    fun onButtonClicked() {
        val transaction = transactionDto ?: return
        val peerAddress = transaction.peerAddress ?: return
        val amount = transaction.amount

        val args = Bundle()
        args.putString(SendAmountController.ArgumentKeyAddress, peerAddress)
        args.putBoolean(SendAmountController.ArgumentKeyAddressEditable, false)
        if (transaction.status == TransactionStatus.Cancelled && amount != null) {
            args.putLong(SendAmountController.ArgumentKeyAmount, amount)
        }
        navigator.add(ScreenParams(Screen.SendAmount, args))
    }

    fun onPeerAddressClicked() {
        transactionDto?.peerAddress?.let { address ->
            Res.context.copyToClipboard(address, Res.str(R.string.address_copied_to_clipboard), true)
        }
    }

    fun onHashClicked() {
        transactionDto?.hash?.let { hash ->
            Res.context.copyToClipboard(hash, Res.str(R.string.transaction_hash_copied_to_clipboard), true)
        }
    }

    fun onViewExplorerClicked(activity: Activity) {
        transactionDto?.hash?.let { hash ->
            BrowserUtils.open(activity, getExplorerUrl(hash))
        }
    }

    private fun getExplorerUrl(hash: String): String {
        return "https://tonscan.org/tx/$hash"
    }

    private fun onTransactionLoaded(transaction: TransactionDto) {
        transactionDto = transaction

        var amountSpannable: SpannableStringBuilder? = null
        if (transaction.amount != null) {
            val amountString = Formatter.getFormattedAmount(transaction.amount)
            amountSpannable = Formatter.getBeautifiedAmount(amountString)
            val color =
                if (transaction.amount >= 0) Res.color(R.color.text_approve)
                else Res.color(R.color.text_error)
            amountSpannable?.setSpan(ForegroundColorSpan(color), 0, amountString.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        }

        var feeText: String? = null
        if (transaction.fee != null && transaction.fee > 0L) {
            feeText = Res.str(R.string.fee_transaction, Formatter.getFormattedAmount(transaction.fee))
        }

        var dateText: String? = null
        if (transaction.timestampSec != null) {
            val dateString = Formatter.getFullDateString(transaction.timestampSec * 1000)
            val timeString = Formatter.getTimeString(transaction.timestampSec * 1000)
            dateText = Res.str(R.string.date_at_time, dateString, timeString)
        }

        val buttonText =
            if (transaction.status == TransactionStatus.Cancelled) Res.str(R.string.send_ton_to_address_retry)
            else Res.str(R.string.send_ton_to_address)

        val hashShort =
            if (transaction.status == TransactionStatus.Pending) null
            else Formatter.getBeautifiedShortStringSafe(Formatter.getShortHash(transaction.hash))
        _stateFlow.value = TransactionDetailsState(
            status = transaction.status,
            type = transaction.type,
            amount = amountSpannable,
            fee = feeText,
            date = dateText,
            message = transaction.message,
            peerDns = null,
            peerShortAddress = Formatter.getBeautifiedShortStringSafe(Formatter.getShortAddressSafe(transaction.peerAddress)),
            hashShort = hashShort,
            buttonText = buttonText
        )

        val explorerUrl = getExplorerUrl(transaction.hash)
        BrowserUtils.mayLaunchUrl(explorerUrl)
    }
}