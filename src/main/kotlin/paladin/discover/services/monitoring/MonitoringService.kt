package paladin.discover.services.monitoring

import com.fasterxml.jackson.databind.JsonNode
import io.debezium.engine.ChangeEvent
import io.debezium.engine.DebeziumEngine
import io.github.oshai.kotlinlogging.KLogger
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Service
import paladin.discover.configuration.properties.DebeziumConfigurationProperties
import paladin.discover.enums.configuration.DatabaseType
import paladin.discover.models.configuration.TableConfiguration
import paladin.discover.models.monitoring.MySQLConnector
import paladin.discover.models.monitoring.PostgresConnector
import paladin.discover.pojo.client.DatabaseClient
import paladin.discover.pojo.monitoring.ChangeEventFormatHandler
import paladin.discover.pojo.monitoring.DatabaseMonitoringConnector
import paladin.discover.services.configuration.TableConfigurationService
import paladin.discover.services.connection.ConnectionService
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Todo:
 * - Implement JMX for monitoring engine pausing and resuming
 * - Implement JMX for real time monitoring statistics and metrics
 * - Configure connections to be monitored based on user preferences
 * */

@Service
class MonitoringService(
    private val debeziumConfigurationProperties: DebeziumConfigurationProperties,
    private val connectionService: ConnectionService,
    private val configurationService: TableConfigurationService,
    private val changeEventHandlerFactory: ChangeEventHandlerFactory,
    private val logger: KLogger,
) {

    private val executor: ExecutorService = Executors.newFixedThreadPool(4)
    private val monitoringEngines = ConcurrentHashMap<UUID, DebeziumEngine<ChangeEvent<String, String>>>()

    fun startMonitoring() {
        val clients: List<DatabaseClient> = connectionService.getAllClients()
        clients.forEach { client -> startMonitoringEngine(client) }
    }

    fun startMonitoringEngine(client: DatabaseClient) {
        val tableConfigurations: List<TableConfiguration> =
            configurationService.getDatabaseClientTableConfiguration(client)

        val monitoringConnector: DatabaseMonitoringConnector =
            buildDatabaseMonitoringConnector(client, tableConfigurations)

        logger.info { "CDC Monitoring Service => Database Id: ${client.id} => Starting Monitoring Engine" }
        try {
            // Will validate current storage backend configuration and make changes if required, otherwise will throw an exception if it cannot be fixed
            monitoringConnector.validateStorageBackend()

            /**
             *
             * ChangeEvent v RecordChangeEvent
             * The RecordChangeEvent interface in Debezium is used in specific scenarios
             * when you need a higher-level abstraction for change events
             */

            monitoringConnector.updateConnectionState(DatabaseMonitoringConnector.MonitoringConnectionState.Connecting)
            val changeEventHandler: ChangeEventFormatHandler<String, JsonNode> =
                changeEventHandlerFactory.createChangeEventHandler(
                    connector = monitoringConnector
                )

            val engine = changeEventHandler.createEngine()
            // Store the engine under the client's connection name, easier to retrieve from the event
            monitoringEngines[client.id] = engine
            executor.execute(engine)
            monitoringConnector.updateConnectionState(DatabaseMonitoringConnector.MonitoringConnectionState.Connected)
            logger.info { "CDC Monitoring Service => Database Id: ${client.id} => Monitoring Engine Instantiated and Started" }

        } catch (e: Exception) {
            logger.error(e) { "CDC Monitoring Service => Database Id: ${client.id} => Failed to Start Monitoring Engine" }
            monitoringConnector.updateConnectionState(DatabaseMonitoringConnector.MonitoringConnectionState.Error(e))
        }
    }

    /**
     * Utilises JMX to pause the actual monitoring task without shutting down connection to the engine
     */
    fun pauseMonitoringEngine(databaseId: UUID) {
        TODO("Not yet implemented")
    }

    /**
     * Utilises JMX to resume the actual monitoring task
     */
    fun resumeMonitoringEngine(databaseId: UUID) {
        TODO("Not yet implemented")
    }

    private fun buildDatabaseMonitoringConnector(
        client: DatabaseClient,
        configuration: List<TableConfiguration>
    ): DatabaseMonitoringConnector {
        return when (client.config.databaseType) {
            DatabaseType.POSTGRES -> PostgresConnector(
                client,
                configuration,
                debeziumConfigurationProperties
            )

            DatabaseType.MYSQL -> MySQLConnector(
                client,
                configuration,
                debeziumConfigurationProperties
            )

            else -> {
                throw IllegalArgumentException("Database Type Not Supported")
            }
        }
    }

    fun updateMonitoringConfiguration() {
        TODO("Not yet implemented")
    }

    fun stopMonitoringEngine(client: DatabaseClient) {
        val engine = monitoringEngines.remove(client.id)

        if (engine == null) {
            logger.info {
                "CDC Monitoring Service => Database Id: ${client.id} => Database Connection Name: ${client.config.connectionName} => No monitoring engine found =>" +
                        "An engine potentially does not exist with the given Id, or has already been stopped by a previous request"
            }
            return
        }

        try {
            engine.close()
            logger.info { "CDC Monitoring Service => Database Id: ${client.id} => Database Connection Name: ${client.config.connectionName} => Monitoring Engine Stopped Successfully" }
        } catch (e: IOException) {
            // Log the *specific* exception (IOException here) for better diagnostics.
            logger.error(e) { "CDC Monitoring Service => Database Id: ${client.id} => Database Connection Name: ${client.config.connectionName} => Failed to close engine: ${e.message}" }
            // Consider re-throwing, or wrapping in a custom exception, *if* higher levels need to handle this failure.
        } catch (e: Exception) {
            logger.error(e) { "CDC Monitoring Service => Database Id: ${client.id} => Database Connection Name: ${client.config.connectionName} => Unexpected error while stopping engine: ${e.message}" }
        }
    }

    @PreDestroy
    fun shutdownMonitoring() {
        monitoringEngines.forEach { (database, engine) ->
            logger.info { "CDC Monitoring Service => Database Id: $database => Shutting Down Database Monitoring Connection... " }
            engine.close()
        }
        monitoringEngines.clear()
        logger.info { "CDC Monitoring Service => All Database Monitoring Connections Shut Down" }
        executor.shutdown()

        // Wait for tasks to complete with a timeout
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                logger.warn { "CDC Monitoring Service => Executor did not terminate in the specified time." }
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            logger.error(e) { "CDC Monitoring Service => Shutdown interrupted" }
            executor.shutdownNow()
        }
    }
}