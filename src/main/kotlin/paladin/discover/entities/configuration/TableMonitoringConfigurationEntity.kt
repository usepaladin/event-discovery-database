package paladin.discover.entities.configuration

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import jakarta.persistence.*
import org.hibernate.annotations.Type
import paladin.discover.entities.brokers.MessageBrokerConfigurationEntity
import paladin.discover.entities.connection.DatabaseConnectionEntity
import paladin.discover.pojo.configuration.TableColumnConfiguration
import paladin.discover.pojo.configuration.TableMetadataConfiguration
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(
    name = "table_monitoring_configuration",
    schema = "database_discovery",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["database_connection_id", "table_name"]),
        UniqueConstraint(columnNames = ["database_connection_id", "namespace", "table_name"])

    ],
    indexes = [
        Index(name = "idx_table_monitoring_database_id", columnList = "database_connection_id"),
        Index(name = "idx_table_monitoring_namespace", columnList = "namespace"),
    ]
)
data class TableMonitoringConfigurationEntity(
    @Id
    @GeneratedValue
    @Column(
        name = "table_monitoring_configuration_id",
        columnDefinition = "UUID DEFAULT uuid_generate_v4()",
        nullable = false
    )
    val id: UUID? = null,

    @Column(name = "database_connection_id", nullable = false)
    val databaseConnectionId: UUID,

    @ManyToOne
    @JoinColumn(
        name = "database_connection_id",
        referencedColumnName = "database_connection_id",
        insertable = false,
        updatable = false
    )
    val databaseConnection: DatabaseConnectionEntity? = null,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
        name = "message_broker_id",
        referencedColumnName = "message_broker_id",
        insertable = false,
        updatable = false,
    )
    val messageBroker: MessageBrokerConfigurationEntity? = null,

    @Column(name = "namespace")
    val namespace: String? = null,

    @Column(name = "table_name", nullable = false)
    val tableName: String,

    @Column(name = "is_enabled", nullable = false)
    var isEnabled: Boolean = true,

    @Column(name = "include_all_columns", nullable = false)
    var includeAllColumns: Boolean = true,

    @Type(JsonBinaryType::class)
    @Column(name = "table_metadata", columnDefinition = "JSONB")
    var metadata: TableMetadataConfiguration? = null,

    @Type(JsonBinaryType::class)
    @Column(name = "table_columns", columnDefinition = "JSONB")
    var columns: List<TableColumnConfiguration>? = null,

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP", updatable = false)
    var createdAt: ZonedDateTime = ZonedDateTime.now(),

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    var updatedAt: ZonedDateTime = ZonedDateTime.now()
)


