package com.txtura.data

import android.net.Uri
import com.txtura.security.SecurityAnalysisResult
import com.txtura.security.SecurityRiskLevel

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
    ALL,
    PINNED,
    UNREAD,
    SECURITY
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
 * Curated built-in backgrounds (10 presets + Default theme).
 */
enum class WallpaperPreset(
    val baseColorHex: Long,
    val primaryOrbHex: Long,
    val secondaryOrbHex: Long,
    val accentOrbHex: Long
) {
    DEFAULT(0xFF000000, 0xFF00E5FF, 0xFF6366F1, 0xFFA855F7),
    AURORA_BOREALIS(0xFF021019, 0xFF00F5D4, 0xFF00BBF9, 0xFF7B2CBF),
    CYBER_NEBULA(0xFF090418, 0xFFF72585, 0xFF7209B7, 0xFF4CC9F0),
    MIDNIGHT_OCEAN(0xFF020B18, 0xFF0077B6, 0xFF00B4D8, 0xFF48CAE4),
    SUNSET_HORIZON(0xFF160712, 0xFFFF6B6B, 0xFFFF9E00, 0xFF9D4EDD),
    EMERALD_FOREST(0xFF03140E, 0xFF10B981, 0xFF059669, 0xFF34D399),
    ROYAL_AMETHYST(0xFF10051C, 0xFFA855F7, 0xFFEC4899, 0xFF6366F1),
    STARLIGHT_GALAXY(0xFF030611, 0xFF38BDF8, 0xFF818CF8, 0xFFE879F9),
    CRIMSON_VELVET(0xFF160409, 0xFFF43F5E, 0xFFE11D48, 0xFFFB923C),
    ARCTIC_FROST(0xFF071622, 0xFF67E8F9, 0xFF38BDF8, 0xFFA5F3FC),
    OBSIDIAN_GOLD(0xFF0A0804, 0xFFF59E0B, 0xFFD97706, 0xFFFDE68A)
}

/**
 * Represents a chosen wallpaper (either one of the 10 built-in presets, Default, or a custom gallery image URI).
 */
data class WallpaperConfig(
    val preset: WallpaperPreset = WallpaperPreset.DEFAULT,
    val customImageUri: String? = null
) {
    val isCustomGalleryImage: Boolean
        get() = !customImageUri.isNullOrBlank()

    fun serialize(): String {
        return if (!customImageUri.isNullOrBlank()) {
            "custom:$customImageUri"
        } else {
            "preset:${preset.name}"
        }
    }

    companion object {
        fun deserialize(raw: String?): WallpaperConfig {
            if (raw.isNullOrBlank()) return WallpaperConfig()
            return when {
                raw.startsWith("custom:") -> {
                    val uriStr = raw.removePrefix("custom:").takeIf { it.isNotBlank() }
                    if (uriStr != null) {
                        WallpaperConfig(preset = WallpaperPreset.DEFAULT, customImageUri = uriStr)
                    } else {
                        WallpaperConfig()
                    }
                }
                raw.startsWith("preset:") -> {
                    val name = raw.removePrefix("preset:")
                    val found = WallpaperPreset.entries.find { it.name == name } ?: WallpaperPreset.DEFAULT
                    WallpaperConfig(preset = found, customImageUri = null)
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
    val lastMessage: ChatMessage? = messages.maxByOrNull { it.timestampMillis }

    val pinnedMessages: List<ChatMessage> = messages.filter { it.isPinned }

    val highestRiskLevel: SecurityRiskLevel =
        messages.maxOfOrNull { it.securityAnalysis.riskLevel } ?: SecurityRiskLevel.SAFE
}

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
