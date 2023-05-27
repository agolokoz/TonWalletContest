package org.ton.wallet.screen.send.address

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.ton.wallet.R
import org.ton.wallet.lib.lists.ListItemClickListener
import org.ton.wallet.lib.lists.RecyclerAdapter
import org.ton.wallet.lib.lists.RecyclerHolder

class RecentAddressesAdapter(
    private val clickListener: ListItemClickListener<RecentAddressItem>
) : RecyclerAdapter<RecentAddressItem, RecentAddressesAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(parent, clickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItemAt(position))
    }

    class ViewHolder(
        parent: ViewGroup,
        private val clickListener: ListItemClickListener<RecentAddressItem>
    ) : RecyclerHolder<RecentAddressItem>(parent, R.layout.item_recent_address), View.OnClickListener {

        private val addressText: TextView = itemView.findViewById(R.id.itemRecentAddressText)
        private val dateText: TextView = itemView.findViewById(R.id.itemRecentDateText)

        init {
            itemView.setOnClickListener(this)
        }

        override fun bind(item: RecentAddressItem) {
            super.bind(item)
            addressText.text = item.shortAddress
            dateText.text = item.dateString
        }

        override fun onClick(v: View?) {
            clickListener.onItemClicked(item, bindingAdapterPosition)
        }
    }
}