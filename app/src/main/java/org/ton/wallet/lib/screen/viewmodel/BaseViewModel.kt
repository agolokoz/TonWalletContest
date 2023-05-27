package org.ton.wallet.lib.screen.viewmodel

import android.os.Bundle
import com.bluelinelabs.conductor.ControllerChangeType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.ton.wallet.ActionHandler
import org.ton.wallet.AppComponentsProvider
import org.ton.wallet.R
import org.ton.wallet.data.repo.AccountsRepository
import org.ton.wallet.data.repo.AuthRepository
import org.ton.wallet.data.repo.SettingsRepository
import org.ton.wallet.data.repo.TransactionsRepository
import org.ton.wallet.lib.core.L
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.navigator.Navigator
import org.ton.wallet.lib.screen.controller.ResultListener
import org.ton.wallet.util.ErrorHandler
import org.ton.wallet.util.FlowBus
import org.ton.wallet.util.FlowBusEvent
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

abstract class BaseViewModel : ResultListener {

    protected val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    protected val actionHandler: ActionHandler get() = AppComponentsProvider.actionHandler
    protected val navigator: Navigator get() = AppComponentsProvider.navigator
    protected val auth: AuthRepository get() = AppComponentsProvider.authRepository
    protected val accounts: AccountsRepository get() = AppComponentsProvider.accountsRepository
    protected val settings: SettingsRepository get() = AppComponentsProvider.settingsRepository
    protected val transactions: TransactionsRepository get() = AppComponentsProvider.transactionsRepository

    private val _setResultEventFlow = MutableSharedFlow<Pair<String, Bundle>>(replay = 1)
    val setResultEventFlow: Flow<Pair<String, Bundle>> = _setResultEventFlow

    var retainType = ViewModelRetainType.Controller
        private set

    open fun onResume() = Unit

    open fun onScreenChange(isStarted: Boolean, changeType: ControllerChangeType) = Unit

    open fun onPause() = Unit

    open fun onDestroyView() = Unit

    open fun onDestroy() {
        viewModelScope.cancel()
    }

    fun launch(
        dispatcher: CoroutineDispatcher,
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        val coroutineContext = dispatcher + context + exceptionHandler
        return viewModelScope.launch(coroutineContext, start, block)
    }

    fun setResult(code: String, data: Bundle = Bundle.EMPTY) {
        viewModelScope.launch {
            _setResultEventFlow.emit(code to data)
        }
    }

    override fun onResult(code: String, data: Bundle) = Unit

    open fun onPermissionsGranted(requestCode: Int, permissions: MutableList<String>) = Unit

    open fun onPermissionsDenied(requestCode: Int, permissions: MutableList<String>) = Unit

    open fun onBackPressed(): Boolean {
        return false
    }

    internal fun setRetainType(retainType: ViewModelRetainType) {
        this.retainType = retainType
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        val messageText = ErrorHandler.getErrorMessage(throwable)
        messageText?.let { msg ->
            val showSnackBar = FlowBusEvent.ShowSnackBar(
                title = Res.str(R.string.error),
                message = msg,
                drawable = Res.drawable(R.drawable.ic_warning_32)
            )
            FlowBus.common.dispatch(showSnackBar)
        }
        L.e(throwable)
    }
}