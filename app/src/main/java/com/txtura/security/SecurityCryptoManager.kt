@file:Suppress("SpellCheckingInspection")

package com.txtura.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONObject

class DeviceKeyMismatchException(message: String) : Exception(message)
class InvalidBackupPasswordException(message: String) : Exception(message)

/**
 * Enterprise-grade cryptographic engine for Txtura:
 * 1. Device-bound hardware KeyStore encryption (AES-256-GCM) for the local conversation disk cache.
 * 2. Authenticated backup encryption (AES-256-GCM) with PBKDF2-HMAC-SHA256 key derivation
 *    or device hardware key, preventing any unauthorized extraction via USB, PC, or file managers.
 */
object SecurityCryptoManager {

    private const val TAG = "SecurityCryptoManager"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val GCM_IV_LENGTH_BYTES = 12

    // Magic 4-byte header: "TENC" (Txtura Encrypted) + 1-byte version (0x01)
    private val CACHE_MAGIC_HEADER = byteArrayOf('T'.code.toByte(), 'E'.code.toByte(), 'N'.code.toByte(), 'C'.code.toByte())
    private const val CACHE_FORMAT_VERSION: Byte = 1

    private const val KEY_ALIAS_CACHE = "txtura_conversations_cache_v1"
    private const val KEY_ALIAS_BACKUP = "txtura_backup_device_key_v1"

    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 100_000
    private const val SALT_LENGTH_BYTES = 16
    private const val KEY_LENGTH_BITS = 256

    const val BACKUP_CONTAINER_FORMAT = "TXTURA_ENCRYPTED_BACKUP"
    const val BACKUP_TYPE_DEVICE = "DEVICE_KEYSTORE"
    const val BACKUP_TYPE_PASSWORD = "PASSWORD"

    private val secureRandom = SecureRandom()

    // =========================================================================
    // 1. Android KeyStore (Hardware-Backed) Master Key Management
    // =========================================================================

    @Synchronized
    private fun getOrCreateKeyStoreSecretKey(alias: String): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(alias)) {
            val entry = keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry
            if (entry?.secretKey != null) {
                return entry.secretKey
            }
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    // =========================================================================
    // 2. Local Conversation Disk Cache Encryption (Hardware TEE / StrongBox)
    // =========================================================================

    /**
     * Encrypts plaintext bytes using hardware-backed AES-256-GCM.
     * Output format: [4 bytes MAGIC ("TENC")] [1 byte VERSION] [12 bytes IV] [Ciphertext + 16 bytes GCM Tag]
     */
    fun encryptCache(plaintextBytes: ByteArray): ByteArray {
        val key = getOrCreateKeyStoreSecretKey(KEY_ALIAS_CACHE)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv = cipher.iv ?: throw IllegalStateException("Cipher failed to generate IV")
        val ciphertext = cipher.doFinal(plaintextBytes)

        val buffer = ByteBuffer.allocate(CACHE_MAGIC_HEADER.size + 1 + iv.size + ciphertext.size)
        buffer.put(CACHE_MAGIC_HEADER)
        buffer.put(CACHE_FORMAT_VERSION)
        buffer.put(iv)
        buffer.put(ciphertext)
        return buffer.array()
    }

    /**
     * Authenticates and decrypts the encrypted cache file using hardware-backed AES-256-GCM.
     * Throws an exception if the file has been tampered with or corrupted.
     */
    fun decryptCache(encryptedBytes: ByteArray): ByteArray {
        val minLength = CACHE_MAGIC_HEADER.size + 1 + GCM_IV_LENGTH_BYTES + (GCM_TAG_LENGTH_BITS / 8)
        if (encryptedBytes.size < minLength) {
            throw IllegalArgumentException("Corrupted or truncated cache file (size ${encryptedBytes.size})")
        }

        // Validate header
        for (i in CACHE_MAGIC_HEADER.indices) {
            if (encryptedBytes[i] != CACHE_MAGIC_HEADER[i]) {
                throw IllegalArgumentException("Invalid cache file header: not a Txtura encrypted file")
            }
        }

        val version = encryptedBytes[CACHE_MAGIC_HEADER.size]
        if (version != CACHE_FORMAT_VERSION) {
            throw IllegalArgumentException("Unsupported cache format version: $version")
        }

        val ivOffset = CACHE_MAGIC_HEADER.size + 1
        val iv = encryptedBytes.copyOfRange(ivOffset, ivOffset + GCM_IV_LENGTH_BYTES)
        val ciphertextOffset = ivOffset + GCM_IV_LENGTH_BYTES
        val ciphertext = encryptedBytes.copyOfRange(ciphertextOffset, encryptedBytes.size)

        val key = getOrCreateKeyStoreSecretKey(KEY_ALIAS_CACHE)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

        return cipher.doFinal(ciphertext)
    }

    // =========================================================================
    // 3. Backup Encryption (Hardware KeyStore OR Password-Derived AES-256-GCM)
    // =========================================================================

    /**
     * Checks if a backup payload is encrypted with Txtura authenticated encryption.
     */
    fun isEncryptedBackup(rawString: String): Boolean {
        val trimmed = rawString.trim()
        if (!trimmed.startsWith("{")) return false
        return try {
            val obj = JSONObject(trimmed)
            obj.optBoolean("txturaEncrypted", false) &&
                obj.optString("format") == BACKUP_CONTAINER_FORMAT
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks if an encrypted backup requires a user-provided password to decrypt.
     */
    fun isPasswordProtectedBackup(rawString: String): Boolean {
        return try {
            val obj = JSONObject(rawString.trim())
            obj.optString("type") == BACKUP_TYPE_PASSWORD
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Encrypts the entire backup JSON string:
     * - If [password] is provided: uses PBKDF2 (100k iterations) + AES-256-GCM (portable to any device).
     * - If [password] is null/blank: uses Android KeyStore hardware key (tamper-proof on current device).
     */
    fun encryptBackup(jsonPlaintext: String, password: String? = null): String {
        val plaintextBytes = jsonPlaintext.toByteArray(Charsets.UTF_8)
        val root = JSONObject()
        root.put("txturaEncrypted", true)
        root.put("format", BACKUP_CONTAINER_FORMAT)
        root.put("version", 1)
        root.put("createdAtMillis", System.currentTimeMillis())

        if (!password.isNullOrBlank()) {
            // Password-derived encryption (cross-device portable)
            val salt = ByteArray(SALT_LENGTH_BYTES)
            secureRandom.nextBytes(salt)

            val iv = ByteArray(GCM_IV_LENGTH_BYTES)
            secureRandom.nextBytes(iv)

            val key = deriveKeyFromPassword(password, salt)
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            val ciphertext = cipher.doFinal(plaintextBytes)

            root.put("type", BACKUP_TYPE_PASSWORD)
            root.put("kdf", PBKDF2_ALGORITHM)
            root.put("iterations", PBKDF2_ITERATIONS)
            root.put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            root.put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            root.put("ciphertext", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
        } else {
            // Device KeyStore encryption (hardware-bound, zero-password convenience)
            val key = getOrCreateKeyStoreSecretKey(KEY_ALIAS_BACKUP)
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv ?: throw IllegalStateException("Cipher failed to generate IV")
            val ciphertext = cipher.doFinal(plaintextBytes)

            root.put("type", BACKUP_TYPE_DEVICE)
            root.put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            root.put("ciphertext", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
        }

        return root.toString(2)
    }

    /**
     * Decrypts an encrypted backup container string back into the original plain JSON string.
     * Throws [InvalidBackupPasswordException] if password is wrong or file is tampered.
     * Throws [DeviceKeyMismatchException] if device key is not found (e.g. backup moved to new device without password).
     */
    fun decryptBackup(rawContainerString: String, password: String? = null): String {
        val root = JSONObject(rawContainerString.trim())
        val type = root.optString("type")

        return when (type) {
            BACKUP_TYPE_PASSWORD -> {
                if (password.isNullOrBlank()) {
                    throw InvalidBackupPasswordException("Password required to decrypt this backup")
                }
                val saltBase64 = root.optString("salt")
                val ivBase64 = root.optString("iv")
                val ciphertextBase64 = root.optString("ciphertext")
                val iterations = root.optInt("iterations", PBKDF2_ITERATIONS)

                if (saltBase64.isBlank() || ivBase64.isBlank() || ciphertextBase64.isBlank()) {
                    throw IllegalArgumentException("Malformed password-protected backup container")
                }

                val salt = Base64.decode(saltBase64, Base64.DEFAULT)
                val iv = Base64.decode(ivBase64, Base64.DEFAULT)
                val ciphertext = Base64.decode(ciphertextBase64, Base64.DEFAULT)

                try {
                    val key = deriveKeyFromPassword(password, salt, iterations)
                    val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
                    cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
                    val decryptedBytes = cipher.doFinal(ciphertext)
                    String(decryptedBytes, Charsets.UTF_8)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to decrypt password backup: invalid password or tag mismatch", e)
                    throw InvalidBackupPasswordException("Incorrect backup password")
                }
            }

            BACKUP_TYPE_DEVICE -> {
                val ivBase64 = root.optString("iv")
                val ciphertextBase64 = root.optString("ciphertext")
                if (ivBase64.isBlank() || ciphertextBase64.isBlank()) {
                    throw IllegalArgumentException("Malformed device-encrypted backup container")
                }

                val iv = Base64.decode(ivBase64, Base64.DEFAULT)
                val ciphertext = Base64.decode(ciphertextBase64, Base64.DEFAULT)

                try {
                    val key = getOrCreateKeyStoreSecretKey(KEY_ALIAS_BACKUP)
                    val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
                    cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
                    val decryptedBytes = cipher.doFinal(ciphertext)
                    String(decryptedBytes, Charsets.UTF_8)
                } catch (e: Exception) {
                    Log.w(TAG, "Device key decryption failed. Likely restored on a different phone without password", e)
                    throw DeviceKeyMismatchException("This backup was encrypted with a different device's hardware key")
                }
            }

            else -> throw IllegalArgumentException("Unknown backup encryption type: $type")
        }
    }

    private fun deriveKeyFromPassword(
        password: String,
        salt: ByteArray,
        iterations: Int = PBKDF2_ITERATIONS
    ): SecretKey {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
