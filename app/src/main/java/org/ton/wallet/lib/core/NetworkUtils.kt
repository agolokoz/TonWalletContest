package org.ton.wallet.lib.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

object NetworkUtils {

    private lateinit var connectivityManager: ConnectivityManager

    private val _stateFlow = MutableStateFlow(NetworkState(false))
    val stateFlow: Flow<NetworkState> = _stateFlow

    fun init(context: Context) {
        connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        _stateFlow.tryEmit(NetworkState(isNetworkConnected()))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } else {
            context.registerReceiver(networkStateReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        }
    }

    private fun isNetworkConnected(): Boolean {
        try {
            var networkInfo = connectivityManager.activeNetworkInfo
            if (networkInfo != null && (networkInfo.isConnectedOrConnecting || networkInfo.isAvailable)) {
                return true
            }
            networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
            if (networkInfo?.isConnectedOrConnecting == true) {
                return true
            }
            networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
            if (networkInfo?.isConnectedOrConnecting == true) {
                return true
            }
        } catch (e: Exception) {
            L.e(e)
        }
        return false
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: android.net.Network) {
            super.onAvailable(network)
            _stateFlow.tryEmit(NetworkState(true))
        }

        override fun onLost(network: android.net.Network) {
            super.onLost(network)
            _stateFlow.tryEmit(NetworkState(false))
        }
    }

    private val networkStateReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            _stateFlow.tryEmit(NetworkState(isNetworkConnected()))
        }
    }


    class NetworkState(
        val isAvailable: Boolean
    )
}