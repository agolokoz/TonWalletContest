package org.ton.lib.sqlite.internal

import org.ton.lib.sqlite.SQLiteDatabaseWrapper
import java.util.concurrent.Executor

internal interface BaseSQLiteOpenHelper {

    val executor: Executor

    val readDatabase: SQLiteDatabaseWrapper

    val writeDatabase: SQLiteDatabaseWrapper

    fun setExecutor(executor: Executor)
}