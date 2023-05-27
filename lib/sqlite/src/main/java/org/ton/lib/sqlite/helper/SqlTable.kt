package org.ton.lib.sqlite.helper

interface SqlTable {

    val tableName: String

    fun getCreateSqlQuery(): String
}