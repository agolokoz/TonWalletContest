package org.ton.wallet.screen.scanqr

import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.Px
import androidx.core.graphics.ColorUtils
import org.ton.wallet.R
import org.ton.wallet.lib.core.Res

class ScanQrForegroundDrawable : Drawable() {

    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val cutOutPath = Path()
    private val cutOutRect = RectF()
    private val cutOutRadius = Res.dp(6f)
    private val cornerSize = Res.dp(26f)
    private val cornerPath = Path()

    private val backgroundColor = ColorUtils.setAlphaComponent(Res.color(R.color.common_black), 127)

    @Px
    private var cutOutSize = Res.dp(200)

    init {
        cornerPaint.color = Res.color(R.color.common_white)
        cornerPaint.style = Paint.Style.STROKE
        cornerPaint.strokeCap = Paint.Cap.ROUND
        cornerPaint.strokeWidth = Res.dp(3.5f)
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        cutOutRect.set((bounds.width() - cutOutSize) * 0.5f, (bounds.height() - cutOutSize) * 0.5f, (bounds.width() + cutOutSize) * 0.5f, (bounds.height() + cutOutSize) * 0.5f)
        invalidatePath()
    }

    override fun draw(canvas: Canvas) {
        canvas.save()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            canvas.clipOutPath(cutOutPath)
        } else {
            canvas.clipPath(cutOutPath, Region.Op.DIFFERENCE)
        }
        canvas.drawColor(backgroundColor)
        canvas.restore()

        canvas.drawPath(cornerPath, cornerPaint)
    }

    override fun setAlpha(alpha: Int) {
        cornerPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        cornerPaint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSPARENT
    }

    fun setCutoutSize(@Px size: Int) {
        this.cutOutSize = size
        invalidatePath()
        invalidateSelf()
    }

    private fun invalidatePath() {
        cornerPath.reset()
        cornerPath.moveTo(cutOutRect.left, cutOutRect.top + cornerSize)
        cornerPath.rLineTo(0f, -(cornerSize - cutOutRadius))
        cornerPath.arcTo(cutOutRect.left, cutOutRect.top, cutOutRect.left + cutOutRadius * 2, cutOutRect.top + cutOutRadius * 2, 180f, 90f, true)
        cornerPath.rLineTo(cornerSize - cutOutRadius, 0f)

        cornerPath.moveTo(cutOutRect.right - cornerSize, cutOutRect.top)
        cornerPath.rLineTo(cornerSize - cutOutRadius, 0f)
        cornerPath.arcTo(cutOutRect.right - cutOutRadius * 2, cutOutRect.top, cutOutRect.right, cutOutRect.top + cutOutRadius * 2, 270f, 90f, true)
        cornerPath.rLineTo(0f, cornerSize - cutOutRadius)

        cornerPath.moveTo(cutOutRect.right, cutOutRect.bottom - cornerSize)
        cornerPath.rLineTo(0f, cornerSize - cutOutRadius)
        cornerPath.arcTo(cutOutRect.right - cutOutRadius * 2, cutOutRect.bottom - cutOutRadius * 2, cutOutRect.right, cutOutRect.bottom, 0f, 90f, true)
        cornerPath.rLineTo(-(cornerSize - cutOutRadius), 0f)

        cornerPath.moveTo(cutOutRect.left + cornerSize, cutOutRect.bottom)
        cornerPath.rLineTo(-(cornerSize - cutOutRadius), 0f)
        cornerPath.arcTo(cutOutRect.left, cutOutRect.bottom - cutOutRadius * 2, cutOutRect.left + cutOutRadius * 2, cutOutRect.bottom, 90f, 90f, true)
        cornerPath.rLineTo(0f, -(cornerSize - cutOutRadius))

        cutOutPath.reset()
        cutOutPath.addRoundRect(cutOutRect, cutOutRadius, cutOutRadius, Path.Direction.CW)
    }
}