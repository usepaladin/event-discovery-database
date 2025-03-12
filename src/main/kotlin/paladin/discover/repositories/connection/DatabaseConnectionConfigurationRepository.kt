package paladin.discover.repositories.connection

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import paladin.discover.entities.connection.DatabaseConnectionEntity
import java.util.*

interface DatabaseConnectionConfigurationRepository : JpaRepository<DatabaseConnectionEntity, UUID> {
    fun findAllByInstanceId(instanceId: UUID): List<DatabaseConnectionEntity>

    @Query(
        "Select d from DatabaseConnectionEntity d where d.isEnabled = :isEnabled and d.instanceId = :instanceId"
    )
    fun findAllByInstanceIdAndEnabled(instanceId: UUID, isEnabled: Boolean): List<DatabaseConnectionEntity>
}