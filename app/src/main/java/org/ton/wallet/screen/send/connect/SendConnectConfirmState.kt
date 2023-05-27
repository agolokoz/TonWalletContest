package org.ton.wallet.screen.send.connect

data class SendConnectConfirmState(
    val amount: Long,
    val receiver: String,
    val feeString: String?,
    val isSending: Boolean,
    val isSent: Boolean
)