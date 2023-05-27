package org.ton.wallet.screen.send.confirm

sealed class SendConfirmFeeState {

    object Loading : SendConfirmFeeState()

    object Error : SendConfirmFeeState()

    class Value(val fee: String) : SendConfirmFeeState()
}