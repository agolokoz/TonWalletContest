package org.ton.wallet.data.model

enum class PassCodeType(val rawValue: Int) {
    Pin4(4),
    Pin6(6);

    companion object {

        fun fromRawValue(rawValue: Int): PassCodeType? {
            return values().firstOrNull { it.rawValue == rawValue }
        }
    }
}