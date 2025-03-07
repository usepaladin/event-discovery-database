package veridius.discover.services.monitoring

import io.debezium.engine.DebeziumEngine
import io.debezium.engine.RecordChangeEvent
import io.debezium.engine.format.ChangeEventFormat
import jakarta.annotation.PreDestroy
import mu.KLogger
import org.springframework.stereotype.Service
import veridius.discover.configuration.properties.DebeziumConfigurationProperties
import veridius.discover.models.common.DatabaseType
import veridius.discover.models.configuration.TableConfiguration
import veridius.discover.models.monitoring.MySQLConnector
import veridius.discover.models.monitoring.PostgresConnector
import veridius.discover.pojo.client.DatabaseClient
import veridius.discover.pojo.monitoring.DatabaseMonitoringConnector
import veridius.discover.services.configuration.TableConfigurationService
import veridius.discover.services.connection.ConnectionService
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Service
class MonitoringService(
    private val debeziumConfigurationProperties: DebeziumConfigurationProperties,
    private val connectionService: ConnectionService,
    private val configurationService: TableConfigurationService,
    private val logger: KLogger,
) {
    private val executor: ExecutorService = Executors.newFixedThreadPool(4)
    private val monitoringEngines = ConcurrentHashMap<UUID, DebeziumEngine<RecordChangeEvent<ByteArray>>>()

    fun startMonitoringEngine(client: DatabaseClient) {
        val tableConfigurations: List<TableConfiguration> =
            configurationService.getDatabaseClientTableConfiguration(client)

        val monitoringConnector: DatabaseMonitoringConnector =
            buildDatabaseMonitoringConnector(client, tableConfigurations)

        logger.info { "CDC Monitoring Service => Database Id: ${client.id} => Starting Monitoring Engine" }
        try {

            val engine: DebeziumEngine<RecordChangeEvent<ByteArray>> =
                //todo: Incorporate Preferences (ie. Schema type)
                DebeziumEngine.create(ChangeEventFormat.of(io.debezium.engine.format.Avro::class.java))
                    .using(monitoringConnector.getConnectorProps())
                    .notifying { record -> handleObservation(record) }
                    .build()

            monitoringEngines[client.id] = engine
            executor.execute(engine)
            logger.info { "CDC Monitoring Service => Database Id: $client.id => Monitoring Engine Instantiated and Started" }
        } catch (e: Exception) {
            logger.error(e) { "CDC Monitoring Service => Database Id: $client.id => Failed to Start Monitoring Engine" }
        }
    }

    private fun buildDatabaseMonitoringConnector(
        client: DatabaseClient,
        configuration: List<TableConfiguration>
    ): DatabaseMonitoringConnector {
        return when (client.config.databaseType) {
            DatabaseType.POSTGRES -> PostgresConnector(
                client,
                configuration,
                debeziumConfigurationProperties.fileDir
            )

            DatabaseType.MYSQL -> MySQLConnector(client, configuration, debeziumConfigurationProperties.fileDir)
            else -> {
                throw IllegalArgumentException("Database Type Not Supported")
            }
        }
    }

    fun updateMonitoringConfiguration() {}

    fun stopMonitoringEngine(databaseId: UUID) {
        val engine: DebeziumEngine<RecordChangeEvent<ByteArray>> = monitoringEngines[databaseId]
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
    private fun handleObservation(record: RecordChangeEvent<ByteArray>) {}

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