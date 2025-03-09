package veridius.discover.pojo.state


/**
 * Represents the state of a connection.
 *
 * @param T The specific type of connection state, constrained to be a subtype of ConnectionState<T>
 */
open class ConnectionState<T : ConnectionState<T>> {
    /** Represents a successfully established connection */
    data object Connected : ConnectionState<Nothing>()

    /** Represents a disconnected state */
    data object Disconnected : ConnectionState<Nothing>()

    /** Represents an in-progress connection attempt */
    data object Connecting : ConnectionState<Nothing>()

    /** Represents an error state with the associated exception */
    data class Error(val exception: Throwable) : ConnectionState<Nothing>()

}
