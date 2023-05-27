package org.ton.wallet.uikit.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.SystemClock
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.graphics.ColorUtils
import androidx.core.math.MathUtils
import org.ton.wallet.R
import org.ton.wallet.lib.core.L
import org.ton.wallet.lib.core.Res
import java.lang.reflect.Field

@SuppressLint("AppCompatCustomView")
class AppEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatEditText(context, attrs), View.OnFocusChangeListener {

    private val focusChangeListeners = mutableListOf<OnFocusChangeListener>()
    private var selectionChangedListeners: MutableList<SelectionChangedListener>? = mutableListOf()

    init {
        background = BackgroundDrawable()
        onFocusChangeListener = this

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            textCursorDrawable = Res.drawable(R.drawable.text_cursor)
        } else {
            var field: Field? = null
            try {
                field = TextView::class.java.getDeclaredField("mCursorDrawableRes")
                field.isAccessible = true
                field.set(this, R.drawable.text_cursor)
            } catch (e: Exception) {
                L.e(e)
            } finally {
                field?.isAccessible = false
            }
        }

        highlightColor = ColorUtils.setAlphaComponent(Res.color(R.color.blue), 80)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val colorFilter = PorterDuffColorFilter(Res.color(R.color.blue), PorterDuff.Mode.SRC_ATOP)
            textSelectHandle?.colorFilter = colorFilter
            textSelectHandleLeft?.colorFilter = colorFilter
            textSelectHandleRight?.colorFilter = colorFilter
        }

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AppEditText)
        try {
            background = typedArray.getDrawable(R.styleable.AppEditText_android_background) ?: BackgroundDrawable()
            setHintTextColor(typedArray.getColor(R.styleable.AppEditText_android_textColorHint, Res.color(R.color.text_hint)))
            setTextSize(TypedValue.COMPLEX_UNIT_PX, typedArray.getDimension(R.styleable.AppEditText_android_textSize, Res.sp(16f)))
        } finally {
            typedArray.recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(measuredWidth + Res.dp(2), measuredHeight)
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        // this check for null needs because this method called before object field initialized
        selectionChangedListeners?.let { listeners ->
            for (i in 0 until listeners.size) {
                listeners[i].onSelectionChanged(selStart, selEnd)
            }
        }
    }

    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        (background as? BackgroundDrawable)?.startAnimation(hasFocus)
        focusChangeListeners.forEach { it.onFocusChange(v, hasFocus) }
    }

    fun addOnFocusChangeListener(listener: OnFocusChangeListener) {
        focusChangeListeners.add(listener)
    }

    fun removeOnFocusChangeListener(listener: OnFocusChangeListener) {
        focusChangeListeners.remove(listener)
    }

    fun addSelectionChangedListener(listener: SelectionChangedListener) {
        selectionChangedListeners?.add(listener)
    }

    fun removeSelectionChangedListener(listener: SelectionChangedListener) {
        selectionChangedListeners?.remove(listener)
    }

    fun setErrorState(isError: Boolean) {
        (background as? BackgroundDrawable)?.setError(isError)
    }

    fun interface SelectionChangedListener {

        fun onSelectionChanged(start: Int, end: Int)
    }

    private class BackgroundDrawable : Drawable() {

        private val lineBackPaint = Paint()
        private val lineFrontPaint = Paint()
        private val lineErrorPaint = Paint()

        private var focusReceivedTimestamp = 0L
        private var isError = false

        init {
            lineBackPaint.color = Res.color(R.color.input_disabled)
            lineBackPaint.strokeWidth = Res.dp(1f)

            lineFrontPaint.color = Res.color(R.color.blue)
            lineFrontPaint.strokeWidth = Res.dp(2f)

            lineErrorPaint.color = Res.color(R.color.text_error)
            lineErrorPaint.strokeWidth = Res.dp(2f)
        }

        override fun draw(canvas: Canvas) {
            val halfWidth = lineFrontPaint.strokeWidth * 0.5f
            val y = bounds.height() - halfWidth

            if (isError) {
                canvas.drawLine(halfWidth, y, bounds.width().toFloat() - halfWidth, y, lineErrorPaint)
            } else {
                canvas.drawLine(halfWidth, y, bounds.width().toFloat() - halfWidth, y, lineBackPaint)
                if (focusReceivedTimestamp != 0L) {
                    var progress =
                        if (focusReceivedTimestamp > 0L) (SystemClock.elapsedRealtime() - focusReceivedTimestamp).toFloat() / LINE_SHOW_ANIMATION_DURATION
                        else 1f - (SystemClock.elapsedRealtime() + focusReceivedTimestamp).toFloat() / LINE_HIDE_ANIMATION_DURATION
                    progress = MathUtils.clamp(progress, 0f, 1f)
                    val xCenter = bounds.width().toFloat() * 0.5f
                    canvas.drawLine(
                        xCenter * (1f - progress) + halfWidth, y,
                        xCenter + (bounds.width().toFloat() - xCenter) * progress - halfWidth, y,
                        lineFrontPaint
                    )
                    if (progress <= 0.0f) {
                        focusReceivedTimestamp = 0L
                        invalidateSelf()
                    } else if (progress < 1.0f) {
                        invalidateSelf()
                    }
                }
            }
        }

        override fun setAlpha(alpha: Int) {
            lineBackPaint.alpha = alpha
            lineFrontPaint.alpha = alpha
            lineErrorPaint.alpha = alpha
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            lineBackPaint.colorFilter = colorFilter
            lineFrontPaint.colorFilter = colorFilter
            lineErrorPaint.colorFilter = colorFilter
        }

        override fun getOpacity(): Int {
            return PixelFormat.TRANSPARENT
        }

        fun startAnimation(isFocused: Boolean) {
            focusReceivedTimestamp =
                if (isFocused) SystemClock.elapsedRealtime()
                else -SystemClock.elapsedRealtime()
        }

        fun setError(isError: Boolean) {
            if (this.isError != isError) {
                this.isError = isError
                invalidateSelf()
            }
        }

        private companion object {
            private const val LINE_SHOW_ANIMATION_DURATION = 150L
            private const val LINE_HIDE_ANIMATION_DURATION = 100L
        }
    }
}