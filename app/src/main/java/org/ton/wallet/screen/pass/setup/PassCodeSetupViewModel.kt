package org.ton.wallet.screen.pass.setup

import android.os.Bundle
import com.bluelinelabs.conductor.ControllerChangeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import org.ton.wallet.R
import org.ton.wallet.data.model.PassCodeType
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.navigator.Screen
import org.ton.wallet.lib.navigator.ScreenParams
import org.ton.wallet.lib.screen.changehandler.SlideChangeHandler
import org.ton.wallet.screen.onboarding.PassCodeCompletedController
import org.ton.wallet.screen.pass.base.BasePassCodeViewModel
import org.ton.wallet.screen.pass.setup.PassCodeSetupController.Companion.ResultCodePassCodeSet
import org.ton.wallet.util.FlowBus
import org.ton.wallet.util.FlowBusEvent

class PassCodeSetupViewModel(args: Bundle) : BasePassCodeViewModel() {

    private val passcode: String = args.getString(PassCodeSetupController.KeyPasscode, "")
    private val isConfirmPasscode: Boolean = passcode.isNotEmpty()
    private val isImport: Boolean = args.getBoolean(PassCodeSetupController.KeyImport, false)
    private val isOnlySet: Boolean = args.getBoolean(PassCodeSetupController.KeyOnlySet, false)
    private val withBiometrics: Boolean = args.getBoolean(PassCodeSetupController.KeyBiometrics, false)

    override val title: String =
        if (isConfirmPasscode) Res.str(R.string.confirm_passcode)
        else Res.str(R.string.set_a_passcode)

    override val optionsText: String? =
        if (isConfirmPasscode) null
        else Res.str(R.string.passcode_options)

    init {
        passCodeTypeFlow.value =
            if (passcode.length == 6) PassCodeType.Pin6
            else PassCodeType.Pin4
    }

    override fun onResult(code: String, data: Bundle) {
        super.onResult(code, data)
        if (code == ResultCodePassCodeSet) {
            setResult(code, data)
        }
    }

    override fun onScreenChange(isStarted: Boolean, changeType: ControllerChangeType) {
        super.onScreenChange(isStarted, changeType)
        if (!isStarted && changeType == ControllerChangeType.PUSH_EXIT) {
            passCodeFlow.value = ""
        }
    }

    override fun onNumberEntered(number: String) {
        val passCodeTotalLength = passCodeTypeFlow.value.rawValue
        if (passCodeFlow.value.length + 1 > passCodeTotalLength) {
            return
        }

        passCodeFlow.value = passCodeFlow.value + number
        FlowBus.common.dispatch(FlowBusEvent.HideSnackBar)

        if (passCodeFlow.value.length == passCodeTotalLength) {
            val passCodeToCheck = passCodeFlow.value
            launch(Dispatchers.Default) {
                delay(200)
                if (isConfirmPasscode) {
                    if (passCodeToCheck == passcode) {
                        isLoadingFlow.tryEmit(true)
                        val type = if (passcode.length == 4) PassCodeType.Pin4 else PassCodeType.Pin6
                        auth.setPassCode(passcode, type)
                        auth.setBiometricAuthOn(withBiometrics)
                        isLoadingFlow.tryEmit(false)
                        if (isOnlySet) {
                            setResult(ResultCodePassCodeSet)
                        } else {
                            val args = Bundle().apply { putBoolean(PassCodeCompletedController.KeyImport, isImport) }
                            navigator.replace(ScreenParams(Screen.PassCodeCompleted, args, SlideChangeHandler()), true)
                        }
                    } else {
                        FlowBus.common.dispatch(FlowBusEvent.ShowSnackBar(Res.str(R.string.error), Res.str(R.string.passcode_not_matches), Res.drawable(R.drawable.ic_warning_32)))
                        _errorEventFlow.tryEmit(Unit)
                        passCodeFlow.value = ""
                    }
                } else {
                    val args = Bundle()
                    args.putBoolean(PassCodeSetupController.KeyBiometrics, withBiometrics)
                    args.putBoolean(PassCodeSetupController.KeyImport, isImport)
                    args.putBoolean(PassCodeSetupController.KeyOnlySet, isOnlySet)
                    args.putString(PassCodeSetupController.KeyPasscode, passCodeToCheck)
                    val screenParams = ScreenParams(Screen.PassCodeSetupCheck, args, SlideChangeHandler(), SlideChangeHandler())
                    navigator.add(screenParams)
                }
            }
        }
    }
}