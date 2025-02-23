package veridius.discover.models.client

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.CqlSessionBuilder
import mu.KotlinLogging
import veridius.discover.entities.connection.DatabaseConnectionConfiguration
import veridius.discover.models.configuration.DatabaseTable
import java.net.InetSocketAddress
import java.util.*

data class CassandraClient(
    override val id: UUID, override val config: DatabaseConnectionConfiguration
) : DatabaseClient() {
    constructor(config: DatabaseConnectionConfiguration) : this(config.id, config)

    private var session: CqlSession? = null
    private val logger = KotlinLogging.logger {}

    override fun connect(): CqlSession {
        try {
            _connectionState.value = ConnectionState.Connecting
            if (session == null) {

                val builder: CqlSessionBuilder = CqlSession.builder()
                    .addContactPoint(InetSocketAddress(config.hostName, config.port.toInt()))
                    .withKeyspace(config.database)

                config.additionalProperties?.dataCenter?.let {
                    builder.withLocalDatacenter(it)
                }

            }
            _connectionState.value = ConnectionState.Connected
            return session!!
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e)
            logger.error(e) {
                "Error connecting to Cassandra \n" +
                        "Stack trace: ${e.message}"
            }
            throw e
        }
    }

    override fun disconnect() {
        try {
            session?.close()
            session = null
            _connectionState.value = ConnectionState.Disconnected
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(e)
            logger.error(e) {
                "Error disconnecting from Cassandra \n" +
                        "Stack trace: ${e.message}"
            }
        }
    }

    override fun isConnected(): Boolean {

        try {
            return session?.let { !it.isClosed } ?: false
        } catch (e: Exception) {
            logger.error(e) {
                "Error checking connection to Cassandra \n" +
                        "Stack trace: ${e.message}"
            }
            return false
        }

    }

    override fun getDatabaseProperties(): List<DatabaseTable> {
        TODO("Not yet implemented")
    }

    override fun validateConfig() {
        TODO("Not yet implemented")
    }

    override fun configure() {
        TODO("Not yet implemented")
    }
}