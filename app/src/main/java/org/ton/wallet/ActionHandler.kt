package org.ton.wallet

import android.os.Bundle
import org.ton.wallet.data.model.AddressType
import org.ton.wallet.domain.GetAddressTypeUseCase
import org.ton.wallet.lib.core.Res
import org.ton.wallet.lib.navigator.Navigator
import org.ton.wallet.lib.navigator.Screen
import org.ton.wallet.lib.navigator.ScreenParams
import org.ton.wallet.screen.connect.approve.TonConnectApproveController
import org.ton.wallet.screen.send.address.SendAddressController
import org.ton.wallet.screen.send.amount.SendAmountController
import org.ton.wallet.util.FlowBus
import org.ton.wallet.util.FlowBusEvent
import org.ton.wallet.util.LinkAction

class ActionHandler(
    private val navigator: Navigator,
    private val getAddressTypeUseCase: GetAddressTypeUseCase,
) {

    fun processDeepLinkAction(action: LinkAction) {
        if (action is LinkAction.TransferAction) {
            processTransfer(action)
        } else if (action is LinkAction.TonConnectAction) {
            processTonConnect(action)
        }
    }

    private fun processTransfer(action: LinkAction.TransferAction) {
        val address = action.address ?: ""
        val addressType = getAddressTypeUseCase.guessAddressType(address)
        val isRawOrDnsAddress = addressType is AddressType.RawAddress || addressType is AddressType.DnsAddress
        if (addressType == null || isRawOrDnsAddress) {
            val args = Bundle()
            if (isRawOrDnsAddress) {
                args.putString(SendAddressController.ArgumentKeyAddress, address)
            }
            action.amount?.let { args.putLong(SendAddressController.ArgumentKeyAmount, it) }
            action.comment?.let { args.putString(SendAddressController.ArgumentKeyComment, it) }
            navigator.add(ScreenParams(Screen.SendAddress, args))
        } else {
            // address is user-friendly
            val args = Bundle()
            args.putString(SendAmountController.ArgumentKeyAddress, address)
            args.putBoolean(SendAmountController.ArgumentKeyAddressEditable, false)
            action.amount?.let { args.putLong(SendAmountController.ArgumentKeyAmount, it) }
            action.comment?.let { args.putString(SendAmountController.ArgumentKeyComment, it) }
            navigator.add(ScreenParams(Screen.SendAmount, args))
        }
    }

    private fun processTonConnect(action: LinkAction.TonConnectAction) {
        if (action.version != 2) {
            FlowBus.common.dispatch(FlowBusEvent.ShowSnackBar(
                title = Res.str(R.string.error),
                message = Res.str(R.string.ton_connect_unsupported_version, action.version),
                drawable = Res.drawable(R.drawable.ic_warning_32)
            ))
            return
        }

        val args = Bundle()
        args.putString(TonConnectApproveController.ArgumentKeyUrl, action.url)
        navigator.add(ScreenParams(Screen.TonConnectApprove, args))
    }
}