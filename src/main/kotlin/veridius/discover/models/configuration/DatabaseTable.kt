package veridius.discover.models.configuration

data class DatabaseTable(
    val tableName: String,
    val schema: String? = null,
    var columns: List<Column> = listOf(),
    var foreignKeys: List<ForeignKey> = listOf(),
    var primaryKey: PrimaryKey? = null,
)

data class Column(
    val name: String,
    val type: String,
    val nullable: Boolean,
    val autoIncrement: Boolean,
    val defaultValue: String?,
)

data class ForeignKey(
    val name: String,
    val column: String,
    val foreignTable: String,
    val foreignColumn: String,
)

data class PrimaryKey(
    val name: String? = null,
    val columns: List<String>,
)
