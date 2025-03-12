package paladin.discover.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class KafkaConfiguration {

    @Value("\${spring.kafka.bootstrap-servers}")
    private lateinit var kafkaBootstrapServers: String

    fun getKafkaBootstrapServers(): String {
        return kafkaBootstrapServers
    }
}