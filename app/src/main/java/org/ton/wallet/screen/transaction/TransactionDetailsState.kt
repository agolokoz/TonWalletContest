package org.ton.wallet.screen.transaction

import org.ton.wallet.data.db.transaction.TransactionDto
import org.ton.wallet.data.db.transaction.TransactionStatus

class TransactionDetailsState(
    val status: TransactionStatus,
    val type: TransactionDto.Type,
    val amount: CharSequence?,
    val fee: String?,
    val date: String?,
    val message: String?,
    val peerDns: String?,
    val peerShortAddress: CharSequence?,
    val hashShort: CharSequence?,
    val buttonText: String
)