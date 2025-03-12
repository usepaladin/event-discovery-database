package paladin.discover.utils

import paladin.discover.models.common.DatabaseType
import paladin.discover.models.connection.DatabaseConnectionConfiguration
import paladin.discover.pojo.connection.ConnectionAdditionalProperties
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

    fun createMongoConfig() = DatabaseConnectionConfiguration(
        id = UUID.randomUUID(),
        databaseType = DatabaseType.MONGO,
        connectionName = "Test MongoDB",
        hostName = "localhost",
        port = "27017",
        database = "test_db",
        user = "test_user",
        password = "test_password",
        isEnabled = true,
        instanceId = UUID.randomUUID(),
        additionalProperties = ConnectionAdditionalProperties(
            authSource = "admin"
        )
    )

    fun createCassandraConfig() = DatabaseConnectionConfiguration(
        id = UUID.randomUUID(),
        databaseType = DatabaseType.CASSANDRA,
        connectionName = "Test Cassandra",
        hostName = "localhost",
        port = "9042",
        database = "test_keyspace",
        user = "test_user",
        password = "test_password",
        isEnabled = true,
        instanceId = UUID.randomUUID(),
        additionalProperties = ConnectionAdditionalProperties(
            dataCenter = "datacenter1"
        )
    )
} 