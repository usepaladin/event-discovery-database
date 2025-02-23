package veridius.discover.models.client

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import veridius.discover.entities.connection.DatabaseConnectionConfiguration
import veridius.discover.models.configuration.TableConfigurationBuilder
import veridius.discover.models.connection.DatabaseConnector
import java.util.*

sealed class DatabaseClient : DatabaseConnector, TableConfigurationBuilder {
    abstract val id: UUID
    abstract val config: DatabaseConnectionConfiguration

    protected val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
}

sealed class ConnectionState {
    data object Connected : ConnectionState()
    data object Disconnected : ConnectionState()
    data class Error(val exception: Throwable) : ConnectionState()
    data object Connecting : ConnectionState()
}