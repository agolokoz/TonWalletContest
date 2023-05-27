package org.ton.wallet.screen.onboarding.recoverycheck

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.ton.wallet.R
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.core.ThreadUtils
import org.ton.wallet.lib.lists.ListItemClickListener
import org.ton.wallet.lib.lists.RecyclerAdapter
import org.ton.wallet.lib.lists.RecyclerHolder
import org.ton.wallet.uikit.popup.BasePopupWindow

class SuggestWordsPopupWindow(
    context: Context,
    clickListener: ListItemClickListener<String>
) : BasePopupWindow(RecyclerView(context)) {

    private val adapter = Adapter(clickListener)
    private val recyclerView = view as RecyclerView

    private val viewHeight: Int

    init {
        // for measure
        adapter.setItems(listOf(""))
        recyclerView.adapter = adapter
        recyclerView.isNestedScrollingEnabled = false
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)

        val widthSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        val heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        rootView.measure(widthSpec, heightSpec)
        viewHeight = rootView.measuredHeight

        contentView.setOnClickListener {
            dismiss()
        }
        ThreadUtils.postOnMain {
            width = Res.dimenInt(R.dimen.phrase_word_width) + backgroundPaddings.left + backgroundPaddings.right
        }
    }

    fun setWords(words: List<String>) {
        adapter.setItems(words.toList())
        recyclerView.scrollToPosition(0)
    }


    private class Adapter(
        private val itemClickListener: ListItemClickListener<String>
    ) : RecyclerAdapter<String, Adapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(parent.context, itemClickListener)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItemAt(position))
        }

        class ViewHolder(
            context: Context,
            private val itemClickListener: ListItemClickListener<String>
        ) : RecyclerHolder<String>(TextView(context)), View.OnClickListener {

            private val textView = itemView as TextView

            init {
                textView.layoutParams = RecyclerView.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                textView.setBackgroundResource(R.drawable.ripple_rect)
                textView.setLineSpacing(Res.sp(2f), 1f)
                textView.setOnClickListener(this)
                val horizontalPadding = Res.dp(16)
                val verticalPadding = Res.dp(12)
                textView.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                textView.setTextColor(Res.color(R.color.text_primary))
                textView.typeface = Res.font(R.font.roboto_regular)
            }

            override fun bind(item: String) {
                super.bind(item)
                textView.text = item
            }

            override fun onClick(v: View?) {
                itemClickListener.onItemClicked(item, bindingAdapterPosition)
            }
        }
    }
}