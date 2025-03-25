package paladin.discover.pojo.configuration.brokers

import paladin.discover.enums.configuration.BrokerType

data class KafkaBrokerConfig(
    override val brokerType: BrokerType = BrokerType.KAFKA,
    val bootstrapServers: String,
    val clientId: String,
    val groupId: String?,
    val enableAutoCommit: Boolean = false,
    val autoCommitIntervalMs: Int = 5000,
    val requestTimeoutMs: Int = 30000,
    val retries: Int = 5,
    val acks: String = "all",
) : BrokerConfig