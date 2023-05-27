package org.ton.wallet.data.db.connect

data class ConnectDto(
    val accountId: Int,
    val clientId: String,
    val publicKey: String,
    val secretKey: String,
    val requestId: Int
)