package veridius.discover.models.monitoring

import io.debezium.connector.postgresql.PostgresConnector as SourcePostgresConnector
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
        return commonProps().apply {
            putAll(
                mapOf(
                    "connector.class" to SourcePostgresConnector::class.java.name,
                    "database.server.name" to client.config.connectionName,
                    "database.hostname" to client.config.hostName,
                    "database.port" to client.config.port,
                    "database.user" to client.config.user,
                    "database.password" to (client.config.password ?: ""),
                    "database.dbname" to client.config.database,
                    "database.server.name" to client.config.connectionName,
                    "topic.prefix" to client.config.connectionName,
        }
    }
}