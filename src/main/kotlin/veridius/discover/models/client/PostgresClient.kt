package veridius.discover.models.client

import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging
import veridius.discover.exceptions.NoActiveConnectionFound
import veridius.discover.models.configuration.Column
import veridius.discover.models.configuration.DatabaseTable
import veridius.discover.models.configuration.ForeignKey
import veridius.discover.models.configuration.PrimaryKey
import veridius.discover.models.connection.DatabaseConnectionConfiguration
import veridius.discover.pojo.client.ConnectionState
import veridius.discover.pojo.client.DatabaseClient
import veridius.discover.util.configuration.HikariTableConfigurationBuilder
import veridius.discover.util.connection.HikariConnectionConfigBuilder
import java.sql.DatabaseMetaData
import java.util.*
import javax.sql.DataSource

data class PostgresClient(
    override val id: UUID, override val config: DatabaseConnectionConfiguration
) : DatabaseClient(), HikariConnectionConfigBuilder, HikariTableConfigurationBuilder {
    constructor(config: DatabaseConnectionConfiguration) : this(config.id, config)

    private var datasource: DataSource? = null

    private val logger = KotlinLogging.logger {}
    override val hikariConfig = generateHikariConfig(config, HikariConnectionConfigBuilder.HikariDatabaseType.POSTGRES)

    override fun connect(): DataSource {
        if (_connectionState.value == ConnectionState.Connected && datasource != null) {
            return datasource!!
        }

        try {
            logger.info { "Postgres Database => ${this.config.connectionName} => $id => Connecting..." }
            _connectionState.value = ConnectionState.Connecting
            datasource = HikariDataSource(hikariConfig)
            _connectionState.value = ConnectionState.Connected
            logger.info { "Postgres Database => ${this.config.connectionName} => $id => Connected" }

            //todo: Ensure User has appropriate roles and permissions for debezium connector purposes

            return datasource!!
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e)
            logger.error(e) {
                "Postgres Database => ${this.config.connectionName} => $id => Failed to connect => Message: ${e.message}"
            }
            throw e
        }
    }

    override fun disconnect() {
        try {
            datasource?.connection?.close()
            datasource = null
            _connectionState.value = ConnectionState.Disconnected
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e)
            logger.error(e) { "Postgres Database => ${this.config.connectionName} => $id => Failed to disconnect => Message: ${e.message}" }
        }
    }

    override fun isConnected(): Boolean {
        try {
            return datasource?.connection?.isValid(1000) ?: false
        } catch (e: Exception) {
            logger.error(e) { "Postgres Database => ${this.config.connectionName} => $id => Failed to check connection status => Message: ${e.message}" }
            return false
        }
    }

    override fun getDatabaseProperties(): List<DatabaseTable> {
        if (datasource == null) {
            logger.warn { "Postgres Database => ${this.config.connectionName} | $id => Can not retrieve properties => No Active Connection found" }
            throw NoActiveConnectionFound("No active connection found for client ${this.id}")
        }

        try {
            datasource.let { source ->
                source?.connection.use { connection ->
                    val metadata: DatabaseMetaData = connection?.metaData ?: throw Exception("No metadata available")

                    // Fetch all data tables (TABLE) from all schemas within the database
                    val tableResultSet = metadata.getTables(null, null, "%", arrayOf("TABLE"))

                    /**
                     * Relevant table data retrieval keys from result set:
                     *  - TABLE-SCHEM (Schema name)
                     *  - TABLE-NAME (Table name)
                     */
                    val tables: MutableList<DatabaseTable> = mutableListOf()
                    while (tableResultSet.next()) {
                        val schema = tableResultSet.getString("TABLE_SCHEM")
                        val tableName = tableResultSet.getString("TABLE_NAME")
                        tables.add(
                            DatabaseTable(
                                tableName = tableName,
                                schema = schema,
                            )
                        )
                    }

                    tables.forEach { table ->
                        val columns: List<Column> = getTableColumns(metadata, table)
                        val primaryKey: PrimaryKey? = getTablePrimaryKey(metadata, table)
                        val foreignKeys: List<ForeignKey> = getTableForeignKeys(metadata, table)

                        table.apply {
                            this.columns = columns
                            this.primaryKey = primaryKey
                            this.foreignKeys = foreignKeys
                        }
                    }
                    return tables
                }
            }

        } catch (ex: Exception) {
            logger.error(ex) { "Postgres Database => ${this.config.connectionName} => $id => Failed to retrieve properties => Message: ${ex.message}" }
            throw ex
        }

    }

    override fun configure() {
        TODO("Not yet implemented")
    }


}