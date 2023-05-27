package org.ton.wallet.screen.onboarding

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import org.ton.lib.rlottie.RLottieImageView
import org.ton.wallet.AppComponentsProvider
import org.ton.wallet.R
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.core.ext.setOnClickListenerWithLock
import org.ton.wallet.lib.navigator.Screen
import org.ton.wallet.lib.navigator.ScreenParams
import org.ton.wallet.lib.screen.controller.BaseController
import org.ton.wallet.screen.main.MainController

class PassCodeCompletedController(args: Bundle? = Bundle.EMPTY) : BaseController(args) {

    override val isStatusBarLight = true
    override val isNavigationBarLight = true

    private lateinit var animationView: RLottieImageView

    override fun createView(inflater: LayoutInflater, container: ViewGroup, savedViewState: Bundle?): View {
        val view = inflater.inflate(R.layout.screen_passcode_completed, container, false)
        val doneButton = view.findViewById<TextView>(R.id.passCodeCompletedDoneButton)
        doneButton.setOnClickListenerWithLock(::onDoneClicked)

        animationView = view.findViewById(R.id.passCodeCompletedAnimationView)

        val isImport = args.getBoolean(KeyImport, false)
        if (isImport) {
            view.findViewById<TextView>(R.id.passCodeCompletedTitle).text = Res.str(R.string.wallet_just_imported)
            view.findViewById<TextView>(R.id.passCodeCompletedSubtitle).isVisible = false
            doneButton.text = Res.str(R.string.proceed)
            animationView.setLottieResource(R.raw.lottie_congratulations)
        } else {
            animationView.setLottieResource(R.raw.lottie_success)
        }

        return view
    }

    override fun onChangeEnded(changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) {
        super.onChangeEnded(changeHandler, changeType)
        if (changeType.isEnter) {
            animationView.playAnimation()
        }
    }

    private fun onDoneClicked() {
        view?.let { v ->
            val screenShotBitmap = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(screenShotBitmap)
            v.draw(canvas)
            canvas.setBitmap(null)
            MainController.BitmapForAnimation = screenShotBitmap
        }
        val screenParams = ScreenParams(Screen.Main)
        AppComponentsProvider.navigator.replace(screenParams)
    }

    companion object {

        const val KeyImport = "import"
    }
}