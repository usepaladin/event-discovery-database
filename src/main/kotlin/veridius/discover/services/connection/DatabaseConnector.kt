package veridius.discover.services.connection

/**
 * Interface for database connectors
 */
sealed interface DatabaseConnector {
    /**
     * Connect to the database
     */
    suspend fun connect()

    /**
     * Disconnect from the database
     */
    suspend fun disconnect()

    /**
     * Get the connection status
     */
    fun isConnected(): Boolean

    fun validateConfig()
    fun configure()
}