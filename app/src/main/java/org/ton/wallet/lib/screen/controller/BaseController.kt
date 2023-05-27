package org.ton.wallet.lib.screen.controller

import android.app.Dialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.PopupWindow
import androidx.activity.result.ActivityResultCallback
import androidx.core.view.*
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.ton.wallet.BuildConfig
import org.ton.wallet.lib.core.KeyboardUtils
import org.ton.wallet.lib.core.L
import org.ton.wallet.lib.core.Res

abstract class BaseController @JvmOverloads constructor(
    args: Bundle? = null
) : Controller(args),
    OnApplyWindowInsetsListener,
    ActivityResultCallback<Any?> {

    protected open val isStatusBarLight: Boolean = false
    protected open val isNavigationBarLight: Boolean = false
    protected open val isSecured: Boolean = false
    protected open val useTopInsetsPadding: Boolean = true
    protected open val useBottomInsetsPadding: Boolean = true
    protected open val useImeInsets: Boolean = true
    protected open val orientation: Int? = null

    protected val context: Context get() = activity!!
    protected var isEnterPushChangeEnded: Boolean = false
        private set

    private val controllerScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    protected lateinit var viewScope: CoroutineScope
        private set
    protected lateinit var lifecycleOwner: ControllerLifecycleOwner
        private set

    private val resultsListeners = mutableListOf<ResultListener>()
    private val insetsController by lazy {
        WindowCompat.getInsetsController(activity!!.window, activity!!.window.decorView)
    }

    private var preCreateTimestampNs: Long = 0L
    private var currentPopupWindow: PopupWindow? = null
    private var currentDialog: Dialog? = null
    private var prevOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedViewState: Bundle?): View {
        lifecycleOwner = ControllerLifecycleOwner()
        addLifecycleListener(lifecycleOwner)
        onPreCreateView()
        if (BuildConfig.DEBUG) {
            preCreateTimestampNs = SystemClock.elapsedRealtimeNanos()
        }
        val view = createView(inflater, container, savedViewState)
        if (BuildConfig.DEBUG) {
            val createViewDurationNs = SystemClock.elapsedRealtimeNanos() - preCreateTimestampNs
            L.d("${this.javaClass.simpleName} create view duration: ${createViewDurationNs.toDouble() / 1000000} ms")
        }
        onPostCreateView(view)
        return view
    }

    protected open fun onPreCreateView() {
        viewScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }

    abstract fun createView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ) : View

    protected open fun onPostCreateView(view: View) {
        insetsController.isAppearanceLightStatusBars = isStatusBarLight
        insetsController.isAppearanceLightNavigationBars = isNavigationBarLight

        ViewCompat.setOnApplyWindowInsetsListener(view, this)
        ViewCompat.setWindowInsetsAnimationCallback(view, animationInsetsCallback)
        view.doOnAttach(ViewCompat::requestApplyInsets)
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        if (isSecured) {
            activity?.window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
        orientation?.let { orient ->
            prevOrientation = activity?.requestedOrientation!!
            activity?.requestedOrientation = orient
        }
    }

    override fun onDetach(view: View) {
        if (prevOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            activity?.requestedOrientation = prevOrientation
        }
        super.onDetach(view)
    }

    override fun onDestroyView(view: View) {
        activity?.window?.let { window ->
            KeyboardUtils.hideKeyboard(window, true)
        }
        viewScope.cancel()

        currentPopupWindow?.dismiss()
        currentPopupWindow = null
        currentDialog?.dismiss()
        currentDialog = null

        if (isSecured) {
            activity?.window?.setFlags(0, WindowManager.LayoutParams.FLAG_SECURE)
        }
        super.onDestroyView(view)
    }

    override fun onDestroy() {
        resultsListeners.clear()
        controllerScope.cancel()
        super.onDestroy()
    }

    override fun onChangeStarted(changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) {
        super.onChangeStarted(changeHandler, changeType)
        if (changeType.isEnter) {
            if (changeType.isPush) {
                isEnterPushChangeEnded = false
            }
            insetsController.isAppearanceLightStatusBars = isStatusBarLight
            insetsController.isAppearanceLightNavigationBars = isNavigationBarLight
        }
    }

    override fun onChangeEnded(changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) {
        super.onChangeEnded(changeHandler, changeType)
        if (changeType.isEnter && changeType.isPush) {
            isEnterPushChangeEnded = true
        }
    }

    open fun onPermissionsGranted(requestCode: Int, permissions: MutableList<String>) = Unit

    open fun onPermissionsDenied(requestCode: Int, permissions: MutableList<String>) = Unit

    override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
        Res.lastInsets = insets
        var typeMask = WindowInsetsCompat.Type.systemBars()
        if (useImeInsets) {
            typeMask += WindowInsetsCompat.Type.ime()
        }
        val typedInsets = insets.getInsets(typeMask)

        if (useTopInsetsPadding) {
            v.updatePadding(top = typedInsets.top)
        }
        if (useBottomInsetsPadding) {
            v.updatePadding(bottom = typedInsets.bottom)
        }
        v.updatePadding(left = typedInsets.left, right = typedInsets.right)

        return insets
    }

    override fun onActivityResult(result: Any?) = Unit


    fun addResultListener(resultListener: ResultListener) {
        if (!resultsListeners.contains(resultListener)) {
            resultsListeners.add(resultListener)
        }
    }

    fun setControllerResult(code: String, data: Bundle) {
        var actualData = data
        if (args.containsKey(KeyResultRequestId)) {
            if (actualData == Bundle.EMPTY) {
                actualData = Bundle()
            }
            actualData.putInt(KeyResultRequestId, args.getInt(KeyResultRequestId))
        }
        resultsListeners.forEach { it.onResult(code, actualData) }
        (targetController as? BaseController)?.onControllerResult(code, actualData)
    }

    protected open fun onControllerResult(code: String, data: Bundle) = Unit


    protected fun setCurrentPopupWindow(popupWindow: PopupWindow?) {
        currentPopupWindow?.dismiss()
        currentPopupWindow = popupWindow
    }

    protected fun setCurrentDialog(dialog: Dialog?) {
        currentDialog?.dismiss()
        currentDialog = dialog
    }


    protected fun <T> Flow<T>.launchInControllerScope(action: suspend (T) -> Unit) {
        launchInScope(controllerScope, action)
    }

    protected fun <T> Flow<T>.launchInViewScope(action: suspend (T) -> Unit) {
        launchInScope(viewScope, action)
    }

    private fun <T> Flow<T>.launchInScope(scope: CoroutineScope, action: suspend (T) -> Unit) {
        onEach { action.invoke(it) }.launchIn(scope)
    }


    private val animationInsetsCallback = object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {

        override fun onProgress(insets: WindowInsetsCompat, runningAnimations: MutableList<WindowInsetsAnimationCompat>): WindowInsetsCompat {
            return insets
        }

        override fun onEnd(animation: WindowInsetsAnimationCompat) {
            super.onEnd(animation)
            if (animation.typeMask and WindowInsetsCompat.Type.ime() != 0) {
                KeyboardUtils.onKeyboardAnimationFinished(activity!!.window.decorView)
            }
        }
    }


    companion object {

        const val KeyResultRequestId = "resultRequestId"
    }
}