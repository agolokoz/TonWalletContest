package org.ton.wallet

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.ton.wallet.lib.core.BrowserUtils

object AppLifecycleDetector : ActivityLifecycleCallbacks {

    var isImagePickerStarted = false

    private var startedActivitiesCount = 0
    private var isActivityChangingConfigurations = false

    private val isAppActivitiesRunning: Boolean
        get() = BrowserUtils.isShowing || isImagePickerStarted

    private val _isAppForegroundFlow = MutableStateFlow(false)
    val isAppForegroundFlow: StateFlow<Boolean> = _isAppForegroundFlow

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        _isAppForegroundFlow.value = true
    }

    override fun onActivityStarted(activity: Activity) {
        startedActivitiesCount++
        if (startedActivitiesCount == 1 && !isActivityChangingConfigurations && !isAppActivitiesRunning) {
            _isAppForegroundFlow.value = true
        }
    }

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) {
        isActivityChangingConfigurations = activity.isChangingConfigurations
        if (!isAppActivitiesRunning) {
            --startedActivitiesCount
            if (startedActivitiesCount == 0 && !isActivityChangingConfigurations) {
                _isAppForegroundFlow.value = false
            }
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}