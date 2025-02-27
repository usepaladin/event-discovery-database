package veridius.discover.models.configuration

import veridius.discover.entities.configuration.TableMonitoringConfigurationEntity
import java.time.ZonedDateTime
import java.util.*

data class TableConfiguration(
    val id: UUID,
    val databaseConnectionId: UUID,
    val tableName: String,
    val identifier: String,
    // Schema, keyspace, etc
    val namespace: String? = null,
    var isEnabled: Boolean = true,
    var includeAllColumns: Boolean = true,
    var columns: List<TableColumnConfiguration>? = listOf(),
    var metadata: TableMetadataConfiguration? = null,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
) {
    companion object Factory {
        fun fromEntity(entity: TableMonitoringConfigurationEntity): TableConfiguration {
            return TableConfiguration(
                id = entity.id!!,
                databaseConnectionId = entity.databaseConnectionId,
                tableName = entity.tableName,
                identifier = "",
                namespace = entity.namespace,
                isEnabled = entity.isEnabled,
                includeAllColumns = entity.includeAllColumns,
                columns = entity.columns,
                metadata = entity.metadata,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
            )
        }
    }
}

