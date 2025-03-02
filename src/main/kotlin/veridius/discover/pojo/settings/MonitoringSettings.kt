package veridius.discover.pojo.settings

data class MonitoringSettings(
    // How often the Database tables should be checked for schema changes + new tables
    var tableSchemaSyncInterval: Int = 60,
    // How many events should be stored in the event queue
    var maxQueueSize: Int = 1000,
    // How often the status of the databases should be monitored and sent to the Web UI
    var statusUpdateIntervalSeconds: Int = 60,

    ) {
    enum class DatabaseStatusMonitoringType {
        CONNECTION_STATUS,
        CONNECTOR_STATUS,
        SERVICE_HEALTH,
        CONFIGURATION_STATUS,
        AVERAGE_LATENCY,
    }
}