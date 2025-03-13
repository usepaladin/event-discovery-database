package paladin.discover.pojo.monitoring

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import paladin.discover.configuration.properties.DebeziumConfigurationProperties
import paladin.discover.models.configuration.TableConfiguration
import paladin.discover.pojo.client.DatabaseClient
import java.util.*

abstract class DatabaseMonitoringConnector(
    private val storageConfig: DebeziumConfigurationProperties
) {
    protected abstract val client: DatabaseClient
    protected abstract val tableConfigurations: List<TableConfiguration>

    protected val _connectionState = MutableStateFlow<MonitoringConnectionState>(MonitoringConnectionState.Disconnected)
    val connectionState: StateFlow<MonitoringConnectionState> = _connectionState

    fun updateConnectionState(newState: MonitoringConnectionState) {
        _connectionState.value = newState
    }

    fun validateStorageBackend() {
        storageConfig.storageBackend.validateConfig(storageConfig)
    }


    protected fun commonProps(): Properties {
        val props: Properties = Properties().apply {
            put("offset.flush.interval.ms", "10000")
            put("offset.flush.timeout.ms", "5000")
            put("offset.flush.size", "10000")
            put("offset.flush.count", "10000")
            put("database.history.kafka.recovery.poll.interval.ms", "500")
            put("include.schema.changes", "true")
        }

        // Apply storage backend specific properties (ie. Either File or Kafka based)
        storageConfig.storageBackend.applyProperties(props, storageConfig)
        return props
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


    sealed class MonitoringConnectionState {
        data object Disconnected : MonitoringConnectionState()
        data object Disconnecting : MonitoringConnectionState()
        data object Reconnecting : MonitoringConnectionState()
        data object Connecting : MonitoringConnectionState()
        data object Connected : MonitoringConnectionState()
        data object Paused : MonitoringConnectionState()

        // todo: Look into making error actionable (ie. Further details of what the error was caused by
        // so we can implement user resolution strategies in the web client (ie. AuthenticationError, ConfigurationError, SchemaError, etc.)
        /**
         * etc...
         *   data class AuthenticationError(val reason: String) : MonitoringConnectionState() // Actionable: Re-prompt for credentials
         *   data class NetworkTimeoutError(val retryAttempts: Int) : MonitoringConnectionState() // Actionable: Retry with backoff
         *   data class ConfigurationError(val configIssue: String) : MonitoringConnectionState() //Actionable: prevent connector running.
         * */
        data class Error(val exception: Throwable) : MonitoringConnectionState()
    }
}

