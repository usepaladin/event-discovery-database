package veridius.discover.models.monitoring

import veridius.discover.models.configuration.TableConfiguration
import veridius.discover.pojo.client.DatabaseClient
import veridius.discover.pojo.monitoring.DatabaseMonitoringConnector
import java.util.*
import io.debezium.connector.postgresql.PostgresConnector as SourcePostgresConnector

data class PostgresConnector(
    override val client: DatabaseClient,
    override val tableConfigurations: List<TableConfiguration>,
    private val fileStorageDir: String
) : DatabaseMonitoringConnector(fileStorageDir) {
    override fun buildTableList(): String {
        return tableConfigurations
            .filter { it.isEnabled }.joinToString(",") { "${it.namespace}.${it.tableName}" }
    }

    override fun buildTableColumnList(): String {
        return tableConfigurations
            .filter { it.isEnabled && !it.includeAllColumns && !it.columns.isNullOrEmpty() }
            .flatMap {
                it.columns!!.map { column ->
                    "${it.namespace}.${it.tableName}.${column.name}"
                }
            }.joinToString(",")
    }

    override fun getConnectorProps(): Properties {
        return commonProps().apply {
            putAll(
                mapOf(
                    "connector.class" to SourcePostgresConnector::class.java.name,
                    "database.server.name" to client.config.connectionName,
                    "database.server.id" to client.config.id.toString(),
                    "database.hostname" to client.config.hostName,
                    "database.port" to client.config.port,
                    "database.user" to client.config.user,
                    "database.password" to (client.config.password ?: ""),
                    "database.dbname" to client.config.database,
                    "database.server.name" to client.config.connectionName,
                    "topic.prefix" to client.config.connectionName,
                    "table.include.list" to buildTableList(),
                    "column.include.list" to buildTableColumnList()
                )
            )
        }
    }
}