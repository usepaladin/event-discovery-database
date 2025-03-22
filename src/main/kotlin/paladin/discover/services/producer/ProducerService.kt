package paladin.discover.services.producer

import io.github.oshai.kotlinlogging.KLogger
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Service
import paladin.discover.configuration.CloudBinderConfiguration
import paladin.discover.pojo.producer.DynamicBindingProperties

@Service
class ProducerService(
    private val streamBridge: StreamBridge,
    private val cloudBinderConfiguration: CloudBinderConfiguration,
    private val topicBindingService: TopicBindingService,
    private val logger: KLogger
) {

    fun <T : Any, V> sendMessage(binding: String, key: V, payload: T) {
        try {
            logger.info { "Message Producer Service => Sending message to Message binding: $binding => Message Key: ${key.toString()}" }
            val message: Message<T> = buildMessage(payload, key)
            val messageSent = streamBridge.send(
                binding,
                message
            )
            handleSendConfirmation(messageSent, binding, key)

        } catch (e: Exception) {
            logger.error(e) { "Message Producer Service => Exception occurred sending message via StreamBridge: $binding" }
        }
    }

    fun <T : Any, V> sendMessage(
        binding: String,
        binder: String,
        key: V,
        payload: T,
    ) {
        try {
            logger.info { "Message Producer Service => Sending message to Message binding: $binding => Message Key: ${key.toString()}" }
            val message: Message<T> = buildMessage(payload, key)
            val messageSent = streamBridge.send(
                binding,
                binder,
                message
            )
            handleSendConfirmation(messageSent, binding, key)
        } catch (e: Exception) {
            logger.error(e) { "Message Producer Service => Exception occurred sending message via StreamBridge: $binding" }
        }
    }

    /** Sending messages to dynamically generated bindings during runtime, configurations would be user set and stored
    in a database that would be cached for quick access, this would allow for the creation of new bindings that
    dictate which topic it is mapped to, and the binder that is used to send the message to the topic.
     */
    fun <T : Any, V> sendMessage(
        binding: String,
        bindingConfig: DynamicBindingProperties,
        key: V,
        payload: T,
    ) {
        try {
            logger.info { "Message Producer Service => Sending message to Message binding: $binding => Message Key: ${key.toString()}" }
            val message: Message<T> = buildMessage(payload, key)
            // Creates a new binding if destination topic does not exist (ie. new database tables being observed)
            createBindingIfNotExist(binding, bindingConfig)
            val messageSent = streamBridge.send(
                binding,
                message
            )

            handleSendConfirmation(messageSent, binding, key)
        } catch (e: Exception) {
            logger.error(e) { "Message Producer Service => Exception occurred sending message via StreamBridge: $binding" }
        }
    }

    private fun <T : Any, V> buildMessage(payload: T, key: V, headers: List<Pair<String, String>>? = null): Message<T> {
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

    private fun <T> handleSendConfirmation(messageSent: Boolean, binding: String, key: T) {
        if (messageSent) {
            logger.info { "Message Producer Service => Message sent to Message binding: $binding => Message Key: ${key.toString()}" }
        } else {
            logger.error { "Message Producer Service => Failed to send message to Message binding: $binding => Message Key: ${key.toString()}" }
        }
    }

    private fun createBindingIfNotExist(binding: String, bindingConfig: DynamicBindingProperties) {
        if (topicBindingService.hasBinding(binding)) return
        // Create a new binder to handle events for this database operation
        topicBindingService.createDynamicTopicBinding(binding, bindingConfig)
    }
}