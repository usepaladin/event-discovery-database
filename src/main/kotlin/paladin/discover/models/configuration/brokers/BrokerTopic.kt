package paladin.discover.models.configuration.brokers

import paladin.discover.entities.brokers.BrokerTopicConfigurationEntity
import paladin.discover.entities.brokers.MessageBrokerConfigurationEntity
import paladin.discover.enums.configuration.BrokerType
import paladin.discover.pojo.configuration.brokers.EncryptedBrokerAuthConfig
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
        fun factory(
            entity: BrokerTopicConfigurationEntity,
            brokerConfigurationEntity: MessageBrokerConfigurationEntity,
            authConfig: EncryptedBrokerAuthConfig
        ): BrokerTopic {
            return BrokerTopic(
                id = entity.id ?: throw IllegalArgumentException("BrokerTopic ID cannot be null"),
                broker = MessageBroker.factory(
                    brokerConfigurationEntity, authConfig
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

