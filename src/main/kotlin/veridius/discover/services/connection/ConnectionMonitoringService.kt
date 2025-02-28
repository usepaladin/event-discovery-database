package veridius.discover.services.connection

import jakarta.annotation.PreDestroy
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import veridius.discover.pojo.client.ConnectionState
import java.util.*

@Service
class ConnectionMonitoringService(private val connectionService: ConnectionService) {
    private val scope = CoroutineScope((Dispatchers.IO + SupervisorJob()))
    private val logger = LoggerFactory.getLogger(ConnectionMonitoringService::class.java)

    /**
     * Continuously monitor database connections throughout the lifecycle of the application and will do the following:
     *  - Monitor the Connection Status of the databases, and will attempt to reconnect on failure
     *  - Listen for any changes to the connection state of the database, and will handle the state changes accordingly
     */
    fun monitorDatabaseConnections() {
        scope.launch {
            // Continuously observe and monitor connection states to handle state changes
            connectionService.observeConnectionStates().collect { states ->
                handleConnectionStateChange(states)
            }
        }

        scope.launch {
            // Continuously monitor connection states and retry on failure/non-user intended disconnect
            connectionService.monitorConnections()
        }
    }

    private suspend fun handleConnectionStateChange(states: Map<UUID, ConnectionState>) {
        logger.info("Connection state change detected")
        println(states.values)
    }

    @PreDestroy
    fun endMonitoring() {
        scope.cancel()
    }

}