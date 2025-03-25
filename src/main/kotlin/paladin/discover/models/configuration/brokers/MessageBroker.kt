package paladin.discover.models.configuration.brokers

import paladin.discover.entities.brokers.MessageBrokerConfigurationEntity
import paladin.discover.enums.configuration.BrokerFormat
import paladin.discover.enums.configuration.BrokerType
import java.time.ZonedDateTime
import java.util.*

data class MessageBroker(
    val id: UUID,
    val binderName: String,
    val brokerType: BrokerType,
    val brokerFormat: BrokerFormat,
    val defaultBroker: Boolean,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime
) {
    companion object {
        fun factory(entity: MessageBrokerConfigurationEntity): MessageBroker {
            return MessageBroker(
                id = entity.id ?: throw IllegalArgumentException("BrokerTopic ID cannot be null"),
                binderName = entity.binderName,
                brokerType = entity.brokerType,
                brokerFormat = entity.brokerFormat,
                defaultBroker = entity.defaultBroker,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}