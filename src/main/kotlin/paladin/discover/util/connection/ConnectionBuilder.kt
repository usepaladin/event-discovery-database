package paladin.discover.util.connection

import paladin.discover.models.common.DatabaseType
import paladin.discover.models.connection.DatabaseConnectionConfiguration

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
        val authCredentials = when {
            connection.user != null && connection.password != null -> "${connection.user}:${connection.password}@"
            else -> ""
        }
        val baseUrl = "mongodb://$authCredentials${connection.hostName}:${connection.port}"
        val db = connection.database?.let { "/$it" } ?: ""

        // Properly format query parameters
        val queryParams = mutableListOf<String>()
        connection.additionalProperties?.authSource?.let { queryParams.add("authSource=$it") }

        val queryString = if (queryParams.isNotEmpty()) "?${queryParams.joinToString("&")}" else ""

        return "$baseUrl$db$queryString"
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