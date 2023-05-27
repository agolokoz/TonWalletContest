package org.ton.wallet.lib.lists

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.ton.wallet.lib.core.Res

open class SpacesItemDecoration(
    protected val start: Int = 0,
    protected val top: Int = 0,
    protected val end: Int = 0,
    protected val bottom: Int = 0
) : RecyclerView.ItemDecoration() {

    constructor(space: Int): this(space, space, space, space)

    constructor(vertical: Int, horizontal: Int): this(horizontal, vertical, horizontal, vertical)

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        outRect.top = top
        outRect.bottom = bottom
        outRect.left = if (Res.isRtl) end else start
        outRect.right = if (Res.isRtl) start else end
    }
}