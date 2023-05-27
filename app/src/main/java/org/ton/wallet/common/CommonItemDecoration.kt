package org.ton.wallet.common

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.ton.wallet.R
import org.ton.wallet.lib.core.Res

class CommonItemDecoration : RecyclerView.ItemDecoration() {

    private val dividerPaint = Paint()
    private val boundsRect = Rect()
    private val bottomSpace = Res.dp(20)

    init {
        dividerPaint.color = Res.color(R.color.input_disabled)
        dividerPaint.strokeWidth = Res.dp(0.5f)
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val adapter = parent.adapter ?: return
        val position = parent.getChildAdapterPosition(view)
        if (isNeedDrawDivider(adapter, position)) {
            outRect.bottom = dividerPaint.strokeWidth.toInt()
        } else if (position == adapter.itemCount - 1) {
            outRect.bottom = bottomSpace
        }
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val adapter = parent.adapter ?: return
        val layoutManager = parent.layoutManager ?: return

        if (parent.clipToPadding) {
            c.save()
            c.clipRect(
                parent.paddingLeft,
                parent.paddingTop,
                parent.width - parent.paddingRight,
                parent.height - parent.paddingBottom
            )
        }

        val childCount = parent.childCount
        val width = parent.width.toFloat()
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val childPosition = parent.getChildAdapterPosition(child)
            if (isNeedDrawDivider(adapter, childPosition)) {
                layoutManager.getDecoratedBoundsWithMargins(child, boundsRect)
                val y = boundsRect.bottom + child.translationY
                c.drawLine(0f, y, width, y, dividerPaint)
            }
        }

        if (parent.clipToPadding) {
            c.restore()
        }
    }

    private fun isNeedDrawDivider(adapter: RecyclerView.Adapter<*>, position: Int): Boolean {
        if (position == 0 || position == adapter.itemCount - 1) {
            return false
        }
        val currViewType = adapter.getItemViewType(position)
        val nextViewType = adapter.getItemViewType(position + 1)
        return currViewType == nextViewType || isTextViewType(currViewType) && isTextViewType(nextViewType)
    }

    private fun isTextViewType(viewType: Int): Boolean {
        return viewType == CommonAdapter.ViewTypeText || viewType == CommonAdapter.ViewTypeSwitch
    }
}