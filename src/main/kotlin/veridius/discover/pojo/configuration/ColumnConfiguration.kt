package veridius.discover.pojo.configuration

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.postgresql.util.PGobject

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
class TableColumnConfigurationConvertor : AttributeConverter<List<TableColumnConfiguration>, Any> {
    private val objectMapper = jacksonObjectMapper()
    override fun convertToDatabaseColumn(attribute: List<TableColumnConfiguration>?): Any? {


        if (attribute == null) {
            return null
        }

        val jsonString = objectMapper.writeValueAsString(attribute)
        val pgObject = PGobject()
        pgObject.type = "jsonb"
        pgObject.value = jsonString
        return pgObject
    }

    override fun convertToEntityAttribute(dbData: Any?): List<TableColumnConfiguration> {
        if (dbData == null) {
            return emptyList()
        }

        val jsonString = when (dbData) {
            is PGobject -> dbData.value
            else -> dbData.toString()
        }

        if (jsonString.isNullOrBlank()) {
            return emptyList()
        }

        val typeRef = object : TypeReference<List<TableColumnConfiguration>>() {}
        return objectMapper.readValue(jsonString, typeRef)
    }
}