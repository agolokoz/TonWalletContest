package org.ton.lib.tonapi.internal

import org.ton.api.pub.PublicKey
import org.ton.block.StateInit
import org.ton.cell.Cell
import org.ton.tlb.CellRef

public sealed interface MessageData {
    public val body: Cell
    public val stateInit: CellRef<StateInit>?

    public data class Raw(
        public override val body: Cell,
        public override val stateInit: CellRef<StateInit>?
    ) : MessageData

    public data class Text(
        public val text: CellRef<MessageText>
    ) : MessageData {
        public constructor(text: MessageText) : this(CellRef(text, MessageText))

        override val body: Cell get() = text.toCell(MessageText)
        override val stateInit: CellRef<StateInit>? get() = null
    }

    public companion object {
        @JvmStatic
        public fun raw(body: Cell, stateInit: CellRef<StateInit>? = null): Raw =
            Raw(body, stateInit)

        @JvmStatic
        public fun text(text: String): Text = Text(
            MessageText.Raw(text)
        )

        @JvmStatic
        public fun encryptedText(publicKey: PublicKey, text: String): Text = Text(
            MessageText.Raw(text).encrypt(publicKey)
        )
    }
}