package org.ton.wallet

import android.app.Dialog
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.ton.wallet.lib.core.*
import org.ton.wallet.lib.navigator.Navigator
import org.ton.wallet.lib.screen.controller.BaseController
import org.ton.wallet.screen.activity.*
import org.ton.wallet.util.FlowBus
import org.ton.wallet.util.FlowBusEvent
import org.ton.wallet.util.NotificationUtils
import pub.devrel.easypermissions.EasyPermissions

class MainActivity : AppCompatActivity(),
    EasyPermissions.PermissionCallbacks,
    ActivityResultCallback<Uri?> {

    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob() + CoroutinesUtils.getCoroutineExceptionHandler("MainActivityScope"))

    private val activityViewModel by lazy { MainActivityViewModel() }
    private val pollingViewModel by lazy { PollingViewModel() }

    private val navigator: Navigator
        get() = AppComponentsProvider.navigator

    private lateinit var rootLayout: FrameLayout
    private lateinit var snackBarDelegate: SnackBarDelegate
    private lateinit var insetsController: WindowInsetsControllerCompat
    private lateinit var imagePickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>

    private var onBoardingAnimationDelegate: OnBoardingAnimationDelegate? = null
    private var currentDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        if (BuildConfig.DEBUG) {
//            val threadPolicy = StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build()
//            StrictMode.setThreadPolicy(threadPolicy)
//            val vmPolicy = StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build()
//            StrictMode.setVmPolicy(vmPolicy)
        }
        Res.init(this, resources.configuration)
        super.onCreate(savedInstanceState)

        rootLayout = FrameLayout(this)
        setContentView(rootLayout)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        insetsController = WindowInsetsControllerCompat(window, rootLayout)

        if (savedInstanceState == null) {
            pollingViewModel.start()
            val animationType = activityViewModel.onNewActivityCreated()
            if (animationType == MainActivityAnimationType.None) {
                insetsController.isAppearanceLightStatusBars = true
                insetsController.isAppearanceLightNavigationBars = true
                window.setBackgroundDrawable(ColorDrawable(Res.color(R.color.common_white)))
            } else {
                if (animationType == MainActivityAnimationType.BottomSheetDown) {
                    insetsController.isAppearanceLightStatusBars = false
                    insetsController.isAppearanceLightNavigationBars = false
                    window.setBackgroundDrawable(ColorDrawable(Res.color(R.color.common_black)))
                } else if (animationType == MainActivityAnimationType.BottomSheetUp) {
                    insetsController.isAppearanceLightStatusBars = true
                    insetsController.isAppearanceLightNavigationBars = true
                }
                onBoardingAnimationDelegate = OnBoardingAnimationDelegate(rootLayout, animationType)
            }
        }

        onBackPressedDispatcher.addCallback(backPressCallback)
        snackBarDelegate = SnackBarDelegate(rootLayout)
        navigator.attach(this, rootLayout, savedInstanceState)

        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia(), this)
        activityViewModel.setIntent(intent)

        FlowBus.common.eventsFlow
            .onEach { event ->
                when (event) {
                    is FlowBusEvent.ShowAlertDialog -> {
                        showDialog(event.dialogBuilder.build(this))
                    }
                    is FlowBusEvent.ShowSnackBar -> {
                        snackBarDelegate.showMessage(event)
                    }
                    FlowBusEvent.HideSnackBar -> {
                        snackBarDelegate.hideMessage()
                    }
                    is FlowBusEvent.PickMediaContent -> {
                        imagePickerLauncher.launch(PickVisualMediaRequest(event.mediaType))
                        AppLifecycleDetector.isImagePickerStarted = true
                    }
                }
            }
            .launchIn(activityScope)
    }

    override fun onStart() {
        super.onStart()
        BrowserUtils.init(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        activityViewModel.setIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        ThreadUtils.postOnMain({
            val animationType = onBoardingAnimationDelegate?.animationType ?: MainActivityAnimationType.None
            onBoardingAnimationDelegate?.startOpenAnimation {
                if (animationType == MainActivityAnimationType.BottomSheetDown) {
                    window.setBackgroundDrawable(ColorDrawable(Res.color(R.color.common_black)))
                } else if (animationType == MainActivityAnimationType.BottomSheetUp) {
                    window.setBackgroundDrawable(ColorDrawable(Res.color(R.color.common_white)))
                }
                activityViewModel.onAnimationFinished()
            }
            onBoardingAnimationDelegate = null
        }, 64)
    }

    override fun onDestroy() {
        currentDialog?.dismiss()
        currentDialog = null
        snackBarDelegate.onDestroy()
        activityScope.cancel()
        AppComponentsProvider.navigator.detach()
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Res.init(this, newConfig)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        navigator.forEachScreen { screen ->
            NotificationUtils.checkPermissions()
            if (screen is BaseController) {
                screen.onPermissionsGranted(requestCode, perms)
            }
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        navigator.forEachScreen { screen ->
            if (screen is BaseController) {
                screen.onPermissionsDenied(requestCode, perms)
            }
        }
    }

    override fun onActivityResult(result: Uri?) {
        AppLifecycleDetector.isImagePickerStarted = false
        navigator.forEachScreen { screen ->
            if (screen is BaseController) {
                screen.onActivityResult(result)
            }
        }
    }

    private fun showDialog(dialog: Dialog) {
        currentDialog?.dismiss()
        currentDialog = dialog
        dialog.show()
    }

    private val backPressCallback = object : OnBackPressedCallback(true) {

        override fun handleOnBackPressed() {
            KeyboardUtils.hideKeyboard(window, false) {
                if (!navigator.onBackPressed()) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }
    }

    companion object {

        const val ArgumentKeyTonConnectAction = "tonConnectAction"
    }
}