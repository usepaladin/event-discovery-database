package veridius.discover.entities.configuration

// Will be converted to a JDBC entity

data class DatabaseConfiguration(
    val databaseConnectionId: String,
    val monitoredTables: Set<DatabaseTableMetadata>,
    val monitoredTableColumns: HashMap<DatabaseTableMetadata, Set<ColumnMetadata>>
){
    data class DatabaseTableMetadata(
        val name: String,
        val columns: List<ColumnMetadata>,
        val primaryKeys: List<String>,
    )

    data class ColumnMetadata(
        val name: String,
        val type: String,
        val isNullable: Boolean
    )
}

