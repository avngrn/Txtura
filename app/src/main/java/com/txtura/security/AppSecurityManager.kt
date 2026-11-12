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

/**
 * Manages device-level privacy and security controls for Txtura:
 * 1. Root state detection & OS integrity scanning
 * 2. Local conversation cache encryption preference management
 * 3. Biometric & device credential app lock policies
 * 4. HTTPS & TLS/SSL certificate pinning enforcement
 * 5. Metadata minimization & Zero-telemetry policies
 */
object AppSecurityManager {

    private const val PREFS_SECURITY = "txtura_security_privacy_prefs"
    private const val KEY_DB_ENCRYPTION = "pref_db_encryption_enabled"
    private const val KEY_BIOMETRIC_LOCK = "pref_biometric_lock_enabled"
    private const val KEY_LOCK_TIMEOUT = "pref_biometric_lock_timeout_mins"
    private const val KEY_SSL_PINNING = "pref_ssl_pinning_enabled"
    private const val KEY_METADATA_MINIMIZATION = "pref_metadata_minimization_enabled"
    private const val KEY_ROOT_DETECTION = "pref_root_detection_enabled"
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
            "nTvjJtN51Jp5ounTgUohZxRsHH5DUHgGorVUZyBxrAM=", // *.gitHub.com (active leaf)
            "VqePxH3EcFwZuYK3CCOMz5HKMoeIZpZcEyBf4diPGSA=", // Sectigo Public Server Authentication CA DV E36 (active intermediate)
            "EdsvlytFf4a/O+hCPwBXFFi46RKXqivCAF+mO7s+5Ng=", // Sectigo Public Server Authentication Root E46 (active root)
            "WoiWRyIOVNa9ihaBciRSC7XHjliYS9VwUGOIud4PB18=", // DigiCert High Assurance TLS Hybrid ECC
            "kIdp6NNEd8wsugYyyIYFsi1ylMCED3hEZMmwcYAASMg=", // DigiCert Global Root G2
            "4a3DemjEi9ahGUHuSafxSkPhcEiWS8NDfl2WFiPiJtE="  // DigiCert Global Root CA
        ),
        "github.com" to setOf(
            "fn1BKnc6ZJaaPk6Amsualghpmf+/FjN+QGxEQoqHFI4=", // gitHub.com (active leaf)
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
        if (!isRootDetectionEnabled(context)) {
            return RootCheckResult(
                isRooted = false,
                suBinaryFound = false,
                testKeysFound = false,
                rootManagementAppFound = false,
            )
        }

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
     * Opens an HTTPS connection. HTTPS is mandatory even when optional certificate pinning is off.
     * Configured hosts additionally require a matching SPKI hash while pinning is enabled;
     * other hosts still use Android's normal certificate-chain and hostname validation.
     */
    fun openSecureConnection(url: URL, context: Context): HttpURLConnection {
        if (!url.protocol.equals("https", ignoreCase = true)) {
            throw SecurityException("Only HTTPS connections are allowed by Txtura ($url)")
        }

        val conn = url.openConnection() as? HttpsURLConnection
            ?: throw SecurityException("URL did not create an HTTPS connection ($url)")
        if (isSslPinningEnabled(context)) {
            val host = url.host.lowercase()
            val pinnedHashes = PINNED_SPKI_HASHES[host]
            if (pinnedHashes != null) {
                val originalHostnameVerifier = conn.hostnameVerifier
                conn.hostnameVerifier = javax.net.ssl.HostnameVerifier { hostname, session ->
                    if (!originalHostnameVerifier.verify(hostname, session)) {
                        return@HostnameVerifier false
                    }
                    try {
                        val certs = session.peerCertificates
                        if (certs.isNullOrEmpty()) return@HostnameVerifier false
                        val md = MessageDigest.getInstance("SHA-256")
                        certs.any { cert ->
                            val spkiHash = Base64.encodeToString(md.digest(cert.publicKey.encoded), Base64.NO_WRAP)
                            spkiHash in pinnedHashes
                        }
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



    // =========================================================================
    // Security Preferences Accessors
    // =========================================================================

    fun isDbEncryptionEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_DB_ENCRYPTION, false)
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
        return getPrefs(context).getBoolean(KEY_SSL_PINNING, false)
    }

    fun setSslPinningEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_SSL_PINNING, enabled) }
    }

    fun isMetadataMinimizationEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_METADATA_MINIMIZATION, false)
    }

    fun setMetadataMinimizationEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_METADATA_MINIMIZATION, enabled) }
    }

    fun isRootDetectionEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ROOT_DETECTION, false)
    }

    fun setRootDetectionEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_ROOT_DETECTION, enabled) }
    }

    fun isSpamFirewallEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SPAM_FIREWALL, false)
    }

    fun setSpamFirewallEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_SPAM_FIREWALL, enabled) }
        com.txtura.data.SmsMmsRepository.refreshSpamFirewallState(context)
    }
}
