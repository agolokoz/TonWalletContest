package org.ton.wallet.screen.onboarding.importation

import android.content.DialogInterface
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.ton.wallet.R
import org.ton.wallet.data.ton.TonApiException
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.navigator.Screen
import org.ton.wallet.lib.navigator.ScreenParams
import org.ton.wallet.lib.screen.changehandler.SlideChangeHandler
import org.ton.wallet.screen.base.input.BaseInputListViewModel
import org.ton.wallet.screen.onboarding.RecoveryCheckFinishedController
import org.ton.wallet.uikit.dialog.AlertDialog
import org.ton.wallet.util.FlowBus
import org.ton.wallet.util.FlowBusEvent

class ImportViewModel : BaseInputListViewModel() {

    private val _isLoadingFlow = MutableStateFlow(false)
    val isLoadingFlow: Flow<Boolean> = _isLoadingFlow

    fun onDoneClicked() {
        _isLoadingFlow.tryEmit(true)
        launch(Dispatchers.IO) {
            try {
                settings.createWallet(enteredWords)
                settings.setRecoveryPhraseChecked(true)
                val args = Bundle().apply { putBoolean(RecoveryCheckFinishedController.KeyImport, true) }
                navigator.replace(ScreenParams(Screen.RecoveryCheckFinished, args, SlideChangeHandler(), SlideChangeHandler()), true)
            } catch (e: Exception) {
                if (e is TonApiException && e.error.message.startsWith("INVALID_MNEMONIC")) {
                    val alertDialog = AlertDialog.Builder(
                        title = Res.str(R.string.incorrect_words),
                        message = Res.str(R.string.incorrect_secret_words),
                        positiveButton = Res.str(R.string.ok) to DialogInterface.OnClickListener { dialog, _ -> dialog.dismiss() },
                    )
                    FlowBus.common.dispatch(FlowBusEvent.ShowAlertDialog(alertDialog))
                } else {
                    throw e
                }
            } finally {
                _isLoadingFlow.tryEmit(false)
            }
        }
    }

    fun onDoNotHaveClicked() {
        navigator.add(ScreenParams(Screen.DoNotHavePhrase, pushChangeHandler = SlideChangeHandler(), popChangeHandler = SlideChangeHandler()))
    }
}