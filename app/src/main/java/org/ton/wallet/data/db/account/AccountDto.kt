package org.ton.wallet.data.db.account

class AccountDto(
    val id: Int,
    val walletId: Int,
    val address: String,
    val version: Int,
    val revision: Int,
    var balance: Long,
    var lastTransactionId: Long?,
    var lastTransactionHash: ByteArray?
)