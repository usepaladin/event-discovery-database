package paladin.discover.models.configuration.brokers

import paladin.discover.entities.brokers.MessageBrokerConfigurationEntity
import paladin.discover.enums.configuration.BrokerFormat
import paladin.discover.enums.configuration.BrokerType
import paladin.discover.pojo.configuration.brokers.BrokerConfig
import paladin.discover.pojo.configuration.brokers.EncryptedBrokerAuthConfig
import paladin.discover.util.configuration.brokers.BrokerConfigFactory
import java.time.ZonedDateTime
import java.util.*

data class MessageBroker(
    val id: UUID,
    val binderName: String,
    val brokerType: BrokerType,
    val brokerFormat: BrokerFormat,
    val defaultBroker: Boolean,
    val brokerConfig: BrokerConfig,
    val authConfig: EncryptedBrokerAuthConfig,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime
) {
    companion object {
        fun factory(entity: MessageBrokerConfigurationEntity, authConfig: EncryptedBrokerAuthConfig): MessageBroker {
            return MessageBroker(
                id = entity.id ?: throw IllegalArgumentException("BrokerTopic ID cannot be null"),
                binderName = entity.binderName,
                brokerType = entity.brokerType,
                brokerFormat = entity.brokerFormat,
                defaultBroker = entity.defaultBroker,
                createdAt = entity.createdAt,
                brokerConfig = BrokerConfigFactory.parseBrokerConfig(entity.brokerType, entity.brokerConfig),
                updatedAt = entity.updatedAt,
                authConfig = authConfig
            )
        }
    }
}