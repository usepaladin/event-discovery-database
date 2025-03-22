package paladin.discover.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.cloud.stream.config.BinderProperties

@ConfigurationProperties(prefix = "spring.cloud.stream")
data class CloudBinderConfiguration(
    val binders: Map<String, BinderProperties> = emptyMap(),
    val defaultBinder: String? = null
) {
    fun getBinderConfiguration(binderName: String): BinderProperties? {
        return binders[binderName]
    }

    fun getAllBinders(): List<Pair<String, BinderProperties>> {
        return binders.flatMap { (name, config) -> listOf(name to config) }
    }

    fun getInternalBinders(): List<Pair<String, BinderProperties>> {
        return binders.filter { (_, config) -> config.environment["type"] == "internal" }
            .flatMap { (name, config) -> listOf(name to config) }
    }
}