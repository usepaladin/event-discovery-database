package veridius.discover.services.connection.internal

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import mu.KotlinLogging
import veridius.discover.configuration.properties.DatabaseConfigurationProperties.DatabaseConnectionConfiguration

data class MongoConnection(
    override val id: String, override val config: DatabaseConnectionConfiguration
) : DatabaseConnection() {
    private var client: MongoClient? = null
    private val logger = KotlinLogging.logger {}

    override fun connect(): MongoClient {
        try{
            _connectionState.value = ConnectionState.Connecting
            if(client != null){
                _connectionState.value = ConnectionState.Connected
                client!!
            }

            if(config.user.isNullOrEmpty() || config.password.isNullOrEmpty()){
                client = MongoClients.create(config.toConnectionURL())
            }

            if (client == null) {
                client = MongoClients.create(config.toConnectionURL())
            }

            _connectionState.value = ConnectionState.Connected
            return client!!
        }
        catch (e: Exception){
            _connectionState.value = ConnectionState.Error(e)
            logger.error(e) { "Error connecting to Mongo \n" +
                    "Stack trace: ${e.message}" }
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
            logger.error(e) { "Error disconnecting from Mongo \n" +
                    "Stack trace: ${e.message}" }
            throw e
        }

    }

    override fun isConnected(): Boolean {

            try{
                return client?.listDatabaseNames()?.first() != null
            } catch (e: Exception){
                logger.error(e) { "Error checking connection to Mongo \n" +
                        "Stack trace: ${e.message}" }
                return false
            }

    }

    override fun validateConfig() {
        TODO("Not yet implemented")
    }

    override fun configure() {
        TODO("Not yet implemented")
    }
}