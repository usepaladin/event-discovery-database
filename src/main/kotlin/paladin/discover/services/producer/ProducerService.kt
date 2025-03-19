package paladin.discover.services.producer

import io.github.oshai.kotlinlogging.KLogger
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Service
import paladin.discover.configuration.CloudBinderConfiguration
import java.util.*

@Service
class ProducerService(
    private val streamBridge: StreamBridge,
    private val cloudBinderConfiguration: CloudBinderConfiguration,
    private val topicBindingService: TopicBindingService,
    private val logger: KLogger
) {

    // In the current moment for simplicity I am going to keep the Key as a UUID,
    // Ill add configurability for the key in the future for additional flexibility and Kafka partitioning purposes
    // Plus it works well with Avro Schemas
    // Todo: Add configurability for the key
    fun <T : Any> sendMessage(binding: String, payload: T, headers: List<Pair<String, String>>? = null) {
        try {
            val key: UUID = UUID.randomUUID()
            logger.info { "Message Producer Service => Sending message to Message binding: $binding => Message Key: $key" }
            val message: Message<T> = buildMessage(payload, key, headers)
            val messageSent = streamBridge.send(
                binding,
                message
            )
            handleMessageSend(messageSent, binding, key)

        } catch (e: Exception) {
            logger.error(e) { "Message Producer Service => Exception occurred sending message via StreamBridge: $binding" }
        }
    }

    fun <T : Any> sendMessage(
        binding: String,
        binder: String,
        payload: T,
        headers: List<Pair<String, String>>? = null
    ) {
        try {

            val key: UUID = UUID.randomUUID()
            logger.info { "Message Producer Service => Sending message to Message binding: $binding => Message Key: $key" }
            val message: Message<T> = buildMessage(payload, key, headers)
            val messageSent = streamBridge.send(
                binding,
                binder,
                message
            )
            handleMessageSend(messageSent, binding, key)
        } catch (e: Exception) {
            logger.error(e) { "Message Producer Service => Exception occurred sending message via StreamBridge: $binding" }
        }
    }

    private fun <T : Any, V> buildMessage(payload: T, key: V, headers: List<Pair<String, String>>?): Message<T> {
        val messageBuilder: MessageBuilder<T> = MessageBuilder // Create a message with the payload and key
            .withPayload(payload)
            .setHeader("message-key", key)

        // Add any additional headers to the message
        headers?.let {
            headers.forEach { (key, value) ->
                messageBuilder.setHeader(key, value)
            }
        }

        return messageBuilder.build()
    }

    private fun <T> handleMessageSend(messageSent: Boolean, binding: String, key: T) {
        if (messageSent) {
            logger.info { "Message Producer Service => Message sent to Message binding: $binding => Message Key: $key" }
        } else {
            logger.error { "Message Producer Service => Failed to send message to Message binding: $binding => Message Key: $key" }
        }
    }

    private fun createBinderIfNotExist(binding: String) {
        if (topicBindingService.hasBinding(binding)) return

        // Create a new binder to handle events for this database operation
    }

}