package org.ton.wallet.screen.main

import android.os.Bundle
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import androidx.core.os.bundleOf
import com.bluelinelabs.conductor.ControllerChangeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.ton.lib.tonapi.TonCoin
import org.ton.wallet.AppComponentsProvider
import org.ton.wallet.R
import org.ton.wallet.data.db.transaction.TransactionDto
import org.ton.wallet.data.model.TonConnectEvent
import org.ton.wallet.domain.UseCases
import org.ton.wallet.lib.core.Formatter
import org.ton.wallet.lib.core.L
import org.ton.wallet.lib.core.NetworkUtils
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.navigator.Screen
import org.ton.wallet.lib.navigator.ScreenParams
import org.ton.wallet.lib.screen.changehandler.SlideChangeHandler
import org.ton.wallet.lib.screen.viewmodel.BaseViewModel
import org.ton.wallet.screen.main.adapter.MainTransactionsAdapter
import org.ton.wallet.screen.scanqr.ScanQrController
import org.ton.wallet.screen.send.connect.SendConnectConfirmController
import org.ton.wallet.screen.transaction.TransactionDetailsController
import org.ton.wallet.util.*
import java.math.BigDecimal
import java.util.*

class MainViewModel : BaseViewModel() {

    private val calendar by lazy { Calendar.getInstance() }
    private val colorPositive by lazy { Res.color(R.color.text_approve) }
    private val colorNegative by lazy { Res.color(R.color.text_error) }

    private val isAccountStateRefreshing = MutableStateFlow(false)

    private var refreshAccountJob: Job? = null
    private var qrCodeProcessJob: Job? = null
    private var qrCodeProcessed = false
    private var linkAction: LinkAction? = null

    val addressFlow: Flow<String> = UseCases.currentAccountAddressFlow.flowOn(Dispatchers.IO)
    val tonBalanceFlow: Flow<Long?> = UseCases.currentAccountBalanceFlow.flowOn(Dispatchers.IO)

    private val tonFiatPriceFlow: Flow<Double> = settings.fiatCurrencyFlow
        .flatMapLatest(AppComponentsProvider.pricesRepository::getFiatPriceFlow)
        .flowOn(Dispatchers.IO)
    val fiatBalanceFlow: Flow<String?> = combine(
        tonBalanceFlow,
        tonFiatPriceFlow,
        settings.fiatCurrencyFlow
    ) { balance, fiatPrice, fiatCurrency ->
        var fiatBalance = BigDecimal(balance ?: 0)
        fiatBalance = fiatBalance.movePointLeft(TonCoin.Decimals)
        fiatBalance = fiatBalance.multiply(BigDecimal.valueOf(fiatPrice))
        val formattedAmount = Formatter.getFormattedAmount(fiatBalance, fiatCurrency.currencySymbol)
        "≈ $formattedAmount"
    }.flowOn(Dispatchers.IO)

    val headerStatusFlow = combine(NetworkUtils.stateFlow, isAccountStateRefreshing) { networkState, isRefreshing ->
        mapHeaderStatus(networkState.isAvailable, isRefreshing)
    }.flowOn(Dispatchers.IO)

    private val _transactionsFlow = MutableStateFlow<List<Any>?>(null)
    val transactionsFlow: Flow<List<Any>?> = _transactionsFlow

    private val _showNotificationPermissionFlow = MutableSharedFlow<Unit>(replay = 1)
    val showNotificationPermissionFlow: Flow<Unit> = _showNotificationPermissionFlow

    init {
        NetworkUtils.stateFlow
            .drop(1)
            .onEach { networkState ->
                if (networkState.isAvailable) {
                    refreshAccount()
                }
            }
            .launchIn(viewModelScope + Dispatchers.IO)

        settings.accountTypeFlow
            .drop(1)
            .onEach {
                _transactionsFlow.tryEmit(null)
                loadCachedTransactions()
                refreshAccount()
            }
            .launchIn(viewModelScope + Dispatchers.IO)

        transactions.transactionsAddedFlow
            .filterNotNull()
            .onEach { loadCachedTransactions() }
            .launchIn(viewModelScope + Dispatchers.IO)

        FlowBus.common.eventsFlow
            .filterIsInstance<FlowBusEvent.AccountReloaded>()
            .onEach { loadCachedTransactions() }
            .launchIn(viewModelScope + Dispatchers.IO)

        launch(Dispatchers.IO) {
            loadCachedTransactions()    // first load data from cache
            refreshAccount()            // then update from api
        }

        launch(Dispatchers.Main) {
            if (!NotificationUtils.isNotificationsPermissionGrantedFlow.value && !settings.isNotificationPermissionDialogShown) {
                _showNotificationPermissionFlow.emit(Unit)
                settings.setNotificationsDialogShown(true)
            }
        }
    }

    override fun onScreenChange(isStarted: Boolean, changeType: ControllerChangeType) {
        super.onScreenChange(isStarted, changeType)
        if (!isStarted && changeType.isEnter) {
            qrCodeProcessed = false
        }
        if (!isStarted && changeType.isEnter && !changeType.isPush) {
            linkAction?.let(actionHandler::processDeepLinkAction)
            linkAction = null
        }
    }

    override fun onResult(code: String, data: Bundle) {
        super.onResult(code, data)
        if (code == ScanQrController.ResultCodeQrDetected) {
            processQrCode(data.getString(ScanQrController.ArgumentKeyQrValue))
        }
    }

    fun onScanClicked() {
        navigator.add(ScreenParams(Screen.ScanQr, SlideChangeHandler(), SlideChangeHandler()))
    }

    fun onSettingsClicked() {
        navigator.add(ScreenParams(Screen.Settings, SlideChangeHandler(), SlideChangeHandler()))
    }

    fun onSendClicked() {
        navigator.add(ScreenParams(Screen.SendAddress))
    }

    fun onReceiveClicked() {
        navigator.add(ScreenParams(Screen.Receive))
    }

    fun onTransactionClicked(item: MainTransactionsAdapter.TransactionItem) {
        val args = Bundle()
        args.putLong(TransactionDetailsController.ArgumentKeyTransactionInternalId, item.internalId)
        navigator.add(ScreenParams(Screen.TransactionDetails, args))
    }

    fun onRefresh() {
        refreshAccount()
    }

    private suspend fun loadCachedTransactions() {
        val trxList = UseCases.getTransactions(false)
        _transactionsFlow.tryEmit(mapTransactions(trxList))
    }

    private fun refreshAccount() {
        if (refreshAccountJob?.isActive == true) {
            return
        }
        isAccountStateRefreshing.tryEmit(true)
        refreshAccountJob = launch(Dispatchers.IO) {
            try {
                val trxList = UseCases.getTransactions(true)
                _transactionsFlow.tryEmit(mapTransactions(trxList))
            } finally {
                isAccountStateRefreshing.tryEmit(false)
            }
        }
    }

    private fun processQrCode(code: String?) {
        L.d("processQrCode: $code")
        if (qrCodeProcessJob?.isActive == true || qrCodeProcessed) {
            return
        }
        qrCodeProcessJob = viewModelScope.launch(Dispatchers.Default) {
            linkAction = LinkUtils.parseLink(code ?: "")
            if (linkAction != null) {
                qrCodeProcessed = true
                navigator.back()
            }
        }
    }

    private fun mapHeaderStatus(isNetworkAvailable: Boolean, isRefreshing: Boolean): MainScreenHeaderStatus {
        return if (isNetworkAvailable) {
            if (isRefreshing) {
                MainScreenHeaderStatus.Updating
            } else {
                MainScreenHeaderStatus.Default
            }
        } else {
            MainScreenHeaderStatus.WaitingNetwork
        }
    }

    private fun mapTransactions(transactions: List<TransactionDto>?): List<Any>? {
        if (transactions == null) {
            return null
        }
        val items = mutableListOf<Any>()
        for (i in transactions.indices) {
            val currentDto = transactions[i]
            val dtoTimestampMs = currentDto.timestampSec?.times(1000L) ?: 0L

            var isNeedDateItem: Boolean
            if (i == 0) {
                isNeedDateItem = true
            } else {
                val previousDtoTimestampMs = transactions[i - 1].timestampSec?.times(1000L) ?: 0L
                calendar.timeInMillis = previousDtoTimestampMs
                val previousDay = calendar.get(Calendar.DAY_OF_YEAR)
                val previousYear = calendar.get(Calendar.YEAR)

                calendar.timeInMillis = dtoTimestampMs
                val currentDay = calendar.get(Calendar.DAY_OF_YEAR)
                val currentYear = calendar.get(Calendar.YEAR)
                isNeedDateItem = previousDay != currentDay || previousYear != currentYear
            }

            if (isNeedDateItem) {
                val dateString = Formatter.getDayMonthString(dtoTimestampMs)
                val dateItem = MainTransactionsAdapter.HeaderItem(dateString)
                items.add(dateItem)
            }
            items.add(mapDtoToItem(currentDto))
        }
        return items
    }

    private fun mapDtoToItem(dto: TransactionDto): MainTransactionsAdapter.TransactionItem {
        var valueCharSequence: CharSequence? = null
        val amount = dto.amount
        if (amount != null && amount != 0L) {
            val valueString = Formatter.getFormattedAmount(amount)
            val valueBuilder = Formatter.getBeautifiedAmount(valueString, proportion = 0.77f)
            val color = if (amount > 0) colorPositive else colorNegative
            valueBuilder?.setSpan(ForegroundColorSpan(color), 0, valueBuilder.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
            valueCharSequence = valueBuilder
        }

        val peerAddressSequence = Formatter.getBeautifiedShortStringSafe(Formatter.getMiddleAddress(dto.peerAddress))

        var feeString: String? = null
        val storageFee = dto.storageFee
        if (storageFee != null && storageFee > 0L) {
            var feeDecimal = BigDecimal(storageFee)
            feeDecimal = feeDecimal.movePointLeft(TonCoin.Decimals).stripTrailingZeros()
            feeString = Res.str(R.string.fee_storage, feeDecimal.toPlainString())
        }

        val timestampSec = dto.timestampSec
        var timeString: String? = null
        if (timestampSec != null && timestampSec != 0L) {
            timeString = Formatter.getTimeString(timestampSec * 1000)
        }

        return MainTransactionsAdapter.TransactionItem(
            internalId = dto.internalId,
            type = dto.type,
            value = valueCharSequence,
            peerAddressShort = peerAddressSequence,
            timeString = timeString,
            feeString = feeString,
            messageText = dto.message
        )
    }
}