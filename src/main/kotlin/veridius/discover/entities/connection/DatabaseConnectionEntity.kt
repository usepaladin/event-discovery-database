package veridius.discover.entities.connection

import jakarta.persistence.*
import veridius.discover.entities.configuration.TableMonitoringConfigurationEntity
import java.time.ZonedDateTime
import java.util.UUID

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

    @Column(name = "enc_host", nullable = false)
    var encryptedHostname: String,

    @Column(name = "enc_port", nullable = false)
    var encryptedPort: String,

    @Column(name = "enc_database_name")
    var encryptedDatabaseName: String,

    @Column(name = "enc_user")
    var encryptedUser: String,

    @Column(name = "enc_password")
    var encryptedPassword: String,

    @Column(name = "enc_additional_properties", columnDefinition = "JSONB")
    @Convert(converter = AdditionalPropertiesConverter::class)
    var additionalProperties: AdditionalProperties,

    @Column(name = "is_enabled", nullable = false)
    var isEnabled: Boolean = true,

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP", updatable = false)
    var createdAt: ZonedDateTime = ZonedDateTime.now(),

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    var updatedAt: ZonedDateTime = ZonedDateTime.now()
){

    @OneToMany(mappedBy = "databaseConnection", fetch = FetchType.LAZY)
    val tableMonitoringConfigurations: List<TableMonitoringConfigurationEntity> = mutableListOf()

    enum class DatabaseType{
        CASSANDRA,
        MYSQL,
        POSTGRES,
        MONGO
    }

    data class AdditionalProperties(
        val dataCenter: String? = null,
        val public: Boolean = false,
        val keySpace: String? = null
    )

    @Converter
    class AdditionalPropertiesConverter : AttributeConverter<AdditionalProperties, String> {
        private val objectMapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()

        override fun convertToDatabaseColumn(attribute: AdditionalProperties?): String {
            return objectMapper.writeValueAsString(attribute ?: AdditionalProperties())
        }

        override fun convertToEntityAttribute(dbData: String?): AdditionalProperties {
            return dbData?.let { objectMapper.readValue(it, AdditionalProperties::class.java) } ?: AdditionalProperties()
        }
    }

}