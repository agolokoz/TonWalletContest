package org.ton.wallet.screen.connect.approve

import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.ton.wallet.AppComponentsProvider
import org.ton.wallet.data.model.TonConnect
import org.ton.wallet.data.repo.TonConnectRepository
import org.ton.wallet.domain.UseCases
import org.ton.wallet.lib.core.ext.toUriSafe
import org.ton.wallet.lib.screen.viewmodel.BaseViewModel
import org.ton.wallet.util.LinkAction
import org.ton.wallet.util.LinkUtils

class TonConnectApproveViewModel(args: Bundle) : BaseViewModel() {

    private lateinit var action: LinkAction.TonConnectAction

    private var manifest: TonConnect.Manifest? = null

    private val _stateFlow = MutableStateFlow(TonConnectApproveState(true))
    val stateFlow: Flow<TonConnectApproveState> = _stateFlow

    private val tonConnect: TonConnectRepository
        get() = AppComponentsProvider.tonConnectRepository

    init {
        val url = args.getString(TonConnectApproveController.ArgumentKeyUrl)
            ?: throw IllegalArgumentException("${TonConnectApproveController.ArgumentKeyUrl} must be non null")
        launch(Dispatchers.IO) {
            action = LinkUtils.parseLink(url) as? LinkAction.TonConnectAction
                ?: throw IllegalArgumentException("${TonConnectApproveController.ArgumentKeyUrl} must contain TonConnectAction")
            val manifestUrl = action.request.manifestUrl
            try {
                val manifest = tonConnect.getManifestInfo(manifestUrl)!!
                val state = TonConnectApproveState(
                    isDataLoading = false,
                    accountAddress = UseCases.currentAccountAddressFlow.first(),
                    accountVersion = settings.accountTypeFlow.value.getString(),
                    appName = manifest.name,
                    appIconUrl = manifest.iconUrl,
                    appHost = manifest.url.toUriSafe()?.host ?: manifest.name,
                    connectionState = TonConnectApproveState.ConnectionDefault,
                )
                this@TonConnectApproveViewModel.manifest = manifest
                _stateFlow.tryEmit(state)
            } catch (e: Exception) {
                navigator.onBackPressed()
                throw e
            }
        }
    }

    fun onCloseClicked() {
        navigator.onBackPressed()
    }

    fun onConnectClicked() {
        _stateFlow.value = _stateFlow.value.copy(connectionState = TonConnectApproveState.ConnectionInProgress)
        launch(Dispatchers.IO) {
            try {
                UseCases.tonConnectOpenConnection(action)
                _stateFlow.value = _stateFlow.value.copy(connectionState = TonConnectApproveState.ConnectionConnected)
                withContext(Dispatchers.Main) {
                    delay(1000L)
                    navigator.onBackPressed()
                }
            } catch (e: Exception) {
                _stateFlow.value = _stateFlow.value.copy(connectionState = TonConnectApproveState.ConnectionDefault)
                throw e
            }
        }
    }
}