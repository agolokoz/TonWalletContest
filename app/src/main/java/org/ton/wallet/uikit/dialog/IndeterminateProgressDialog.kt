package org.ton.wallet.uikit.dialog

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import org.ton.wallet.R
import org.ton.wallet.lib.core.Res
import org.ton.wallet.uikit.drawable.IndeterminateProgressDrawable

class IndeterminateProgressDialog(
    context: Context,
    isCancelable: Boolean
) : BaseDialog(context, isCancelable) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val contentView = ImageView(context)
        contentView.background = Res.drawableColored(R.drawable.bkg_rect_rounded_18dp, Res.color(R.color.progress_dialog_background))
        contentView.scaleType = ImageView.ScaleType.CENTER
        val drawable = IndeterminateProgressDrawable(Res.dp(48))
        drawable.setColor(Res.color(R.color.progress_dialog_color))
        drawable.setStrokeWidth(Res.dp(3f))
        contentView.setImageDrawable(drawable)

        setView(contentView, FrameLayout.LayoutParams(Res.dp(86), Res.dp(86), Gravity.CENTER))
    }
}