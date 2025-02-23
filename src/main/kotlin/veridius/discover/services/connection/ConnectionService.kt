package veridius.discover.services.connection

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import mu.KotlinLogging
import org.springframework.beans.factory.DisposableBean
import org.springframework.stereotype.Service
import veridius.discover.entities.connection.DatabaseConnectionConfiguration
import veridius.discover.exceptions.ConnectionJobNotFound
import veridius.discover.exceptions.DatabaseConnectionNotFound
import veridius.discover.models.client.*
import veridius.discover.models.common.DatabaseType
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

@Service
class ConnectionService : CoroutineScope, DisposableBean {
    override val coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.IO

    private val activeClients = ConcurrentHashMap<UUID, DatabaseClient>()
    private val clientConnectionJobs = ConcurrentHashMap<UUID, Job>()
    private val logger = KotlinLogging.logger {}

    fun createConnection(
        connection: DatabaseConnectionConfiguration,
        autoConnect: Boolean = true
    ): DatabaseClient? {
        // Instantiate database client with provided connection configuration
        val client: DatabaseClient =
            when (connection.databaseType) {
                DatabaseType.POSTGRES -> PostgresClient(connection)
                DatabaseType.MYSQL -> MySQLClient(connection)
                DatabaseType.MONGO -> MongoClient(connection)
                DatabaseType.CASSANDRA -> CassandraClient(connection)
            }

        // Store details of active database client
        activeClients[connection.id] = client

        // Attempt client connection if autoConnect is true
        if (autoConnect) {
            clientConnectionJobs[connection.id] = launch {
                try {
                    client.connect()
                    logger.info { "Connected to ${client.id}" }
                } catch (e: Exception) {
                    logger.error(e) { "Error connecting to ${client.id}" }
                }
            }
        }

        return client
    }

    fun getClient(id: UUID): DatabaseClient? {
        val connection: DatabaseClient = activeClients[id] ?: throw DatabaseConnectionNotFound(
            "Active database connection not found \n" +
                    "Database ID: $id"
        )

        return connection
    }

    fun getAllClients(): List<DatabaseClient> {
        return activeClients.values.toList()
    }

    suspend fun disconnectClient(id: UUID) {
        val connection: DatabaseClient = activeClients[id] ?: throw DatabaseConnectionNotFound(
            "Active database connection not found \n" +
                    "Database ID: $id"
        )

        connection.disconnect()
    }

    suspend fun removeDatabaseClient(id: UUID) {
        val connectionJob: Job = clientConnectionJobs[id] ?: throw ConnectionJobNotFound(
            "Connection job not found \n" +
                    "Connection ID: $id"
        )

        val connection: DatabaseClient = activeClients[id] ?: throw DatabaseConnectionNotFound(
            "Active database connection not found \n" +
                    "Database ID: $id"
        )

        // Disconnect and remove database from active connections
        connectionJob.cancelAndJoin()
        connection.disconnect()
        activeClients.remove(id)
        clientConnectionJobs.remove(id)
    }

    suspend fun connectAll() = coroutineScope {
        activeClients.values.map { connection ->
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
        clientConnectionJobs.values.forEach { it.cancelAndJoin() }
        activeClients.values.map { connection ->
            async {
                try {
                    connection.disconnect()
                    logger.info { "Disconnected from ${connection.id}" }
                } catch (e: Exception) {
                    logger.error(e) { "Error disconnecting from ${connection.id}" }
                }
            }
        }.awaitAll()
        activeClients.clear()
        clientConnectionJobs.clear()
    }

    // Connection health monitoring
    fun monitorConnections() = launch {
        while (isActive) {
            activeClients.values.forEach { connection ->
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
    fun observeConnectionStates(): Flow<Map<UUID, ConnectionState>> = flow {
        val stateFlows = activeClients.values.map { connection ->
            connection.connectionState.map { state -> connection.id to state }
        }

        merge(*stateFlows.toTypedArray())
            .scan(emptyMap<UUID, ConnectionState>()) { acc, (id, state) ->
                acc + (id to state)
            }
            .collect { emit(it) }
    }

    override fun destroy() {
        logger.info { "ConnectionService is destroying, cancelling coroutine scope..." }
        cancel() // Cancel the CoroutineScope when the bean is destroyed
        logger.info { "Coroutine scope cancelled." }
    }
}
