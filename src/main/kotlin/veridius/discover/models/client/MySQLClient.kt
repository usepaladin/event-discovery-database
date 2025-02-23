package veridius.discover.models.client

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging
import veridius.discover.entities.connection.DatabaseConnectionConfiguration
import veridius.discover.exceptions.NoActiveConnectionFound
import veridius.discover.models.configuration.*
import veridius.discover.models.connection.HikariConnectionConfigBuilder
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
        try {
            _connectionState.value = ConnectionState.Connecting
            datasource = HikariDataSource(hikariConfig)
            _connectionState.value = ConnectionState.Connected
            return datasource!!
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e)
            logger.error(e) {
                "Error connecting to MySQL Database with provided configuration \n" +
                        "Stack trace: ${e.message}"
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
            logger.error(e) {
                "Error disconnecting from MySQL Database \n" +
                        "Stack trace: ${e.message}"
            }
        }
    }

    override fun isConnected(): Boolean {

        try {
            return datasource?.connection?.isValid(1000) ?: false
        } catch (e: Exception) {
            logger.error(e) {
                "Error checking connection to Postgres \n" +
                        "Stack trace: ${e.message}"
            }
            return false
        }

    }

    /**
     * Retrieves the database properties for the MySQL database
     * Important Notes:
     *  - MySQL doesn't inherently support schemas, so the schema name will be null
     */
    override fun getDatabaseProperties(): List<DatabaseTable> {
        if (datasource == null) {
            logger.warn {
                "Cannot fetch metadata for database client \n" +
                        "Client Id: ${this.id} \n" +
                        "Database Type: MySQL \n" +
                        "Reason: No active connection available"
            }
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
                        tables.add(DatabaseTable(tableName))
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
                "Error fetching metadata for MySQL Database \n" +
                        "Stack trace: ${ex.message}"
            }
            throw ex
        }
    }

    override fun validateConfig() {
        TODO("Not yet implemented")
    }

    override fun configure() {
        TODO("Not yet implemented")
    }

}