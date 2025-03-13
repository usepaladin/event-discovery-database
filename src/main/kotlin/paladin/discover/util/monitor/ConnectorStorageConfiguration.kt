package paladin.discover.util.monitor

import paladin.discover.configuration.properties.DebeziumConfigurationProperties
import java.util.*

interface ConnectorStorageConfiguration {
    fun validateConfig(config: DebeziumConfigurationProperties)
    fun applyProperties(props: Properties, config: DebeziumConfigurationProperties)
}