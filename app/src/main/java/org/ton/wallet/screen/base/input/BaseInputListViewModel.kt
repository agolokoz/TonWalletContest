package org.ton.wallet.screen.base.input

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.ton.wallet.lib.screen.viewmodel.BaseViewModel

abstract class BaseInputListViewModel : BaseViewModel() {

    private var allRecoveryWords = arrayOf<String>()

    private val _suggestWords = MutableStateFlow<List<String>>(emptyList())
    val suggestWordsFlow: StateFlow<List<String>> = _suggestWords

    val enteredWords = Array(settings.getRecoveryPhraseWordsCount()) { "" }

    init {
        launch(Dispatchers.Default) {
            allRecoveryWords = settings.getRecoveryWords()
        }
    }

    open fun setEnteredWord(position: Int, word: String) {
        enteredWords[position] = word
        launch(Dispatchers.Default) {
            if (word.isEmpty() || word.length < 2) {
                _suggestWords.tryEmit(emptyList())
            } else {
                val suggestedWords = allRecoveryWords.filter { it.startsWith(word, ignoreCase = true) }
                if (suggestedWords.isEmpty() || suggestedWords[0] == word) {
                    _suggestWords.tryEmit(emptyList())
                } else {
                    _suggestWords.tryEmit(suggestedWords)
                }
            }
        }
    }
}