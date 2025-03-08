package veridius.discover.configuration

import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Executors

@Configuration
class CoroutineConfig {


    private val dispatcher: ExecutorCoroutineDispatcher = Executors.newFixedThreadPool(10).asCoroutineDispatcher()

    @Bean
    fun coroutineDispatcher(): CoroutineDispatcher = dispatcher

    @PreDestroy
    fun closeDispatcher() {
        dispatcher.close()
    }

}