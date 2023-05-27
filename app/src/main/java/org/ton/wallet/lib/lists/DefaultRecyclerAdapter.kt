package org.ton.wallet.lib.lists

abstract class DefaultRecyclerAdapter<T, VH : RecyclerHolder<T>> : RecyclerAdapter<T, VH>() {

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItemAt(position))
    }

    override fun onBindViewHolder(holder: VH, position: Int, payloads: MutableList<Any>) {
        super.onBindViewHolder(holder, position, payloads)
        holder.bindPayloads(payloads)
    }
}