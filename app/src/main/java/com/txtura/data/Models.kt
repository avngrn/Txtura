package com.txtura.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.core.content.edit
import com.txtura.security.MessageSecurityAnalyzer
import com.txtura.security.SecurityAnalysisResult
import com.txtura.security.SecurityRiskLevel
import com.txtura.ui.theme.TxturaThemeMode
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
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
    FAILED
}

enum class ConversationFilter {
    PEOPLE,
    SERVICES,
    UNREAD,
    ALL;

    companion object {
        val defaultOrder: List<ConversationFilter> = listOf(
            PEOPLE,
            SERVICES,
            UNREAD,
            ALL,
        )

        fun deserializeOrder(raw: String?): List<ConversationFilter> {
            if (raw.isNullOrBlank()) return defaultOrder
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

enum class UiColorMode {
    COLORFUL,
    MONOCHROME
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
    OPEN_CONTACT,
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
) {
    // 10 Curated Colorful Presets + Default (with bespoke matching textures)
    DEFAULT(0xFF070A12, 0xFF1D4ED8, 0xFF4338CA, 0xFF38BDF8, false, WallpaperTexture.SILK_WEAVE),
    AURORA_BOREALIS(0xFF041416, 0xFF0D9488, 0xFF0284C7, 0xFF14B8A6, false, WallpaperTexture.AURORA_RIBBONS),
    VELVET_PLUM(0xFF120616, 0xFF7E22CE, 0xFF9333EA, 0xFFA855F7, false, WallpaperTexture.VELVET_NAP),
    MIDNIGHT_OCEAN(0xFF030D1A, 0xFF1D4ED8, 0xFF0284C7, 0xFF38BDF8, false, WallpaperTexture.OCEAN_CAUSTICS),
    SUNSET_HORIZON(0xFF180A0B, 0xFFBE123C, 0xFFC2410C, 0xFFF97316, false, WallpaperTexture.SAND_DUNES),
    EMERALD_FOREST(0xFF03140C, 0xFF047857, 0xFF059669, 0xFF10B981, false, WallpaperTexture.ORGANIC_BOTANICAL),
    ROYAL_AMETHYST(0xFF100618, 0xFF6D28D9, 0xFF7C3AED, 0xFF8B5CF6, false, WallpaperTexture.CRYSTAL_FACETS),
    STARLIGHT_GALAXY(0xFF040816, 0xFF2563EB, 0xFF475569, 0xFF60A5FA, false, WallpaperTexture.CONSTELLATION_MESH),
    CRIMSON_VELVET(0xFF17050A, 0xFF9F1239, 0xFFBE123C, 0xFFE11D48, false, WallpaperTexture.DAMASK_JACQUARD),
    ARCTIC_FROST(0xFF05131C, 0xFF0284C7, 0xFF0369A1, 0xFF38BDF8, false, WallpaperTexture.FROST_CRYSTALS),
    OBSIDIAN_GOLD(0xFF110C04, 0xFFB45309, 0xFF92400E, 0xFFD97706, false, WallpaperTexture.KINTSUGI_MARBLE),

    // 10 Refined Monochrome Presets (Light/Dark base + 1 distinct accent color + bespoke tactile texture)
    MONO_SLATE(0xFF0B0F17, 0xFF334155, 0xFF475569, 0xFF64748B, true, WallpaperTexture.SLATE_STRATA),
    MONO_SAPPHIRE(0xFF050B18, 0xFF1E3A8A, 0xFF1D4ED8, 0xFF2563EB, true, WallpaperTexture.BRUSHED_METAL),
    MONO_SAGE(0xFF06120D, 0xFF14532D, 0xFF166534, 0xFF15803D, true, WallpaperTexture.LINEN_CANVAS),
    MONO_CHAMPAGNE(0xFF131008, 0xFF785E2F, 0xFF8C713B, 0xFFA1824A, true, WallpaperTexture.FLUTED_GLASS),
    MONO_PLUM(0xFF140714, 0xFF581C87, 0xFF6B21A8, 0xFF7E22CE, true, WallpaperTexture.SUEDE_MICROFIBER),
    MONO_GRAPHITE(0xFF0A0A0A, 0xFF262626, 0xFF404040, 0xFF525252, true, WallpaperTexture.CARBON_TWILL),
    MONO_ROSE(0xFF15080C, 0xFF831843, 0xFF9D174D, 0xFFBE185D, true, WallpaperTexture.COTTON_PAPER),
    MONO_NAVY(0xFF040915, 0xFF172554, 0xFF1E3A8A, 0xFF1E40AF, true, WallpaperTexture.HERRINGBONE_TWILL),
    MONO_SMOKE(0xFF0F0E0E, 0xFF0D9488, 0xFF0F766E, 0xFF14B8A6, true, WallpaperTexture.TOPOGRAPHIC_CONTOUR),
    MONO_ESPRESSO(0xFF110A06, 0xFF9A3412, 0xFFC2410C, 0xFFEA580C, true, WallpaperTexture.PEBBLED_LEATHER);

    companion object {
        val colorfulPresets: List<WallpaperPreset>
            get() = entries.filter { !it.isMonochrome }

        val monochromePresets: List<WallpaperPreset>
            get() = entries.filter { it.isMonochrome }
    }
}

/**
 * Represents a chosen wallpaper (either one of the built-in presets, Default, or a custom gallery image URI)
 * together with its tactile surface texture ([texture]).
 */
data class WallpaperConfig(
    val preset: WallpaperPreset = WallpaperPreset.DEFAULT,
    val customImageUri: String? = null,
    val texture: WallpaperTexture = WallpaperTexture.AUTO_THEME
) {
    val isCustomGalleryImage: Boolean
        get() = !customImageUri.isNullOrBlank()

    val resolvedTexture: WallpaperTexture
        get() = if (texture == WallpaperTexture.AUTO_THEME) preset.defaultTexture else texture

    fun serialize(): String {
        val base = if (!customImageUri.isNullOrBlank()) {
            "custom:$customImageUri"
        } else {
            "preset:${preset.name}"
        }
        return if (texture == WallpaperTexture.AUTO_THEME) {
            base
        } else {
            "$base|tex:${texture.name}"
        }
    }

    companion object {
        fun deserialize(raw: String?): WallpaperConfig {
            if (raw.isNullOrBlank()) return WallpaperConfig()
            val parts = raw.split("|tex:", limit = 2)
            val basePart = parts[0]
            val parsedTexture = parts.getOrNull(1)?.let { texName ->
                WallpaperTexture.entries.find { it.name == texName.trim() }
            } ?: WallpaperTexture.AUTO_THEME

            return when {
                basePart.startsWith("custom:") -> {
                    val uriStr = basePart.removePrefix("custom:").takeIf { it.isNotBlank() }
                    if (uriStr != null) {
                        WallpaperConfig(
                            preset = WallpaperPreset.DEFAULT,
                            customImageUri = uriStr,
                            texture = parsedTexture
                        )
                    } else {
                        WallpaperConfig(texture = parsedTexture)
                    }
                }
                basePart.startsWith("preset:") -> {
                    val name = basePart.removePrefix("preset:")
                    val targetName = if (name == "CYBER_NEBULA") "VELVET_PLUM" else name
                    val found = WallpaperPreset.entries.find { it.name == targetName } ?: WallpaperPreset.DEFAULT
                    WallpaperConfig(preset = found, customImageUri = null, texture = parsedTexture)
                }
                else -> WallpaperConfig()
            }
        }
    }
}

data class MmsAttachment(
    val uri: Uri,
    val mimeType: String = "image/jpeg",
    val caption: String = "MMS Photo"
)

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

data class ConversationThread(
    val id: Long,
    val contactName: String,
    val address: String,
    val avatarInitials: String,
    val avatarAccentColorHex: Long,
    val contactPhotoUri: Uri? = null,
    val messages: List<ChatMessage>,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false
) {
    val lastMessage: ChatMessage? = messages.maxByOrNull(ChatMessage::timestampMillis)

    val pinnedMessages: List<ChatMessage> = messages.filter(ChatMessage::isPinned)

    val highestRiskLevel: SecurityRiskLevel =
        messages.maxOfOrNull { it.securityAnalysis.riskLevel } ?: SecurityRiskLevel.SAFE
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
    val categoryOrder: List<ConversationFilter> = ConversationFilter.defaultOrder
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
        get() = threads.sumOf { it.messages.size } + deletedThreads.sumOf { it.thread.messages.size }
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
        deletedThreads.forEach { entry ->
            val entryObj = JSONObject()
            entryObj.put("deletedAtMillis", entry.deletedAtMillis)
            entryObj.put("thread", serializeThread(entry.thread))
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
     */
    fun parseBackupJson(jsonString: String): BackupRestorePayload {
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
            categoryOrder = categoryOrder
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
            isPinned = isPinned
        )
    }

    /**
     * Writes the backup JSON into app internal storage under files/backups/.
     */
    fun createInternalBackupFile(context: Context, jsonString: String): File {
        val dir = File(context.filesDir, "backups")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val timestampedFile = File(dir, "txtura_backup_$dateStr.json")
        timestampedFile.writeText(jsonString, Charsets.UTF_8)

        // Also update latest snapshot file
        val latestFile = File(dir, "txtura_backup_latest.json")
        latestFile.writeText(jsonString, Charsets.UTF_8)

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
     */
    fun saveBackupToUri(context: Context, targetUri: Uri, jsonString: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                out.write(jsonString.toByteArray(Charsets.UTF_8))
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
