package org.ton.wallet.screen

import android.content.DialogInterface
import android.os.Bundle
import android.os.SystemClock
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.GridLayout
import androidx.core.math.MathUtils.clamp
import androidx.core.view.forEach
import androidx.core.view.setMargins
import androidx.core.view.updateMarginsRelative
import androidx.core.widget.NestedScrollView
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import org.ton.lib.rlottie.RLottieImageView
import org.ton.wallet.AppComponentsProvider
import org.ton.wallet.R
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.core.ext.setOnClickListenerWithLock
import org.ton.wallet.lib.navigator.Screen
import org.ton.wallet.lib.navigator.ScreenParams
import org.ton.wallet.lib.screen.changehandler.SlideChangeHandler
import org.ton.wallet.lib.screen.controller.BaseController
import org.ton.wallet.uikit.dialog.AlertDialog
import org.ton.wallet.uikit.view.AppToolbar
import org.ton.wallet.uikit.view.NumericTextView
import org.ton.wallet.util.FlowBus
import org.ton.wallet.util.FlowBusEvent
import kotlin.math.max

class RecoveryViewController(args: Bundle?) : BaseController(args) {

    override val isStatusBarLight = true
    override val isNavigationBarLight = true
    override val isSecured = true

    private val isOnlyShow = args?.getBoolean(KeyOnlyShow) ?: false

    private lateinit var toolbar: AppToolbar
    private lateinit var animationView: RLottieImageView

    private var showTime = 0L
    private var doneClicksCount = 0
    private var lastScrollY = 0

    private val doneButtonBottomMargin: Int
        get() = if (Res.isLandscapeScreenSize) Res.dp(20) else Res.dp(56)

    override fun createView(inflater: LayoutInflater, container: ViewGroup, savedViewState: Bundle?): View {
        val view = inflater.inflate(R.layout.screen_recovery_phrase, container, false)
        view.findViewById<NestedScrollView>(R.id.recoveryPhraseScrollView).setOnScrollChangeListener(scrollChangeListener)

        val doneButton = view.findViewById<View>(R.id.recoveryPhraseDoneButton)
        doneButton.setOnClickListenerWithLock(::onDoneClicked)
        (doneButton.layoutParams as MarginLayoutParams).bottomMargin = doneButtonBottomMargin

        toolbar = view.findViewById(R.id.recoveryPhraseToolbar)
        toolbar.setTitleAlpha(0f)
        toolbar.setShadowAlpha(0f)

        animationView = view.findViewById(R.id.recoveryPhraseAnimationView)

        return view
    }

    override fun onPostCreateView(view: View) {
        super.onPostCreateView(view)
        showTime = SystemClock.elapsedRealtime()
        val words = AppComponentsProvider.settingsRepository.getRecoveryPhrase()

        val gridLayout = view.findViewById<GridLayout>(R.id.recoveryPhraseGridLayout)
        gridLayout.removeAllViews()
        var maxTextWidth = 0f
        words.forEachIndexed { index, text ->
            val textView = NumericTextView(context)
            textView.setTextColor(Res.color(R.color.text_primary))
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            textView.text = text
            textView.typeface = Res.font(R.font.roboto_medium)
            textView.setNumber(index + 1)
            maxTextWidth = max(textView.textWidth, maxTextWidth)

            val isFirstColumn = index < 12
            val rowSpec = GridLayout.spec(index % 12, 1, 1f)
            val columnSpec = GridLayout.spec(index / 12, 1, 1f)
            val layoutParams = GridLayout.LayoutParams(rowSpec, columnSpec)
            layoutParams.setMargins(Res.dp(4))
            if (isFirstColumn) {
                layoutParams.updateMarginsRelative(end = Res.dp(64))
            }
            gridLayout.addView(textView, layoutParams)
        }
        gridLayout.forEach { child ->
            (child as NumericTextView).setMaxTextWidth(maxTextWidth)
        }
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        scrollChangeListener.onScrollChange(null, 0, lastScrollY, 0, 0)
    }

    override fun onChangeEnded(changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) {
        super.onChangeEnded(changeHandler, changeType)
        if (changeType.isEnter) {
            animationView.playAnimation()
        }
    }

    private fun onDoneClicked() {
        if (isOnlyShow) {
            AppComponentsProvider.navigator.back()
            return
        }

        doneClicksCount++
//        if (!BuildConfig.DEBUG && SystemClock.elapsedRealtime() - showTime < 60 * 1000) {
        if (SystemClock.elapsedRealtime() - showTime < 60 * 1000) {
            val negativeButton =
                if (doneClicksCount > 1) {
                    Res.str(R.string.skip) to DialogInterface.OnClickListener { dialog, _ ->
                        dialog.dismiss()
                        navigateToCheck()
                    }
                } else {
                    null
                }
            val dialogBuilder = AlertDialog.Builder(
                title = Res.str(R.string.sure_done),
                message = Res.str(R.string.you_didnt_have_time),
                positiveButton = Res.str(R.string.ok_sorry) to DialogInterface.OnClickListener { dialog, _ -> dialog.dismiss() },
                negativeButton = negativeButton,
                isCancelable = false
            )
            FlowBus.common.dispatch(FlowBusEvent.ShowAlertDialog(dialogBuilder))
        } else {
            navigateToCheck()
        }
    }

    private fun navigateToCheck() {
        val screenParams = ScreenParams(
            screen = Screen.RecoveryCheck,
            pushChangeHandler = SlideChangeHandler(),
            popChangeHandler = SlideChangeHandler()
        )
        AppComponentsProvider.navigator.add(screenParams)
    }

    private val scrollChangeListener = object : View.OnScrollChangeListener {

        private val toolbarTitleHeight = Res.sp(24)
        private val titleMinScroll = Res.dp(112) + toolbarTitleHeight / 2

        override fun onScrollChange(v: View?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int) {
            toolbar.setTitleAlpha(clamp((scrollY.toFloat() - titleMinScroll) / toolbarTitleHeight, 0f, 1f))
            toolbar.setShadowAlpha(clamp(scrollY.toFloat() / Res.dp(16), 0f, 1f))
            lastScrollY = scrollY
        }
    }

    companion object {

        const val KeyOnlyShow = "onlyShow"
    }
}