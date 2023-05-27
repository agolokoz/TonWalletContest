package org.ton.wallet.screen.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.biometric.BiometricPrompt
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import org.ton.lib.rlottie.RLottieImageView
import org.ton.wallet.AppComponentsProvider
import org.ton.wallet.R
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.core.SecurityUtils
import org.ton.wallet.lib.core.ext.setOnClickListenerWithLock
import org.ton.wallet.lib.navigator.Screen
import org.ton.wallet.lib.navigator.ScreenParams
import org.ton.wallet.lib.screen.changehandler.SlideChangeHandler
import org.ton.wallet.lib.screen.controller.BaseController
import org.ton.wallet.screen.pass.setup.PassCodeSetupController
import org.ton.wallet.uikit.view.CheckBoxTextView

class RecoveryCheckFinishedController(args: Bundle?) : BaseController(args) {

    override val isStatusBarLight = true
    override val isNavigationBarLight = true

    private lateinit var animationView: RLottieImageView
    private lateinit var checkBoxView: CheckBoxTextView

    override fun createView(inflater: LayoutInflater, container: ViewGroup, savedViewState: Bundle?): View {
        val view = inflater.inflate(R.layout.screen_recovery_check_finsihed, container, false)
        view.findViewById<View>(R.id.recoveryCheckFinishedDoneButton).setOnClickListenerWithLock(::onDoneClicked)
        animationView = view.findViewById(R.id.recoveryCheckFinishedAnimationView)
        checkBoxView = view.findViewById(R.id.recoveryCheckFinishedCheckBox)
        checkBoxView.isVisible = SecurityUtils.isBiometricsAvailableOnDevice(context)
        return view
    }

    override fun onChangeEnded(changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) {
        super.onChangeEnded(changeHandler, changeType)
        if (changeType.isEnter) {
            animationView.playAnimation()
        }
    }

    override fun onSaveViewState(view: View, outState: Bundle) {
        outState.putBoolean(BiometricChecked, checkBoxView.isChecked)
        super.onSaveViewState(view, outState)
    }

    override fun onRestoreViewState(view: View, savedViewState: Bundle) {
        super.onRestoreViewState(view, savedViewState)
        checkBoxView.setChecked(savedViewState.getBoolean(BiometricChecked))
    }

    private fun onDoneClicked() {
        val isBiometricChecked = checkBoxView.isChecked
        if (isBiometricChecked && SecurityUtils.isBiometricsAvailableOnDevice(context)) {
            SecurityUtils.showBiometricPrompt(activity as FragmentActivity, Res.str(R.string.biometric_prompt_activate_description), biometricAuthCallback)
        } else {
            toPassCodeSetup()
        }
    }

    private fun toPassCodeSetup() {
        val arguments = Bundle().apply {
            putBoolean(PassCodeSetupController.KeyBiometrics, checkBoxView.isChecked)
            putBoolean(PassCodeSetupController.KeyImport, args.getBoolean(KeyImport, false))
        }
        val screenParams = ScreenParams(Screen.PassCodeSetup, arguments, SlideChangeHandler(), SlideChangeHandler())
        AppComponentsProvider.navigator.add(screenParams)
    }

    private val biometricAuthCallback = object : BiometricPrompt.AuthenticationCallback() {

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            toPassCodeSetup()
        }
    }

    companion object {

        private const val BiometricChecked = "biometricChecked"

        const val KeyImport = "import"
    }
}