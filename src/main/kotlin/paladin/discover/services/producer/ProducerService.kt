package paladin.discover.services.producer

import io.github.oshai.kotlinlogging.KLogger
import jakarta.annotation.Nullable
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.kafka.support.KafkaHeaders

import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Service
import paladin.discover.configuration.CloudBinderConfiguration
import java.util.*

@Service
class ProducerService(
    private val streamBridge: StreamBridge,
    private val cloudBinderConfiguration: CloudBinderConfiguration,
    private val logger: KLogger
) {

    // In the current moment for simplicity I am going to keep the Key as a UUID,
    // Ill add configurability for the key in the future for additional flexibility and Kafka partitioning purposes
    // Plus it works well with Avro Schemas
    // Todo: Add configurability for the key
    fun <T : Any> sendMessage(binding: String, payload: T, @Nullable headers: List<String>? = null) {
        try {
            val key: UUID = UUID.randomUUID()
            logger.info { "Message Producer Service => Sending message to Message binding: $binding => Message Key: $key" }
            val messageBuilder: MessageBuilder<T> = MessageBuilder // Create a message with the payload and key
                .withPayload(payload)
                .setHeader(KafkaHeaders.KEY, key.toString())
            val message: Message<T> = messageBuilder.build()

            val messageSent = streamBridge.send(
                binding,
                message
            )


            if (messageSent) {
                logger.info { "Message Producer Service => Message sent to Message binding: $binding => Message Key: $key" }
            } else {
                logger.error { "Message Producer Service => Failed to send message to Message binding: $binding => Message Key: $key" }
            }


        } catch (e: Exception) {
            logger.error(e) { "Message Producer Service => Exception occurred sending message via StreamBridge: $binding" }
        }
    }

}