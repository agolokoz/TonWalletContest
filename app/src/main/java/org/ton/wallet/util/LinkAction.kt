package org.ton.wallet.util

import org.ton.wallet.data.model.TonConnect

sealed class LinkAction {

    class TransferAction(
        val address: String?,
        val amount: Long?,
        val comment: String?
    ) : LinkAction()

    class TonConnectAction(
        val url: String,
        val version: Int,
        val clientId: String,
        val request: TonConnect.ConnectRequest,
        val ret: TonConnect.Ret
    ) : LinkAction()
}