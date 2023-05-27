package org.ton.wallet.util

import android.graphics.drawable.Drawable
import androidx.activity.result.contract.ActivityResultContracts
import org.ton.wallet.uikit.dialog.AlertDialog

sealed class FlowBusEvent {

    object AccountReloaded : FlowBusEvent()

    object HideSnackBar : FlowBusEvent()

    class PickMediaContent(
        val mediaType: ActivityResultContracts.PickVisualMedia.VisualMediaType
    ) : FlowBusEvent()

    class ShowAlertDialog(
        val dialogBuilder: AlertDialog.Builder
    ) : FlowBusEvent()

    class ShowSnackBar(
        val title: String? = null,
        val message: String? = null,
        val drawable: Drawable? = null,
        val durationMs: Long = 3000L
    ) : FlowBusEvent()
}