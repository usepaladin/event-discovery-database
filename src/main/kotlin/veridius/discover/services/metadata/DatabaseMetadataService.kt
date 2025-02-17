package veridius.discover.services.metadata

import com.datastax.oss.driver.api.core.CqlSession
import com.mongodb.client.MongoClients
import java.net.InetSocketAddress
import java.sql.DriverManager

class DatabaseMetadataService {
    fun getDatabaseMetadata(dbType: String, connectionUrl: String, user: String, pass: String): Map<String, List<String>> {
        return when (dbType.lowercase()) {
            "mysql", "postgresql" -> getRelationalDbMetadata(connectionUrl, user, pass)
            "mongodb" -> getMongoDbMetadata(connectionUrl)
            "cassandra" -> getCassandraMetadata(connectionUrl)
            else -> throw IllegalArgumentException("Unsupported database type: $dbType")
        }
    }
    fun getRelationalDbMetadata(connectionUrl: String, user: String, pass: String): Map<String, List<String>> {
        val metadata = mutableMapOf<String, List<String>>()
        try {
            DriverManager.getConnection(connectionUrl, user, pass).use { connection ->
                val meta = connection.metaData
                val tables = meta.getTables(null, null, "%", arrayOf("TABLE"))
                while (tables.next()) {
                    val tableName = tables.getString("TABLE_NAME")
                    val columns = mutableListOf<String>()
                    meta.getColumns(null, null, tableName, null).use { cols ->
                        while (cols.next()) {
                            columns.add(cols.getString("COLUMN_NAME"))
                        }
                    }
                    metadata[tableName] = columns
                }
            }
        } catch (e: Exception) {
            println("Error getting relational metadata: ${e.message}")
            e.printStackTrace()
        }
        return metadata
    }

    fun getMongoDbMetadata(connectionUrl: String): Map<String, List<String>> {
        val metadata = mutableMapOf<String, List<String>>()
        MongoClients.create(connectionUrl).use { mongoClient ->
            val database = mongoClient.getDatabase(connectionUrl.substringAfterLast("/")) // Extract DB name
            database.listCollectionNames().forEach { collectionName ->
                // MongoDB doesn't have a strict schema like SQL.  We'll get a sample document.
                val sampleDocument = database.getCollection(collectionName).find().first()
                val fields = sampleDocument?.keys?.toList() ?: emptyList()
                metadata[collectionName] = fields
            }
        }
        return metadata
    }

    fun getCassandraMetadata(connectionUrl: String): Map<String, List<String>> {
        val metadata = mutableMapOf<String, List<String>>()
        val address = connectionUrl.substringAfter("://").substringBefore("?").substringBefore("/")
        val port = if (":" in address) address.substringAfter(":").toInt() else 9042
        val host = address.substringBefore(":")
        val keyspaceName = connectionUrl.substringAfterLast("/").substringBefore("?")

        CqlSession.builder()
            .addContactPoint(InetSocketAddress(host, port))
            .withLocalDatacenter("datacenter1") //  Adjust as necessary, often required
            .withKeyspace(keyspaceName)
            .build().use { session ->
                val meta = session.metadata
                val keyspace = meta.getKeyspace(keyspaceName).orElseThrow {
                    IllegalArgumentException("Keyspace not found: $keyspaceName")
                }

                keyspace.tables.forEach { (tableName, tableMetadata) ->
                    val columns = tableMetadata.columns.values.map { it.name.toString()}
                    metadata[tableName.toString()] = columns
                }
            }
        return metadata
    }

}