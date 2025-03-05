package veridius.discover.pojo.connection

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

data class ConnectionAdditionalProperties(
    // All Cassandra instances will need to connect to a specific datacenter
    val dataCenter: String? = null,
    // MongoDB - Origin of the User connecting to the database (ie. Admin)
    @Enumerated(EnumType.STRING)
    val authSource: String? = null,
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