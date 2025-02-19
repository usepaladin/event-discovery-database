package veridius.discover.services.connection;

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.scan
import org.springframework.stereotype.Service
import veridius.discover.configuration.properties.DatabaseConfigurationProperties.*
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.Flow
import mu.KotlinLogging

@Service
class DatabaseConnectionManager(private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
    private val connections = HashMap<String, DatabaseConnection>()
    private val connectionJobs = HashMap<String, Job>()
    private val logger = KotlinLogging.logger {}

    fun createConnection(
        config: DatabaseConnectionConfiguration,
        autoConnect: Boolean = true
    ): DatabaseConnection {

        val connection = when (config.type) {
            DatabaseType.POSTGRES -> PostgresConnection(config.id, config)
            DatabaseType.CASSANDRA -> CassandraConnection(config.id, config)
            DatabaseType.MYSQL -> MySQLConnection(config.id, config)
            DatabaseType.MONGODB -> MongoConnection(config.id, config)
        }

        connections[connection.id] = connection

        connection.connect()
        return connection
    }

    fun getConnection(id: String): DatabaseConnection? {
        return connections[id]
    }

    fun getAllConnections(): List<DatabaseConnection> {
        return connections.values.toList()
    }

    suspend fun removeConnection(id: String) {
        connectionJobs[id]?.cancelAndJoin()
        connections[id]?.disconnect()
        connections.remove(id)
        connectionJobs.remove(id)
    }

    suspend fun connectAll() = coroutineScope {
        connections.values.map { connection ->
            async {
                try {
                    connection.connect()
                    logger.info { "Connected to ${connection.id}" }
                } catch (e: Exception) {
                    logger.error(e) { "Error connecting to ${connection.id}" }
                }
            }
        }.awaitAll()
    }

    suspend fun disconnectAll() = coroutineScope {
        connectionJobs.values.forEach { it.cancelAndJoin() }
        connections.values.map { connection ->
            async {
                try {
                    connection.disconnect()
                    logger.info { "Disconnected from ${connection.id}" }
                } catch (e: Exception) {
                    logger.error(e) { "Error disconnecting from ${connection.id}" }
                }
            }
        }.awaitAll()
        connections.clear()
        connectionJobs.clear()
    }

    // Connection health monitoring
    fun monitorConnections() = scope.launch {
        while (isActive) {
            connections.values.forEach { connection ->
                launch {
                    try {
                        if (!connection.isConnected()) {
                            // Attempt reconnection
                            logger.info { "Attempting to reconnect to ${connection.id}" }
                            connection.connect()
                        }
                    } catch (e: Exception) {
                        // Handle reconnection error
                        logger.error(e) {
                            "Error reconnecting to ${connection.id}"
                        }
                    }
                }
            }
            delay(30000) // Check every 30 seconds
        }
    }

    // Reactive connection states
    fun observeConnectionStates(): Flow<Map<String, ConnectionState>> = flow {
        val stateFlows = connections.values.map { connection ->
            connection.connectionState.map { state -> connection.id to state }
        }

        merge(*stateFlows.toTypedArray())
            .scan(emptyMap<String, ConnectionState>()) { acc, (id, state) ->
                acc + (id to state)
            }
            .collect { emit(it) }
    }
}
