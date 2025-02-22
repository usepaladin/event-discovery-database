package veridius.discover.entities.connection

import veridius.discover.entities.common.DatabaseType

data class ConnectionBuilder(val connection: DatabaseConnectionConfiguration) {

    fun buildConnectionURL(): String {
        return when (connection.databaseType) {
            DatabaseType.POSTGRES -> buildPostgresURL()
            DatabaseType.MONGO -> buildMongoUrl()
            DatabaseType.CASSANDRA -> buildCassandraUrl()
            DatabaseType.MYSQL -> buildMySQLUrl()
        }
    }

    private fun buildPostgresURL(): String {
        val baseUrl = "jdbc:postgresql://${connection.hostName}:${connection.port}"
        return when {
            connection.database != null -> "$baseUrl/${connection.database}"
            else -> baseUrl
        }
    }

    private fun buildMongoUrl(): String {
        val baseUrl = "mongodb://${connection.hostName}:${connection.port}"
        val auth = when {
            connection.user != null && connection.password != null ->
                "$connection.password:$connection.password@"

            else -> ""
        }
        val db = connection.database?.let { "/$it" } ?: ""
        return "$baseUrl$auth${connection.hostName}:${connection.port}$db"
    }

    private fun buildCassandraUrl(): String {
        val baseUrl = "jdbc:cassandra://${connection.hostName}:${connection.port}"
        return when {
            connection.database != null -> "$baseUrl/${connection.database}"
            else -> baseUrl
        }
    }

    private fun buildMySQLUrl(): String {
        val baseUrl = "jdbc:mysql://${connection.hostName}:${connection.port}"
        return when {
            connection.database != null -> "$baseUrl/${connection.database}"
            else -> baseUrl
        }
    }
}