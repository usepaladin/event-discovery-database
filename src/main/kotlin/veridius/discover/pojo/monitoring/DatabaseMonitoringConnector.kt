package veridius.discover.pojo.monitoring

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.apache.kafka.connect.storage.FileOffsetBackingStore
import veridius.discover.configuration.properties.DebeziumConfigurationProperties
import veridius.discover.models.configuration.TableConfiguration
import veridius.discover.pojo.client.DatabaseClient
import veridius.discover.pojo.state.ConnectionState
import java.util.*

abstract class DatabaseMonitoringConnector(private val storageConfig: DebeziumConfigurationProperties) {
    protected abstract val client: DatabaseClient
    protected abstract val tableConfigurations: List<TableConfiguration>

    protected val _connectionState = MutableStateFlow<MonitoringConnectionState>(MonitoringConnectionState.Disconnected)
    val connectionState: StateFlow<MonitoringConnectionState> = _connectionState

    fun updateConnectionState(newState: MonitoringConnectionState) {
        _connectionState.value = newState
    }

    protected fun commonProps(): Properties {
        return Properties().apply {
            "offset.storage" to FileOffsetBackingStore::class.java.name // Storing offset information,
            "offset.storage.file.filename" to "${storageConfig.offsetStorageDir}/${client.id}.${storageConfig.offsetStorageFileName}" // File to store offset information
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

    sealed class MonitoringConnectionState : ConnectionState<MonitoringConnectionState>() {
        data object Disconnected : MonitoringConnectionState()
        data object Connecting : MonitoringConnectionState()
        data object Connected : MonitoringConnectionState()
        data object Paused : MonitoringConnectionState()
        data class Error(val exception: Exception) : MonitoringConnectionState()
    }
}

