package org.ton.wallet.data.model

enum class LoadType {
    CacheOrApi,
    OnlyCache,
    OnlyApi;

    val useCache: Boolean
        get() = this == CacheOrApi || this == OnlyCache
}