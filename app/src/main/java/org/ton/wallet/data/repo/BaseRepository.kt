package org.ton.wallet.data.repo

interface BaseRepository {

    suspend fun deleteWallet()
}