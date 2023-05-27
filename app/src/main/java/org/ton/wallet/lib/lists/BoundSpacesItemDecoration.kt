package org.ton.wallet.lib.lists

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

open class BoundSpacesItemDecoration(
    var head: Int = 0,
    var tail: Int = 0,
    start: Int = 0,
    top: Int = 0,
    end: Int = 0,
    bottom: Int = 0
) : SpacesItemDecoration(start, top, end, bottom) {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        val isHorizontal = (parent.layoutManager as? LinearLayoutManager)?.orientation == LinearLayoutManager.HORIZONTAL
        val position = parent.getChildAdapterPosition(view)
        if (position == 0) {
            if (isHorizontal) {
                outRect.left = head
            } else {
                outRect.top = head
            }
        } else if (position == (parent.adapter?.itemCount ?: 0) - 1) {
            if (isHorizontal) {
                outRect.right = tail
            } else {
                outRect.bottom = tail
            }
        }
    }
}