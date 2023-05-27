package org.ton.wallet.common

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.ton.wallet.lib.lists.RecyclerAdapter
import org.ton.wallet.lib.lists.RecyclerHolder

class CommonAdapter(
    private val callback: CommonAdapterCallback
) : RecyclerAdapter<Any, RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewTypeHeader -> CommonHeaderViewHolder(parent)
            ViewTypeText -> CommonTextViewHolder(parent, callback)
            ViewTypeSwitch -> CommonSwitchViewHolder(parent, callback)
            else -> throw IllegalArgumentException("Unsupported viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CommonHeaderViewHolder -> holder.bind(getItemAt(position) as CommonHeaderItem)
            is CommonTextViewHolder -> holder.bind(getItemAt(position) as CommonTextItem)
            is CommonSwitchViewHolder -> holder.bind(getItemAt(position) as CommonSwitchItem)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        super.onBindViewHolder(holder, position, payloads)
        if (holder is RecyclerHolder<*>) {
            holder.bindPayloads(payloads)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItemAt(position)) {
            is CommonHeaderItem -> ViewTypeHeader
            is CommonTextItem -> ViewTypeText
            is CommonSwitchItem -> ViewTypeSwitch
            else -> super.getItemViewType(position)
        }
    }

    companion object {
        const val ViewTypeHeader = 0
        const val ViewTypeText = 1
        const val ViewTypeSwitch = 2
    }

    interface CommonAdapterCallback : CommonTextItemCallback,
        CommonSwitchItemCallback
}