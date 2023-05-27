package org.ton.lib.sqlite

interface SqliteDatabase {

    val readableDatabase: SQLiteDatabaseWrapper

    val writeableDatabase: SQLiteDatabaseWrapper

    suspend fun withTransaction(block: SQLiteDatabaseWrapper.() -> Unit)
}