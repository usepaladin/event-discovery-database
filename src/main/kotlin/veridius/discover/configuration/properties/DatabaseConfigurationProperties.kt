package veridius.discover.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty

@ConfigurationProperties(prefix = "discover")
data class DatabaseConfigurationProperties(
    @NestedConfigurationProperty val databases: List<DatabaseConnectionConfiguration>
)
{
    data class DatabaseConnectionConfiguration(
        val id: String,
        val type: DatabaseType,
        val public: Boolean,
        val host: String,
        val port: String,
        val user: String?,
        val password: String?,
        val database: String?,
        val dataCenter: String?,
    ){
        fun toConnectionURL(): String {
            return when(type){
                DatabaseType.POSTGRES -> buildPostgresURL()
                DatabaseType.CASSANDRA -> buildCassandraUrl()
                DatabaseType.MYSQL -> buildMySQLUrl()
                DatabaseType.MONGODB -> buildMongoUrl()
            }
        }

        private fun buildPostgresURL(): String {
            val baseUrl = "jdbc:postgresql://$host:$port"
            return when {
                database != null -> "$baseUrl/$database"
                else -> baseUrl
            }
        }
        private fun buildMongoUrl(): String {
            val baseUrl = "mongodb://$host:$port"
            val auth = when {
                user != null && password != null ->
                    "$user:$password@"
                else -> ""
            }
            val db = database?.let { "/$it" } ?: ""
            return "$baseUrl$auth$host:$port$db"
        }

        private fun buildCassandraUrl(): String {
            val baseUrl = "jdbc:cassandra://$host:$port"
            return when {
                database != null -> "$baseUrl/$database"
                else -> baseUrl
            }
        }

        private fun buildMySQLUrl(): String {
            val baseUrl = "jdbc:mysql://$host:$port"
            return when {
                database != null -> "$baseUrl/$database"
                else -> baseUrl
            }
        }

    }

    enum class DatabaseType {
        POSTGRES,
        CASSANDRA,
        MYSQL,
        MONGODB
    }

}