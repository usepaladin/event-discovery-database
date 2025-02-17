package veridius.discover.services.connection

import com.mongodb.MongoCredential
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import veridius.discover.configuration.properties.DatabaseConfigurationProperties.DatabaseConnectionConfiguration

data class MongoConnection(
    override val id: String, override val config: DatabaseConnectionConfiguration
) : DatabaseConnection() {
    private var client: MongoClient? = null
    private val logger = KotlinLogging.logger {}

    override suspend fun connect(): MongoClient = withContext(Dispatchers.IO) {
        try{
            _connectionState.value = ConnectionState.Connected
            if(client != null){
                _connectionState.value = ConnectionState.Connected
                client!!
            }

            if(config.user.isNullOrEmpty() || config.password.isNullOrEmpty()){
                client = MongoClients.create(config.toConnectionURL())
            }

            if (client == null) {
                val connectionString = "${config.toConnectionURL()}/${config.database}"
                val credentials = "?authSource=admin&username=${config.user}&password=${config.password}"
                client = MongoClients.create("$connectionString$credentials")
            }

            client!!
        }
        catch (e: Exception){
            _connectionState.value = ConnectionState.Error(e)
            logger.error(e) { "Error connecting to Mongo \n" +
                    "Stack trace: ${e.message}" }
            throw e
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            client?.close()
            client = null
            _connectionState.value = ConnectionState.Disconnected
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e)
            logger.error(e) { "Error disconnecting from Mongo \n" +
                    "Stack trace: ${e.message}" }
            throw e
        }

    }

    override suspend fun isConnected(): Boolean {
        return withContext(Dispatchers.IO) {
            try{
                client?.listDatabaseNames()?.first() != null
            } catch (e: Exception){
                logger.error(e) { "Error checking connection to Mongo \n" +
                        "Stack trace: ${e.message}" }
                false
            }
        }
    }

    override fun validateConfig() {
        TODO("Not yet implemented")
    }

    override fun configure() {
        TODO("Not yet implemented")
    }
}