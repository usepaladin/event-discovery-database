package veridius.discover.services.monitoring

import io.debezium.engine.ChangeEvent
import io.debezium.engine.DebeziumEngine
import io.debezium.engine.format.Json
import jakarta.annotation.PreDestroy
import mu.KLogger
import org.springframework.stereotype.Service
import veridius.discover.configuration.KafkaConfiguration
import veridius.discover.configuration.properties.DebeziumConfigurationProperties
import veridius.discover.models.common.DatabaseType
import veridius.discover.models.configuration.TableConfiguration
import veridius.discover.models.monitoring.MySQLConnector
import veridius.discover.models.monitoring.PostgresConnector
import veridius.discover.pojo.client.DatabaseClient
import veridius.discover.pojo.monitoring.DatabaseMonitoringConnector
import veridius.discover.services.configuration.TableConfigurationService
import veridius.discover.services.connection.ConnectionService
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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
            val offsetDir = File("/tmp/debezium/offsets/")
            if (!offsetDir.exists()) {
                offsetDir.mkdirs()
            }

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
            logger.error(e) { "CDC Monitoring Service => Database Id: $client.id => Failed to Start Monitoring Engine" }
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
        val engine: DebeziumEngine<ChangeEvent<String, String>> = monitoringEngines[databaseId]
            ?: throw IllegalArgumentException("No monitoring engine found for database id: $databaseId")

        try {
            engine.close()
        } catch (e: Exception) {
            logger.error(e) { "CDC Monitoring Service => Database Id: $databaseId => Failed to Stop Monitoring Engine" }
        } finally {
            logger.info { "CDC Monitoring Service => Database Id: $databaseId => Monitoring Engine Stopped" }
            monitoringEngines.remove(databaseId)
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
    }
}