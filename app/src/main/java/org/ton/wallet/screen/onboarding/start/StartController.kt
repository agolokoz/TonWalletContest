package org.ton.wallet.screen.onboarding.start

import android.os.Bundle
import android.os.Looper
import android.os.MessageQueue.IdleHandler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ton.lib.rlottie.RLottieImageView
import org.ton.wallet.AppComponentsProvider
import org.ton.wallet.R
import org.ton.wallet.lib.core.ext.setOnClickListenerWithLock
import org.ton.wallet.lib.screen.controller.BaseViewModelController
import org.ton.wallet.lib.screen.viewmodel.viewModels
import org.ton.wallet.screen.onboarding.importation.ImportController
import org.ton.wallet.uikit.dialog.IndeterminateProgressDialog
import org.ton.wallet.uikit.view.NumericEditTextLayout

class StartController(args: Bundle?) : BaseViewModelController<StartViewModel>(args) {

    override val viewModel by viewModels { StartViewModel() }

    override val isStatusBarLight = true
    override val isNavigationBarLight = true

    private lateinit var animationView: RLottieImageView
    private lateinit var createButton: TextView
    private lateinit var importButton: TextView

    private var progressDialog: IndeterminateProgressDialog? = null

    override fun onPreCreateView() {
        super.onPreCreateView()
        Looper.getMainLooper().queue.addIdleHandler(idleHandler)
    }

    override fun createView(inflater: LayoutInflater, container: ViewGroup, savedViewState: Bundle?): View {
        val view = inflater.inflate(R.layout.screen_start, container, false)
        animationView = view.findViewById(R.id.startAnimationView)
        createButton = view.findViewById(R.id.startCreateButton)
        createButton.setOnClickListenerWithLock(viewModel::onCreateClicked)
        importButton = view.findViewById(R.id.startImportButton)
        importButton.setOnClickListenerWithLock(viewModel::onImportClicked)
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

    private val idleHandler = IdleHandler {
        val wordsCount = AppComponentsProvider.settingsRepository.getRecoveryPhraseWordsCount()
        val context = activity
        if (context != null) {
            viewScope.launch(Dispatchers.Default) {
                val textLayouts = Array(wordsCount) { NumericEditTextLayout(context) }
                withContext(Dispatchers.Main) {
                    ImportController.PreCreatedEditTextLayouts = textLayouts
                }
            }
        }
        false
    }
}