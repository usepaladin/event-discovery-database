package veridius.discover.pojo.state

open class ConnectionState<T : ConnectionState<T>> {
    data object Connected : ConnectionState<Nothing>()
    data object Disconnected : ConnectionState<Nothing>()
    data object Connecting : ConnectionState<Nothing>()
    data class Error(val exception: Throwable) : ConnectionState<Nothing>()

}
