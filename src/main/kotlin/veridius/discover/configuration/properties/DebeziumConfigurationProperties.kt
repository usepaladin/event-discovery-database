package veridius.discover.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "debezium")
data class DebeziumConfigurationProperties(
    val fileDir: String,
)