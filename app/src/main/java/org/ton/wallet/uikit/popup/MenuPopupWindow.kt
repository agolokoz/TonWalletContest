package org.ton.wallet.uikit.popup

import android.content.Context
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.updatePadding
import org.ton.wallet.R
import org.ton.wallet.lib.core.Res

class MenuPopupWindow(context: Context) : BasePopupWindow(LinearLayout(context), MATCH_PARENT, WRAP_CONTENT),
    View.OnClickListener {

    private val itemsLayout = view as LinearLayout
    private var items = emptyList<MenuPopupItem>()

    init {
        itemsLayout.orientation = LinearLayout.VERTICAL
        itemsLayout.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        isOutsideTouchable = true
    }

    override fun onClick(v: View) {
        val position = itemsLayout.indexOfChild(v)
        if (position in items.indices) {
            items[position].clickListener?.invoke()
            dismiss()
        }
    }

    fun setItems(menuItems: List<MenuPopupItem>): MenuPopupWindow {
        this.items = menuItems
        itemsLayout.removeAllViews()

        val horizontalPadding = Res.dp(20)
        val verticalPadding = Res.dp(13)
        val textColor = Res.color(R.color.text_primary)

        menuItems.forEachIndexed { index, item ->
            val textView = TextView(itemsLayout.context)
            textView.ellipsize = TextUtils.TruncateAt.END
            textView.maxLines = 1
            textView.setBackgroundResource(R.drawable.ripple_rect)
            textView.setOnClickListener(this)
            textView.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
            if (index == 0) {
                textView.updatePadding(top = Res.dp(15))
            }
            if (index == menuItems.size - 1) {
                textView.updatePadding(bottom = Res.dp(15))
            }
            textView.setTextColor(textColor)
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            textView.text = item.title
            textView.typeface = Res.font(R.font.roboto_regular)
            itemsLayout.addView(textView, MATCH_PARENT, WRAP_CONTENT)
        }
        return this
    }

    class MenuPopupItem(
        val title: CharSequence,
        val clickListener: (() -> Unit)? = null
    )
}