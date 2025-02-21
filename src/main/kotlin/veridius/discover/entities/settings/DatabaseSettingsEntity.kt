package veridius.discover.entities.settings

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(
    name = "database_settings",
    schema = "database_discovery",
    indexes = [
        Index(name = "idx_database_settings_database_id", columnList = "database_connection_id"),
    ]
)
class DatabaseSettingsEntity(
    @Id
    @GeneratedValue
    @Column(name = "database_settings_id", columnDefinition = "UUID DEFAULT uuid_generate_v4()", nullable = false)
    val id: UUID? = null,

    @Column(name = "database_connection_id", nullable = false)
    val databaseConnectionId: UUID,



    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP", updatable = false)
    var createdAt: java.time.ZonedDateTime = java.time.ZonedDateTime.now(),

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    var updatedAt: java.time.ZonedDateTime = java.time.ZonedDateTime.now()
) {


}