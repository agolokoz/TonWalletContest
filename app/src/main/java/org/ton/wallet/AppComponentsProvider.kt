package org.ton.wallet

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.ton.lib.sqlite.SqliteDatabase
import org.ton.wallet.data.api.PricesApi
import org.ton.wallet.data.api.PricesApiImpl
import org.ton.wallet.data.db.AppDataBase
import org.ton.wallet.data.db.account.AccountsDao
import org.ton.wallet.data.db.account.AccountsDaoImpl
import org.ton.wallet.data.db.connect.ConnectDao
import org.ton.wallet.data.db.connect.ConnectDaoImpl
import org.ton.wallet.data.db.price.FiatPricesDao
import org.ton.wallet.data.db.price.FiatPricesDaoImpl
import org.ton.wallet.data.db.transaction.TransactionsDao
import org.ton.wallet.data.db.transaction.TransactionsDaoImpl
import org.ton.wallet.data.repo.*
import org.ton.wallet.data.ton.TonClient
import org.ton.wallet.domain.GetAddressTypeUseCase
import org.ton.wallet.domain.GetAddressTypeUseCaseImpl
import org.ton.wallet.lib.navigator.ControllerTransactionsFactory
import org.ton.wallet.lib.navigator.ControllerTransactionsFactoryImpl
import org.ton.wallet.lib.navigator.Navigator
import org.ton.wallet.lib.navigator.NavigatorImpl
import java.util.concurrent.TimeUnit

@SuppressLint("StaticFieldLeak")
object AppComponentsProvider {

    private lateinit var context: Context

    // navigation
    private val controllerTransactionsFactory: ControllerTransactionsFactory by lazy {
        ControllerTransactionsFactoryImpl()
    }

    val navigator: Navigator by lazy { NavigatorImpl(controllerTransactionsFactory) }

    val actionHandler: ActionHandler by lazy { ActionHandler(navigator, getAddressTypeUseCase) }


    // serialization
    val json: Json by lazy {
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }
    }

    // api
    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor(HttpLoggingInterceptor.Logger.DEFAULT)
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    private val sseHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.MINUTES)
            .writeTimeout(10, TimeUnit.MINUTES)
            .build()
    }

    private val tonClient: TonClient by lazy { TonClient(okHttpClient, json) }

    private val pricesApi: PricesApi by lazy { PricesApiImpl(okHttpClient) }


    // storage
    private val dataBase: SqliteDatabase by lazy {
        AppDataBase(context)
    }

    val defaultPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    }

    val securedPreferences: SharedPreferences by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "tonsprefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }


    // db
    private val accountsDao: AccountsDao by lazy { AccountsDaoImpl(dataBase) }

    private val connectDao: ConnectDao by lazy { ConnectDaoImpl(dataBase) }

    private val pricesDao: FiatPricesDao by lazy { FiatPricesDaoImpl(dataBase) }

    private val transactionsDao: TransactionsDao by lazy { TransactionsDaoImpl(dataBase) }


    // repositories
    val accountsRepository: AccountsRepository by lazy {
        AccountsRepositoryImpl(tonClient, accountsDao, securedPreferences)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(securedPreferences)
    }

    val pricesRepository: PricesRepository by lazy {
        PricesRepositoryImpl(pricesApi, pricesDao)
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(tonClient, defaultPreferences, securedPreferences)
    }

    val tonConnectRepository: TonConnectRepository by lazy {
        TonConnectRepositoryImpl(okHttpClient, sseHttpClient, json, connectDao)
    }

    val transactionsRepository: TransactionsRepository by lazy {
        TransactionsRepositoryImpl(tonClient, accountsDao, transactionsDao)
    }

    val baseRepositories: List<BaseRepository>
        get() = listOf(accountsRepository, authRepository, pricesRepository, settingsRepository)


    // use cases
    val getAddressTypeUseCase: GetAddressTypeUseCase by lazy {
        GetAddressTypeUseCaseImpl(accountsRepository)
    }

    fun init(application: Application) {
        context = application
    }
}