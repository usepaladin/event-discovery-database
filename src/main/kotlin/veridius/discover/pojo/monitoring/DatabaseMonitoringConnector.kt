package veridius.discover.pojo.monitoring

import io.debezium.storage.file.history.FileSchemaHistory
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
            put("offset.storage", FileOffsetBackingStore::class.java.name)
            put(
                "offset.storage.file.filename",
                "${storageConfig.offsetStorageDir}/${client.id}.${storageConfig.offsetStorageFileName}"
            )
            // Schema history
            put("schema.history.internal", FileSchemaHistory::class.java.name)
            put(
                "schema.history.internal.file.filename",
                "${storageConfig.historyDir}/${client.id}.${storageConfig.historyFileName}"
            )
            put("offset.flush.interval.ms", "10000")
            put("offset.flush.timeout.ms", "5000")
            put("offset.flush.size", "10000")
            put("offset.flush.count", "10000")
            put("database.history.kafka.bootstrap.servers", "192.168.0.241:9092")
            put("database.history.kafka.topic", "dbhistory.${client.config.connectionName}")
            put("database.history.kafka.recovery.poll.interval.ms", "500")
            put("include.schema.changes", "true")
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
        data class Error(val exception: Throwable) : MonitoringConnectionState()
    }
}

