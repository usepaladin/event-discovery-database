package paladin.discover.utils

import paladin.discover.enums.configuration.DatabaseType
import paladin.discover.models.connection.DatabaseConnectionConfiguration
import java.util.*

object TestDatabaseConfigurations {
    fun createPostgresConfig() = DatabaseConnectionConfiguration(
        id = UUID.randomUUID(),
        databaseType = DatabaseType.POSTGRES,
        connectionName = "Test Postgres",
        hostName = "localhost",
        port = "5432",
        database = "test_db",
        user = "test_user",
        password = "test_password",
        isEnabled = true,
        instanceId = UUID.randomUUID(),
        additionalProperties = null

    )

    fun createMySQLConfig() = DatabaseConnectionConfiguration(
        id = UUID.randomUUID(),
        databaseType = DatabaseType.MYSQL,
        connectionName = "Test MySQL",
        hostName = "localhost",
        port = "3306",
        database = "test_db",
        user = "test_user",
        password = "test_password",
        isEnabled = true,
        instanceId = UUID.randomUUID(),
        additionalProperties = null
    )
} 