package org.ton.wallet.data.repo

import android.content.SharedPreferences
import android.util.Base64
import drinkless.org.ton.TonApi
import kotlinx.coroutines.flow.*
import org.ton.lib.tonapi.TonAccountType
import org.ton.mnemonic.Mnemonic
import org.ton.wallet.data.DefaultPrefsKeys
import org.ton.wallet.data.SecuredPrefsKeys
import org.ton.wallet.data.model.FiatCurrency
import org.ton.wallet.data.ton.TonClient
import org.ton.wallet.lib.core.SecurityUtils
import org.ton.wallet.lib.core.ThreadUtils
import org.ton.wallet.lib.core.ext.clear
import org.ton.wallet.util.BooleanPrefDelegate
import org.ton.wallet.util.EnumPrefDelegate
import org.ton.wallet.util.NotificationUtils

interface SettingsRepository : BaseRepository {

    val accountTypeFlow: StateFlow<TonAccountType>

    val fiatCurrencyFlow: StateFlow<FiatCurrency>

    val isNotificationsOnFlow: StateFlow<Boolean>

    val isWalletCreatedFlow: StateFlow<Boolean>

    val syncProgressFlow: StateFlow<Int>


    val isNotificationPermissionDialogShown: Boolean

    val isRecoveryChecked: Boolean

    val publicKey: String

    val password: ByteArray

    val secret: ByteArray

    val seed: ByteArray


    @Throws(Exception::class)
    suspend fun createWallet(words: Array<String>?)

    fun getRecoveryPhrase(): List<String>

    fun getRecoveryPhraseWordsCount(): Int = 24

    suspend fun getRecoveryWords(): Array<String>

    fun setRecoveryPhraseChecked(isChecked: Boolean)

    fun setCurrentWalletType(type: TonAccountType)

    fun setCurrentFiatCurrency(fiatCurrency: FiatCurrency)

    fun setNotificationsOn(isNotificationsOn: Boolean)

    fun setNotificationsDialogShown(isShown: Boolean)
}

internal class SettingsRepositoryImpl(
    private val client: TonClient,
    defaultPreferences: SharedPreferences,
    private val securedPreferences: SharedPreferences
) : SettingsRepository {

    private val isNotificationsTurnedOnFlow = MutableStateFlow(true)

    override val accountTypeFlow = MutableStateFlow(DefaultAccountType)

    override val fiatCurrencyFlow = MutableStateFlow(DefaultFiatCurrency)

    override val isNotificationsOnFlow: StateFlow<Boolean> = combine(
        isNotificationsTurnedOnFlow,
        NotificationUtils.isNotificationsPermissionGrantedFlow
    ) { isTurnedOn, isPermissionGranted ->
        isTurnedOn && isPermissionGranted
    }.stateIn(ThreadUtils.appCoroutineScope, SharingStarted.Eagerly, false)

    override val isWalletCreatedFlow = MutableStateFlow(false)

    override val syncProgressFlow = client.syncProgressFlow


    override var isNotificationPermissionDialogShown by BooleanPrefDelegate(defaultPreferences, DefaultPrefsKeys.NotificationsPermissionDialog, false)

    override var isRecoveryChecked by BooleanPrefDelegate(defaultPreferences, DefaultPrefsKeys.RecoveryChecked, false)

    override val publicKey: String
        get() = securedPreferences.getString(DefaultPrefsKeys.PublicKey, "") ?: ""

    override val password: ByteArray
        get() = Base64.decode(securedPreferences.getString(SecuredPrefsKeys.Password, "") ?: "", Base64.NO_WRAP)

    override val secret: ByteArray
        get() = Base64.decode(securedPreferences.getString(SecuredPrefsKeys.Secret, "") ?: "", Base64.NO_WRAP)

    override val seed: ByteArray
        get() = Mnemonic.toSeed(getRecoveryPhrase())


    private var accountType by EnumPrefDelegate(defaultPreferences, DefaultPrefsKeys.AccountTypeSelected, TonAccountType.values(), DefaultAccountType, accountTypeFlow)

    private var fiatCurrency by EnumPrefDelegate(defaultPreferences, DefaultPrefsKeys.FiatCurrency, FiatCurrency.values(), DefaultFiatCurrency, fiatCurrencyFlow)

    private var notifications by BooleanPrefDelegate(defaultPreferences, DefaultPrefsKeys.Notifications, true, isNotificationsTurnedOnFlow)

    private val hasWallet: Boolean
        get() = securedPreferences.contains(SecuredPrefsKeys.Words)

    init {
        accountTypeFlow.tryEmit(accountType)
        fiatCurrencyFlow.tryEmit(fiatCurrency)
        isWalletCreatedFlow.tryEmit(hasWallet)
    }

    override suspend fun createWallet(words: Array<String>?) {
        val password = SecurityUtils.randomBytesSecured(64)
        val seed = SecurityUtils.randomBytesSecured(32)

        val key: TonApi.Key
        val wordsArray: Array<String>
        if (words == null) {
            key = client.sendRequestTyped(TonApi.CreateNewKey(password, null, seed))
            val exportKeyRequest = TonApi.ExportKey(TonApi.InputKeyRegular(key, password))
            val exportKeyResponse = client.sendRequestTyped<TonApi.ExportedKey>(exportKeyRequest)
            wordsArray = exportKeyResponse.wordList
        } else {
            key = client.sendRequestTyped(TonApi.ImportKey(password, null, TonApi.ExportedKey(words)))
            wordsArray = words
        }

        securedPreferences.edit()
            .putString(SecuredPrefsKeys.Password, Base64.encodeToString(password, Base64.NO_WRAP))
            .putString(DefaultPrefsKeys.PublicKey, key.publicKey)
            .putString(SecuredPrefsKeys.Secret, Base64.encodeToString(key.secret, Base64.NO_WRAP))
            .putString(SecuredPrefsKeys.Words, wordsArray.joinToString("|"))
            .apply()

        password.clear()
        seed.clear()
        isWalletCreatedFlow.tryEmit(true)
    }

    override fun getRecoveryPhrase(): List<String> {
        return securedPreferences.getString(SecuredPrefsKeys.Words, "")?.split("|") ?: emptyList()
    }

    override suspend fun getRecoveryWords(): Array<String> {
        return client.sendRequestTyped<TonApi.Bip39Hints>(TonApi.GetBip39Hints()).words
    }

    override fun setRecoveryPhraseChecked(isChecked: Boolean) {
        isRecoveryChecked = isChecked
    }

    override fun setCurrentWalletType(type: TonAccountType) {
        accountType = type
    }

    override fun setCurrentFiatCurrency(fiatCurrency: FiatCurrency) {
        this.fiatCurrency = fiatCurrency
    }

    override fun setNotificationsOn(isNotificationsOn: Boolean) {
        this.notifications = isNotificationsOn
    }

    override fun setNotificationsDialogShown(isShown: Boolean) {
        isNotificationPermissionDialogShown = isShown
    }

    override suspend fun deleteWallet() = Unit


    private companion object {

        private val DefaultAccountType = TonAccountType.v3r2
        private val DefaultFiatCurrency = FiatCurrency.USD
    }
}