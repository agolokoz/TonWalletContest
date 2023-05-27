package org.ton.wallet.lib.core.ext

import java.util.*

fun ByteArray.clear() {
    Arrays.fill(this, 0)
}