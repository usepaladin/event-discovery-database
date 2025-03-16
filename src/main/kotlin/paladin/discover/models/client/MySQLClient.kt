package paladin.discover.models.client

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import paladin.discover.exceptions.NoActiveConnectionFound
import paladin.discover.models.configuration.Column
import paladin.discover.models.configuration.DatabaseTable
import paladin.discover.models.configuration.ForeignKey
import paladin.discover.models.configuration.PrimaryKey
import paladin.discover.models.connection.DatabaseConnectionConfiguration
import paladin.discover.pojo.client.DatabaseClient
import paladin.discover.util.configuration.HikariTableConfigurationBuilder
import paladin.discover.util.connection.HikariConnectionConfigBuilder
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.util.*
import javax.sql.DataSource

data class MySQLClient(
    override val id: UUID, override val config: DatabaseConnectionConfiguration
) : DatabaseClient(), HikariConnectionConfigBuilder, HikariTableConfigurationBuilder {
    constructor(config: DatabaseConnectionConfiguration) : this(config.id, config)

    private var datasource: DataSource? = null
    private val logger = KotlinLogging.logger {}
    override val hikariConfig: HikariConfig =
        generateHikariConfig(config, HikariConnectionConfigBuilder.HikariDatabaseType.MYSQL)

    override fun connect(): DataSource {
        validateConfig()
        try {
            logger.info { "MySQL Database => ${this.config.connectionName} => $id => Connecting..." }
            _connectionState.value = ClientConnectionState.Connecting
            datasource = HikariDataSource(hikariConfig)
            _connectionState.value = ClientConnectionState.Connected
            logger.info { "MySQL Database => ${this.config.connectionName} => $id => Connected" }
            return datasource!!
        } catch (e: Exception) {
            _connectionState.value = ClientConnectionState.Error(e)
            logger.error(e) {
                "MySQL Database => ${this.config.connectionName} => $id => Failed to connect => Message: ${e.message}"
            }
            throw e
        }
    }

    override fun disconnect() {
        try {
            logger.info { "MySQL Database => ${this.config.connectionName} => $id => Disconnecting..." }
            datasource?.connection?.close()
            datasource = null
            _connectionState.value = ClientConnectionState.Disconnected
            logger.info { "MySQL Database => ${this.config.connectionName} => $id => Disconnected" }
        } catch (e: Exception) {
            _connectionState.value = ClientConnectionState.Error(e)
            logger.error(e) {
                "MySQL Database => ${this.config.connectionName} => $id => Failed to disconnect => Message: ${e.message}"
            }
        }
    }

    override fun isConnected(): Boolean {

        var connection: Connection? = null
        try {
            connection = datasource?.connection
            return connection?.isValid(4000) ?: false
        } catch (e: Exception) {
            logger.error(e) { "Postgres Database => ${this.config.connectionName} => $id => Failed to check connection status => Message: ${e.message}" }
            return false
        } finally {
            connection?.close()  // Return connection to pool
        }

    }

    /**
     * Retrieves the database properties for the MySQL database
     * Important Notes:
     *  - MySQL doesn't inherently support schemas, so the schema name will be null
     */
    override fun getDatabaseProperties(): List<DatabaseTable> {
        if (datasource == null) {
            logger.warn { "MySQL Database => ${this.config.connectionName} => $id => Could not check properties, no active connection found" }
            throw NoActiveConnectionFound("No active connection found for client ${this.id}")
        }

        try {
            datasource.let { source ->
                source?.connection.use { connection ->
                    val metadata: DatabaseMetaData = connection?.metaData ?: throw Exception("No metadata available")
                    val tableResultSet = metadata.getTables(null, null, null, arrayOf("TABLE"))
                    val tables: MutableList<DatabaseTable> = mutableListOf()
                    while (tableResultSet.next()) {
                        val tableName = tableResultSet.getString("TABLE_NAME")
                        tables.add(
                            DatabaseTable(
                                tableName = tableName
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
            logger.error(ex) {
                "MySQL Database => ${this.config.connectionName} => $id => Failed to retrieve properties => Message: ${ex.message}"
            }
            throw ex
        }
    }

    override fun clientConfigValidation() {
        // No additional validation required for MySQL
    }

    override fun configure() {
        TODO("Not yet implemented")
    }

}