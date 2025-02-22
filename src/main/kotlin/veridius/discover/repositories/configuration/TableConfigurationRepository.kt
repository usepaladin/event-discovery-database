package veridius.discover.repositories.configuration

import org.springframework.data.jpa.repository.JpaRepository
import veridius.discover.entities.configuration.TableMonitoringConfigurationEntity
import java.util.*

interface TableConfigurationRepository : JpaRepository<TableMonitoringConfigurationEntity, UUID>