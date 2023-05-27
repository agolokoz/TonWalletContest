package org.ton.wallet.util

import android.net.Uri
import org.json.JSONObject
import org.ton.wallet.data.model.TonConnect
import org.ton.wallet.lib.core.ext.getStringOrNull
import org.ton.wallet.lib.core.ext.toUriSafe

object LinkUtils {

    private const val DeepLinkScheme = "ton"
    private const val DeepLinkTransferAuthority = "transfer"

    private const val ParameterAmount = "amount"
    private const val ParameterText = "text"

    fun getTransferLink(address: String, amount: Long? = null, comment: String? = null): String {
        val builder = Uri.Builder()
            .scheme(DeepLinkScheme)
            .authority(DeepLinkTransferAuthority)
            .path(address)
        if (amount != null) {
            builder.appendQueryParameter("amount", amount.toString())
        }
        if (comment != null) {
            builder.appendQueryParameter("text", comment)
        }
        return builder.build().toString()
    }

    fun parseLink(url: String): LinkAction? {
        val uri = url.toUriSafe() ?: return null
        var action: LinkAction? = parseTransfer(uri)
        if (action == null) {
            action = parseTonConnect(uri)
        }
        return action
    }

    private fun parseTransfer(uri: Uri): LinkAction.TransferAction? {
        if (uri.scheme != DeepLinkScheme || uri.authority != DeepLinkTransferAuthority) {
            return null
        }
        return LinkAction.TransferAction(
            address = uri.path?.removePrefix("/"),
            amount = uri.getQueryParameter(ParameterAmount)?.toLongOrNull(),
            comment = uri.getQueryParameter(ParameterText)
        )
    }

    private fun parseTonConnect(uri: Uri): LinkAction.TonConnectAction? {
        if (uri.scheme != "tc" && (uri.scheme != "https" || uri.authority != "app.tonkeeper.com" || uri.path != "/ton-connect")) {
            return null
        }

        val version = uri.getQueryParameter("v")?.toIntOrNull() ?: return null
        val clientId = uri.getQueryParameter("id") ?: return null
        val requestJson = uri.getQueryParameter("r") ?: return null
        val ret = uri.getQueryParameter("ret")

        // request
        val jsonObject = JSONObject(requestJson)
        val manifestUrl = jsonObject.getStringOrNull("manifestUrl") ?: return null
        val jsonArray = jsonObject.optJSONArray("items") ?: return null
        val tonConnectItems = mutableListOf<TonConnect.ConnectItem>()
        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            val name = item.getStringOrNull("name") ?: continue
            if (name == TonConnect.ConnectItem.TonAddressItem.name) {
                tonConnectItems.add(TonConnect.ConnectItem.TonAddressItem)
            }
        }
        val request = TonConnect.ConnectRequest(manifestUrl, tonConnectItems)

        // ret
        val retObject = when {
            ret == "none" -> TonConnect.Ret.None
            ret?.toUriSafe() != null -> TonConnect.Ret.Url(ret)
            else -> TonConnect.Ret.Back
        }

        return LinkAction.TonConnectAction(uri.toString(), version, clientId, request, retObject)
    }
}