package paladin.discover.repositories.settings

import org.springframework.data.jpa.repository.JpaRepository
import paladin.discover.entities.settings.DatabaseSettingsEntity
import java.util.*

interface DatabaseSettingsRepository : JpaRepository<DatabaseSettingsEntity, UUID>