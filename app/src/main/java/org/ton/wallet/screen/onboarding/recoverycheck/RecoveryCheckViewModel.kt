package org.ton.wallet.screen.onboarding.recoverycheck

import android.app.Activity
import android.content.DialogInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.ton.wallet.R
import org.ton.wallet.lib.core.AndroidUtils
import org.ton.wallet.lib.core.KeyboardUtils
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.navigator.Screen
import org.ton.wallet.lib.navigator.ScreenParams
import org.ton.wallet.lib.screen.changehandler.SlideChangeHandler
import org.ton.wallet.screen.base.input.BaseInputListViewModel
import org.ton.wallet.uikit.dialog.AlertDialog
import org.ton.wallet.util.FlowBus
import org.ton.wallet.util.FlowBusEvent

class RecoveryCheckViewModel : BaseInputListViewModel() {

    private val errorStates = BooleanArray(WordsCount)

    private val _errorStatesFlow = MutableStateFlow(errorStates)
    val errorStatesFlow: Flow<BooleanArray> = _errorStatesFlow

    val subtitle: String
    val wordPositions = Array(WordsCount) { 0 }

    init {
        val wordsCount = settings.getRecoveryPhraseWordsCount()
        val range = wordsCount / wordPositions.size
        for (i in wordPositions.indices) {
            wordPositions[i] = AndroidUtils.random.nextInt(range * i, range * (i + 1))
        }
        subtitle = Res.str(R.string.lets_check_recovery_phrase,
            wordPositions[0] + 1, wordPositions[1] + 1, wordPositions[2] + 1)
    }

    override fun setEnteredWord(position: Int, word: String) {
        super.setEnteredWord(position, word)
        if (errorStates[position]) {
            val errorStates = getErrorStatesCopy()
            errorStates[position] = false
            _errorStatesFlow.tryEmit(errorStates)
        }
    }

    fun onContinueClicked(activity: Activity) {
        var hasError = false
        val errorStates = getErrorStatesCopy()
        val recoveryPhrase = settings.getRecoveryPhrase()
        for (i in 0 until WordsCount) {
            errorStates[i] = recoveryPhrase[wordPositions[i]] != enteredWords[i]
            if (errorStates[i]) {
                hasError = true
            }
        }
//        if (hasError && !BuildConfig.DEBUG) {
        if (hasError) {
            val alertDialog = AlertDialog.Builder(
                title = Res.str(R.string.incorrect_words),
                message = Res.str(R.string.incorrect_words_description),
                positiveButton = Res.str(R.string.try_again) to DialogInterface.OnClickListener { dialog, _ ->
                    dialog.dismiss()
                },
                negativeButton = Res.str(R.string.see_words) to DialogInterface.OnClickListener { dialog, _ ->
                    dialog.dismiss()
                    navigator.back()
                }
            )
            FlowBus.common.dispatch(FlowBusEvent.ShowAlertDialog(alertDialog))
            _errorStatesFlow.value = errorStates
        } else {
            settings.setRecoveryPhraseChecked(true)
            val screenParams = ScreenParams(Screen.RecoveryCheckFinished, SlideChangeHandler(), SlideChangeHandler())
            KeyboardUtils.hideKeyboard(activity.window) {
                navigator.replace(screenParams, true)
            }
        }
    }

    private fun getErrorStatesCopy(): BooleanArray {
        return errorStates.copyOf()
    }

    private companion object {
        private const val WordsCount = 3
    }
}