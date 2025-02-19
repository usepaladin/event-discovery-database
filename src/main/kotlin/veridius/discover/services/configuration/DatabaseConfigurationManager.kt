package veridius.discover.services.configuration

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import veridius.discover.entities.DatabaseMonitoringConfig
import java.util.concurrent.ConcurrentHashMap

@Component
class DatabaseConfigurationManager {
    // In-memory storage for active configurations
    private val activeConfigurations = ConcurrentHashMap<String, DatabaseMonitoringConfig>()

    @PostConstruct
    fun initialize() {
        //todo: load configurations from database on startup
    }

    private fun loadConfiguration(): Unit{
        //todo: load configurations from database
    }

    private fun saveConfiguration(): Unit{
        //todo: Update configuration in database
    }

    fun getConfiguration(databaseConnectionId: String): DatabaseMonitoringConfig? {
        return activeConfigurations[databaseConnectionId]
    }

    /**
     * Periodically poll the database to determine if any internal changes have occurred, such as:
     *  - New tables added
     *  - New columns added
     *  - Tables removed
     *  - Columns removed
     *  - etc.
     *
     *  If changes are detected, either update existing configurations and actively monitor,
     *  or just updated Web UI to reflect changes, depends on user preferences with auto configuration
     */
    fun pollSchemaConfiguration(){}
}