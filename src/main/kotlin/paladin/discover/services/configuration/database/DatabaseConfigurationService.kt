package paladin.discover.services.configuration.database

import org.springframework.stereotype.Service
import paladin.discover.configuration.properties.CoreConfigurationProperties
import paladin.discover.entities.connection.DatabaseConnectionEntity
import paladin.discover.exceptions.DatabaseConnectionNotFound
import paladin.discover.models.connection.DatabaseConnectionConfiguration
import paladin.discover.repositories.connection.DatabaseConnectionConfigurationRepository
import paladin.discover.services.encryption.EncryptionService
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service
class DatabaseConfigurationService(
    private val serverConfig: CoreConfigurationProperties,
    private val databaseConnectionConfigurationRepository: DatabaseConnectionConfigurationRepository,
    private val encryptionService: EncryptionService
) {

    val databaseConfigurationConnections: ConcurrentHashMap<UUID, DatabaseConnectionConfiguration> = ConcurrentHashMap()

    fun getDatabaseConnectionConfiguration(id: UUID): DatabaseConnectionConfiguration {
        return databaseConfigurationConnections[id] ?: fetchDatabaseConnectionConfiguration(id)!!
    }

    fun fetchDatabaseConnectionConfiguration(id: UUID): DatabaseConnectionConfiguration? {
        val databaseConnection: DatabaseConnectionEntity =
            databaseConnectionConfigurationRepository.findById(id)
                .orElseThrow {
                    DatabaseConnectionNotFound(
                        "Database connection not found \n" +
                                "Database ID: $id"
                    )
                }

        val connectionConfig: DatabaseConnectionConfiguration = decryptDatabaseConfiguration(databaseConnection)
        databaseConfigurationConnections[id] = connectionConfig
        return connectionConfig
    }

    fun fetchAllDatabaseConnectionConfigurations(): List<DatabaseConnectionConfiguration> {
        val databaseConnections: List<DatabaseConnectionEntity> =
            databaseConnectionConfigurationRepository.findAllByInstanceId(serverConfig.serverInstanceId)

        return handleDatabaseConnectionDecryption(databaseConnections)
    }

    fun fetchAllEnabledDatabaseConnectionConfigurations(): List<DatabaseConnectionConfiguration> {
        val databaseConnections: List<DatabaseConnectionEntity> =
            databaseConnectionConfigurationRepository.findAllByInstanceIdAndEnabled(
                serverConfig.serverInstanceId,
                true
            )

        return handleDatabaseConnectionDecryption(databaseConnections)
    }

    private fun handleDatabaseConnectionDecryption(connections: List<DatabaseConnectionEntity>): List<DatabaseConnectionConfiguration> {
        if (connections.isEmpty()) {
            throw Exception("No database connections found")
        }

        return connections.map { configuration ->
            val connectionConfig: DatabaseConnectionConfiguration = decryptDatabaseConfiguration(configuration)
            databaseConfigurationConnections[configuration.id!!] = connectionConfig
            connectionConfig
        }
    }

    /**
     * Update the database connection configuration with new configuration details
     * Test connection with new details to ensure validity before saving to the database
     *
     * @param {DatabaseConnectionConfiguration} configuration -> Updated configuration details
     *
     * @return {DatabaseConnectionConfiguration} -> Updated configuration details saved to the Database
     */
    fun updateDatabaseConnectionConfiguration(configuration: DatabaseConnectionConfiguration): DatabaseConnectionConfiguration {
        val databaseEntity: DatabaseConnectionEntity =
            databaseConnectionConfigurationRepository.findById(configuration.id)
                .orElseThrow {
                    DatabaseConnectionNotFound(
                        "Database connection not found \n" +
                                "Database ID: ${configuration.id}"
                    )
                }
        TODO()
    }

    private fun decryptDatabaseConfiguration(configuration: DatabaseConnectionEntity): DatabaseConnectionConfiguration {
        if (!serverConfig.requireDataEncryption) {
            return DatabaseConnectionConfiguration.fromEntity(configuration)
        }

        val hostName = encryptionService.decrypt(configuration.hostName)
            ?: throw Exception("Failed to decrypt hostname")
        val port = encryptionService.decrypt(configuration.port)
            ?: throw Exception("Failed to decrypt port")
        val database = encryptionService.decrypt(configuration.databaseName)
            ?: throw Exception("Failed to decrypt database")
        val user = encryptionService.decrypt(configuration.user)
            ?: throw Exception("Failed to decrypt user")
        val password = configuration.password?.let { encryptionService.decrypt(it) }

        return DatabaseConnectionConfiguration.fromEntity(configuration, hostName, port, database, user, password)
    }

}