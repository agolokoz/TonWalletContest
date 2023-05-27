package org.ton.wallet.uikit.drawable

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

class EmptyDrawable(
    private val width: Int,
    private val height: Int
) : Drawable() {

    override fun draw(canvas: Canvas) = Unit

    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    override fun getOpacity(): Int {
        return PixelFormat.TRANSPARENT
    }

    override fun getIntrinsicWidth(): Int {
        return width
    }

    override fun getIntrinsicHeight(): Int {
        return height
    }
}