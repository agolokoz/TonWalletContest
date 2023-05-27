package org.ton.wallet.common

import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.ton.wallet.R
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.lists.RecyclerHolder
import org.ton.wallet.uikit.view.SwitchView

class CommonSwitchItem(
    val id: Int,
    val title: String,
    var isChecked: Boolean
)

class CommonSwitchCheckChangePayload(val isChecked: Boolean)

class CommonSwitchViewHolder(
    parent: ViewGroup,
    private val callback: CommonSwitchItemCallback
) : RecyclerHolder<CommonSwitchItem>(FrameLayout(parent.context)), View.OnClickListener {

    private val textView = TextView(parent.context)
    private val switchItem = SwitchView(parent.context)

    init {
        val rootLayout = itemView as FrameLayout
        rootLayout.layoutParams = RecyclerView.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        rootLayout.setBackgroundResource(R.drawable.ripple_rect)
        rootLayout.setOnClickListener(this)
        rootLayout.setPadding(Res.dp(20), Res.dp(16), Res.dp(18), Res.dp(16))

        textView.setTextColor(Res.color(R.color.common_black))
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        textView.typeface = Res.font(R.font.roboto_regular)
        val textViewLayoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.CENTER_VERTICAL)
        textViewLayoutParams.marginEnd = SwitchView.Width + Res.dp(8)
        rootLayout.addView(textView, textViewLayoutParams)
        rootLayout.addView(switchItem, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER_VERTICAL or Gravity.END))
    }

    override fun bind(item: CommonSwitchItem) {
        super.bind(item)
        textView.text = item.title
        switchItem.setChecked(item.isChecked, false)
    }

    override fun bindPayloads(payloads: List<Any>) {
        super.bindPayloads(payloads)
        payloads.forEach { payload ->
            if (payload is CommonSwitchCheckChangePayload) {
                item.isChecked = payload.isChecked
                switchItem.setChecked(payload.isChecked, true)
            }
        }
    }

    override fun onClick(v: View?) {
        callback.onSwitchItemClicked(item)
    }
}

interface CommonSwitchItemCallback {

    fun onSwitchItemClicked(item: CommonSwitchItem) = Unit
}