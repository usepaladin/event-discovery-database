package paladin.discover.entities.brokers

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import jakarta.persistence.*
import org.hibernate.annotations.Type
import paladin.discover.enums.configuration.BrokerFormat
import paladin.discover.enums.configuration.BrokerType
import java.time.ZonedDateTime
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
@Table(
    name = "message_brokers",
    schema = "database_discovery",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["binder_name"])
    ],
    indexes = [
        Index(name = "idx_message_brokers_binder_name", columnList = "binder_name")
    ]
)
/**
 * Represents the configuration details about each user provided message broker that will receive messages from the database change events
 *
 * All sensitive configuration details associated with the connection of a message broker, will be default,
 * be encrypted when stored in the database, and decrypted upon retrieval, this would include:
 *  - Connection details (IP, Port, etc)
 *  - Authentication details (if required)
 *
 * Less sensitive details would be stored in a Binary JSON format, this would include:
 * - Binder Configuration details
 * - Topic Configuration details
 * - Producer Configuration details
 * - Consumer Configuration details
 *
 * If the server instance does not require encryption, the object will be stored as a JSON object in string form
 */
data class MessageBrokerConfigurationEntity(
    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "UUID DEFAULT uuid_generate_v4()", nullable = false)
    val id: UUID? = null,

    @Column(name = "binder_name", nullable = false, unique = true)
    var binderName: String,

    @Column(name = "broker_type", nullable = false)
    @Enumerated(EnumType.STRING)
    val brokerType: BrokerType,

    @Column(name = "broker_format", nullable = false)
    @Enumerated(EnumType.STRING)
    val brokerFormat: BrokerFormat,

    @Column(name = "enc_broker_config", nullable = false)
    var brokerConfigEncrypted: String,

    @Type(JsonBinaryType::class)
    @Column(name = "broker_config", nullable = false, columnDefinition = "JSONB")
    //todo: type it
    var brokerConfig: Map<String, Any>,

    @Column(name = "default_broker")
    var defaultBroker: Boolean = false,

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP", updatable = false)
    var createdAt: ZonedDateTime = ZonedDateTime.now(),

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    var updatedAt: ZonedDateTime = ZonedDateTime.now()
)
