package com.txtura.telephony

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.txtura.MainActivity
import com.txtura.R
import com.txtura.ui.i18n.TxturaStrings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

enum class EmergencySeverity {
    GLOBAL_CATASTROPHE,
    PRESIDENTIAL,
    EXTREME,
    SEVERE,
    AMBER_ALERT,
    TEST,
    INFO;

    val isGlobal: Boolean
        get() = this == GLOBAL_CATASTROPHE || this == PRESIDENTIAL
}

data class EmergencyAlert(
    val id: Long,
    val title: String,
    val body: String,
    val severity: EmergencySeverity,
    val timestampMillis: Long,
    val countryIso: String,
    val scope: String,
    val isRead: Boolean = false
)

object EmergencyAlertManager {

    private const val TAG = "EmergencyAlertManager"
    private const val PREFS_NAME = "txtura_emergency_alerts_prefs"

    private const val KEY_EXTREME_ENABLED = "key_emergency_extreme_enabled"
    private const val KEY_SEVERE_ENABLED = "key_emergency_severe_enabled"
    private const val KEY_AMBER_ENABLED = "key_emergency_amber_enabled"
    private const val KEY_SOUND_ENABLED = "key_emergency_sound_enabled"
    private const val KEY_SAVED_ALERTS_JSON = "key_emergency_saved_alerts_json"

    private const val CHANNEL_ID_EMERGENCY = "channel_txtura_emergency_alerts_v1"

    private val _alerts = MutableStateFlow<List<EmergencyAlert>>(emptyList())
    val alerts = _alerts.asStateFlow()

    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        isInitialized = true
        ensureNotificationChannel(context)
        loadAlerts(context)
        syncSystemCellBroadcasts(context)
    }

    fun getDeviceCountryIso(context: Context): String {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val netCountry = tm?.networkCountryIso?.trim()?.uppercase(Locale.ROOT)
        val simCountry = tm?.simCountryIso?.trim()?.uppercase(Locale.ROOT)
        return when {
            !netCountry.isNullOrBlank() -> netCountry
            !simCountry.isNullOrBlank() -> simCountry
            else -> Locale.getDefault().country.takeIf { it.isNotBlank() } ?: "RS"
        }
    }

    fun getCountryDisplayName(countryIso: String, isSerbian: Boolean): String {
        val clean = countryIso.trim().uppercase(Locale.ROOT)
        if (clean == "GLOBAL") {
            return if (isSerbian) "Globalno (Ceo svet)" else "Global (Worldwide)"
        }
        return when (clean) {
            "RS" -> if (isSerbian) "Srbija (RS)" else "Serbia (RS)"
            "ME" -> if (isSerbian) "Crna Gora (ME)" else "Montenegro (ME)"
            "BA" -> if (isSerbian) "Bosna i Hercegovina (BA)" else "Bosnia and Herzegovina (BA)"
            "HR" -> if (isSerbian) "Hrvatska (HR)" else "Croatia (HR)"
            "SI" -> if (isSerbian) "Slovenija (SI)" else "Slovenia (SI)"
            "MK" -> if (isSerbian) "Severna Makedonija (MK)" else "North Macedonia (MK)"
            "DE" -> if (isSerbian) "Nemačka (DE)" else "Germany (DE)"
            "AT" -> if (isSerbian) "Austrija (AT)" else "Austria (AT)"
            "CH" -> if (isSerbian) "Švajcarska (CH)" else "Switzerland (CH)"
            "US" -> if (isSerbian) "Sjedinjene Američke Države (US)" else "United States (US)"
            else -> {
                try {
                    val loc = Locale("", clean)
                    val disp = loc.getDisplayCountry(if (isSerbian) Locale.forLanguageTag("sr-RS") else Locale.ENGLISH)
                    if (disp.isNotBlank()) "$disp ($clean)" else clean
                } catch (_: Exception) {
                    clean
                }
            }
        }
    }

    fun isGlobalCatastropheText(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        val globalKeywords = listOf(
            "global catastrophe", "global disaster", "globalna katastrofa", "globalna opasnost",
            "tsunami", "radijacija", "nuklearna", "svetska", "planetary", "najviši nivo uzbune",
            "catastrophic", "međunarodna uzbuna", "nacionalna vanredna situacija", "predsedničko upozorenje",
            "presidential alert", "national emergency", "state of emergency"
        )
        return globalKeywords.any { lower.contains(it) }
    }

    /**
     * Determines whether an emergency alert should be accepted by the device:
     * Rule: Only take alerts from the country where the phone is currently located,
     * OR if it is a global catastrophe.
     */
    fun isAlertRelevant(context: Context, alert: EmergencyAlert): Boolean {
        // Global catastrophe check (always relevant worldwide)
        if (alert.severity.isGlobal ||
            alert.countryIso.equals("GLOBAL", ignoreCase = true) ||
            isGlobalCatastropheText(alert.title + " " + alert.body)
        ) {
            return true
        }

        // Category filter checks
        when (alert.severity) {
            EmergencySeverity.EXTREME -> if (!isExtremeAlertsEnabled(context)) return false
            EmergencySeverity.SEVERE -> if (!isSevereAlertsEnabled(context)) return false
            EmergencySeverity.AMBER_ALERT -> if (!isAmberAlertsEnabled(context)) return false
            EmergencySeverity.TEST, EmergencySeverity.INFO -> Unit
            else -> Unit
        }

        // Country match check: must match phone's current country location
        val deviceCountry = getDeviceCountryIso(context)
        return alert.countryIso.equals(deviceCountry, ignoreCase = true)
    }

    fun isExtremeAlertsEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_EXTREME_ENABLED, true)
    }

    fun setExtremeAlertsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_EXTREME_ENABLED, enabled)
        }
    }

    fun isSevereAlertsEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SEVERE_ENABLED, true)
    }

    fun setSevereAlertsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_SEVERE_ENABLED, enabled)
        }
    }

    fun isAmberAlertsEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_AMBER_ENABLED, true)
    }

    fun setAmberAlertsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_AMBER_ENABLED, enabled)
        }
    }

    fun isSoundAlertsEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SOUND_ENABLED, true)
    }

    fun setSoundAlertsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_SOUND_ENABLED, enabled)
        }
    }

    fun processIncomingAlert(context: Context, alert: EmergencyAlert): Boolean {
        initialize(context)
        if (!isAlertRelevant(context, alert)) {
            Log.i(TAG, "Ignored alert for foreign country '${alert.countryIso}' (device is in '${getDeviceCountryIso(context)}')")
            return false
        }

        val current = _alerts.value.toMutableList()
        if (current.none { it.id == alert.id || (it.title == alert.title && it.timestampMillis == alert.timestampMillis) }) {
            current.add(0, alert)
            _alerts.value = current
            saveAlerts(context, current)
            dispatchAlertNotification(context, alert)
            return true
        }
        return false
    }

    fun triggerTestAlert(context: Context): EmergencyAlert {
        initialize(context)
        val isSr = TxturaStrings.isSerbianLocale()
        val deviceCountry = getDeviceCountryIso(context)
        val alert = EmergencyAlert(
            id = System.currentTimeMillis(),
            title = if (isSr) "TEST: Hitno bežično upozorenje" else "TEST: Wireless Emergency Alert",
            body = if (isSr) {
                "Ovo je probno hitno upozorenje mobilne mreže za teritoriju (${getCountryDisplayName(deviceCountry, true)}). Nije potrebna nikakva akcija. Txtura sistem bezbednosti je aktivan."
            } else {
                "This is a test wireless emergency alert for (${getCountryDisplayName(deviceCountry, false)}). No action is required. Txtura emergency system is active."
            },
            severity = EmergencySeverity.TEST,
            timestampMillis = System.currentTimeMillis(),
            countryIso = deviceCountry,
            scope = if (isSr) "Nacionalno testiranje" else "National Test",
            isRead = false
        )
        processIncomingAlert(context, alert)
        return alert
    }

    fun triggerGlobalCatastropheSample(context: Context): EmergencyAlert {
        initialize(context)
        val isSr = TxturaStrings.isSerbianLocale()
        val alert = EmergencyAlert(
            id = System.currentTimeMillis() + 1,
            title = if (isSr) "GLOBALNA KATASTROFA: Međunarodna uzbuna" else "GLOBAL CATASTROPHE: Worldwide Alert",
            body = if (isSr) {
                "Najviši nivo uzbune. Globalna katastrofa aktivirana. Primaju se upozorenja bez obzira na lokaciju uređaja."
            } else {
                "Highest alert level. Global catastrophe active. Dispatched globally across all regions."
            },
            severity = EmergencySeverity.GLOBAL_CATASTROPHE,
            timestampMillis = System.currentTimeMillis(),
            countryIso = "GLOBAL",
            scope = if (isSr) "Globalna katastrofa" else "Global Catastrophe",
            isRead = false
        )
        processIncomingAlert(context, alert)
        return alert
    }

    fun openSystemEmergencyAlertSettings(context: Context) {
        val intents = listOf(
            Intent("android.provider.Telephony.ACTION_MANAGE_EMERGENCY_ALERTS"),
            Intent("android.settings.EMERGENCY_BROADCAST_SETTINGS"),
            Intent(Settings.ACTION_WIRELESS_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )
        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (_: Exception) { }
        }
    }

    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                CHANNEL_ID_EMERGENCY,
                "Txtura Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Wireless emergency alerts and civil protection"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 1000)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun dispatchAlertNotification(context: Context, alert: EmergencyAlert) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val openIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val pi = android.app.PendingIntent.getActivity(
                context,
                alert.id.toInt(),
                openIntent,
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID_EMERGENCY)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(alert.title)
                .setContentText(alert.body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(alert.body))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pi)

            if (!isSoundAlertsEnabled(context)) {
                builder.setSilent(true)
            }

            nm.notify(alert.id.toInt(), builder.build())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch alert notification", e)
        }
    }

    private fun syncSystemCellBroadcasts(context: Context) {
        try {
            val uri = Uri.parse("content://cellbroadcasts")
            val cursor = context.contentResolver.query(uri, null, null, null, "date DESC")
            cursor?.use { c ->
                val bodyCol = c.getColumnIndex("body").takeIf { it >= 0 }
                    ?: c.getColumnIndex("message").takeIf { it >= 0 }
                val dateCol = c.getColumnIndex("date").takeIf { it >= 0 }
                val subCol = c.getColumnIndex("sub_id").takeIf { it >= 0 }

                var count = 0
                while (c.moveToNext() && count < 10) {
                    val body = if (bodyCol != null) c.getString(bodyCol).orEmpty() else ""
                    val date = if (dateCol != null) c.getLong(dateCol) else System.currentTimeMillis()
                    if (body.isNotBlank()) {
                        val isGlobal = isGlobalCatastropheText(body)
                        val alert = EmergencyAlert(
                            id = date,
                            title = if (isGlobal) "Globalna katastrofa / Uzbuna" else "Hitno upozorenje mobilne mreže",
                            body = body,
                            severity = if (isGlobal) EmergencySeverity.GLOBAL_CATASTROPHE else EmergencySeverity.EXTREME,
                            timestampMillis = date,
                            countryIso = if (isGlobal) "GLOBAL" else getDeviceCountryIso(context),
                            scope = if (isGlobal) "Globalno" else "Nacionalno",
                            isRead = true
                        )
                        if (isAlertRelevant(context, alert)) {
                            val current = _alerts.value.toMutableList()
                            if (current.none { it.id == alert.id }) {
                                current.add(alert)
                                _alerts.value = current
                            }
                        }
                    }
                    count++
                }
            }
        } catch (_: Exception) {
            // content://cellbroadcasts is vendor-dependent; fallback to local persistence
        }
    }

    private fun loadAlerts(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_SAVED_ALERTS_JSON, null)
        if (raw.isNullOrBlank()) {
            // Seed a helpful initial informational alert about active emergency protection for the device country
            val isSr = TxturaStrings.isSerbianLocale()
            val country = getDeviceCountryIso(context)
            val initial = EmergencyAlert(
                id = 1001L,
                title = if (isSr) "Sistem za hitna upozorenja aktivan" else "Wireless Emergency Alerts Active",
                body = if (isSr) {
                    "Praćenje hitnih bežičnih upozorenja je aktivno za zonu: ${getCountryDisplayName(country, true)}. Primaju se samo alarmi za zemlju u kojoj se nalazi telefon i globalne katastrofe."
                } else {
                    "Wireless emergency alert monitoring active for: ${getCountryDisplayName(country, false)}. Only alerts from the phone's current country and global catastrophes are received."
                },
                severity = EmergencySeverity.INFO,
                timestampMillis = System.currentTimeMillis() - 86400000L,
                countryIso = country,
                scope = if (isSr) "Sistem Txtura" else "Txtura System",
                isRead = true
            )
            _alerts.value = listOf(initial)
            saveAlerts(context, listOf(initial))
            return
        }

        try {
            val arr = JSONArray(raw)
            val list = mutableListOf<EmergencyAlert>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val alert = EmergencyAlert(
                    id = obj.optLong("id"),
                    title = obj.optString("title"),
                    body = obj.optString("body"),
                    severity = try {
                        EmergencySeverity.valueOf(obj.optString("severity", "EXTREME"))
                    } catch (_: Exception) {
                        EmergencySeverity.EXTREME
                    },
                    timestampMillis = obj.optLong("timestampMillis"),
                    countryIso = obj.optString("countryIso", getDeviceCountryIso(context)),
                    scope = obj.optString("scope", "Nacionalno"),
                    isRead = obj.optBoolean("isRead", false)
                )
                if (isAlertRelevant(context, alert)) {
                    list.add(alert)
                }
            }
            _alerts.value = list
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse saved emergency alerts", e)
        }
    }

    private fun saveAlerts(context: Context, list: List<EmergencyAlert>) {
        try {
            val arr = JSONArray()
            for (item in list.take(50)) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("body", item.body)
                    put("severity", item.severity.name)
                    put("timestampMillis", item.timestampMillis)
                    put("countryIso", item.countryIso)
                    put("scope", item.scope)
                    put("isRead", item.isRead)
                }
                arr.put(obj)
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                putString(KEY_SAVED_ALERTS_JSON, arr.toString())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist emergency alerts", e)
        }
    }
}
