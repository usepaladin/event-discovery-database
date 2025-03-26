package paladin.discover.models.connection

import paladin.discover.entities.configuration.TableMonitoringConfigurationEntity
import paladin.discover.entities.connection.DatabaseConnectionEntity
import paladin.discover.enums.configuration.DatabaseType
import paladin.discover.pojo.connection.ConnectionAdditionalProperties
import java.util.*

data class DatabaseConnectionConfiguration(
    val id: UUID,
    val instanceId: UUID,
    val connectionName: String,
    val databaseType: DatabaseType,
    var hostName: String,
    var port: String,
    var database: String,
    var user: String,
    var password: String?,
    var additionalProperties: ConnectionAdditionalProperties? = null,
    val isEnabled: Boolean,
    val tableConfigurations: List<TableMonitoringConfigurationEntity> = mutableListOf()
) {
    companion object Factory {
        /**
         * Converts a Database Connection Entity fetched from storage to a Database Connection Configuration
         * In the event that the entity is not encrypted, we can safely pass through most of the fields,
         * but would need to object map the JSON Additional properties string.
         *
         * For encrypted entities, they will be left blank for the calling service to decrypt and apply the
         * associated values
         */
        fun fromEntity(
            entity: DatabaseConnectionEntity,
            hostName: String,
            port: String,
            database: String,
            user: String,
            password: String?,
        ): DatabaseConnectionConfiguration {
            return DatabaseConnectionConfiguration(
                id = entity.id ?: throw IllegalArgumentException("Entity ID cannot be null"),
                instanceId = entity.instanceId,
                connectionName = entity.connectionName,
                databaseType = entity.databaseType,
                hostName = hostName,
                port = port,
                database = database,
                user = user,
                password = password,
                isEnabled = entity.isEnabled,
                tableConfigurations = entity.tableMonitoringConfigurations
            )

        }

        fun fromEntity(
            entity: DatabaseConnectionEntity,
        ): DatabaseConnectionConfiguration {
            return DatabaseConnectionConfiguration(
                id = entity.id ?: throw IllegalArgumentException("Entity ID cannot be null"),
                instanceId = entity.instanceId,
                connectionName = entity.connectionName,
                databaseType = entity.databaseType,
                hostName = entity.hostName,
                port = entity.port,
                database = entity.databaseName,
                user = entity.user,
                password = entity.password,
                isEnabled = entity.isEnabled,
                tableConfigurations = entity.tableMonitoringConfigurations
            )

        }
    }
}