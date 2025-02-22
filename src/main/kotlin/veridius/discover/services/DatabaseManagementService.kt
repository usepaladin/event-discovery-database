package veridius.discover.services

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
import veridius.discover.services.connection.ConnectionService
import veridius.discover.services.monitoring.MonitoringService

/**
 * Core database management service, Responsible for:
 *  - Service initialization
 *  - Pausing monitoring flow to update configurations based on user requests
 *  - Core operations
 */
@Service
class DatabaseManagementService(
    private val configurationService: ConfigurationService,
    private val connectionService: ConnectionService,
    private val monitoringService: MonitoringService
) {

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
    @PostConstruct
    fun init() {

    }
}