package veridius.discover.services.configuration

import org.springframework.stereotype.Service
import veridius.discover.configuration.properties.CoreConfigurationProperties
import veridius.discover.entities.connection.ConnectionAdditionalProperties
import veridius.discover.entities.connection.DatabaseConnectionConfiguration
import veridius.discover.entities.connection.DatabaseConnectionEntity
import veridius.discover.exceptions.DatabaseConnectionNotFound
import veridius.discover.repositories.configuration.TableConfigurationRepository
import veridius.discover.repositories.connection.DatabaseConnectionConfigurationRepository
import veridius.discover.services.encryption.EncryptionService
import java.util.*

@Service
class ConfigurationService(
    private val serverConfig: CoreConfigurationProperties,
    private val databaseConnectionConfigurationRepository: DatabaseConnectionConfigurationRepository,
    private val tableMonitoringRepository: TableConfigurationRepository,
    private val encryptionService: EncryptionService
) {
    fun getDatabaseConnectionConfiguration(id: UUID): DatabaseConnectionConfiguration? {
        val databaseConnection: DatabaseConnectionEntity =
            databaseConnectionConfigurationRepository.findById(id)
                .orElseThrow {
                    DatabaseConnectionNotFound(
                        "Database connection not found \n" +
                                "Database ID: $id"
                    )
                }

        return decryptDatabaseConfiguration(databaseConnection)
    }

    fun getAllDatabaseConnectionConfigurations(): List<DatabaseConnectionConfiguration> {
        val databaseConnections: List<DatabaseConnectionEntity> =
            databaseConnectionConfigurationRepository.findAllByInstanceId(serverConfig.serverInstanceId)

        if (databaseConnections.isEmpty()) {
            throw Exception("No database connections found")
        }

        return databaseConnections.map { configuration ->
            decryptDatabaseConfiguration(configuration)
        }
    }

    private fun decryptDatabaseConfiguration(configuration: DatabaseConnectionEntity): DatabaseConnectionConfiguration {
        val databaseConfiguration =
            DatabaseConnectionConfiguration.fromEntity(configuration, serverConfig.requireDataEncryption)

        if (!serverConfig.requireDataEncryption) return databaseConfiguration

        // Decrypt Encrypted Fields
        return databaseConfiguration.apply {
            hostName = encryptionService.decrypt(configuration.hostName, String::class.java)
            port = encryptionService.decrypt(configuration.port, String::class.java)

            configuration.databaseName?.let { encDatabase ->
                database = encryptionService.decrypt(encDatabase, String::class.java)
            }

            configuration.user?.let { encUser ->
                user = encryptionService.decrypt(encUser, String::class.java)
            }

            configuration.password?.let { encPassword ->
                password = encryptionService.decrypt(encPassword, String::class.java)
            }

            configuration.additionalPropertiesText?.let { encProperties ->
                additionalProperties =
                    encryptionService.decrypt(encProperties, ConnectionAdditionalProperties::class.java)
            }
        }
    }

}