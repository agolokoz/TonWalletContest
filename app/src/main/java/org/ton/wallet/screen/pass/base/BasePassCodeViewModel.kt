package org.ton.wallet.screen.pass.base

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import org.ton.wallet.R
import org.ton.wallet.data.model.PassCodeType
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.screen.viewmodel.BaseViewModel

abstract class BasePassCodeViewModel : BaseViewModel() {

    protected val isLoadingFlow = MutableStateFlow(false)
    protected val passCodeFlow = MutableStateFlow("")
    protected val passCodeTypeFlow = MutableStateFlow(PassCodeType.Pin4)

    protected val _errorEventFlow = MutableSharedFlow<Unit>(replay = 1)
    val errorEventFlow: Flow<Unit> = _errorEventFlow

    protected val passCodeTotalLength: Int
        get() = passCodeTypeFlow.value.rawValue

    val screenStateFlow = combine(
        passCodeFlow,
        passCodeTypeFlow,
        isLoadingFlow
    ) { passcode, type, isLoading ->
        PassCodeScreenState(
            title = title,
            subtitle = Res.str(R.string.enter_passcode_digits, type.rawValue),
            optionsText = optionsText,
            passCodeLength = type.rawValue,
            filledDotsCount = passcode.length,
            isLoading = isLoading
        )
    }

    abstract val title: String?

    abstract val optionsText: String?

    abstract fun onNumberEntered(number: String)

    fun onBackClicked() {
        navigator.back()
    }

    fun onBackSpaceClicked() {
        if (!isLoadingFlow.value) {
            passCodeFlow.value = passCodeFlow.value.dropLast(1)
        }
    }

    fun onClearClicked() {
        if (!isLoadingFlow.value) {
            passCodeFlow.value = ""
        }
    }

    fun onForDigitPassCodeClicked() {
        passCodeTypeFlow.value = PassCodeType.Pin4
        passCodeFlow.value = ""
    }

    fun onSixDigitPassCodeClicked() {
        passCodeTypeFlow.value = PassCodeType.Pin6
        passCodeFlow.value = ""
    }
}