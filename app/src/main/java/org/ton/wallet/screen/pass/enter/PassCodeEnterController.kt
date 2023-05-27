package org.ton.wallet.screen.pass.enter

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentActivity
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.screen.viewmodel.viewModels
import org.ton.wallet.screen.pass.base.BasePassCodeController

class PassCodeEnterController(args: Bundle) : BasePassCodeController<PassCodeEnterViewModel>(args) {

    override val viewModel by viewModels { PassCodeEnterViewModel(args) }

    private var isActivityResumed = true

    override fun onPostCreateView(view: View) {
        super.onPostCreateView(view)
        titleView.isVisible = false
        subTitleView.updateLayoutParams<MarginLayoutParams> { topMargin = Res.dp(20) }
        optionsButton.isVisible = false
    }

    override fun onChangeEnded(changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) {
        super.onChangeEnded(changeHandler, changeType)
        if (changeType.isEnter && changeType.isPush) {
            checkAndShowBiometric()
        }
    }

    override fun onActivityResumed(activity: Activity) {
        super.onActivityResumed(activity)
        isActivityResumed = true
        checkAndShowBiometric()
    }

    override fun onActivityPaused(activity: Activity) {
        isActivityResumed = false
        super.onActivityPaused(activity)
    }

    override fun onSetupOptionsClicked(v: View) {
        viewModel.showBiometricPrompt(activity as FragmentActivity)
    }

    private fun checkAndShowBiometric() {
        if (isActivityResumed && isEnterPushChangeEnded && !viewModel.onlyPassCode) {
            viewModel.showBiometricPrompt(activity as FragmentActivity)
        }
    }

    companion object {

        const val ArgumentKeyOnlyPassCode = "onlyPassCode"
        const val ArgumentKeyPopOnBack = "popOnBack"
        const val ArgumentKeyPopOnSuccess = "popOnSuccess"
        const val ArgumentKeyPurpose = "purpose"
        const val ResultKeyCorrectPassCodeEntered = "correctPassCodeEntered"
    }
}