package veridius.discover.services.connection

import kotlinx.coroutines.*
import mu.KLogger
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
import veridius.discover.pojo.client.DatabaseClient
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

/*
    Todo: More connection support
        - Oracle
        - MariaDB
 */

@Service
class ConnectionService(private val logger: KLogger, private val dispatcher: CoroutineDispatcher) : CoroutineScope,
    DisposableBean {
    override val coroutineContext: CoroutineContext = SupervisorJob() + dispatcher

    private val databaseClients = ConcurrentHashMap<UUID, DatabaseClient>()
    private val clientConnectionJobs = ConcurrentHashMap<UUID, Job>()

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

    fun getClient(id: UUID): DatabaseClient {
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
        logger.info { "Connection Service => Force Client Disconnect" }
        clientConnectionJobs.values.forEach { it.cancelAndJoin() }
        databaseClients.values.map { connection ->
            async {
                try {
                    logger.info { "Connection Service => ${connection.config.databaseType} Database => ${connection.id} => ${connection.config.connectionName} => Disconnecting..." }
                    connection.disconnect()
                    logger.info { "Connection Service => ${connection.config.databaseType} Database => ${connection.id} => ${connection.config.connectionName} => Disconnected Successfully" }
                } catch (e: Exception) {
                    logger.error(e) { "Connection Service => ${connection.config.databaseType} Database => ${connection.id} => ${connection.config.connectionName} => Unsuccessful Disconnect Attempt" }
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


    override fun destroy() {
        logger.info { "ConnectionService is destroying, cancelling coroutine scope..." }
        cancel() // Cancel the CoroutineScope when the bean is destroyed
        logger.info { "Coroutine scope cancelled." }
    }
}
