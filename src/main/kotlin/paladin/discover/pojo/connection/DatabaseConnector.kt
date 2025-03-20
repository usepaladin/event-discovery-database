package paladin.discover.pojo.connection

/**
 * Interface for database connectors
 */
interface DatabaseConnector {
    /**
     * Connect to the database
     */
    fun connect(): Any

    /**
     * Disconnect from the database, removing any active connections
     */
    fun disconnect()

    /**
     * Fetching the connection status of the database
     */
    fun isConnected(): Boolean

    /**
     * Ensure the current configuration associated with a Connector is valid and relevant to the type of database
     * that the connector is connecting to. This is to help avoid any potential connection failures and exceptions
     * purely due to incorrect configuration.
     */
    fun validateConfig()
    fun configure()
}