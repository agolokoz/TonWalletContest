package org.ton.lib.tonapi

import android.util.Base64
import kotlinx.datetime.Clock
import org.ton.api.pk.PrivateKeyEd25519
import org.ton.bitstring.BitString
import org.ton.block.*
import org.ton.boc.BagOfCells
import org.ton.cell.Cell
import org.ton.cell.CellBuilder
import org.ton.contract.wallet.WalletTransfer
import org.ton.crypto.decodeHex
import org.ton.hashmap.HmeEmpty
import org.ton.lib.tonapi.internal.MessageData
import org.ton.tlb.CellRef
import org.ton.tlb.constructor.AnyTlbConstructor
import org.ton.tlb.storeRef
import org.ton.tlb.storeTlb
import kotlin.time.Duration.Companion.seconds

enum class TonAccountType(val version: Int, val revision: Int) {
    v3r1(3, 1),
    v3r2(3, 2),
    v4r2(4, 2);

    fun getString(): String {
        return when (this) {
            v3r1 -> "v3R1"
            v3r2 -> "v3R2"
            v4r2 -> "v4R2"
        }
    }

    companion object {

        fun getAccountType(version: Int, revision: Int): TonAccountType {
            return when (version) {
                3 -> if (revision == 1) v3r1 else v3r2
                4 -> v4r2
                else -> throw IllegalArgumentException("Unsupported account version $version")
            }
        }
    }
}

class TonAccount(
    publicKeyBase64: String,
    val version: Int,
    val revision: Int,
    val subWalletId: Int = DefaultWalletId,
    val seqNo: Int = 0
) {

    val publicKey = Base64.decode(publicKeyBase64, Base64.URL_SAFE).copyOfRange(2, 34)

    val type: TonAccountType = TonAccountType.getAccountType(version, revision)

    fun getCode(): ByteArray {
        val codeString = when (type) {
            TonAccountType.v3r1 -> Contracts.WalletV3R1
            TonAccountType.v3r2 -> Contracts.WalletV3R2
            TonAccountType.v4r2 -> Contracts.WalletV4R2
        }
        return codeString.decodeHex()
    }

    fun getData(): ByteArray {
        val rootCellBuilder = CellBuilder()
            .storeUInt(seqNo, 32)
            .storeUInt(subWalletId, 32)
            .storeBytes(publicKey)
        if (version == 4) {
            rootCellBuilder.storeBit(false)
        }
        return BagOfCells(rootCellBuilder.endCell()).toByteArray()
    }

    fun getStateInitBytes(): ByteArray {
        val stateInit = getStateInit()
        val stateInitCell = CellBuilder.createCell {
            storeTlb(StateInit.tlbCodec(), stateInit)
        }
        return BagOfCells(stateInitCell).toByteArray()
    }

    fun getTransferMessageBody(toWorkChainId: Int, toRawAddress: ByteArray, amount: Long, message: String? = null, seed: ByteArray? = null): ByteArray {
        val bodyCell =
            if (message.isNullOrEmpty()) null
            else MessageData.text(message).body
        val transfer = WalletTransfer {
            destination = AddrStd(toWorkChainId, toRawAddress)
            coins = Coins.ofNano(amount)
            body = bodyCell
        }

        val unsignedBody = CellBuilder.createCell {
            storeUInt(subWalletId, 32)
            storeUInt((Clock.System.now() + 60.seconds).epochSeconds.toInt(), 32)
            storeUInt(seqNo, 32)
            storeUInt(0, 8) // op
            var sendMode = 3
            if (transfer.sendMode > -1) {
                sendMode = transfer.sendMode
            }
            val intMsg = CellRef(createIntMsg(transfer))
            storeUInt(sendMode, 8)
            storeRef(MessageRelaxed.tlbCodec(AnyTlbConstructor), intMsg)
        }
        val privateKey = seed?.let { PrivateKeyEd25519(it) }
        val signature = privateKey?.let { BitString(it.sign(unsignedBody.hash())) }

        val msgCell = CellBuilder.createCell {
            signature?.let { storeBits(it) }
            storeBits(unsignedBody.bits)
            storeRefs(unsignedBody.refs)
        }
        return BagOfCells(msgCell).toByteArray()
    }

    private fun createIntMsg(transfer: WalletTransfer): MessageRelaxed<Cell> {
        val info = CommonMsgInfoRelaxed.IntMsgInfoRelaxed(
            ihrDisabled = true,
            bounce = transfer.bounceable,
            bounced = false,
            src = AddrNone,
            dest = transfer.destination,
            value = transfer.coins,
            ihrFee = Coins(),
            fwdFee = Coins(),
            createdLt = 0u,
            createdAt = 0u
        )
        val init = Maybe.of(transfer.stateInit?.let {
            Either.of<StateInit, CellRef<StateInit>>(null, CellRef(it))
        })
        val body = if (transfer.body == null) {
            Either.of<Cell, CellRef<Cell>>(Cell.empty(), null)
        } else {
            Either.of<Cell, CellRef<Cell>>(null, CellRef(transfer.body!!))
        }

        return MessageRelaxed(
            info = info,
            init = init,
            body = body,
        )
    }

    private fun getStateInit(): StateInit {
        return StateInit(
            code = BagOfCells(getCode()).roots.first(),
            data = CellBuilder.createCell {
                storeUInt(0, 32)
                storeBits(BitString(publicKey))
            },
            library = HmeEmpty(),
            splitDepth = null,
            special = null,
        )
    }

    companion object {

        const val DefaultWalletId = 698983191
    }
}