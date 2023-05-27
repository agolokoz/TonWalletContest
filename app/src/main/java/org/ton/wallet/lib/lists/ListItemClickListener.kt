package org.ton.wallet.lib.lists

fun interface ListItemClickListener<T> {

    fun onItemClicked(item: T, position: Int)
}