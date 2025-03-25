package paladin.discover.services

import jakarta.annotation.PreDestroy
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import paladin.discover.models.connection.DatabaseConnectionConfiguration
import paladin.discover.services.configuration.database.DatabaseConfigurationService
import paladin.discover.services.configuration.database.TableConfigurationService
import paladin.discover.services.connection.ConnectionMonitoringService
import paladin.discover.services.connection.ConnectionService
import paladin.discover.services.monitoring.MonitoringService

/**
 * Core database management service, Responsible for:
 *  - Service initialization
 *  - Pausing monitoring flow to update configurations based on user requests
 *  - Core operations
 */
@Service
class DatabaseManagementService(
    private val databaseConfigurationService: DatabaseConfigurationService,
    private val connectionService: ConnectionService,
    private val connectionMonitoringService: ConnectionMonitoringService,
    private val tableConfigurationService: TableConfigurationService,
    private val monitoringService: MonitoringService,
    private val applicationContext: ApplicationContext
) : ApplicationRunner {
    /**
     * This method is called after the bean has been constructed and the dependencies have been injected.
     *
     * This serves as the main initialization process for the service, as the flow begins with the retrieval
     * of all relevant Database and Setting based configurations from the database
     *
     * This will start the process of:
     *  1. Retrieving all relevant configurations + validation
     *  2. Attempting to connect to each monitored database
     *  3. Proceeding with further configuration updates + fetching based on database status
     *  4. Begin monitoring the databases
     */
    override fun run(args: ApplicationArguments?) {
        startDatabaseProcessing()
    }

    fun startDatabaseProcessing() {
        // Retrieve the connection configuration of all databases we aim to connect to
        val databaseConfig: List<DatabaseConnectionConfiguration> =
            databaseConfigurationService.fetchAllEnabledDatabaseConnectionConfigurations()

        // Attempt database connection and configuration
        runBlocking {
            databaseConfig.map {
                async {
                    val client = connectionService.createConnection(it)
                    client?.let {
                        it.connect()
                        tableConfigurationService.scanDatabaseTableConfiguration(it)
                    }
                }
            }.awaitAll()
        }

        // Set up background connection monitoring
        connectionMonitoringService.monitorDatabaseConnections()
        // Start monitoring engines
        monitoringService.startMonitoring()

//        // Fetch current database table configurations and update database if any changes have occurred
//        destroy()
//        SpringApplication.exit(applicationContext)
//        exitProcess(0)
    }

    @PreDestroy
    fun destroy() {
        // Disconnect from all active database connections
        runBlocking {
            monitoringService.shutdownMonitoring()
            connectionService.disconnectAll(removeConnections = true)
        }
    }

}