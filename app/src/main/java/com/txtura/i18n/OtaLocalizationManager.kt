package com.txtura.i18n

import android.content.Context
import android.util.Log
import com.txtura.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * High-performance, zero-blocking Over-The-Air (OTA) Localization Engine.
 *
 * Hierarchy & Strict Fallback Rules:
 * 1. Primary: Cached local OTA JSON payload for active system language ([Locale.getDefault]).
 * 2. Fallback: Immediate English default (from built-in strings / res/values/strings.xml).
 * 3. Zero Cold Start impact: Inits lazily on [Dispatchers.IO] without blocking UI thread.
 * 4. Silent Error Handling: Any 404, 500, or network drop falls back gracefully to English.
 */
object OtaLocalizationManager {

    private const val TAG = "OtaLocalizationManager"
    private const val OTA_CACHE_DIR = "ota_i18n"

    private val _cachedTranslations = MutableStateFlow<Map<String, String>>(emptyMap())
    val cachedTranslations: StateFlow<Map<String, String>> = _cachedTranslations.asStateFlow()

    private val _currentLanguage = MutableStateFlow("en")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private var isInitialized = false

    /**
     * Initializes OTA Localization engine asynchronously on [Dispatchers.IO].
     * Guarantees zero blocking on Cold Start.
     */
    fun initializeAsync(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                loadLocalOtaTranslations(appContext)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load cached OTA translations, defaulting to English", e)
            }
        }
    }

    /**
     * Loads cached OTA translation JSON for the active system locale.
     * Falls back to English if missing, corrupt, or offline.
     */
    fun loadLocalOtaTranslations(context: Context) {
        val systemLocale = Locale.getDefault()
        val langCode = systemLocale.language.lowercase(Locale.ROOT)
        _currentLanguage.value = langCode

        val cacheDir = File(context.cacheDir, OTA_CACHE_DIR).apply { mkdirs() }
        val langFile = File(cacheDir, "strings_$langCode.json")

        if (langFile.exists() && langFile.length() > 0L) {
            try {
                val jsonStr = langFile.readText(Charsets.UTF_8)
                val jsonObject = JSONObject(jsonStr)
                val map = mutableMapOf<String, String>()
                val keys = jsonObject.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    map[key] = jsonObject.getString(key)
                }
                _cachedTranslations.value = map
                Log.i(TAG, "Successfully loaded OTA translations for language: $langCode (${map.size} keys)")
                return
            } catch (e: Exception) {
                Log.w(TAG, "Corrupt OTA JSON file for $langCode, purging and falling back to English", e)
                try { langFile.delete() } catch (_: Exception) {}
            }
        }

        // Fallback: Clear map to trigger English default
        _cachedTranslations.value = emptyMap()
        Log.i(TAG, "No cached OTA package for $langCode. Defaulting to English fallback.")
    }

    /**
     * Attempts a silent background OTA sync for a target language code.
     * [targetLangCode]: e.g., "fr", "sr", "de", "es"
     * Returns true if a new translation package was fetched and cached.
     */
    fun syncLanguagePackageOnline(context: Context, targetLangCode: String): Boolean {
        if (targetLangCode.isBlank() || targetLangCode == "en") return false

        return try {
            val projectId = BuildConfig.OTA_PROJECT_ID
            val hash = BuildConfig.OTA_DISTRIBUTION_HASH
            val urlString = "https://ota.crowdin.com/content/$projectId/$hash/strings_$targetLangCode.json"

            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                doInput = true
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                if (jsonText.isNotBlank() && jsonText.startsWith("{")) {
                    val cacheDir = File(context.cacheDir, OTA_CACHE_DIR).apply { mkdirs() }
                    val targetFile = File(cacheDir, "strings_$targetLangCode.json")
                    targetFile.writeText(jsonText, Charsets.UTF_8)

                    Log.i(TAG, "Successfully downloaded & cached OTA language package for $targetLangCode")

                    // If target language is the active system language, update in-memory state
                    if (targetLangCode == Locale.getDefault().language.lowercase(Locale.ROOT)) {
                        loadLocalOtaTranslations(context)
                    }
                    return true
                }
            } else {
                Log.d(TAG, "OTA server returned code ${connection.responseCode} for language $targetLangCode")
            }
            false
        } catch (e: Exception) {
            Log.d(TAG, "Silent OTA sync network attempt failed for $targetLangCode (offline/404): ${e.message}")
            false
        }
    }

    /**
     * Retrieves a dynamic localized string by key with strict English fallback.
     */
    fun getString(key: String, defaultEnglishText: String): String {
        return _cachedTranslations.value[key] ?: defaultEnglishText
    }
}
