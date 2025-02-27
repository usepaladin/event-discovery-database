package veridius.discover.models.configuration

data class DatabaseTable(
    val tableName: String,
    /**
     * The identifier is a unique string made from the hash of:
     *  - Database name
     *  - Schema name (if exists)
     *  - Table name
     *  - Primary keys
     * This will help create a unique identification for the table, to automatically adjust to
     * updates such as table name changes, schema changes, etc, which would otherwise render
     * the table unrecognizable.
     */
    val identifier: String,
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
