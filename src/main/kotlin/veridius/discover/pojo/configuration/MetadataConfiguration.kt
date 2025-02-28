package veridius.discover.pojo.configuration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import veridius.discover.models.configuration.ForeignKey
import veridius.discover.models.configuration.PrimaryKey

data class TableMetadataConfiguration(
    var primaryKey: PrimaryKey? = null,
    var foreignKeys: List<ForeignKey> = listOf(),
)

@Converter
class TableMetadataConfigurationConvertor : AttributeConverter<TableMetadataConfiguration, String> {
    private val objectMapper = jacksonObjectMapper()

    override fun convertToDatabaseColumn(attribute: TableMetadataConfiguration?): String {
        return objectMapper.writeValueAsString(attribute)
    }

    override fun convertToEntityAttribute(dbData: String?): TableMetadataConfiguration {
        return objectMapper.readValue(dbData, TableMetadataConfiguration::class.java)
    }
}
