@file:Suppress("SpellCheckingInspection")

package com.txtura.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import android.util.AtomicFile
import java.io.File
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
 * Authenticated encryption for the local conversation snapshot and exported backups.
 *
 * The local Room snapshot can use a random AES-256-GCM data key wrapped by Android
 * Keystore. The SQLCipher database passphrase is independently generated and Keystore-wrapped.
 */
object SecurityCryptoManager {

    private const val TAG = "SecurityCryptoManager"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val GCM_IV_LENGTH_BYTES = 12

    private val CACHE_MAGIC_HEADER = byteArrayOf('T'.code.toByte(), 'E'.code.toByte(), 'N'.code.toByte(), 'C'.code.toByte())
    private val CACHE_KEY_MAGIC_HEADER = byteArrayOf('T'.code.toByte(), 'K'.code.toByte(), 'E'.code.toByte(), 'Y'.code.toByte())
    private const val CACHE_FORMAT_VERSION_V1: Byte = 1
    private const val CACHE_FORMAT_VERSION_V2: Byte = 2
    private const val CACHE_DATA_KEY_FILE = "txtura_cache_data_key_v2"
    private const val CACHE_FILE_NAME = "threads_cache.enc"
    private val CACHE_V2_AAD = CACHE_MAGIC_HEADER + byteArrayOf(CACHE_FORMAT_VERSION_V2)

    private val SQLCIPHER_KEY_MAGIC_HEADER =
        byteArrayOf('T'.code.toByte(), 'D'.code.toByte(), 'B'.code.toByte(), 'K'.code.toByte())
    private const val SQLCIPHER_KEY_FORMAT_VERSION: Byte = 1
    private const val SQLCIPHER_PASSPHRASE_LENGTH_BYTES = 44
    private const val SQLCIPHER_KEY_FILE = "txtura_room_sqlcipher_key_v1"
    private const val KEY_ALIAS_SQLCIPHER = "txtura_room_sqlcipher_wrap_v1"
    private val SQLCIPHER_KEY_AAD = SQLCIPHER_KEY_MAGIC_HEADER + byteArrayOf(SQLCIPHER_KEY_FORMAT_VERSION)
    private val sqlCipherKeyLock = Any()

    private const val KEY_ALIAS_CACHE = "txtura_conversations_cache_v1"
    private const val KEY_ALIAS_BACKUP = "txtura_backup_device_key_v1"

    private val cacheKeyLock = Any()
    @Volatile
    private var cacheSessionKey: ByteArray? = null

    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 100_000
    private const val SALT_LENGTH_BYTES = 16
    private const val KEY_LENGTH_BITS = 256

    const val BACKUP_CONTAINER_FORMAT = "TXTURA_ENCRYPTED_BACKUP"
    const val BACKUP_TYPE_DEVICE = "DEVICE_KEYSTORE"
    const val BACKUP_TYPE_PASSWORD = "PASSWORD"

    private val secureRandom = SecureRandom()

    // =========================================================================
    // 1. Android Keystore-Managed Key Management
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

    /**
     * Returns the stable random passphrase used by SQLCipher to encrypt the Room database.
     * The passphrase is wrapped by a separate Android Keystore AES-GCM key and stored in
     * noBackupFilesDir so Android backup/restore cannot separate it from the database.
     */
    fun getOrCreateSqlCipherPassphrase(
        context: Context,
        databaseFileExists: Boolean,
    ): ByteArray = synchronized(sqlCipherKeyLock) {
        val keyFile = File(context.noBackupFilesDir, SQLCIPHER_KEY_FILE)
        val atomicKeyFile = AtomicFile(keyFile)
        val backupKeyFile = File(keyFile.path + ".bak")

        if (keyFile.exists() || backupKeyFile.exists()) {
            val wrappedBytes = atomicKeyFile.openRead().use { it.readBytes() }
            try {
                unwrapSqlCipherPassphrase(wrappedBytes)
            } catch (error: Exception) {
                throw IllegalStateException(
                    "Unable to unlock the Room SQLCipher database key; database files were preserved.",
                    error,
                )
            } finally {
                wrappedBytes.fill(0)
            }
        } else {
            check(!databaseFileExists) {
                "Room SQLCipher key is missing while the database exists; refusing to replace the database key."
            }
            val randomKeyMaterial = ByteArray(32)
            secureRandom.nextBytes(randomKeyMaterial)
            val passphrase = Base64.encode(randomKeyMaterial, Base64.NO_WRAP)
            randomKeyMaterial.fill(0)
            try {
                persistSqlCipherPassphrase(keyFile, wrapSqlCipherPassphrase(passphrase))
                passphrase
            } catch (error: Exception) {
                passphrase.fill(0)
                throw error
            }
        }
    }

    private fun wrapSqlCipherPassphrase(passphrase: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKeyStoreSecretKey(KEY_ALIAS_SQLCIPHER))
        cipher.updateAAD(SQLCIPHER_KEY_AAD)
        val iv = cipher.iv ?: throw IllegalStateException("Cipher failed to generate SQLCipher key IV")
        val encryptedPassphrase = cipher.doFinal(passphrase)
        return ByteBuffer.allocate(
            SQLCIPHER_KEY_MAGIC_HEADER.size + 1 + iv.size + encryptedPassphrase.size
        )
            .put(SQLCIPHER_KEY_MAGIC_HEADER)
            .put(SQLCIPHER_KEY_FORMAT_VERSION)
            .put(iv)
            .put(encryptedPassphrase)
            .array()
    }

    private fun unwrapSqlCipherPassphrase(wrappedBytes: ByteArray): ByteArray {
        val expectedLength =
            SQLCIPHER_KEY_MAGIC_HEADER.size + 1 + GCM_IV_LENGTH_BYTES +
                SQLCIPHER_PASSPHRASE_LENGTH_BYTES + (GCM_TAG_LENGTH_BITS / 8)
        require(wrappedBytes.size == expectedLength) { "Invalid wrapped SQLCipher passphrase length" }
        require(SQLCIPHER_KEY_MAGIC_HEADER.indices.all { wrappedBytes[it] == SQLCIPHER_KEY_MAGIC_HEADER[it] }) {
            "Invalid wrapped SQLCipher passphrase header"
        }
        require(wrappedBytes[SQLCIPHER_KEY_MAGIC_HEADER.size] == SQLCIPHER_KEY_FORMAT_VERSION) {
            "Unsupported wrapped SQLCipher passphrase version"
        }

        val ivOffset = SQLCIPHER_KEY_MAGIC_HEADER.size + 1
        val iv = wrappedBytes.copyOfRange(ivOffset, ivOffset + GCM_IV_LENGTH_BYTES)
        val ciphertext = wrappedBytes.copyOfRange(ivOffset + GCM_IV_LENGTH_BYTES, wrappedBytes.size)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKeyStoreSecretKey(KEY_ALIAS_SQLCIPHER),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv),
        )
        cipher.updateAAD(SQLCIPHER_KEY_AAD)
        return cipher.doFinal(ciphertext)
    }

    private fun persistSqlCipherPassphrase(keyFile: File, wrappedBytes: ByteArray) {
        keyFile.parentFile?.mkdirs()
        val atomicFile = AtomicFile(keyFile)
        val stream = atomicFile.startWrite()
        try {
            stream.write(wrappedBytes)
            atomicFile.finishWrite(stream)
        } catch (error: Exception) {
            atomicFile.failWrite(stream)
            throw error
        } finally {
            wrappedBytes.fill(0)
        }
    }

    // =========================================================================
    // 2. Local Conversation Snapshot Encryption
    // =========================================================================

    /**
     * Initializes the cache data key on a worker thread. Keystore is used once to unwrap
     * the data key. New v2 snapshots use AES-GCM with the in-memory raw key.
     */
    fun initializeCacheSessionKey(context: Context) {
        if (cacheSessionKey != null) return
        synchronized(cacheKeyLock) {
            if (cacheSessionKey != null) return

            val keyFile = File(context.noBackupFilesDir, CACHE_DATA_KEY_FILE)
            val atomicKeyFile = AtomicFile(keyFile)
            val backupKeyFile = File(keyFile.path + ".bak")
            val keyBytes = if (keyFile.exists() || backupKeyFile.exists()) {
                val wrappedKey = atomicKeyFile.openRead().use { it.readBytes() }
                try {
                    unwrapCacheDataKey(wrappedKey)
                } catch (error: java.security.GeneralSecurityException) {
                    Log.w(TAG, "Discarding cache snapshot whose wrapped key can no longer be opened", error)
                    discardUnrecoverableCache(context, atomicKeyFile)
                    createAndPersistCacheDataKey(keyFile)
                } catch (error: IllegalArgumentException) {
                    Log.w(TAG, "Discarding cache snapshot with an invalid wrapped key", error)
                    discardUnrecoverableCache(context, atomicKeyFile)
                    createAndPersistCacheDataKey(keyFile)
                } finally {
                    wrappedKey.fill(0)
                }
            } else {
                if (hasV2CacheFile(context)) {
                    Log.w(TAG, "Discarding cache snapshot without its wrapped data key")
                    File(context.filesDir, CACHE_FILE_NAME).delete()
                }
                createAndPersistCacheDataKey(keyFile)
            }
            cacheSessionKey = keyBytes
        }
    }

    /**
     * Encrypts a snapshot using the cached random AES-256 data key.
     * V2 format: [TENC] [version 2] [12-byte IV] [ciphertext + GCM tag].
     */
    fun encryptCache(context: Context, plaintextBytes: ByteArray): ByteArray {
        initializeCacheSessionKey(context)
        val keyBytes = copyCacheSessionKey()
        try {
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "AES"))
            cipher.updateAAD(CACHE_V2_AAD)
            val iv = cipher.iv ?: throw IllegalStateException("Cipher failed to generate IV")
            val ciphertext = cipher.doFinal(plaintextBytes)

            return ByteBuffer.allocate(CACHE_MAGIC_HEADER.size + 1 + iv.size + ciphertext.size)
                .put(CACHE_MAGIC_HEADER)
                .put(CACHE_FORMAT_VERSION_V2)
                .put(iv)
                .put(ciphertext)
                .array()
        } finally {
            keyBytes.fill(0)
        }
    }

    /** Reads both the original Keystore-encrypted v1 file and the v2 wrapped-data-key format. */
    fun decryptCache(context: Context, encryptedBytes: ByteArray): ByteArray {
        val minLength = CACHE_MAGIC_HEADER.size + 1 + GCM_IV_LENGTH_BYTES + (GCM_TAG_LENGTH_BITS / 8)
        require(encryptedBytes.size >= minLength) {
            "Corrupted or truncated cache file (size ${encryptedBytes.size})"
        }
        require(CACHE_MAGIC_HEADER.indices.all { encryptedBytes[it] == CACHE_MAGIC_HEADER[it] }) {
            "Invalid cache file header: not a Txtura encrypted file"
        }

        val version = encryptedBytes[CACHE_MAGIC_HEADER.size]
        val ivOffset = CACHE_MAGIC_HEADER.size + 1
        val iv = encryptedBytes.copyOfRange(ivOffset, ivOffset + GCM_IV_LENGTH_BYTES)
        val ciphertext = encryptedBytes.copyOfRange(ivOffset + GCM_IV_LENGTH_BYTES, encryptedBytes.size)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)

        return when (version) {
            CACHE_FORMAT_VERSION_V1 -> {
                val key = getOrCreateKeyStoreSecretKey(KEY_ALIAS_CACHE)
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
                cipher.doFinal(ciphertext)
            }
            CACHE_FORMAT_VERSION_V2 -> {
                initializeCacheSessionKey(context)
                val keyBytes = copyCacheSessionKey()
                try {
                    cipher.init(
                        Cipher.DECRYPT_MODE,
                        SecretKeySpec(keyBytes, "AES"),
                        GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
                    )
                    cipher.updateAAD(CACHE_V2_AAD)
                    cipher.doFinal(ciphertext)
                } finally {
                    keyBytes.fill(0)
                }
            }
            else -> throw IllegalArgumentException("Unsupported cache format version: $version")
        }
    }

    fun isEncryptedCache(encryptedBytes: ByteArray): Boolean =
        encryptedBytes.size >= CACHE_MAGIC_HEADER.size + 1 &&
            CACHE_MAGIC_HEADER.indices.all { encryptedBytes[it] == CACHE_MAGIC_HEADER[it] } &&
            encryptedBytes[CACHE_MAGIC_HEADER.size] in setOf(CACHE_FORMAT_VERSION_V1, CACHE_FORMAT_VERSION_V2)

    fun isLegacyCacheFormat(encryptedBytes: ByteArray): Boolean =
        isEncryptedCache(encryptedBytes) &&
            encryptedBytes[CACHE_MAGIC_HEADER.size] == CACHE_FORMAT_VERSION_V1

    /** Clears the in-process data key and optionally removes its wrapped copy. */
    fun clearCacheSessionKey(context: Context, deleteWrappedKey: Boolean = false) {
        synchronized(cacheKeyLock) {
            cacheSessionKey?.fill(0)
            cacheSessionKey = null
            if (deleteWrappedKey) {
                AtomicFile(File(context.noBackupFilesDir, CACHE_DATA_KEY_FILE)).delete()
            }
        }
    }

    private fun copyCacheSessionKey(): ByteArray = synchronized(cacheKeyLock) {
        cacheSessionKey?.copyOf() ?: error("Cache session key has not been initialized")
    }

    private fun createAndPersistCacheDataKey(keyFile: File): ByteArray {
        val generatedKey = ByteArray(32)
        secureRandom.nextBytes(generatedKey)
        try {
            writeWrappedCacheDataKey(keyFile, wrapCacheDataKey(generatedKey))
            return generatedKey
        } catch (error: Exception) {
            generatedKey.fill(0)
            throw error
        }
    }

    private fun discardUnrecoverableCache(context: Context, keyFile: AtomicFile) {
        File(context.filesDir, CACHE_FILE_NAME).delete()
        keyFile.delete()
    }

    private fun wrapCacheDataKey(keyBytes: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKeyStoreSecretKey(KEY_ALIAS_CACHE))
        cipher.updateAAD(CACHE_KEY_MAGIC_HEADER)
        val iv = cipher.iv ?: throw IllegalStateException("Cipher failed to generate cache-key IV")
        val encryptedKey = cipher.doFinal(keyBytes)
        return ByteBuffer.allocate(CACHE_KEY_MAGIC_HEADER.size + iv.size + encryptedKey.size)
            .put(CACHE_KEY_MAGIC_HEADER)
            .put(iv)
            .put(encryptedKey)
            .array()
    }

    private fun unwrapCacheDataKey(wrappedBytes: ByteArray): ByteArray {
        val expectedLength = CACHE_KEY_MAGIC_HEADER.size + GCM_IV_LENGTH_BYTES + 32 + (GCM_TAG_LENGTH_BITS / 8)
        require(wrappedBytes.size == expectedLength) { "Invalid wrapped cache key length" }
        require(CACHE_KEY_MAGIC_HEADER.indices.all { wrappedBytes[it] == CACHE_KEY_MAGIC_HEADER[it] }) {
            "Invalid wrapped cache key header"
        }

        val ivOffset = CACHE_KEY_MAGIC_HEADER.size
        val iv = wrappedBytes.copyOfRange(ivOffset, ivOffset + GCM_IV_LENGTH_BYTES)
        val encryptedKey = wrappedBytes.copyOfRange(ivOffset + GCM_IV_LENGTH_BYTES, wrappedBytes.size)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKeyStoreSecretKey(KEY_ALIAS_CACHE),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        )
        cipher.updateAAD(CACHE_KEY_MAGIC_HEADER)
        return cipher.doFinal(encryptedKey)
    }

    private fun writeWrappedCacheDataKey(file: File, bytes: ByteArray) {
        file.parentFile?.mkdirs()
        val atomicFile = AtomicFile(file)
        val stream = atomicFile.startWrite()
        try {
            stream.write(bytes)
            atomicFile.finishWrite(stream)
        } catch (error: Exception) {
            atomicFile.failWrite(stream)
            throw error
        } finally {
            bytes.fill(0)
        }
    }

    private fun hasV2CacheFile(context: Context): Boolean {
        val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
        return !(!cacheFile.isFile || cacheFile.length() < CACHE_MAGIC_HEADER.size + 1L) && cacheFile.inputStream().buffered().use { input ->
            val header = ByteArray(CACHE_MAGIC_HEADER.size + 1)
            input.read(header) == header.size &&
                    CACHE_MAGIC_HEADER.indices.all { header[it] == CACHE_MAGIC_HEADER[it] } &&
                    header[CACHE_MAGIC_HEADER.size] == CACHE_FORMAT_VERSION_V2
        }
    }

    // =========================================================================
    // 3. Backup Encryption (Hardware KeyStore OR Password-Derived AES-256-GCM)
    // =========================================================================

    /**
     * Checks if a backup payload is encrypted with Txtura authenticated encryption.
     */
    fun isEncryptedBackup(rawString: String): Boolean {
        val trimmed = rawString.trim()
        return trimmed.startsWith("{") && try {
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
     * - If [password] is null/blank: uses an Android Keystore-managed key bound to this installation.
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
            // Device Keystore encryption (installation-bound, zero-password convenience)
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
        return when (val type = root.optString("type")) {
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
        val passwordChars = password.toCharArray()
        val spec = PBEKeySpec(passwordChars, salt, iterations, KEY_LENGTH_BITS)
        try {
            val keyBytes = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
                .generateSecret(spec)
                .encoded
            return try {
                SecretKeySpec(keyBytes, "AES")
            } finally {
                keyBytes.fill(0)
            }
        } finally {
            spec.clearPassword()
            passwordChars.fill('\u0000')
        }
    }
}
