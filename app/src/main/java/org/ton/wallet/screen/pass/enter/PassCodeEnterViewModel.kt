package org.ton.wallet.screen.pass.enter

import android.os.Bundle
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import org.ton.wallet.R
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.core.SecurityUtils
import org.ton.wallet.screen.pass.base.BasePassCodeViewModel

class PassCodeEnterViewModel(args: Bundle) : BasePassCodeViewModel() {

    private val isPopOnBack = args.getBoolean(PassCodeEnterController.ArgumentKeyPopOnBack)
    private val isPopOnSuccess = args.getBoolean(PassCodeEnterController.ArgumentKeyPopOnSuccess)

    private val purpose = args.getString(PassCodeEnterController.ArgumentKeyPurpose)
        ?: throw IllegalArgumentException("${PassCodeEnterController.ArgumentKeyPurpose} must be not null")

    val onlyPassCode = args.getBoolean(PassCodeEnterController.ArgumentKeyOnlyPassCode, false)

    override val title: String? = null

    override val optionsText: String? =
        if (auth.isBiometricActiveFlow.value && !onlyPassCode) Res.str(R.string.biometric_auth)
        else null

    override fun onNumberEntered(number: String) {
        if (passCodeFlow.value.length + 1 > passCodeTotalLength || isLoadingFlow.value) {
            return
        }

        passCodeFlow.value = passCodeFlow.value + number
        if (passCodeFlow.value.length == passCodeTotalLength) {
            val passCodeToCheck = passCodeFlow.value
            launch(Dispatchers.Default) {
                delay(200L)
                isLoadingFlow.tryEmit(true)
                val isPassCodeCorrect = auth.checkPassCode(passCodeToCheck)
                if (isPassCodeCorrect) {
                    onAuthSuccess()
                } else {
                    _errorEventFlow.tryEmit(Unit)
                    isLoadingFlow.tryEmit(false)
                    passCodeFlow.tryEmit("")
                }
            }
        }
    }

    fun showBiometricPrompt(activity: FragmentActivity) {
        if (auth.isBiometricActiveFlow.value) {
            SecurityUtils.showBiometricPrompt(activity, Res.str(R.string.biometric_prompt_default_description), object : AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    val fakePassCode = StringBuilder()
                    repeat(passCodeTotalLength) { fakePassCode.append("x") }
                    passCodeFlow.tryEmit(fakePassCode.toString())
                    onAuthSuccess()
                }
            })
        }
    }

    override fun onBackPressed(): Boolean {
        return if (isPopOnBack) {
            navigator.closeApp()
            true
        } else {
            super.onBackPressed()
        }
    }

    private fun onAuthSuccess() {
        val args = Bundle()
        args.putString(PassCodeEnterController.ArgumentKeyPurpose, purpose)
        setResult(PassCodeEnterController.ResultKeyCorrectPassCodeEntered, args)
        if (isPopOnSuccess) {
            navigator.back()
        }
    }
}