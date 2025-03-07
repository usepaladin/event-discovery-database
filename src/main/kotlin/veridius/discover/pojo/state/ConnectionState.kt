package veridius.discover.pojo.state

open class ConnectionState {

    data object Connected : ConnectionState()
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data class Error(val exception: Throwable) : ConnectionState()

}

open class MonitoringConnectionState : ConnectionState() {
    data object Paused : MonitoringConnectionState()
}