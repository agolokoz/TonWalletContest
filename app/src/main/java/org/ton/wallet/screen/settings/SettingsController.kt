package org.ton.wallet.screen.settings

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.math.MathUtils.clamp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.ton.lib.tonapi.TonAccountType
import org.ton.wallet.R
import org.ton.wallet.common.*
import org.ton.wallet.data.model.FiatCurrency
import org.ton.wallet.lib.core.Formatter
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.core.SecurityUtils
import org.ton.wallet.lib.screen.controller.BaseViewModelController
import org.ton.wallet.lib.screen.viewmodel.viewModels
import org.ton.wallet.uikit.drawable.TopRoundRectDrawable
import org.ton.wallet.uikit.popup.BasePopupWindow
import org.ton.wallet.uikit.popup.MenuPopupWindow
import org.ton.wallet.uikit.view.AppToolbar

class SettingsController(args: Bundle?) : BaseViewModelController<SettingsViewModel>(args) {

    override val viewModel by viewModels { SettingsViewModel() }
    override val isNavigationBarLight get() = !Res.isLandscapeScreenSize
    override val useBottomInsetsPadding = false

    private val adapterCallback = object : CommonAdapter.CommonAdapterCallback {

        override fun onTextItemClicked(item: CommonTextItem) {
            super.onTextItemClicked(item)
            when (item.id) {
                ItemAddress -> onAddressClicked()
                ItemCurrency -> onFiatCurrencyClicked()
                ItemRecoveryPhrase -> viewModel.onShowRecoveryClicked()
                ItemChangePasscode -> viewModel.onChangePasscodeClicked()
                ItemDeleteWallet -> viewModel.onDeleteWalletClicked(activity!!)
            }
        }

        override fun onSwitchItemClicked(item: CommonSwitchItem) {
            super.onSwitchItemClicked(item)
            when (item.id) {
                ItemNotifications -> viewModel.onNotificationsClicked(activity!!, item.isChecked)
                ItemBiometricAuth -> viewModel.onBiometricAuthClicked(item.isChecked)
            }
        }
    }

    private val adapter = CommonAdapter(adapterCallback)
    private val bottomSheetDrawable = TopRoundRectDrawable(Res.screenHeight)

    private lateinit var toolbar: AppToolbar
    private lateinit var recyclerView: RecyclerView

    private var initialDrawableOffset = 0f
    private var popupWindow: BasePopupWindow? = null

    init {
        val items = mutableListOf(
            CommonHeaderItem(Res.str(R.string.general)),
            CommonSwitchItem(ItemNotifications, Res.str(R.string.notifications), viewModel.isNotificationsOnFlow.value),
            CommonTextItem(ItemAddress, Res.str(R.string.active_address), viewModel.accountTypeFlow.value.getString()),
            CommonTextItem(ItemCurrency, Res.str(R.string.primary_currency), viewModel.fiatCurrencyFlow.value.name.uppercase()),
            CommonHeaderItem(Res.str(R.string.security)),
            CommonTextItem(ItemRecoveryPhrase, Res.str(R.string.show_recovery_phrase)),
            CommonTextItem(ItemChangePasscode, Res.str(R.string.change_passcode)),
        )
        if (SecurityUtils.isBiometricsAvailableOnDevice(Res.context)) {
            items.add(CommonSwitchItem(ItemBiometricAuth, Res.str(R.string.biometric_auth), viewModel.isBiometricOnFlow.value))
        }
        items.add(CommonTextItem(ItemDeleteWallet, Res.str(R.string.delete_wallet), titleColor = Res.color(R.color.text_error)))
        adapter.setItems(items)
    }

    override fun createView(inflater: LayoutInflater, container: ViewGroup, savedViewState: Bundle?): View {
        val view = inflater.inflate(R.layout.screen_settings, container, false)

        toolbar = view.findViewById(R.id.settingsToolbar)
        toolbar.setShadowAlpha(0f)

        initialDrawableOffset = Res.dimenAttr(android.R.attr.actionBarSize).toFloat()
        bottomSheetDrawable.setColor(Res.color(R.color.common_white))
        bottomSheetDrawable.setTopRadius(Res.dimen(R.dimen.bottom_sheet_radius))
        bottomSheetDrawable.setTopOffset(initialDrawableOffset)
        view.findViewById<View>(R.id.settingsBackgroundView).background = bottomSheetDrawable

        recyclerView = view.findViewById(R.id.settingsRecyclerView)
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(CommonItemDecoration())
        recyclerView.addOnScrollListener(scrollListener)
        recyclerView.itemAnimator = null
        recyclerView.isNestedScrollingEnabled = false
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setHasFixedSize(true)

        return view
    }

    override fun onPostCreateView(view: View) {
        super.onPostCreateView(view)
        viewModel.setActivity(activity as FragmentActivity)
        viewModel.isNotificationsOnFlow.launchInViewScope(::onNotificationsChanged)
        viewModel.isBiometricOnFlow.launchInViewScope(::onBiometricChanged)
        viewModel.accountTypeFlow.launchInViewScope(::onAccountTypeChanged)
        viewModel.fiatCurrencyFlow.launchInViewScope(::onFiatCurrencyChanged)
    }

    override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
        val superInsets = super.onApplyWindowInsets(v, insets)
        val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        recyclerView.updatePadding(bottom = systemBarsInsets.bottom)
        return superInsets
    }

    override fun onDestroyView(view: View) {
        recyclerView.removeOnScrollListener(scrollListener)
        super.onDestroyView(view)
    }

    // flow changes
    private fun onNotificationsChanged(isTurnedOn: Boolean) {
        setSwitchItemOn(ItemNotifications, isTurnedOn)
    }

    private fun onBiometricChanged(isTurnedOn: Boolean) {
        setSwitchItemOn(ItemBiometricAuth, isTurnedOn)
    }

    private fun onAccountTypeChanged(type: TonAccountType) {
        val position = adapter.items.indexOfFirst { it is CommonTextItem && it.id == ItemAddress }
        if (position >= 0) {
            adapter.notifyItemChanged(position, CommonTextValueChangePayload(type.getString()))
        }
    }

    private fun onFiatCurrencyChanged(fiatCurrency: FiatCurrency) {
        val position = adapter.items.indexOfFirst { it is CommonTextItem && it.id == ItemCurrency }
        if (position >= 0) {
            adapter.notifyItemChanged(position, CommonTextValueChangePayload(fiatCurrency.name.uppercase()))
        }
    }

    // clicks
    private fun onAddressClicked() {
        val viewPosition = adapter.items.indexOfFirst { it is CommonTextItem && it.id == ItemAddress }
        if (viewPosition >= 0) {
            val view = recyclerView.findViewHolderForAdapterPosition(viewPosition)?.itemView ?: return
            val items = TonAccountType.values().map { type ->
                val typeString = type.getString()
                val addressString = Formatter.getShortAddressSafe(viewModel.getAccountAddress(type))
                val stringBuilder = SpannableStringBuilder(typeString).append("  ")
                if (addressString != null) {
                    stringBuilder.append(addressString)
                }
                stringBuilder.setSpan(ForegroundColorSpan(Res.color(R.color.text_primary)), 0, typeString.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                stringBuilder.setSpan(ForegroundColorSpan(Res.color(R.color.text_secondary)), typeString.length + 1, stringBuilder.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                MenuPopupWindow.MenuPopupItem(stringBuilder) { viewModel.onAccountTypeSelected(type) }
            }
            showPopupItems(items, view)
        }
    }

    private fun onFiatCurrencyClicked() {
        val viewPosition = adapter.items.indexOfFirst { it is CommonTextItem && it.id == ItemCurrency }
        if (viewPosition >= 0) {
            val view = recyclerView.findViewHolderForAdapterPosition(viewPosition)?.itemView ?: return
            val items = FiatCurrency.values().map { currency ->
                val currencyCode = currency.name.uppercase()
                val currencyName = getFiatCurrencyString(currency)
                val stringBuilder = SpannableStringBuilder().append(currencyCode).append("  ").append(currencyName)
                stringBuilder.setSpan(ForegroundColorSpan(Res.color(R.color.text_primary)), 0, currencyCode.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                stringBuilder.setSpan(ForegroundColorSpan(Res.color(R.color.text_secondary)), currencyCode.length + 1, stringBuilder.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                MenuPopupWindow.MenuPopupItem(stringBuilder) { viewModel.onFiatCurrencySelected(currency) }
            }
            showPopupItems(items, view)
        }
    }

    // utils
    private fun showPopupItems(items: List<MenuPopupWindow.MenuPopupItem>, view: View) {
        if (popupWindow != null) {
            popupWindow = null
            return
        }
        popupWindow = MenuPopupWindow(context)
            .setItems(items)
            .setDismissListener { popupWindow = null }
            .also { setCurrentPopupWindow(it) }
            .show(view, Gravity.TOP or Gravity.END, -view.height)
    }

    private fun getFiatCurrencyString(fiatCurrency: FiatCurrency): String {
        return when (fiatCurrency) {
            FiatCurrency.AED -> Res.str(R.string.fiat_aed)
            FiatCurrency.CHF -> Res.str(R.string.fiat_chf)
            FiatCurrency.CNY -> Res.str(R.string.fiat_cny)
            FiatCurrency.EUR -> Res.str(R.string.fiat_eur)
            FiatCurrency.GBP -> Res.str(R.string.fiat_gbp)
            FiatCurrency.IDR -> Res.str(R.string.fiat_idr)
            FiatCurrency.INR -> Res.str(R.string.fiat_inr)
            FiatCurrency.JPY -> Res.str(R.string.fiat_jpy)
            FiatCurrency.KRW -> Res.str(R.string.fiat_krw)
            FiatCurrency.RUB -> Res.str(R.string.fiat_rub)
            FiatCurrency.USD -> Res.str(R.string.fiat_usd)
        }
    }

    private fun setSwitchItemOn(id: Int, isOn: Boolean) {
        val position = adapter.items.indexOfFirst { it is CommonSwitchItem && it.id == id }
        if (position >= 0) {
            adapter.notifyItemChanged(position, CommonSwitchCheckChangePayload(isOn))
        }
    }

    private val scrollListener = object : RecyclerView.OnScrollListener() {

        private val offsetThreshold = Res.dp(16f)
        private var cumulativeOffset = 0

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            cumulativeOffset += dy
            val topOffset = clamp(initialDrawableOffset - cumulativeOffset, initialDrawableOffset - bottomSheetDrawable.topRadius, initialDrawableOffset)
            bottomSheetDrawable.setTopOffset(topOffset)
            toolbar.setShadowAlpha(clamp(cumulativeOffset / offsetThreshold, 0f, 1f))
        }
    }

    private companion object {
        private var ItemCount = 0
        private var ItemNotifications = ItemCount++
        private var ItemAddress = ItemCount++
        private var ItemCurrency = ItemCount++
        private var ItemRecoveryPhrase = ItemCount++
        private var ItemChangePasscode = ItemCount++
        private var ItemBiometricAuth = ItemCount++
        private var ItemDeleteWallet = ItemCount++
    }
}