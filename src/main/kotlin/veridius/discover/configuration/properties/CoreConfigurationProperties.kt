package veridius.discover.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.util.*

@ConfigurationProperties(prefix = "discover")
data class CoreConfigurationProperties(
    val requireDataEncryption: Boolean,
    val serverInstanceId: UUID
)
