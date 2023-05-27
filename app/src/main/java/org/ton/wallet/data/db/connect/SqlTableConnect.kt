package org.ton.wallet.data.db.connect

import org.ton.lib.sqlite.helper.SqlColumnBuilder
import org.ton.lib.sqlite.helper.SqlColumnReference
import org.ton.lib.sqlite.helper.SqlTable
import org.ton.lib.sqlite.helper.SqlTableBuilder
import org.ton.wallet.data.db.account.SqlTableAccounts

object SqlTableConnect : SqlTable {

    override val tableName: String = "connect"

    const val ColumnAccountId = "accountId"
    const val ColumnClientId = "clientId"
    const val ColumnPublicKey = "publicKey"
    const val ColumnSecretKey = "secretKey"
    const val ColumnRequestId = "requestId"

    override fun getCreateSqlQuery(): String {
        return SqlTableBuilder(tableName)
            .addColumn(ColumnAccountId, SqlColumnBuilder.Type.INTEGER) {
                isNotNull = true
                addReference(SqlTableAccounts.tableName, SqlTableAccounts.ColumnId) {
                    onDelete = SqlColumnReference.Action.Cascade
                    onUpdate = SqlColumnReference.Action.NoAction
                }
            }
            .addColumn(ColumnClientId, SqlColumnBuilder.Type.TEXT) {
                isNotNull = true
            }
            .addColumn(ColumnPublicKey, SqlColumnBuilder.Type.TEXT) {
                isNotNull = true
            }
            .addColumn(ColumnSecretKey, SqlColumnBuilder.Type.TEXT) {
                isNotNull = true
            }
            .addColumn(ColumnRequestId, SqlColumnBuilder.Type.INTEGER) {
                isNotNull = true
                default = "-1"
            }
            .buildCreateSql()
    }
}