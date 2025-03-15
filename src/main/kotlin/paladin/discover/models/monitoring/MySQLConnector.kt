package paladin.discover.models.monitoring

import paladin.discover.configuration.properties.DebeziumConfigurationProperties
import paladin.discover.models.configuration.TableConfiguration
import paladin.discover.pojo.client.DatabaseClient
import paladin.discover.pojo.monitoring.DatabaseMonitoringConnector
import java.util.*
import kotlin.math.absoluteValue
import io.debezium.connector.mysql.MySqlConnector as SourceMySQLConnector

class MySQLConnector(
    override val client: DatabaseClient,
    override val tableConfigurations: List<TableConfiguration>,
    private val storageConfig: DebeziumConfigurationProperties,
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

    // Same concept to Postgres Schemas
    private fun buildDatabaseList(): String {
        return tableConfigurations.filter { it.isEnabled }.map { it.namespace }.distinct().joinToString(",")
    }

    /**
     * Debezium Connector requires a Long value for the attribute 'database.server.id'
     *
     * This method will generate a unique Long value for the server id based on the connections configuration attributes
     */
    private fun generateServerId(): Long {
        return this.client.id.hashCode()
            .toLong().absoluteValue % 2_147_483_647 + 1 // Max value for a signed 32-bit integer
    }

    override fun getConnectorProps(): Properties {
        val includedDatabases: String = buildDatabaseList()
        val includedTables: String = buildTableList()
        val includedColumns: String = buildTableColumnList()

        val props: Properties = commonProps().apply {
            putAll(
                mapOf(
                    "name" to "mysql-connector-${client.config.connectionName}",
                    "connector.class" to SourceMySQLConnector::class.java.name,
                    "database.server.id" to generateServerId().toString(),
                    "database.server.name" to client.config.connectionName,
                    "database.hostname" to client.config.hostName,
                    "database.port" to client.config.port,
                    "database.user" to client.config.user,
                    "database.password" to (client.config.password ?: ""),
                    "database.dbname" to client.config.database,
                    "topic.prefix" to client.config.connectionName,

                    // Performance Tuning
                    "max.batch.size" to "2048",
                    "max.queue.size" to "8192",
                    "poll.interval.ms" to "100",
                    "heartbeat.interval.ms" to "5000",

                    // MySQL-Specific Settings
                    "database.serverTimezone" to "UTC",
                    "database.allowPublicKeyRetrieval" to "true",
                    "database.tcpKeepAlive" to "true",
                    "database.ssl.mode" to "preferred"
                )
            )
        }
        
        storageBackend.applySchemaHistory(
            props = props,
            config = storageConfig,
            clientId = client.id
        )

        includedDatabases.takeIf { it.isNotEmpty() }?.let {
            props.apply {
                put("database.include.list", includedDatabases)
            }
        }

        includedTables.takeIf { it.isNotEmpty() }?.let {
            props.apply {
                put("table.include.list", includedTables)
            }
        }

        includedColumns.takeIf { it.isNotEmpty() }?.let {
            props.apply {
                put("column.include.list", includedColumns)
            }
        }



        return props
    }
}