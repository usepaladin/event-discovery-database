package veridius.discover.services.configuration

import org.springframework.stereotype.Service
import veridius.discover.configuration.properties.CoreConfigurationProperties
import veridius.discover.repositories.configuration.TableConfigurationRepository

@Service
class TableConfigurationService(
    private val serverConfig: CoreConfigurationProperties,
    private val tableMonitoringRepository: TableConfigurationRepository,
)