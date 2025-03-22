package paladin.discover.pojo.configuration

data class TableColumnConfiguration(
    override var name: String? = null,
    override var type: String? = null,
    override var nullable: Boolean = true,
    override var autoIncrement: Boolean = false,
    override var defaultValue: String? = null,
    var isEnabled: Boolean = true,
    // Include both the old and updated value in the Debezium event message
    var includeOldValue: Boolean = true,
    //todo: Research Column specific transformation within Debezium
) : BaseColumn {
    companion object Factory {
        fun fromColumn(
            column: BaseColumn,
            isEnabled: Boolean = true,
            includeOldValue: Boolean = true
        ): TableColumnConfiguration {
            return TableColumnConfiguration(
                name = column.name,
                type = column.type,
                nullable = column.nullable,
                autoIncrement = column.autoIncrement,
                defaultValue = column.defaultValue,
            )
        }
    }
}
