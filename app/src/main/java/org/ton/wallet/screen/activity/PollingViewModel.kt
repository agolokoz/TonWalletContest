package org.ton.wallet.screen.activity

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.ton.wallet.AppComponentsProvider
import org.ton.wallet.data.DefaultPrefsKeys
import org.ton.wallet.domain.UseCases
import org.ton.wallet.lib.core.L
import org.ton.wallet.lib.screen.viewmodel.BaseViewModel
import org.ton.wallet.util.FlowBus
import org.ton.wallet.util.FlowBusEvent

class PollingViewModel : BaseViewModel() {

    private val preferences = AppComponentsProvider.defaultPreferences

    fun start() {
        initPolling("fiatPrices", 60_000) {
            AppComponentsProvider.pricesRepository.refreshPrices()
        }
        initPolling("accountState", 60_000) {
            try {
                UseCases.getTransactions(true)
                FlowBus.common.dispatch(FlowBusEvent.AccountReloaded)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun initPolling(name: String, intervalMillis: Long, action: suspend () -> Unit) {
        launch(Dispatchers.IO) {
            val lastUpdateTimeMillis = preferences.getLong(getPreferencesKey(name), 0)
            val timeSinceLastUpdateMillis = System.currentTimeMillis() - lastUpdateTimeMillis
            var delayMillis =
                if (timeSinceLastUpdateMillis > intervalMillis) 0
                else intervalMillis - timeSinceLastUpdateMillis
            var failureDelay = DefaultFailureDelayMillis
            while (isActive) {
                runCatching {
                    delay(delayMillis)
                    L.d("polling $name")
                    action.invoke()
                }.onFailure { throwable ->
                    L.e(throwable)
                    delay(failureDelay)
                    failureDelay *= 2
                }.onSuccess {
                    failureDelay = DefaultFailureDelayMillis
                    delayMillis = intervalMillis
                    preferences.edit().putLong(getPreferencesKey(name), System.currentTimeMillis()).apply()
                }
            }
        }
    }

    private fun getPreferencesKey(key: String): String {
        return DefaultPrefsKeys.PollingPrefix + key
    }

    private companion object {
        private const val DefaultFailureDelayMillis = 1_000L
    }
}