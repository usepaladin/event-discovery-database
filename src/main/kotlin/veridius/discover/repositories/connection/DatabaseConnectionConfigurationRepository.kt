package veridius.discover.repositories.connection

import org.springframework.data.jpa.repository.JpaRepository
import veridius.discover.entities.connection.DatabaseConnectionEntity
import java.util.*

interface DatabaseConnectionConfigurationRepository : JpaRepository<DatabaseConnectionEntity, UUID> {
    fun findAllByInstanceId(instanceId: UUID): List<DatabaseConnectionEntity>
}