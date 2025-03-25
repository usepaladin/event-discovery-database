package paladin.discover.models.client

import com.datastax.oss.driver.api.core.CqlIdentifier
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.CqlSessionBuilder
import com.datastax.oss.driver.api.core.metadata.Metadata
import com.datastax.oss.driver.api.core.metadata.schema.ColumnMetadata
import com.datastax.oss.driver.api.core.metadata.schema.KeyspaceMetadata
import com.datastax.oss.driver.api.core.metadata.schema.TableMetadata
import com.datastax.oss.driver.api.core.type.DataType
import com.datastax.oss.driver.api.core.type.UserDefinedType
import io.github.oshai.kotlinlogging.KotlinLogging

import paladin.discover.exceptions.NoActiveConnectionFound
import paladin.discover.models.configuration.database.Column
import paladin.discover.models.configuration.database.DatabaseTable
import paladin.discover.models.configuration.database.PrimaryKey
import paladin.discover.models.connection.DatabaseConnectionConfiguration
import paladin.discover.pojo.client.DatabaseClient
import java.net.InetSocketAddress
import java.util.*

data class CassandraClient(
    override val id: UUID, override val config: DatabaseConnectionConfiguration
) : DatabaseClient() {
    constructor(config: DatabaseConnectionConfiguration) : this(config.id, config)

    private var session: CqlSession? = null
    private val logger = KotlinLogging.logger {}

    override fun connect(): CqlSession {
        this.validateConfig()
        try {
            logger.info { "Cassandra Database => ${config.connectionName} => $id => Connecting..." }
            _connectionState.value = ClientConnectionState.Connecting
            if (session == null) {
                val builder: CqlSessionBuilder = CqlSession.builder()
                    .addContactPoint(InetSocketAddress(config.hostName, config.port.toInt()))
                    .withKeyspace(config.database)
                    .withLocalDatacenter(config.additionalProperties?.dataCenter ?: "datacenter1")

                // Add credentials if provided
                config.user.let { user ->
                    config.password?.let { password ->
                        builder.withAuthCredentials(user, password)
                    }
                }
            }
            _connectionState.value = ClientConnectionState.Connected
            logger.info { "Cassandra Database => ${config.connectionName} => $id => Connected" }
            return session!!
        } catch (e: Exception) {
            _connectionState.value = ClientConnectionState.Error(e)
            logger.error(e) {
                "Cassandra Database => ${config.connectionName} => $id => Failed to connect => Message: ${e.message}"
            }
            throw e
        }
    }

    override fun disconnect() {
        try {
            logger.info { "Cassandra Database => ${config.connectionName} => $id => Disconnecting..." }
            session?.close()
            session = null
            _connectionState.value = ClientConnectionState.Disconnected
            logger.info { "Cassandra Database => ${config.connectionName} => $id => Disconnected" }
        } catch (e: Exception) {
            _connectionState.value = ClientConnectionState.Error(e)
            logger.error(e) {
                "Cassandra Database => ${config.connectionName} => $id => Failed to disconnect => Message: ${e.message}"
            }
        }
    }

    override fun isConnected(): Boolean {
        try {
            return session?.let { !it.isClosed } ?: false
        } catch (e: Exception) {
            logger.error(e) {
                "Cassandra Database => ${config.connectionName} => $id => Failed to check connection status => Message: ${e.message}"
            }
            return false
        }

    }

    override fun getDatabaseProperties(): List<DatabaseTable> {
        if (session == null) {
            logger.warn { "Cassandra Database => ${config.connectionName} | $id => Can not retrieve properties => No Active Connection found" }
            throw NoActiveConnectionFound("No active connection found for client $id")
        }

        try {
            session.let { source ->
                val databaseTables: MutableList<DatabaseTable> = mutableListOf()
                val databaseMetadata: Metadata = source!!.metadata
                val database: Map<CqlIdentifier, KeyspaceMetadata> = databaseMetadata.keyspaces.filter {
                    // Find user defined keyspace
                        name ->
                    name.key.asInternal() == config.database
                }

                if (database.isEmpty()) {
                    logger.warn { "Cassandra Database => ${config.connectionName} | $id => Can not retrieve properties => No Database found" }
                    throw NoActiveConnectionFound("No Keyspace found for database ${this.id} => Keyspace name: ${config.database}")
                }

                database.forEach { (name, metadata) ->
                    // Fetch all tables from the keyspace
                    val tables = metadata.tables

                    // Create a DatabaseTable object for each table
                    tables.forEach { (tableName, tableMetadata) ->
                        // Create a class with the table name and keyspace name
                        val databaseTable = DatabaseTable(
                            tableName = tableName.asInternal(),
                            // Keyspace
                            schema = name.asInternal(),
                        )

                        // Find all Columns, and primary key (Cassandra does not fuck with foreign keys so no bother)
                        populateCassandraTable(tableMetadata, databaseTable)
                        databaseTables.add(databaseTable)
                    }
                }
                return databaseTables
            }

        } catch (e: Exception) {
            logger.error(e) {
                "Cassandra Database => ${config.connectionName} => $id => Failed to check connection status => Message: ${e.message}"
            }
            throw e
        }
    }

    /**
     * Retrieve all columns that make up the tables Partition key, or
     */

    private fun populateCassandraTable(tableMetadata: TableMetadata, databaseTable: DatabaseTable): Unit {
        // Get all columns that make up the primary key (Partition + Clustering)
        val primaryKeyColumns: List<ColumnMetadata> = tableMetadata.primaryKey
        val primaryKey = PrimaryKey(
            name = null,
            columns = primaryKeyColumns.map { it.name.asInternal() }
        )

        // Process all columns
        val columns: List<Column> = tableMetadata.columns.map { (columnName, columnMetadata) ->
            val cassandraType: DataType = columnMetadata.type
            val columnType: String = mapCassandraTypeToGeneric(cassandraType)

            Column(
                name = columnName.asInternal(),
                type = columnType,
                // A column that is not apart of the primary key are considered nullable, there is no NON NULL declaration
                nullable = primaryKeyColumns.find { pkCol -> pkCol.name == columnName } == null,
                // Cassandra does not support auto incrementing columns or default values
                autoIncrement = false,
                defaultValue = null
            )
        }

        // Apply the column and primary key data to the table
        databaseTable.apply {
            this.primaryKey = primaryKey
            this.columns = columns
        }
    }

    // Helper function to map Cassandra data types to generic types
    private fun mapCassandraTypeToGeneric(cassandraType: DataType): String {
        return when (cassandraType.asCql(false, false)) {
            "int", "bigint", "smallint", "tinyint", "varint", "counter" -> "INTEGER"
            "float", "double", "decimal" -> "DOUBLE"
            "text", "varchar", "ascii" -> "STRING"
            "boolean" -> "BOOLEAN"
            "date", "timestamp", "time" -> "TIMESTAMP"
            "uuid", "timeuuid" -> "UUID"
            "blob" -> "BLOB"
            "map", "set", "list", "tuple" -> "COLLECTION"
            else -> if (cassandraType is UserDefinedType) "UDT" else "OTHER"
        }
    }

    override fun clientConfigValidation() {
        if (config.additionalProperties?.dataCenter == null) {
            throw IllegalArgumentException("Cassandra Database => ${config.connectionName} => $id => Datacenter must be provided")
        }
    }

    override fun configure() {
        TODO("Not yet implemented")
    }
}