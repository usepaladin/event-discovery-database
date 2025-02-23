package veridius.discover.services.encryption

import org.springframework.stereotype.Service
import org.springframework.vault.VaultException
import org.springframework.vault.core.VaultTemplate
import org.springframework.vault.support.VaultResponse
import veridius.discover.configuration.properties.CoreConfigurationProperties


@Service
class VaultService(private val vaultTemplate: VaultTemplate, private val config: CoreConfigurationProperties) {

    fun getEncryptionKeyForTenant(): String? {
        val path: String = "secret/encryption/tenant/${config.tenantId}"
        return try {
            val secretResponse: VaultResponse = vaultTemplate.read(path)
            return secretResponse.data?.get("encryption-key") as? String
        } catch (e: VaultException) {
            println(
                "Error retrieving encryption key from Vault for tenant \n" +
                        "Tenant Id: ${config.tenantId} \n" +
                        "Stack trace: ${e.message}"
            )
            null // Or handle error as appropriate (e.g., throw exception, return null, log)
        }
    }
}