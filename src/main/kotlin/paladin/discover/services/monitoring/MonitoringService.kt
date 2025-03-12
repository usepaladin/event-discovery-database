package paladin.discover.services.monitoring

import io.debezium.engine.ChangeEvent
import io.debezium.engine.DebeziumEngine
import io.debezium.engine.format.Json
import io.github.oshai.kotlinlogging.KLogger
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Service
import paladin.discover.configuration.KafkaConfiguration
import paladin.discover.configuration.properties.DebeziumConfigurationProperties
import paladin.discover.models.common.DatabaseType
import paladin.discover.models.configuration.TableConfiguration
import paladin.discover.models.monitoring.MySQLConnector
import paladin.discover.models.monitoring.PostgresConnector
import paladin.discover.pojo.client.DatabaseClient
import paladin.discover.pojo.monitoring.DatabaseMonitoringConnector
import paladin.discover.services.configuration.TableConfigurationService
import paladin.discover.services.connection.ConnectionService
import java.io.File
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
    private val kafkaConfiguration: KafkaConfiguration,
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
            // Add this before initializing the Debezium engine
            val offsetDir = File(debeziumConfigurationProperties.offsetStorageDir)
            if (!offsetDir.exists()) {
                offsetDir.mkdirs()
            }

            /**
             * ChangeEvent v RecordChangeEvent
             * The RecordChangeEvent interface in Debezium is used in specific scenarios
             * when you need a higher-level abstraction for change events
             *
             * Currently we are using ChangeEvent<String, String> which is a generic implementation of RecordChangeEvent
             * which provides access to the raw change event data for the pure focus of Pushing events to Kafka
             */

            monitoringConnector.updateConnectionState(DatabaseMonitoringConnector.MonitoringConnectionState.Connecting)
            val engine: DebeziumEngine<ChangeEvent<String, String>> =
                //todo: Incorporate Preferences (ie. Schema type)
                DebeziumEngine.create(Json::class.java)
                    .using(monitoringConnector.getConnectorProps())
                    .notifying { record -> handleObservation(record) }
                    .build()

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
                debeziumConfigurationProperties,
                kafkaConfiguration.getKafkaBootstrapServers()
            )

            DatabaseType.MYSQL -> MySQLConnector(
                client,
                configuration,
                debeziumConfigurationProperties,
                kafkaConfiguration.getKafkaBootstrapServers()
            )

            else -> {
                throw IllegalArgumentException("Database Type Not Supported")
            }
        }
    }

    fun updateMonitoringConfiguration() {}

    fun stopMonitoringEngine(databaseId: UUID) {
        val engine = monitoringEngines.remove(databaseId)

        if (engine == null) {
            logger.info {
                "CDC Monitoring Service => Database Id: $databaseId => No monitoring engine found =>" +
                        "An engine potentially does not exist with the given Id, or has already been stopped by a previous request"
            }
            return
        }

        try {
            engine.close()
            logger.info { "CDC Monitoring Service => Database Id: $databaseId => Monitoring Engine Stopped Successfully" }
        } catch (e: IOException) {
            // Log the *specific* exception (IOException here) for better diagnostics.
            logger.error(e) { "CDC Monitoring Service => Database Id: $databaseId => Failed to close engine: ${e.message}" }
            // Consider re-throwing, or wrapping in a custom exception, *if* higher levels need to handle this failure.
        } catch (e: Exception) {
            logger.error(e) { "CDC Monitoring Service => Database Id: $databaseId => Unexpected error while stopping engine: ${e.message}" }
        }
    }

    /**
     * Notification handler upon observation of a Database record alteration
     */
    private fun handleObservation(record: ChangeEvent<String, String>) {
        logger.info { "CDC Monitoring Service => Database Id: ${record.key()} => Record Observed: ${record.value()}" }
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