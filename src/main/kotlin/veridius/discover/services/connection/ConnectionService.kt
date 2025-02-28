package veridius.discover.services.connection

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import mu.KotlinLogging
import org.springframework.beans.factory.DisposableBean
import org.springframework.stereotype.Service
import veridius.discover.exceptions.ConnectionJobNotFound
import veridius.discover.exceptions.DatabaseConnectionNotFound
import veridius.discover.models.client.CassandraClient
import veridius.discover.models.client.MongoClient
import veridius.discover.models.client.MySQLClient
import veridius.discover.models.client.PostgresClient
import veridius.discover.models.common.DatabaseType
import veridius.discover.models.connection.DatabaseConnectionConfiguration
import veridius.discover.pojo.client.ConnectionState
import veridius.discover.pojo.client.DatabaseClient
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

@Service
class ConnectionService : CoroutineScope, DisposableBean {
    override val coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.IO

    private val databaseClients = ConcurrentHashMap<UUID, DatabaseClient>()
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
        databaseClients[connection.id] = client

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
        val connection: DatabaseClient = databaseClients[id] ?: throw DatabaseConnectionNotFound(
            "Active database connection not found \n" +
                    "Database ID: $id"
        )

        return connection
    }

    fun getAllClients(): List<DatabaseClient> {
        return databaseClients.values.toList()
    }

    suspend fun disconnectClient(id: UUID) {
        val connection: DatabaseClient = databaseClients[id] ?: throw DatabaseConnectionNotFound(
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

        val connection: DatabaseClient = databaseClients[id] ?: throw DatabaseConnectionNotFound(
            "Active database connection not found \n" +
                    "Database ID: $id"
        )

        // Disconnect and remove database from active connections
        connectionJob.cancelAndJoin()
        connection.disconnect()
        databaseClients.remove(id)
        clientConnectionJobs.remove(id)
    }

    suspend fun connectAll() = coroutineScope {
        databaseClients.values.map { connection ->
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

    /**
     * This will disconnect all database clients, and clear any active or pending connection jobs.
     * This is useful for when the application is shutting down, or when a user requests to disconnect all clients.
     */
    suspend fun disconnectAll(removeConnections: Boolean = false) = coroutineScope {
        clientConnectionJobs.values.forEach { it.cancelAndJoin() }
        databaseClients.values.map { connection ->
            async {
                try {
                    connection.disconnect()
                    logger.info { "Disconnected from ${connection.id}" }
                } catch (e: Exception) {
                    logger.error(e) { "Error disconnecting from ${connection.id}" }
                }
            }
        }.awaitAll()

        // Remove from cache if we are planning on permanently removing the connections
        if (removeConnections) {
            databaseClients.clear()
        }

        // Clear all connection jobs
        clientConnectionJobs.clear()
    }

    /**
     * Continuous monitoring of database connections, checking all vital signs of each connection.
     * Should be run as a background task on a separate thread, and will attempt to reconnect to a database
     * if it has been disconnected not at the will of the user (ie. Paused, Connecting, etc)
     */
    fun monitorConnections() = launch {
        while (isActive) {
            coroutineScope {
                databaseClients.values.forEach { connection ->
                    launch {
                        try {
                            if (!connection.isConnected() && connection.connectionState.value == ConnectionState.Disconnected) {
                                logger.info { "Attempting to reconnect to ${connection.id}" }
                                connection.connect()
                            }
                        } catch (e: Exception) {
                            logger.error(e) { "Error reconnecting to ${connection.id}" }
                        }
                    }
                }
            }

            delay(30000)
        }
    }

    fun observeConnectionStates(): Flow<Map<UUID, ConnectionState>> = flow {
        val stateFlows = databaseClients.values.map { connection ->
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
