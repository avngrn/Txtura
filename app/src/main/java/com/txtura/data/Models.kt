@file:Suppress("SpellCheckingInspection", "DestructuringDeclaration", "TrailingComma")

package com.txtura.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.txtura.MainActivity
import com.txtura.R
import com.txtura.ui.i18n.TxturaStrings
import com.txtura.security.AppSecurityManager
import com.txtura.security.MessageSecurityAnalyzer
import com.txtura.security.SecurityAnalysisResult
import com.txtura.security.SecurityCryptoManager
import com.txtura.security.SecurityRiskLevel
import com.txtura.ui.theme.TxturaThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.minutes
import org.json.JSONArray
import org.json.JSONObject
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MessageType {
    SMS,
    MMS
}

enum class DeliveryStatus {
    SENDING,
    SENT,
    DELIVERED,
    FAILED,
    PENDING,
    DOWNLOADING,
    FAILED_NO_DATA
}

enum class MmsAutoRetrieveMode {
    NOT_ROAMING,
    ALWAYS,
    DISABLED
}

enum class MmsDataDisabledMode {
    NOT_ROAMING,
    ALWAYS,
    NEVER
}

enum class MmsCreationMode {
    FREE,
    WARNING,
    RESTRICTED
}

data class MmsApnSettings(
    val carrierName: String = "",
    val mmscUrl: String = "",
    val mmsProxy: String = "",
    val mmsPort: Int = 8080,
    val source: String = "Automatski (SMSC)"
)

/**
 * Curated custom notification tones for incoming SMS/MMS messages.
 */
enum class SmsNotificationSound(val id: String, val rawResName: String?) {
    SYSTEM("system", null),
    DUPLI_PULS("dupli_puls", "sms_dupli_puls"),
    KRISTALNI_ZVONCIC("kristalni_zvoncic", "sms_kristalni_zvoncic"),
    MEKI_POP("meki_pop", "sms_meki_pop"),
    RITAM_KOD("ritam_kod", "sms_ritam_kod"),
    TOPLA_KALIMBA("topla_kalimba", "sms_topla_kalimba"),
    DISKRETNI_OBLAK("diskretni_oblak", "sms_diskretni_oblak");

    val rawResId: Int?
        get() = when (this) {
            SYSTEM -> null
            DUPLI_PULS -> R.raw.sms_dupli_puls
            KRISTALNI_ZVONCIC -> R.raw.sms_kristalni_zvoncic
            MEKI_POP -> R.raw.sms_meki_pop
            RITAM_KOD -> R.raw.sms_ritam_kod
            TOPLA_KALIMBA -> R.raw.sms_topla_kalimba
            DISKRETNI_OBLAK -> R.raw.sms_diskretni_oblak
        }

    fun getDisplayName(isSerbian: Boolean): String = when (this) {
        SYSTEM -> if (isSerbian) "Sistemski ton" else "System tone"
        DUPLI_PULS -> if (isSerbian) "Dupli puls" else "Double Pulse"
        KRISTALNI_ZVONCIC -> if (isSerbian) "Kristalni zvončić" else "Crystal Chime"
        MEKI_POP -> if (isSerbian) "Meki pop" else "Soft Pop"
        RITAM_KOD -> if (isSerbian) "Ritam kod" else "Rhythm Code"
        TOPLA_KALIMBA -> if (isSerbian) "Topla kalimba" else "Warm Kalimba"
        DISKRETNI_OBLAK -> if (isSerbian) "Diskretni oblak" else "Discreet Cloud"
    }

    companion object {
        fun fromId(id: String?): SmsNotificationSound =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SYSTEM
    }
}

/**
 * Phone ringer mode condition under which light feedback (flash alert) is triggered.
 */
enum class FlashAlertMode(val id: String) {
    ALWAYS("always"),
    VIBRATE_ONLY("vibrate_only"),
    SILENT_ONLY("silent_only"),
    SOUND_ONLY("sound_only");

    fun getDisplayName(isSerbian: Boolean): String = when (this) {
        ALWAYS -> if (isSerbian) "Uvek" else "Always"
        VIBRATE_ONLY -> if (isSerbian) "Kad je telefon u režimu samo vibracija" else "When phone is in vibrate mode"
        SILENT_ONLY -> if (isSerbian) "Kad je telefon u režimu nečujno" else "When phone is in silent mode"
        SOUND_ONLY -> if (isSerbian) "Kad je telefon u režimu zvuk" else "When phone is in sound mode"
    }

    fun getShortDisplayName(isSerbian: Boolean): String = when (this) {
        ALWAYS -> if (isSerbian) "Uvek" else "Always"
        VIBRATE_ONLY -> if (isSerbian) "Samo vibracija" else "Vibrate only"
        SILENT_ONLY -> if (isSerbian) "Nečujno" else "Silent"
        SOUND_ONLY -> if (isSerbian) "Zvuk" else "Sound"
    }

    fun matchesRingerMode(ringerMode: Int): Boolean = when (this) {
        ALWAYS -> true
        VIBRATE_ONLY -> ringerMode == android.media.AudioManager.RINGER_MODE_VIBRATE
        SILENT_ONLY -> ringerMode == android.media.AudioManager.RINGER_MODE_SILENT
        SOUND_ONLY -> ringerMode == android.media.AudioManager.RINGER_MODE_NORMAL
    }

    companion object {
        fun fromId(id: String?): FlashAlertMode =
            entries.firstOrNull {
                it.id.equals(id, ignoreCase = true) || it.name.equals(id, ignoreCase = true)
            } ?: ALWAYS
    }
}

data class SimCardMessage(
    val id: Long,
    val address: String,
    val body: String,
    val timestamp: Long,
)

enum class ConversationFilter {
    PEOPLE,
    SERVICES,
    UNREAD,
    ALL;

    companion object {
        val defaultOrder: List<ConversationFilter> = listOf(
            ALL,
            PEOPLE,
            SERVICES,
            UNREAD,
        )

        fun deserializeOrder(raw: String?): List<ConversationFilter> {
            if (raw.isNullOrBlank() || raw == "PEOPLE,SERVICES,UNREAD,ALL") return defaultOrder
            val parsed = raw.splitToSequence(",")
                .mapNotNull { name -> entries.find { it.name == name.trim() } }
                .distinct()
                .toList()
            val missing = defaultOrder.filter { it !in parsed }
            return if (parsed.isEmpty()) defaultOrder else (parsed + missing)
        }

        fun serializeOrder(order: List<ConversationFilter>): String {
            return order.asSequence().distinct().joinToString(",") { it.name }
        }
    }
}

enum class DockBarItem {
    MARK_ALL_READ,
    HIGHLIGHTED,
    NEW_MESSAGE,
    DELETED,
    SETTINGS;

    companion object {
        val defaultOrder: List<DockBarItem> = listOf(
            MARK_ALL_READ,
            HIGHLIGHTED,
            NEW_MESSAGE,
            DELETED,
            SETTINGS,
        )

        fun deserializeOrder(raw: String?): List<DockBarItem> {
            if (raw.isNullOrBlank()) return defaultOrder
            val parsed = raw.splitToSequence(",")
                .mapNotNull { name -> entries.find { it.name == name.trim() } }
                .distinct()
                .toList()
            val missing = defaultOrder.filter { it !in parsed }
            return if (parsed.isEmpty()) defaultOrder else (parsed + missing)
        }

        fun serializeOrder(order: List<DockBarItem>): String {
            return order.asSequence().distinct().joinToString(",") { it.name }
        }
    }
}

enum class UiColorMode {
    COLORFUL,
    MONOCHROME,
    MINIMALIST
}

/**
 * Display density / layout mode for the main conversation list window.
 */
enum class MainListLayoutMode {
    DEFAULT,
    COMPACT
}

/**
 * Configurable action executed when the user swipes a conversation card left or right
 * on the main screen.
 */
enum class SwipeAction {
    PIN,
    DELETE,
    CALL,
    MARK_READ,
    NONE
}

/**
 * Configurable action executed when the user swipes a message bubble left or right
 * inside a conversation.
 */
enum class MessageSwipeAction {
    PIN,
    HIGHLIGHT,
    DELETE,
    NONE
}

/**
 * Grouping and sorting mode for the Deleted Messages (Trash) folder.
 */
enum class DeletedGroupingMode {
    BY_DATE,
    BY_CONTACT
}

/**
 * Tactile background textures inspired by the "Txtura" (tekstura) identity.
 * Each UI theme ([WallpaperPreset]) has its own signature matched texture ([WallpaperPreset.defaultTexture]),
 * and the user can either keep [AUTO_THEME] or pick any texture style for any theme.
 */
enum class WallpaperTexture {
    AUTO_THEME,
    SILK_WEAVE,
    AURORA_RIBBONS,
    VELVET_NAP,
    OCEAN_CAUSTICS,
    SAND_DUNES,
    ORGANIC_BOTANICAL,
    CRYSTAL_FACETS,
    CONSTELLATION_MESH,
    DAMASK_JACQUARD,
    FROST_CRYSTALS,
    KINTSUGI_MARBLE,
    SLATE_STRATA,
    BRUSHED_METAL,
    LINEN_CANVAS,
    FLUTED_GLASS,
    SUEDE_MICROFIBER,
    CARBON_TWILL,
    COTTON_PAPER,
    HERRINGBONE_TWILL,
    TOPOGRAPHIC_CONTOUR,
    PEBBLED_LEATHER,
    LAVENDER_PETAL,
    COFFEE_BEANS,
    NONE
}

/**
 * Curated built-in backgrounds (10 colorful presets + 10 monochrome presets + Default theme).
 * Every preset includes a bespoke signature texture ([defaultTexture]) that matches its color palette.
 */
enum class WallpaperPreset(
    val baseColorHex: Long,
    val primaryOrbHex: Long,
    val secondaryOrbHex: Long,
    val accentOrbHex: Long,
    val isMonochrome: Boolean = false,
    val defaultTexture: WallpaperTexture = WallpaperTexture.SILK_WEAVE,
    val isMinimalist: Boolean = false,
) {
    // Curated Colorful Presets + Default (with bespoke matching textures)
    DEFAULT(0xFF070A12, 0xFF1D4ED8, 0xFF4338CA, 0xFF38BDF8, false, WallpaperTexture.SILK_WEAVE),
    AURORA_BOREALIS(0xFF041416, 0xFF0D9488, 0xFF0284C7, 0xFF14B8A6, false, WallpaperTexture.AURORA_RIBBONS),
    VELVET_PLUM(0xFF120616, 0xFF7E22CE, 0xFF9333EA, 0xFFA855F7, false, WallpaperTexture.VELVET_NAP),
    MIDNIGHT_OCEAN(0xFF030D1A, 0xFF1D4ED8, 0xFF0284C7, 0xFF38BDF8, false, WallpaperTexture.OCEAN_CAUSTICS),
    SUNSET_HORIZON(0xFF180A0B, 0xFFBE123C, 0xFFC2410C, 0xFFF97316, false, WallpaperTexture.SAND_DUNES),
    EMERALD_FOREST(0xFF03140C, 0xFF047857, 0xFF059669, 0xFF10B981, false, WallpaperTexture.ORGANIC_BOTANICAL),
    ROYAL_AMETHYST(0xFF100618, 0xFF6D28D9, 0xFF7C3AED, 0xFF8B5CF6, false, WallpaperTexture.CRYSTAL_FACETS),
    STARLIGHT_GALAXY(0xFF040816, 0xFF2563EB, 0xFF475569, 0xFF60A5FA, false, WallpaperTexture.CONSTELLATION_MESH),
    CRIMSON_VELVET(0xFF17050A, 0xFF9F1239, 0xFFBE123C, 0xFFE11D48, false, WallpaperTexture.DAMASK_JACQUARD),
    BURGUNDY_ANITA(0xFF15040A, 0xFF881337, 0xFFBE185D, 0xFFF43F5E, false, WallpaperTexture.VELVET_NAP),
    ARCTIC_FROST(0xFF05131C, 0xFF0284C7, 0xFF0369A1, 0xFF38BDF8, false, WallpaperTexture.FROST_CRYSTALS),
    OBSIDIAN_GOLD(0xFF110C04, 0xFFB45309, 0xFF92400E, 0xFFD97706, false, WallpaperTexture.KINTSUGI_MARBLE),
    LAVENDER_DREAM(0xFF0F0C1B, 0xFF7C3AED, 0xFF8B5CF6, 0xFFA78BFA, false, WallpaperTexture.LAVENDER_PETAL),
    ESPRESSO_CREAM(0xFF120A06, 0xFF4A2810, 0xFF783D19, 0xFFD7B999, false, WallpaperTexture.COFFEE_BEANS),

    // Refined Monochrome Presets (Light/Dark base + 1 distinct accent color + bespoke tactile texture)
    MONO_SLATE(0xFF0B0F17, 0xFF334155, 0xFF475569, 0xFF64748B, true, WallpaperTexture.SLATE_STRATA),
    MONO_SAPPHIRE(0xFF050B18, 0xFF1E3A8A, 0xFF1D4ED8, 0xFF2563EB, true, WallpaperTexture.BRUSHED_METAL),
    MONO_SAGE(0xFF06120D, 0xFF14532D, 0xFF166534, 0xFF15803D, true, WallpaperTexture.LINEN_CANVAS),
    MONO_CHAMPAGNE(0xFF131008, 0xFF785E2F, 0xFF8C713B, 0xFFA1824A, true, WallpaperTexture.FLUTED_GLASS),
    MONO_PLUM(0xFF140714, 0xFF581C87, 0xFF6B21A8, 0xFF7E22CE, true, WallpaperTexture.SUEDE_MICROFIBER),
    MONO_GRAPHITE(0xFF0A0A0A, 0xFF262626, 0xFF404040, 0xFF525252, true, WallpaperTexture.CARBON_TWILL),
    MONO_BURGUNDY(0xFF140508, 0xFF6B0E23, 0xFF80102A, 0xFF991B38, true, WallpaperTexture.DAMASK_JACQUARD),
    MONO_NAVY(0xFF040915, 0xFF172554, 0xFF1E3A8A, 0xFF1E40AF, true, WallpaperTexture.HERRINGBONE_TWILL),
    MONO_SMOKE(0xFF0F0E0E, 0xFF0D9488, 0xFF0F766E, 0xFF14B8A6, true, WallpaperTexture.TOPOGRAPHIC_CONTOUR),
    MONO_ESPRESSO(0xFF100804, 0xFF5C2D12, 0xFF7C3E1D, 0xFFA05A2C, true, WallpaperTexture.COFFEE_BEANS),
    MONO_LAVENDER(0xFF0C0816, 0xFF6D28D9, 0xFF7C3AED, 0xFFA78BFA, true, WallpaperTexture.LAVENDER_PETAL),

    // 2 Minimalist Themes (True AMOLED pitch-black & Pure clean white, zero decorative accent colors)
    ALL_BLACK(0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000, isMonochrome = true, defaultTexture = WallpaperTexture.NONE, isMinimalist = true),
    ALL_WHITE(0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, isMonochrome = true, defaultTexture = WallpaperTexture.NONE, isMinimalist = true);

    companion object {
        val colorfulPresets: List<WallpaperPreset>
            get() = entries.filter { !it.isMonochrome && !it.isMinimalist }

        val monochromePresets: List<WallpaperPreset>
            get() = entries.filter { it.isMonochrome && !it.isMinimalist }

        val minimalistPresets: List<WallpaperPreset>
            get() = entries.filter { it.isMinimalist }
    }
}

/**
 * Represents a chosen wallpaper (either one of the built-in presets, Default, or a custom gallery image URI)
 * together with its tactile surface texture ([texture]).
 */
data class WallpaperConfig(
    val preset: WallpaperPreset = WallpaperPreset.STARLIGHT_GALAXY,
    val customImageUri: String? = null,
    val texture: WallpaperTexture = WallpaperTexture.AUTO_THEME,
    val customAccentColorHex: Long? = null,
) {
    val isCustomGalleryImage: Boolean
        get() = !customImageUri.isNullOrBlank()

    val resolvedTexture: WallpaperTexture
        get() = when {
            isCustomGalleryImage && customAccentColorHex != null -> WallpaperTexture.NONE
            texture == WallpaperTexture.AUTO_THEME -> preset.defaultTexture
            else -> texture
        }

    fun serialize(): String {
        val base = if (!customImageUri.isNullOrBlank()) {
            "custom:$customImageUri"
        } else {
            "preset:${preset.name}"
        }
        val withTex = if (texture == WallpaperTexture.AUTO_THEME) {
            base
        } else {
            "$base|tex:${texture.name}"
        }
        return if (customAccentColorHex != null) {
            "$withTex|acc:${customAccentColorHex.toString(16)}"
        } else {
            withTex
        }
    }

    companion object {
        fun deserialize(raw: String?): WallpaperConfig {
            if (raw.isNullOrBlank()) return WallpaperConfig()
            var accHex: Long? = null
            var textureName: String? = null
            var basePart = raw

            if (basePart.contains("|acc:")) {
                val accSplit = basePart.split("|acc:", limit = 2)
                basePart = accSplit[0]
                accHex = accSplit.getOrNull(1)?.trim()?.toLongOrNull(16)
            }
            if (basePart.contains("|tex:")) {
                val texSplit = basePart.split("|tex:", limit = 2)
                basePart = texSplit[0]
                textureName = texSplit.getOrNull(1)?.trim()
            }

            val parsedTexture = textureName?.let { tex ->
                WallpaperTexture.entries.find { it.name == tex }
            } ?: WallpaperTexture.AUTO_THEME

            return when {
                basePart.startsWith("custom:") -> {
                    val uriStr = basePart.removePrefix("custom:").takeIf { it.isNotBlank() }
                    if (uriStr != null) {
                        WallpaperConfig(
                            preset = WallpaperPreset.DEFAULT,
                            customImageUri = uriStr,
                            texture = parsedTexture,
                            customAccentColorHex = accHex,
                        )
                    } else {
                        WallpaperConfig(texture = parsedTexture, customAccentColorHex = accHex)
                    }
                }
                basePart.startsWith("preset:") -> {
                    val name = basePart.removePrefix("preset:")
                    val targetName = if (name == "CYBER_NEBULA") "VELVET_PLUM" else name
                    val found = WallpaperPreset.entries.find { it.name == targetName } ?: WallpaperPreset.DEFAULT
                    WallpaperConfig(
                        preset = found,
                        customImageUri = null,
                        texture = parsedTexture,
                        customAccentColorHex = accHex,
                    )
                }
                else -> WallpaperConfig()
            }
        }
    }
}

@Immutable
data class MmsAttachment(
    val uri: Uri,
    val mimeType: String = "image/jpeg",
    val caption: String = "MMS Photo",
    val durationMs: Long = 0L
) {
    val isAudio: Boolean get() = mimeType.startsWith("audio/", ignoreCase = true) ||
        uri.path?.endsWith(".amr", ignoreCase = true) == true ||
        uri.path?.endsWith(".3gp", ignoreCase = true) == true ||
        uri.path?.endsWith(".aac", ignoreCase = true) == true ||
        uri.path?.endsWith(".m4a", ignoreCase = true) == true ||
        caption.endsWith(".amr", ignoreCase = true)
    val isImage: Boolean get() = mimeType.startsWith("image/", ignoreCase = true)
    val isCalendar: Boolean get() = mimeType.contains("calendar", ignoreCase = true) ||
        uri.path?.endsWith(".ics", ignoreCase = true) == true ||
        uri.path?.endsWith(".vcs", ignoreCase = true) == true ||
        caption.endsWith(".ics", ignoreCase = true) ||
        caption.endsWith(".vcs", ignoreCase = true)
    val isDocument: Boolean get() = !isAudio && !isImage && !isCalendar
}

@Immutable
data class ChatMessage(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val timestampMillis: Long,
    val isIncoming: Boolean,
    val type: MessageType = MessageType.SMS,
    val status: DeliveryStatus = DeliveryStatus.DELIVERED,
    val isPinned: Boolean = false,
    val isHighlighted: Boolean = false,
    val attachment: MmsAttachment? = null,
    val securityAnalysis: SecurityAnalysisResult = SecurityAnalysisResult(
        riskLevel = SecurityRiskLevel.SAFE,
        reasons = emptyList(),
        extractedOtpCode = null
    )
)

fun deduplicateMessageList(messages: List<ChatMessage>): List<ChatMessage> {
    if (messages.size <= 1) return messages
    val result = ArrayList<ChatMessage>(messages.size)
    val seenIds = HashSet<Long>()
    for (msg in messages) {
        if (msg.id > 0L && !seenIds.add(msg.id)) {
            continue
        }
        val isDuplicate = result.any { existing ->
            existing.id == msg.id ||
                (existing.isIncoming == msg.isIncoming &&
                    existing.body == msg.body &&
                    kotlin.math.abs(existing.timestampMillis - msg.timestampMillis) < 3000L)
        }
        if (!isDuplicate) {
            result.add(msg)
        }
    }
    return result
}

@Immutable
data class ConversationThread(
    val id: Long,
    val contactName: String,
    val address: String,
    val avatarInitials: String,
    val avatarAccentColorHex: Long,
    val contactPhotoUri: Uri? = null,
    val messages: List<ChatMessage>,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isService: Boolean = false,
    val draftText: String? = null,
    val isHistoryLoaded: Boolean = true
) {
    val lastMessage: ChatMessage? = messages.maxByOrNull(ChatMessage::timestampMillis)

    val pinnedMessages: List<ChatMessage> = messages.filter(ChatMessage::isPinned)

    val highestRiskLevel: SecurityRiskLevel =
        messages.maxOfOrNull { (_, _, _, _, _, _, _, _, _, _, _, securityAnalysis) ->
            securityAnalysis.riskLevel
        } ?: SecurityRiskLevel.SAFE
}

data class ContactPickerItem(
    val name: String,
    val address: String,
    val initials: String,
    val photoUri: Uri? = null,
    val existingThreadId: Long? = null
)

/**
 * Represents a conversation moved to the Deleted Messages (Trash) folder,
 * retained for a configurable number of days (default 30 days) before automatic permanent deletion.
 */
data class DeletedConversationEntry(
    val thread: ConversationThread,
    val deletedAtMillis: Long
) {
    fun daysRemaining(retentionDays: Int, nowMillis: Long = System.currentTimeMillis()): Int {
        val elapsedMillis = (nowMillis - deletedAtMillis).coerceAtLeast(0L)
        val elapsedDays = (elapsedMillis / 86_400_000L).toInt()
        return (retentionDays - elapsedDays).coerceAtLeast(0)
    }
}

/**
 * Represents an outgoing SMS/MMS message scheduled to be sent at a future timestamp.
 */
data class ScheduledSmsMessage(
    val id: Long,
    val threadId: Long,
    val recipientAddress: String,
    val body: String,
    val attachmentUri: Uri? = null,
    val scheduledAtMillis: Long,
)


data class AppSettingsSnapshot(
    val themeMode: TxturaThemeMode,
    val showContactPhotos: Boolean,
    val listLayoutMode: MainListLayoutMode,
    val mainWallpaper: WallpaperConfig,
    val contactWallpapers: Map<String, WallpaperConfig>,
    val swipeRightAction: SwipeAction,
    val swipeLeftAction: SwipeAction,
    val autoDeleteDays: Int,
    val deletedGroupingMode: DeletedGroupingMode,
    val bubbleZoomScale: Float,
    val uiColorMode: UiColorMode = UiColorMode.COLORFUL,
    val uiOpacity: Float = 0.50f,
    val categoryOrder: List<ConversationFilter> = ConversationFilter.defaultOrder,
    val dockBarOrder: List<DockBarItem> = DockBarItem.defaultOrder,
    val recallMessagesEnabled: Boolean = true,
    val identifyMaliciousWebsites: Boolean = true,
    val inAppSoundsEnabled: Boolean = true,
    val pinchToZoomEnabled: Boolean = true,
    val expressiveAnimationsEnabled: Boolean = true,
    val flashAlertEnabled: Boolean = true,
    val flashAlertMode: FlashAlertMode = FlashAlertMode.ALWAYS,
    val hapticsEnabled: Boolean = true,
    val bubblesEnabled: Boolean = false,
    val autoPreviewsShowAll: Boolean = true,
    val autoPreviewsWebLinksOnly: Boolean = false,
    val autoPreviewsWifiOnly: Boolean = false,
    val deliveryReportsEnabled: Boolean = true,
    val mmsRequestReadReports: Boolean = false,
    val mmsSendReadReports: Boolean = false,
    val mmsAutoRetrieve: MmsAutoRetrieveMode = MmsAutoRetrieveMode.NOT_ROAMING,
    val mmsDataDisabledMode: MmsDataDisabledMode = MmsDataDisabledMode.NOT_ROAMING,
    val mmsCreationMode: MmsCreationMode = MmsCreationMode.RESTRICTED,
    val smscNumber: String = "",
    val mmsApnCarrier: String = "",
    val mmsApnMmsc: String = "",
    val mmsApnProxy: String = "",
    val mmsApnPort: Int = 8080,
    val mmsApnSource: String = "",
)

data class BackupRestorePayload(
    val version: Int,
    val createdAtMillis: Long,
    val deviceModel: String,
    val settings: AppSettingsSnapshot,
    val pinnedAddresses: Set<String>,
    val pinnedMessageIds: Set<Long>,
    val highlightedMessageIds: Set<Long> = emptySet(),
    val blockedAddresses: Set<String> = emptySet(),
    val deletedThreadsMeta: Map<Long, Long>,
    val threads: List<ConversationThread>,
    val deletedThreads: List<DeletedConversationEntry>
) {
    val totalMessagesCount: Int
        get() = threads.sumOf { (_, _, _, _, _, _, messages) -> messages.size } +
            deletedThreads.sumOf { (thread, _) -> thread.messages.size }
}

object BackupManager {

    private const val PREFS_BACKUP = "txtura_backup_prefs"
    private const val KEY_LAST_BACKUP_TIME = "last_backup_timestamp"
    private const val GOOGLE_DRIVE_PACKAGE = "com.google.android.apps.docs"

    fun getLastBackupTimestamp(context: Context): Long {
        return context.getSharedPreferences(PREFS_BACKUP, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_BACKUP_TIME, 0L)
    }

    fun saveLastBackupTimestamp(context: Context, timestamp: Long) {
        context.getSharedPreferences(PREFS_BACKUP, Context.MODE_PRIVATE).edit {
            putLong(KEY_LAST_BACKUP_TIME, timestamp)
        }
    }

    /**
     * Checks if the Google Drive application is installed on the device.
     */
    fun isGoogleDriveInstalled(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    GOOGLE_DRIVE_PACKAGE,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(GOOGLE_DRIVE_PACKAGE, 0)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Generates a self-contained, human-readable JSON string containing all messages,
     * conversations, trash items, wallpapers, and user settings.
     */
    fun generateBackupJson(
        settings: AppSettingsSnapshot,
        threads: List<ConversationThread>,
        deletedThreads: List<DeletedConversationEntry>,
        pinnedAddresses: Set<String>,
        pinnedMessageIds: Set<Long>,
        highlightedMessageIds: Set<Long> = emptySet(),
        blockedAddresses: Set<String> = emptySet(),
        deletedThreadsMeta: Map<Long, Long>
    ): String {
        val root = JSONObject()
        root.put("version", 2)
        root.put("createdAtMillis", System.currentTimeMillis())
        root.put("deviceModel", "${Build.MANUFACTURER} ${Build.MODEL}".trim())

        // 1. Settings
        val settingsObj = JSONObject()
        settingsObj.put("themeMode", settings.themeMode.name)
        settingsObj.put("showContactPhotos", settings.showContactPhotos)
        settingsObj.put("listLayoutMode", settings.listLayoutMode.name)
        settingsObj.put("mainWallpaper", settings.mainWallpaper.serialize())
        settingsObj.put("swipeRightAction", settings.swipeRightAction.name)
        settingsObj.put("swipeLeftAction", settings.swipeLeftAction.name)
        settingsObj.put("autoDeleteDays", settings.autoDeleteDays)
        settingsObj.put("deletedGroupingMode", settings.deletedGroupingMode.name)
        settingsObj.put("bubbleZoomScale", settings.bubbleZoomScale.toDouble())
        settingsObj.put("uiColorMode", settings.uiColorMode.name)
        settingsObj.put("uiOpacity", settings.uiOpacity.toDouble())
        settingsObj.put("categoryOrder", ConversationFilter.serializeOrder(settings.categoryOrder))
        settingsObj.put("dockBarOrder", DockBarItem.serializeOrder(settings.dockBarOrder))
        settingsObj.put("recallMessagesEnabled", settings.recallMessagesEnabled)
        settingsObj.put("identifyMaliciousWebsites", settings.identifyMaliciousWebsites)
        settingsObj.put("inAppSoundsEnabled", settings.inAppSoundsEnabled)
        settingsObj.put("pinchToZoomEnabled", settings.pinchToZoomEnabled)
        settingsObj.put("expressiveAnimationsEnabled", settings.expressiveAnimationsEnabled)
        settingsObj.put("flashAlertEnabled", settings.flashAlertEnabled)
        settingsObj.put("flashAlertMode", settings.flashAlertMode.name)
        settingsObj.put("hapticsEnabled", settings.hapticsEnabled)
        settingsObj.put("bubblesEnabled", settings.bubblesEnabled)
        settingsObj.put("autoPreviewsShowAll", settings.autoPreviewsShowAll)
        settingsObj.put("autoPreviewsWebLinksOnly", settings.autoPreviewsWebLinksOnly)
        settingsObj.put("autoPreviewsWifiOnly", settings.autoPreviewsWifiOnly)

        val contactWpObj = JSONObject()
        settings.contactWallpapers.forEach { (addr, wp) ->
            contactWpObj.put(addr, wp.serialize())
        }
        settingsObj.put("contactWallpapers", contactWpObj)
        root.put("settings", settingsObj)

        // 2. Pins, Highlights, Blocked & Metadata
        val pinnedAddrsArray = JSONArray()
        pinnedAddresses.forEach { pinnedAddrsArray.put(it) }
        root.put("pinnedAddresses", pinnedAddrsArray)

        val pinnedMsgsArray = JSONArray()
        pinnedMessageIds.forEach { pinnedMsgsArray.put(it) }
        root.put("pinnedMessageIds", pinnedMsgsArray)

        val highlightedMsgsArray = JSONArray()
        highlightedMessageIds.forEach { highlightedMsgsArray.put(it) }
        root.put("highlightedMessageIds", highlightedMsgsArray)

        val blockedAddrsArray = JSONArray()
        blockedAddresses.forEach { blockedAddrsArray.put(it) }
        root.put("blockedAddresses", blockedAddrsArray)

        val deletedMetaObj = JSONObject()
        deletedThreadsMeta.forEach { (id, ts) ->
            deletedMetaObj.put(id.toString(), ts)
        }
        root.put("deletedThreadsMeta", deletedMetaObj)

        // 3. Active Threads
        val threadsArray = JSONArray()
        threads.forEach { thread ->
            threadsArray.put(serializeThread(thread))
        }
        root.put("threads", threadsArray)

        // 4. Deleted Threads (Trash)
        val deletedArray = JSONArray()
        deletedThreads.forEach { (thread, deletedAtMillis) ->
            val entryObj = JSONObject()
            entryObj.put("deletedAtMillis", deletedAtMillis)
            entryObj.put("thread", serializeThread(thread))
            deletedArray.put(entryObj)
        }
        root.put("deletedThreads", deletedArray)

        return root.toString(2)
    }

    private fun serializeThread(thread: ConversationThread): JSONObject {
        val obj = JSONObject()
        obj.put("id", thread.id)
        obj.put("contactName", thread.contactName)
        obj.put("address", thread.address)
        obj.put("avatarInitials", thread.avatarInitials)
        obj.put("avatarAccentColorHex", thread.avatarAccentColorHex)
        obj.put("unreadCount", thread.unreadCount)
        obj.put("isPinned", thread.isPinned)
        obj.put("isService", thread.isService)

        val msgsArray = JSONArray()
        thread.messages.forEach { msg ->
            val msgObj = JSONObject()
            msgObj.put("id", msg.id)
            msgObj.put("threadId", msg.threadId)
            msgObj.put("address", msg.address)
            msgObj.put("body", msg.body)
            msgObj.put("timestampMillis", msg.timestampMillis)
            msgObj.put("isIncoming", msg.isIncoming)
            msgObj.put("type", msg.type.name)
            msgObj.put("status", msg.status.name)
            msgObj.put("isPinned", msg.isPinned)
            msgObj.put("isHighlighted", msg.isHighlighted)
            msg.attachment?.let { att ->
                val attObj = JSONObject()
                attObj.put("uri", att.uri.toString())
                attObj.put("mimeType", att.mimeType)
                attObj.put("caption", att.caption)
                msgObj.put("attachment", attObj)
            }
            msgsArray.put(msgObj)
        }
        obj.put("messages", msgsArray)
        return obj
    }

    /**
     * Parses and validates a JSON backup payload.
     * Automatically handles decryption if the backup was encrypted with a hardware key or password.
     */
    fun parseBackupJson(rawString: String, password: String? = null): BackupRestorePayload {
        val jsonString = if (SecurityCryptoManager.isEncryptedBackup(rawString)) {
            SecurityCryptoManager.decryptBackup(rawString, password)
        } else {
            rawString
        }
        val root = JSONObject(jsonString)
        val version = root.optInt("version", 1)
        val createdAt = root.optLong("createdAtMillis", System.currentTimeMillis())
        val deviceModel = root.optString("deviceModel", "Unknown")

        // 1. Settings
        val settingsObj = root.optJSONObject("settings") ?: JSONObject()
        val themeModeStr = settingsObj.optString("themeMode", TxturaThemeMode.AMOLED.name)
        val themeMode = TxturaThemeMode.entries.find { it.name == themeModeStr } ?: TxturaThemeMode.AMOLED
        val showContactPhotos = settingsObj.optBoolean("showContactPhotos", true)
        val layoutModeStr = settingsObj.optString("listLayoutMode", MainListLayoutMode.DEFAULT.name)
        val layoutMode = MainListLayoutMode.entries.find { it.name == layoutModeStr } ?: MainListLayoutMode.DEFAULT
        val mainWpStr = settingsObj.optString("mainWallpaper", "")
        val mainWallpaper = WallpaperConfig.deserialize(mainWpStr)

        val rightActionStr = settingsObj.optString("swipeRightAction", SwipeAction.PIN.name)
        val rightAction = SwipeAction.entries.find { it.name == rightActionStr } ?: SwipeAction.PIN
        val leftActionStr = settingsObj.optString("swipeLeftAction", SwipeAction.DELETE.name)
        val leftAction = SwipeAction.entries.find { it.name == leftActionStr } ?: SwipeAction.DELETE

        val autoDeleteDays = settingsObj.optInt("autoDeleteDays", 30).coerceIn(1, 365)
        val groupingStr = settingsObj.optString("deletedGroupingMode", DeletedGroupingMode.BY_DATE.name)
        val groupingMode = DeletedGroupingMode.entries.find { it.name == groupingStr } ?: DeletedGroupingMode.BY_DATE
        val bubbleZoom = settingsObj.optDouble("bubbleZoomScale", 1.0).toFloat().coerceIn(0.75f, 1.65f)
        val uiColorModeStr = settingsObj.optString("uiColorMode", UiColorMode.COLORFUL.name)
        val uiColorMode = UiColorMode.entries.find { it.name == uiColorModeStr } ?: UiColorMode.COLORFUL
        val uiOpacity = settingsObj.optDouble("uiOpacity", 0.50).toFloat().coerceIn(0.0f, 1.0f)
        val categoryOrder = ConversationFilter.deserializeOrder(settingsObj.optString("categoryOrder", ""))
        val dockBarOrder = DockBarItem.deserializeOrder(settingsObj.optString("dockBarOrder", ""))
        val recallMessagesEnabled = settingsObj.optBoolean("recallMessagesEnabled", true)
        val identifyMaliciousWebsites = settingsObj.optBoolean("identifyMaliciousWebsites", true)
        val inAppSoundsEnabled = settingsObj.optBoolean("inAppSoundsEnabled", true)
        val pinchToZoomEnabled = settingsObj.optBoolean("pinchToZoomEnabled", true)
        val expressiveAnimationsEnabled = settingsObj.optBoolean("expressiveAnimationsEnabled", true)
        val flashAlertEnabled = settingsObj.optBoolean("flashAlertEnabled", false)
        val flashAlertMode = FlashAlertMode.fromId(settingsObj.optString("flashAlertMode", FlashAlertMode.ALWAYS.name))
        val hapticsEnabled = settingsObj.optBoolean("hapticsEnabled", true)
        val bubblesEnabled = settingsObj.optBoolean("bubblesEnabled", true)
        val autoPreviewsShowAll = settingsObj.optBoolean("autoPreviewsShowAll", true)
        val autoPreviewsWebLinksOnly = settingsObj.optBoolean("autoPreviewsWebLinksOnly", false)
        val autoPreviewsWifiOnly = settingsObj.optBoolean("autoPreviewsWifiOnly", false)

        val contactWpMap = mutableMapOf<String, WallpaperConfig>()
        val contactWpObj = settingsObj.optJSONObject("contactWallpapers")
        if (contactWpObj != null) {
            val keys = contactWpObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val serializedWp = contactWpObj.optString(key)
                contactWpMap[key] = WallpaperConfig.deserialize(serializedWp)
            }
        }

        val settings = AppSettingsSnapshot(
            themeMode = themeMode,
            showContactPhotos = showContactPhotos,
            listLayoutMode = layoutMode,
            mainWallpaper = mainWallpaper,
            contactWallpapers = contactWpMap,
            swipeRightAction = rightAction,
            swipeLeftAction = leftAction,
            autoDeleteDays = autoDeleteDays,
            deletedGroupingMode = groupingMode,
            bubbleZoomScale = bubbleZoom,
            uiColorMode = uiColorMode,
            uiOpacity = uiOpacity,
            categoryOrder = categoryOrder,
            dockBarOrder = dockBarOrder,
            recallMessagesEnabled = recallMessagesEnabled,
            identifyMaliciousWebsites = identifyMaliciousWebsites,
            inAppSoundsEnabled = inAppSoundsEnabled,
            pinchToZoomEnabled = pinchToZoomEnabled,
            expressiveAnimationsEnabled = expressiveAnimationsEnabled,
            flashAlertEnabled = flashAlertEnabled,
            flashAlertMode = flashAlertMode,
            hapticsEnabled = hapticsEnabled,
            bubblesEnabled = bubblesEnabled,
            autoPreviewsShowAll = autoPreviewsShowAll,
            autoPreviewsWebLinksOnly = autoPreviewsWebLinksOnly,
            autoPreviewsWifiOnly = autoPreviewsWifiOnly,
        )

        // 2. Pins, Highlights, Blocked & Metadata
        val pinnedAddrs = mutableSetOf<String>()
        root.optJSONArray("pinnedAddresses")?.let { arr ->
            for (i in 0 until arr.length()) {
                pinnedAddrs.add(arr.getString(i))
            }
        }

        val pinnedMsgs = mutableSetOf<Long>()
        root.optJSONArray("pinnedMessageIds")?.let { arr ->
            for (i in 0 until arr.length()) {
                pinnedMsgs.add(arr.getLong(i))
            }
        }

        val highlightedMsgs = mutableSetOf<Long>()
        root.optJSONArray("highlightedMessageIds")?.let { arr ->
            for (i in 0 until arr.length()) {
                highlightedMsgs.add(arr.getLong(i))
            }
        }

        val blockedAddrs = mutableSetOf<String>()
        root.optJSONArray("blockedAddresses")?.let { arr ->
            for (i in 0 until arr.length()) {
                blockedAddrs.add(arr.getString(i))
            }
        }

        val deletedMeta = mutableMapOf<Long, Long>()
        root.optJSONObject("deletedThreadsMeta")?.let { obj ->
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                k.toLongOrNull()?.let { id ->
                    deletedMeta[id] = obj.optLong(k, System.currentTimeMillis())
                }
            }
        }

        // 3. Threads
        val threads = mutableListOf<ConversationThread>()
        root.optJSONArray("threads")?.let { arr ->
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let { tObj ->
                    threads.add(deserializeThread(tObj, pinnedMsgs, highlightedMsgs))
                }
            }
        }

        // 4. Deleted Threads
        val deletedThreads = mutableListOf<DeletedConversationEntry>()
        root.optJSONArray("deletedThreads")?.let { arr ->
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let { dObj ->
                    val deletedAt = dObj.optLong("deletedAtMillis", System.currentTimeMillis())
                    val tObj = dObj.optJSONObject("thread")
                    if (tObj != null) {
                        deletedThreads.add(
                            DeletedConversationEntry(
                                thread = deserializeThread(tObj, pinnedMsgs, highlightedMsgs),
                                deletedAtMillis = deletedAt
                            )
                        )
                    }
                }
            }
        }

        return BackupRestorePayload(
            version = version,
            createdAtMillis = createdAt,
            deviceModel = deviceModel,
            settings = settings,
            pinnedAddresses = pinnedAddrs,
            pinnedMessageIds = pinnedMsgs,
            highlightedMessageIds = highlightedMsgs,
            blockedAddresses = blockedAddrs,
            deletedThreadsMeta = deletedMeta,
            threads = threads,
            deletedThreads = deletedThreads
        )
    }

    private fun deserializeThread(
        obj: JSONObject,
        pinnedMsgs: Set<Long>,
        highlightedMsgs: Set<Long>
    ): ConversationThread {
        val id = obj.optLong("id", System.currentTimeMillis())
        val name = obj.optString("contactName", "Unknown")
        val addr = obj.optString("address", "")
        val initials = obj.optString("avatarInitials", "U")
        val colorHex = obj.optLong("avatarAccentColorHex", 0xFF00E5FF)
        val unreadCount = obj.optInt("unreadCount", 0)
        val isPinned = obj.optBoolean("isPinned", false)
        val isService = obj.optBoolean("isService", false)

        val messages = mutableListOf<ChatMessage>()
        obj.optJSONArray("messages")?.let { mArr ->
            for (i in 0 until mArr.length()) {
                mArr.optJSONObject(i)?.let { mObj ->
                    val mId = mObj.optLong("id", 0L)
                    val mThreadId = mObj.optLong("threadId", id)
                    val mAddr = mObj.optString("address", addr)
                    val mBody = mObj.optString("body", "")
                    val mTime = mObj.optLong("timestampMillis", System.currentTimeMillis())
                    val isIncoming = mObj.optBoolean("isIncoming", true)
                    val typeStr = mObj.optString("type", MessageType.SMS.name)
                    val type = MessageType.entries.find { it.name == typeStr } ?: MessageType.SMS
                    val statusStr = mObj.optString("status", DeliveryStatus.DELIVERED.name)
                    val status = DeliveryStatus.entries.find { it.name == statusStr } ?: DeliveryStatus.DELIVERED
                    val mPinned = mObj.optBoolean("isPinned", mId in pinnedMsgs)
                    val mHighlighted = mObj.optBoolean("isHighlighted", mId in highlightedMsgs)

                    var attachment: MmsAttachment? = null
                    mObj.optJSONObject("attachment")?.let { aObj ->
                        val uStr = aObj.optString("uri")
                        if (uStr.isNotBlank()) {
                            attachment = MmsAttachment(
                                uri = Uri.parse(uStr),
                                mimeType = aObj.optString("mimeType", "image/jpeg"),
                                caption = aObj.optString("caption", "MMS Photo")
                            )
                        }
                    }

                    messages.add(
                        ChatMessage(
                            id = mId,
                            threadId = mThreadId,
                            address = mAddr,
                            body = mBody,
                            timestampMillis = mTime,
                            isIncoming = isIncoming,
                            type = type,
                            status = status,
                            isPinned = mPinned,
                            isHighlighted = mHighlighted,
                            attachment = attachment,
                            securityAnalysis = MessageSecurityAnalyzer.analyzeMessage(mBody)
                        )
                    )
                }
            }
        }

        return ConversationThread(
            id = id,
            contactName = name,
            address = addr,
            avatarInitials = initials,
            avatarAccentColorHex = colorHex,
            contactPhotoUri = null,
            messages = messages,
            unreadCount = unreadCount,
            isPinned = isPinned,
            isService = isService
        )
    }

    /**
     * Writes the encrypted backup JSON into app internal storage under files/backups/.
     * Prevents any unauthorized extraction if the device is connected to a PC.
     */
    fun createInternalBackupFile(context: Context, jsonString: String, password: String? = null): File {
        val dir = File(context.filesDir, "backups")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val contentToWrite = if (SecurityCryptoManager.isEncryptedBackup(jsonString)) {
            jsonString
        } else {
            SecurityCryptoManager.encryptBackup(jsonString, password)
        }
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val timestampedFile = File(dir, "txtura_backup_$dateStr.json")
        timestampedFile.writeText(contentToWrite, Charsets.UTF_8)

        // Also update latest snapshot file
        val latestFile = File(dir, "txtura_backup_latest.json")
        latestFile.writeText(contentToWrite, Charsets.UTF_8)

        saveLastBackupTimestamp(context, System.currentTimeMillis())
        return timestampedFile
    }

    /**
     * Resolves a secure FileProvider Content URI for sharing or uploading to Google Drive.
     */
    fun getBackupContentUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Creates an Intent to share or upload the backup JSON to Google Drive or other apps.
     */
    fun createShareOrDriveIntent(
        context: Context,
        backupFile: File,
        targetGoogleDriveOnly: Boolean = false
    ): Intent {
        val uri = getBackupContentUri(context, backupFile)
        val dateStr = SimpleDateFormat("dd.MM.yyyy. HH:mm", Locale.getDefault()).format(Date())
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Txtura Backup - $dateStr")
            putExtra(Intent.EXTRA_TEXT, "Txtura SMS/MMS backup file ($dateStr)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (targetGoogleDriveOnly && isGoogleDriveInstalled(context)) {
            intent.`package` = GOOGLE_DRIVE_PACKAGE
        }
        return intent
    }

    /**
     * Saves backup content directly to a user-selected SAF document Uri.
     * Guarantees AES-256-GCM encryption before writing to external storage or SAF.
     */
    fun saveBackupToUri(context: Context, targetUri: Uri, jsonString: String, password: String? = null): Boolean {
        return try {
            val contentToWrite = if (SecurityCryptoManager.isEncryptedBackup(jsonString)) {
                jsonString
            } else {
                SecurityCryptoManager.encryptBackup(jsonString, password)
            }
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                out.write(contentToWrite.toByteArray(Charsets.UTF_8))
                out.flush()
            }
            saveLastBackupTimestamp(context, System.currentTimeMillis())
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Reads a backup JSON string from a user-selected SAF document Uri.
     */
    fun readBackupFromUri(context: Context, sourceUri: Uri): String {
        return context.contentResolver.openInputStream(sourceUri)?.use { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
        } ?: throw IllegalArgumentException("Cannot open input stream for $sourceUri")
    }
}

data class GitHubReleaseInfo(
    val versionTag: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val releasePageUrl: String,
    val apkDownloadUrl: String?,
    val isUpdateAvailable: Boolean,
)

sealed class AppUpdateCheckResult {
    data class Success(val info: GitHubReleaseInfo) : AppUpdateCheckResult()
    data object NoInternet : AppUpdateCheckResult()
    data class Error(val message: String) : AppUpdateCheckResult()
}

/**
 * Checks GitHub Releases (`avngrn/Txtura`) for new application versions,
 * supports automatic real-time checks every 6 hours when Wi-Fi or mobile data is connected,
 * and downloads/installs the latest APK update.
 */
object AppUpdateManager {

    const val GITHUB_OWNER = "avngrn"
    const val GITHUB_REPO = "Txtura"
    const val CHECK_INTERVAL_MILLIS = 6L * 60L * 60L * 1000L // 6 hours
    private const val PREFS_UPDATE = "txtura_github_update_prefs"
    private const val KEY_LAST_CHECK_TIME = "last_update_check_timestamp"
    private const val KEY_CACHED_TAG = "cached_latest_tag"
    private const val KEY_CACHED_TITLE = "cached_release_title"
    private const val KEY_CACHED_NOTES = "cached_release_notes"
    private const val KEY_CACHED_PAGE_URL = "cached_release_page_url"
    private const val KEY_CACHED_APK_URL = "cached_release_apk_url"

    private const val KEY_PENDING_INSTALL_APK_PATH = "pending_install_apk_path"

    private const val LATEST_RELEASE_API = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    private const val ALL_RELEASES_API = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases?per_page=10"
    private const val ALL_TAGS_API = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/tags?per_page=10"
    const val RELEASES_WEB_URL = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases"
    const val GITHUB_REPO_WEB_URL = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO"

    const val CHANNEL_ID_UPDATES = "txtura_app_updates"
    const val NOTIFICATION_ID_UPDATE = 9901
    const val ACTION_NAVIGATE_TO_UPDATES = "com.txtura.action.NAVIGATE_TO_UPDATES"
    const val EXTRA_NAVIGATE_TO_UPDATES = "extra_navigate_to_updates"
    private const val UPDATE_PERIODIC_WORK_NAME = "txtura_github_update_periodic_work"
    private const val UPDATE_IMMEDIATE_WORK_NAME = "txtura_github_update_immediate_work"

    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile
    private var backgroundMonitoringStarted = false

    fun resolveCurrentVersionName(context: Context): String {
        return runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "3.3.1"
        }.getOrDefault("3.3.1")
    }

    /**
     * Starts continuous background update monitoring at the Application level so updates are detected
     * and notified immediately even when the user has not opened MainActivity.
     */
    fun startBackgroundUpdateMonitoring(context: Context) {
        val appContext = context.applicationContext
        scheduleUpdateCheckWork(appContext)
        if (backgroundMonitoringStarted) return
        synchronized(this) {
            if (backgroundMonitoringStarted) return
            backgroundMonitoringStarted = true
        }

        backgroundScope.launch {
            while (true) {
                try {
                    val ver = resolveCurrentVersionName(appContext)
                    checkAndNotifyIfUpdateAvailable(appContext, ver)
                } catch (_: Exception) {
                }
                delay(15.minutes)
            }
        }

        try {
            val cm = appContext.getSystemService(ConnectivityManager::class.java)
            cm?.registerDefaultNetworkCallback(
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        backgroundScope.launch {
                            val ver = resolveCurrentVersionName(appContext)
                            checkAndNotifyIfUpdateAvailable(appContext, ver)
                        }
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities,
                    ) {
                        val hasNet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                            (
                                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                            )
                        if (hasNet) {
                            backgroundScope.launch {
                                val ver = resolveCurrentVersionName(appContext)
                                checkAndNotifyIfUpdateAvailable(appContext, ver)
                            }
                        }
                    }
                }
            )
        } catch (_: Exception) {
        }
    }

    fun scheduleUpdateCheckWork(context: Context) {
        try {
            val appContext = context.applicationContext
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<AppUpdateCheckWorker>(
                15,
                TimeUnit.MINUTES,
            )
                .setConstraints(constraints)
                .build()

            val immediateRequest = OneTimeWorkRequestBuilder<AppUpdateCheckWorker>()
                .setConstraints(constraints)
                .build()

            val wm = WorkManager.getInstance(appContext)
            wm.enqueueUniquePeriodicWork(
                UPDATE_PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest,
            )
            wm.enqueueUniqueWork(
                UPDATE_IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                immediateRequest,
            )
        } catch (_: Exception) {
        }
    }

    fun ensureNotificationChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val locStrings = TxturaStrings.forLocale()
        val channel = NotificationChannel(
            CHANNEL_ID_UPDATES,
            locStrings.updateNotificationChannelName,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = locStrings.updateNotificationChannelDesc
            enableLights(true)
            setShowBadge(true)
        }
        nm.createNotificationChannel(channel)
    }

    fun postUpdateAvailableNotification(context: Context, releaseInfo: GitHubReleaseInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotifPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasNotifPerm) return
        }

        ensureNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_NAVIGATE_TO_UPDATES
            putExtra(EXTRA_NAVIGATE_TO_UPDATES, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_UPDATE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val locStrings = TxturaStrings.forLocale()
        val title = locStrings.updateNotificationTitle
        val text = locStrings.updateNotificationText(releaseInfo.versionTag)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_UPDATES)
            .setSmallIcon(R.drawable.ic_stat_message)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_UPDATE, notification)
        } catch (_: SecurityException) {
        }
    }

    fun dismissUpdateNotification(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_UPDATE)
        } catch (_: Exception) {
        }
    }

    suspend fun checkAndNotifyIfUpdateAvailable(
        context: Context,
        currentVersionName: String,
        force: Boolean = false,
    ) = withContext(Dispatchers.IO) {
        if (!hasActiveInternetConnection(context)) return@withContext

        val lastCheck = getLastCheckTimestamp(context)
        val elapsed = System.currentTimeMillis() - lastCheck

        if (!force && lastCheck > 0L && elapsed < 45_000L) {
            val cached = getCachedReleaseInfo(context, currentVersionName)
            if (cached?.isUpdateAvailable == true) {
                postUpdateAvailableNotification(context, cached)
            }
            return@withContext
        }

        when (val res = checkForUpdates(context, currentVersionName)) {
            is AppUpdateCheckResult.Success -> {
                if (res.info.isUpdateAvailable) {
                    postUpdateAvailableNotification(context, res.info)
                } else {
                    dismissUpdateNotification(context)
                }
            }
            else -> Unit
        }
    }

    fun getLastCheckTimestamp(context: Context): Long {
        return context.getSharedPreferences(PREFS_UPDATE, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_CHECK_TIME, 0L)
    }

    fun getCachedReleaseInfo(context: Context, currentVersionName: String): GitHubReleaseInfo? {
        val prefs = context.getSharedPreferences(PREFS_UPDATE, Context.MODE_PRIVATE)
        val tag = prefs.getString(KEY_CACHED_TAG, null)?.takeIf { it.isNotBlank() } ?: return null
        val title = prefs.getString(KEY_CACHED_TITLE, tag).orEmpty().ifBlank { tag }
        val notes = prefs.getString(KEY_CACHED_NOTES, "").orEmpty()
        val pageUrl = prefs.getString(KEY_CACHED_PAGE_URL, RELEASES_WEB_URL).orEmpty().ifBlank { RELEASES_WEB_URL }
        val apkUrl = prefs.getString(KEY_CACHED_APK_URL, null)?.takeIf { it.isNotBlank() }
        return GitHubReleaseInfo(
            versionTag = tag,
            releaseTitle = title,
            releaseNotes = notes,
            releasePageUrl = pageUrl,
            apkDownloadUrl = apkUrl,
            isUpdateAvailable = isRemoteVersionNewer(currentVersionName, tag),
        )
    }

    /**
     * Checks whether the phone currently has an active internet connection over Wi-Fi, mobile data, Ethernet, or VPN.
     * Supports any network interface (Wi-Fi, Cellular, Hotspot, VPN) without restricting to Wi-Fi.
     */
    fun hasActiveInternetConnection(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val hasTransport = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
            hasInternet && hasTransport
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Returns true if the current active internet connection is using mobile/cellular data
     * (or is a metered connection without Wi-Fi/Ethernet).
     */
    fun isCellularConnection(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> false
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                else -> cm.isActiveNetworkMetered
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Returns true if internet is available and at least 6 hours have elapsed since the last check.
     */
    fun shouldPerformPeriodicCheck(context: Context): Boolean {
        if (!hasActiveInternetConnection(context)) return false
        val lastCheck = getLastCheckTimestamp(context)
        val elapsed = System.currentTimeMillis() - lastCheck
        return (lastCheck == 0L) || (elapsed >= CHECK_INTERVAL_MILLIS)
    }

    /**
     * Compares two semantic version strings (e.g., `"1.0"`, `"v2.3.1"`, `"v3.7"`).
     * Returns > 0 if `versionB` is newer than `versionA`, < 0 if older, 0 if equal.
     * Works across any number of version jumps (e.g., v1.0 directly to v3.7).
     */
    fun compareSemanticVersions(versionA: String, versionB: String): Int {
        val cleanA = versionA.trim().removePrefix("v").removePrefix("V")
        val cleanB = versionB.trim().removePrefix("v").removePrefix("V")
        if (cleanA.equals(cleanB, ignoreCase = true)) return 0

        val partsA = cleanA.split(Regex("[^0-9]+")).mapNotNull { it.toIntOrNull() }
        val partsB = cleanB.split(Regex("[^0-9]+")).mapNotNull { it.toIntOrNull() }
        if (partsB.isEmpty()) return -1
        if (partsA.isEmpty()) return 1

        val maxLen = maxOf(partsA.size, partsB.size)
        for (i in 0 until maxLen) {
            val a = partsA.getOrElse(i) { 0 }
            val b = partsB.getOrElse(i) { 0 }
            if (b > a) return 1
            if (b < a) return -1
        }
        return 0
    }

    /**
     * Compares semantic version strings (e.g., current `"1.0"` vs remote `"v3.7"`).
     * Always returns true if `remoteTag` is higher than `currentVersion`, regardless of how many
     * intermediate versions were skipped.
     */
    fun isRemoteVersionNewer(currentVersion: String, remoteTag: String): Boolean {
        return compareSemanticVersions(currentVersion, remoteTag) > 0
    }

    private fun openHttpsConnectionWithFallback(url: URL, context: Context): HttpURLConnection {
        return try {
            AppSecurityManager.openSecureConnection(url, context)
        } catch (_: Exception) {
            (url.openConnection() as HttpsURLConnection).apply {
                connectTimeout = 12000
                readTimeout = 12000
            }
        }
    }

    /**
     * Fetches updater metadata over HTTPS (works on both Wi-Fi and Cellular).
     */
    private fun fetchJsonFromUrl(context: Context, urlString: String): String? {
        val url = try {
            URL(urlString)
        } catch (_: Exception) {
            return null
        }

        return try {
            val conn = openHttpsConnectionWithFallback(url, context)
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "Txtura-Android-App-Updater")
            conn.connectTimeout = 12000
            conn.readTimeout = 12000
            try {
                val code = conn.responseCode
                if (code == HttpURLConnection.HTTP_OK) {
                    conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } else {
                    null
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Log.w("TxturaUpdate", "Connection failed for $urlString: ${e.message}")
            null
        }
    }

    private fun parseReleaseObject(
        releaseObj: JSONObject,
        currentVersionName: String,
    ): GitHubReleaseInfo? {
        if (releaseObj.optBoolean("draft", false)) return null
        val tag = releaseObj.optString("tag_name", "").trim()
            .ifBlank { releaseObj.optString("name", "").trim() }
        if (tag.isBlank()) return null

        val title = releaseObj.optString("name", tag).trim().ifBlank { tag }
        val notes = releaseObj.optString("body", "").trim()
        val htmlUrl = releaseObj.optString("html_url", RELEASES_WEB_URL).trim().ifBlank { RELEASES_WEB_URL }

        var apkUrl: String? = null
        val assets = releaseObj.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val assetName = asset.optString("name", "").lowercase(Locale.ROOT)
                val downloadUrl = asset.optString("browser_download_url", "").trim()
                if (assetName.endsWith(".apk") && downloadUrl.isNotBlank()) {
                    apkUrl = downloadUrl
                    break
                }
            }
        }

        if (apkUrl.isNullOrBlank()) {
            apkUrl = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/download/$tag/app-release.apk"
        }

        return GitHubReleaseInfo(
            versionTag = tag,
            releaseTitle = title,
            releaseNotes = notes,
            releasePageUrl = htmlUrl,
            apkDownloadUrl = apkUrl,
            isUpdateAvailable = isRemoteVersionNewer(currentVersionName, tag),
        )
    }

    /**
     * Queries GitHub Releases API for the absolute newest version of Txtura across `/releases/latest`,
     * `/releases`, and `/tags` so any previous version can always update directly to the latest version.
     */
    suspend fun checkForUpdates(
        context: Context,
        currentVersionName: String,
    ): AppUpdateCheckResult = withContext(Dispatchers.IO) {
        if (!hasActiveInternetConnection(context)) {
            return@withContext AppUpdateCheckResult.NoInternet
        }

        try {
            val candidates = mutableListOf<GitHubReleaseInfo>()

            fetchJsonFromUrl(context, LATEST_RELEASE_API)?.let { text ->
                runCatching {
                    val obj = JSONObject(text.trim())
                    parseReleaseObject(obj, currentVersionName)?.let { candidates.add(it) }
                }
            }

            fetchJsonFromUrl(context, ALL_RELEASES_API)?.let { text ->
                runCatching {
                    val arr = JSONArray(text.trim())
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        parseReleaseObject(obj, currentVersionName)?.let { candidates.add(it) }
                    }
                }
            }

            if (candidates.isEmpty()) {
                fetchJsonFromUrl(context, ALL_TAGS_API)?.let { text ->
                    runCatching {
                        val tagsArr = JSONArray(text.trim())
                        for (i in 0 until tagsArr.length()) {
                            val tagObj = tagsArr.optJSONObject(i) ?: continue
                            val tagName = tagObj.optString("name", "").trim()
                            if (tagName.isNotBlank()) {
                                val fallbackApkUrl = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/download/$tagName/app-release.apk"
                                candidates.add(
                                    GitHubReleaseInfo(
                                        versionTag = tagName,
                                        releaseTitle = tagName,
                                        releaseNotes = "",
                                        releasePageUrl = RELEASES_WEB_URL,
                                        apkDownloadUrl = fallbackApkUrl,
                                        isUpdateAvailable = isRemoteVersionNewer(currentVersionName, tagName),
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Pick the release with the highest semantic version so any older version jumps straight to the newest one
            val bestRelease = candidates.maxWithOrNull { a, b ->
                compareSemanticVersions(b.versionTag, a.versionTag)
            }

            val info = bestRelease ?: GitHubReleaseInfo(
                versionTag = "v$currentVersionName",
                releaseTitle = "v$currentVersionName",
                releaseNotes = "",
                releasePageUrl = RELEASES_WEB_URL,
                apkDownloadUrl = null,
                isUpdateAvailable = false,
            )

            val now = System.currentTimeMillis()
            context.getSharedPreferences(PREFS_UPDATE, Context.MODE_PRIVATE).edit {
                putLong(KEY_LAST_CHECK_TIME, now)
                putString(KEY_CACHED_TAG, info.versionTag)
                putString(KEY_CACHED_TITLE, info.releaseTitle)
                putString(KEY_CACHED_NOTES, info.releaseNotes)
                putString(KEY_CACHED_PAGE_URL, info.releasePageUrl)
                putString(KEY_CACHED_APK_URL, info.apkDownloadUrl.orEmpty())
            }

            if (info.isUpdateAvailable) {
                postUpdateAvailableNotification(context.applicationContext, info)
            } else {
                dismissUpdateNotification(context.applicationContext)
            }

            AppUpdateCheckResult.Success(info)
        } catch (e: Exception) {
            AppUpdateCheckResult.Error(e.localizedMessage ?: "Greška pri proveri ažuriranja")
        }
    }

    /**
     * Checks if there is a downloaded APK waiting for installation (after the user returns from
     * granting "Install unknown apps" permission) and immediately launches the installer.
     */
    fun resumePendingApkInstallIfAllowed(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_UPDATE, Context.MODE_PRIVATE)
            val pendingPath = prefs.getString(KEY_PENDING_INSTALL_APK_PATH, null) ?: return
            val apkFile = File(pendingPath)
            if (!apkFile.exists() || apkFile.length() < 10_000L) {
                prefs.edit { remove(KEY_PENDING_INSTALL_APK_PATH) }
                return
            }
            if (context.packageManager.canRequestPackageInstalls()) {
                prefs.edit { remove(KEY_PENDING_INSTALL_APK_PATH) }
                launchApkPackageInstaller(context, apkFile)
            }
        } catch (e: Exception) {
            Log.e("TxturaUpdate", "Failed to resume pending APK installation", e)
        }
    }

    /**
     * Downloads the APK directly from GitHub (works over both Wi-Fi and Mobile Data) and triggers
     * the Android package installer to update Txtura in-place without losing any local data.
     */
    suspend fun downloadAndInstallUpdate(
        context: Context,
        releaseInfo: GitHubReleaseInfo,
        onProgress: (Int) -> Unit,
    ): Boolean = withContext(Dispatchers.IO) {
        val apkUrlStr = releaseInfo.apkDownloadUrl
        if (apkUrlStr.isNullOrBlank()) {
            withContext(Dispatchers.Main) {
                openReleasePageInBrowser(context, releaseInfo.releasePageUrl)
            }
            return@withContext true
        }

        try {
            withContext(Dispatchers.Main) { onProgress(2) }

            val updatesDir = File(context.cacheDir, "updates").apply {
                if (!exists()) mkdirs()
            }
            val cleanVer = releaseInfo.versionTag.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val targetApk = File(updatesDir, "Txtura-update-$cleanVer.apk")
            if (targetApk.exists()) {
                targetApk.delete()
            }

            var currentUrl = apkUrlStr
            var redirects = 0
            var connection: HttpURLConnection? = null

            while (redirects < 8) {
                val urlObj = URL(currentUrl)
                val conn = openHttpsConnectionWithFallback(urlObj, context)
                conn.instanceFollowRedirects = false
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/octet-stream, */*")
                conn.setRequestProperty("User-Agent", "Txtura-Android-App-Updater")
                conn.connectTimeout = 20000
                conn.readTimeout = 30000

                val code = conn.responseCode
                if (code in 300..399) {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (location.isNullOrBlank()) break
                    currentUrl = if (location.startsWith("http", ignoreCase = true)) {
                        location
                    } else {
                        URL(urlObj, location).toString()
                    }
                    redirects++
                } else {
                    connection = conn
                    break
                }
            }

            val activeConn = connection
            if (activeConn == null || activeConn.responseCode != HttpURLConnection.HTTP_OK) {
                activeConn?.disconnect()
                withContext(Dispatchers.Main) {
                    openReleasePageInBrowser(context, releaseInfo.releasePageUrl)
                }
                return@withContext false
            }

            try {
                val totalBytes = activeConn.contentLengthLong
                var downloadedBytes = 0L
                var lastReportedProgress = 2

                activeConn.inputStream.use { input ->
                    targetApk.outputStream().use { output ->
                        val buffer = ByteArray(16 * 1024)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloadedBytes += read
                            if (totalBytes > 0L) {
                                val pct = ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(3, 99)
                                if (pct >= lastReportedProgress + 2) {
                                    lastReportedProgress = pct
                                    withContext(Dispatchers.Main) { onProgress(pct) }
                                }
                            }
                        }
                        output.flush()
                    }
                }
            } finally {
                activeConn.disconnect()
            }

            if (!targetApk.exists() || targetApk.length() < 10_000L) {
                targetApk.delete()
                withContext(Dispatchers.Main) {
                    openReleasePageInBrowser(context, releaseInfo.releasePageUrl)
                }
                return@withContext false
            }

            withContext(Dispatchers.Main) {
                onProgress(100)
                if (!context.packageManager.canRequestPackageInstalls()) {
                    context.getSharedPreferences(PREFS_UPDATE, Context.MODE_PRIVATE).edit {
                        putString(KEY_PENDING_INSTALL_APK_PATH, targetApk.absolutePath)
                    }
                    val locStrings = TxturaStrings.forLocale()
                    Toast.makeText(
                        context,
                        if (locStrings.isSerbian) {
                            "Dozvolite instaliranje iz ovog izvora da bi se ažuriranje završilo."
                        } else {
                            "Allow installing from this source to complete the update."
                        },
                        Toast.LENGTH_LONG
                    ).show()
                    val permIntent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        "package:${context.packageName}".toUri()
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(permIntent)
                } else {
                    launchApkPackageInstaller(context, targetApk)
                }
            }
            true
        } catch (e: Exception) {
            Log.e("TxturaUpdate", "Direct APK download failed, opening browser fallback", e)
            withContext(Dispatchers.Main) {
                openReleasePageInBrowser(context, releaseInfo.releasePageUrl)
            }
            false
        }
    }

    private fun launchApkPackageInstaller(context: Context, apkFile: File) {
        try {
            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e("TxturaUpdate", "Failed to launch package installer", e)
            openReleasePageInBrowser(context, RELEASES_WEB_URL)
        }
    }

    fun openReleasePageInBrowser(context: Context, url: String = RELEASES_WEB_URL) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Ignore if no browser available
        }
    }

    fun openGitHubRepoInBrowser(context: Context, url: String = GITHUB_REPO_WEB_URL) {
        openReleasePageInBrowser(context, url)
    }
}

class AppUpdateCheckWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            val currentVer = AppUpdateManager.resolveCurrentVersionName(applicationContext)
            AppUpdateManager.checkAndNotifyIfUpdateAvailable(
                context = applicationContext,
                currentVersionName = currentVer,
                force = true,
            )
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
