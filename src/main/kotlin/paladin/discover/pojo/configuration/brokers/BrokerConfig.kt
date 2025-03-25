package paladin.discover.pojo.configuration.brokers

import paladin.discover.enums.configuration.BrokerType
import java.io.Serializable

interface BrokerConfig : Serializable {
    val brokerType: BrokerType
}