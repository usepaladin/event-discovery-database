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
    @Column(name = "table_monitoring_configuration_id", columnDefinition = "UUID DEFAULT uuid_generate_v4()", nullable = false)
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
    @Convert(converter = veridius.discover.entities.configuration.TableMonitoringConfigurationEntity.TableColumnConfigurationConvertor::class)
    var columns: List<TableColumnConfigurationEntity> = emptyList(),

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP", updatable = false)
    var createdAt: ZonedDateTime = ZonedDateTime.now(),

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    var updatedAt: ZonedDateTime = ZonedDateTime.now()
){

    data class TableColumnConfigurationEntity(
        var name: String? = null,
        var isEnabled: Boolean = true,
        // Include both the old and updated value in the Debezium event message
        var includeOldValue: Boolean? = null
        //todo: Research Column specific transformation within Debezium
    )

    @Converter
    class TableColumnConfigurationConvertor : AttributeConverter<TableColumnConfigurationEntity, String> {
        private val objectMapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()

        override fun convertToDatabaseColumn(attribute: TableColumnConfigurationEntity?): String {
            return objectMapper.writeValueAsString(attribute ?: TableColumnConfigurationEntity())
        }

        override fun convertToEntityAttribute(dbData: String?): TableColumnConfigurationEntity {
            return dbData?.let { objectMapper.readValue(it, TableColumnConfigurationEntity::class.java) } ?: TableColumnConfigurationEntity()
        }
    }

}


