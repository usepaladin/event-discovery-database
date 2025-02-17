package veridius.discover.services.connection;

import org.springframework.stereotype.Service
import veridius.discover.configuration.properties.DatabaseConfigurationProperties.DatabaseConnectionConfiguration
import java.io.IOException

@Service
class DatabaseConnectionManager {
    private val activeConnections = mutableMapOf<String, DatabaseConnection>()





}
