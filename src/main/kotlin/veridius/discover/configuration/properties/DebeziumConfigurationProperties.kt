package veridius.discover.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "debezium.offset-storage")
class DebeziumConfigurationProperties(
    val type: String,
    val fileName: String
)