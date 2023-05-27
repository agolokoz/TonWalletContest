package org.ton.wallet.data.db.account

import android.content.ContentValues
import androidx.core.database.getBlobOrNull
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import org.ton.lib.sqlite.SqliteDatabase
import org.ton.lib.tonapi.TonAccountType

interface AccountsDao {

    suspend fun getCount(): Int

    suspend fun add(walletId: Int, address: String, type: TonAccountType): Int

    suspend fun setBalance(address: String, balance: Long)

    suspend fun setLastTransaction(address: String, id: Long, hash: ByteArray)


    suspend fun getAddress(walletId: Int, type: TonAccountType): String?

    suspend fun getAddress(id: Int): String?

    suspend fun get(address: String): AccountDto?

    suspend fun get(id: Int): AccountDto?

    suspend fun getAll(): List<AccountDto>

    suspend fun getId(type: TonAccountType): Int?


    suspend fun remove(walletId: Int)
}

internal class AccountsDaoImpl(
    private val db: SqliteDatabase
) : AccountsDao {

    override suspend fun getCount(): Int {
        db.readableDatabase.query(table = SqlTableAccounts.tableName).use { cursor ->
            return cursor?.count ?: 0
        }
    }

    override suspend fun add(walletId: Int, address: String, type: TonAccountType): Int {
        val values = ContentValues()
        values.put(SqlTableAccounts.ColumnWalletId, walletId)
        values.put(SqlTableAccounts.ColumnAddress, address)
        values.put(SqlTableAccounts.ColumnVersion, type.version)
        values.put(SqlTableAccounts.ColumnRevision, type.revision)
        return try {
            db.writeableDatabase.insertOrThrow(SqlTableAccounts.tableName, null, values).toInt()
        } catch (e: Exception) {
            -1
        }
    }

    override suspend fun setBalance(address: String, balance: Long) {
        val values = ContentValues().apply {
            put(SqlTableAccounts.ColumnBalance, balance)
        }
        set("${SqlTableAccounts.ColumnAddress} = ?", arrayOf(address), values)
    }

    override suspend fun setLastTransaction(address: String, id: Long, hash: ByteArray) {
        val values = ContentValues().apply {
            put(SqlTableAccounts.ColumnLastTransactionId, id)
            put(SqlTableAccounts.ColumnLastTransactionHash, hash)
        }
        set("${SqlTableAccounts.ColumnAddress} = ?", arrayOf(address), values)
    }


    override suspend fun getAddress(walletId: Int, type: TonAccountType): String? {
        return db.readableDatabase.query(
            table = SqlTableAccounts.tableName,
            columns = arrayOf(SqlTableAccounts.ColumnAddress),
            selection = "${SqlTableAccounts.ColumnWalletId} = ? AND ${SqlTableAccounts.ColumnVersion} = ? AND ${SqlTableAccounts.ColumnRevision} = ?",
            selectionArgs = arrayOf(walletId.toString(), type.version.toString(), type.revision.toString()),
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val addressIndex = cursor.getColumnIndex(SqlTableAccounts.ColumnAddress)
                cursor.getString(addressIndex)
            } else {
                null
            }
        }
    }

    override suspend fun getAddress(id: Int): String? {
        return db.readableDatabase.query(
            table = SqlTableAccounts.tableName,
            columns = arrayOf(SqlTableAccounts.ColumnAddress),
            selection = "${SqlTableAccounts.ColumnId} = ?",
            selectionArgs = arrayOf(id.toString()),
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val addressIndex = cursor.getColumnIndex(SqlTableAccounts.ColumnAddress)
                cursor.getString(addressIndex)
            } else {
                null
            }
        }
    }

    override suspend fun get(address: String): AccountDto? {
        return getAccounts(
            selection = "${SqlTableAccounts.ColumnAddress} = ?",
            selectionArgs = arrayOf(address)
        ).firstOrNull()
    }

    override suspend fun get(id: Int): AccountDto? {
        return getAccounts(
            selection = "${SqlTableAccounts.ColumnId} = ?",
            selectionArgs = arrayOf(id.toString())
        ).firstOrNull()
    }

    override suspend fun getAll(): List<AccountDto> {
        return getAccounts(null, null)
    }

    override suspend fun getId(type: TonAccountType): Int? {
        var accountId: Int? = null
        db.readableDatabase.query(
            table = SqlTableAccounts.tableName,
            selection = "${SqlTableAccounts.ColumnVersion} = ? AND ${SqlTableAccounts.ColumnRevision} = ?",
            selectionArgs = arrayOf(type.version.toString(), type.revision.toString()),
        )?.use { cursor ->
            if (cursor.moveToNext()) {
                accountId = cursor.getIntOrNull(cursor.getColumnIndex(SqlTableAccounts.ColumnId))
            }
        }
        return accountId
    }

    override suspend fun remove(walletId: Int) {
        db.writeableDatabase.delete(
            table = SqlTableAccounts.tableName,
            whereClause = "${SqlTableAccounts.ColumnWalletId} = ?",
            whereArgs = arrayOf(walletId.toString())
        )
    }

    private fun set(whereClause: String, whereArgs: Array<String?>?, values: ContentValues) {
        db.writeableDatabase.update(SqlTableAccounts.tableName, values, whereClause, whereArgs)
    }

    private fun getAccounts(selection: String? = null, selectionArgs: Array<String?>? = null): List<AccountDto> {
        val accounts = mutableListOf<AccountDto>()
        db.readableDatabase.query(
            table = SqlTableAccounts.tableName,
            selection = selection,
            selectionArgs = selectionArgs
        )?.use { cursor ->
            val columnIndexId = cursor.getColumnIndex(SqlTableAccounts.ColumnId)
            val columnIndexWalletId = cursor.getColumnIndex(SqlTableAccounts.ColumnWalletId)
            val columnIndex = cursor.getColumnIndex(SqlTableAccounts.ColumnAddress)
            val columnIndexVersion = cursor.getColumnIndex(SqlTableAccounts.ColumnVersion)
            val columnIndexRevision = cursor.getColumnIndex(SqlTableAccounts.ColumnRevision)
            val columnIndexBalance = cursor.getColumnIndex(SqlTableAccounts.ColumnBalance)
            val columnIndexLastTransactionId = cursor.getColumnIndex(SqlTableAccounts.ColumnLastTransactionId)
            val columnIndexLastTransactionHash = cursor.getColumnIndex(SqlTableAccounts.ColumnLastTransactionHash)
            while (cursor.moveToNext()) {
                val account = AccountDto(
                    id = cursor.getInt(columnIndexId),
                    walletId = cursor.getInt(columnIndexWalletId),
                    address = cursor.getString(columnIndex),
                    version = cursor.getInt(columnIndexVersion),
                    revision = cursor.getInt(columnIndexRevision),
                    balance = cursor.getLong(columnIndexBalance),
                    lastTransactionId = cursor.getLongOrNull(columnIndexLastTransactionId),
                    lastTransactionHash = cursor.getBlobOrNull(columnIndexLastTransactionHash)
                )
                accounts.add(account)
            }
        }
        return accounts
    }
}