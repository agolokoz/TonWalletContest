package org.ton.wallet.data.db

import android.content.Context
import org.ton.lib.sqlite.SQLiteDatabaseWrapper
import org.ton.lib.sqlite.SqliteDatabaseImpl
import org.ton.wallet.data.db.account.SqlTableAccounts
import org.ton.wallet.data.db.connect.SqlTableConnect
import org.ton.wallet.data.db.price.SqlTableFiatPrices
import org.ton.wallet.data.db.transaction.SqlTableTransactions

class AppDataBase(context: Context) : SqliteDatabaseImpl(context, "db", 1, CustomSqlite) {

    override fun onCreate(db: SQLiteDatabaseWrapper) {
        db.executeSql("PRAGMA foreign_keys = ON;")
        db.executeSql(SqlTableAccounts.getCreateSqlQuery())
        db.executeSql(SqlTableFiatPrices.getCreateSqlQuery())
        db.executeSql(SqlTableFiatPrices.getInitSqlQuery())
        db.executeSql(SqlTableTransactions.getCreateSqlQuery())
        db.executeSql(SqlTableConnect.getCreateSqlQuery())
    }

    override fun onOpen(db: SQLiteDatabaseWrapper) {
        db.executeSql("PRAGMA foreign_keys = ON;")
    }

    private companion object {

//        private val CustomSqlite = !BuildConfig.DEBUG
        private val CustomSqlite = false

        init {
            if (CustomSqlite) {
                System.loadLibrary("sqliteX")
            }
        }
    }
}