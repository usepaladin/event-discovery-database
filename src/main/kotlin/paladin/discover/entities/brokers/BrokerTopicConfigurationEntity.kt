package paladin.discover.entities.brokers

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.persistence.*
import paladin.discover.enums.configuration.BrokerType
import java.time.ZonedDateTime
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
@Table(
    name = "broker_topic",
    schema = "database_discovery",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["message_broker_id", "topic_name"]),
        UniqueConstraint(columnNames = ["message_broker_id", "topic_binding"])
    ],
    indexes = [
        Index(name = "idx_broker_topic_message_broker_id", columnList = "message_broker_id"),
        Index(name = "idx_broker_topic_topic_name", columnList = "topic_name")
    ]
)
data class BrokerTopicConfigurationEntity(
    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "UUID DEFAULT uuid_generate_v4()", nullable = false)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
        name = "broker_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false
    )
    val broker: MessageBrokerConfigurationEntity? = null,
    @Column(name = "topic_name", nullable = false)
    val topicName: String,
    @Column(name = "topic_binding", nullable = false)
    val topicBinding: String,
    @Column(name = "topic_format", nullable = false)
    @Enumerated(EnumType.STRING)
    val topicFormat: BrokerType,

    @Column(name = "schema", nullable = true, columnDefinition = "TEXT")
    val schema: String? = null,

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP", updatable = false)
    var createdAt: ZonedDateTime = ZonedDateTime.now(),

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    var updatedAt: ZonedDateTime = ZonedDateTime.now()
)