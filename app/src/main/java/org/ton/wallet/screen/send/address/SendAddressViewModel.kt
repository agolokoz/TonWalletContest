package org.ton.wallet.screen.send.address

import android.app.Activity
import android.content.ClipDescription
import android.content.ClipboardManager
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ton.wallet.AppComponentsProvider
import org.ton.wallet.R
import org.ton.wallet.data.model.AddressType
import org.ton.wallet.domain.UseCases
import org.ton.wallet.lib.core.*
import org.ton.wallet.lib.navigator.Screen
import org.ton.wallet.lib.navigator.ScreenParams
import org.ton.wallet.lib.screen.changehandler.SlideChangeHandler
import org.ton.wallet.lib.screen.viewmodel.BaseViewModel
import org.ton.wallet.screen.scanqr.ScanQrController
import org.ton.wallet.screen.send.amount.SendAmountController
import org.ton.wallet.util.FlowBus
import org.ton.wallet.util.FlowBusEvent
import org.ton.wallet.util.LinkAction
import org.ton.wallet.util.LinkUtils

class SendAddressViewModel(args: Bundle?) : BaseViewModel() {

    private val getAddressTypeUseCase = AppComponentsProvider.getAddressTypeUseCase

    private val amount: Long? =
        if (args?.containsKey(SendAddressController.ArgumentKeyAmount) == true) {
            args.getLong(SendAddressController.ArgumentKeyAmount)
        } else {
            null
        }

    private val comment: String? = args?.getString(SendAddressController.ArgumentKeyComment, null)

    private var qrCodeProcessJob: Job? = null
    private var qrCodeProcessed = false

    private val _addressTextFlow = MutableStateFlow<String?>(null)
    val textFlow: Flow<String?> = _addressTextFlow

    private val _isLoadingFlow = MutableStateFlow(false)
    val isLoadingFlow: Flow<Boolean> = _isLoadingFlow

    private val _recentAddressesFlow = MutableStateFlow(emptyList<RecentAddressItem>())
    val recentAddressesFlow: Flow<List<RecentAddressItem>> = _recentAddressesFlow

    init {
        val address = args?.getString(SendAddressController.ArgumentKeyAddress)
        if (!address.isNullOrEmpty()) {
            _addressTextFlow.tryEmit(address)
        }

        viewModelScope.launch(Dispatchers.IO) {
            val recent = UseCases.getRecentSendTransactions().map { dto ->
                RecentAddressItem(
                    address = dto.address,
                    shortAddress = Formatter.getShortAddress(dto.address),
                    dateString = Formatter.getDayMonthString(dto.timestampSec * 1000L)
                )
            }
            _recentAddressesFlow.tryEmit(recent)
        }
    }

    override fun onResult(code: String, data: Bundle) {
        super.onResult(code, data)
        if (code == ScanQrController.ResultCodeQrDetected) {
            processQrCode(data.getString(ScanQrController.ArgumentKeyQrValue))
        }
    }

    fun onTextChanged(text: String) {
        _addressTextFlow.tryEmit(text)
    }

    fun onPasteClicked() {
        val text = getClipboardText()
        if (text != null) {
            _addressTextFlow.tryEmit(text)
        }
    }

    fun onScanClicked() {
        qrCodeProcessed = false
        navigator.add(ScreenParams(Screen.ScanQr, SlideChangeHandler(), SlideChangeHandler()))
    }

    fun onContinueClicked(activity: Activity) {
        if (_isLoadingFlow.value) {
            return
        }
        _isLoadingFlow.tryEmit(true)
        launch(Dispatchers.IO) {
            try {
                val addressType = getAddressTypeUseCase.getAddressType(_addressTextFlow.value ?: "")
                if (addressType == null) {
                    val snackBar = FlowBusEvent.ShowSnackBar(
                        title = Res.str(R.string.invalid_address),
                        message = Res.str(R.string.invalid_address_description),
                        drawable = Res.drawable(R.drawable.ic_warning_32)
                    )
                    FlowBus.common.dispatch(snackBar)
                } else {
                    withContext(Dispatchers.Main) {
                        val args = Bundle()
                        args.putString(SendAmountController.ArgumentKeyAddress, addressType.ufAddress)
                        if (addressType is AddressType.DnsAddress) {
                            args.putString(SendAmountController.ArgumentKeyDns, addressType.dns)
                        }
                        amount?.let { args.putLong(SendAmountController.ArgumentKeyAmount, it) }
                        comment?.let { args.putString(SendAmountController.ArgumentKeyComment, it) }
                        KeyboardUtils.hideKeyboard(activity.window) {
                            ThreadUtils.postOnMain { navigator.add(ScreenParams(Screen.SendAmount, args)) }
                        }
                    }
                }
            } finally {
                _isLoadingFlow.tryEmit(false)
            }
        }
    }

    fun onRecentItemClicked(item: RecentAddressItem) {
        _addressTextFlow.tryEmit(item.address)
    }

    private fun processQrCode(code: String?) {
        if (qrCodeProcessJob?.isActive == true || qrCodeProcessed) {
            return
        }
        qrCodeProcessJob = viewModelScope.launch(Dispatchers.Default) {
            val linkAction = LinkUtils.parseLink(code ?: "")
            if (linkAction != null && linkAction is LinkAction.TransferAction) {
                _addressTextFlow.tryEmit(linkAction.address)
                navigator.back()
                qrCodeProcessed = true
            }
        }
    }

    private fun getClipboardText(): String? {
        val clipboardManager = Res.context.getSystemService(ClipboardManager::class.java)
        val primaryClip = clipboardManager.primaryClip ?: return null
        val isClipboardContainsText = primaryClip.description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                || primaryClip.description.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)
        if (!isClipboardContainsText) {
            return null
        }
        return primaryClip.getItemAt(0)?.text.toString()
    }
}