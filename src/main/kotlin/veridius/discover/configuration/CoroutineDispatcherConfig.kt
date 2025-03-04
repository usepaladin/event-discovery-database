package veridius.discover.configuration

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CoroutineDispatcherConfig {

    @Bean
    fun coroutineDispatcher(): CoroutineDispatcher {
        return Dispatchers.IO // Default dispatcher for production
    }
}