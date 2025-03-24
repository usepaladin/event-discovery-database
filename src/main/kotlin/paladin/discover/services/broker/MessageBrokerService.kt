package paladin.discover.services.broker

import io.github.oshai.kotlinlogging.KLogger
import org.springframework.stereotype.Service
import paladin.discover.models.brokers.BrokerTopic
import paladin.discover.models.brokers.MessageBroker
import paladin.discover.repositories.brokers.BrokerTopicRepository
import paladin.discover.repositories.brokers.MessageBrokerRepository
import java.util.concurrent.ConcurrentHashMap

@Service
class MessageBrokerService(

    private val logger: KLogger,
    private val messageBrokerRepository: MessageBrokerRepository,
    private val brokerTopicRepository: BrokerTopicRepository
) {

    private val messageBrokers = ConcurrentHashMap<String, MessageBroker>()
    private val brokerTopics = ConcurrentHashMap<String, BrokerTopic>()
}