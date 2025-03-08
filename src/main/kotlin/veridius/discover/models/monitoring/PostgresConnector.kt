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
        val includedSchemas: String = buildSchemaList()
        val includedTables: String = buildTableList()
        val includedColumns: String = buildTableColumnList()

        val props: Properties = commonProps().apply {
            putAll(
                mapOf(
                    "name" to "postgres-connector-${client.config.connectionName}",
                    "connector.class" to SourcePostgresConnector::class.java.name,
                    "database.server.name" to client.config.connectionName,
                    "database.server.id" to client.config.id.toString(),
                    "database.hostname" to client.config.hostName,
                    "database.port" to client.config.port,
                    "database.user" to client.config.user,
                    "database.password" to (client.config.password ?: ""),
                    "database.dbname" to client.config.database,
                    "topic.prefix" to client.config.connectionName,
                    // Postgres Logical Replication
                    "plugin.name" to "pgoutput",
                    "publication.name" to "dds_publication_${client.config.connectionName}",

                    // Schema history
                    "schema.history.internal" to FileSchemaHistory::class.java.name,
                    "schema.history.internal.file.filename" to "${storageConfig.historyDir}/${client.id}.${storageConfig.historyFileName}",
                    // Performance tuning
                    "max.batch.size" to "2048", // Increase batch size for better throughput
                    "max.queue.size" to "8192", // Increase queue size
                    "poll.interval.ms" to "100", // More frequent polling
                    // Heartbeat for connection monitoring
                    "heartbeat.interval.ms" to "5000", // Send heartbeat every 5 seconds
                )
            )
        }

        includedSchemas.takeIf { it.isNotBlank() }?.let {
            props.apply {
                put("schema.include.list", includedSchemas)
            }
        }

        includedTables.takeIf { it.isNotBlank() }?.let {
            props.apply {
                put("table.include.list", includedTables)
            }
        }

        includedColumns.takeIf { it.isNotBlank() }?.let {
            props.apply {
                put("column.include.list", includedColumns)
            }
        }

        return props
    }
}