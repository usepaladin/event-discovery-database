package veridius.discover.models.monitoring

import veridius.discover.configuration.properties.DebeziumConfigurationProperties
import veridius.discover.models.configuration.TableConfiguration
import veridius.discover.pojo.client.DatabaseClient
import veridius.discover.pojo.monitoring.DatabaseMonitoringConnector
import java.util.*
import io.debezium.connector.mysql.MySqlConnector as SourceMySQLConnector

class MySQLConnector(
    override val client: DatabaseClient,
    override val tableConfigurations: List<TableConfiguration>,
    private val storageConfig: DebeziumConfigurationProperties
) : DatabaseMonitoringConnector(storageConfig) {

    override fun buildTableList(): String {
        return tableConfigurations
            .filter { it.isEnabled }.joinToString(",") { "${client.config.database}.${it.tableName}" }
    }

    override fun buildTableColumnList(): String {
        return tableConfigurations
            .filter { it.isEnabled && !it.includeAllColumns && !it.columns.isNullOrEmpty() }
            .flatMap {
                it.columns!!.map { column ->
                    "${client.config.database}.${it.tableName}.${column.name}"
                }
            }.joinToString(",")
    }

    override fun getConnectorProps(): Properties {
        return commonProps().apply {
            putAll(
                mapOf(
                    "connector.class" to SourceMySQLConnector::class.java.name,
                    "database.server.id" to client.config.id.toString(),
                    "database.server.name" to client.config.connectionName,
                    "database.hostname" to client.config.hostName,
                    "database.port" to client.config.port,
                    "database.user" to client.config.user,
                    "database.password" to (client.config.password ?: ""),
                    "database.dbname" to client.config.database,
                    "topic.prefix" to client.config.connectionName,
                    "table.include.list" to buildTableList(),
                    "column.include.list" to buildTableColumnList()
                )
            )
        }
    }
}