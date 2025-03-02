package veridius.discover.util.configuration

import veridius.discover.models.configuration.Column
import veridius.discover.models.configuration.DatabaseTable
import veridius.discover.models.configuration.ForeignKey
import veridius.discover.models.configuration.PrimaryKey
import java.sql.DatabaseMetaData

interface HikariTableConfigurationBuilder {

    fun getTableColumns(metadata: DatabaseMetaData, table: DatabaseTable): List<Column> {
        /**
         * Relevant column data retrieval keys from result set:
         *  - COLUMN_NAME (Column name)
         *  - TYPE_NAME (Column type)
         *  - NULLABLE (Column nullability)
         *  - COLUMN_DEF (Column default value)
         *  - IS_AUTOINCREMENT (Column auto increment)
         *
         */
        val columnResultSet = metadata.getColumns(null, table.schema, table.tableName, null)
        val columns: MutableList<Column> = mutableListOf()
        while (columnResultSet.next()) {
            val columnName = columnResultSet.getString("COLUMN_NAME")
            val columnType = columnResultSet.getString("TYPE_NAME")
            val nullable = columnResultSet.getInt("NULLABLE") == DatabaseMetaData.columnNullable
            val defaultValue = columnResultSet.getString("COLUMN_DEF")
            val autoIncrement = columnResultSet.getString("IS_AUTOINCREMENT") == "YES"
            columns.add(Column(columnName, columnType, nullable, autoIncrement, defaultValue))
        }
        return columns
    }

    fun getTablePrimaryKey(metadata: DatabaseMetaData, table: DatabaseTable): PrimaryKey? {
        val primaryKeyResultSet = metadata.getPrimaryKeys(null, table.schema, table.tableName)

        //Fuck while loops and iterables
        var primaryKeyName: String? = null
        val primaryKeyColumns: MutableList<String> = mutableListOf()
        while (primaryKeyResultSet.next()) {
            if (primaryKeyName == null) {
                primaryKeyName = primaryKeyResultSet.getString("PK_NAME")
            }

            primaryKeyColumns.add(primaryKeyResultSet.getString("COLUMN_NAME"))
        }

        if (primaryKeyColumns.size == 0) return null

        return PrimaryKey(primaryKeyName, primaryKeyColumns)
    }

    fun getTableForeignKeys(metadata: DatabaseMetaData, table: DatabaseTable): List<ForeignKey> {
        val foreignKeyResultSet = metadata.getImportedKeys(null, table.schema, table.tableName)
        val foreignKeys: MutableList<ForeignKey> = mutableListOf()
        while (foreignKeyResultSet.next()) {
            val foreignKeyName = foreignKeyResultSet.getString("FK_NAME")
            val foreignColumn = foreignKeyResultSet.getString("FKCOLUMN_NAME")
            val foreignTable = foreignKeyResultSet.getString("PKTABLE_NAME")
            val foreignColumnReference = foreignKeyResultSet.getString("PKCOLUMN_NAME")
            foreignKeys.add(ForeignKey(foreignKeyName, foreignColumn, foreignTable, foreignColumnReference))
        }
        return foreignKeys
    }
}