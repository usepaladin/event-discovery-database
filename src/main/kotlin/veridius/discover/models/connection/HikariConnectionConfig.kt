package veridius.discover.models.connection

import com.zaxxer.hikari.HikariConfig
import veridius.discover.entities.connection.ConnectionBuilder
import veridius.discover.entities.connection.DatabaseConnectionConfiguration

interface HikariConnectionConfigBuilder {

    val hikariConfig: HikariConfig

    fun generateHikariConfig(config: DatabaseConnectionConfiguration, type: HikariDatabaseType): HikariConfig {
        val connectionBuilder = ConnectionBuilder(config)
        val connectionURL = connectionBuilder.buildConnectionURL()
        return HikariConfig().apply {
            driverClassName = getDatabaseDriver(type)
            jdbcUrl = connectionURL
            username = config.user
            password = config.password
            minimumIdle = 2
            maximumPoolSize = 10
            connectionTimeout = 3000
        }
    }

    enum class HikariDatabaseType {
        POSTGRES,
        MYSQL
    }

    private fun getDatabaseDriver(databaseType: HikariDatabaseType): String {
        return when (databaseType) {
            HikariDatabaseType.POSTGRES -> "org.postgresql.Driver"
            HikariDatabaseType.MYSQL -> "com.mysql.cj.jdbc.Driver"
        }
    }
}