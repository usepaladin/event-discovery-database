package veridius.discover.entities.connection

import jakarta.persistence.*
import veridius.discover.entities.configuration.TableMonitoringConfigurationEntity
import veridius.discover.models.common.DatabaseType
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(
    name = "database_connection",
    schema = "database_discovery",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["connection_name"])
    ],
    indexes = [
        Index(name = "idx_database_connection_name", columnList = "connection_name"),
        Index(name = "idx_database_connection_instance_id", columnList = "instance_id")
    ]
)
/**
 * Represents the configuration details to procure a connection to a given database.
 *
 * All configuration details associated with the connection of a database, will be default, be encrypted when stored
 * in the database, and decrypted upon retrieval,
 * If the server instance does not require encryption, the object will be stored as a JSON object in string form
 */
data class DatabaseConnectionEntity(
    @Id
    @GeneratedValue
    @Column(name = "database_connection_id", columnDefinition = "UUID DEFAULT uuid_generate_v4()", nullable = false)
    val id: UUID? = null,

    @Column(name = "instance_id", nullable = false)
    val instanceId: UUID,

    @Column(name = "connection_name", nullable = false, unique = true)
    var connectionName: String,

    @Column(name = "database_type", nullable = false)
    @Enumerated(EnumType.STRING)
    val databaseType: DatabaseType,

    @Column(name = "host", nullable = false)
    var hostName: String,

    @Column(name = "port", nullable = false)
    var port: String,

    @Column(name = "database_name", nullable = false)
    var databaseName: String,

    @Column(name = "username", nullable = false)
    var user: String,

    @Column(name = "password")
    var password: String? = null,

    /**
     * Additional properties will be stored as a String, this is because the object will be encrypted into a string
     * for most server instances as Encryption will be enabled, in the event that the server does not require
     * encryption (ie. Local Development/Hosting) the JSON object will just be stored in string form
     */
    @Column(name = "additional_properties", columnDefinition = "TEXT")
    var additionalPropertiesText: String? = null,

    @Column(name = "is_enabled", nullable = false)
    var isEnabled: Boolean = true,

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP", updatable = false)
    var createdAt: ZonedDateTime = ZonedDateTime.now(),

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    var updatedAt: ZonedDateTime = ZonedDateTime.now()
) {

    @OneToMany(mappedBy = "databaseConnection", fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    val tableMonitoringConfigurations: List<TableMonitoringConfigurationEntity> = mutableListOf()


}