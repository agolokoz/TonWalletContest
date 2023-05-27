package org.ton.wallet.uikit.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import org.ton.wallet.R
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.core.ext.toActivitySafe
import kotlin.math.roundToInt

class AppToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val paint = Paint()
    private val shadowDrawable = Res.drawable(R.drawable.toolbar_shadow)
    private val shadowHeight = Res.dp(3)
    private val imageView = ImageView(context)
    private val titleText = TextView(context)

    init {
        gravity = Gravity.CENTER_VERTICAL
        paint.color = (background as? ColorDrawable)?.color ?: Color.TRANSPARENT
        background = null

        imageView.setBackgroundResource(R.drawable.ripple_oval_dark)
        imageView.setImageResource(R.drawable.ic_back_24)
        imageView.setOnClickListener {
            (context.toActivitySafe() as? AppCompatActivity)?.onBackPressedDispatcher?.onBackPressed()
        }
        imageView.scaleType = ImageView.ScaleType.CENTER
        val imageSize = Res.dp(48)
        val imageViewLayoutParams = LayoutParams(imageSize, imageSize)
        imageViewLayoutParams.marginStart = Res.dp(4)
        imageViewLayoutParams.bottomMargin = shadowHeight / 2
        addView(imageView, imageViewLayoutParams)

        titleText.ellipsize = TextUtils.TruncateAt.END
        titleText.maxLines = 1
        titleText.setTextColor(Res.color(R.color.text_primary))
        titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        titleText.typeface = Res.font(R.font.roboto_medium)
        val titleLayoutParams = LayoutParams(0, WRAP_CONTENT)
        titleLayoutParams.marginStart = Res.dp(20)
        titleLayoutParams.marginEnd = Res.dp(20)
        titleLayoutParams.bottomMargin = shadowHeight / 2
        titleLayoutParams.weight = 1f
        addView(titleText, titleLayoutParams)

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AppToolbar)
        try {
            setTitle(typedArray.getString(R.styleable.AppToolbar_android_title))
            setTitleColor(typedArray.getColor(R.styleable.AppToolbar_titleColor, titleText.currentTextColor))
            val tintColor = typedArray.getColor(R.styleable.AppToolbar_tintColor, Color.TRANSPARENT)
            if (tintColor != Color.TRANSPARENT) {
                setTintColor(tintColor)
            }
            setShadowAlpha(typedArray.getFloat(R.styleable.AppToolbar_shadowAlpha, 1f))
            setImageVisible(typedArray.getBoolean(R.styleable.AppToolbar_imageVisible, true))
        } finally {
            typedArray.recycle()
        }
        setWillNotDraw(false)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val heightSpecMode = MeasureSpec.getMode(heightMeasureSpec)
        val actionBarSizeWithPaddings = Res.dimenAttr(android.R.attr.actionBarSize) + shadowHeight + paddingTop + paddingBottom
        val newHeightMeasureSpec = when (heightSpecMode) {
            MeasureSpec.UNSPECIFIED -> MeasureSpec.makeMeasureSpec(actionBarSizeWithPaddings, MeasureSpec.EXACTLY)
            MeasureSpec.AT_MOST -> MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec).coerceAtMost(actionBarSizeWithPaddings), MeasureSpec.EXACTLY)
            else -> heightMeasureSpec
        }
        super.onMeasure(widthMeasureSpec, newHeightMeasureSpec)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        shadowDrawable.setBounds(0, h - shadowHeight, w, h)
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (shadowDrawable.alpha > 0) {
            shadowDrawable.draw(canvas)
        }
        if (paint.color != Color.TRANSPARENT) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat() - shadowHeight, paint)
        }
        super.dispatchDraw(canvas)
    }

    fun setImageVisible(isVisible: Boolean) {
        imageView.isVisible = isVisible
    }

    fun setTintColor(@ColorInt color: Int) {
        imageView.setColorFilter(color)
    }

    fun setTitle(title: CharSequence?) {
        titleText.text = title
    }

    fun setTitleColor(@ColorInt color: Int) {
        titleText.setTextColor(color)
    }

    fun setTitleAlpha(alpha: Float) {
        titleText.alpha = alpha
    }

    fun setShadowAlpha(alpha: Float) {
        shadowDrawable.alpha = (alpha * 255).roundToInt()
        invalidate()
    }
}