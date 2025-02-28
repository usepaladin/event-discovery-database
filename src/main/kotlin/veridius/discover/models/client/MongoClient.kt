package veridius.discover.models.client

import com.mongodb.client.*
import com.mongodb.client.MongoClient
import mu.KotlinLogging
import org.bson.Document
import org.bson.types.ObjectId
import veridius.discover.exceptions.NoActiveConnectionFound
import veridius.discover.models.configuration.Column
import veridius.discover.models.configuration.DatabaseTable
import veridius.discover.models.configuration.PrimaryKey
import veridius.discover.models.connection.DatabaseConnectionConfiguration
import veridius.discover.pojo.client.ConnectionState
import veridius.discover.pojo.client.DatabaseClient
import veridius.discover.util.connection.ConnectionBuilder
import java.util.*

data class MongoClient(
    override val id: UUID, override val config: DatabaseConnectionConfiguration
) : DatabaseClient() {
    private var client: MongoClient? = null
    private val logger = KotlinLogging.logger {}

    constructor(config: DatabaseConnectionConfiguration) : this(config.id, config)

    override fun connect(): MongoClient {
        try {
            if (client != null) {
                _connectionState.value = ConnectionState.Connected
                client!!
            }

            _connectionState.value = ConnectionState.Connecting
            logger.info { "MongoDB Database => ${config.connectionName} => $id => Connecting..." }
            val connectionBuilder = ConnectionBuilder(config)
            val connectionURL = connectionBuilder.buildConnectionURL()
            client = MongoClients.create(connectionURL)
            _connectionState.value = ConnectionState.Connected
            logger.info { "MongoDB Database => ${config.connectionName} => $id => Connected" }
            return client!!
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e)
            logger.error(e) { "MongoDB Database => ${config.connectionName} => $id => Failed to connect => Message: ${e.message}" }
            throw e
        }
    }

    override fun disconnect() {
        try {
            logger.info { "MongoDB Database => ${config.connectionName} => $id => Disconnecting..." }
            client?.close()
            client = null
            _connectionState.value = ConnectionState.Disconnected
            logger.info { "MongoDB Database => ${config.connectionName} => $id => Disconnected" }
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e)
            logger.error(e) { "MongoDB Database => ${config.connectionName} => $id => Failed to disconnect => Message: ${e.message}" }
            throw e
        }

    }

    override fun isConnected(): Boolean {

        try {
            return client?.listDatabaseNames()?.first() != null
        } catch (e: Exception) {
            logger.error(e) { "MongoDB Database => ${config.connectionName} => $id => Failed to check connection status => Message: ${e.message}" }
            return false
        }
    }

    override fun getDatabaseProperties(): List<DatabaseTable> {
        if (client == null) {
            logger.warn { "MongoDB Database => ${config.connectionName} | $id => Can not retrieve properties => No Active Connection found" }
            throw NoActiveConnectionFound("No active connection for client $id")
        }

        try {
            client.let { source ->
                val databaseTables: MutableList<DatabaseTable> = mutableListOf()
                val databases: MongoIterable<String> = source!!.listDatabaseNames()
                databases.forEach { identifier ->
                    // Fetch Database
                    val database: MongoDatabase = source.getDatabase(identifier)
                    // Fetch all Collections (Tables) in the Database
                    val collections: ListCollectionNamesIterable = database.listCollectionNames()
                    collections.forEach { name ->
                        val collection: MongoCollection<Document> = database.getCollection(name)
                        val databaseTable = DatabaseTable(name, identifier)
                        // Find Primary and as many columns as possible (based on sample document), no need for foreign keys (we live that nosql life)
                        populateDatabaseCollection(collection, databaseTable)
                        databaseTables.add(databaseTable)
                    }
                }

                return databaseTables
            }
        } catch (ex: Exception) {
            logger.error(ex) { "MongoDB Database => ${config.connectionName} => $id => Failed to retrieve properties => Message: ${ex.message}" }
            throw ex
        }
    }

    private fun populateDatabaseCollection(collection: MongoCollection<Document>, databaseTable: DatabaseTable): Unit {
        // MongoDB uses a unique identifier for each document, so we can use this as the primary key
        val primaryKey = PrimaryKey("_id_index", listOf("_id"))
        databaseTable.apply {
            this.primaryKey = primaryKey
        }

        // Need to retrieve a sample document from the collection to infer an appropriate schema
        // If there are no documents in the collection, we cant populate any table information
        val sampleDocument: Document = collection.find().limit(1).firstOrNull() ?: return

        // Flatten the sample document to get the column names and types
        val documentFields = flattenDocument(sampleDocument)
        val columns: List<Column> = documentFields.map { (fieldPath, value) ->
            val type: String = when (value) {
                is String -> "String"
                is Int -> "Int"
                is Double, is Float -> "Double"
                is Boolean -> "Boolean"
                is Date -> "Date"
                is ObjectId -> "ObjectId"
                is Document, is List<*> -> "List"
                else -> "Unknown"
            }

            Column(
                name = fieldPath,
                type = type,
                nullable = value == null,
                autoIncrement = false,
                defaultValue = null
            )
        }

        databaseTable.apply {
            this.columns = columns
        }
    }

    /**
     * Flattens a MongoDB document into a map of key-value pairs
     *
     * @param doc The document to flatten
     * @param prefix The prefix to use for the keys
     *
     * @return A map of key-value pairs representing the flattened document, and each column
     */
    private fun flattenDocument(doc: Document, prefix: String = ""): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()

        doc.entries.forEach { (key, value) ->
            val newKey = if (prefix.isEmpty()) key else "$prefix.$key"

            when (value) {
                is Document -> {
                    // Recursively flatten nested documents
                    result.putAll(flattenDocument(value, newKey))
                    // Also add the object itself as a field
                    result[newKey] = value
                }

                is List<*> -> {
                    // For arrays, we'll add the array itself and sample the first element if available
                    result[newKey] = value

                    if (value.isNotEmpty() && value[0] is Document) {
                        // Sample the first document in the array to get its structure
                        result.putAll(flattenDocument(value[0] as Document, "$newKey.0"))
                    }
                }

                else -> result[newKey] = value
            }
        }

        return result
    }

    override fun validateConfig() {
        TODO("Not yet implemented")
    }

    override fun configure() {
        TODO("Not yet implemented")
    }
}