package org.ton.wallet.screen.settings

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.os.Bundle
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.ton.lib.tonapi.TonAccountType
import org.ton.wallet.R
import org.ton.wallet.data.model.FiatCurrency
import org.ton.wallet.domain.UseCases
import org.ton.wallet.lib.core.AndroidUtils
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.core.SecurityUtils
import org.ton.wallet.lib.core.ext.weak
import org.ton.wallet.lib.navigator.Screen
import org.ton.wallet.lib.navigator.ScreenParams
import org.ton.wallet.lib.screen.changehandler.SlideChangeHandler
import org.ton.wallet.lib.screen.controller.BaseController
import org.ton.wallet.lib.screen.viewmodel.BaseViewModel
import org.ton.wallet.screen.RecoveryViewController
import org.ton.wallet.screen.pass.enter.PassCodeEnterController
import org.ton.wallet.screen.pass.setup.PassCodeSetupController
import org.ton.wallet.uikit.dialog.AlertDialog
import org.ton.wallet.util.FlowBus
import org.ton.wallet.util.FlowBusEvent
import org.ton.wallet.util.NotificationUtils
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

class SettingsViewModel : BaseViewModel() {

    private val accountTypeAddressMap = ConcurrentHashMap<TonAccountType, String>()

    private var activityRef: WeakReference<FragmentActivity?> = weak(null)

    val accountTypeFlow: StateFlow<TonAccountType> = settings.accountTypeFlow
    val fiatCurrencyFlow: StateFlow<FiatCurrency> = settings.fiatCurrencyFlow
    val isBiometricOnFlow: StateFlow<Boolean> = auth.isBiometricActiveFlow
    val isNotificationsOnFlow: StateFlow<Boolean> = settings.isNotificationsOnFlow

    init {
        launch(Dispatchers.IO) {
            val deferred = TonAccountType.values().map { type ->
                async { accounts.getAccountAddress(type) }
            }
            deferred.awaitAll().forEachIndexed { index, s ->
                val type = TonAccountType.values()[index]
                accountTypeAddressMap[type] = s
            }
        }
    }

    override fun onResult(code: String, data: Bundle) {
        super.onResult(code, data)
        if (code == PassCodeEnterController.ResultKeyCorrectPassCodeEntered && data.containsKey(BaseController.KeyResultRequestId)) {
            when (data.getInt(BaseController.KeyResultRequestId)) {
                RequestIdRecovery -> {
                    val args = Bundle()
                    args.putBoolean(RecoveryViewController.KeyOnlyShow, true)
                    navigator.replace(ScreenParams(Screen.RecoveryPhrase, args, SlideChangeHandler(), SlideChangeHandler()))
                }
                RequestIdChangePasscode -> {
                    val args = Bundle()
                    args.putBoolean(PassCodeSetupController.KeyOnlySet, true)
                    navigator.replace(ScreenParams(Screen.PassCodeSetup, args, SlideChangeHandler(), SlideChangeHandler()))
                }
                RequestIdChangeBiometric -> {
                    navigator.back()
                    if (isBiometricOnFlow.value) {
                        auth.setBiometricAuthOn(false)
                    } else {
                        activityRef.get()?.let { activity ->
                            SecurityUtils.showBiometricPrompt(activity, Res.str(R.string.biometric_prompt_default_description), object : AuthenticationCallback() {
                                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                    super.onAuthenticationSucceeded(result)
                                    auth.setBiometricAuthOn(true)
                                }
                            })
                        }
                    }
                }
            }
        } else if (code == PassCodeSetupController.ResultCodePassCodeSet) {
            navigator.popTo(ScreenParams(Screen.Settings, SlideChangeHandler(), SlideChangeHandler()))
            FlowBus.common.dispatch(FlowBusEvent.ShowSnackBar(Res.str(R.string.passcode_changed)))
        }
    }

    override fun onPermissionsGranted(requestCode: Int, permissions: MutableList<String>) {
        super.onPermissionsGranted(requestCode, permissions)
        if (requestCode == PermissionRequestIdNotifications
            && !NotificationUtils.isNotificationsPermissionGrantedFlow.value
            && permissions.contains(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            settings.setNotificationsOn(true)
        }
    }

    override fun onPermissionsDenied(requestCode: Int, permissions: MutableList<String>) {
        super.onPermissionsDenied(requestCode, permissions)
        if (requestCode == PermissionRequestIdNotifications) {
            AndroidUtils.openAppSettings(Res.context)
        }
    }

    fun setActivity(activity: FragmentActivity) {
        activityRef = weak(activity)
    }

    fun onAccountTypeSelected(type: TonAccountType) {
        settings.setCurrentWalletType(type)
    }

    fun onFiatCurrencySelected(currency: FiatCurrency) {
        settings.setCurrentFiatCurrency(currency)
    }

    fun onShowRecoveryClicked() {
        showEnterPasscode(RequestIdRecovery)
    }

    fun onChangePasscodeClicked() {
        showEnterPasscode(RequestIdChangePasscode)
    }

    fun onNotificationsClicked(activity: Activity, isChecked: Boolean) {
        if (!isChecked && !NotificationUtils.isNotificationsPermissionGrantedFlow.value) {
            val request = NotificationUtils.getPermissionRequest(activity, PermissionRequestIdNotifications)
            request?.let { EasyPermissions.requestPermissions(it) }
            return
        }
        settings.setNotificationsOn(!isChecked)
    }

    fun onBiometricAuthClicked(isChecked: Boolean) {
        val context = activityRef.get() ?: return
        if (!isChecked && SecurityUtils.isBiometricsNoneEnrolled(context)) {
            SecurityUtils.showBiometricEnrollAlert(context)
            return
        }
        showEnterPasscode(RequestIdChangeBiometric)
    }

    fun getAccountAddress(type: TonAccountType): String? {
        return accountTypeAddressMap[type]
    }

    fun onDeleteWalletClicked(activity: Activity) {
        val clickListener = DialogInterface.OnClickListener { dialog, which ->
            if (which == DialogInterface.BUTTON_POSITIVE) {
                performDeleteWallet()
            }
            dialog.dismiss()
        }
        AlertDialog.Builder(
            title = Res.str(R.string.delete_wallet_alert_title),
            message = Res.str(R.string.delete_wallet_alert_description),
            positiveButton = Res.str(R.string.ok) to clickListener,
            negativeButton = Res.str(R.string.cancel) to clickListener
        ).build(activity)
            .show()
    }

    private fun showEnterPasscode(resultRequestId: Int) {
        val args = Bundle()
        args.putString(PassCodeEnterController.ArgumentKeyPurpose, PassCodeEnterPurpose)
        if (resultRequestId == RequestIdChangePasscode) {
            args.putBoolean(PassCodeEnterController.ArgumentKeyOnlyPassCode, true)
        }
        navigator.add(ScreenParams(Screen.PassCodeEnter, args, SlideChangeHandler(), SlideChangeHandler(), resultRequestId = resultRequestId))
    }

    private fun performDeleteWallet() {
        launch(Dispatchers.IO) {
            UseCases.deleteWallet()
            navigator.replace(ScreenParams(Screen.Start, SlideChangeHandler(), SlideChangeHandler()), true)
        }
    }

    private companion object {

        private const val RequestIdRecovery = 0
        private const val RequestIdChangePasscode = 1
        private const val RequestIdChangeBiometric = 2

        private const val PermissionRequestIdNotifications = 10

        private const val PassCodeEnterPurpose = "settingsUnlock"
    }
}