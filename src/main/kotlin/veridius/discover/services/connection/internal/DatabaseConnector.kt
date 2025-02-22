package veridius.discover.services.connection.internal

/**
 * Interface for database connectors
 */
sealed interface DatabaseConnector {
    /**
     * Connect to the database
     */
    fun connect(): Any

    /**
     * Disconnect from the database
     */
    fun disconnect()

    /**
     * Get the connection status
     */
    fun isConnected(): Boolean

    fun validateConfig()
    fun configure()
}