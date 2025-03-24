package paladin.discover.repositories.brokers

import org.springframework.data.jpa.repository.JpaRepository
import paladin.discover.entities.brokers.BrokerTopicConfigurationEntity
import java.util.*

interface BrokerTopicRepository : JpaRepository<BrokerTopicConfigurationEntity, UUID>