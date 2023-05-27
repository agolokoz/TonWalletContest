package org.ton.wallet.data

object DefaultPrefsKeys {

    const val AccountTypeSelected = "accountType"
    const val FiatCurrency = "fiatCurrency"
    const val Notifications = "notifications"
    const val NotificationsPermissionDialog = "notificationsPermissionDialog"
    const val PublicKey = "publicKey"
    const val PollingPrefix = "polling_"
    const val RecoveryChecked = "recoveryChecked"
}

object SecuredPrefsKeys {

    const val BiometricTurnedOn = "g"
    const val PassCodeHash = "d"
    const val PassCodeType = "f"
    const val PassCodeSalt = "e"
    const val Password = "a"
    const val Secret = "b"
    const val Words = "c"
}