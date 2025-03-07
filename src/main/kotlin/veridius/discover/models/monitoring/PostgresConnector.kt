package veridius.discover.models.monitoring

import io.debezium.storage.file.history.FileSchemaHistory
import veridius.discover.configuration.properties.DebeziumConfigurationProperties
import veridius.discover.models.configuration.TableConfiguration
import veridius.discover.pojo.client.DatabaseClient
import veridius.discover.pojo.monitoring.DatabaseMonitoringConnector
import java.util.*
import io.debezium.connector.postgresql.PostgresConnector as SourcePostgresConnector

data class PostgresConnector(
    override val client: DatabaseClient,
    override val tableConfigurations: List<TableConfiguration>,
    private val storageConfig: DebeziumConfigurationProperties
) : DatabaseMonitoringConnector(storageConfig) {
    override fun buildTableList(): String {
        return tableConfigurations
            .filter { it.isEnabled }.joinToString(",") { "${it.namespace}.${it.tableName}" }
    }

    private fun buildSchemaList(): String {
        return tableConfigurations.filter { it.isEnabled }.map { it.namespace }.distinct().joinToString(",")
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
                    "plugin.name" to "pgoutput",
                    "schema.history.internal" to FileSchemaHistory::class.java.name,
                    "schema.history.internal.file.filename" to "${storageConfig.historyDir}/${client.id}.${storageConfig.historyFileName}",
                    // Performance tuning
                    "max.batch.size" to "2048", // Increase batch size for better throughput
                    "max.queue.size" to "8192", // Increase queue size
                    "poll.interval.ms" to "100", // More frequent polling
                    // Heartbeat for connection monitoring
                    "heartbeat.interval.ms" to "5000", // Send heartbeat every 5 seconds
                    "schema.include.list" to buildSchemaList(),
                    "table.include.list" to buildTableList(),
                    "column.include.list" to buildTableColumnList()
                )
            )
        }
    }
}