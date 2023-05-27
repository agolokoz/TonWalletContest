package org.ton.wallet.screen.base.input

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.math.MathUtils.clamp
import org.ton.wallet.lib.core.KeyboardUtils
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.core.ext.animateShake
import org.ton.wallet.lib.core.ext.setOnClickListener
import org.ton.wallet.lib.core.ext.setTextWithSelection
import org.ton.wallet.lib.core.ext.vibrate
import org.ton.wallet.lib.screen.controller.BaseViewModelController
import org.ton.wallet.screen.onboarding.recoverycheck.SuggestWordsPopupWindow
import org.ton.wallet.uikit.view.AppEditText
import org.ton.wallet.uikit.view.AppToolbar
import org.ton.wallet.uikit.view.NumericEditTextLayout

abstract class BaseInputListController<VM : BaseInputListViewModel>(args: Bundle?) : BaseViewModelController<VM>(args) {

    override val isStatusBarLight = true
    override val isNavigationBarLight = true
    override val isSecured = true

    protected abstract val toolbar: AppToolbar
    protected abstract val inputLayouts: Array<NumericEditTextLayout>

    private lateinit var suggestPopupWindow: SuggestWordsPopupWindow

    protected var currentFocusedEditText: EditText? = null

    override fun onPreCreateView() {
        super.onPreCreateView()
        suggestPopupWindow = SuggestWordsPopupWindow(context) { item, _ -> onWordSelected(item) }
    }

    override fun onPostCreateView(view: View) {
        super.onPostCreateView(view)
        view.setOnClickListener(::dismissSuggestPopupWindow)
        viewModel.suggestWordsFlow.launchInViewScope(::onSuggestWordsChanged)
    }

    protected fun dismissSuggestPopupWindow() {
        if (suggestPopupWindow.isShowing) {
            suggestPopupWindow.dismiss()
        }
    }

    protected fun checkInputs(): Boolean {
        for (i in inputLayouts.indices) {
            val editText = inputLayouts[i].editText
            if (editText.length() == 0) {
                editText.clearFocus()
                editText.requestFocus()
                inputLayouts[i].animateShake(4)
                context.vibrate()
                return false
            }
        }
        return true
    }

    private fun onSuggestWordsChanged(words: List<String>) {
        if (words.isEmpty()) {
            dismissSuggestPopupWindow()
        } else {
            suggestPopupWindow.setWords(words)
            if (!suggestPopupWindow.isShowing) {
                currentFocusedEditText?.let { suggestPopupWindow.show(it.parent as ViewGroup) }
            }
        }
    }

    private fun onWordSelected(word: String) {
        currentFocusedEditText?.let { et ->
            et.setTextWithSelection(word)
            val editTextPosition = inputLayouts.indexOfFirst { it.editText == et }
            if (editTextPosition != -1) {
                if (editTextPosition < inputLayouts.size - 1) {
                    inputLayouts[editTextPosition + 1].editText.requestFocus()
                } else {
                    KeyboardUtils.hideKeyboard(activity!!.window, clearFocus = false)
                }
            }
        }
        dismissSuggestPopupWindow()
    }

    protected val editTextFocusChangedListener = object : NumericEditTextLayout.TextFocusChangedListener {

        override fun onTextFocusChanged(v: AppEditText, text: CharSequence, isFocused: Boolean) {
            val editTextLayout = v.parent as NumericEditTextLayout
            if (isFocused) {
                currentFocusedEditText = v
                val position = inputLayouts.indexOf(editTextLayout)
                viewModel.setEnteredWord(position, text.toString().trim())
                if (text.isEmpty()) {
                    dismissSuggestPopupWindow()
                } else if (!suggestPopupWindow.isShowing
                    && !Res.isLandscapeScreenSize
                    && viewModel.suggestWordsFlow.value.isNotEmpty()
                    && viewModel.suggestWordsFlow.value.first() != text
                ) {
                    suggestPopupWindow.show(editTextLayout)
                }
            } else {
                dismissSuggestPopupWindow()
            }
        }
    }

    protected val scrollChangeListener = object : View.OnScrollChangeListener {

        private val toolbarTitleHeight = Res.sp(24)
        private val titleMinScroll = Res.dp(112) + toolbarTitleHeight / 2

        override fun onScrollChange(v: View?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int) {
            dismissSuggestPopupWindow()
            toolbar.setTitleAlpha(clamp((scrollY.toFloat() - titleMinScroll) / toolbarTitleHeight, 0f, 1f))
            toolbar.setShadowAlpha(clamp(scrollY.toFloat() / Res.dp(16), 0f, 1f))
        }
    }
}