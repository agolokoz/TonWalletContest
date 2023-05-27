package org.ton.wallet.data.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.ton.wallet.data.api.PricesApi
import org.ton.wallet.data.db.price.FiatPricesDao
import org.ton.wallet.data.model.FiatCurrency
import org.ton.wallet.lib.core.CoroutinesUtils
import java.util.concurrent.ConcurrentHashMap

interface PricesRepository : BaseRepository {

    fun getFiatPriceFlow(fiatCurrency: FiatCurrency): StateFlow<Double>

    suspend fun getFiatPrice(fiatCurrency: FiatCurrency): Double

    suspend fun refreshPrices()
}

class PricesRepositoryImpl(
    private val pricesApi: PricesApi,
    private val pricesDao: FiatPricesDao
) : PricesRepository {

    private val fiatPricesFlow = ConcurrentHashMap<FiatCurrency, MutableStateFlow<Double>>()

    init {
        CoroutinesUtils.appCoroutinesScope.launch(Dispatchers.IO) {
            val daoPrices = pricesDao.getPrices()
            daoPrices.forEach { (currency, price) ->
                val fiatCurrency = FiatCurrency.valueOf(currency.uppercase())
                fiatPricesFlow.getOrPut(fiatCurrency) { MutableStateFlow(0.0) }.tryEmit(price)
            }
        }
    }

    override fun getFiatPriceFlow(fiatCurrency: FiatCurrency): MutableStateFlow<Double> {
        var flow = fiatPricesFlow[fiatCurrency]
        if (flow == null) {
            flow = MutableStateFlow(0.0)
            fiatPricesFlow[fiatCurrency] = flow
            CoroutinesUtils.appCoroutinesScope.launch(Dispatchers.IO) {
                flow.value = getFiatPrice(fiatCurrency)
            }
        }
        return flow
    }

    override suspend fun getFiatPrice(fiatCurrency: FiatCurrency): Double {
        var value = pricesDao.getPrice(fiatCurrency.name.lowercase())
        if (value == null) {
            refreshPrices()
            value = pricesDao.getPrice(fiatCurrency.name.lowercase())
        }
        return value ?: 0.0
    }

    override suspend fun refreshPrices() {
        val fiatCurrencies = FiatCurrency.values().map { it.name.lowercase() }
        val prices = pricesApi.getPrices(fiatCurrencies)
        pricesDao.setPrices(prices)
        prices.forEach { (currency, price) ->
            val fiatCurrency = FiatCurrency.valueOf(currency.uppercase())
            fiatPricesFlow[fiatCurrency]?.tryEmit(price)
        }
    }

    override suspend fun deleteWallet() = Unit
}