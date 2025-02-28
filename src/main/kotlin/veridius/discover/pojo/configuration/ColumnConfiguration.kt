package veridius.discover.pojo.configuration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

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

@Converter
class TableColumnConfigurationConvertor : AttributeConverter<List<TableColumnConfiguration>, String> {
    private val objectMapper = jacksonObjectMapper()

    override fun convertToDatabaseColumn(attribute: List<TableColumnConfiguration>?): String {
        return objectMapper.writeValueAsString(attribute ?: emptyList<TableColumnConfiguration>())
    }

    override fun convertToEntityAttribute(dbData: String?): List<TableColumnConfiguration> {
        return dbData?.let { objectMapper.readValue(it) } ?: emptyList()
    }
}
