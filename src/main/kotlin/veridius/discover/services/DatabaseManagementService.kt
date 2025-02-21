package veridius.discover.services

import mu.KotlinLogging
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import veridius.discover.configuration.properties.CoreConfigurationProperties
import veridius.discover.services.connection.ConnectionService



@Service
class DatabaseManagementService(
    private val connectionManager: ConnectionService,
    private val configuration: CoreConfigurationProperties
) {

    private val logger = KotlinLogging.logger {}

    @Bean
    fun initializeDatabases() = ApplicationRunner {
        configuration.databases.forEach{config ->
            connectionManager.createConnection(config)
        }

//        CoroutineScope(Dispatchers.IO).launch {
//            configurations.forEach { config ->
//                connectionManager.createConnection(config)
//            }
//
//            // Start connection monitoring
//            connectionManager.monitorConnections()
//
//            // Observe connection states
//            connectionManager.observeConnectionStates()
//                .collect { states ->
//                    states.forEach { (id, state) ->
//                        when (state) {
//                            is ConnectionState.Error -> logger.error("Connection $id error: ${state.exception}")
//                            is ConnectionState.Connected -> logger.info("Connection $id established")
//                            is ConnectionState.Disconnected -> logger.info("Connection $id disconnected")
//                            is ConnectionState.Connecting -> logger.info("Connection $id connecting...")
//                        }
//                    }
//                }
//        }
    }

}