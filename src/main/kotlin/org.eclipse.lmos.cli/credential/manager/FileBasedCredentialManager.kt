package org.eclipse.lmos.cli.credential.manager

import jakarta.enterprise.context.ApplicationScoped
import kotlinx.serialization.builtins.ListSerializer
import net.mamoe.yamlkt.Yaml
import org.eclipse.lmos.cli.constants.LmosCliConstants.CredentialManagerConstants.CREDENTIAL_DIRECTORY
import org.eclipse.lmos.cli.credential.Credential
import org.eclipse.lmos.cli.credential.CredentialManagerType
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec


//fun main() {
//    val manager = FileBasedCredentialManager()
//
//    val testPrefix = "Random:"
//
//    val credential1 = Credential(id = "cred1", content = "secret1")
//    val credential2 = Credential(id = "cred2", content = "secret2")
//
//    manager.addCredential(testPrefix, credential1)
//    manager.addCredential(testPrefix, credential2)
//
//    // List credentials
//    val listed = manager.listCredentials(testPrefix)
//    println(2 == listed.size)
//    println(listed.any { it.id == credential1.id })
//    println(listed.any { it.id == credential2.id })
//
//    // Get credential and check decrypted content
//    val fetched1 = manager.getCredential(testPrefix, credential1.id)
//    println(fetched1 != null)
//    println(credential1.id == fetched1?.id)
//    println(credential1.content == fetched1?.content)
//
//    // Update credential
//    val updatedCredential = credential1.copy(content = "updatedSecret")
//    manager.updateCredential(testPrefix, updatedCredential)
//    val fetchedUpdated = manager.getCredential(testPrefix, credential1.id)
//    println(fetchedUpdated != null)
//    println("updatedSecret" == fetchedUpdated?.content)
//
//    // Delete one credential
//    manager.deleteCredential(testPrefix, credential2.id)
//    val afterDelete = manager.listCredentials(testPrefix)
//    println(1 == afterDelete.size)
//    println(manager.getCredential(testPrefix, credential2.id) == null)
//
//    // Delete all credentials
//    manager.deleteAllCredentials(testPrefix)
//    val finalList = manager.listCredentials(testPrefix)
//    println(finalList.isEmpty())
//
//}

@ApplicationScoped
class FileBasedCredentialManager : CredentialManager {

    companion object {
        private const val SECRET_KEY = "lmos-cli-secret"
    }

    private val encryption = SecureStringEncryption()
    private val yaml = Yaml()
    private val credentialSerializer = ListSerializer(Credential.serializer())

    override fun credentialManagerType() = CredentialManagerType.MAC

    override fun testCredentialManager() = true

    override fun addCredential(prefix: String, credential: Credential) {
        val credentials = listCredentials(prefix).toMutableSet()
        val encryptedContent = encryption.encrypt(credential.content, SECRET_KEY)
        val encryptedCredential = credential.copy(
            content = Base64.getEncoder().encodeToString(encryptedContent)
        )
        credentials.removeIf { it.id == credential.id } // Replace existing credential with same ID
        credentials.add(encryptedCredential)
        saveCredentials(prefix, credentials)
    }

    override fun listCredentials(prefix: String): Set<Credential> {
        val file = credentialFile(prefix)
        if (!file.exists()) return setOf()

        val content = file.readText()
        return yaml.decodeFromString(credentialSerializer, content).toSet()
    }

    override fun getCredential(prefix: String, id: String): Credential? {
        val credential = listCredentials(prefix).find { it.id == id } ?: return null
        val decryptedContent = encryption.readAndDecrypt(
            SECRET_KEY,
            Base64.getDecoder().decode(credential.content)
        )
        return credential.copy(content = decryptedContent)
    }

    override fun updateCredential(prefix: String, credential: Credential) {
        addCredential(prefix, credential)
    }

    override fun deleteCredential(prefix: String, id: String) {
        val credentials = listCredentials(prefix).toMutableSet()
        val removed = credentials.removeIf { it.id == id }
        if (removed) {
            if (credentials.isEmpty()) {
                // Delete file if no credentials remain
                credentialFile(prefix).delete()
            } else {
                saveCredentials(prefix, credentials)
            }
        }
    }

    override fun deleteAllCredentials(prefix: String) {
        credentialFile(prefix).delete()
    }

    private fun saveCredentials(prefix: String, credentials: Set<Credential>) {
        val configYaml = yaml.encodeToString(credentialSerializer, credentials.toList())
        credentialFile(prefix).writeText(configYaml)
    }

    private fun credentialFile(prefix: String) =
        CREDENTIAL_DIRECTORY.resolve(".secret-${prefix}.yaml").toFile()
}



/**
 * Securely encrypts a string using AES-GCM and stores it in a file
 * Uses PBKDF2 for key derivation with a random salt
 */
class SecureStringEncryption {
    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val KEY_ALGORITHM = "AES"
        private const val KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val ITERATIONS = 65536 // High iteration count for security
        private const val KEY_LENGTH = 256
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
        private const val SALT_LENGTH = 32
    }

    /**
     * Encrypts a string with a password and writes it to a file
     * @param plainText The string to encrypt
     * @param password The password used for encryption
     * @param outputFile The file to write the encrypted data to
     */
    fun encrypt(plainText: String, password: String): ByteArray {
        // Generate random salt for PBKDF2
        val salt = ByteArray(SALT_LENGTH).apply {
            SecureRandom().nextBytes(this)
        }

        // Generate a secure key using PBKDF2
        val secretKey = deriveKey(password, salt)

        // Generate random IV for GCM mode
        val iv = ByteArray(GCM_IV_LENGTH).apply {
            SecureRandom().nextBytes(this)
        }

        // Initialize cipher for encryption
        val cipher = Cipher.getInstance(ALGORITHM)
        val gcmParameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec)

        // Encrypt the data
        val encryptedBytes = cipher.doFinal(plainText.toByteArray())

        // Combine salt, IV, and encrypted data for storage
        val outputData = ByteArray(salt.size + iv.size + encryptedBytes.size).apply {
            System.arraycopy(salt, 0, this, 0, salt.size)
            System.arraycopy(iv, 0, this, salt.size, iv.size)
            System.arraycopy(encryptedBytes, 0, this, salt.size + iv.size, encryptedBytes.size)
        }
        return outputData;
    }

    /**
     * Decrypts a string from a file using the provided password
     * @param password The password used for decryption
     * @param inputFile The file containing the encrypted data
     * @return The decrypted string
     */
    fun readAndDecrypt(password: String, encryptedData: ByteArray): String {

        // Extract salt, IV, and encrypted bytes
        val salt = encryptedData.copyOfRange(0, SALT_LENGTH)
        val iv = encryptedData.copyOfRange(SALT_LENGTH, SALT_LENGTH + GCM_IV_LENGTH)
        val encrypted = encryptedData.copyOfRange(SALT_LENGTH + GCM_IV_LENGTH, encryptedData.size)

        // Derive the key using the same password and extracted salt
        val secretKey = deriveKey(password, salt)

        // Initialize cipher for decryption
        val cipher = Cipher.getInstance(ALGORITHM)
        val gcmParameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec)

        // Decrypt the data
        val decryptedBytes = cipher.doFinal(encrypted)

        return String(decryptedBytes)
    }

    /**
     * Derives an encryption key from a password and salt using PBKDF2
     */
    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, KEY_ALGORITHM)
    }
}