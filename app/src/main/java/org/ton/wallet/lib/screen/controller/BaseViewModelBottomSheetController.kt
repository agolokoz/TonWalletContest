package org.ton.wallet.lib.screen.controller

import android.app.Activity
import android.os.Bundle
import android.view.View
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import org.ton.wallet.lib.screen.viewmodel.BaseViewModel
import org.ton.wallet.lib.screen.viewmodel.ViewModelRetainType

abstract class BaseViewModelBottomSheetController<VM : BaseViewModel> @JvmOverloads constructor(
    args: Bundle? = null
) : BaseBottomSheetController(args) {

    abstract val viewModel: VM

    override fun onPostCreateView(view: View) {
        super.onPostCreateView(view)
        viewModel.setResultEventFlow.launchInControllerScope { (code, args) ->
            setControllerResult(code, args)
        }
    }

    override fun onChangeStarted(changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) {
        super.onChangeStarted(changeHandler, changeType)
        viewModel.onScreenChange(true, changeType)
    }

    override fun onChangeEnded(changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) {
        viewModel.onScreenChange(false, changeType)
        super.onChangeEnded(changeHandler, changeType)
    }

    override fun onActivityResumed(activity: Activity) {
        super.onActivityResumed(activity)
        viewModel.onResume()
    }

    override fun onControllerResult(code: String, data: Bundle) {
        super.onControllerResult(code, data)
        viewModel.onResult(code, data)
    }

    override fun onActivityPaused(activity: Activity) {
        viewModel.onPause()
        super.onActivityPaused(activity)
    }

    override fun onDestroyView(view: View) {
        viewModel.onDestroyView()
        super.onDestroyView(view)
    }

    override fun onDestroy() {
        if (viewModel.retainType == ViewModelRetainType.Controller) {
            viewModel.onDestroy()
        }
        super.onDestroy()
    }
}