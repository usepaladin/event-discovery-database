package paladin.discover.repositories.brokers

import org.springframework.data.jpa.repository.JpaRepository
import paladin.discover.entities.brokers.MessageBrokerConfigurationEntity
import paladin.discover.enums.configuration.BrokerType
import java.util.*

interface MessageBrokerRepository : JpaRepository<MessageBrokerConfigurationEntity, UUID> {

    fun findByDefaultBrokerIsTrue(): MessageBrokerConfigurationEntity?
    fun findAllByBrokerType(brokerType: BrokerType): List<MessageBrokerConfigurationEntity>
}