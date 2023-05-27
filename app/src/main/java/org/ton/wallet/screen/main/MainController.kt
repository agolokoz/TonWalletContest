package org.ton.wallet.screen.main

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.math.MathUtils.clamp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isInvisible
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import org.ton.wallet.R
import org.ton.wallet.lib.core.Formatter
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.core.ThreadUtils
import org.ton.wallet.lib.core.ext.setOnClickListenerWithLock
import org.ton.wallet.lib.screen.controller.BaseViewModelController
import org.ton.wallet.lib.screen.viewmodel.viewModels
import org.ton.wallet.screen.main.adapter.MainEmptyAdapter
import org.ton.wallet.screen.main.adapter.MainHeaderAdapter
import org.ton.wallet.screen.main.adapter.MainTransactionsAdapter
import org.ton.wallet.screen.main.delegate.MainControllerBinding
import org.ton.wallet.screen.main.delegate.MainStartAnimationDelegate
import org.ton.wallet.screen.settings.SettingsViewModel
import org.ton.wallet.util.NotificationUtils
import pub.devrel.easypermissions.EasyPermissions
import kotlin.math.max
import kotlin.math.roundToInt

class MainController(args: Bundle? = Bundle.EMPTY) : BaseViewModelController<MainViewModel>(args) {

    override val viewModel by viewModels { MainViewModel() }
    override val useBottomInsetsPadding = false
    override val orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

    override val isNavigationBarLight
        get() = !Res.isLandscapeScreenSize

    private val headerAdapter = MainHeaderAdapter(object : MainHeaderAdapter.HeaderItemCallback {
        override fun onReceiveClicked() = viewModel.onReceiveClicked()
        override fun onSendClicked() = viewModel.onSendClicked()
    })

    private val emptyAdapter = MainEmptyAdapter()

    private val transactionsAdapter = MainTransactionsAdapter(object : MainTransactionsAdapter.AdapterCallback {
        override fun onTransactionClicked(transaction: MainTransactionsAdapter.TransactionItem) {
            viewModel.onTransactionClicked(transaction)
        }
    })

    private val concatAdapter = ConcatAdapter(headerAdapter)

    private val bottomSheetDrawable = MainBottomSheetDrawable(Res.context)
    private lateinit var firstOpenAnimationController: MainStartAnimationDelegate

    private var _binding: MainControllerBinding? = null
    private val binding get() = _binding!!

    private var maxBottomSheetOffset = 0f
    private var transactionsState = TransactionsState.Loading

    init {
        val animationOffset = (Res.screenHeight - Res.dimenInt(R.dimen.splash_bottom_sheet_top) - Res.dp(100)) * 0.5f
        bottomSheetDrawable.setAnimationOffset(animationOffset)
        bottomSheetDrawable.setColor(Res.color(R.color.common_white))
        bottomSheetDrawable.setTopRadius(Res.dp(12f))
    }

    override fun onPreCreateView() {
        super.onPreCreateView()
        firstOpenAnimationController = MainStartAnimationDelegate()
    }

    override fun createView(inflater: LayoutInflater, container: ViewGroup, savedViewState: Bundle?): View {
        _binding = MainControllerBinding(container)
        binding.scanButton.setOnClickListenerWithLock(viewModel::onScanClicked)
        binding.settingsButton.setOnClickListenerWithLock(viewModel::onSettingsClicked)
        binding.pullToRefreshLayout.setPullToRefreshListener(viewModel::onRefresh)

        binding.recyclerView.adapter = concatAdapter
        binding.recyclerView.addItemDecoration(recyclerDecoration)
        binding.recyclerView.addOnScrollListener(recyclerScrollChangeListener)
        binding.recyclerView.isNestedScrollingEnabled = false
        binding.recyclerView.itemAnimator = null
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.setHasFixedSize(true)
        return binding.root
    }

    override fun onPostCreateView(view: View) {
        super.onPostCreateView(view)
        if (BitmapForAnimation == null) {
            onStartAnimationFinished()
        } else {
            firstOpenAnimationController.run(binding.root, binding.toolbarLayout, binding.recyclerView, bottomSheetDrawable, ::onStartAnimationFinished)
        }

        viewModel.addressFlow.launchInViewScope(::onAddressChanged)
        viewModel.tonBalanceFlow.launchInViewScope(::onBalanceChanged)
        viewModel.fiatBalanceFlow.launchInViewScope(binding.toolbarFiatBalanceText::setText)
        viewModel.headerStatusFlow.launchInViewScope(::onHeaderStatusChanged)
        viewModel.transactionsFlow.launchInViewScope(::onItemsLoaded)
        viewModel.showNotificationPermissionFlow.launchInViewScope(::showNotificationPermission)
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        binding.recyclerView.scrollToPosition(0)
        recyclerScrollChangeListener.clearOffset()
    }

    override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
        val superInsets = super.onApplyWindowInsets(v, insets)
        val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        maxBottomSheetOffset = getMaxDrawableOffset(insets)
        headerAdapter.height = maxBottomSheetOffset.roundToInt()
        emptyAdapter.height = Res.screenHeight - binding.toolbarLayout.layoutParams.height - headerAdapter.height - systemBarsInsets.top - systemBarsInsets.bottom
        binding.recyclerView.updatePadding(bottom = systemBarsInsets.bottom)
        return superInsets
    }

    override fun onDestroyView(view: View) {
        binding.recyclerView.removeOnScrollListener(recyclerScrollChangeListener)
        firstOpenAnimationController.reset()
        _binding = null
        super.onDestroyView(view)
    }

    private fun onAddressChanged(address: String?) {
        headerAdapter.setAddress(address)
        emptyAdapter.setAddress(address)
    }

    private fun onBalanceChanged(balance: Long?) {
        val balanceString = if (balance == null) null else Formatter.getFormattedAmount(balance)
        binding.toolbarBalanceText.text = balanceString
        headerAdapter.setBalance(Formatter.getBeautifiedAmount(balanceString))
    }

    private fun onStartAnimationFinished() {
        maxBottomSheetOffset = getMaxDrawableOffset(Res.lastInsets)
        bottomSheetDrawable.setBitmap(null)
        bottomSheetDrawable.setTopOffset(maxBottomSheetOffset)
        binding.root.foreground = null
        binding.recyclerView.background = bottomSheetDrawable
    }

    private fun onHeaderStatusChanged(status: MainScreenHeaderStatus) {
        binding.toolbarStatusText.text = when (status) {
            MainScreenHeaderStatus.Connecting -> Res.str(R.string.status_connecting)
            MainScreenHeaderStatus.Updating -> Res.str(R.string.status_updating)
            MainScreenHeaderStatus.WaitingNetwork -> Res.str(R.string.status_waiting_network)
            else -> null
        }
        val isToolbarContentHidden = !binding.toolbarStatusText.text.isNullOrEmpty()
        binding.toolbarAnimationView.isInvisible = isToolbarContentHidden
        binding.toolbarBalanceText.isInvisible = isToolbarContentHidden
        binding.toolbarFiatBalanceText.isInvisible = isToolbarContentHidden
    }

    private fun onItemsLoaded(items: List<Any>?) {
        val newState =
            if (items == null) TransactionsState.Loading
            else if (items.isEmpty()) TransactionsState.Empty
            else TransactionsState.Data

        when (newState) {
            TransactionsState.Loading -> {
                bottomSheetDrawable.showAnimation()
                concatAdapter.removeAdapter(emptyAdapter)
                concatAdapter.removeAdapter(transactionsAdapter)
            }
            TransactionsState.Empty -> {
                bottomSheetDrawable.hideAnimation()
                concatAdapter.removeAdapter(transactionsAdapter)
                concatAdapter.addAdapter(emptyAdapter)
                emptyAdapter.animate(true)
            }
            TransactionsState.Data -> {
                bottomSheetDrawable.hideAnimation()
                concatAdapter.removeAdapter(emptyAdapter)
                concatAdapter.addAdapter(transactionsAdapter)
            }
        }

        transactionsAdapter.setItems(items ?: emptyList(), newState == TransactionsState.Data && transactionsState != newState)
        if (transactionsAdapter.items.isNotEmpty()) {
            ThreadUtils.postOnMain(::updateRecyclerViewBottomOffset, 64)
        }

        transactionsState = newState
    }

    private fun getMaxDrawableOffset(insets: WindowInsetsCompat): Float {
        val systemBarsInets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        return Res.dimen(R.dimen.splash_bottom_sheet_top) - (systemBarsInets.top + (binding.pullToRefreshLayout.layoutParams as MarginLayoutParams).topMargin)
    }

    private fun updateRecyclerViewBottomOffset() {
        val binding = _binding ?: return
        val extent = binding.recyclerView.computeVerticalScrollExtent()
        val range = binding.recyclerView.computeVerticalScrollRange()
        if (extent < range && range < headerAdapter.height + extent) {
            recyclerDecoration.setLastItemBottomOffset(headerAdapter.height + extent - range)
        } else {
            recyclerDecoration.setLastItemBottomOffset(0)
        }
        binding.recyclerView.invalidateItemDecorations()
        binding.recyclerView.requestLayout()
    }

    private fun showNotificationPermission(unit: Unit) {
        val request = NotificationUtils.getPermissionRequest(activity!!, PermissionRequestIdNotifications)
        request?.let { EasyPermissions.requestPermissions(it) }
    }

    private val recyclerScrollChangeListener = object : RecyclerView.OnScrollListener() {

        private var cumulativeOffset = 0

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            if (firstOpenAnimationController.isAnimating) {
                return
            }

            cumulativeOffset += dy

            // bottom sheet
            val topOffset = max(0f, maxBottomSheetOffset - cumulativeOffset)
            bottomSheetDrawable.setTopOffset(topOffset)

            // toolbar
            val toolbarAlpha = clamp((cumulativeOffset.toFloat() * 2f - headerAdapter.height * 0.35f) / headerAdapter.height, 0f, 1f)
            binding.toolbarAnimationView.alpha = toolbarAlpha
            binding.toolbarBalanceText.alpha = toolbarAlpha
            binding.toolbarFiatBalanceText.alpha = toolbarAlpha
            if (toolbarAlpha == 0f) {
                binding.toolbarAnimationView.stopAnimation()
            } else {
                binding.toolbarAnimationView.playAnimation()
            }
        }

        fun clearOffset() {
            cumulativeOffset = 0
        }
    }

    private val recyclerDecoration = object : ItemDecoration() {

        private var lastItemBottom = 0

        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            val position = parent.getChildAdapterPosition(view)
            if (position == (parent.adapter?.itemCount ?: -1) - 1) {
                outRect.bottom = lastItemBottom
            }
        }

        fun setLastItemBottomOffset(offset: Int) {
            lastItemBottom = offset
        }
    }

    companion object {

        var BitmapForAnimation: Bitmap? = null

        private const val PermissionRequestIdNotifications = 0
    }

    private enum class TransactionsState {
        Data,
        Empty,
        Loading;
    }
}