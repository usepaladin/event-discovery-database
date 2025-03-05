package veridius.discover.models.monitoring

import veridius.discover.models.client.PostgresClient
import veridius.discover.models.configuration.TableConfiguration
import veridius.discover.models.connection.DatabaseConnectionConfiguration
import veridius.discover.pojo.monitoring.DatabaseMonitoringConnector
import java.util.*

data class PostgresConnector(
    override val client: PostgresClient,
    override val databaseConnectionConfiguration: DatabaseConnectionConfiguration,
    override val tableConfigurations: Map<UUID, TableConfiguration>,
    private val fileStorageDir: String
) : DatabaseMonitoringConnector(fileStorageDir) {
    override fun buildTableList(): List<String> {
        return tableConfigurations.values.map { it.tableName }
    }

    override fun buildTableColumnList(): Map<String, List<String>> {
        
    }

    override fun getConnectorProps(): Map<String, String> {
        return commonProps()
    }
}