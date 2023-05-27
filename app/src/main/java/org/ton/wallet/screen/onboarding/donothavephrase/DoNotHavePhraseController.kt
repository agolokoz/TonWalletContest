package org.ton.wallet.screen.onboarding.donothavephrase

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import org.ton.lib.rlottie.RLottieImageView
import org.ton.wallet.R
import org.ton.wallet.lib.core.ext.setOnClickListenerWithLock
import org.ton.wallet.lib.screen.controller.BaseViewModelController
import org.ton.wallet.lib.screen.viewmodel.viewModels
import org.ton.wallet.uikit.dialog.IndeterminateProgressDialog

class DoNotHavePhraseController(args: Bundle?) : BaseViewModelController<DoNotHavePhraseViewModel>(args) {

    override val viewModel by viewModels { DoNotHavePhraseViewModel() }
    override val isStatusBarLight = true
    override val isNavigationBarLight = true

    private lateinit var animationView: RLottieImageView

    private var progressDialog: IndeterminateProgressDialog? = null

    override fun createView(inflater: LayoutInflater, container: ViewGroup, savedViewState: Bundle?): View {
        val view = inflater.inflate(R.layout.screen_do_not_have_phrase, container, false)
        view.findViewById<View>(R.id.doNotHavePhraseBackButton).setOnClickListenerWithLock(viewModel::onBackClicked)
        view.findViewById<View>(R.id.doNotHavePhraseDoneButton).setOnClickListenerWithLock(viewModel::onEnterClicked)
        view.findViewById<View>(R.id.doNotHavePhraseCreateButton).setOnClickListenerWithLock(viewModel::onCreateClicked)
        animationView = view.findViewById(R.id.doNotHavePhraseAnimationView)
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

    private fun onLoadingChanged(isLoading: Boolean) {
        if (isLoading) {
            progressDialog = IndeterminateProgressDialog(context, false)
            progressDialog?.show()
            setCurrentDialog(progressDialog)
        } else {
            progressDialog?.dismiss()
        }
    }
}