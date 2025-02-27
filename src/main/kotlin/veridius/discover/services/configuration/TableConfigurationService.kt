package veridius.discover.services.configuration

import mu.KotlinLogging
import org.springframework.stereotype.Service
import veridius.discover.configuration.properties.CoreConfigurationProperties
import veridius.discover.models.client.DatabaseClient
import veridius.discover.models.configuration.DatabaseTable
import veridius.discover.models.configuration.TableConfiguration
import veridius.discover.repositories.configuration.TableConfigurationRepository
import veridius.discover.services.connection.ConnectionService
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service
class TableConfigurationService(
    private val serverConfig: CoreConfigurationProperties,
    private val connectionService: ConnectionService,
    private val tableMonitoringRepository: TableConfigurationRepository,
) {
    private val logger = KotlinLogging.logger {}
    private val tableConfigurations: ConcurrentHashMap<UUID, TableConfiguration> = ConcurrentHashMap()

    /**
     * Scan database client to obtain all relevant metadata for a tables database table configuration, which includes:
     *  - Database Schemas (If applicable)
     *  - Database Tables
     *  - Table Columns
     *  - Column Metadata
     *  - Table Primary Keys
     *  - Table Foreign Keys (If applicable)
     */
    fun scanDatabaseTableConfiguration(client: DatabaseClient) {
        try {
            // Retrieve database table configuration
            val databaseTableConfig: List<DatabaseTable> = client.getDatabaseProperties()
        } catch (e: Exception) {
            logger.error(e) {
                "${client.config.databaseType} Database => ${client.config.connectionName} => ${client.id} => Failed to retrieve database table configuration => Message: ${e.message}"
            }
        }
    }

}