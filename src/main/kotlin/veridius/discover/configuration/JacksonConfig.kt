package veridius.discover.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.afterburner.AfterburnerModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JacksonConfig {

    @Bean
    fun objectMapper(): ObjectMapper {
        return ObjectMapper().apply {
            registerModule(JavaTimeModule()) // If you use Java 8 Date/Time API
            registerModule(KotlinModule.Builder().build())
            // Only register Afterburner if needed
            registerModule(AfterburnerModule())
            disable(SerializationFeature.FAIL_ON_EMPTY_BEANS) // Avoid Hibernate errors
        }
    }
}