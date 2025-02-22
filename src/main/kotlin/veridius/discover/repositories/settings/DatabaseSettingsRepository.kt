package veridius.discover.repositories.settings

import org.springframework.data.jpa.repository.JpaRepository
import veridius.discover.entities.settings.DatabaseSettings
import java.util.*

interface DatabaseSettingsRepository : JpaRepository<DatabaseSettings, UUID>