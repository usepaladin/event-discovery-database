package veridius.discover.entities.settings

data class MonitoringSettings(
    // How often the Database tables should be checked for schema changes + new tables
    var tableSchemaSyncInterval: Int = 60,
)