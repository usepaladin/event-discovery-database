package veridius.discover.entities.settings

import veridius.discover.pojo.settings.ErrorSettings
import veridius.discover.pojo.settings.MonitoringSettings
import veridius.discover.pojo.settings.PreviewSettings

data class DatabaseSettings(
    val errors: ErrorSettings? = null,
    val monitoring: MonitoringSettings? = null,
    val preview: PreviewSettings? = null,
)

