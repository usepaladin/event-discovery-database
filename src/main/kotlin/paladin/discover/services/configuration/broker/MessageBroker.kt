package paladin.discover.services.configuration.broker

import paladin.discover.enums.configuration.BrokerFormat
import paladin.discover.enums.configuration.BrokerType
import java.time.ZonedDateTime
import java.util.*

data class MessageBroker(
    val id: UUID,
    val brokerName: String,
    val brokerType: BrokerType,
    val brokerFormat: BrokerFormat,
    val defaultBroker: Boolean,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime
)