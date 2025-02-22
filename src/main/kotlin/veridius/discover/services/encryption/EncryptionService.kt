package veridius.discover.services.encryption

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import javax.crypto.Cipher
import javax.crypto.SecretKey

@Service
class EncryptionService {

    private val encryptionKey: SecretKey = TODO()

    private val objectMapper = ObjectMapper()
    private val cipherAlgorithm = "AES/ECB/NoPadding"

    fun encrypt(data: Any): String {
        val dataString = objectMapper.writeValueAsString(data)
        val cipher = Cipher.getInstance(cipherAlgorithm)
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey)
        val encryptedData = cipher.doFinal(dataString.toByteArray())
        return encryptedData.toString(Charsets.UTF_8)
    }

    fun <T> decrypt(data: String, parsedClass: Class<T>): T {
        val cipher = Cipher.getInstance(cipherAlgorithm)
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey)
        val decryptedData = cipher.doFinal(data.toByteArray())
        return objectMapper.readValue(decryptedData.toString(Charsets.UTF_8), parsedClass)
    }

}