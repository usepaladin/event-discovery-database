package veridius.discover.entities.settings

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import veridius.discover.pojo.settings.ErrorSettings
import veridius.discover.pojo.settings.MonitoringSettings
import veridius.discover.pojo.settings.PreviewSettings

data class DatabaseSettings(
    val errors: ErrorSettings? = null,
    val monitoring: MonitoringSettings? = null,
    val preview: PreviewSettings? = null,
)

@Converter
class DatabaseSettingsConvertor : AttributeConverter<DatabaseSettings, String> {
    private val objectMapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()

    override fun convertToDatabaseColumn(attribute: DatabaseSettings?): String {
        return objectMapper.writeValueAsString(attribute ?: DatabaseSettings())
    }

    override fun convertToEntityAttribute(dbData: String?): DatabaseSettings {
        return dbData?.let { objectMapper.readValue(it, DatabaseSettings::class.java) } ?: DatabaseSettings()
    }
}