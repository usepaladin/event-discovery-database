package paladin.discover.models.brokers

import paladin.discover.entities.brokers.BrokerTopicConfigurationEntity
import paladin.discover.enums.configuration.BrokerType
import java.time.ZonedDateTime
import java.util.*

data class BrokerTopic(
    val id: UUID,
    val broker: MessageBroker,
    val topicName: String,
    val topicBinding: String,
    val topicFormat: BrokerType,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime
) {
    companion object {
        fun factory(entity: BrokerTopicConfigurationEntity): BrokerTopic {
            return BrokerTopic(
                id = entity.id ?: throw IllegalArgumentException("BrokerTopic ID cannot be null"),
                broker = MessageBroker.factory(
                    entity.broker ?: throw IllegalArgumentException("Broker cannot be null")
                ),
                topicName = entity.topicName,
                topicBinding = entity.topicBinding,
                topicFormat = entity.topicFormat,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}

