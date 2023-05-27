package org.ton.wallet.screen.send.address

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.ton.wallet.R
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.core.ext.setOnClickListenerWithLock
import org.ton.wallet.lib.lists.BoundSpacesItemDecoration
import org.ton.wallet.lib.screen.controller.BaseViewModelBottomSheetController
import org.ton.wallet.lib.screen.viewmodel.viewModels
import org.ton.wallet.uikit.drawable.EmptyDrawable
import org.ton.wallet.uikit.drawable.IndeterminateProgressDrawable
import org.ton.wallet.uikit.view.AppEditText
import org.ton.wallet.util.ViewUtils

class SendAddressController(args: Bundle?) : BaseViewModelBottomSheetController<SendAddressViewModel>(args) {

    override val viewModel by viewModels { SendAddressViewModel(args) }

    override val useBottomInsetsPadding = false
    override val isFullHeight = true

    private val drawableSize = Res.dp(20)
    private val emptyDrawable = EmptyDrawable(drawableSize, drawableSize)
    private val loadingDrawable = IndeterminateProgressDrawable(drawableSize)

    private val recentAdapter = RecentAddressesAdapter { item, _ ->
        viewModel.onRecentItemClicked(item)
    }

    private lateinit var contentLayout: ViewGroup
    private lateinit var editText: AppEditText
    private lateinit var sendButton: TextView
    private lateinit var recentTitleText: TextView
    private lateinit var recentRecyclerView: RecyclerView

    init {
        loadingDrawable.setColor(Res.color(R.color.common_white))
        loadingDrawable.setStrokeWidth(Res.dp(2f))
    }

    override fun createBottomSheetView(inflater: LayoutInflater, container: ViewGroup?, savedViewState: Bundle?): View {
        val view = inflater.inflate(R.layout.screen_send_address, container, false)
        view.findViewById<View>(R.id.sendAddressPasteButton).setOnClickListenerWithLock(viewModel::onPasteClicked)
        view.findViewById<View>(R.id.sendAddressScanButton).setOnClickListenerWithLock(viewModel::onScanClicked)
        ViewUtils.connectAppToolbarWithScrollableView(view.findViewById(R.id.sendAddressToolbar), view.findViewById(R.id.sendAddressScrollView))

        contentLayout = view.findViewById(R.id.sendAddressContentLayout)
        editText = view.findViewById(R.id.sendAddressEditText)
        var isFirstEditTextChange = true
        editText.addTextChangedListener { editable ->
            if (!isFirstEditTextChange) {
                viewModel.onTextChanged(editable?.toString() ?: "")
            }
            isFirstEditTextChange = false
        }

        sendButton = view.findViewById(R.id.sendAddressButton)
        sendButton.setCompoundDrawablesWithIntrinsicBounds(emptyDrawable, null, emptyDrawable, null)
        sendButton.setOnClickListenerWithLock { viewModel.onContinueClicked(activity!!) }

        recentTitleText = view.findViewById(R.id.sendAddressRecentTitleText)
        recentRecyclerView = view.findViewById(R.id.sendAddressHistoryRecyclerView)
        recentRecyclerView.adapter = recentAdapter
        recentRecyclerView.addItemDecoration(BoundSpacesItemDecoration(Res.dp(6), Res.dp(6)))
        recentRecyclerView.layoutManager = LinearLayoutManager(context)

        return view
    }

    override fun onPostCreateView(view: View) {
        super.onPostCreateView(view)
        viewModel.textFlow.launchInViewScope(::setAmountText)
        viewModel.isLoadingFlow.launchInViewScope(::setLoading)
        viewModel.recentAddressesFlow.launchInViewScope(::setRecentItems)
    }

    private fun setAmountText(text: String?) {
        if (editText.text.toString() != text) {
            editText.setText(text)
            editText.setSelection(text?.length ?: 0)
        }
    }

    private fun setLoading(isLoading: Boolean) {
        val rightDrawable =
            if (isLoading) loadingDrawable
            else emptyDrawable
        sendButton.setCompoundDrawablesWithIntrinsicBounds(emptyDrawable, null, rightDrawable, null)
    }

    private fun setRecentItems(recentAddressesList: List<RecentAddressItem>) {
        recentTitleText.isVisible = recentAddressesList.isNotEmpty()
        recentRecyclerView.isVisible = recentAddressesList.isNotEmpty()
        recentAdapter.setItems(recentAddressesList)
    }


    companion object {

        const val ArgumentKeyAddress = "address"
        const val ArgumentKeyAmount = "amount"
        const val ArgumentKeyComment = "comment"
    }
}