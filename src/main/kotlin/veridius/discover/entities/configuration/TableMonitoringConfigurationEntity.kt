package veridius.discover.entities.configuration

import jakarta.persistence.*
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(
    name = "table_monitoring_configuration",
    schema = "database_discovery",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["database_connection_id", "table_name"])
    ],
    indexes = [
        Index(name = "idx_table_monitoring_database_id", columnList = "database_connection_id"),
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

    @Column(name = "table_name", nullable = false)
    val tableName: String,

    @Column(name = "is_enabled", nullable = false)
    var isEnabled: Boolean = true,

    @Column(name = "include_all_columns", nullable = false)
    var includeAllColumns: Boolean = true,

    @Column(name = "table_columns", columnDefinition = "JSONB")
    @Convert(converter = TableColumnConfiguration::class)
    var columns: List<TableColumnConfiguration> = emptyList(),

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP", updatable = false)
    var createdAt: ZonedDateTime = ZonedDateTime.now(),

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    var updatedAt: ZonedDateTime = ZonedDateTime.now()
)


