package com.txtura.security

import android.app.KeyguardManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.core.content.edit
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLPeerUnverifiedException

/**
 * Manages device-level privacy and security controls for Txtura:
 * 1. Root state detection & OS integrity scanning
 * 2. Local conversation cache encryption preference management
 * 3. Biometric & PIN / Passphrase app lock policies
 * 4. HTTPS & TLS/SSL certificate pinning enforcement
 * 5. Metadata minimization & Zero-telemetry policies
 */
object AppSecurityManager {

    private const val PREFS_SECURITY = "txtura_security_privacy_prefs"
    private const val KEY_DB_ENCRYPTION = "pref_db_encryption_enabled"
    private const val KEY_BIOMETRIC_LOCK = "pref_biometric_lock_enabled"
    private const val KEY_LOCK_TIMEOUT = "pref_biometric_lock_timeout_mins"
    private const val KEY_APP_PIN_HASH = "pref_app_pin_hash_v1"
    private const val KEY_SSL_PINNING = "pref_ssl_pinning_enabled"
    private const val KEY_METADATA_MINIMIZATION = "pref_metadata_minimization_enabled"
    private const val KEY_INTENT_SANITIZER = "pref_intent_sanitizer_enabled"
    private const val KEY_ZERO_PERM_PHOTO_PICKER = "pref_zero_perm_photo_picker_enabled"
    private const val KEY_SENSITIVE_CLIPBOARD_COPY = "pref_sensitive_clipboard_copy_enabled"
    private const val KEY_PROTECTED_RECEIVERS = "pref_protected_receivers_enabled"
    private const val KEY_ROOT_DETECTION = "pref_root_detection_enabled"
    private const val KEY_BACKUP_ENCRYPTION = "pref_backup_encryption_enabled"
    private const val KEY_SPAM_FIREWALL = "pref_spam_firewall_enabled"

    data class RootCheckResult(
        val isRooted: Boolean,
        val suBinaryFound: Boolean,
        val testKeysFound: Boolean,
        val rootManagementAppFound: Boolean,
        val scanTimestamp: Long = System.currentTimeMillis(),
    )

    private val SU_PATHS = arrayOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su",
        "/su/bin/su",
        "/system/xbin/daemonsu",
        "/system/bin/daemonsu"
    )

    private val ROOT_PACKAGES = arrayOf(
        "com.noshufou.android.su",
        "com.thirdparty.superuser",
        "eu.chainfire.supersu",
        "com.koushikdutta.superuser",
        "com.topjohnwu.magisk",
        "me.weishu.kernelsu",
        "org.bnd.apatch",
        "stericson.busybox"
    )

    private val MAGISK_KSU_PATHS = arrayOf(
        "/sbin/.magisk",
        "/data/adb/magisk",
        "/data/adb/ksu",
        "/data/adb/ap"
    )

    // SHA-256 SPKI Public Key Hashes for GitHub and security endpoints
    private val PINNED_SPKI_HASHES = mapOf(
        "api.github.com" to setOf(
            "nTvjJtN51Jp5ounTgUohZxRsHH5DUHgGorVUZyBxrAM=", // *.github.com (active leaf)
            "VqePxH3EcFwZuYK3CCOMz5HKMoeIZpZcEyBf4diPGSA=", // Sectigo Public Server Authentication CA DV E36 (active intermediate)
            "EdsvlytFf4a/O+hCPwBXFFi46RKXqivCAF+mO7s+5Ng=", // Sectigo Public Server Authentication Root E46 (active root)
            "WoiWRyIOVNa9ihaBciRSC7XHjliYS9VwUGOIud4PB18=", // DigiCert High Assurance TLS Hybrid ECC
            "kIdp6NNEd8wsugYyyIYFsi1ylMCED3hEZMmwcYAASMg=", // DigiCert Global Root G2
            "4a3DemjEi9ahGUHuSafxSkPhcEiWS8NDfl2WFiPiJtE="  // DigiCert Global Root CA
        ),
        "github.com" to setOf(
            "fn1BKnc6ZJaaPk6Amsualghpmf+/FjN+QGxEQoqHFI4=", // github.com (active leaf)
            "VqePxH3EcFwZuYK3CCOMz5HKMoeIZpZcEyBf4diPGSA=", // Sectigo Public Server Authentication CA DV E36 (active intermediate)
            "EdsvlytFf4a/O+hCPwBXFFi46RKXqivCAF+mO7s+5Ng=", // Sectigo Public Server Authentication Root E46 (active root)
            "WoiWRyIOVNa9ihaBciRSC7XHjliYS9VwUGOIud4PB18=",
            "kIdp6NNEd8wsugYyyIYFsi1ylMCED3hEZMmwcYAASMg="
        ),
        "security.cloudflare-dns.com" to setOf(
            "kIdp6NNEd8wsugYyyIYFsi1ylMCED3hEZMmwcYAASMg="
        )
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_SECURITY, Context.MODE_PRIVATE)
    }

    /**
     * Performs a deep scan of the device operating system for known root compromises.
     */
    fun checkRootStatus(context: Context): RootCheckResult {
        // 1. Check for test-keys in build tags
        val buildTags = Build.TAGS
        val testKeysFound = (buildTags != null) && buildTags.contains("test-keys")

        // 2. Check for presence of su binary in system locations
        var suFound = false
        for (path in SU_PATHS) {
            try {
                if (File(path).exists()) {
                    suFound = true
                    break
                }
            } catch (_: Exception) {}
        }

        // 2b. Check which su command
        if (!suFound) {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
                val line = process.inputStream.bufferedReader().readLine()
                if (!line.isNullOrBlank()) {
                    suFound = true
                }
                process.destroy()
            } catch (_: Exception) {}
        }

        // 3. Check for root privilege manager package installations or Magisk/KSU artifacts
        var rootAppFound = false
        val pm = context.packageManager
        for (pkg in ROOT_PACKAGES) {
            try {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, 0)
                rootAppFound = true
                break
            } catch (_: Exception) {}
        }

        if (!rootAppFound) {
            for (p in MAGISK_KSU_PATHS) {
                try {
                    if (File(p).exists()) {
                        rootAppFound = true
                        break
                    }
                } catch (_: Exception) {}
            }
        }

        val isRooted = testKeysFound || suFound || rootAppFound
        return RootCheckResult(
            isRooted = isRooted,
            suBinaryFound = suFound,
            testKeysFound = testKeysFound,
            rootManagementAppFound = rootAppFound
        )
    }

    /**
     * Checks whether device has lock screen credentials (PIN, pattern, password or biometric) configured.
     */
    fun isDeviceLockConfigured(context: Context): Boolean {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return km?.isDeviceSecure == true
    }

    // =========================================================================
    // SSL Pinning & Secure Connection Opener
    // =========================================================================

    /**
     * Opens an HTTP/HTTPS connection with strict SSL Pinning and HTTPS enforcement.
     * Throws [SecurityException] if unencrypted HTTP is attempted while pinning is active.
     * Throws [SSLPeerUnverifiedException] if the peer certificate SPKI does not match pinned keys.
     */
    fun openSecureConnection(url: URL, context: Context? = null): HttpURLConnection {
        val pinningActive = context?.let { isSslPinningEnabled(it) } ?: true

        if (pinningActive && url.protocol.equals("http", ignoreCase = true)) {
            throw SecurityException("Insecure plaintext HTTP connection blocked by Txtura SSL Pinning policy ($url)")
        }

        val conn = url.openConnection() as HttpURLConnection
        if (conn is HttpsURLConnection && pinningActive) {
            val host = url.host.lowercase()
            val pinnedHashes = PINNED_SPKI_HASHES[host]
            if (pinnedHashes != null) {
                // Verify peer certificates post-connect
                val originalHostnameVerifier = conn.hostnameVerifier
                conn.hostnameVerifier = javax.net.ssl.HostnameVerifier { hostname, session ->
                    if (!originalHostnameVerifier.verify(hostname, session)) {
                        return@HostnameVerifier false
                    }
                    try {
                        val certs = session.peerCertificates
                        if (certs.isNullOrEmpty()) return@HostnameVerifier false
                        val md = MessageDigest.getInstance("SHA-256")
                        var matched = false
                        for (cert in certs) {
                            val spkiHash = Base64.encodeToString(md.digest(cert.publicKey.encoded), Base64.NO_WRAP)
                            if (spkiHash in pinnedHashes) {
                                matched = true
                                break
                            }
                        }
                        matched
                    } catch (_: Exception) {
                        false
                    }
                }
            }
        }
        return conn
    }

    // =========================================================================
    // Metadata Minimization (EXIF & Telemetry Stripping)
    // =========================================================================

    /**
     * Strips all EXIF metadata (GPS coordinates, camera model, timestamp, serials)
     * by decoding the image bitmap and re-encoding it into a clean local file.
     */
    fun stripExifMetadata(context: Context, sourceUri: Uri): Uri {
        if (!isMetadataMinimizationEnabled(context)) return sourceUri
        return try {
            val bitmap = context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return sourceUri

            val cleanFile = File(context.cacheDir, "mms_clean_${System.currentTimeMillis()}.jpg")
            cleanFile.outputStream().use { outStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, outStream)
            }
            bitmap.recycle()
            Uri.fromFile(cleanFile)
        } catch (_: Exception) {
            sourceUri
        }
    }

    // =========================================================================
    // In-App PIN Support
    // =========================================================================

    fun hasCustomAppPin(context: Context): Boolean {
        return !getPrefs(context).getString(KEY_APP_PIN_HASH, null).isNullOrBlank()
    }

    fun setAppPin(context: Context, pin: String) {
        val hash = hashPin(pin)
        getPrefs(context).edit { putString(KEY_APP_PIN_HASH, hash) }
    }

    fun verifyAppPin(context: Context, pin: String): Boolean {
        val storedHash = getPrefs(context).getString(KEY_APP_PIN_HASH, null) ?: return false
        return storedHash == hashPin(pin)
    }

    private fun hashPin(pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest("txtura_salt_${pin.trim()}".toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    // =========================================================================
    // Security Preferences Accessors
    // =========================================================================

    fun isDbEncryptionEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_DB_ENCRYPTION, true)
    }

    fun setDbEncryptionEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_DB_ENCRYPTION, enabled) }
        com.txtura.data.SmsMmsRepository.reencryptCache(context)
    }

    fun isBiometricLockEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BIOMETRIC_LOCK, false)
    }

    fun setBiometricLockEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_BIOMETRIC_LOCK, enabled) }
    }

    fun getBiometricLockTimeout(context: Context): Int {
        return getPrefs(context).getInt(KEY_LOCK_TIMEOUT, 0)
    }

    fun setBiometricLockTimeout(context: Context, timeoutMinutes: Int) {
        getPrefs(context).edit { putInt(KEY_LOCK_TIMEOUT, timeoutMinutes) }
    }


    fun isSslPinningEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SSL_PINNING, true)
    }

    fun setSslPinningEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_SSL_PINNING, enabled) }
    }

    fun isMetadataMinimizationEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_METADATA_MINIMIZATION, true)
    }

    fun setMetadataMinimizationEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_METADATA_MINIMIZATION, enabled) }
    }

    fun isIntentSanitizerEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_INTENT_SANITIZER, true)
    }

    fun setIntentSanitizerEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_INTENT_SANITIZER, enabled) }
    }

    fun isZeroPermPhotoPickerEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ZERO_PERM_PHOTO_PICKER, true)
    }

    fun setZeroPermPhotoPickerEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_ZERO_PERM_PHOTO_PICKER, enabled) }
    }

    fun isSensitiveClipboardCopyEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SENSITIVE_CLIPBOARD_COPY, true)
    }

    fun setSensitiveClipboardCopyEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_SENSITIVE_CLIPBOARD_COPY, enabled) }
    }

    fun isProtectedReceiversEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_PROTECTED_RECEIVERS, true)
    }

    fun setProtectedReceiversEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_PROTECTED_RECEIVERS, enabled) }
    }

    fun isRootDetectionEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ROOT_DETECTION, true)
    }

    fun setRootDetectionEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_ROOT_DETECTION, enabled) }
    }

    fun isBackupEncryptionEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BACKUP_ENCRYPTION, true)
    }

    fun setBackupEncryptionEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_BACKUP_ENCRYPTION, enabled) }
    }

    fun isSpamFirewallEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SPAM_FIREWALL, true)
    }

    fun setSpamFirewallEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_SPAM_FIREWALL, enabled) }
    }
}
