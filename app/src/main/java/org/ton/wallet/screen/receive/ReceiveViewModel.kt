package org.ton.wallet.screen.receive

import android.app.Activity
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import org.ton.lib.qr.QrCodeBuilder
import org.ton.wallet.R
import org.ton.wallet.domain.UseCases
import org.ton.wallet.lib.core.AndroidUtils
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.core.ext.copyToClipboard
import org.ton.wallet.lib.screen.viewmodel.BaseViewModel
import org.ton.wallet.util.LinkUtils

class ReceiveViewModel : BaseViewModel() {

    val addressFlow: StateFlow<String> = UseCases.currentAccountAddressFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private val _qrBitmapFlow = MutableStateFlow<Bitmap?>(null)
    val qrBitmapFlow: Flow<Bitmap?> = _qrBitmapFlow

    init {
        addressFlow.filter { it.isNotEmpty() }
            .take(1)
            .onEach(::generateBitmap)
            .launchIn(viewModelScope)
    }

    fun onAddressClicked() {
        Res.context.copyToClipboard(addressFlow.value, Res.str(R.string.address_copied_to_clipboard))
    }

    fun onShareClicked(activity: Activity) {
        AndroidUtils.shareText(activity, addressFlow.value, Res.str(R.string.share_your_ton_address))
    }

    private fun generateBitmap(address: String) {
        if (address.isEmpty()) {
            return
        }
        launch(Dispatchers.Default) {
            val size = Res.dimenInt(R.dimen.qr_image_size)
            val content = LinkUtils.getTransferLink(address)
            val bitmap = QrCodeBuilder(content, size, size)
                .setBackgroundColor(Res.color(R.color.common_white))
                .setFillColor(Res.color(R.color.common_black))
                .setCutoutDrawable(Res.drawable(R.drawable.ic_gem_large))
                .setWithCutout(true)
                .build()
            _qrBitmapFlow.tryEmit(bitmap)
        }
    }
}