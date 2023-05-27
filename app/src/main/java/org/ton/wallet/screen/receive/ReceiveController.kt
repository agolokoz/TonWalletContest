package org.ton.wallet.screen.receive

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.ton.wallet.R
import org.ton.wallet.lib.core.ext.setOnClickListenerWithLock
import org.ton.wallet.lib.screen.controller.BaseViewModelBottomSheetController
import org.ton.wallet.lib.screen.viewmodel.viewModels
import org.ton.wallet.util.ViewUtils

class ReceiveController(args: Bundle?) : BaseViewModelBottomSheetController<ReceiveViewModel>(args) {

    override val viewModel by viewModels { ReceiveViewModel() }

    private lateinit var qrImageView: ImageView
    private lateinit var addressText: TextView

    override fun createBottomSheetView(inflater: LayoutInflater, container: ViewGroup?, savedViewState: Bundle?): View {
        val view = inflater.inflate(R.layout.screen_receive, container, false)
        ViewUtils.connectAppToolbarWithScrollableView(view.findViewById(R.id.receiveToolbar), view.findViewById(R.id.receiveScrollView))
        view.findViewById<View>(R.id.receiveShareButton).setOnClickListenerWithLock { viewModel.onShareClicked(activity!!) }

        addressText = view.findViewById(R.id.receiveAddressText)
        addressText.setOnClickListenerWithLock(viewModel::onAddressClicked)

        qrImageView = view.findViewById(R.id.receiveQrImage)

        return view
    }

    override fun onPostCreateView(view: View) {
        super.onPostCreateView(view)
        viewModel.addressFlow.launchInViewScope(::setAddress)
        viewModel.qrBitmapFlow.launchInViewScope { bitmap ->
            qrImageView.setImageBitmap(bitmap)
        }
    }

    private fun setAddress(address: String) {
        addressText.text = address.replaceRange(address.length / 2, address.length / 2, "\n")
    }
}