package paladin.discover.models.configuration.brokers

import paladin.discover.entities.brokers.BrokerTopicConfigurationEntity
import paladin.discover.enums.configuration.BrokerType
import java.time.ZonedDateTime
import java.util.*

data class BrokerTopic(
    val id: UUID,
    val brokerName: String,
    val topicName: String,
    val topicBinding: String,
    val topicFormat: BrokerType,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime
) {
    companion object {
        fun factory(
            entity: BrokerTopicConfigurationEntity,
            brokerName: String
        ): BrokerTopic {
            return BrokerTopic(
                id = entity.id ?: throw IllegalArgumentException("BrokerTopic ID cannot be null"),
                brokerName = brokerName,
                topicName = entity.topicName,
                topicBinding = entity.topicBinding,
                topicFormat = entity.topicFormat,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}

