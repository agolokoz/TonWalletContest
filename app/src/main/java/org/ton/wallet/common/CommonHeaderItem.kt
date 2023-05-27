package org.ton.wallet.common

import android.text.TextUtils
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.TextView
import org.ton.wallet.R
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.lists.RecyclerHolder

class CommonHeaderItem(val title: String)

class CommonHeaderViewHolder(
    parent: ViewGroup
) : RecyclerHolder<CommonHeaderItem>(TextView(parent.context)) {

    private val textView: TextView = itemView as TextView

    init {
        textView.ellipsize = TextUtils.TruncateAt.END
        textView.maxLines = 1
        textView.setPadding(Res.dp(20), Res.dp(20), Res.dp(20), Res.dp(4))
        textView.setTextColor(Res.color(R.color.blue))
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        textView.typeface = Res.font(R.font.roboto_medium)
    }

    override fun bind(item: CommonHeaderItem) {
        super.bind(item)
        textView.text = item.title
    }
}