package org.ton.wallet.lib.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import kotlin.random.Random

object AndroidUtils {

    val random by lazy { Random(SystemClock.elapsedRealtime()) }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", context.packageName, null)
        context.startActivity(intent)
    }

    fun shareText(context: Context, text: String, chooserTitle: String = "", ) {
        val sendIntent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, text)
        startChooserIntent(context, sendIntent, chooserTitle)
    }

    private fun startChooserIntent(context: Context, intent: Intent, title: String) {
        try {
            val chooserIntent = Intent.createChooser(intent, title)
            chooserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (intent.extras?.containsKey(Intent.EXTRA_STREAM) == true) {
                val resolvedActivities = context.packageManager.queryIntentActivities(chooserIntent, PackageManager.MATCH_DEFAULT_ONLY)
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                for (resolveInfo in resolvedActivities) {
                    val packageName = resolveInfo.activityInfo.packageName
                    context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            L.e(e)
        }
    }
}