package org.ton.wallet.screen.pass.setup

import android.os.Bundle
import org.ton.wallet.lib.screen.viewmodel.viewModels
import org.ton.wallet.screen.pass.base.BasePassCodeController

class PassCodeSetupController(args: Bundle?) : BasePassCodeController<PassCodeSetupViewModel>(args) {

    override val viewModel by viewModels { PassCodeSetupViewModel(args ?: Bundle.EMPTY) }
    override val isStatusBarLight = true
    override val isNavigationBarLight = true

    companion object {

        const val KeyBiometrics = "biometric"
        const val KeyImport = "import"
        const val KeyOnlySet = "onlySet"
        const val KeyPasscode = "checkPasscode"

        const val ResultCodePassCodeSet = "passCodeSet"
    }
}