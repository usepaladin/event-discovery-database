package veridius.discover.services.connection

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.CqlSessionBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import veridius.discover.configuration.properties.DatabaseConfigurationProperties
import veridius.discover.configuration.properties.DatabaseConfigurationProperties.DatabaseConnectionConfiguration
import java.net.InetSocketAddress

data class CassandraConnection(
    override val id: String, override val config: DatabaseConnectionConfiguration
) : DatabaseConnection() {
    private var session: CqlSession? = null

    private val logger = KotlinLogging.logger {}

    override fun connect(): CqlSession {
        try {
            _connectionState.value = ConnectionState.Connecting
            if (session == null) {

                if(!config.public && (config.user == null || config.password == null)){
                    throw Exception("User and password are required for private connections")
                }

                val builder: CqlSessionBuilder = CqlSession.builder()
                    .addContactPoint(InetSocketAddress(config.host, config.port.toInt()))
                    .withKeyspace(config.database)

                if(config.dataCenter != null){
                    builder.withLocalDatacenter(config.dataCenter)
                }

            }
            _connectionState.value = ConnectionState.Connected
            return session!!
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e)
            logger.error(e) { "Error connecting to Cassandra \n" +
                    "Stack trace: ${e.message}" }
            throw e
        }
    }

    override fun disconnect() {
        try{
            session?.close()
            session = null
            _connectionState.value = ConnectionState.Disconnected
        } catch (e: Exception){
            _connectionState.value = ConnectionState.Error(e)
            logger.error(e) { "Error disconnecting from Cassandra \n" +
                    "Stack trace: ${e.message}" }
        }
    }

    override fun isConnected(): Boolean {

            try {
                return session?.let { !it.isClosed} ?: false
            } catch (e: Exception) {
                logger.error(e) { "Error checking connection to Cassandra \n" +
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