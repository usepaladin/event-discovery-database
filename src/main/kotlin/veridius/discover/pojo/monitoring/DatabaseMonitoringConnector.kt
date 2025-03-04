package veridius.discover.pojo.monitoring

import org.apache.kafka.connect.storage.FileOffsetBackingStore
import veridius.discover.models.configuration.TableConfiguration
import veridius.discover.models.connection.DatabaseConnectionConfiguration
import veridius.discover.pojo.client.DatabaseClient
import java.util.*

abstract class DatabaseMonitoringConnector(
    protected val client: DatabaseClient,
    protected val databaseConnectionConfiguration: DatabaseConnectionConfiguration,
    protected val tableConfigurations: Map<UUID, TableConfiguration>,
    protected val fileStorageDir: String
) {
    protected fun commonProps(): MutableMap<String, String> = mutableMapOf(
        "offset.storage" to FileOffsetBackingStore::class.java.name, // Storing offset information,
        "offset.storage.file.filename" to "$fileStorageDir/${client.id}.offsets", // File to store offset information
        "offset.flush.interval.ms" to "10000", // Flush interval for offset storage,
        "offset.flush.timeout.ms" to "5000", // Timeout for offset flush,
        "offset.flush.size" to "10000", // Size of offset flush,
        "offset.flush.count" to "10000", // Count of offset flush,
        "database.history.kafka.bootstrap.servers" to "192.168.0.241:9092",
        "database.history.kafka.topic" to "dbhistory.${client.config.connectionName}",
        "database.history.kafka.recovery.poll.interval.ms" to "500",
        "include.schema.changes" to "true",
    )

    abstract fun buildTableList(): List<String>
    abstract fun buildTableColumnList(): Map<String, List<String>>

    abstract fun getConnectorProps(): Map<String, String>
}