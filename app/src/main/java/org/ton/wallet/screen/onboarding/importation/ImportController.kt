package org.ton.wallet.screen.onboarding.importation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.core.widget.NestedScrollView
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import org.ton.lib.rlottie.RLottieImageView
import org.ton.wallet.R
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.core.ext.setOnClickListener
import org.ton.wallet.lib.core.ext.setOnClickListenerWithLock
import org.ton.wallet.lib.core.ext.setTextWithSelection
import org.ton.wallet.lib.screen.viewmodel.viewModels
import org.ton.wallet.screen.base.input.BaseInputListController
import org.ton.wallet.uikit.dialog.IndeterminateProgressDialog
import org.ton.wallet.uikit.view.AppToolbar
import org.ton.wallet.uikit.view.NumericEditTextLayout
import kotlin.math.max

class ImportController(args: Bundle?) : BaseInputListController<ImportViewModel>(args) {

    override val viewModel by viewModels { ImportViewModel() }
    override val toolbar: AppToolbar get() = appToolbar
    override val inputLayouts: Array<NumericEditTextLayout> get() = editTextLayouts as Array<NumericEditTextLayout>

    private lateinit var animationView: RLottieImageView
    private lateinit var editTextLayouts: Array<NumericEditTextLayout?>
    private lateinit var appToolbar: AppToolbar

    private val editTextWidth = Res.dimenInt(R.dimen.phrase_word_width)
    private var progressDialog: IndeterminateProgressDialog? = null

    override fun createView(inflater: LayoutInflater, container: ViewGroup, savedViewState: Bundle?): View {
        val view = inflater.inflate(R.layout.screen_import, container, false)
        view.findViewById<NestedScrollView>(R.id.importScrollView).setOnScrollChangeListener(scrollChangeListener)
        view.findViewById<View>(R.id.importDontHaveButton).setOnClickListenerWithLock(viewModel::onDoNotHaveClicked)

        val doneButton = view.findViewById<View>(R.id.importDoneButton)
        doneButton.setOnClickListenerWithLock(::onDoneClicked)

        val contentLayout = view.findViewById<LinearLayout>(R.id.importContentLayout)
        contentLayout.setOnClickListener(::dismissSuggestPopupWindow)

        // setup inputs
        val childPosition = contentLayout.indexOfChild(doneButton)
        var maxNumberWidth = 0f
        val recoveryWordsCount = viewModel.enteredWords.size
        editTextLayouts = Array(recoveryWordsCount) { null }
        for (i in 0 until recoveryWordsCount) {
            val editTextLayout = PreCreatedEditTextLayouts?.get(i) ?: NumericEditTextLayout(context)
            editTextLayout.editText.setTextWithSelection(viewModel.enteredWords[i])
            editTextLayout.setNumber(i + 1)
            editTextLayout.setTextFocusChangedListener(editTextFocusChangedListener)

            val layoutParams = LinearLayout.LayoutParams(editTextWidth, WRAP_CONTENT)
            layoutParams.topMargin = if (i == 0) Res.dp(20) else Res.dp(8)
            contentLayout.addView(editTextLayout, childPosition + i, layoutParams)

            maxNumberWidth = max(maxNumberWidth, editTextLayout.numberTextWidth)
            editTextLayouts[i] = editTextLayout
        }
        for (i in editTextLayouts.indices) {
            editTextLayouts[i]?.setMaxTextWidth(maxNumberWidth)
        }

        animationView = view.findViewById(R.id.importAnimationView)
        appToolbar = view.findViewById(R.id.importToolbar)
        appToolbar.setTitleAlpha(0f)
        appToolbar.setShadowAlpha(0f)

        return view
    }

    override fun onPostCreateView(view: View) {
        super.onPostCreateView(view)
        viewModel.isLoadingFlow.launchInViewScope(::onLoadingChanged)
    }

    override fun onChangeEnded(changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) {
        super.onChangeEnded(changeHandler, changeType)
        if (changeType.isEnter) {
            animationView.playAnimation()
        }
    }

    override fun onDestroyView(view: View) {
        PreCreatedEditTextLayouts?.forEach { (it.parent as? ViewGroup)?.removeView(it) }
        super.onDestroyView(view)
    }

    private fun onDoneClicked() {
        if (checkInputs()) {
            viewModel.onDoneClicked()
        }
    }

    private fun onLoadingChanged(isLoading: Boolean) {
        if (isLoading) {
            progressDialog = IndeterminateProgressDialog(context, false)
            progressDialog?.show()
            setCurrentDialog(progressDialog)
        } else {
            progressDialog?.dismiss()
        }
    }

    companion object {

        var PreCreatedEditTextLayouts: Array<NumericEditTextLayout>? = null
    }
}