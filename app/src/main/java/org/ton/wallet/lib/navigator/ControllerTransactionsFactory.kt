package org.ton.wallet.lib.navigator

import android.os.Bundle
import com.bluelinelabs.conductor.RouterTransaction
import org.ton.wallet.lib.screen.changehandler.BottomSheetChangeHandler
import org.ton.wallet.lib.screen.controller.BaseBottomSheetController
import org.ton.wallet.lib.screen.controller.BaseController
import org.ton.wallet.screen.RecoveryViewController
import org.ton.wallet.screen.connect.approve.TonConnectApproveController
import org.ton.wallet.screen.main.MainController
import org.ton.wallet.screen.onboarding.CongratulationsController
import org.ton.wallet.screen.onboarding.PassCodeCompletedController
import org.ton.wallet.screen.onboarding.RecoveryCheckFinishedController
import org.ton.wallet.screen.onboarding.donothavephrase.DoNotHavePhraseController
import org.ton.wallet.screen.onboarding.importation.ImportController
import org.ton.wallet.screen.onboarding.recoverycheck.RecoveryCheckController
import org.ton.wallet.screen.onboarding.start.StartController
import org.ton.wallet.screen.pass.enter.PassCodeEnterController
import org.ton.wallet.screen.pass.setup.PassCodeSetupController
import org.ton.wallet.screen.receive.ReceiveController
import org.ton.wallet.screen.scanqr.ScanQrController
import org.ton.wallet.screen.send.address.SendAddressController
import org.ton.wallet.screen.send.amount.SendAmountController
import org.ton.wallet.screen.send.confirm.SendConfirmController
import org.ton.wallet.screen.send.connect.SendConnectConfirmController
import org.ton.wallet.screen.send.processing.SendProcessingController
import org.ton.wallet.screen.settings.SettingsController
import org.ton.wallet.screen.transaction.TransactionDetailsController

interface ControllerTransactionsFactory {

    fun createTransaction(screenParams: ScreenParams): RouterTransaction
}

internal class ControllerTransactionsFactoryImpl : ControllerTransactionsFactory {

    override fun createTransaction(screenParams: ScreenParams): RouterTransaction {
        var arguments = screenParams.arguments
        screenParams.resultRequestId?.let { requestId ->
            if (arguments == null) {
                arguments = Bundle()
            }
            arguments!!.putInt(BaseController.KeyResultRequestId, requestId)
        }
        
        val controller = when (screenParams.screen) {
            Screen.Congratulations -> CongratulationsController(arguments)
            Screen.DoNotHavePhrase -> DoNotHavePhraseController(arguments)
            Screen.Import -> ImportController(arguments)
            Screen.Main -> MainController(arguments)
            Screen.PassCodeCompleted -> PassCodeCompletedController(arguments)
            Screen.PassCodeEnter -> PassCodeEnterController(arguments!!)
            Screen.PassCodeSetup -> PassCodeSetupController(arguments)
            Screen.PassCodeSetupCheck -> PassCodeSetupController(arguments)
            Screen.Receive -> ReceiveController(arguments)
            Screen.RecoveryCheck -> RecoveryCheckController(arguments)
            Screen.RecoveryCheckFinished -> RecoveryCheckFinishedController(arguments)
            Screen.RecoveryPhrase -> RecoveryViewController(arguments)
            Screen.ScanQr -> ScanQrController(arguments)
            Screen.SendAddress -> SendAddressController(arguments)
            Screen.SendAmount -> SendAmountController(arguments!!)
            Screen.SendConfirm -> SendConfirmController(arguments!!)
            Screen.SendConnectConfirm -> SendConnectConfirmController(arguments!!)
            Screen.SendProcessing -> SendProcessingController(arguments!!)
            Screen.Settings -> SettingsController(arguments)
            Screen.Start -> StartController(arguments)
            Screen.TonConnectApprove -> TonConnectApproveController(arguments!!)
            Screen.TransactionDetails -> TransactionDetailsController(arguments!!)
        }

        screenParams.resultListener?.let { controller.addResultListener(it) }

        val transaction = RouterTransaction.with(controller)
        if (controller is BaseBottomSheetController) {
            transaction.pushChangeHandler(screenParams.pushChangeHandler ?: BottomSheetChangeHandler())
            transaction.popChangeHandler(screenParams.popChangeHandler ?: BottomSheetChangeHandler())
        } else {
            transaction.pushChangeHandler(screenParams.pushChangeHandler)
            transaction.popChangeHandler(screenParams.popChangeHandler)
        }

        return transaction
    }
}