package veridius.discover.services.configuration

import org.springframework.stereotype.Service
import veridius.discover.configuration.properties.CoreConfigurationProperties
import veridius.discover.entities.connection.DatabaseConnectionEntity
import veridius.discover.exceptions.DatabaseConnectionNotFound
import veridius.discover.models.connection.DatabaseConnectionConfiguration
import veridius.discover.pojo.connection.ConnectionAdditionalProperties
import veridius.discover.repositories.connection.DatabaseConnectionConfigurationRepository
import veridius.discover.services.encryption.EncryptionService
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
        val databaseConfiguration =
            DatabaseConnectionConfiguration.fromEntity(configuration, serverConfig.requireDataEncryption)

        if (!serverConfig.requireDataEncryption) return databaseConfiguration

        // Decrypt Encrypted Fields
        return databaseConfiguration.apply {
            hostName =
                encryptionService.decrypt(configuration.hostName) ?: throw Exception("Failed to decrypt hostname")
            port = encryptionService.decrypt(configuration.port) ?: throw Exception("Failed to decrypt port")

            configuration.databaseName.let { encDatabase ->
                database = encryptionService.decrypt(encDatabase) ?: throw Exception("Failed to decrypt database")
            }

            configuration.user?.let { encUser ->
                user = encryptionService.decrypt(encUser) ?: throw Exception("Failed to decrypt user")
            }

            configuration.password?.let { encPassword ->
                password = encryptionService.decrypt(encPassword) ?: throw Exception("Failed to decrypt password")
            }

            configuration.additionalPropertiesText?.let { encProperties ->
                additionalProperties =
                    encryptionService.decryptObject(encProperties, ConnectionAdditionalProperties::class.java)
                        ?: throw Exception("Failed to decrypt additional properties")
            }
        }
    }

}