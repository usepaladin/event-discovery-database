package veridius.discover.services.connection.internal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import veridius.discover.entities.connection.DatabaseConnectionEntity

sealed class DatabaseConnection : DatabaseConnector {
    abstract val id: String
    abstract val config: DatabaseConnectionEntity

    protected val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
}

sealed class ConnectionState {
    data object Connected : ConnectionState()
    data object Disconnected : ConnectionState()
    data class Error(val exception: Throwable) : ConnectionState()
    data object Connecting : ConnectionState()
}