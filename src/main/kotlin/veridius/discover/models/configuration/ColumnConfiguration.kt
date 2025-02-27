package veridius.discover.models.configuration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

data class TableColumnConfiguration(
    var name: String? = null,
    var type: String? = null,
    var nullable: Boolean = true,
    var autoIncrement: Boolean = false,
    var defaultValue: String? = null,
    var isEnabled: Boolean = true,
    // Include both the old and updated value in the Debezium event message
    var includeOldValue: Boolean? = null
    //todo: Research Column specific transformation within Debezium
)

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
