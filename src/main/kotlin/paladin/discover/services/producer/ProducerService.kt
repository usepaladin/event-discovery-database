package paladin.discover.services.producer

import io.github.oshai.kotlinlogging.KLogger
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.stereotype.Service

@Service
class ProducerService(private val streamBridge: StreamBridge, private val logger: KLogger) {

    fun <V, T> sendMessage(topic: String, key: V, payload: T) {
        try {
            logger.info { "Message Producer Service => Sending message to topic: $topic => Message Key: ${key.toString()}" }
            val messageSent = streamBridge.send(topic, payload)
            if (messageSent) {
                logger.info { "Message Producer Service => Message sent to topic: $topic => Message Key: ${key.toString()}" }
            } else {
                logger.error { "Message Producer Service => Failed to send message to topic: $topic => Message Key: ${key.toString()}" }
            }
        } catch (e: Exception) {
            logger.error(e) { "Message Producer Service => Exception occurred sending message via StreamBridge: $topic => Message Key: ${key.toString()}" }
        }
    }
}