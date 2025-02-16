package veridius.discover.entities

data class TableMetadata(
    val name: String,
    val columns: List<ColumnMetadata>,
    val primaryKeys: List<String>,
)

data class ColumnMetadata(
    val name: String,
    val type: String,
    val isNullable: Boolean
)