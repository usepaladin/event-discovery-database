package veridius.discover.pojo.client

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import veridius.discover.models.connection.DatabaseConnectionConfiguration
import veridius.discover.pojo.connection.DatabaseConnector
import veridius.discover.util.configuration.TableConfigurationBuilder
import java.util.*

/**
 * Todo: Ensure Configured Database Driver is supported by debezium
 */
abstract class DatabaseClient : DatabaseConnector, TableConfigurationBuilder {
    abstract val id: UUID
    abstract val config: DatabaseConnectionConfiguration

    protected val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    fun updateConnectionState(newState: ConnectionState) {
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
        if (config.additionalProperties?.public == false && (config.user.isNullOrEmpty() || config.password.isNullOrEmpty())) {
            throw IllegalArgumentException("Private connections must have associated authentication credentials")
        }
    }

    /**
     * Extendable configuration validation for specific database clients
     */
    protected open fun clientConfigValidation() {
        // No-op
    }
}

sealed class ConnectionState {
    data object Connected : ConnectionState()
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data class Error(val exception: Throwable) : ConnectionState()
}