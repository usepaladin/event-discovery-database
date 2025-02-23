package veridius.discover.services.configuration

import org.springframework.stereotype.Service
import veridius.discover.configuration.properties.CoreConfigurationProperties
import veridius.discover.exceptions.DatabaseConnectionNotFound
import veridius.discover.models.client.DatabaseClient
import veridius.discover.models.configuration.DatabaseTable
import veridius.discover.repositories.configuration.TableConfigurationRepository
import veridius.discover.services.connection.ConnectionService
import java.util.*

@Service
class TableConfigurationService(
    private val serverConfig: CoreConfigurationProperties,
    private val connectionService: ConnectionService,
    private val tableMonitoringRepository: TableConfigurationRepository,
) {
    fun getDatabaseConfigurationProperties(id: UUID) {
        val client: DatabaseClient = connectionService.getClient(id)
            ?: throw DatabaseConnectionNotFound("No active database client found with id: $id")
        getDatabaseConfigurationProperties(client)
    }

    fun getDatabaseConfigurationProperties(client: DatabaseClient) {
        val databaseTableConfig: List<DatabaseTable> = client.getDatabaseProperties()
        println(databaseTableConfig)
    }

}