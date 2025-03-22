package paladin.discover.services.connection

import io.github.oshai.kotlinlogging.KLogger
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import paladin.discover.pojo.client.DatabaseClient.ClientConnectionState
import java.util.*

/**
 * Todo: Further Expand Functionality: I just made barebones monitoring capabilities `for the s`hits n giggles
 *
 * - Configurable Polling Intervals
 * - Configurable Reconnect Attempts + Backoff
 * - Connection State History
 * - Event Pushing to Kafka Broker
 * - Integration with Spring Actuator + Health Checks
 */

@Service
class ConnectionMonitoringService(
    private val connectionService: ConnectionService,
    private val logger: KLogger,
    @Qualifier("coroutineDispatcher") private val dispatcher: CoroutineDispatcher
) {

    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    /**
     * Continuously monitor database connections throughout the lifecycle of the application and will do the following:
     *  - Monitor the Connection Status of the databases, and will attempt to reconnect on failure
     *  - Listen for any changes to the connection state of the database, and will handle the state changes accordingly
     */
    fun monitorDatabaseConnections() {
        scope.launch {
            logger.info { "DDS Monitoring => Connection Monitoring Service => Monitoring Database Connection State" }
            // Continuously observe and monitor connection states to handle state changes
            observeConnectionStates()
                .collect { handleConnectionStateChange(it) }
        }

        scope.launch {
            logger.info { "DDS Monitoring => Connection Monitoring Service => Monitoring Database Connection Health" }
            // Continuously monitor connection states and retry on failure/non-user intended disconnect
            monitorConnections()
        }
    }

    private suspend fun handleConnectionStateChange(states: Map<UUID, ClientConnectionState>) {
        logger.info { "DDS Monitoring => Connection Monitoring Service => Connection State Change Detected" }
        println(states.values)
    }

    /**
     * Continuous monitoring of database connections, checking all vital signs of each connection.
     * Should be run as a background task on a separate thread, and will attempt to reconnect to a database
     * if it has been disconnected not at the will of the user (ie. Paused, Connecting, etc)
     */
    private fun monitorConnections() = scope.launch {
        while (isActive) {
            coroutineScope {
                connectionService.getAllClients().forEach { connection ->
                    logger.info { "DDS Monitoring => Connection Monitoring Service => Attempting Database Connection Health Check" }
                    launch {
                        try {
                            if (!connection.isConnected() && connection.connectionState.value == ClientConnectionState.Disconnected) {
                                logger.info { "DDS Monitoring => ${connection.config.databaseType} Database => ${connection.id} => ${connection.config.connectionName} => Database Connection Lost, attempting reconnect..." }
                                connection.connect()
                                logger.info { "DDS Monitoring => ${connection.config.databaseType} Database => ${connection.id} => ${connection.config.connectionName} => Database Reconnect Successful" }
                            } else {
                                logger.info { "DDS Monitoring => ${connection.config.databaseType} Database => ${connection.id} => ${connection.config.connectionName} => Database Connection Healthy" }
                            }
                        } catch (e: Exception) {
                            logger.error(e) { "DDS Monitoring => ${connection.config.databaseType} Database => ${connection.id} => ${connection.config.connectionName} => Database Reconnect Unsuccessful" }
                        }
                    }
                }
            }

            delay(30000)
        }
    }

    fun observeConnectionStates(): Flow<Map<UUID, ClientConnectionState>> = flow {
        val stateFlows = connectionService.getAllClients().map { connection ->
            connection.connectionState.map { state -> connection.id to state }
        }

        merge(*stateFlows.toTypedArray())
            .scan(emptyMap<UUID, ClientConnectionState>()) { acc, (id, state) ->
                acc + (id to state)
            }
            .collect { emit(it) }
    }

    @PreDestroy
    fun endMonitoring() {
        logger.info { "DDS Monitoring => Connection Monitoring Service => Stopping Connection Monitoring Service" }
        scope.cancel()
    }

}