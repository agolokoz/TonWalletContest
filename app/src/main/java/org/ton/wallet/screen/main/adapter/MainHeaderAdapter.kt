package org.ton.wallet.screen.main.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.ton.lib.rlottie.RLottieDrawable
import org.ton.lib.rlottie.RLottieResourceLoader
import org.ton.wallet.R
import org.ton.wallet.lib.core.Formatter
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.core.ext.copyToClipboard
import org.ton.wallet.lib.lists.DefaultRecyclerAdapter
import org.ton.wallet.lib.lists.RecyclerHolder

class MainHeaderAdapter(
    private val callback: HeaderItemCallback
) : DefaultRecyclerAdapter<MainHeaderAdapter.HeaderItem, MainHeaderAdapter.ViewHolder>() {

    private val item = HeaderItem()

    var height = 0

    init {
        setItems(listOf(item))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(parent, callback, height)
    }

    fun setAddress(address: String?) {
        item.addressFull = address
        item.addressShort = Formatter.getShortAddressSafe(address)
        notifyItemChanged(0)
    }

    fun setBalance(balance: CharSequence?) {
        item.balance = balance
        notifyItemChanged(0)
    }

    class ViewHolder(
        parent: ViewGroup,
        private val callback: HeaderItemCallback,
        height: Int
    ) : RecyclerHolder<HeaderItem>(parent, R.layout.item_main_header), View.OnClickListener {

        private val addressText: TextView = itemView.findViewById(R.id.itemHeaderAddressText)
        private val balanceText: TextView = itemView.findViewById(R.id.itemHeaderBalanceText)

        init {
            itemView.findViewById<View>(R.id.itemHeaderAddressText).setOnClickListener(this)
            itemView.findViewById<View>(R.id.mainReceiveButtonBackground).setOnClickListener(this)
            itemView.findViewById<View>(R.id.mainSendButtonBackground).setOnClickListener(this)
            itemView.layoutParams.height = height

            RLottieResourceLoader.readRawResourceAsync(itemView.context, R.raw.lottie_main) { json, _, _ ->
                val animationDrawable = RLottieDrawable(json, "" + R.raw.lottie_main, Res.dp(44), Res.dp(44), true)
                animationDrawable.setAutoRepeat(1)
                animationDrawable.start()
                balanceText.setCompoundDrawablesRelativeWithIntrinsicBounds(animationDrawable, null, null, null)
            }
        }

        override fun bind(item: HeaderItem) {
            super.bind(item)
            addressText.text = item.addressShort
            balanceText.text = item.balance
        }

        override fun bindPayloads(payloads: List<Any>) {
            super.bindPayloads(payloads)
            payloads.forEach { payload ->
                if (payload is PayloadAddressFirstSet) {
                    item.addressFull = payload.address
                    item.addressShort = payload.addressShort
                    addressText.text = item.addressShort
                    addressText.alpha = 0f
                    addressText.animate().cancel()
                    addressText.animate()
                        .alpha(1f)
                        .setDuration(150)
                        .start()
                }
            }
        }

        override fun onClick(v: View?) {
            when (v?.id) {
                R.id.mainReceiveButtonBackground -> callback.onReceiveClicked()
                R.id.mainSendButtonBackground -> callback.onSendClicked()
                R.id.itemHeaderAddressText -> item.addressFull?.let { context.copyToClipboard(it, Res.str(R.string.address_copied_to_clipboard)) }
            }
        }
    }

    class HeaderItem(
        var addressFull: String? = null,
        var addressShort: String? = null,
        var balance: CharSequence? = null
    )

    class PayloadAddressFirstSet(
        val address: String,
        val addressShort: String
    )

    interface HeaderItemCallback {

        fun onReceiveClicked()

        fun onSendClicked()
    }
}