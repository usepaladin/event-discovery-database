package paladin.discover.repositories.configuration

import org.springframework.data.jpa.repository.JpaRepository
import paladin.discover.entities.configuration.TableMonitoringConfigurationEntity
import java.util.*

interface TableConfigurationRepository : JpaRepository<TableMonitoringConfigurationEntity, UUID> {
    fun findAllByDatabaseConnectionId(databaseConnectionId: UUID): List<TableMonitoringConfigurationEntity>
    fun findByDatabaseConnectionIdAndTableName(
        databaseConnectionId: UUID,
        tableName: String
    ): TableMonitoringConfigurationEntity?

    fun findAllByDatabaseConnectionIdAndNamespace(
        databaseConnectionId: UUID,
        namespace: String
    ): List<TableMonitoringConfigurationEntity>
}