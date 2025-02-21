package veridius.discover.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "discover")
data class CoreConfigurationProperties(
    val requireDataEncryption: Boolean
)
