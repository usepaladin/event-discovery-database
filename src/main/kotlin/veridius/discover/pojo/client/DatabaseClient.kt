package veridius.discover.pojo.client

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import veridius.discover.models.connection.DatabaseConnectionConfiguration
import veridius.discover.pojo.connection.DatabaseConnector
import veridius.discover.util.configuration.TableConfigurationBuilder
import java.util.*

abstract class DatabaseClient : DatabaseConnector, TableConfigurationBuilder {
    abstract val id: UUID
    abstract val config: DatabaseConnectionConfiguration

    protected val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
}

sealed class ConnectionState {
    data object Connected : ConnectionState()
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data object Paused : ConnectionState()
    data class Error(val exception: Throwable) : ConnectionState()
}