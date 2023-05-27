package org.ton.wallet.data.db.connect

import android.content.ContentValues
import org.ton.lib.sqlite.SqliteDatabase

interface ConnectDao {

    suspend fun addConnection(dto: ConnectDto)

    suspend fun getConnections(accountId: Int): List<ConnectDto>

    suspend fun getConnection(accountId: Int, clientId: String): ConnectDto?

    suspend fun hasConnection(accountId: Int, clientId: String): Boolean

    suspend fun updateRequestId(accountId: Int, clientId: String, requestId: Int)

    suspend fun removeConnection(accountId: Int, clientId: String)
}

class ConnectDaoImpl(
    private val db: SqliteDatabase
) : ConnectDao {

    override suspend fun addConnection(dto: ConnectDto) {
        val values = ContentValues()
        values.put(SqlTableConnect.ColumnAccountId, dto.accountId)
        values.put(SqlTableConnect.ColumnClientId, dto.clientId)
        values.put(SqlTableConnect.ColumnPublicKey, dto.publicKey)
        values.put(SqlTableConnect.ColumnSecretKey, dto.secretKey)
        db.writeableDatabase.insertOrThrow(
            tableName = SqlTableConnect.tableName,
            nullColumnHack = null,
            contentValues = values
        )
    }

    override suspend fun getConnections(accountId: Int): List<ConnectDto> {
        val connections = mutableListOf<ConnectDto>()
        db.readableDatabase.query(
            table = SqlTableConnect.tableName,
            columns = arrayOf(SqlTableConnect.ColumnClientId, SqlTableConnect.ColumnPublicKey, SqlTableConnect.ColumnSecretKey, SqlTableConnect.ColumnRequestId),
            selection = "${SqlTableConnect.ColumnAccountId} = ?",
            selectionArgs = arrayOf(accountId.toString())
        )?.use { cursor ->
            val columnIndexClientId = cursor.getColumnIndex(SqlTableConnect.ColumnClientId)
            val columnIndexPublicKey = cursor.getColumnIndex(SqlTableConnect.ColumnPublicKey)
            val columnIndexSecretKey = cursor.getColumnIndex(SqlTableConnect.ColumnSecretKey)
            val columnIndexRequestId = cursor.getColumnIndex(SqlTableConnect.ColumnRequestId)
            while (cursor.moveToNext()) {
                val clientId = cursor.getString(columnIndexClientId)
                val publicKey = cursor.getString(columnIndexPublicKey)
                val secretKey = cursor.getString(columnIndexSecretKey)
                val requestId = cursor.getInt(columnIndexRequestId)
                connections.add(ConnectDto(accountId, clientId, publicKey, secretKey, requestId))
            }
        }
        return connections
    }

    override suspend fun getConnection(accountId: Int, clientId: String): ConnectDto? {
        db.readableDatabase.query(
            table = SqlTableConnect.tableName,
            columns = arrayOf(SqlTableConnect.ColumnPublicKey, SqlTableConnect.ColumnSecretKey, SqlTableConnect.ColumnRequestId),
            selection = "${SqlTableConnect.ColumnAccountId} = ? AND ${SqlTableConnect.ColumnClientId} = ?",
            selectionArgs = arrayOf(accountId.toString(), clientId)
        )?.use { cursor ->
            val columnIndexPublicKey = cursor.getColumnIndex(SqlTableConnect.ColumnPublicKey)
            val columnIndexSecretKey = cursor.getColumnIndex(SqlTableConnect.ColumnSecretKey)
            val columnIndexRequestId = cursor.getColumnIndex(SqlTableConnect.ColumnRequestId)
            while (cursor.moveToNext()) {
                val publicKey = cursor.getString(columnIndexPublicKey)
                val secretKey = cursor.getString(columnIndexSecretKey)
                val requestId = cursor.getInt(columnIndexRequestId)
                return ConnectDto(accountId, clientId, publicKey, secretKey, requestId)
            }
        }
        return null
    }

    override suspend fun hasConnection(accountId: Int, clientId: String): Boolean {
        db.readableDatabase.query(
            table = SqlTableConnect.tableName,
            columns = arrayOf(SqlTableConnect.ColumnAccountId),
            selection = "${SqlTableConnect.ColumnAccountId} = ? AND ${SqlTableConnect.ColumnClientId} = ?",
            selectionArgs = arrayOf(accountId.toString(), clientId),
            limit = "1"
        )?.use { cursor ->
            return cursor.count > 0
        }
        return false
    }

    override suspend fun updateRequestId(accountId: Int, clientId: String, requestId: Int) {
        val values = ContentValues().apply {
            put(SqlTableConnect.ColumnRequestId, requestId)
        }
        db.readableDatabase.update(
            table = SqlTableConnect.tableName,
            whereClause = "${SqlTableConnect.ColumnAccountId} = ? AND ${SqlTableConnect.ColumnClientId} = ?",
            whereArgs = arrayOf(accountId.toString(), clientId),
            values = values
        )
    }

    override suspend fun removeConnection(accountId: Int, clientId: String) {
        db.writeableDatabase.delete(
            table = SqlTableConnect.tableName,
            whereClause = "${SqlTableConnect.ColumnAccountId} = ? AND ${SqlTableConnect.ColumnClientId} = ?",
            whereArgs = arrayOf(accountId.toString(), clientId)
        )
    }
}