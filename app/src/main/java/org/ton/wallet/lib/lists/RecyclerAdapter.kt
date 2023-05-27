package org.ton.wallet.lib.lists

import android.annotation.SuppressLint
import androidx.recyclerview.widget.RecyclerView

abstract class RecyclerAdapter<T, VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {

    var items: List<T> = emptyList()
        private set

    override fun getItemCount(): Int {
        return items.size
    }

    fun getItemAt(position: Int): T {
        return items[position]
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setItems(items: List<T>) {
        this.items = items
        notifyDataSetChanged()
    }
}