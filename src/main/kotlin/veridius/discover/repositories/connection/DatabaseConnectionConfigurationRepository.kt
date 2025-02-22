package veridius.discover.repositories.connection

import org.springframework.data.jpa.repository.JpaRepository
import veridius.discover.entities.connection.DatabaseConnectionEntity
import java.util.*

interface DatabaseConnectionRepository : JpaRepository<DatabaseConnectionEntity, UUID> {
    fun findAllByInstanceId(instanceId: UUID): List<DatabaseConnectionEntity>
}