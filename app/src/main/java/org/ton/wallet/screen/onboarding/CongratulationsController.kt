package org.ton.wallet.screen.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import org.ton.lib.rlottie.RLottieImageView
import org.ton.wallet.AppComponentsProvider
import org.ton.wallet.R
import org.ton.wallet.lib.core.ext.setOnClickListenerWithLock
import org.ton.wallet.lib.navigator.Screen
import org.ton.wallet.lib.navigator.ScreenParams
import org.ton.wallet.lib.screen.changehandler.SlideChangeHandler
import org.ton.wallet.lib.screen.controller.BaseController
import org.ton.wallet.screen.onboarding.importation.ImportController

class CongratulationsController(args: Bundle?) : BaseController(args) {

    override val isStatusBarLight = true
    override val isNavigationBarLight = true

    private lateinit var animationView: RLottieImageView

    override fun onPreCreateView() {
        super.onPreCreateView()
        ImportController.PreCreatedEditTextLayouts = null
    }

    override fun createView(inflater: LayoutInflater, container: ViewGroup, savedViewState: Bundle?): View {
        val view = inflater.inflate(R.layout.screen_congratulations, container, false)
        view.findViewById<View>(R.id.passCodeCompletedDoneButton).setOnClickListenerWithLock(::onProceedClicked)
        animationView = view.findViewById(R.id.congratulationsAnimationView)
        return view
    }

    override fun onChangeEnded(changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) {
        super.onChangeEnded(changeHandler, changeType)
        if (changeType.isEnter) {
            animationView.playAnimation()
        }
    }

    private fun onProceedClicked() {
        val screenParams = ScreenParams(
            screen = Screen.RecoveryPhrase,
            pushChangeHandler = SlideChangeHandler(),
            popChangeHandler = SlideChangeHandler()
        )
        AppComponentsProvider.navigator.add(screenParams)
    }
}