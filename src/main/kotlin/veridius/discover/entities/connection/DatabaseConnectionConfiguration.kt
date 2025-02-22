package veridius.discover.entities.connection

import veridius.discover.entities.configuration.TableMonitoringConfigurationEntity
import java.time.ZonedDateTime
import java.util.*

data class DatabaseConnection(
    val id: UUID,
    val instanceId: UUID,
    val connectionName: String,
    val hostName: String,
    val port: String,
    val database: String?,
    val user: String?,
    val password: String?,
    val additionalProperties: ConnectionAdditionalProperties?,
    val isEnabled: Boolean,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
    val tableConfigurations: List<TableMonitoringConfigurationEntity> = mutableListOf()
)