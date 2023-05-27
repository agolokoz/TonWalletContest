package org.ton.wallet.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.ton.wallet.AppLifecycleDetector
import org.ton.wallet.BuildConfig
import org.ton.wallet.R
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.core.ThreadUtils
import pub.devrel.easypermissions.PermissionRequest

object NotificationUtils {

    const val ChannelId = BuildConfig.APPLICATION_ID
    const val IdTonConnectAction = 0

    private val isNeedNotificationPermission: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    private val _isNotificationsPermissionGrantedFlow = MutableStateFlow(!isNeedNotificationPermission)
    val isNotificationsPermissionGrantedFlow: StateFlow<Boolean> = _isNotificationsPermissionGrantedFlow

    private var isChannelCreated = false

    @SuppressLint("InlinedApi")
    fun init() {
        if (isNeedNotificationPermission) {
            AppLifecycleDetector.isAppForegroundFlow
                .onEach { isForeground ->
                    if (isForeground) {
                        checkPermissions()
                    }
                }
                .launchIn(ThreadUtils.appCoroutineScope)
        }
    }

    fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            _isNotificationsPermissionGrantedFlow.value =
                ContextCompat.checkSelfPermission(Res.context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("InlinedApi")
    fun getPermissionRequest(activity: Activity, requestCode: Int): PermissionRequest? {
        if (!isNeedNotificationPermission) {
            return null
        }
        return PermissionRequest.Builder(activity, requestCode, Manifest.permission.POST_NOTIFICATIONS)
            .setRationale(Res.str(R.string.notifications_permission_description))
            .setPositiveButtonText(Res.str(R.string.ok))
            .setNegativeButtonText(Res.str(R.string.cancel))
            .build()
    }

    fun showNotification(context: Context, id: Int, notification: Notification) {
        createChannelIfNeeded(context)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(id, notification)
        }
    }

    private fun createChannelIfNeeded(context: Context) {
        if (isChannelCreated || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val channel = NotificationChannelCompat.Builder(ChannelId, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(Res.str(R.string.app_name))
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }
}