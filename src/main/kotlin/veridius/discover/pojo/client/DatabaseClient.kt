package veridius.discover.pojo.client

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import veridius.discover.models.connection.DatabaseConnectionConfiguration
import veridius.discover.pojo.connection.DatabaseConnector
import veridius.discover.pojo.state.ConnectionState
import veridius.discover.util.configuration.TableConfigurationBuilder
import java.util.*

/**
 * Todo: Ensure Configured Database Driver is supported by debezium
 */
abstract class DatabaseClient : DatabaseConnector, TableConfigurationBuilder {
    abstract val id: UUID
    abstract val config: DatabaseConnectionConfiguration

    protected val _connectionState = MutableStateFlow<ClientConnectionState>(ClientConnectionState.Disconnected)
    val connectionState: StateFlow<ClientConnectionState> = _connectionState.asStateFlow()

    fun updateConnectionState(newState: ClientConnectionState) {
        _connectionState.value = newState
    }


    final override fun validateConfig() {
        baseConfigValidation()
        clientConfigValidation()
    }

    /**
     * Base configuration validation applicable to all database clients
     */
    protected open fun baseConfigValidation() {
        require(config.hostName.isNotBlank()) { "Hostname cannot be blank" }
        require(config.port.toIntOrNull()?.let { it in 1..65535 } == true) {
            "Port must be a valid integer in the range 1..65535"
        }
        require(config.user.isNotBlank()) { "Username cannot be blank" }
        require(config.database.isNotBlank()) { "Database name cannot be blank" }
    }

    /**
     * Extendable configuration validation for specific database clients
     */
    abstract fun clientConfigValidation()

    sealed class ClientConnectionState : ConnectionState<ClientConnectionState>() {
        data object Disconnected : ClientConnectionState()
        data object Disconnecting : ClientConnectionState()
        data object Reconnecting : ClientConnectionState()
        data object Connecting : ClientConnectionState()
        data object Connected : ClientConnectionState()
        data class Error(val exception: Throwable) : ClientConnectionState()
    }
}
