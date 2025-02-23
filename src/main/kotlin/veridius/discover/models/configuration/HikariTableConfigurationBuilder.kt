package veridius.discover.models.configuration

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
        TODO()
    }

    fun getTableForeignKeys(metadata: DatabaseMetaData, table: DatabaseTable): List<ForeignKey> {
        TODO()
    }
}