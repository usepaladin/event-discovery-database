package paladin.discover.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "encryption")
data class EncryptionConfigurationProperties(
    val key: String
)