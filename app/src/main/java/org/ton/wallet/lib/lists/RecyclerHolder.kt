package org.ton.wallet.lib.lists

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView

abstract class RecyclerHolder<T>(view: View) : RecyclerView.ViewHolder(view) {

    protected val context: Context
        get() = itemView.context

    protected var _item: T? = null
        private set
    protected val item: T
        get() = _item!!

    constructor(parent: ViewGroup, @LayoutRes layoutResId: Int): this(LayoutInflater.from(parent.context).inflate(layoutResId, parent, false))

    @CallSuper
    open fun bind(item: T) {
        this._item = item
    }

    open fun bindPayloads(payloads: List<Any>) = Unit

    protected fun <V : View> findViewById(@IdRes id: Int): V {
        return itemView.findViewById(id)
    }
}