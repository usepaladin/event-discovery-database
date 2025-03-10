package veridius.discover.utils

import veridius.discover.models.configuration.PrimaryKey
import veridius.discover.models.configuration.TableConfiguration
import veridius.discover.pojo.configuration.TableColumnConfiguration
import veridius.discover.pojo.configuration.TableMetadataConfiguration
import java.time.ZonedDateTime
import java.util.*

object TestColumnConfigurations {

    fun createMockPostgresTableConfig(
        databaseConnectionId: UUID,
        primaryKey: PrimaryKey,
        columns: List<TableColumnConfiguration> = listOf()
    ) = TableConfiguration(
        id = UUID.randomUUID(),
        databaseConnectionId = databaseConnectionId,
        tableName = "test_table",
        namespace = "test_db",
        isEnabled = true,
        includeAllColumns = true,
        columns = columns,
        metadata = createTableMetadata(primaryKey),
        createdAt = ZonedDateTime.now(),
        updatedAt = ZonedDateTime.now()
    )

    fun createMockMySQLTableConfig(
        databaseConnectionId: UUID,
        primaryKey: PrimaryKey,
        columns: List<TableColumnConfiguration> = listOf()
    ) = TableConfiguration(
        id = UUID.randomUUID(),
        databaseConnectionId = databaseConnectionId,
        tableName = "test_table",
        namespace = "test_db",
        isEnabled = true,
        includeAllColumns = true,
        columns = columns,
        metadata = createTableMetadata(primaryKey),
        createdAt = ZonedDateTime.now(),
        updatedAt = ZonedDateTime.now()
    )

    fun createMockColumn(name: String, type: String, nullable: Boolean = true): TableColumnConfiguration {
        return TableColumnConfiguration(
            name = name,
            type = type,
            nullable = nullable
        )
    }

    fun createTablePrimaryKey(): PrimaryKey {
        return PrimaryKey(
            columns = listOf("id"),
            name = "test_table_pkey"
        )
    }

    private fun createTableMetadata(primaryKey: PrimaryKey): TableMetadataConfiguration {
        return TableMetadataConfiguration(
            primaryKey = primaryKey
        )
    }
}