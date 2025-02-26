package veridius.discover.models.client

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import mu.KotlinLogging
import veridius.discover.entities.connection.ConnectionBuilder
import veridius.discover.entities.connection.DatabaseConnectionConfiguration
import veridius.discover.models.configuration.DatabaseTable
import java.util.*

data class MongoClient(
    override val id: UUID, override val config: DatabaseConnectionConfiguration
) : DatabaseClient() {
    private var client: MongoClient? = null
    private val logger = KotlinLogging.logger {}

    constructor(config: DatabaseConnectionConfiguration) : this(config.id, config)

    override fun connect(): MongoClient {
        try {
            _connectionState.value = ConnectionState.Connecting
            if (client != null) {
                _connectionState.value = ConnectionState.Connected
                client!!
            }

            val connectionBuilder = ConnectionBuilder(config)
            val connectionURL = connectionBuilder.buildConnectionURL()
            client = MongoClients.create(connectionURL)

            _connectionState.value = ConnectionState.Connected
            return client!!
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e)
            logger.error(e) {
                "Error connecting to Mongo Database with provided credentials \n" +
                        "Stack trace: ${e.message}"
            }
            throw e
        }
    }

    override fun disconnect() {
        try {
            client?.close()
            client = null
            _connectionState.value = ConnectionState.Disconnected
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e)
            logger.error(e) {
                "Error disconnecting from Mongo \n" +
                        "Stack trace: ${e.message}"
            }
            throw e
        }

    }

    override fun isConnected(): Boolean {

        try {
            return client?.listDatabaseNames()?.first() != null
        } catch (e: Exception) {
            logger.error(e) {
                "Error checking connection to Mongo \n" +
                        "Stack trace: ${e.message}"
            }
            return false
        }
    }

    override fun getDatabaseProperties(): List<DatabaseTable> {
        if (client == null) {

        }

    }

    override fun validateConfig() {
        TODO("Not yet implemented")
    }

    override fun configure() {
        TODO("Not yet implemented")
    }
}