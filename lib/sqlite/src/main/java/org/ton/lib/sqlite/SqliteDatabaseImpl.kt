package org.ton.lib.sqlite

import android.content.Context
import org.ton.lib.sqlite.internal.AndroidSQLiteOpenHelper
import org.ton.lib.sqlite.internal.BaseSQLiteOpenHelper
//import org.ton.lib.sqlite.internal.CustomSQLiteOpenHelper
import org.ton.lib.sqlite.internal.withTransaction

abstract class SqliteDatabaseImpl(
    context: Context,
    name: String,
    version: Int,
    isCustom: Boolean = false
) : SqliteDatabaseCallback, SqliteDatabase {

    private val impl: BaseSQLiteOpenHelper =
//        if (isCustom) CustomSQLiteOpenHelper(this, context, name, version)
//        else
            AndroidSQLiteOpenHelper(this, context, name, version)

    override val readableDatabase: SQLiteDatabaseWrapper = impl.readDatabase

    override val writeableDatabase: SQLiteDatabaseWrapper = impl.writeDatabase

    override suspend fun withTransaction(block: SQLiteDatabaseWrapper.() -> Unit) {
        impl.withTransaction(block)
    }
}