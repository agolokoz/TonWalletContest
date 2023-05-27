package org.ton.wallet.screen.activity

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.os.bundleOf
import com.bluelinelabs.conductor.RouterTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.ton.wallet.AppLifecycleDetector
import org.ton.wallet.MainActivity
import org.ton.wallet.R
import org.ton.wallet.data.model.TonConnectEvent
import org.ton.wallet.domain.UseCases
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.navigator.Screen
import org.ton.wallet.lib.navigator.ScreenParams
import org.ton.wallet.lib.screen.changehandler.OnBoardingShowChangeHandler
import org.ton.wallet.lib.screen.changehandler.SlideChangeHandler
import org.ton.wallet.lib.screen.viewmodel.BaseViewModel
import org.ton.wallet.screen.pass.base.BasePassCodeController
import org.ton.wallet.screen.pass.enter.PassCodeEnterController
import org.ton.wallet.screen.send.connect.SendConnectConfirmController
import org.ton.wallet.util.LinkUtils
import org.ton.wallet.util.NotificationUtils

class MainActivityViewModel : BaseViewModel() {

    private var backStackUnderPassCode: List<RouterTransaction>? = null
    private var intent: Intent? = null

    private val isNeedShowPassCode: Boolean
        get() = settings.isWalletCreatedFlow.value && auth.hasPassCode

    private val isPasscodeAtTop: Boolean
        get() = navigator.getBackStack().lastOrNull()?.tag() == Screen.PassCodeEnter.name

    private var isPassCodeShown = false

    init {
        AppLifecycleDetector.isAppForegroundFlow
            .onEach { isForeground ->
                if (isForeground && !isPasscodeAtTop && isNeedShowPassCode) {
                    backStackUnderPassCode = navigator.getBackStack()
                    showPassCodeEnter(false)
                }
                if (!isForeground) {
                    isPassCodeShown = false
                }
            }
            .launchIn(viewModelScope)

        UseCases.currentAccountTonConnectEventsFlow
            .onEach(::onTonConnectEvent)
            .launchIn(viewModelScope)
    }

    fun onNewActivityCreated(): MainActivityAnimationType {
        backStackUnderPassCode = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S || isNeedShowPassCode) {
            onAnimationFinished()
            return MainActivityAnimationType.None
        }
        return MainActivityAnimationType.BottomSheetUp
    }

    override fun onResult(code: String, data: Bundle) {
        super.onResult(code, data)
        if (code == PassCodeEnterController.ResultKeyCorrectPassCodeEntered) {
            val purpose = data.getString(PassCodeEnterController.ArgumentKeyPurpose)
            var isPurposeUnlock = false
            if (purpose == PurposeAppUnlock) {
                isPurposeUnlock = true
                navigator.replace(ScreenParams(Screen.Main, SlideChangeHandler(), SlideChangeHandler()))
            } else if (purpose == PurposeActivityUnlock) {
                isPurposeUnlock = true
                navigator.back()
            }
            if (isPurposeUnlock) {
                onAppUnlocked()
            }
        }
    }

    fun setIntent(intent: Intent?) {
        this.intent = intent
        if (AppLifecycleDetector.isAppForegroundFlow.value && !isPasscodeAtTop && isPassCodeShown) {
            handleIntent()
        }
    }

    fun onAnimationFinished() {
        if (settings.isWalletCreatedFlow.value) {
            if (auth.hasPassCode) {
                showPassCodeEnter(true)
            } else if (settings.isRecoveryChecked) {
                navigator.replace(ScreenParams(Screen.RecoveryCheckFinished, OnBoardingShowChangeHandler()), true)
            } else {
                navigator.replace(ScreenParams(Screen.Congratulations, OnBoardingShowChangeHandler()), true)
            }
        } else {
            navigator.replace(ScreenParams(Screen.Start, OnBoardingShowChangeHandler()), true)
        }
    }

    private fun onAppUnlocked() {
        handleIntent()
    }

    private fun handleIntent() {
        if (intent == null) {
            return
        }
        launch(Dispatchers.Default) {
            val tonConnectTransferAction = intent?.getParcelableExtra<TonConnectEvent.Transfer>(MainActivity.ArgumentKeyTonConnectAction)
            val action = LinkUtils.parseLink(intent?.data?.toString() ?: "")
            if (tonConnectTransferAction != null) {
                val params = ScreenParams(
                    screen = Screen.SendConnectConfirm,
                    arguments = bundleOf(SendConnectConfirmController.ArgumentKeyTransfer to tonConnectTransferAction)
                )
                navigator.add(params)
            } else if (action != null) {
                actionHandler.processDeepLinkAction(action)
            }
            intent = null
        }
    }

    private fun showPassCodeEnter(appUnlock: Boolean) {
        val args = Bundle()
        args.putBoolean(BasePassCodeController.ArgumentKeyBackVisible, false)
        args.putBoolean(BasePassCodeController.ArgumentKeyDark, true)
        if (appUnlock) {
            args.putString(PassCodeEnterController.ArgumentKeyPurpose, PurposeAppUnlock)
            navigator.add(ScreenParams(Screen.PassCodeEnter, args, null, null, this))
        } else {
            args.putString(PassCodeEnterController.ArgumentKeyPurpose, PurposeActivityUnlock)
            args.putBoolean(PassCodeEnterController.ArgumentKeyPopOnBack, true)
            navigator.add(ScreenParams(Screen.PassCodeEnter, args, null, SlideChangeHandler(), this))
        }
        isPassCodeShown = true
    }

    private fun onTonConnectEvent(event: TonConnectEvent) {
        val isAppForeground = AppLifecycleDetector.isAppForegroundFlow.value
        if (event is TonConnectEvent.Transfer) {
            if (isAppForeground && !isPasscodeAtTop) {
                val params = ScreenParams(
                    screen = Screen.SendConnectConfirm,
                    arguments = bundleOf(SendConnectConfirmController.ArgumentKeyTransfer to event)
                )
                navigator.add(params)
            } else if (settings.isNotificationsOnFlow.value) {
                val intent = Intent(Res.context, MainActivity::class.java)
                intent.putExtra(MainActivity.ArgumentKeyTonConnectAction, event)

                var flags = PendingIntent.FLAG_UPDATE_CURRENT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    flags = flags or PendingIntent.FLAG_MUTABLE
                }
                val pendingIntent = PendingIntent.getActivity(Res.context, 0, intent, flags)

                val notification = NotificationCompat.Builder(Res.context, NotificationUtils.ChannelId)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setContentTitle(Res.str(R.string.notification_ton_connect_title))
                    .setContentText(Res.str(R.string.notification_ton_connect_description))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setSmallIcon(R.drawable.ic_gem_18)
                    .build()
                NotificationUtils.showNotification(Res.context, NotificationUtils.IdTonConnectAction, notification)
            }
        }
    }

    private companion object {

        private const val PurposeAppUnlock = "appUnlock"
        private const val PurposeActivityUnlock = "activityUnlock"
    }
}