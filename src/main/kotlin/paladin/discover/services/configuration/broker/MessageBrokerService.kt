package paladin.discover.services.configuration.broker

import io.github.oshai.kotlinlogging.KLogger
import org.springframework.stereotype.Service
import paladin.discover.models.configuration.brokers.BrokerTopic
import paladin.discover.models.configuration.brokers.MessageBroker
import paladin.discover.repositories.brokers.BrokerTopicRepository
import paladin.discover.repositories.brokers.MessageBrokerRepository
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service
class MessageBrokerService(

    private val logger: KLogger,
    private val messageBrokerRepository: MessageBrokerRepository,
    private val brokerTopicRepository: BrokerTopicRepository
) {
    private val messageBrokers = ConcurrentHashMap<UUID, MessageBroker>()
    private val brokerTopics = ConcurrentHashMap<UUID, BrokerTopic>()

    fun loadBrokersFromDatabase() {}
    fun loadTopicsFromDatabase() {}

    fun createBrokerConfiguration() {}
    fun createTopicConfiguration() {}

    fun updateBroker(broker: MessageBroker) {

    }

    fun updateTopic(topic: BrokerTopic) {

    }

    fun deleteBroker(brokerId: UUID) {

    }

    fun deleteTopic(topicId: UUID) {

    }
}