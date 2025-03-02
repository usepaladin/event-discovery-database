package veridius.discover.repositories.settings

import org.springframework.data.jpa.repository.JpaRepository
import veridius.discover.entities.settings.DatabaseSettingsEntity
import java.util.*

interface DatabaseSettingsRepository : JpaRepository<DatabaseSettingsEntity, UUID>