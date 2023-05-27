package org.ton.wallet.screen.onboarding.recoverycheck

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import org.ton.lib.rlottie.RLottieImageView
import org.ton.wallet.R
import org.ton.wallet.lib.core.KeyboardUtils
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.core.ext.setOnClickListener
import org.ton.wallet.lib.core.ext.setOnClickListenerWithLock
import org.ton.wallet.lib.core.ext.setTextWithSelection
import org.ton.wallet.lib.screen.viewmodel.viewModels
import org.ton.wallet.screen.base.input.BaseInputListController
import org.ton.wallet.uikit.view.AppToolbar
import org.ton.wallet.uikit.view.NumericEditTextLayout
import kotlin.math.max

class RecoveryCheckController(args: Bundle?) : BaseInputListController<RecoveryCheckViewModel>(args) {

    override val viewModel by viewModels { RecoveryCheckViewModel() }
    override val toolbar: AppToolbar get() = appToolbar
    override val inputLayouts: Array<NumericEditTextLayout> get() = editTextLayouts

    private lateinit var animationView: RLottieImageView
    private lateinit var appToolbar: AppToolbar
    private lateinit var continueButton: TextView
    private lateinit var editTextLayouts: Array<NumericEditTextLayout>

    private val continueButtonBottomMargin: Int
        get() = if (Res.isLandscapeScreenSize) Res.dp(20) else Res.dp(100)

    override fun createView(inflater: LayoutInflater, container: ViewGroup, savedViewState: Bundle?): View {
        val view = inflater.inflate(R.layout.screen_recovery_check, container, false)
        view.findViewById<NestedScrollView>(R.id.recoveryCheckScrollView).setOnScrollChangeListener(scrollChangeListener)
        view.findViewById<View>(R.id.recoveryCheckLinearLayout).setOnClickListener(::dismissSuggestPopupWindow)

        animationView = view.findViewById(R.id.recoveryCheckAnimationView)

        appToolbar = view.findViewById(R.id.recoveryCheckToolbar)
        appToolbar.setTitleAlpha(0f)
        appToolbar.setShadowAlpha(0f)

        continueButton = view.findViewById(R.id.recoveryCheckContinueButton)
        (continueButton.layoutParams as MarginLayoutParams).bottomMargin = continueButtonBottomMargin
        continueButton.setOnClickListenerWithLock(::onContinueClicked)

        // setup subtitle
        val subtitleText = view.findViewById<TextView>(R.id.recoveryCheckSubTitle)
        subtitleText.text = viewModel.subtitle

        // setup inputs
        var maxNumberWidth = 0f
        editTextLayouts = arrayOf(
            view.findViewById(R.id.recoveryCheckEditText1),
            view.findViewById(R.id.recoveryCheckEditText2),
            view.findViewById(R.id.recoveryCheckEditText3),
        )
        for (i in editTextLayouts.indices) {
            val editTextLayout = editTextLayouts[i]
            editTextLayout.editText.setTextWithSelection(viewModel.enteredWords[i])
            editTextLayout.setNumber(viewModel.wordPositions[i] + 1)
            editTextLayout.setTextFocusChangedListener(editTextFocusChangedListener)
            maxNumberWidth = max(editTextLayouts[i].numberTextWidth, maxNumberWidth)
        }
        for (i in editTextLayouts.indices) {
            editTextLayouts[i].setMaxTextWidth(maxNumberWidth)
        }

        return view
    }

    override fun onPostCreateView(view: View) {
        super.onPostCreateView(view)
        viewModel.errorStatesFlow.launchInViewScope(::onErrorStatesChanged)
    }

    override fun onChangeEnded(changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) {
        super.onChangeEnded(changeHandler, changeType)
        if (changeType.isEnter) {
            animationView.playAnimation()
        }
    }

    override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
        val superInsets = super.onApplyWindowInsets(v, insets)
        (continueButton.layoutParams as MarginLayoutParams).bottomMargin =
            if (KeyboardUtils.isKeyboardOpened(v)) Res.dp(20)
            else continueButtonBottomMargin
        continueButton.requestLayout()
        return superInsets
    }

    private fun onErrorStatesChanged(errors: BooleanArray) {
        editTextLayouts.forEachIndexed { index, numericEditTextLayout ->
            numericEditTextLayout.editText.setErrorState(errors[index])
        }
    }

    private fun onContinueClicked() {
        dismissSuggestPopupWindow()
        if (checkInputs()) {
            viewModel.onContinueClicked(activity!!)
        }
    }
}