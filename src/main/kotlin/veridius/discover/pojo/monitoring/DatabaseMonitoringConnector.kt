package veridius.discover.pojo.monitoring

import org.apache.kafka.connect.storage.FileOffsetBackingStore
import veridius.discover.models.configuration.TableConfiguration
import veridius.discover.pojo.client.DatabaseClient
import java.util.*

abstract class DatabaseMonitoringConnector(private val fileStorageDir: String) {
    protected abstract val client: DatabaseClient
    protected abstract val tableConfigurations: List<TableConfiguration>

    protected fun commonProps(): Properties {
        return Properties().apply {
            "offset.storage" to FileOffsetBackingStore::class.java.name // Storing offset information,
            "offset.storage.file.filename" to "$fileStorageDir/${client.id}.offsets" // File to store offset information
            "offset.flush.interval.ms" to "10000" // Flush interval for offset storage,
            "offset.flush.timeout.ms" to "5000" // Timeout for offset flush,
            "offset.flush.size" to "10000" // Size of offset flush,
            "offset.flush.count" to "10000" // Count of offset flush,
            "database.history.kafka.bootstrap.servers" to "192.168.0.241:9092"
            "database.history.kafka.topic" to "dbhistory.${client.config.connectionName}"
            "database.history.kafka.recovery.poll.interval.ms" to "500"
            "include.schema.changes" to "true"
        }
    }


    /**
     * Configure the list of tables to be monitored by the connector
     */
    abstract fun buildTableList(): String

    /**
     * Configure a list of specific columns that will be monitored for each table
     * If the table is not included in the table list, the columns will not be monitored
     * If the column list is empty, all columns will be monitored
     */
    abstract fun buildTableColumnList(): String
    abstract fun getConnectorProps(): Properties
}