package veridius.discover.pojo.connection

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

data class ConnectionAdditionalProperties(
    val dataCenter: String? = null,
    val public: Boolean = false,
    val keySpace: String? = null
)

@Converter
class ConnectionPropertyConverter :
    AttributeConverter<ConnectionAdditionalProperties, String> {
    private val objectMapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()

    override fun convertToDatabaseColumn(attribute: ConnectionAdditionalProperties?): String {
        return objectMapper.writeValueAsString(attribute ?: ConnectionAdditionalProperties())
    }

    override fun convertToEntityAttribute(dbData: String?): ConnectionAdditionalProperties {
        return dbData?.let { objectMapper.readValue(it, ConnectionAdditionalProperties::class.java) }
            ?: ConnectionAdditionalProperties()
    }

}