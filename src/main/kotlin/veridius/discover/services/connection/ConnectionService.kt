package veridius.discover.services.connection

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import mu.KotlinLogging
import org.springframework.stereotype.Service
import veridius.discover.configuration.properties.CoreConfigurationProperties.*
import veridius.discover.entities.connection.ConnectionBuilder
import veridius.discover.entities.connection.DatabaseConnectionConfiguration
import veridius.discover.exceptions.ConnectionJobNotFound
import veridius.discover.exceptions.DatabaseConnectionNotFound
import veridius.discover.services.connection.internal.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service
class ConnectionService {
    private val connections = ConcurrentHashMap<UUID, DatabaseConnection>()
    private val connectionJobs = ConcurrentHashMap<UUID, Job>()
    private val logger = KotlinLogging.logger {}

    fun createConnection(
        connection: DatabaseConnectionConfiguration,
        autoConnect: Boolean = true
    ): DatabaseConnection? {
        val connectionBuilder = ConnectionBuilder(connection)
        val connectionURL = connectionBuilder.buildConnectionURL()
        return null
    }

    fun getConnection(id: UUID): DatabaseConnection? {
        val connection: DatabaseConnection = connections[id] ?: throw DatabaseConnectionNotFound(
            "Active database connection not found \n" +
                    "Database ID: $id"
        )

        return connection
    }

    fun getAllConnections(): List<DatabaseConnection> {
        return connections.values.toList()
    }

    suspend fun removeConnection(id: UUID) {
        val connectionJob: Job = connectionJobs[id] ?: throw ConnectionJobNotFound(
            "Connection job not found \n" +
                    "Connection ID: $id"
        )

        val connection: DatabaseConnection = connections[id] ?: throw DatabaseConnectionNotFound(
            "Active database connection not found \n" +
                    "Database ID: $id"
        )

        // Disconnect and remove database from active connections
        connectionJob.cancelAndJoin()
        connection.disconnect()
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
