package paladin.discover.entities.settings

import paladin.discover.pojo.settings.ErrorSettings
import paladin.discover.pojo.settings.MonitoringSettings
import paladin.discover.pojo.settings.PreviewSettings

data class DatabaseSettings(
    val errors: ErrorSettings? = null,
    val monitoring: MonitoringSettings? = null,
    val preview: PreviewSettings? = null,
)

