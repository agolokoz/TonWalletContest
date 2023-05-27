package org.ton.wallet.screen.onboarding.start

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.ton.wallet.lib.navigator.Screen
import org.ton.wallet.lib.navigator.ScreenParams
import org.ton.wallet.lib.screen.changehandler.SlideChangeHandler
import org.ton.wallet.lib.screen.viewmodel.BaseViewModel

class StartViewModel : BaseViewModel() {

    private val _isLoadingFlow = MutableStateFlow(false)
    val isLoadingFlow: Flow<Boolean> = _isLoadingFlow

    fun onCreateClicked() {
        _isLoadingFlow.tryEmit(true)
        launch(Dispatchers.IO) {
            try {
                settings.createWallet(null)
            } finally {
                _isLoadingFlow.tryEmit(false)
            }
            val params = ScreenParams(Screen.Congratulations, SlideChangeHandler(), SlideChangeHandler())
            navigator.add(params)
        }
    }

    fun onImportClicked() {
        val params = ScreenParams(Screen.Import, SlideChangeHandler(), SlideChangeHandler())
        navigator.add(params)
    }
}