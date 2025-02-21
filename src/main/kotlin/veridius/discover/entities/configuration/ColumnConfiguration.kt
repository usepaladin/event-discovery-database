package veridius.discover.entities.configuration

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

data class TableColumnConfiguration(
    var name: String? = null,
    var isEnabled: Boolean = true,
    // Include both the old and updated value in the Debezium event message
    var includeOldValue: Boolean? = null
    //todo: Research Column specific transformation within Debezium
)

@Converter
class TableColumnConfigurationConvertor : AttributeConverter<TableColumnConfiguration, String> {
    private val objectMapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()

    override fun convertToDatabaseColumn(attribute: TableColumnConfiguration?): String {
        return objectMapper.writeValueAsString(attribute ?: TableColumnConfiguration())
    }

    override fun convertToEntityAttribute(dbData: String?): TableColumnConfiguration {
        return dbData?.let { objectMapper.readValue(it, TableColumnConfiguration::class.java) } ?: TableColumnConfiguration()
    }
}