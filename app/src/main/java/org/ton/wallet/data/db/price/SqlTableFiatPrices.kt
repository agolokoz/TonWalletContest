package org.ton.wallet.data.db.price

import org.ton.lib.sqlite.helper.SqlColumnBuilder
import org.ton.lib.sqlite.helper.SqlTable
import org.ton.lib.sqlite.helper.SqlTableBuilder
import org.ton.wallet.data.model.FiatCurrency

object SqlTableFiatPrices : SqlTable {

    override val tableName = "fiatPrices"

    const val ColumnTokenId = "tokenId"

    val currenciesColumns = FiatCurrency.values().map { it.name.lowercase() }

    override fun getCreateSqlQuery(): String {
        return SqlTableBuilder(tableName)
            .addColumn(ColumnTokenId, SqlColumnBuilder.Type.INTEGER) { isNotNull = true }
            .apply {
                currenciesColumns.forEach { currency ->
                    addColumn(currency, SqlColumnBuilder.Type.REAL) {
                        default = "0"
                        isNotNull = true
                    }
                }
            }
            .buildCreateSql()
    }

    fun getInitSqlQuery(): String {
        return "INSERT INTO $tableName ($ColumnTokenId) VALUES (0)"
    }
}