@file:Suppress("SpellCheckingInspection", "DestructuringDeclaration", "TrailingComma", "ClarifyingParentheses", "BooleanLiteralArgument")

package com.txtura.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.text.selection.rememberSelectionState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import com.txtura.R
import com.txtura.data.AppUpdateCheckResult
import com.txtura.data.AppUpdateManager
import com.txtura.data.BackupRestorePayload
import com.txtura.data.ChatMessage
import com.txtura.data.ContactPickerItem
import com.txtura.data.ConversationFilter
import com.txtura.data.ConversationThread
import com.txtura.data.DeletedConversationEntry
import com.txtura.data.DeletedGroupingMode
import com.txtura.data.DeliveryStatus
import com.txtura.data.DockBarItem
import com.txtura.data.GitHubReleaseInfo
import com.txtura.data.MainListLayoutMode
import com.txtura.data.MessageType
import com.txtura.data.MmsAttachment
import com.txtura.data.MmsAutoRetrieveMode
import com.txtura.data.MmsCreationMode
import com.txtura.data.MmsApnSettings
import com.txtura.data.MmsDataDisabledMode
import com.txtura.data.ScheduledSmsMessage
import com.txtura.data.SimCardMessage
import com.txtura.data.SmsMmsRepository
import com.txtura.data.SmsNotificationSound
import com.txtura.data.SwipeAction
import com.txtura.data.UiColorMode
import com.txtura.data.WallpaperConfig
import com.txtura.data.WallpaperPreset
import com.txtura.data.WallpaperTexture
import com.txtura.data.deduplicateMessageList
import com.txtura.security.AppSecurityManager
import com.txtura.security.DynamicPermissionManager
import com.txtura.security.MessageSecurityAnalyzer
import com.txtura.security.SecurityRiskLevel
import com.txtura.telephony.CalendarEventManager
import com.txtura.telephony.DocumentAttachmentHelper
import com.txtura.telephony.EmergencyAlertManager
import com.txtura.telephony.EmergencySeverity
import com.txtura.telephony.MmsImageCompressor
import com.txtura.telephony.VoiceMessageManager
import com.txtura.ui.components.GlassAlertDialog
import com.txtura.ui.components.GlassBackground
import com.txtura.ui.components.GlassCard
import com.txtura.ui.components.GlassIconButton
import com.txtura.ui.components.GlassModernSlider
import com.txtura.ui.components.GlassTooltipIconButton
import com.txtura.ui.components.OverflowTooltipText
import com.txtura.ui.components.TxturaIcons
import com.txtura.ui.components.drawTxturaTexture
import com.txtura.ui.i18n.AppStrings
import com.txtura.ui.i18n.TxturaStrings
import com.txtura.ui.theme.GlassPalette
import com.txtura.ui.theme.LocalAdaptiveMetrics
import com.txtura.ui.theme.LocalExpressiveAnimations
import com.txtura.ui.theme.LocalGlassPalette
import com.txtura.ui.theme.TxturaHaptics
import com.txtura.ui.theme.TxturaMotion
import com.txtura.ui.theme.TxturaThemeMode
import com.txtura.ui.theme.WallpaperHarmonyEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

private enum class BatchSelectionAction {
    DELETE,
    MARK_READ,
}

/**
 * Computes a harmonious [ColorFilter] for both the light-mode Txtura logo (`txtw.png`) and
 * the dark/AMOLED-mode Txtura logo (`txt.png`) so that the interior of the letters and 3D bevels
 * are in tone and in harmony with every individual light and dark theme (colorful presets,
 * monochrome accents, minimalist themes, or custom gallery wallpaper) on both the main screen
 * and in the Settings footer.
 */
@Composable
private fun rememberTxturaLightLogoColorFilter(
    palette: GlassPalette,
    wallpaperConfig: WallpaperConfig,
    isWhiteBackground: Boolean,
): ColorFilter {
    return remember(
        palette.isDark,
        palette.isAmoled,
        palette.accentPrimary,
        palette.accentSecondary,
        palette.glassSurfaceElevated,
        palette.uiColorMode,
        wallpaperConfig.preset,
        wallpaperConfig.customImageUri,
        isWhiteBackground,
    ) {
        val preset = wallpaperConfig.preset
        val isMinimalist = preset.isMinimalist || palette.uiColorMode == UiColorMode.MINIMALIST
        val isMono = (preset.isMonochrome || palette.uiColorMode == UiColorMode.MONOCHROME) && !isMinimalist
        val surfaceRgb = palette.glassSurfaceElevated.copy(alpha = 1f)
        val accentRgb = palette.accentPrimary.copy(alpha = 1f)

        if (isWhiteBackground) {
            val (interiorColor, outlineTone) = when {
                isMinimalist -> {
                    Color(0xFFECEEF2) to Color(0xFF18181B)
                }
                wallpaperConfig.isCustomGalleryImage -> {
                    val interior = lerp(lerp(Color(0xFFF3F6FC), surfaceRgb, 0.55f), accentRgb, 0.25f)
                    val outline = lerp(Color(0xFF141B29), accentRgb, 0.58f)
                    interior to outline
                }
                isMono -> {
                    val interior = lerp(lerp(Color(0xFFF2F5FA), surfaceRgb, 0.48f), accentRgb, 0.28f)
                    val outline = lerp(Color(0xFF151C28), accentRgb, 0.62f)
                    interior to outline
                }
                else -> {
                    val presetHue = lerp(Color(preset.primaryOrbHex), Color(preset.accentOrbHex), 0.45f)
                    val themeHarmony = lerp(presetHue, accentRgb, 0.45f)
                    val interior = lerp(lerp(Color(0xFFF3F6FC), surfaceRgb, 0.50f), themeHarmony, 0.28f)
                    val outline = lerp(Color(0xFF141B28), themeHarmony, 0.62f)
                    interior to outline
                }
            }

            val iR = interiorColor.red
            val iG = interiorColor.green
            val iB = interiorColor.blue
            val oR = outlineTone.red
            val oG = outlineTone.green
            val oB = outlineTone.blue

            // Row sums equal (iR, iG, iB) so white letter interiors (1, 1, 1) become exact `interiorColor`,
            // inner gray bevels scale smoothly with `interiorColor`, and dark outlines harmonize with `outlineTone`.
            val matrix = ColorMatrix(
                floatArrayOf(
                    iR - oR * 0.42f, oR * 0.14f, oR * 0.28f, 0f, 0f,
                    iG - oG * 0.42f, oG * 0.14f, oG * 0.28f, 0f, 0f,
                    iB - oB * 0.42f, oB * 0.14f, oB * 0.28f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            ColorFilter.colorMatrix(matrix)
        } else {
            // Dark & AMOLED themes (`txt.png`): harmonize the letter interior and 3D bevels with each dark theme
            val darkInteriorColor = when {
                isMinimalist -> {
                    if (palette.isAmoled) Color(0xFFDADCE2) else Color(0xFFCBD5E1)
                }
                wallpaperConfig.isCustomGalleryImage -> {
                    val secRgb = palette.accentSecondary.copy(alpha = 1f)
                    val harmony = lerp(accentRgb, secRgb, 0.30f)
                    lerp(Color(0xFFF0F4FA), harmony, if (palette.isAmoled) 0.46f else 0.42f)
                }
                isMono -> {
                    lerp(Color(0xFFF0F4FA), accentRgb, if (palette.isAmoled) 0.48f else 0.44f)
                }
                else -> {
                    val presetHue = lerp(Color(preset.accentOrbHex), Color(preset.primaryOrbHex), 0.35f)
                    val themeHarmony = lerp(accentRgb, presetHue, 0.38f)
                    lerp(Color(0xFFF1F5FB), themeHarmony, if (palette.isAmoled) 0.48f else 0.44f)
                }
            }

            val iR = darkInteriorColor.red
            val iG = darkInteriorColor.green
            val iB = darkInteriorColor.blue

            val matrix = ColorMatrix(
                floatArrayOf(
                    iR, 0f, 0f, 0f, 0f,
                    0f, iG, 0f, 0f, 0f,
                    0f, 0f, iB, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            ColorFilter.colorMatrix(matrix)
        }
    }
}

/**
 * A pin/unpin toggle that is waiting for the re-sorted conversation list to arrive.
 * Not snapshot state on purpose: it is only read and cleared from [SideEffect].
 */
private class PendingPinScroll(val threadId: Long, val wasPinned: Boolean)

private class PinScrollRequestHolder {
    var pending: PendingPinScroll? = null
    /** Clears the elevated zIndex of the travelling card once its move has settled. */
    var settleJob: Job? = null
}

/** Time the travelling card keeps its raised zIndex (covers the low-stiffness spring settle). */
private const val PIN_MOVE_SETTLE_MS = 750L

/** Slightly bouncy, slow placement spring used only for the card being pinned/unpinned. */
private val PinMovePlacementSpec = spring(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessLow,
    visibilityThreshold = IntOffset.VisibilityThreshold
)

@Composable
fun ConversationsListScreen(
    threads: List<ConversationThread>,
    isInitialLoading: Boolean = false,
    deletedCount: Int = 0,
    autoDeleteDays: Int = 30,
    showContactPhotos: Boolean = true,
    listLayoutMode: MainListLayoutMode = MainListLayoutMode.DEFAULT,
    mainWallpaper: WallpaperConfig = WallpaperConfig(),
    swipeRightAction: SwipeAction = SwipeAction.PIN,
    swipeLeftAction: SwipeAction = SwipeAction.DELETE,
    categoryOrder: List<ConversationFilter> = ConversationFilter.defaultOrder,
    onUpdateCategoryOrder: (List<ConversationFilter>) -> Unit = {},
    dockBarOrder: List<DockBarItem> = DockBarItem.defaultOrder,
    onUpdateDockBarOrder: (List<DockBarItem>) -> Unit = {},
    onOpenThread: (Long) -> Unit,
    onTogglePin: (Long) -> Unit,
    onRequestDeleteThread: (ConversationThread) -> Unit,
    onDeleteSelectedThreads: (Set<Long>) -> Unit = {},
    onMarkThreadRead: (Long) -> Unit = {},
    onMarkSelectedThreadsRead: (Set<Long>) -> Unit = {},
    onCallSender: (String) -> Unit,
    onOpenContact: (String, String) -> Unit,
    onCopyOtp: (String) -> Unit,
    onOpenNewMessage: () -> Unit,
    onOpenDeletedMessages: () -> Unit = {},
    onOpenHighlightedMessages: () -> Unit = {},
    onMarkAllRead: () -> Unit = {},
    onSelectionModeChange: (Boolean) -> Unit = {},
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val strings = TxturaStrings.current()
    val metrics = LocalAdaptiveMetrics.current
    val view = LocalView.current
    var searchQuery by remember { mutableStateOf(value = "") }
    var fullHistoryLoadedForSearch by remember { mutableStateOf(false) }
    LaunchedEffect(searchQuery.isNotBlank()) {
        if (searchQuery.isNotBlank() && !fullHistoryLoadedForSearch) {
            SmsMmsRepository.awaitInitialization()
            SmsMmsRepository.loadAllCachedThreadHistories(context)
            fullHistoryLoadedForSearch = true
        }
    }
    val activeCategories = remember(categoryOrder) {
        categoryOrder.ifEmpty { ConversationFilter.defaultOrder }
    }
    var selectedFilter by remember { mutableStateOf(value = activeCategories.firstOrNull() ?: ConversationFilter.PEOPLE) }
    var showMarkAllReadConfirmation by remember { mutableStateOf(value = false) }

    val totalHighlightedCount = remember(threads) {
        SmsMmsRepository.getHighlightedMessageIdsSnapshot().size
    }

    // Multi-selection mode triggered by long-pressing any conversation card on the main screen
    var isSelectionMode by remember { mutableStateOf(value = false) }
    var selectedThreadIds by remember { mutableStateOf<Set<Long>>(value = emptySet()) }
    var pendingBatchAction by remember { mutableStateOf<BatchSelectionAction?>(value = null) }

    LaunchedEffect(isSelectionMode) {
        onSelectionModeChange(isSelectionMode)
    }

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedThreadIds = emptySet()
    }

    val filteredThreads = remember(threads, searchQuery, selectedFilter) {
        val isBlankSearch = searchQuery.isBlank()
        val trimmedQuery = if (!isBlankSearch) searchQuery.trim() else ""
        threads.filter { thread ->
            val matchesFilter = when (selectedFilter) {
                ConversationFilter.PEOPLE -> !thread.isService
                ConversationFilter.SERVICES -> thread.isService
                ConversationFilter.UNREAD -> thread.unreadCount > 0
                ConversationFilter.ALL -> true
            }
            if (!matchesFilter) return@filter false

            if (isBlankSearch) {
                true
            } else {
                thread.contactName.contains(trimmedQuery, ignoreCase = true) ||
                    thread.address.contains(trimmedQuery, ignoreCase = true) ||
                    thread.messages.any { it.body.contains(trimmedQuery, ignoreCase = true) }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val palette = LocalGlassPalette.current

        // Confirmation dialog for "Označi sve kao pročitano" in the top bar
        if (showMarkAllReadConfirmation) {
            GlassAlertDialog(
                onDismissRequest = { showMarkAllReadConfirmation = false },
                accentBorderColor = palette.accentPrimary,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = TxturaIcons.CheckDouble,
                            contentDescription = null,
                            tint = palette.accentPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.confirmMarkAllReadTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.textPrimary
                        )
                    }
                },
                text = {
                    Text(
                        text = strings.confirmMarkAllReadMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textPrimary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showMarkAllReadConfirmation = false
                            onMarkAllRead()
                        }
                    ) {
                        Text(
                            text = strings.confirmMarkAllReadButton,
                            color = palette.accentPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showMarkAllReadConfirmation = false }) {
                        Text(strings.cancel, color = palette.textSecondary)
                    }
                }
            )
        }

        // Confirmation dialog for selection mode actions (Delete, Mark Read)
        val batchAction = pendingBatchAction
        if (batchAction != null) {
            val count = selectedThreadIds.size
            val titleText = when (batchAction) {
                BatchSelectionAction.DELETE -> strings.confirmBatchDeleteTitle
                BatchSelectionAction.MARK_READ -> strings.confirmBatchMarkReadTitle
            }
            val bodyText = when (batchAction) {
                BatchSelectionAction.DELETE -> strings.confirmBatchDeleteMessage(count, autoDeleteDays)
                BatchSelectionAction.MARK_READ -> strings.confirmBatchMarkReadMessage(count)
            }
            val accent = when (batchAction) {
                BatchSelectionAction.DELETE -> palette.dangerColor
                BatchSelectionAction.MARK_READ -> palette.accentPrimary
            }

            GlassAlertDialog(
                onDismissRequest = { pendingBatchAction = null },
                accentBorderColor = accent,
                title = {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.textPrimary
                    )
                },
                text = {
                    Text(
                        text = bodyText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textPrimary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pendingBatchAction = null
                            when (batchAction) {
                                BatchSelectionAction.DELETE -> {
                                    TxturaHaptics.reject(view, context)
                                    onDeleteSelectedThreads(selectedThreadIds)
                                    isSelectionMode = false
                                    selectedThreadIds = emptySet()
                                }
                                BatchSelectionAction.MARK_READ -> {
                                    TxturaHaptics.confirm(view, context)
                                    onMarkSelectedThreadsRead(selectedThreadIds)
                                    isSelectionMode = false
                                    selectedThreadIds = emptySet()
                                }
                            }
                        }
                    ) {
                        Text(
                            text = strings.confirm,
                            color = accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingBatchAction = null }) {
                        Text(strings.cancel, color = palette.textSecondary)
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Top Header: Centered "Txtura" branding (with bear icon for Burgundy Anita & Mono Burgundy themes)
            val isWhiteBackground = !palette.isDark ||
                (!mainWallpaper.isCustomGalleryImage && (mainWallpaper.preset == WallpaperPreset.ALL_WHITE))
            val logoColorFilter = rememberTxturaLightLogoColorFilter(
                palette = palette,
                wallpaperConfig = mainWallpaper,
                isWhiteBackground = isWhiteBackground
            )
            val isBurgundyTheme = !mainWallpaper.isCustomGalleryImage &&
                (mainWallpaper.preset == WallpaperPreset.MONO_BURGUNDY || mainWallpaper.preset == WallpaperPreset.BURGUNDY_ANITA)
            val bearResId = if (palette.isDark) R.drawable.bear_w else R.drawable.bear_b

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(
                            id = if (isWhiteBackground) R.drawable.txtw else R.drawable.txt
                        ),
                        contentDescription = "Txtura",
                        contentScale = ContentScale.Fit,
                        colorFilter = logoColorFilter,
                        modifier = Modifier
                            .height(34.dp)
                            .offset(y = (-3).dp)
                    )

                    if (isBurgundyTheme) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Image(
                            painter = painterResource(id = bearResId),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            colorFilter = logoColorFilter,
                            modifier = Modifier
                                .height(32.dp)
                                .offset(y = (-3).dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Glass Search Bar
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = TxturaIcons.Search,
                        contentDescription = strings.searchPlaceholder,
                        tint = palette.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = strings.searchPlaceholder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = palette.textPrimary
                            ),
                            cursorBrush = SolidColor(palette.accentPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = TxturaIcons.Close,
                            contentDescription = strings.clearSearch,
                            tint = palette.textSecondary,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val totalUnreadCount = remember(threads) { threads.sumOf { it.unreadCount } }

            // Single-line swipeable horizontal category bar.
            // Holding a category chip and dragging it left or right reorders categories live!
            DraggableCategoryBar(
                categories = activeCategories,
                selectedFilter = selectedFilter,
                strings = strings,
                totalUnreadCount = totalUnreadCount,
                onSelectFilter = { selectedFilter = it },
                onUpdateCategoryOrder = onUpdateCategoryOrder
            )

            Spacer(modifier = Modifier.height(10.dp))

            val density = LocalDensity.current
            val swipeThresholdPx = remember(density) { with(density) { 105.dp.toPx() } }
            val swipeMaxDragPx = remember(density) { with(density) { 165.dp.toPx() } }
            val listState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()

            val pinScrollRequest = remember { PinScrollRequestHolder() }
            // Card currently travelling after a pin/unpin: drawn above the others (zIndex) and
            // animated with PinMovePlacementSpec so it visibly flies over the neighbouring cards.
            var movingThreadId by remember { mutableStateOf<Long?>(null) }

            val handleTogglePin: (Long) -> Unit = { threadId ->
                val targetThread = filteredThreads.find { it.id == threadId } ?: threads.find { it.id == threadId }
                pinScrollRequest.settleJob?.cancel()
                movingThreadId = threadId
                pinScrollRequest.pending = PendingPinScroll(
                    threadId = threadId,
                    wasPinned = targetThread?.isPinned == true
                )
                onTogglePin(threadId)
            }

            // By default LazyColumn keeps its scroll position anchored to the KEY of the first
            // visible item. When the toggled card is (or becomes) that first item, the whole list
            // would jump along with it. Right before the re-sorted list is measured, we pin the
            // viewport to its current INDEX instead, so only the toggled card animates (via
            // animateItem placement) up to the top on pin, or down to its new slot on unpin.
            // Requesting the same index/offset keeps placement animations alive.
            SideEffect {
                val pending = pinScrollRequest.pending ?: return@SideEffect
                val target = filteredThreads.firstOrNull { it.id == pending.threadId }
                if (target == null) {
                    pinScrollRequest.pending = null
                    movingThreadId = null
                    return@SideEffect
                }
                // Re-sorted list has not arrived yet.
                if (target.isPinned == pending.wasPinned) return@SideEffect
                pinScrollRequest.pending = null

                val anchorIndex = listState.firstVisibleItemIndex
                val anchorOffset = listState.firstVisibleItemScrollOffset
                listState.requestScrollToItem(index = anchorIndex, scrollOffset = anchorOffset)

                pinScrollRequest.settleJob = coroutineScope.launch {
                    // Let the index-anchored remeasure (and the card's lift-off) happen first.
                    withFrameNanos { }
                    if (!pending.wasPinned) {
                        // PIN: follow the card as it climbs to the top of the list.
                        // (No-op when the list is already at the top.)
                        try {
                            listState.animateScrollToItem(0)
                        } catch (_: CancellationException) {
                            // Interrupted by a user drag; still clear the zIndex below.
                        }
                    }
                    // UNPIN: no scroll at all, the list stays put and only the card slides down.
                    delay(PIN_MOVE_SETTLE_MS)
                    if (movingThreadId == pending.threadId) movingThreadId = null
                }
            }

            // Conversations List
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(
                        if (listLayoutMode == MainListLayoutMode.COMPACT) (6 * metrics.scaleFactor).dp else (10 * metrics.scaleFactor).dp
                    ),
                    contentPadding = PaddingValues(bottom = (8 * metrics.scaleFactor).dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (filteredThreads.isEmpty() && isInitialLoading) {
                        item(key = "initial_message_loading", contentType = "initial_message_loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(144.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = palette.accentPrimary,
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                    items(
                        items = filteredThreads,
                        key = { it.id },
                        contentType = { "conversation_thread" }
                    ) { thread ->
                        val isChecked = thread.id in selectedThreadIds
                        val toggleSelection = {
                            TxturaHaptics.tick(view, context)
                            val updated = if (isChecked) {
                                selectedThreadIds - thread.id
                            } else {
                                selectedThreadIds + thread.id
                            }
                            selectedThreadIds = updated
                            if (updated.isEmpty()) {
                                isSelectionMode = false
                            }
                        }

                        val isExpressive = LocalExpressiveAnimations.current
                        val isMovingAfterPinToggle = movingThreadId == thread.id
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                // Travelling card flies over its neighbours instead of under them.
                                .zIndex(if (isMovingAfterPinToggle) 10f else 0f)
                                .fillMaxWidth()
                                .then(
                                    if (isExpressive) {
                                        Modifier.animateItem(
                                            fadeInSpec = null,
                                            fadeOutSpec = TxturaMotion.listItemFadeOutSpec(),
                                            placementSpec = if (isMovingAfterPinToggle) {
                                                PinMovePlacementSpec
                                            } else {
                                                TxturaMotion.listItemPlacementSpec()
                                            }
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            // Checkbox displayed next to each message window when in selection mode
                            AnimatedVisibility(visible = isSelectionMode) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    GlassSelectionCheckbox(
                                        checked = isChecked,
                                        onToggle = toggleSelection
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                }
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                SwipeableConversationCard(
                                    thread = thread,
                                    strings = strings,
                                    swipeRightAction = if (isSelectionMode) SwipeAction.NONE else swipeRightAction,
                                    swipeLeftAction = if (isSelectionMode) SwipeAction.NONE else swipeLeftAction,
                                    thresholdPx = swipeThresholdPx,
                                    maxDragPx = swipeMaxDragPx,
                                    onExecuteSwipeAction = { action ->
                                        when (action) {
                                            SwipeAction.PIN -> handleTogglePin(thread.id)
                                            SwipeAction.DELETE -> onRequestDeleteThread(thread)
                                            SwipeAction.CALL -> onCallSender(thread.address)
                                            SwipeAction.MARK_READ -> onMarkThreadRead(thread.id)
                                            SwipeAction.NONE -> Unit
                                        }
                                    }
                                ) {
                                    ConversationThreadCard(
                                        thread = thread,
                                        showContactPhotos = showContactPhotos,
                                        listLayoutMode = listLayoutMode,
                                        isSelected = isSelectionMode && isChecked,
                                        showPinAction = swipeRightAction != SwipeAction.PIN &&
                                            swipeLeftAction != SwipeAction.PIN,
                                        strings = strings,
                                        onClick = {
                                            if (isSelectionMode) {
                                                toggleSelection()
                                            } else {
                                                onOpenThread(thread.id)
                                            }
                                        },
                                        onLongClick = {
                                            TxturaHaptics.longPress(view, context)
                                            isSelectionMode = true
                                            selectedThreadIds += thread.id
                                        },
                                        onTogglePin = { handleTogglePin(thread.id) },
                                        onOpenContact = { onOpenContact(thread.address, thread.contactName) },
                                        onCopyOtp = onCopyOtp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Multi-Conversation Selection Action Bar (displayed at the bottom in selection mode)
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
            ) {
                val allSelected = filteredThreads.isNotEmpty() && filteredThreads.all { it.id in selectedThreadIds }
                val selCount = selectedThreadIds.size

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = (4 * metrics.scaleFactor).dp, bottom = metrics.safeBottomClearance),
                    shape = RoundedCornerShape(metrics.dockCornerRadius),
                    elevated = true,
                    accentHighlight = palette.accentPrimary,
                    contentPadding = PaddingValues(horizontal = (10 * metrics.scaleFactor).dp, vertical = (8 * metrics.scaleFactor).dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 1. Cancel / Deselect with counter chip (Close)
                        GlassCard(
                            shape = RoundedCornerShape(50),
                            elevated = true,
                            contentPadding = PaddingValues(horizontal = (10 * metrics.scaleFactor).dp, vertical = (6 * metrics.scaleFactor).dp),
                            onClick = {
                                isSelectionMode = false
                                selectedThreadIds = emptySet()
                            }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = TxturaIcons.Close,
                                    contentDescription = strings.cancel,
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size((18 * metrics.scaleFactor).dp)
                                )
                                Spacer(modifier = Modifier.width((6 * metrics.scaleFactor).dp))
                                Text(
                                    text = selCount.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.accentPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 2. Select All / Deselect All
                        GlassTooltipIconButton(
                            icon = if (allSelected) TxturaIcons.DeselectAll else TxturaIcons.SelectAll,
                            tooltipText = if (allSelected) {
                                strings.bottomBarDeselectAllTooltip
                            } else {
                                strings.bottomBarSelectAllTooltip
                            },
                            tint = if (allSelected) palette.accentPrimary else palette.safeColor,
                            pressedTint = palette.accentPrimary,
                            size = metrics.dockIconButtonSize,
                            iconSize = metrics.dockIconInnerSize,
                            onClick = {
                                if (allSelected) {
                                    selectedThreadIds = emptySet()
                                    isSelectionMode = false
                                } else if (filteredThreads.isNotEmpty()) {
                                    selectedThreadIds = filteredThreads.asSequence().map { it.id }.toSet()
                                }
                            }
                        )

                        // 3. Mark selected as read (Označi kao pročitano)
                        GlassTooltipIconButton(
                            icon = TxturaIcons.CheckDouble,
                            tooltipText = strings.bottomBarMarkReadTooltip,
                            tint = palette.accentPrimary,
                            pressedTint = palette.accentPrimary,
                            size = metrics.dockIconButtonSize,
                            iconSize = metrics.dockIconInnerSize,
                            onClick = {
                                if (selectedThreadIds.isNotEmpty()) {
                                    pendingBatchAction = BatchSelectionAction.MARK_READ
                                }
                            }
                        )

                        // 4. Delete selected threads (Izbriši)
                        GlassTooltipIconButton(
                            icon = TxturaIcons.Delete,
                            tooltipText = strings.bottomBarDeleteTooltip,
                            tint = palette.dangerColor,
                            pressedTint = palette.dangerColor,
                            size = metrics.dockIconButtonSize,
                            iconSize = metrics.dockIconInnerSize,
                            onClick = {
                                if (selectedThreadIds.isNotEmpty()) {
                                    pendingBatchAction = BatchSelectionAction.DELETE
                                }
                            }
                        )
                    }
                }
            }

            // Home Bottom Dock Bar (displayed at the bottom in regular mode)
            AnimatedVisibility(
                visible = !isSelectionMode,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
            ) {
                HomeBottomDockBar(
                    deletedCount = deletedCount,
                    highlightedCount = totalHighlightedCount,
                    strings = strings,
                    dockBarOrder = dockBarOrder,
                    onUpdateDockBarOrder = onUpdateDockBarOrder,
                    onOpenDeletedMessages = onOpenDeletedMessages,
                    onOpenHighlightedMessages = onOpenHighlightedMessages,
                    onOpenNewMessage = onOpenNewMessage,
                    onMarkAllRead = onMarkAllRead,
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = (4 * metrics.scaleFactor).dp, bottom = metrics.safeBottomClearance)
                )
            }
        }
    }
}

@Composable
internal fun HomeBottomDockBar(
    deletedCount: Int,
    highlightedCount: Int,
    strings: AppStrings,
    dockBarOrder: List<DockBarItem> = DockBarItem.defaultOrder,
    onUpdateDockBarOrder: (List<DockBarItem>) -> Unit = {},
    onOpenDeletedMessages: () -> Unit,
    onOpenHighlightedMessages: () -> Unit,
    onOpenNewMessage: () -> Unit,
    onMarkAllRead: () -> Unit,
    onOpenSettings: () -> Unit,
    activeItem: DockBarItem? = null,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    val metrics = LocalAdaptiveMetrics.current
    val view = LocalView.current
    val context = LocalContext.current

    var localOrder by remember { mutableStateOf(dockBarOrder) }
    var draggedItem by remember { mutableStateOf<DockBarItem?>(null) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    val itemWidthsPx = remember { mutableStateMapOf<DockBarItem, Int>() }

    LaunchedEffect(dockBarOrder) {
        if (draggedItem == null) {
            localOrder = dockBarOrder.ifEmpty { DockBarItem.defaultOrder }
        }
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Full-width Glass Bottom Dock Bar matching the rest of the interface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(metrics.bottomDockHeight)
                .clip(RoundedCornerShape(metrics.dockCornerRadius))
                .background(palette.popupContainerColor)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            palette.glassSurfaceElevated.copy(alpha = 0.96f),
                            palette.glassSurface.copy(alpha = 0.96f)
                        )
                    )
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(palette.glassBorderTop, palette.glassBorderBottom)
                    ),
                    shape = RoundedCornerShape(metrics.dockCornerRadius)
                )
                .padding(horizontal = (8 * metrics.scaleFactor).dp, vertical = (6 * metrics.scaleFactor).dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                localOrder.forEach { item ->
                    key(item) {
                        val isDragged = item == draggedItem
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .zIndex(if (isDragged) 10f else 0f)
                                .onSizeChanged { size ->
                                    itemWidthsPx[item] = size.width
                                }
                                .graphicsLayer {
                                    translationX = if (isDragged) dragOffsetX else 0f
                                    scaleX = if (isDragged) 1.15f else 1f
                                    scaleY = if (isDragged) 1.15f else 1f
                                }
                                .pointerInput(item) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            TxturaHaptics.longPress(view, context)
                                            draggedItem = item
                                            dragOffsetX = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetX += dragAmount.x
                                            val currentIndex = localOrder.indexOf(item)
                                            if (currentIndex != -1) {
                                                if (dragOffsetX > 0f && currentIndex < localOrder.lastIndex) {
                                                    val nextItem = localOrder[currentIndex + 1]
                                                    val nextWidth = (itemWidthsPx[nextItem] ?: 120).toFloat()
                                                    val threshold = nextWidth * 0.5f
                                                    if (dragOffsetX > threshold) {
                                                        val updated = localOrder.toMutableList().apply {
                                                            val itm = removeAt(currentIndex)
                                                            add(currentIndex + 1, itm)
                                                        }
                                                        localOrder = updated
                                                        dragOffsetX -= nextWidth
                                                        TxturaHaptics.tick(view, context)
                                                        onUpdateDockBarOrder(updated)
                                                    }
                                                } else if (dragOffsetX < 0f && currentIndex > 0) {
                                                    val prevItem = localOrder[currentIndex - 1]
                                                    val prevWidth = (itemWidthsPx[prevItem] ?: 120).toFloat()
                                                    val threshold = -(prevWidth * 0.5f)
                                                    if (dragOffsetX < threshold) {
                                                        val updated = localOrder.toMutableList().apply {
                                                            val itm = removeAt(currentIndex)
                                                            add(currentIndex - 1, itm)
                                                        }
                                                        localOrder = updated
                                                        dragOffsetX += prevWidth
                                                        TxturaHaptics.tick(view, context)
                                                        onUpdateDockBarOrder(updated)
                                                    }
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            draggedItem = null
                                            dragOffsetX = 0f
                                            TxturaHaptics.confirm(view, context)
                                            onUpdateDockBarOrder(localOrder)
                                        },
                                        onDragCancel = {
                                            draggedItem = null
                                            dragOffsetX = 0f
                                        }
                                    )
                                }
                        ) {
                            when (item) {
                                DockBarItem.MARK_ALL_READ -> {
                                    GlassTooltipIconButton(
                                        icon = TxturaIcons.CheckDouble,
                                        tooltipText = strings.markAllReadTooltip,
                                        tint = if (activeItem == item) palette.accentSecondary else palette.accentPrimary,
                                        onClick = onMarkAllRead,
                                        size = metrics.dockIconButtonSize,
                                        iconSize = metrics.dockIconInnerSize,
                                        enableTooltip = false
                                    )
                                }
                                DockBarItem.HIGHLIGHTED -> {
                                    Box {
                                        GlassTooltipIconButton(
                                            icon = TxturaIcons.HighlightStar,
                                            tooltipText = strings.highlightedMessagesTooltip,
                                            tint = if (highlightedCount > 0 || activeItem == item) palette.cautionColor else palette.accentPrimary,
                                            onClick = onOpenHighlightedMessages,
                                            size = metrics.dockIconButtonSize,
                                            iconSize = metrics.dockIconInnerSize,
                                            enableTooltip = false
                                        )
                                        if (highlightedCount > 0) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size((18 * metrics.scaleFactor).dp)
                                                    .clip(CircleShape)
                                                    .background(palette.cautionColor)
                                            ) {
                                                Text(
                                                    text = highlightedCount.coerceAtMost(99).toString(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.Black,
                                                    fontSize = (10 * metrics.scaleFactor).sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                                DockBarItem.NEW_MESSAGE -> {
                                    val newMsgInteractionSource = remember { MutableInteractionSource() }
                                    val isNewMsgPressed by newMsgInteractionSource.collectIsPressedAsState()
                                    val isExpressiveDock = LocalExpressiveAnimations.current
                                    val newMsgScale by animateFloatAsState(
                                        targetValue = if (isNewMsgPressed && isExpressiveDock) 0.89f else 1f,
                                        animationSpec = TxturaMotion.pressSpec(isNewMsgPressed),
                                        label = "newMsgButtonScale"
                                    )

                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(metrics.centerActionSize)
                                            .graphicsLayer {
                                                scaleX = newMsgScale
                                                scaleY = newMsgScale
                                            }
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        palette.accentPrimary,
                                                        palette.outgoingBubbleEnd
                                                    )
                                                )
                                            )
                                            .border(
                                                width = 1.5.dp,
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.White.copy(alpha = 0.75f),
                                                        palette.accentPrimary.copy(alpha = 0.90f)
                                                    )
                                                ),
                                                shape = CircleShape
                                            )
                                            .clickable(
                                                interactionSource = newMsgInteractionSource,
                                                indication = null,
                                                onClick = onOpenNewMessage
                                            )
                                    ) {
                                        Icon(
                                            imageVector = TxturaIcons.Plus,
                                            contentDescription = strings.newMessage,
                                            tint = if (palette.isDark) Color.Black else Color.White,
                                            modifier = Modifier.size(metrics.centerActionIconSize)
                                        )
                                    }
                                }
                                DockBarItem.DELETED -> {
                                    Box {
                                        GlassTooltipIconButton(
                                            icon = TxturaIcons.Delete,
                                            tooltipText = strings.deletedMessagesTitle,
                                            tint = if (activeItem == item) palette.cautionColor else palette.accentPrimary,
                                            onClick = onOpenDeletedMessages,
                                            size = metrics.dockIconButtonSize,
                                            iconSize = metrics.dockIconInnerSize,
                                            enableTooltip = false
                                        )
                                        if (deletedCount > 0) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size((18 * metrics.scaleFactor).dp)
                                                    .clip(CircleShape)
                                                    .background(palette.accentPrimary)
                                            ) {
                                                Text(
                                                    text = deletedCount.coerceAtMost(99).toString(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (palette.isDark) Color.Black else Color.White,
                                                    fontSize = (10 * metrics.scaleFactor).sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                                DockBarItem.SETTINGS -> {
                                    GlassTooltipIconButton(
                                        icon = TxturaIcons.SettingsGear,
                                        tooltipText = strings.settingsTooltip,
                                        tint = if (activeItem == item) palette.accentSecondary else palette.accentPrimary,
                                        onClick = onOpenSettings,
                                        size = metrics.dockIconButtonSize,
                                        iconSize = metrics.dockIconInnerSize,
                                        enableTooltip = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class HighlightedMessageEntry(
    val thread: ConversationThread,
    val message: ChatMessage
)

@Composable
fun HighlightedMessagesScreen(
    threads: List<ConversationThread>,
    mainWallpaper: WallpaperConfig = WallpaperConfig(),
    onBack: () -> Unit,
    onOpenThread: (Long) -> Unit,
    onToggleHighlight: (Long, Long) -> Unit,
    deletedCount: Int = 0,
    dockBarOrder: List<DockBarItem> = DockBarItem.defaultOrder,
    onUpdateDockBarOrder: (List<DockBarItem>) -> Unit = {},
    onOpenDeletedMessages: () -> Unit = {},
    onOpenNewMessage: () -> Unit = {},
    onMarkAllRead: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val palette = LocalGlassPalette.current
    val metrics = LocalAdaptiveMetrics.current
    val strings = TxturaStrings.current()

    val highlightedItems = remember(threads) {
        threads.flatMap { thread ->
            thread.messages.filter { it.isHighlighted }.map { message ->
                HighlightedMessageEntry(thread = thread, message = message)
            }
        }.sortedByDescending { it.message.timestampMillis }
    }

    BackHandler(onBack = onBack)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Top Header Bar
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevated = true,
                accentHighlight = palette.cautionColor,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        GlassIconButton(
                            icon = TxturaIcons.ArrowBack,
                            contentDescription = strings.back,
                            tint = palette.textPrimary,
                            onClick = onBack,
                            size = 38.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = TxturaIcons.HighlightStar,
                            contentDescription = null,
                            tint = palette.cautionColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.allHighlightedMessagesTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(palette.cautionColor.copy(alpha = 0.2f))
                            .border(1.dp, palette.cautionColor.copy(alpha = 0.6f), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${highlightedItems.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.cautionColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (highlightedItems.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(0.92f),
                        shape = RoundedCornerShape(24.dp),
                        elevated = true,
                        accentHighlight = palette.cautionColor,
                        contentPadding = PaddingValues(24.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = TxturaIcons.HighlightStar,
                                contentDescription = null,
                                tint = palette.cautionColor,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = strings.noHighlightedMessages,
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = strings.noHighlightedMessagesSub,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy((10 * metrics.scaleFactor).dp),
                    contentPadding = PaddingValues(bottom = (16 * metrics.scaleFactor).dp + metrics.safeBottomClearance),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(
                        items = highlightedItems,
                        key = { "${it.thread.id}_${it.message.id}" }
                    ) { item ->
                        val thread = item.thread
                        val message = item.message
                        val avatarColor = Color(thread.avatarAccentColorHex)

                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            elevated = true,
                            accentHighlight = palette.cautionColor,
                            contentPadding = PaddingValues(14.dp),
                            onClick = { onOpenThread(thread.id) }
                        ) {
                            Column {
                                // Contact Header Row
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.radialGradient(
                                                        colors = listOf(
                                                            avatarColor.copy(alpha = 0.35f),
                                                            avatarColor.copy(alpha = 0.10f)
                                                        )
                                                    )
                                                )
                                                .border(1.dp, avatarColor.copy(alpha = 0.65f), CircleShape)
                                        ) {
                                            Text(
                                                text = thread.avatarInitials,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (palette.isDark) Color.White else palette.textPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = thread.contactName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = palette.textPrimary,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = thread.address,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.textSecondary
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = formatTimestamp(message.timestampMillis),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.textSecondary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = TxturaIcons.HighlightStar,
                                            contentDescription = strings.unhighlightMessageAction,
                                            tint = palette.cautionColor,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clickable {
                                                    onToggleHighlight(thread.id, message.id)
                                                }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Message content preview
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    palette.cautionColor.copy(alpha = 0.18f),
                                                    palette.glassSurfaceElevated
                                                )
                                            )
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = palette.cautionColor.copy(alpha = 0.45f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        if (message.attachment != null) {
                                            MmsAttachmentCard(
                                                uri = message.attachment.uri,
                                                caption = message.attachment.caption,
                                                hdrLabel = strings.hdrMmsPhoto
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                        }
                                        Text(
                                            text = message.body,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = palette.textPrimary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Footer: "Otvori u razgovoru →"
                                Row(
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = strings.openInChatAction + " →",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.accentPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassSelectionCheckbox(
    checked: Boolean,
    onToggle: () -> Unit
) {
    val palette = LocalGlassPalette.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (checked) palette.accentPrimary
                else palette.glassSurfaceElevated
            )
            .border(
                width = 1.5.dp,
                color = if (checked) palette.accentPrimary else palette.textSecondary.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onToggle)
    ) {
        if (checked) {
            Icon(
                imageVector = TxturaIcons.Check,
                contentDescription = null,
                tint = if (palette.isDark) Color.Black else Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Single-line horizontal category bar that supports:
 * - Normal left/right swipe scrolling
 * - Tap to select active category
 * - Long-press (hold) + drag left or right to directly reorder the category chips and save as default order!
 */
@Composable
private fun DraggableCategoryBar(
    categories: List<ConversationFilter>,
    selectedFilter: ConversationFilter,
    strings: AppStrings,
    totalUnreadCount: Int = 0,
    onSelectFilter: (ConversationFilter) -> Unit,
    onUpdateCategoryOrder: (List<ConversationFilter>) -> Unit
) {
    val palette = LocalGlassPalette.current
    val density = LocalDensity.current
    val view = LocalView.current
    val context = LocalContext.current
    val spacingPx = remember(density) { with(density) { 6.dp.toPx() } }

    var localOrder by remember { mutableStateOf(categories) }
    var draggedFilter by remember { mutableStateOf<ConversationFilter?>(null) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    val itemWidthsPx = remember { mutableStateMapOf<ConversationFilter, Int>() }

    LaunchedEffect(categories) {
        if (draggedFilter == null) {
            localOrder = categories
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        localOrder.forEach { filter ->
            key(filter) {
                val selected = filter == selectedFilter
                val isDragged = filter == draggedFilter
                val label = filterLabel(filter, strings, totalUnreadCount)

                val itemWeight = when (filter) {
                    ConversationFilter.UNREAD -> if (totalUnreadCount > 0) 1.55f else 1.32f
                    ConversationFilter.ALL -> 0.68f
                    else -> 1.0f
                }

                Box(
                    modifier = Modifier
                        .weight(itemWeight)
                        .zIndex(if (isDragged) 10f else 0f)
                        .onSizeChanged { size ->
                            itemWidthsPx[filter] = size.width
                        }
                        .graphicsLayer {
                            translationX = if (isDragged) dragOffsetX else 0f
                            scaleX = if (isDragged) 1.08f else 1f
                            scaleY = if (isDragged) 1.08f else 1f
                        }
                        .pointerInput(filter) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    TxturaHaptics.longPress(view, context)
                                    draggedFilter = filter
                                    dragOffsetX = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetX += dragAmount.x
                                    val currentIndex = localOrder.indexOf(filter)
                                    if (currentIndex != -1) {
                                        if ((dragOffsetX > 0f) && (currentIndex < localOrder.lastIndex)) {
                                            val nextFilter = localOrder[currentIndex + 1]
                                            val nextWidth = (itemWidthsPx[nextFilter] ?: 160).toFloat()
                                            val threshold = (nextWidth * 0.55f) + spacingPx
                                            if (dragOffsetX > threshold) {
                                                val updated = localOrder.toMutableList().apply {
                                                    val item = removeAt(currentIndex)
                                                    add(currentIndex + 1, item)
                                                }
                                                localOrder = updated
                                                dragOffsetX -= (nextWidth + spacingPx)
                                                TxturaHaptics.tick(view, context)
                                                onUpdateCategoryOrder(updated)
                                            }
                                        } else if ((dragOffsetX < 0f) && (currentIndex > 0)) {
                                            val prevFilter = localOrder[currentIndex - 1]
                                            val prevWidth = (itemWidthsPx[prevFilter] ?: 160).toFloat()
                                            val threshold = -((prevWidth * 0.55f) + spacingPx)
                                            if (dragOffsetX < threshold) {
                                                val updated = localOrder.toMutableList().apply {
                                                    val item = removeAt(currentIndex)
                                                    add(currentIndex - 1, item)
                                                }
                                                localOrder = updated
                                                dragOffsetX += (prevWidth + spacingPx)
                                                TxturaHaptics.tick(view, context)
                                                onUpdateCategoryOrder(updated)
                                            }
                                        }
                                    }
                                },
                                onDragEnd = {
                                    draggedFilter = null
                                    dragOffsetX = 0f
                                    TxturaHaptics.confirm(view, context)
                                    onUpdateCategoryOrder(localOrder)
                                },
                                onDragCancel = {
                                    draggedFilter = null
                                    dragOffsetX = 0f
                                }
                            )
                        }
                ) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(50),
                        elevated = selected || isDragged,
                        accentHighlight = when {
                            isDragged -> palette.safeColor
                            selected -> palette.accentPrimary
                            else -> null
                        },
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp),
                        onClick = {
                            TxturaHaptics.click(view, context)
                            onSelectFilter(filter)
                        }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = if (label.length >= 13) 10.5.sp else if (label.length >= 10) 11.sp else 12.sp
                            ),
                            color = when {
                                isDragged -> palette.safeColor
                                selected -> palette.accentPrimary
                                else -> palette.textSecondary
                            },
                            fontWeight = if (selected || isDragged) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}

fun filterLabel(filter: ConversationFilter, strings: AppStrings, unreadCount: Int = 0): String {
    return when (filter) {
        ConversationFilter.PEOPLE -> strings.filterPeople
        ConversationFilter.SERVICES -> strings.filterServices
        ConversationFilter.UNREAD -> {
            if (unreadCount > 0) {
                "${strings.filterUnread} ($unreadCount)"
            } else {
                strings.filterUnread
            }
        }
        ConversationFilter.ALL -> strings.filterAll
    }
}

private enum class SettingsCategoryWindow {
    APPEARANCE_AND_DISPLAY,
    NOTIFICATIONS_SOUNDS_HAPTICS,
    CHAT_AND_GESTURES,
    PRIVACY_AND_SECURITY,
    DATA_AND_STORAGE,
    MESSAGING_AND_NETWORK,
    SYSTEM_AND_ABOUT,
}

/**
 * Single Active Dropdown / Collapsible Coordinator:
 * Enforces that at most one dropdown menu or expandable card can be open at any given time across the UI.
 */
val LocalActiveDropdownId = compositionLocalOf<MutableState<String?>> {
    mutableStateOf(null)
}

/**
 * Dedicated Settings Screen ("Settings" / "Podešavanja") organized into modern category windows.
 * Clicking on any category opens that category in its own full-screen window (rather than an inline dropdown menu).
 */
@Composable
fun SettingsScreen(
    themeMode: TxturaThemeMode,
    uiOpacity: Float = 0.50f,
    showContactPhotos: Boolean,
    listLayoutMode: MainListLayoutMode = MainListLayoutMode.DEFAULT,
    mainWallpaper: WallpaperConfig = WallpaperConfig(),
    uiColorMode: UiColorMode = UiColorMode.COLORFUL,
    swipeRightAction: SwipeAction,
    swipeLeftAction: SwipeAction,
    autoDeleteDays: Int,
    deletedGroupingMode: DeletedGroupingMode,
    deletedMessagesCount: Int,
    recallMessagesEnabled: Boolean = true,
    identifyMaliciousWebsites: Boolean = true,
    inAppSoundsEnabled: Boolean = true,
    onToggleInAppSounds: (Boolean) -> Unit = {},
    pinchToZoomEnabled: Boolean = true,
    onTogglePinchToZoom: (Boolean) -> Unit = {},
    expressiveAnimationsEnabled: Boolean = true,
    onToggleExpressiveAnimations: (Boolean) -> Unit = {},
    flashAlertEnabled: Boolean = false,
    onToggleFlashAlert: (Boolean) -> Unit = {},
    hapticsEnabled: Boolean = true,
    onToggleHaptics: (Boolean) -> Unit = {},
    smsNotificationSound: SmsNotificationSound = SmsNotificationSound.SYSTEM,
    onSelectSmsNotificationSound: (SmsNotificationSound) -> Unit = {},
    bubblesEnabled: Boolean = true,
    onToggleBubbles: (Boolean) -> Unit = {},
    onOpenSystemBubbleSettings: () -> Unit = {},
    autoPreviewsShowAll: Boolean = true,
    onToggleAutoPreviewsShowAll: (Boolean) -> Unit = {},
    autoPreviewsWebLinksOnly: Boolean = false,
    onToggleAutoPreviewsWebLinksOnly: (Boolean) -> Unit = {},
    autoPreviewsWifiOnly: Boolean = false,
    onToggleAutoPreviewsWifiOnly: (Boolean) -> Unit = {},
    blockedSenders: List<String> = emptyList(),
    lastBackupTimestamp: Long = 0L,
    hasSmsPermission: Boolean,
    hasMicrophonePermission: Boolean = false,
    hasContactsPermission: Boolean = false,
    hasNotificationPermission: Boolean = true,
    isDefaultSmsApp: Boolean,
    rationaleMessage: String?,
    onBack: () -> Unit,
    onSelectTheme: (TxturaThemeMode) -> Unit,
    onUpdateUiOpacity: (Float) -> Unit = {},
    onToggleShowContactPhotos: (Boolean) -> Unit,
    onSelectListLayoutMode: (MainListLayoutMode) -> Unit = {},
    onSelectUiColorMode: (UiColorMode) -> Unit = {},
    onSelectMainWallpaperPreset: (WallpaperPreset) -> Unit = {},
    onSelectMainWallpaperTexture: (WallpaperTexture) -> Unit = {},
    onSelectMainWallpaperAccentColor: (Long?) -> Unit = {},
    onPickMainWallpaperFromGallery: () -> Unit = {},
    onSelectSwipeRightAction: (SwipeAction) -> Unit,
    onSelectSwipeLeftAction: (SwipeAction) -> Unit,
    onSelectAutoDeleteDays: (Int) -> Unit,
    onSelectDeletedGroupingMode: (DeletedGroupingMode) -> Unit,
    onToggleRecallMessages: (Boolean) -> Unit = {},
    onToggleIdentifyMaliciousWebsites: (Boolean) -> Unit = {},
    onOpenDeletedMessages: () -> Unit,
    onUnblockSender: (String) -> Unit = {},
    onCreateLocalBackup: () -> Unit = {},
    onRestoreLocalBackup: () -> Unit = {},
    onShareBackupFile: () -> Unit = {},
    onRequestSmsPermissions: () -> Unit,
    onRequestMicrophonePermission: () -> Unit = {},
    onRequestContactsPermission: () -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    onRequestDefaultSmsRole: () -> Unit,
    onOpenSystemAppSettings: () -> Unit,
    onLockAppNow: () -> Unit = {},
    focusUpdateSection: Boolean = false,
    onUpdateSectionFocused: () -> Unit = {},
) {
    val context = LocalContext.current
    val strings = TxturaStrings.current()
    val (appVersionName, appVersionCode) = remember(context) {
        runCatching {
            val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val vName = pkgInfo.versionName ?: "3.3.1"
            val vCode = pkgInfo.longVersionCode
            vName to vCode
        }.getOrDefault("3.3.1" to 12L)
    }

    val coroutineScope = rememberCoroutineScope()
    var cachedReleaseInfo by remember(context, appVersionName) {
        mutableStateOf(AppUpdateManager.getCachedReleaseInfo(context, appVersionName))
    }
    var lastUpdateCheckMs by remember(context) {
        mutableLongStateOf(AppUpdateManager.getLastCheckTimestamp(context))
    }
    var isCheckingUpdates by remember { mutableStateOf(false) }
    var downloadProgressPct by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(context, appVersionName) {
        if (AppUpdateManager.shouldPerformPeriodicCheck(context)) {
            isCheckingUpdates = true
            when (val res = AppUpdateManager.checkForUpdates(context, appVersionName)) {
                is AppUpdateCheckResult.Success -> {
                    cachedReleaseInfo = res.info
                    lastUpdateCheckMs = AppUpdateManager.getLastCheckTimestamp(context)
                }
                else -> Unit
            }
            isCheckingUpdates = false
        }
    }

    // Active category window (null = main Settings categories overview window)
    var activeCategoryWindow by remember { mutableStateOf<SettingsCategoryWindow?>(null) }
    var targetSecurityHighlight by remember { mutableStateOf<String?>(null) }
    var showSecurityMeasuresDialog by remember { mutableStateOf(false) }
    var showChangelogDialog by remember { mutableStateOf(false) }
    var showSmscDialog by remember { mutableStateOf(false) }
    var showSimMessagesDialog by remember { mutableStateOf(false) }
    var showMmsApnManualDialog by remember { mutableStateOf(false) }
    var showEmergencyAlertsDialog by remember { mutableStateOf(false) }
    var showAddBlockSenderDialog by remember { mutableStateOf(false) }
    var newBlockAddressText by remember { mutableStateOf("") }
    var smscAddress by remember { mutableStateOf(SmsMmsRepository.getSmscAddress(context)) }
    var currentMmsApn by remember { mutableStateOf(SmsMmsRepository.getMmsApnSettings(context)) }

    LaunchedEffect(targetSecurityHighlight) {
        if (targetSecurityHighlight != null) {
            delay(4000.milliseconds)
            targetSecurityHighlight = null
        }
    }

    LaunchedEffect(focusUpdateSection) {
        if (focusUpdateSection) {
            activeCategoryWindow = SettingsCategoryWindow.SYSTEM_AND_ABOUT
        }
    }

    BackHandler(enabled = activeCategoryWindow != null) {
        activeCategoryWindow = null
    }

    val activeDropdownId = rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(activeCategoryWindow) {
        activeDropdownId.value = null
    }

    CompositionLocalProvider(LocalActiveDropdownId provides activeDropdownId) {
        Box(modifier = Modifier.fillMaxSize()) {
            val palette = LocalGlassPalette.current

            val isExpressive = LocalExpressiveAnimations.current

        AnimatedContent(
            targetState = activeCategoryWindow,
            transitionSpec = {
                val forward = targetState != null
                TxturaMotion.screenTransition(forward = forward, isExpressive = isExpressive)
            },
            label = "SettingsWindowTransition",
            modifier = Modifier.fillMaxSize()
        ) { currentWindow ->
            val windowScrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                if (currentWindow == null) {
                    // Main Settings Top Bar with Security Shield Icon in Top-Right Corner
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        elevated = true,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                GlassIconButton(
                                    icon = TxturaIcons.ArrowBack,
                                    contentDescription = strings.back,
                                    onClick = onBack,
                                    size = 38.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = strings.settingsTooltip,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = palette.textPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            GlassTooltipIconButton(
                                icon = TxturaIcons.ShieldCheck,
                                tooltipText = strings.securitySectionTitle,
                                tint = palette.accentPrimary,
                                pressedTint = palette.safeColor,
                                onClick = { showSecurityMeasuresDialog = true }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // List of Settings Category Cards — each opens as a new window on click
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(windowScrollState)
                            .padding(bottom = 24.dp)
                    ) {
                        // 1. Appearance & Display (Izgled i prilagođavanje)
                        SettingsCategoryWindowCard(
                            icon = TxturaIcons.SparklesTheme,
                            iconTint = palette.accentPrimary,
                            title = strings.settingsCategoryAppearance,
                            accentHighlight = null,
                        ) { activeCategoryWindow = SettingsCategoryWindow.APPEARANCE_AND_DISPLAY }

                        // 2. Notifications, Sounds & Haptics (Obaveštenja, zvuk i odziv)
                        SettingsCategoryWindowCard(
                            icon = TxturaIcons.NotificationBell,
                            iconTint = palette.accentPrimary,
                            title = strings.settingsCategoryNotifications,
                            accentHighlight = null,
                        ) { activeCategoryWindow = SettingsCategoryWindow.NOTIFICATIONS_SOUNDS_HAPTICS }

                        // 3. Chat & Gestures (Ponašanje četa i interakcije)
                        SettingsCategoryWindowCard(
                            icon = TxturaIcons.SwipeHoriz,
                            iconTint = palette.accentPrimary,
                            title = strings.settingsCategoryChat,
                            accentHighlight = null,
                        ) { activeCategoryWindow = SettingsCategoryWindow.CHAT_AND_GESTURES }

                        // 4. Privacy & Security (Privatnost i bezbednost)
                        SettingsCategoryWindowCard(
                            icon = TxturaIcons.SecurityLock,
                            iconTint = palette.accentPrimary,
                            title = strings.settingsCategoryPrivacy,
                            accentHighlight = null,
                        ) { activeCategoryWindow = SettingsCategoryWindow.PRIVACY_AND_SECURITY }

                        // 5. Data & Storage (Podaci i skladište)
                        SettingsCategoryWindowCard(
                            icon = TxturaIcons.FolderBackup,
                            iconTint = palette.accentPrimary,
                            title = strings.settingsCategoryData,
                            accentHighlight = null,
                        ) { activeCategoryWindow = SettingsCategoryWindow.DATA_AND_STORAGE }

                        // 6. Messaging & Network (SMS i MMS podešavanja)
                        SettingsCategoryWindowCard(
                            icon = TxturaIcons.CellularNetwork,
                            iconTint = palette.accentPrimary,
                            title = strings.settingsCategoryMessaging,
                            accentHighlight = null,
                        ) { activeCategoryWindow = SettingsCategoryWindow.MESSAGING_AND_NETWORK }

                        // 7. System & About (Sistem i aplikacija)
                        SettingsCategoryWindowCard(
                            icon = TxturaIcons.PermissionsKey,
                            iconTint = palette.accentPrimary,
                            title = strings.settingsCategorySystem,
                            accentHighlight = if (!hasSmsPermission || !isDefaultSmsApp || (cachedReleaseInfo?.isUpdateAvailable == true)) palette.accentPrimary else null,
                        ) { activeCategoryWindow = SettingsCategoryWindow.SYSTEM_AND_ABOUT }
                    }

                    // Official Centered App Footer at the bottom of Settings
                    val isWhiteBackground = !palette.isDark ||
                        (!mainWallpaper.isCustomGalleryImage && (mainWallpaper.preset == WallpaperPreset.ALL_WHITE))
                    val footerLogoColorFilter = rememberTxturaLightLogoColorFilter(
                        palette = palette,
                        wallpaperConfig = mainWallpaper,
                        isWhiteBackground = isWhiteBackground
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 14.dp)
                    ) {
                        Image(
                            painter = painterResource(
                                id = if (isWhiteBackground) R.drawable.txtw else R.drawable.txt
                            ),
                            contentDescription = "Txtura GitHub Repository",
                            contentScale = ContentScale.Fit,
                            colorFilter = footerLogoColorFilter,
                            modifier = Modifier
                                .height(26.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    AppUpdateManager.openGitHubRepoInBrowser(context)
                                }
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = strings.settingsFooterVersion(appVersionName, appVersionCode),
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textPrimary.copy(alpha = 0.88f),
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    AppUpdateManager.openGitHubRepoInBrowser(context)
                                }
                        )

                        val footerBadgeAnnotated = remember(strings.settingsFooterPrivacyBadge) {
                            buildAnnotatedString {
                                appendInlineContent("footerShield", "[✓]")
                                append(" ")
                                append(strings.settingsFooterPrivacyBadge)
                            }
                        }
                        val footerInlineContent = remember(palette.safeColor) {
                            mapOf(
                                "footerShield" to InlineTextContent(
                                    Placeholder(
                                        width = 12.sp,
                                        height = 12.sp,
                                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                                    ),
                                ) {
                                    Icon(
                                        imageVector = TxturaIcons.ShieldCheck,
                                        contentDescription = null,
                                        tint = palette.safeColor,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                },
                            )
                        }
                        Text(
                            text = footerBadgeAnnotated,
                            inlineContent = footerInlineContent,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = palette.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                        )

                        Text(
                            text = strings.settingsFooterCopyright,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = palette.textSecondary.copy(alpha = 0.72f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    val (windowIcon, windowTint, windowTitle) = when (currentWindow) {
                        SettingsCategoryWindow.APPEARANCE_AND_DISPLAY -> Triple(
                            TxturaIcons.SparklesTheme,
                            palette.accentPrimary,
                            strings.settingsCategoryAppearance
                        )
                        SettingsCategoryWindow.NOTIFICATIONS_SOUNDS_HAPTICS -> Triple(
                            TxturaIcons.ChatBubble,
                            palette.accentPrimary,
                            strings.settingsCategoryNotifications
                        )
                        SettingsCategoryWindow.CHAT_AND_GESTURES -> Triple(
                            TxturaIcons.SwipeHoriz,
                            palette.accentPrimary,
                            strings.settingsCategoryChat
                        )
                        SettingsCategoryWindow.PRIVACY_AND_SECURITY -> Triple(
                            TxturaIcons.ShieldCheck,
                            palette.accentPrimary,
                            strings.settingsCategoryPrivacy
                        )
                        SettingsCategoryWindow.DATA_AND_STORAGE -> Triple(
                            TxturaIcons.FolderBackup,
                            palette.accentPrimary,
                            strings.settingsCategoryData
                        )
                        SettingsCategoryWindow.MESSAGING_AND_NETWORK -> Triple(
                            TxturaIcons.Send,
                            palette.accentPrimary,
                            strings.settingsCategoryMessaging
                        )
                        SettingsCategoryWindow.SYSTEM_AND_ABOUT -> Triple(
                            TxturaIcons.PermissionsKey,
                            palette.accentPrimary,
                            strings.settingsCategorySystem
                        )
                    }

                    // Dedicated Category Window Top Bar
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        elevated = true,
                        accentHighlight = windowTint,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            GlassIconButton(
                                icon = TxturaIcons.ArrowBack,
                                contentDescription = strings.back,
                                onClick = { activeCategoryWindow = null },
                                size = 38.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(windowTint.copy(alpha = 0.14f))
                                    .border(1.dp, windowTint.copy(alpha = 0.45f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = windowIcon,
                                    contentDescription = null,
                                    tint = windowTint,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = windowTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dedicated Category Window Body
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(windowScrollState)
                            .padding(bottom = 24.dp)
                    ) {
                        when (currentWindow) {
                            SettingsCategoryWindow.APPEARANCE_AND_DISPLAY -> {
                                // 1A & 1B. Spojena kartica: Providnost interfejsa i Režim prikaza teme
                                val clampedOpacity = uiOpacity.coerceIn(0.0f, 1.0f)
                                val transparencyPercent = (clampedOpacity * 100f).roundToInt().coerceIn(0, 100)
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        // 1A. Providnost i zamućivanje korisničkog interfejsa
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = strings.uiTransparencyTitle,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = palette.textPrimary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(palette.glassSurfaceElevated)
                                                    .border(
                                                        0.8.dp,
                                                        palette.glassBorderTop.copy(alpha = 0.45f),
                                                        RoundedCornerShape(50)
                                                    )
                                                    .padding(horizontal = 9.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = "$transparencyPercent%",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = palette.accentPrimary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        GlassModernSlider(
                                            value = clampedOpacity,
                                            onValueChange = { onUpdateUiOpacity(it.coerceIn(0.0f, 1.0f)) },
                                            valueRange = 0.0f..1.0f,
                                            tickFractions = listOf(0f, 0.25f, 0.5f, 0.75f, 1f),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        val pctMarks = listOf(0, 25, 50, 75, 100)
                                        Layout(
                                            modifier = Modifier.fillMaxWidth(),
                                            content = {
                                                pctMarks.forEach { pct ->
                                                    val isCurrentPct = abs(transparencyPercent - pct) <= 3
                                                    Text(
                                                        text = "$pct%",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontSize = 10.sp,
                                                        textAlign = TextAlign.Center,
                                                        color = if (isCurrentPct) {
                                                            palette.accentPrimary
                                                        } else {
                                                            palette.textSecondary.copy(alpha = 0.65f)
                                                        },
                                                        fontWeight = if (isCurrentPct) FontWeight.Bold else FontWeight.Normal,
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .clickable { onUpdateUiOpacity(pct / 100f) }
                                                            .padding(horizontal = 3.dp, vertical = 2.dp)
                                                    )
                                                }
                                            },
                                        ) { measurables, constraints ->
                                            val itemConstraints = constraints.copy(minWidth = 0)
                                            val placeables = measurables.map { it.measure(itemConstraints) }
                                            val height = placeables.maxOfOrNull { it.height } ?: 0
                                            val totalWidth = constraints.maxWidth
                                            val trackInsetPx = 14.dp.toPx()
                                            val usableTrackWidth = (totalWidth - trackInsetPx * 2f).coerceAtLeast(1f)

                                            layout(totalWidth, height) {
                                                placeables.forEachIndexed { index, placeable ->
                                                    val fraction = pctMarks[index] / 100f
                                                    val tickCenterX = trackInsetPx + usableTrackWidth * fraction
                                                    val x = (tickCenterX - placeable.width * 0.5f)
                                                        .roundToInt()
                                                        .coerceIn(0, (totalWidth - placeable.width).coerceAtLeast(0))
                                                    val y = (height - placeable.height) / 2
                                                    placeable.placeRelative(x, y)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(0.8.dp)
                                                .background(palette.glassBorderTop.copy(alpha = 0.28f))
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))

                                        // 1B. Režim teme (Auto, Light, Dark, AMOLED)
                                        Text(
                                            text = strings.subSettingThemeModeTitle,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = palette.textPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            TxturaThemeMode.entries.forEach { mode ->
                                                val selected = mode == themeMode
                                                val dotBrush = when (mode) {
                                                    TxturaThemeMode.SYSTEM -> Brush.linearGradient(
                                                        colors = listOf(
                                                            Color(0xFFF8FAFC),
                                                            Color(0xFFF8FAFC),
                                                            Color(0xFF28364D),
                                                            Color(0xFF28364D)
                                                        )
                                                    )
                                                    TxturaThemeMode.LIGHT -> SolidColor(Color(0xFFF8FAFC))
                                                    TxturaThemeMode.DARK -> SolidColor(Color(0xFF28364D))
                                                    TxturaThemeMode.AMOLED -> SolidColor(Color.Black)
                                                }
                                                GlassCard(
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp),
                                                    elevated = selected,
                                                    accentHighlight = if (selected) palette.accentPrimary else null,
                                                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
                                                    onClick = { onSelectTheme(mode) }
                                                ) {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(16.dp)
                                                                .clip(CircleShape)
                                                                .background(brush = dotBrush, shape = CircleShape)
                                                                .border(
                                                                    1.dp,
                                                                    if (selected) palette.accentPrimary else palette.textSecondary.copy(alpha = 0.5f),
                                                                    CircleShape
                                                                )
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = mode.label,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (selected) palette.accentPrimary else palette.textSecondary,
                                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                            textAlign = TextAlign.Center,
                                                            maxLines = 1,
                                                            softWrap = false,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                val activeDropdown = LocalActiveDropdownId.current
                                val themesExpanded = activeDropdown.value == "themes"
                                val texturesExpanded = activeDropdown.value == "textures"

                                val themesRotation by animateFloatAsState(
                                    targetValue = if (themesExpanded) 180f else 0f,
                                    animationSpec = TxturaMotion.rotateSpec(),
                                    label = "themesRotation"
                                )
                                val currentPresetSubtitle = if (mainWallpaper.isCustomGalleryImage) {
                                    strings.customGalleryWallpaperActive
                                } else {
                                    wallpaperPresetTitle(mainWallpaper.preset, strings)
                                }

                                // 1C. Pozadine i teme (Themes & Backgrounds)
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    elevated = themesExpanded,
                                    accentHighlight = if (themesExpanded) palette.accentPrimary else null,
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                                    onClick = {
                                        activeDropdown.value = if (activeDropdown.value == "themes") null else "themes"
                                    }
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = strings.wallpaperSectionTitle,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = palette.textPrimary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = currentPresetSubtitle,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (themesExpanded) palette.accentPrimary else palette.textSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Icon(
                                                imageVector = TxturaIcons.ChevronDown,
                                                contentDescription = null,
                                                tint = if (themesExpanded) palette.accentPrimary else palette.textSecondary,
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .rotate(themesRotation)
                                            )
                                        }

                                        AnimatedVisibility(
                                            visible = themesExpanded,
                                            enter = fadeIn() + expandVertically(),
                                            exit = fadeOut() + shrinkVertically()
                                        ) {
                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                Spacer(modifier = Modifier.height(12.dp))
                                                WallpaperThemesSelectorGrid(
                                                    currentConfig = mainWallpaper,
                                                    uiColorMode = uiColorMode,
                                                    strings = strings,
                                                    onSelectPreset = onSelectMainWallpaperPreset,
                                                    onPickFromGallery = onPickMainWallpaperFromGallery,
                                                    onSwitchColorMode = onSelectUiColorMode
                                                )
                                            }
                                        }
                                    }
                                }

                                val texturesRotation by animateFloatAsState(
                                    targetValue = if (texturesExpanded) 180f else 0f,
                                    animationSpec = TxturaMotion.rotateSpec(),
                                    label = "texturesRotation"
                                )
                                val textureTitle = if (mainWallpaper.isCustomGalleryImage) {
                                    strings.customWallpaperAccentTitle
                                } else {
                                    strings.textureSectionTitle
                                }
                                val currentTextureSubtitle = if (mainWallpaper.isCustomGalleryImage) {
                                    strings.customWallpaperAccentSub
                                } else {
                                    wallpaperTextureTitle(mainWallpaper.texture, strings)
                                }

                                // 1D. Tekstura pozadine / Boja akcenta (Background Txtura)
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    elevated = texturesExpanded,
                                    accentHighlight = if (texturesExpanded) palette.accentPrimary else null,
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                                    onClick = {
                                        activeDropdown.value = if (activeDropdown.value == "textures") null else "textures"
                                    }
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = textureTitle,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = palette.textPrimary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = currentTextureSubtitle,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (texturesExpanded) palette.accentPrimary else palette.textSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Icon(
                                                imageVector = TxturaIcons.ChevronDown,
                                                contentDescription = null,
                                                tint = if (texturesExpanded) palette.accentPrimary else palette.textSecondary,
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .rotate(texturesRotation)
                                            )
                                        }

                                        AnimatedVisibility(
                                            visible = texturesExpanded,
                                            enter = fadeIn() + expandVertically(),
                                            exit = fadeOut() + shrinkVertically()
                                        ) {
                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                Spacer(modifier = Modifier.height(12.dp))
                                                WallpaperTexturesSelectorGrid(
                                                    currentConfig = mainWallpaper,
                                                    strings = strings,
                                                    showInternalHeader = false,
                                                    onSelectTexture = onSelectMainWallpaperTexture,
                                                    onSelectAccentColor = onSelectMainWallpaperAccentColor
                                                )
                                            }
                                        }
                                    }
                                }

                                // 1E. Kompaktni prikaz poruka u glavnom prozoru (Compact Messages View)
                                val isCompactMode = listLayoutMode == MainListLayoutMode.COMPACT
                                SettingsSwitchRow(
                                    title = strings.layoutModeCompact,
                                    checked = isCompactMode
                                ) { compact ->
                                    onSelectListLayoutMode(
                                        if (compact) MainListLayoutMode.COMPACT else MainListLayoutMode.DEFAULT
                                    )
                                }

                                // 1F. Prikaz slika kontakata (Show Contact Photos)
                                SettingsSwitchRow(
                                    title = strings.showContactPhotosTitle,
                                    checked = showContactPhotos,
                                    onCheckedChange = onToggleShowContactPhotos
                                )

                                // 1G. Prikaži izražajne animacije (Show Expressive Animations)
                                SettingsSwitchRow(
                                    title = strings.expressiveAnimationsTitle,
                                    subtitle = strings.expressiveAnimationsSub,
                                    checked = expressiveAnimationsEnabled,
                                    onCheckedChange = onToggleExpressiveAnimations
                                )
                            }

                            SettingsCategoryWindow.NOTIFICATIONS_SOUNDS_HAPTICS -> {
                                var currentSmsSound by remember(smsNotificationSound) {
                                    mutableStateOf(smsNotificationSound)
                                }

                                // 2A-0. Izbor tona obaveštenja za poruke (SMS Notification Sound Selector)
                                SettingsSelectorRow(
                                    title = strings.smsNotificationSoundTitle,
                                    subtitle = strings.smsNotificationSoundSub,
                                    currentValueLabel = currentSmsSound.getDisplayName(strings.isSerbian),
                                    options = SmsNotificationSound.entries.map { sound ->
                                        Pair(sound, sound.getDisplayName(strings.isSerbian))
                                    },
                                    selectedOption = currentSmsSound,
                                    onSelect = { selectedSound ->
                                        currentSmsSound = selectedSound
                                        onSelectSmsNotificationSound(selectedSound)
                                    }
                                )

                                // 2A. Podesi zvuk sistema (prikazuje se samo kada je u izboru zvuka odabran Sistemski ton / System tone)
                                if (currentSmsSound == SmsNotificationSound.SYSTEM) {
                                    SettingsActionRow(
                                        title = strings.messageToneTitle,
                                        subtitle = strings.messageToneSub,
                                        onClick = { SmsMmsRepository.openMessageSoundSettings(context) }
                                    )
                                }

                                // 2B. Zvučni odzivi (Sound Feedback)
                                SettingsSwitchRow(
                                    title = strings.inAppSoundsTitle,
                                    subtitle = strings.inAppSoundsSub,
                                    checked = inAppSoundsEnabled,
                                    onCheckedChange = onToggleInAppSounds
                                )

                                // Svetlosni odziv (Flash Alert - rhythm synchronized torch notification)
                                SettingsSwitchRow(
                                    title = strings.flashAlertTitle,
                                    subtitle = strings.flashAlertSub,
                                    checked = flashAlertEnabled,
                                    onCheckedChange = onToggleFlashAlert
                                )

                                // 2C. Haptički odzivi (Haptic Feedback)
                                SettingsSwitchRow(
                                    title = strings.hapticsTitle,
                                    subtitle = strings.hapticsSub,
                                    checked = hapticsEnabled,
                                    onCheckedChange = onToggleHaptics
                                )

                                // 2D. Mehurići (Bubbles)
                                BubblesCollapsibleCard(
                                    bubblesEnabled = bubblesEnabled,
                                    onToggleBubbles = onToggleBubbles,
                                    onOpenSystemBubbleSettings = onOpenSystemBubbleSettings,
                                    strings = strings
                                )

                                // 2E. Upozorenja o hitnom slučaju (Wireless Emergency Alerts)
                                SettingsActionRow(
                                    title = strings.emergencyAlertsTitle,
                                    subtitle = strings.emergencyAlertsSub,
                                    onClick = { showEmergencyAlertsDialog = true }
                                )
                            }

                            SettingsCategoryWindow.CHAT_AND_GESTURES -> {
                                // 3A. Svajp gestovi (Swipe Gestures)
                                SwipeDirectionInteractiveCard(
                                    title = strings.swipeRightLabel,
                                    currentAction = swipeRightAction,
                                    defaultFallbackAction = SwipeAction.PIN,
                                    strings = strings,
                                    onSelectAction = onSelectSwipeRightAction
                                )

                                SwipeDirectionInteractiveCard(
                                    title = strings.swipeLeftLabel,
                                    currentAction = swipeLeftAction,
                                    defaultFallbackAction = SwipeAction.DELETE,
                                    strings = strings,
                                    onSelectAction = onSelectSwipeLeftAction
                                )

                                // 3B. Uštinite da biste zumirali tekst konverzacije (Pinch to Zoom Conversation Text)
                                SettingsSwitchRow(
                                    title = strings.pinchToZoomTitle,
                                    subtitle = strings.pinchToZoomSub,
                                    checked = pinchToZoomEnabled,
                                    onCheckedChange = onTogglePinchToZoom
                                )

                                // 3C. Opozovi poruke (Recall Messages)
                                SettingsSwitchRow(
                                    title = strings.recallMessagesTitle,
                                    subtitle = strings.recallMessagesSub,
                                    checked = recallMessagesEnabled,
                                    onCheckedChange = onToggleRecallMessages
                                )

                                // 3D. Eksterni linkovi (External Links)
                                ExternalLinksCard(
                                    identifyMaliciousWebsites = identifyMaliciousWebsites,
                                    onToggleIdentifyMaliciousWebsites = onToggleIdentifyMaliciousWebsites,
                                    autoPreviewsShowAll = autoPreviewsShowAll,
                                    onToggleAutoPreviewsShowAll = onToggleAutoPreviewsShowAll,
                                    autoPreviewsWebLinksOnly = autoPreviewsWebLinksOnly,
                                    onToggleAutoPreviewsWebLinksOnly = onToggleAutoPreviewsWebLinksOnly,
                                    autoPreviewsWifiOnly = autoPreviewsWifiOnly,
                                    onToggleAutoPreviewsWifiOnly = onToggleAutoPreviewsWifiOnly,
                                    strings = strings
                                )
                            }

                            SettingsCategoryWindow.MESSAGING_AND_NETWORK -> {
                                var deliveryReportsEnabled by remember {
                                    mutableStateOf(SmsMmsRepository.isDeliveryReportsEnabled(context))
                                }
                                var mmsRequestReadReports by remember {
                                    mutableStateOf(SmsMmsRepository.isMmsRequestReadReports(context))
                                }
                                var mmsSendReadReports by remember {
                                    mutableStateOf(SmsMmsRepository.isMmsSendReadReports(context))
                                }
                                var mmsAutoRetrieve by remember {
                                    mutableStateOf(SmsMmsRepository.getMmsAutoRetrieveMode(context))
                                }
                                var mmsDataDisabledMode by remember {
                                    mutableStateOf(SmsMmsRepository.getMmsDataDisabledMode(context))
                                }
                                var mmsCreationMode by remember {
                                    mutableStateOf(SmsMmsRepository.getMmsCreationMode(context))
                                }

                                // 4A. Delivery & receipts (Potvrde i izveštaji)
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    contentPadding = PaddingValues(14.dp)
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = strings.deliveryAndReceiptsSectionTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = palette.accentPrimary,
                                            fontWeight = FontWeight.Bold
                                        )

                                        // Delivery reports
                                        SettingsSwitchRow(
                                            title = strings.deliveryReportsTitle,
                                            subtitle = strings.deliveryReportsSub,
                                            checked = deliveryReportsEnabled,
                                            onCheckedChange = { checked ->
                                                deliveryReportsEnabled = checked
                                                SmsMmsRepository.setDeliveryReportsEnabled(context, checked)
                                            }
                                        )

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(0.8.dp)
                                                .background(palette.glassBorderTop.copy(alpha = 0.28f))
                                        )

                                        // Request read receipts
                                        SettingsSwitchRow(
                                            title = strings.requestReadReportsTitle,
                                            subtitle = strings.requestReadReportsSub,
                                            checked = mmsRequestReadReports,
                                            onCheckedChange = { checked ->
                                                mmsRequestReadReports = checked
                                                SmsMmsRepository.setMmsRequestReadReports(context, checked)
                                            }
                                        )

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(0.8.dp)
                                                .background(palette.glassBorderTop.copy(alpha = 0.28f))
                                        )

                                        // Send read receipts
                                        SettingsSwitchRow(
                                            title = strings.sendReadReportsTitle,
                                            subtitle = strings.sendReadReportsSub,
                                            checked = mmsSendReadReports,
                                            onCheckedChange = { checked ->
                                                mmsSendReadReports = checked
                                                SmsMmsRepository.setMmsSendReadReports(context, checked)
                                            }
                                        )
                                    }
                                }

                                // 4B. Preuzimanje i kreiranje MMS-a
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    contentPadding = PaddingValues(14.dp)
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = strings.mmsSectionTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = palette.accentPrimary,
                                            fontWeight = FontWeight.Bold
                                        )

                                        // Auto-retrieve MMS
                                        val autoRetrieveLabel = when (mmsAutoRetrieve) {
                                            MmsAutoRetrieveMode.NOT_ROAMING -> strings.mmsAutoRetrieveNotRoaming
                                            MmsAutoRetrieveMode.ALWAYS -> strings.mmsAutoRetrieveAlways
                                            MmsAutoRetrieveMode.DISABLED -> strings.mmsAutoRetrieveDisabled
                                        }
                                        SettingsSelectorRow(
                                            title = strings.mmsAutoRetrieveTitle,
                                            currentValueLabel = autoRetrieveLabel,
                                            options = listOf(
                                                MmsAutoRetrieveMode.NOT_ROAMING to strings.mmsAutoRetrieveNotRoaming,
                                                MmsAutoRetrieveMode.ALWAYS to strings.mmsAutoRetrieveAlways,
                                                MmsAutoRetrieveMode.DISABLED to strings.mmsAutoRetrieveDisabled
                                            ),
                                            selectedOption = mmsAutoRetrieve,
                                            onSelect = { mode ->
                                                mmsAutoRetrieve = mode
                                                SmsMmsRepository.setMmsAutoRetrieveMode(context, mode)
                                            }
                                        )


                                    }
                                }

                                // 4C. SIM i mrežni servisi (SIM Card & SMS Center)
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    contentPadding = PaddingValues(14.dp)
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = strings.simAndSmscSectionTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = palette.accentPrimary,
                                            fontWeight = FontWeight.Bold
                                        )

                                        // SMS center number
                                        val displaySmsc = smscAddress.ifBlank { SmsMmsRepository.getSmscAddress(context) }.ifBlank { strings.smscNotSet }
                                        SettingsActionRow(
                                            title = strings.smscNumberTitle,
                                            subtitle = displaySmsc,
                                            showChevron = false,
                                            onClick = null
                                        )

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(0.8.dp)
                                                .background(palette.glassBorderTop.copy(alpha = 0.28f))
                                        )

                                        // Manage SIM card messages
                                        SettingsActionRow(
                                            title = strings.simMessagesTitle,
                                            subtitle = strings.simMessagesSub,
                                            onClick = { showSimMessagesDialog = true }
                                        )
                                    }
                                }
                            }

                            SettingsCategoryWindow.DATA_AND_STORAGE -> {
                                // 5A. Obrisane poruke (Deleted Messages)
                                DeletedMessagesCollapsibleCard(
                                    autoDeleteDays = autoDeleteDays,
                                    onSelectAutoDeleteDays = onSelectAutoDeleteDays,
                                    deletedGroupingMode = deletedGroupingMode,
                                    onSelectDeletedGroupingMode = onSelectDeletedGroupingMode,
                                    deletedMessagesCount = deletedMessagesCount,
                                    onOpenDeletedMessages = onOpenDeletedMessages,
                                    strings = strings
                                )

                                // 5B. Bekap i vraćanje (Backup and Restore)
                                val lastBackupText = if (lastBackupTimestamp > 0L) {
                                    strings.lastBackupLabel(formatTimestamp(lastBackupTimestamp))
                                } else {
                                    strings.noPreviousBackup
                                }

                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(16.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            text = lastBackupText,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = palette.accentPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            GlassCard(
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(14.dp),
                                                elevated = true,
                                                accentHighlight = palette.accentPrimary,
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                                                onClick = onCreateLocalBackup
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(
                                                        imageVector = TxturaIcons.FolderBackup,
                                                        contentDescription = null,
                                                        tint = palette.accentPrimary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    OverflowTooltipText(
                                                        text = strings.createLocalBackupButton,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = palette.accentPrimary,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1
                                                    )
                                                }
                                            }

                                            GlassCard(
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(14.dp),
                                                elevated = true,
                                                accentHighlight = palette.accentSecondary,
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                                                onClick = onRestoreLocalBackup
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(
                                                        imageVector = TxturaIcons.Restore,
                                                        contentDescription = null,
                                                        tint = palette.accentSecondary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    OverflowTooltipText(
                                                        text = strings.restoreLocalBackupButton,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = palette.textPrimary,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }

                                        GlassCard(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp),
                                            elevated = true,
                                            accentHighlight = palette.accentPrimary,
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                                            onClick = onShareBackupFile
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(
                                                    imageVector = TxturaIcons.Send,
                                                    contentDescription = null,
                                                    tint = palette.accentPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = strings.shareBackupButton,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = palette.accentPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                            }

                            SettingsCategoryWindow.SYSTEM_AND_ABOUT -> {
                                LaunchedEffect(focusUpdateSection) {
                                    if (focusUpdateSection) {
                                        cachedReleaseInfo = AppUpdateManager.getCachedReleaseInfo(context, appVersionName)
                                        delay(180.milliseconds)
                                        windowScrollState.animateScrollTo(windowScrollState.maxValue)
                                        onUpdateSectionFocused()
                                    }
                                }

                                // 6A. Dozvole aplikacije (App Permissions)
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    contentPadding = PaddingValues(14.dp)
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = strings.permissionsSectionTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = palette.accentPrimary,
                                            fontWeight = FontWeight.Bold
                                        )

                                        // 1. SMS i MMS dozvole
                                        SettingsSwitchRow(
                                            title = strings.permSmsTitle,
                                            subtitle = if (hasSmsPermission) strings.permGranted else strings.permDemoNotGranted,
                                            checked = hasSmsPermission
                                        ) {
                                            if (!hasSmsPermission) onRequestSmsPermissions() else onOpenSystemAppSettings()
                                        }

                                        rationaleMessage?.let { msg ->
                                            Text(
                                                text = msg,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = palette.cautionColor
                                            )
                                        }

                                        // 2. Dozvola za mikrofon (za slanje glasovnih poruka)
                                        SettingsSwitchRow(
                                            title = strings.permMicrophoneTitle,
                                            subtitle = if (hasMicrophonePermission) strings.permMicrophoneSubGranted else strings.permMicrophoneSubNotGranted,
                                            checked = hasMicrophonePermission
                                        ) {
                                            if (!hasMicrophonePermission) onRequestMicrophonePermission() else onOpenSystemAppSettings()
                                        }

                                        // 3. Dozvola za kontakte
                                        SettingsSwitchRow(
                                            title = strings.permContactsTitle,
                                            subtitle = if (hasContactsPermission) strings.permGranted else strings.permDemoNotGranted,
                                            checked = hasContactsPermission
                                        ) {
                                            if (!hasContactsPermission) onRequestContactsPermission() else onOpenSystemAppSettings()
                                        }

                                        // 4. Dozvola za obaveštenja (Android 13+)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            SettingsSwitchRow(
                                                title = strings.permNotificationsTitle,
                                                subtitle = if (hasNotificationPermission) strings.permGranted else strings.permDemoNotGranted,
                                                checked = hasNotificationPermission
                                            ) {
                                                if (!hasNotificationPermission) onRequestNotificationPermission() else onOpenSystemAppSettings()
                                            }
                                        }

                                        // 5. Podrazumevana SMS aplikacija
                                        SettingsSwitchRow(
                                            title = strings.defaultSmsRoleLabel,
                                            subtitle = if (isDefaultSmsApp) strings.roleActive else strings.roleNotSet,
                                            checked = isDefaultSmsApp
                                        ) {
                                            if (!isDefaultSmsApp) onRequestDefaultSmsRole() else onOpenSystemAppSettings()
                                        }

                                        GlassCard(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                            onClick = onOpenSystemAppSettings
                                        ) {
                                            Text(
                                                text = strings.openSystemAppSettingsButton,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = palette.textSecondary,
                                                modifier = Modifier.align(Alignment.Center)
                                            )
                                        }
                                    }
                                }

                                // 6B. Ažuriranja aplikacije (App Updates)
                                val hasUpdate = cachedReleaseInfo?.isUpdateAvailable == true
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(22.dp),
                                    elevated = true,
                                    accentHighlight = if (hasUpdate) palette.safeColor else null,
                                    contentPadding = PaddingValues(16.dp),
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                val updateIconTint = if (hasUpdate) palette.safeColor else palette.accentPrimary
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(CircleShape)
                                                        .background(updateIconTint.copy(alpha = 0.14f))
                                                        .border(1.dp, updateIconTint.copy(alpha = 0.45f), CircleShape),
                                                ) {
                                                    Icon(
                                                        imageVector = TxturaIcons.ClockTimer,
                                                        contentDescription = null,
                                                        tint = updateIconTint,
                                                        modifier = Modifier.size(19.dp),
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = strings.updateSectionTitle,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = palette.textPrimary,
                                                        fontWeight = FontWeight.Bold,
                                                    )
                                                    val statusText = when {
                                                        downloadProgressPct != null -> strings.downloadingUpdateLabel(downloadProgressPct ?: 0)
                                                        hasUpdate -> strings.updateAvailableTitle(cachedReleaseInfo?.versionTag.orEmpty())
                                                        else -> strings.appUpToDateLabel(appVersionName)
                                                    }
                                                    Text(
                                                        text = statusText,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (hasUpdate) palette.safeColor else palette.accentPrimary,
                                                        fontWeight = FontWeight.SemiBold,
                                                    )
                                                    if (lastUpdateCheckMs > 0L) {
                                                        val formattedTime = remember(lastUpdateCheckMs) {
                                                            SimpleDateFormat("dd.MM.yyyy. HH:mm", Locale.getDefault())
                                                                .format(Date(lastUpdateCheckMs))
                                                        }
                                                        Text(
                                                            text = strings.updateLastCheckedLabel(formattedTime),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontSize = 10.sp,
                                                            color = palette.textSecondary.copy(alpha = 0.78f),
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            GlassTooltipIconButton(
                                                icon = TxturaIcons.Info,
                                                tooltipText = strings.changelogTooltip,
                                                tint = palette.accentPrimary,
                                                pressedTint = palette.safeColor,
                                                size = 38.dp,
                                                onClick = { showChangelogDialog = true }
                                            )
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            GlassCard(
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(14.dp),
                                                elevated = !hasUpdate,
                                                accentHighlight = palette.accentPrimary,
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                                onClick = {
                                                    if (!isCheckingUpdates && (downloadProgressPct == null)) {
                                                        coroutineScope.launch {
                                                            isCheckingUpdates = true
                                                            when (val res = AppUpdateManager.checkForUpdates(context, appVersionName)) {
                                                                is AppUpdateCheckResult.Success -> {
                                                                    cachedReleaseInfo = res.info
                                                                    lastUpdateCheckMs = AppUpdateManager.getLastCheckTimestamp(context)
                                                                    if (!res.info.isUpdateAvailable) {
                                                                        Toast.makeText(
                                                                            context,
                                                                            strings.updateUpToDateToast,
                                                                            Toast.LENGTH_SHORT,
                                                                        ).show()
                                                                    }
                                                                }
                                                                is AppUpdateCheckResult.NoInternet -> {
                                                                    Toast.makeText(
                                                                        context,
                                                                        strings.updateNoInternetToast,
                                                                        Toast.LENGTH_SHORT,
                                                                    ).show()
                                                                }
                                                                is AppUpdateCheckResult.Error -> {
                                                                    Toast.makeText(
                                                                        context,
                                                                        strings.updateCheckFailedToast,
                                                                        Toast.LENGTH_SHORT,
                                                                    ).show()
                                                                }
                                                            }
                                                            isCheckingUpdates = false
                                                        }
                                                    }
                                                },
                                            ) {
                                                Text(
                                                    text = if (isCheckingUpdates) strings.checkingForUpdatesLabel else strings.checkForUpdatesButton,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = palette.accentPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.align(Alignment.Center),
                                                )
                                            }

                                            if (hasUpdate && (cachedReleaseInfo != null)) {
                                                val releaseToInstall = cachedReleaseInfo!!
                                                GlassCard(
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(14.dp),
                                                    elevated = true,
                                                    accentHighlight = palette.safeColor,
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                                    onClick = {
                                                        if (downloadProgressPct == null) {
                                                            coroutineScope.launch {
                                                                downloadProgressPct = 0
                                                                AppUpdateManager.downloadAndInstallUpdate(
                                                                    context = context,
                                                                    releaseInfo = releaseToInstall,
                                                                    onProgress = { pct -> downloadProgressPct = pct },
                                                                )
                                                                downloadProgressPct = null
                                                            }
                                                        }
                                                    },
                                                ) {
                                                    Text(
                                                        text = strings.downloadAndInstallUpdateButton,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = palette.safeColor,
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.align(Alignment.Center),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            SettingsCategoryWindow.PRIVACY_AND_SECURITY -> {
                                var dbEncryptionEnabled by remember {
                                    mutableStateOf(AppSecurityManager.isDbEncryptionEnabled(context))
                                }
                                var biometricLockEnabled by remember {
                                    mutableStateOf(AppSecurityManager.isBiometricLockEnabled(context))
                                }
                                var sslPinningEnabled by remember {
                                    mutableStateOf(AppSecurityManager.isSslPinningEnabled(context))
                                }
                                var metadataMinimizationEnabled by remember {
                                    mutableStateOf(AppSecurityManager.isMetadataMinimizationEnabled(context))
                                }
                                var spamFirewallEnabled by remember {
                                    mutableStateOf(AppSecurityManager.isSpamFirewallEnabled(context))
                                }
                                var rootDetectionEnabled by remember {
                                    mutableStateOf(AppSecurityManager.isRootDetectionEnabled(context))
                                }
                                var rootStatus by remember {
                                    mutableStateOf(AppSecurityManager.RootCheckResult(false, false, false, false))
                                }
                                var isScanningRoot by remember { mutableStateOf(rootDetectionEnabled) }
                                val coroutineScope = rememberCoroutineScope()
                                val isDeviceSecure = remember(context) { AppSecurityManager.isDeviceLockConfigured(context) }

                                LaunchedEffect(context, rootDetectionEnabled) {
                                    if (rootDetectionEnabled) {
                                        isScanningRoot = true
                                        rootStatus = withContext(Dispatchers.IO) {
                                            AppSecurityManager.checkRootStatus(context)
                                        }
                                        isScanningRoot = false
                                    } else {
                                        rootStatus = AppSecurityManager.RootCheckResult(false, false, false, false)
                                        isScanningRoot = false
                                    }
                                }
                                val rootStatusColor = when {
                                    !rootDetectionEnabled || isScanningRoot -> palette.textSecondary
                                    rootStatus.isRooted -> palette.cautionColor
                                    else -> palette.safeColor
                                }
                                val rootStatusLabel = when {
                                    !rootDetectionEnabled -> strings.securityStatusDisabled
                                    isScanningRoot -> strings.rootScanInProgress
                                    rootStatus.isRooted -> strings.rootStateCompromised
                                    else -> strings.rootStateSafe
                                }

                                // 1. Blokirani pošiljaoci (Blocked Senders)
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    contentPadding = PaddingValues(14.dp)
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = TxturaIcons.Block,
                                                    contentDescription = null,
                                                    tint = palette.accentPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = strings.blockedSendersSectionTitle,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = palette.textPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            GlassCard(
                                                shape = RoundedCornerShape(50),
                                                accentHighlight = palette.accentPrimary,
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                onClick = {
                                                    newBlockAddressText = ""
                                                    showAddBlockSenderDialog = true
                                                }
                                            ) {
                                                Text(
                                                    text = "+ " + strings.addBlockedSenderTitle,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = palette.accentPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        SettingsSwitchRow(
                                            title = strings.spamFirewallTitle,
                                            subtitle = strings.spamFirewallSub,
                                            checked = spamFirewallEnabled,
                                            onCheckedChange = { enabled ->
                                                spamFirewallEnabled = enabled
                                                AppSecurityManager.setSpamFirewallEnabled(context, enabled)
                                            }
                                        )

                                        if (blockedSenders.isEmpty()) {
                                            Text(
                                                text = strings.blockedSendersEmpty,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = palette.textSecondary
                                            )
                                        } else {
                                            blockedSenders.forEach { sender ->
                                                GlassCard(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(12.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            text = sender,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = palette.textPrimary,
                                                            fontWeight = FontWeight.SemiBold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        GlassCard(
                                                            shape = RoundedCornerShape(50),
                                                            accentHighlight = palette.safeColor,
                                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                            onClick = { onUnblockSender(sender) }
                                                        ) {
                                                            Text(
                                                                text = strings.unblockSenderButton,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = palette.safeColor,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // 2. Biometrijska zaštita i PIN (Biometric Protection & PIN)
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    contentPadding = PaddingValues(14.dp)
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = TxturaIcons.PermissionsKey,
                                                contentDescription = null,
                                                tint = palette.accentPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = strings.biometricLockTitle,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = palette.textPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Text(
                                            text = strings.biometricLockSub,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = palette.textSecondary
                                        )

                                        SettingsSwitchRow(
                                            title = strings.biometricLockEnabledLabel,
                                            subtitle = if (isDeviceSecure) null else (if (strings.isSerbian) "Zahteva podešavanje PIN-a ili biometrije u podešavanjima telefona" else "Requires device PIN or biometrics in Android Settings"),
                                            checked = biometricLockEnabled && isDeviceSecure,
                                            onCheckedChange = { checked ->
                                                if (checked && !isDeviceSecure) {
                                                    Toast.makeText(
                                                        context,
                                                        if (strings.isSerbian) "Prvo podesite PIN ili biometriju u podešavanjima telefona pre zaključavanja aplikacije."
                                                        else "Please configure a device PIN or biometrics in Android Settings first.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                    runCatching {
                                                        context.startActivity(Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS).apply {
                                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        })
                                                    }
                                                } else {
                                                    biometricLockEnabled = checked
                                                    AppSecurityManager.setBiometricLockEnabled(context, checked)
                                                }
                                            }
                                        )

                                        if (biometricLockEnabled) {
                                            var lockTimeout by remember {
                                                mutableIntStateOf(AppSecurityManager.getBiometricLockTimeout(context))
                                            }
                                            val timeoutLabel = when (lockTimeout) {
                                                1 -> strings.lockTimeout1Min
                                                5 -> strings.lockTimeout5Min
                                                else -> strings.lockTimeoutImmediate
                                            }
                                            SettingsSelectorRow(
                                                title = strings.lockTimeoutTitle,
                                                currentValueLabel = timeoutLabel,
                                                options = listOf(
                                                    0 to strings.lockTimeoutImmediate,
                                                    1 to strings.lockTimeout1Min,
                                                    5 to strings.lockTimeout5Min
                                                ),
                                                selectedOption = lockTimeout,
                                                onSelect = { timeout ->
                                                    lockTimeout = timeout
                                                    AppSecurityManager.setBiometricLockTimeout(context, timeout)
                                                }
                                            )

                                            GlassCard(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                accentHighlight = palette.accentPrimary,
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                                onClick = onLockAppNow
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(
                                                        imageVector = TxturaIcons.PermissionsKey,
                                                        contentDescription = null,
                                                        tint = palette.accentPrimary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = strings.lockAppNowButton,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = palette.accentPrimary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // 3. Lokalna enkripcija baze podataka (Local Database Encryption)
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    contentPadding = PaddingValues(14.dp)
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = TxturaIcons.ShieldCheck,
                                                contentDescription = null,
                                                tint = palette.accentPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = strings.dbEncryptionTitle,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = palette.textPrimary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(palette.safeColor.copy(alpha = 0.15f))
                                                .border(
                                                    0.8.dp,
                                                    palette.safeColor.copy(alpha = 0.45f),
                                                    RoundedCornerShape(50)
                                                )
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(palette.safeColor)
                                                )
                                                Text(
                                                    text = strings.dbEncryptionBadge(dbEncryptionEnabled),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = palette.safeColor,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        Text(
                                            text = strings.dbEncryptionSub,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = palette.textSecondary
                                        )

                                        SettingsSwitchRow(
                                            title = strings.dbEncryptionSwitchLabel,
                                            checked = dbEncryptionEnabled,
                                            onCheckedChange = { checked ->
                                                dbEncryptionEnabled = checked
                                                AppSecurityManager.setDbEncryptionEnabled(context, checked)
                                                val toastMsg = if (checked) strings.dbEncryptionEnabledToast else strings.dbEncryptionDisabledToast
                                                Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }

                                // 7C. HTTPS i TLS/SSL sa Pinning-om (SSL Pinning)
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    contentPadding = PaddingValues(14.dp)
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = TxturaIcons.Link,
                                                contentDescription = null,
                                                tint = palette.accentPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = strings.sslPinningTitle,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = palette.textPrimary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(if (sslPinningEnabled) palette.safeColor.copy(alpha = 0.15f) else palette.textSecondary.copy(alpha = 0.15f))
                                                .border(
                                                    0.8.dp,
                                                    if (sslPinningEnabled) palette.safeColor.copy(alpha = 0.45f) else palette.textSecondary.copy(alpha = 0.35f),
                                                    RoundedCornerShape(50)
                                                )
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(if (sslPinningEnabled) palette.safeColor else palette.textSecondary)
                                                )
                                                Text(
                                                    text = strings.sslPinningBadge,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (sslPinningEnabled) palette.safeColor else palette.textSecondary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        Text(
                                            text = strings.sslPinningSub,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = palette.textSecondary
                                        )

                                        SettingsSwitchRow(
                                            title = strings.sslPinningSwitchLabel,
                                            checked = sslPinningEnabled,
                                            onCheckedChange = { checked ->
                                                sslPinningEnabled = checked
                                                AppSecurityManager.setSslPinningEnabled(context, checked)
                                            }
                                        )
                                    }
                                }

                                // 7D. Minimizacija metapodataka (Metadata Minimization)
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    contentPadding = PaddingValues(14.dp)
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = TxturaIcons.EyePreview,
                                                contentDescription = null,
                                                tint = palette.accentPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = strings.metadataMinimizationTitle,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = palette.textPrimary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(if (metadataMinimizationEnabled) palette.safeColor.copy(alpha = 0.15f) else palette.textSecondary.copy(alpha = 0.15f))
                                                .border(
                                                    0.8.dp,
                                                    if (metadataMinimizationEnabled) palette.safeColor.copy(alpha = 0.45f) else palette.textSecondary.copy(alpha = 0.35f),
                                                    RoundedCornerShape(50)
                                                )
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(if (metadataMinimizationEnabled) palette.safeColor else palette.textSecondary)
                                                )
                                                Text(
                                                    text = strings.metadataMinimizationBadge,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (metadataMinimizationEnabled) palette.safeColor else palette.textSecondary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        Text(
                                            text = strings.metadataMinimizationSub,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = palette.textSecondary
                                        )

                                        SettingsSwitchRow(
                                            title = strings.metadataMinimizationSwitchLabel,
                                            checked = metadataMinimizationEnabled,
                                            onCheckedChange = { checked ->
                                                metadataMinimizationEnabled = checked
                                                AppSecurityManager.setMetadataMinimizationEnabled(context, checked)
                                            }
                                        )
                                    }
                                }

                                // 7E. Detekcija "Root" stanja (Root State Detection)
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    contentPadding = PaddingValues(14.dp)
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = if (rootDetectionEnabled && rootStatus.isRooted) TxturaIcons.Warning else TxturaIcons.ShieldCheck,
                                                contentDescription = null,
                                                tint = rootStatusColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = strings.rootDetectionTitle,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = palette.textPrimary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(rootStatusColor.copy(alpha = 0.15f))
                                                .border(
                                                    0.8.dp,
                                                    rootStatusColor.copy(alpha = 0.45f),
                                                    RoundedCornerShape(50)
                                                )
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(rootStatusColor)
                                                )
                                                Text(
                                                    text = rootStatusLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = rootStatusColor,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        Text(
                                            text = strings.rootDetectionSub,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = palette.textSecondary
                                        )

                                        SettingsSwitchRow(
                                            title = strings.rootDetectionSwitchLabel,
                                            subtitle = strings.rootDetectionSwitchSub,
                                            checked = rootDetectionEnabled,
                                            onCheckedChange = { enabled ->
                                                rootDetectionEnabled = enabled
                                                isScanningRoot = enabled
                                                AppSecurityManager.setRootDetectionEnabled(context, enabled)
                                            }
                                        )

                                        if (rootDetectionEnabled) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(palette.glassSurfaceElevated.copy(alpha = 0.4f))
                                                .padding(10.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (!rootStatus.suBinaryFound) TxturaIcons.Check else TxturaIcons.Close,
                                                    contentDescription = null,
                                                    tint = if (!rootStatus.suBinaryFound) palette.safeColor else palette.cautionColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (!rootStatus.suBinaryFound) strings.rootCheckBinarySafe else strings.rootCheckBinaryRisk,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = palette.textPrimary
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (!rootStatus.testKeysFound) TxturaIcons.Check else TxturaIcons.Close,
                                                    contentDescription = null,
                                                    tint = if (!rootStatus.testKeysFound) palette.safeColor else palette.cautionColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (!rootStatus.testKeysFound) strings.rootCheckTagsSafe else strings.rootCheckTagsRisk,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = palette.textPrimary
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (!rootStatus.rootManagementAppFound) TxturaIcons.Check else TxturaIcons.Close,
                                                    contentDescription = null,
                                                    tint = if (!rootStatus.rootManagementAppFound) palette.safeColor else palette.cautionColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (!rootStatus.rootManagementAppFound) strings.rootCheckAppsSafe else strings.rootCheckAppsRisk,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = palette.textPrimary
                                                )
                                            }
                                        }

                                        GlassCard(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp),
                                            accentHighlight = palette.accentPrimary,
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                            onClick = {
                                                if (!isScanningRoot) {
                                                    isScanningRoot = true
                                                    coroutineScope.launch {
                                                        delay(350.milliseconds)
                                                        rootStatus = withContext(Dispatchers.IO) {
                                                            AppSecurityManager.checkRootStatus(context)
                                                        }
                                                        isScanningRoot = false
                                                    }
                                                }
                                            }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(
                                                    imageVector = TxturaIcons.ShieldCheck,
                                                    contentDescription = null,
                                                    tint = palette.accentPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (isScanningRoot) strings.rootScanInProgress else strings.rootRescanButton,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = palette.accentPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        }
                                    }
                                }
                            }
                        }

                        if (showAddBlockSenderDialog) {
                            GlassAlertDialog(
                                onDismissRequest = { showAddBlockSenderDialog = false },
                                title = {
                                    Text(
                                        text = strings.addBlockedSenderTitle,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                text = {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                    ) {
                                        BasicTextField(
                                            value = newBlockAddressText,
                                            onValueChange = { newBlockAddressText = it },
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                                color = palette.textPrimary,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            cursorBrush = SolidColor(palette.accentPrimary),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                            decorationBox = { innerTextField ->
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(palette.glassSurfaceElevated)
                                                        .border(
                                                            1.dp,
                                                            palette.accentPrimary.copy(alpha = 0.5f),
                                                            RoundedCornerShape(14.dp)
                                                        )
                                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                                ) {
                                                    if (newBlockAddressText.isEmpty()) {
                                                        Text(
                                                            text = strings.blockSenderDialogHint,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = palette.textSecondary.copy(alpha = 0.6f)
                                                        )
                                                    } else {
                                                        innerTextField()
                                                    }
                                                }
                                            }
                                        )
                                    }
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            val trimmed = newBlockAddressText.trim()
                                            if (trimmed.isNotBlank()) {
                                                SmsMmsRepository.blockSender(context, trimmed)
                                                newBlockAddressText = ""
                                                showAddBlockSenderDialog = false
                                            }
                                        }
                                    ) {
                                        Text(
                                            text = strings.blockSenderAction,
                                            color = palette.cautionColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showAddBlockSenderDialog = false }) {
                                        Text(text = strings.cancel, color = palette.textSecondary)
                                    }
                                }
                            )
                        }

                        if (showSmscDialog) {
                            SmscEditorDialog(
                                currentSmsc = smscAddress,
                                strings = strings,
                                onSave = { newAddress ->
                                    smscAddress = newAddress
                                    SmsMmsRepository.setSmscAddress(context, newAddress)
                                    currentMmsApn = SmsMmsRepository.getMmsApnSettings(context)
                                    Toast.makeText(context, strings.smscSavedToast, Toast.LENGTH_SHORT).show()
                                },
                                onDismiss = { showSmscDialog = false }
                            )
                        }

                        if (showMmsApnManualDialog) {
                            MmsApnEditorDialog(
                                currentApn = currentMmsApn,
                                strings = strings,
                                onSave = { newApn ->
                                    currentMmsApn = newApn
                                    SmsMmsRepository.setMmsApnSettings(context, newApn)
                                    Toast.makeText(context, strings.mmsApnSaveSuccess, Toast.LENGTH_SHORT).show()
                                },
                                onDismiss = { showMmsApnManualDialog = false }
                            )
                        }

                        if (showSimMessagesDialog) {
                            SimMessagesDialog(
                                strings = strings,
                                onDismiss = { showSimMessagesDialog = false }
                            )
                        }

                        if (showEmergencyAlertsDialog) {
                            EmergencyAlertsDialog(
                                strings = strings,
                                onDismiss = { showEmergencyAlertsDialog = false }
                            )
                        }
                    }
                }
            }
        }

        if (showSecurityMeasuresDialog) {
            SecurityMeasuresDialog(
                onDismiss = { showSecurityMeasuresDialog = false },
                identifyMaliciousWebsites = identifyMaliciousWebsites,
                onNavigateToSetting = { targetCategory, targetKey ->
                    showSecurityMeasuresDialog = false
                    activeCategoryWindow = targetCategory
                    targetSecurityHighlight = targetKey
                }
            )
        }

        if (showChangelogDialog) {
            AppChangelogDialog(
                currentVersionName = appVersionName,
                currentVersionCode = appVersionCode,
                cachedReleaseInfo = cachedReleaseInfo,
                onDismiss = { showChangelogDialog = false }
            )
        }
    }
    }
}

@Composable
private fun BubblesCollapsibleCard(
    bubblesEnabled: Boolean,
    onToggleBubbles: (Boolean) -> Unit,
    onOpenSystemBubbleSettings: () -> Unit,
    strings: AppStrings
) {
    val palette = LocalGlassPalette.current
    val isExpressive = LocalExpressiveAnimations.current
    val activeDropdown = LocalActiveDropdownId.current
    val expanded = activeDropdown.value == "bubbles"
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = if (isExpressive) {
            TxturaMotion.rotateSpec()
        } else {
            tween(200)
        },
        label = "bubbles_arrow_rotation"
    )

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevated = expanded,
        accentHighlight = if (expanded) palette.accentPrimary else null,
        contentPadding = PaddingValues(14.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header: Clickable to toggle dropdown
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        activeDropdown.value = if (activeDropdown.value == "bubbles") null else "bubbles"
                    }
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(palette.accentPrimary.copy(alpha = 0.14f))
                            .border(1.dp, palette.accentPrimary.copy(alpha = 0.42f), CircleShape)
                    ) {
                        Icon(
                            imageVector = TxturaIcons.ChatBubble,
                            contentDescription = null,
                            tint = palette.accentPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = strings.bubblesSectionTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(palette.glassSurfaceElevated)
                        .border(0.8.dp, palette.glassBorderTop.copy(alpha = 0.35f), CircleShape)
                ) {
                    Icon(
                        imageVector = TxturaIcons.ArrowDown,
                        contentDescription = strings.bubblesSectionTitle,
                        tint = palette.accentPrimary,
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(arrowRotation)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = TxturaMotion.verticalExpandEnter(isExpressive = isExpressive),
                exit = TxturaMotion.verticalCollapseExit(isExpressive = isExpressive)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.8.dp)
                            .background(palette.glassBorderTop.copy(alpha = 0.28f))
                    )

                    // 1. Prekidač: Dozvoli aplikaciji Txtura da prikazuje obaveštenja kao mehuriće
                    ExternalLinkToggleRow(
                        title = strings.bubblesSectionTitle,
                        subtitle = strings.bubblesAllowTitle,
                        checked = bubblesEnabled,
                        onCheckedChange = onToggleBubbles
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.8.dp)
                            .background(palette.glassBorderTop.copy(alpha = 0.2f))
                    )

                    // 2. Info panel: objašnjenje gestova mehurića
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.glassSurfaceElevated)
                            .border(0.8.dp, palette.glassBorderTop.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(palette.accentPrimary.copy(alpha = 0.12f))
                                .border(1.dp, palette.accentPrimary.copy(alpha = 0.35f), CircleShape)
                        ) {
                            Icon(
                                imageVector = TxturaIcons.Info,
                                contentDescription = null,
                                tint = palette.accentPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = strings.bubblesInfoDesc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textPrimary.copy(alpha = 0.92f),
                            lineHeight = 20.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 3. Dugme za otvaranje sistemskih podešavanja mehurića
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevated = true,
                        accentHighlight = palette.accentPrimary,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 11.dp),
                        onClick = onOpenSystemBubbleSettings
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = TxturaIcons.ChatBubble,
                                contentDescription = null,
                                tint = palette.accentPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = strings.bubblesOpenSystemSettings,
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.accentPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExternalLinksCard(
    identifyMaliciousWebsites: Boolean,
    onToggleIdentifyMaliciousWebsites: (Boolean) -> Unit,
    autoPreviewsShowAll: Boolean,
    onToggleAutoPreviewsShowAll: (Boolean) -> Unit,
    autoPreviewsWebLinksOnly: Boolean,
    onToggleAutoPreviewsWebLinksOnly: (Boolean) -> Unit,
    autoPreviewsWifiOnly: Boolean,
    onToggleAutoPreviewsWifiOnly: (Boolean) -> Unit,
    strings: AppStrings
) {
    val palette = LocalGlassPalette.current

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(14.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header: Classic static title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(palette.accentPrimary.copy(alpha = 0.14f))
                        .border(1.dp, palette.accentPrimary.copy(alpha = 0.42f), CircleShape)
                ) {
                    Icon(
                        imageVector = TxturaIcons.Link,
                        contentDescription = null,
                        tint = palette.accentPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = strings.externalLinksTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.8.dp)
                    .background(palette.glassBorderTop.copy(alpha = 0.28f))
            )

            // 1. Identifikacija zlonamernih veb-sajtova u porukama
            ExternalLinkToggleRow(
                title = strings.maliciousWebsitesTitle,
                subtitle = strings.maliciousWebsitesSub,
                checked = identifyMaliciousWebsites,
                onCheckedChange = onToggleIdentifyMaliciousWebsites
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.8.dp)
                    .background(palette.glassBorderTop.copy(alpha = 0.2f))
            )

            // 2. Prikazuj sve preglede
            ExternalLinkToggleRow(
                title = strings.autoPreviewsShowAllTitle,
                subtitle = strings.autoPreviewsShowAllSub,
                checked = autoPreviewsShowAll,
                onCheckedChange = { checked ->
                    onToggleAutoPreviewsShowAll(checked)
                    if (checked && autoPreviewsWebLinksOnly) {
                        onToggleAutoPreviewsWebLinksOnly(false)
                    }
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.8.dp)
                    .background(palette.glassBorderTop.copy(alpha = 0.2f))
            )

            // 3. Prikazuj samo preglede veb-linkova
            ExternalLinkToggleRow(
                title = strings.autoPreviewsWebLinksOnlyTitle,
                subtitle = strings.autoPreviewsWebLinksOnlySub,
                checked = autoPreviewsWebLinksOnly,
                onCheckedChange = { checked ->
                    onToggleAutoPreviewsWebLinksOnly(checked)
                    if (checked && !autoPreviewsShowAll) {
                        onToggleAutoPreviewsShowAll(true)
                    }
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.8.dp)
                    .background(palette.glassBorderTop.copy(alpha = 0.2f))
            )

            // 4. Preuzimaj podatke samo preko WiFi-a
            ExternalLinkToggleRow(
                title = strings.autoPreviewsWifiOnlyTitle,
                subtitle = strings.autoPreviewsWifiOnlySub,
                checked = autoPreviewsWifiOnly,
                onCheckedChange = onToggleAutoPreviewsWifiOnly
            )
        }
    }
}

@Composable
private fun ExternalLinkToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalGlassPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                OverflowTooltipText(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (checked) palette.accentPrimary else palette.textSecondary,
                    maxLines = 1
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = if (palette.isDark) Color.Black else Color.White,
                checkedTrackColor = palette.accentPrimary,
                uncheckedThumbColor = palette.textSecondary,
                uncheckedTrackColor = palette.glassSurfaceElevated
            )
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalGlassPalette.current
    val view = LocalView.current
    val context = LocalContext.current
    val handleToggle: (Boolean) -> Unit = { newChecked ->
        TxturaHaptics.toggle(view, context, newChecked)
        onCheckedChange(newChecked)
    }
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevated = checked,
        accentHighlight = if (checked) palette.accentPrimary else null,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        onClick = if (enabled) { { handleToggle(!checked) } } else null
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) palette.textPrimary else palette.textSecondary.copy(alpha = 0.7f),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    OverflowTooltipText(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (checked) palette.accentPrimary else palette.textSecondary,
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = if (enabled) handleToggle else null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = if (palette.isDark) Color.Black else Color.White,
                    checkedTrackColor = palette.accentPrimary,
                    uncheckedThumbColor = palette.textSecondary,
                    uncheckedTrackColor = palette.glassSurfaceElevated
                )
            )
        }
    }
}

@Composable
private fun <T> SettingsSelectorRow(
    title: String,
    subtitle: String? = null,
    currentValueLabel: String,
    options: List<Pair<T, String>>,
    selectedOption: T,
    onSelect: (T) -> Unit
) {
    val palette = LocalGlassPalette.current
    val view = LocalView.current
    val context = LocalContext.current
    val activeDropdown = LocalActiveDropdownId.current
    val dropdownKey = "selector_${title.hashCode()}"
    val menuExpanded = activeDropdown.value == dropdownKey

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        onClick = {
            activeDropdown.value = if (activeDropdown.value == dropdownKey) null else dropdownKey
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                        lineHeight = 15.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentValueLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = TxturaIcons.ChevronRight,
                        contentDescription = null,
                        tint = palette.textSecondary.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                val menuShape = RoundedCornerShape(16.dp)
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = {
                        if (activeDropdown.value == dropdownKey) {
                            activeDropdown.value = null
                        }
                    },
                    modifier = Modifier.widthIn(min = 180.dp, max = 280.dp),
                    shape = menuShape,
                    containerColor = palette.popupContainerColor,
                    tonalElevation = 0.dp,
                    shadowElevation = 8.dp,
                    border = BorderStroke(
                        width = 1.dp,
                        brush = Brush.linearGradient(listOf(palette.glassBorderTop, palette.glassBorderBottom))
                    )
                ) {
                    options.forEach { (value, label) ->
                        val isSelected = value == selectedOption
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) palette.accentPrimary else palette.textPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                            trailingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = TxturaIcons.Check,
                                        contentDescription = null,
                                        tint = palette.accentPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            onClick = {
                                TxturaHaptics.click(view, context)
                                onSelect(value)
                                activeDropdown.value = null
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String? = null,
    trailingLabel: String? = null,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val palette = LocalGlassPalette.current
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                        lineHeight = 15.sp
                    )
                }
            }
            if (!trailingLabel.isNullOrBlank() || showChevron) {
                Spacer(modifier = Modifier.width(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!trailingLabel.isNullOrBlank()) {
                        Text(
                            text = trailingLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (showChevron) {
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                    if (showChevron) {
                        Icon(
                            imageVector = TxturaIcons.ChevronRight,
                            contentDescription = null,
                            tint = palette.textSecondary.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmscEditorDialog(
    currentSmsc: String,
    strings: AppStrings,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalGlassPalette.current
    var smscInput by remember { mutableStateOf(currentSmsc) }

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = strings.smscDialogTitle,
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) {
                Text(
                    text = strings.smscDialogDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    lineHeight = 16.sp
                )
                BasicTextField(
                    value = smscInput,
                    onValueChange = { smscInput = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = palette.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    cursorBrush = SolidColor(palette.accentPrimary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    decorationBox = { innerTextField ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(palette.glassSurfaceElevated)
                                .border(
                                    1.dp,
                                    palette.accentPrimary.copy(alpha = 0.5f),
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            if (smscInput.isEmpty()) {
                                Text(
                                    text = "+381...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = palette.textSecondary.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        },
        confirmButton = {
            GlassCard(
                shape = RoundedCornerShape(50),
                elevated = true,
                accentHighlight = palette.accentPrimary,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                onClick = {
                    onSave(smscInput.trim())
                    onDismiss()
                }
            ) {
                Text(
                    text = strings.confirm,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.accentPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            GlassCard(
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                onClick = onDismiss
            ) {
                Text(
                    text = strings.cancel,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Composable
private fun MmsApnEditorDialog(
    currentApn: MmsApnSettings,
    strings: AppStrings,
    onSave: (MmsApnSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalGlassPalette.current
    var mmscInput by remember { mutableStateOf(currentApn.mmscUrl) }
    var proxyInput by remember { mutableStateOf(currentApn.mmsProxy) }
    var portInput by remember { mutableStateOf(currentApn.mmsPort.toString()) }

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = strings.mmsApnManualDialogTitle,
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) {
                Text(
                    text = strings.mmsApnSettingsSub,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    lineHeight = 16.sp
                )

                Text(
                    text = strings.mmsApnMmscLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                BasicTextField(
                    value = mmscInput,
                    onValueChange = { mmscInput = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(palette.accentPrimary),
                    decorationBox = { innerTextField ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(palette.glassSurfaceElevated)
                                .border(
                                    1.dp,
                                    palette.accentPrimary.copy(alpha = 0.4f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            if (mmscInput.isEmpty()) {
                                Text(
                                    text = strings.mmsApnEnterMmscPlaceholder,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Text(
                    text = strings.mmsApnProxyLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                BasicTextField(
                    value = proxyInput,
                    onValueChange = { proxyInput = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(palette.accentPrimary),
                    decorationBox = { innerTextField ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(palette.glassSurfaceElevated)
                                .border(
                                    1.dp,
                                    palette.accentPrimary.copy(alpha = 0.4f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            if (proxyInput.isEmpty()) {
                                Text(
                                    text = strings.mmsApnEnterProxyPlaceholder,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Text(
                    text = strings.mmsApnPortLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                BasicTextField(
                    value = portInput,
                    onValueChange = { portInput = it.filter { ch -> ch.isDigit() } },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(palette.accentPrimary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    decorationBox = { innerTextField ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(palette.glassSurfaceElevated)
                                .border(
                                    1.dp,
                                    palette.accentPrimary.copy(alpha = 0.4f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            if (portInput.isEmpty()) {
                                Text(
                                    text = strings.mmsApnEnterPortPlaceholder,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        },
        confirmButton = {
            GlassCard(
                shape = RoundedCornerShape(50),
                elevated = true,
                accentHighlight = palette.accentPrimary,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                onClick = {
                    val port = portInput.toIntOrNull() ?: 8080
                    onSave(
                        MmsApnSettings(
                            carrierName = currentApn.carrierName.ifBlank { "Korisnički APN" },
                            mmscUrl = mmscInput.trim(),
                            mmsProxy = proxyInput.trim(),
                            mmsPort = port,
                            source = "Ručno podešeno"
                        )
                    )
                    onDismiss()
                }
            ) {
                Text(
                    text = strings.confirm,
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.accentPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            GlassCard(
                shape = RoundedCornerShape(50),
                elevated = false,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                onClick = onDismiss
            ) {
                Text(
                    text = strings.cancel,
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.textSecondary
                )
            }
        }
    )
}

@Composable
private fun SimMessagesDialog(
    strings: AppStrings,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalGlassPalette.current
    var simMessages by remember { mutableStateOf<List<SimCardMessage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            simMessages = SmsMmsRepository.getSimMessages(context)
        }
        isLoading = false
    }

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = strings.simMessagesTitle,
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .padding(top = 4.dp)
            ) {
                if (isLoading) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
                    ) {
                        Text(
                            text = strings.simMessagesLoading,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                    }
                } else if (simMessages.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
                    ) {
                        Text(
                            text = strings.simMessagesEmpty,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(simMessages, key = { it.id }) { msg ->
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(10.dp)
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = msg.address.ifBlank { "SIM" },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = palette.accentPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = formatTimestamp(msg.timestamp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            color = palette.textSecondary
                                        )
                                    }
                                    Text(
                                        text = msg.body,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textPrimary
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                    ) {
                                        GlassCard(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            accentHighlight = palette.safeColor,
                                            contentPadding = PaddingValues(vertical = 5.dp),
                                            onClick = {
                                                val copied = SmsMmsRepository.copySimMessageToInbox(context, msg)
                                                if (copied) {
                                                    Toast.makeText(context, strings.simMessageCopiedToast, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        ) {
                                            Text(
                                                text = strings.simMessageCopyInbox,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 11.sp,
                                                color = palette.safeColor,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        GlassCard(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            accentHighlight = palette.dangerColor,
                                            contentPadding = PaddingValues(vertical = 5.dp),
                                            onClick = {
                                                val deleted = SmsMmsRepository.deleteSimMessage(context, msg.id)
                                                if (deleted) {
                                                    simMessages = simMessages.filterNot { it.id == msg.id }
                                                    Toast.makeText(context, strings.simMessageDeletedToast, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        ) {
                                            Text(
                                                text = strings.simMessageDelete,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 11.sp,
                                                color = palette.dangerColor,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            GlassCard(
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                onClick = onDismiss
            ) {
                Text(
                    text = strings.cancel,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.accentPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Composable
private fun SwipeDirectionInteractiveCard(
    title: String,
    currentAction: SwipeAction,
    defaultFallbackAction: SwipeAction,
    strings: AppStrings,
    onSelectAction: (SwipeAction) -> Unit
) {
    val palette = LocalGlassPalette.current
    val enabled = currentAction != SwipeAction.NONE
    val activeDropdown = LocalActiveDropdownId.current
    val dropdownKey = "swipe_${title.hashCode()}"
    val expanded = activeDropdown.value == dropdownKey
    val selectableActions = remember {
        SwipeAction.entries.filter { it != SwipeAction.NONE }
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevated = enabled || expanded,
        accentHighlight = if (enabled) palette.accentPrimary else null,
        contentPadding = PaddingValues(14.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        activeDropdown.value = if (activeDropdown.value == dropdownKey) null else dropdownKey
                    }
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = swipeActionLabel(currentAction, strings),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (enabled) palette.accentPrimary else palette.textSecondary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = enabled,
                    onCheckedChange = { turnOn ->
                        if (turnOn) {
                            onSelectAction(defaultFallbackAction)
                            activeDropdown.value = dropdownKey
                        } else {
                            onSelectAction(SwipeAction.NONE)
                            if (activeDropdown.value == dropdownKey) {
                                activeDropdown.value = null
                            }
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = if (palette.isDark) Color.Black else Color.White,
                        checkedTrackColor = palette.accentPrimary,
                        uncheckedThumbColor = palette.textSecondary,
                        uncheckedTrackColor = palette.glassSurfaceElevated
                    )
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                ) {
                    selectableActions.chunked(2).forEach { rowActions ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowActions.forEach { action ->
                                val selected = action == currentAction
                                GlassCard(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    elevated = selected,
                                    accentHighlight = if (selected) palette.accentPrimary else null,
                                    contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp),
                                    onClick = { onSelectAction(action) }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.CenterStart,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = swipeActionIcon(action),
                                                contentDescription = null,
                                                tint = if (selected) palette.accentPrimary else palette.textSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(7.dp))
                                            Text(
                                                text = swipeActionLabel(action, strings),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 11.sp,
                                                lineHeight = 13.sp,
                                                color = if (selected) palette.accentPrimary else palette.textPrimary.copy(alpha = 0.88f),
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                            if (rowActions.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeletedMessagesCollapsibleCard(
    autoDeleteDays: Int,
    onSelectAutoDeleteDays: (Int) -> Unit,
    deletedGroupingMode: DeletedGroupingMode,
    onSelectDeletedGroupingMode: (DeletedGroupingMode) -> Unit,
    deletedMessagesCount: Int,
    onOpenDeletedMessages: () -> Unit,
    strings: AppStrings
) {
    val palette = LocalGlassPalette.current
    val isExpressive = LocalExpressiveAnimations.current
    val activeDropdown = LocalActiveDropdownId.current
    val expanded = activeDropdown.value == "deleted_messages"
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = if (isExpressive) {
            TxturaMotion.rotateSpec()
        } else {
            tween(200)
        },
        label = "deleted_messages_arrow_rotation"
    )
    var autoDeleteDaysInput by remember { mutableStateOf(autoDeleteDays.toString()) }
    LaunchedEffect(autoDeleteDays) {
        if (autoDeleteDaysInput.isNotEmpty() && autoDeleteDaysInput.toIntOrNull() != autoDeleteDays) {
            autoDeleteDaysInput = autoDeleteDays.toString()
        }
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevated = expanded,
        accentHighlight = if (expanded) palette.accentPrimary else null,
        contentPadding = PaddingValues(14.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header: Clickable to toggle dropdown
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        activeDropdown.value = if (activeDropdown.value == "deleted_messages") null else "deleted_messages"
                    }
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(palette.accentPrimary.copy(alpha = 0.14f))
                            .border(1.dp, palette.accentPrimary.copy(alpha = 0.42f), CircleShape)
                    ) {
                        Icon(
                            imageVector = TxturaIcons.Delete,
                            contentDescription = null,
                            tint = palette.accentPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = strings.deletedMessagesTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(palette.glassSurfaceElevated)
                        .border(0.8.dp, palette.glassBorderTop.copy(alpha = 0.35f), CircleShape)
                ) {
                    Icon(
                        imageVector = TxturaIcons.ArrowDown,
                        contentDescription = strings.deletedMessagesTitle,
                        tint = palette.accentPrimary,
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(arrowRotation)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = TxturaMotion.verticalExpandEnter(isExpressive = isExpressive),
                exit = TxturaMotion.verticalCollapseExit(isExpressive = isExpressive)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.8.dp)
                            .background(palette.glassBorderTop.copy(alpha = 0.28f))
                    )

                    // 1) Polje za unos broja dana za automatsko brisanje obrisanih poruka
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = strings.autoDeleteDaysTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(palette.glassSurfaceElevated)
                                .border(
                                    width = 1.dp,
                                    color = palette.accentPrimary.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.widthIn(min = 38.dp)
                            ) {
                                if (autoDeleteDaysInput.isEmpty()) {
                                    Text(
                                        text = "30",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        ),
                                        color = palette.textSecondary.copy(alpha = 0.5f)
                                    )
                                }
                                BasicTextField(
                                    value = autoDeleteDaysInput,
                                    onValueChange = { rawValue ->
                                        val digitsOnly = rawValue.filter { it.isDigit() }.take(3)
                                        autoDeleteDaysInput = digitsOnly
                                        if (digitsOnly.isEmpty()) {
                                            if (autoDeleteDays != 30) {
                                                onSelectAutoDeleteDays(30)
                                            }
                                        } else {
                                            val parsed = digitsOnly.toIntOrNull()
                                            if (parsed != null && parsed >= 1) {
                                                val clamped = parsed.coerceIn(1, 365)
                                                if (clamped != autoDeleteDays) {
                                                    onSelectAutoDeleteDays(clamped)
                                                }
                                            }
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = palette.accentPrimary,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    ),
                                    cursorBrush = SolidColor(palette.accentPrimary),
                                    modifier = Modifier.width(42.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = strings.daysCountLabel(autoDeleteDays).substringAfter(' '),
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.8.dp)
                            .background(palette.glassBorderTop.copy(alpha = 0.28f))
                    )

                    // 2) Grupisanje i sortiranje obrisanih poruka
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = strings.groupingModeTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            DeletedGroupingMode.entries.forEach { mode ->
                                val isSelected = deletedGroupingMode == mode
                                val optionLabel = when (mode) {
                                    DeletedGroupingMode.BY_DATE -> strings.groupByDateLabel
                                    DeletedGroupingMode.BY_CONTACT -> strings.groupByContactLabel
                                }
                                val optionIcon = when (mode) {
                                    DeletedGroupingMode.BY_DATE -> TxturaIcons.ClockTimer
                                    DeletedGroupingMode.BY_CONTACT -> TxturaIcons.ContactPerson
                                }

                                GlassCard(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    elevated = isSelected,
                                    accentHighlight = if (isSelected) palette.accentPrimary else null,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    onClick = { onSelectDeletedGroupingMode(mode) }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = optionIcon,
                                                contentDescription = null,
                                                tint = if (isSelected) palette.accentPrimary else palette.textSecondary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = optionLabel,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isSelected) palette.accentPrimary else palette.textSecondary,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.8.dp)
                            .background(palette.glassBorderTop.copy(alpha = 0.28f))
                    )

                    // 3) Dugme za otvaranje obrisanih poruka
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        elevated = true,
                        accentHighlight = palette.accentPrimary,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        onClick = onOpenDeletedMessages
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = TxturaIcons.Restore,
                                    contentDescription = null,
                                    tint = palette.accentPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = strings.openDeletedMessagesButton(deletedMessagesCount),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = palette.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "→",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.accentPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityMeasuresDialog(
    onDismiss: () -> Unit,
    identifyMaliciousWebsites: Boolean = true,
    onNavigateToSetting: (SettingsCategoryWindow, String) -> Unit = { _, _ -> }
) {
    val palette = LocalGlassPalette.current
    val strings = TxturaStrings.current()
    val scrollState = rememberScrollState()

    val isIntentSanitizerActive = true
    val isPhotoPickerActive = true
    val isSensitiveClipboardActive = true
    val isProtectedReceiversActive = true
    val isBackupEncryptionActive = true

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        accentBorderColor = palette.safeColor,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = TxturaIcons.ShieldCheck,
                    contentDescription = null,
                    tint = palette.safeColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.securitySectionTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = strings.securitySummaryDisclaimer,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                SecurityItemRow(
                    title = strings.secItem2Title,
                    subtitle = strings.secItem2Sub,
                    isActive = isIntentSanitizerActive,
                    isConfigurable = false,
                    onClick = {}
                )
                SecurityItemRow(
                    title = strings.secItem3Title,
                    subtitle = strings.secItem3Sub,
                    isActive = isPhotoPickerActive,
                    isConfigurable = false,
                    onClick = {}
                )
                SecurityItemRow(
                    title = strings.secItem4Title,
                    subtitle = strings.secItem4Sub,
                    isActive = isSensitiveClipboardActive,
                    isConfigurable = false,
                    onClick = {}
                )
                SecurityItemRow(
                    title = strings.secItem5Title,
                    subtitle = strings.secItem5Sub,
                    isActive = isProtectedReceiversActive,
                    isConfigurable = false,
                    onClick = {}
                )
                SecurityItemRow(
                    title = strings.secItem11Title,
                    subtitle = strings.secItem11Sub,
                    isActive = isBackupEncryptionActive,
                    isConfigurable = false,
                    onClick = {}
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = strings.back,
                    color = palette.accentPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

private data class ChangelogCategory(
    val title: String,
    val items: List<String>
)

@Composable
private fun ChangelogVersionSection(
    versionName: String,
    releaseDate: String,
    isCurrent: Boolean,
    categories: List<ChangelogCategory>
) {
    val palette = LocalGlassPalette.current
    val accent = if (isCurrent) palette.accentPrimary else palette.textSecondary

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = versionName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                text = releaseDate,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary.copy(alpha = 0.8f)
            )
        }

        categories.forEach { category ->
            Text(
                text = category.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                category.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = accent,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppChangelogDialog(
    currentVersionName: String,
    currentVersionCode: Long,
    cachedReleaseInfo: GitHubReleaseInfo?,
    onDismiss: () -> Unit
) {
    val palette = LocalGlassPalette.current
    val strings = TxturaStrings.current()
    val scrollState = rememberScrollState()

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        accentBorderColor = palette.accentPrimary,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(palette.accentPrimary.copy(alpha = 0.16f))
                        .border(1.dp, palette.accentPrimary.copy(alpha = 0.45f), CircleShape)
                ) {
                    Icon(
                        imageVector = TxturaIcons.Info,
                        contentDescription = null,
                        tint = palette.accentPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = strings.changelogDialogTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(scrollState)
            ) {
                    if (cachedReleaseInfo?.isUpdateAvailable == true) {
                        GlassCard(
                            shape = RoundedCornerShape(12.dp),
                            accentHighlight = palette.safeColor,
                            contentPadding = PaddingValues(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = TxturaIcons.SparklesTheme,
                                        contentDescription = null,
                                        tint = palette.safeColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = strings.changelogAvailableUpdateBadge(cachedReleaseInfo.versionTag),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.safeColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (cachedReleaseInfo.releaseTitle.isNotBlank() && cachedReleaseInfo.releaseTitle != cachedReleaseInfo.versionTag) {
                                    Text(
                                        text = cachedReleaseInfo.releaseTitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = palette.textPrimary
                                    )
                                }
                                if (cachedReleaseInfo.releaseNotes.isNotBlank()) {
                                    Text(
                                        text = cachedReleaseInfo.releaseNotes,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    GlassCard(
                        shape = RoundedCornerShape(12.dp),
                        accentHighlight = palette.accentPrimary,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = strings.changelogCurrentVersionBadge(currentVersionName, currentVersionCode),
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.accentPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    ChangelogVersionSection(
                        versionName = "v3.5",
                        releaseDate = if (strings.isSerbian) "Trenutno izdanje" else "Current Release",
                        isCurrent = true,
                        categories = listOf(
                            ChangelogCategory(
                                title = if (strings.isSerbian) "MMS telefonija i pozadinska sinhronizacija" else "MMS Telephony & Background Sync",
                                items = listOf(
                                    if (strings.isSerbian) "Automatska pozadinska MMS sinhronizacija: Pozadinski proces na svakih 3 minuta za automatsko ponovno slanje zaglavljenih odlaznih i preuzimanje pristiglih MMS poruka" else "Periodic background MMS retry engine: Background process running every 3 minutes via AlarmManager and JobScheduler to auto-retry stuck outgoing MMS and retrieve pending incoming MMS",
                                    if (strings.isSerbian) "Automatska detekcija MMS APN podešavanja: Pozadinsko preuzimanje i podešavanje MMS parametara (MMSC URL, Proxy, Port) prema SMSC broju i SIM operateru" else "SMSC-based MMS APN auto-detection: Background setup of carrier MMS APN parameters based on SMSC center number and SIM operator"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Zvučni enkoderi i mediji" else "Audio Encoders & Media Processing",
                                items = listOf(
                                    if (strings.isSerbian) "Konverzija i provera AMR-NB formata: Obrada glasovnih poruka i zvučnih zapisa uz proveru i transkodovanje u važeći AMR-NB format pre slanja" else "AMR-NB audio format converter: Ensures standard #!AMR\\n header compliance and transcodes non-AMR audio formats to valid AMR-NB before MMS transmission"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Interfejs i podešavanja" else "UI Layouts & Settings Refinements",
                                items = listOf(
                                    if (strings.isSerbian) "Simetrična APN kartica i naslovi u jednom redu: Redizajnirana APN podešavanja sa podjednakom dugmadi (50/50), prikaz naslova kartica u 1 redu i fiksiran prostor pri poništavanju slanja" else "Symmetrical APN card & single-line titles: Redesigned MMS APN card with 50/50 symmetrical action buttons, single-line settings card titles, and isolated countdown timer slot to eliminate flicker"
                                )
                            )
                        )
                    )

                    ChangelogVersionSection(
                        versionName = "v3.4",
                        releaseDate = if (strings.isSerbian) "Prethodno izdanje" else "Previous Release",
                        isCurrent = false,
                        categories = listOf(
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Ažuriranje i instalacija" else "App Updates & Installation",
                                items = listOf(
                                    if (strings.isSerbian) "Direktno preuzimanje i instalacija ažuriranja sa GitHub-a uz prikaz napretka u realnom vremenu i automatsko pokretanje instalatera bez otvaranja pretraživača" else "Direct APK update download from GitHub Releases with live percentage progress and automatic package installer launch without external browser"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Poruke i animacije" else "Messaging & Animations",
                                items = listOf(
                                    if (strings.isSerbian) "Animacija opruge pri pinovanju i otpinovanju: Glatka animacija premeštanja razgovora na vrh ili novu poziciju uz zadržavanje pozicije skrolovanja" else "Pin and unpin spring motion: Smooth placement spring animation when pinning or unpinning conversations with index-anchored viewport scroll"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Podešavanja i teme" else "Settings & Defaults",
                                items = listOf(
                                    if (strings.isSerbian) "Osvežene podrazumevane opcije: Sistemska tema kao podrazumevani izbor, Starlight Galaxy pozadina i optimizovani tonovi obaveštenja" else "Updated default settings: System dark/light theme as default, Starlight Galaxy wallpaper preset, and refined notification sounds"
                                )
                            )
                        )
                    )

                    ChangelogVersionSection(
                        versionName = "v3.3.1",
                        releaseDate = if (strings.isSerbian) "Prethodno izdanje" else "Previous Release",
                        isCurrent = false,
                        categories = listOf(
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Upravljanje obrisanim porukama" else "Selective Trash Management",
                                items = listOf(
                                    if (strings.isSerbian) "Selektivno vraćanje i trajno brisanje: Detaljan pregled obrisanih razgovora i izbor pojedinačnih poruka za vraćanje u inbox ili trajno uklanjanje iz memorije uređaja" else "Selective Trash recovery: Inspect deleted conversations and select individual messages to restore to inbox or permanently purge from device storage"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Interfejs, prilagođavanje i lokalizacija" else "UI, Customization & Localization",
                                items = listOf(
                                    if (strings.isSerbian) "Datumski zaglavci na srpskoj latinici: Datumski razdelnici u razgovoru primenjuju srpsku latinicu (npr. utorak, 19. maj)" else "Serbian Latin date headers: Conversation date dividers use Serbian Latin script (e.g., utorak, 19. maj) for consistent readability",
                                    if (strings.isSerbian) "Brojač nepročitanih poruka: Kategorija Nepročitano prikazuje ukupan broj nepročitanih poruka desno od naziva (npr. Nepročitano (3))" else "Unread category count indicator: The Unread category chip displays the total number of currently unread messages in real-time (e.g., Unread (3))",
                                    if (strings.isSerbian) "Tematska ikonica za Burgundi teme: Ikonica medveda u glavnom zaglavlju za teme Burgundi Anita i Monohrom sa burgundi akcentom uz automatsku prilagodljivost svetlom i tamnom režimu" else "Burgundy theme branding: Custom bear icon rendered in top header for Burgundy Anita & Monochrome Burgundy themes with light/dark asset switching"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Pokreti i obaveštenja" else "Gestures & Notifications",
                                items = listOf(
                                    if (strings.isSerbian) "Glatko skrolovanje bez slučajnog obeležavanja: Detektor pokreta osigurava da skrolovanje preko poruka nikada ne pokreće smanjenje ili lažno obeležavanje poruke" else "Smooth scrolling without accidental selection: Optimized touch gesture pipeline so scrolling over message bubbles never triggers press scaling or false long-press selection",
                                    if (strings.isSerbian) "Otklonjeno lažno okidanje zvuka i blica: Ulazak u razgovor i učitavanje istorije poruka više ne aktivira zvučni i blic signal za dolazne poruke" else "Fix false incoming alert sounds on thread open: Opening a thread or loading historical messages no longer triggers incoming message sounds or flash alerts"
                                )
                            )
                        )
                    )

                    ChangelogVersionSection(
                        versionName = "v3.3",
                        releaseDate = if (strings.isSerbian) "Prethodno izdanje" else "Previous Release",
                        isCurrent = false,
                        categories = listOf(
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Bezbednost i arhitektura baze" else "Security & Database Architecture",
                                items = listOf(
                                    if (strings.isSerbian) "SQLCipher Room baza: Kriptovana baza podataka uz Room KSP, SQLCipher enkripciju i selektivno učitavanje istorije razgovora" else "SQLCipher Room database: Encrypted storage with Room KSP, SQLCipher database encryption, and granular lazy loading for conversation history",
                                    if (strings.isSerbian) "Vidljivost aplikacija i sertifikati: Dodat eksplicitan unos u AndroidManifest.xml za root aplikacije i osveženo HTTPS/TLS povezivanje sertifikata" else "Package visibility & certificates: Explicit AndroidManifest.xml queries for root tools and refined HTTPS/TLS certificate pinning"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Performanse i životni ciklus" else "Performance & Lifecycle",
                                items = listOf(
                                    if (strings.isSerbian) "Brži životni ciklus: Inicijalizacija baze i hitnih obaveštenja direktno u MainActivity.onCreate() bez TxturaApp klase za brže hladno pokretanje" else "Streamlined lifecycle: Async repository and emergency alert initialization directly in MainActivity.onCreate() for faster cold starts",
                                    if (strings.isSerbian) "Deduplikacija i smanjeno opterećenje: Automatsko uklanjanje duplikata poruka i kontrola učestalosti sinhronizacije sa uređajem" else "Deduplication & sync throttling: Automated message deduplication and throttled background telephony syncs"
                                )
                            )
                        )
                    )

                    ChangelogVersionSection(
                        versionName = "v3.2",
                        releaseDate = if (strings.isSerbian) "Prethodno izdanje" else "Previous Release",
                        isCurrent = false,
                        categories = listOf(
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Performanse i bezbednost" else "Performance & Security",
                                items = listOf(
                                    if (strings.isSerbian) "Brže pokretanje: Učitavanje podešavanja, lokalnog keša i hitnih obaveštenja premešteno je van glavne niti; sinhronizacija poruka čeka asinhrono učitavanje keša" else "Faster startup: Preferences, local cache, and emergency-alert initialization run off the main thread; message sync waits asynchronously for cache loading",
                                    if (strings.isSerbian) "AES-256-GCM ključ keša: Lokalni JSON snapshot koristi nasumični 256-bitni ključ zaštićen Android Keystore-om, uz migraciju starog formata keša" else "AES-256-GCM cache key: Local JSON snapshots use a random 256-bit key wrapped by Android Keystore, with migration from the previous cache format",
                                    if (strings.isSerbian) "Bekapi bez blokiranja interfejsa: PBKDF2 zaštita lozinkom je zadržana, a čitanje, parsiranje i kriptografija bekapa izvršavaju se u pozadini" else "Non-blocking backups: Password-based PBKDF2 protection is retained while backup reading, parsing, and cryptography run in the background",
                                    if (strings.isSerbian) "Jasan opis enkripcije: Podešavanja prikazuju AES-256-GCM zaštitu lokalnog JSON keša poruka" else "Clear encryption details: Settings describe AES-256-GCM protection of the local JSON message cache"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Poruke i navigacija" else "Messaging & Navigation",
                                items = listOf(
                                    if (strings.isSerbian) "Animacije pinovanja: Razgovori se animirano vraćaju na prethodno mesto pri otpinovanju, a pin ikonice se skrivaju kada je pinovanje dodeljeno prevlačenju" else "Pin motion: Conversations animate back to their prior position when unpinned, and pin icons are hidden when pinning is assigned to a swipe action",
                                    if (strings.isSerbian) "Sistemski pokret za nazad: Gest navigacije poziva isto ponašanje kao strelica za nazad u menijima" else "System back gesture: Android navigation gestures trigger the same behavior as menu back buttons",
                                    if (strings.isSerbian) "Slanje i prijem SMS/MMS poruka: Registrovane su povratne akcije slanja i preuzimanja, a WAP Push sadržaj se proverava i obrađuje asinhrono" else "SMS/MMS delivery: Sent and downloaded callbacks are registered; WAP Push content is validated and processed asynchronously"
                                )
                            )
                        )
                    )

                    ChangelogVersionSection(
                        versionName = "v3.1",
                        releaseDate = if (strings.isSerbian) "Prethodno izdanje" else "Previous Release",
                        isCurrent = false,
                        categories = listOf(
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Upravljanje memorijom i dozvolama" else "Storage & Permissions Management",
                                items = listOf(
                                    if (strings.isSerbian) "Trajno brisanje iz memorije uređaja: Pražnjenje smeća i trajno uklanjanje razgovora sada fizički uklanja sve poruke (SMS, MMS) i multimedijalne priloge (audio zapise, slike, fajlove) iz baze uređaja i interne memorije" else "Permanent storage deletion: Emptying trash and permanently deleting conversations now thoroughly removes messages (SMS, MMS) and all media attachments from device telephony storage and internal memory",
                                    if (strings.isSerbian) "Automatski zahtev za dozvolu za kalendar: Dozvola za kalendar se sada traži odmah pri prvom pokretanju aplikacije uz ostale sistemske dozvole, omogućavajući kreiranje i slanje kalendarskih .ics događaja" else "First-launch calendar permission prompt: Calendar permission is now requested during the initial app startup alongside core permissions, enabling calendar event picker and .ics generation"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Interfejs i navigacija" else "UI & Navigation",
                                items = listOf(
                                    if (strings.isSerbian) "Prilagodljivi dok bar na svim ekranima: Ikonice dok bara se mogu slobodno premeštati prevlačenjem levo-desno, a dok bar ostaje postojan na ekranima Obrisane poruke i Hajlajtovane poruke" else "Customizable dock bar: Dock bar icons can be freely reordered via drag-and-drop, and the dock bar remains seamlessly visible on Deleted and Highlighted screens",
                                    if (strings.isSerbian) "Usklađene animacije i brzi prelazi: Prelaz između glavnog menija i podešavanja, hajlajtovanih i obrisanih poruka optimizovan je bez zadrške i treperenja uz prirodnu fiziku opruge" else "Synchronized transitions: Smooth spring transitions between main screen, settings, highlighted, and deleted messages with no lag or flicker",
                                    if (strings.isSerbian) "GIF animacije i tastatura: Direktno slanje GIF animacija iz tastature do 299.9 kB sa automatskim prepoznavanjem i reprodukcijom unutar oblačića poruke" else "Keyboard GIF animations: Direct sending of GIFs up to 299.9 kB from keyboard with automatic detection and animated playback in chat bubbles"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Obaveštenja i odziv" else "Notifications & Feedback",
                                items = listOf(
                                    if (strings.isSerbian) "Sinhronizovani svetlosni odziv: Blic obaveštenja počinju u tačnoj sinhronizaciji sa zvukom dolazne SMS poruke" else "Synchronized flash notifications: Camera flashlight pulse starts simultaneously with incoming SMS audio alerts",
                                    if (strings.isSerbian) "Zvučni odzivi: Proširena podešavanja za širok spektar tonova i zvučnih povratnih informacija" else "Audio feedback: Expanded sound settings covering custom tones and operational audio feedback"
                                )
                            )
                        )
                    )

                    ChangelogVersionSection(
                        versionName = "v2.4",
                        releaseDate = "10.11.2026.",
                        isCurrent = false,
                        categories = listOf(
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Tonovi obaveštenja i zvučni efekti" else "Notification Sounds & Audio Effects",
                                items = listOf(
                                    if (strings.isSerbian) "Prilagođeni tonovi obaveštenja: 6 novih visokokvalitetnih melodija (Dupli puls, Kristalni zvončić, Meki pop, Ritam kod, Topla kalimba, Diskretni oblak) uz trenutni pregled zvuka" else "Custom notification tones: 6 curated high-quality notification melodies (Double Pulse, Crystal Chime, Soft Pop, Rhythm Code, Warm Kalimba, Discreet Cloud) with instant preview",
                                    if (strings.isSerbian) "Sistemski meni za zvuk: Dinamičko podešavanje 'Podesi zvuk sistema' koje se pojavljuje kada je odabran Sistemski ton i vodi direktno na sistemski meni kanala za izbor melodije" else "System sound integration: Dynamic 'Set system sound' option when System tone is chosen, directly opening the system notification channel sound settings",
                                    if (strings.isSerbian) "Zvučni efekti akcija: Novi namenski zvuci za hajlajtovanje, uklanjanje hajlajta, brisanje u kantu, trajno brisanje, pinovanje, otpinovanje i označavanje statusa poruka" else "Action sound effects: Distinct in-app sound effects for highlighting, unhighlighting, trash deletion, permanent deletion, pinning, unpinning, and message status"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Privatnost i bezbednost" else "Privacy & Security",
                                items = listOf(
                                    if (strings.isSerbian) "Selektivna zabrana snimanja ekrana: Nova opcija u Privatnosti i bezbednosti koja dinamički sprečava skrinšotovanje četova i liste poruka, dok su podešavanja i ostali delovi aplikacije slobodni za snimanje" else "Selective chat screenshot protection: New setting in Privacy & Security to selectively prevent screenshots and screen recordings in conversations and message lists while leaving other screens accessible"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Korisničko iskustvo i performanse" else "UX & Performance",
                                items = listOf(
                                    if (strings.isSerbian) "Poboljšanja nove poruke: Uklonjena ikonica iz polja primaoca radi maksimalne širine i pune preglednosti svih izabranih kontakata" else "New message improvements: Removed contact icon from the recipient field to provide maximum width and full visibility of all chosen contacts",
                                    if (strings.isSerbian) "Uvek uključen ekran u četu: Ekran ostaje uključen dok ste unutar aktivnog razgovora kako se ne bi gasio tokom kucanja i čitanja" else "Keep screen on in chat: Screen stays awake while viewing a conversation, preventing screen timeout",
                                    if (strings.isSerbian) "Optimizacija prelaza i menija: Uklonjeno zadržavanje pri povratku na glavni ekran i osiguran samo jedan otvoren padajući meni u isto vreme" else "Transition & menu optimization: Eliminated lag when returning to the main screen and ensured only a single dropdown menu can be open at a time"
                                )
                            )
                        )
                    )

                    ChangelogVersionSection(
                        versionName = "v2.2",
                        releaseDate = "09.11.2026.",
                        isCurrent = false,
                        categories = listOf(
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Deduplikacija i konsolidacija kontakata" else "Contact Deduplication & Consolidation",
                                items = listOf(
                                    if (strings.isSerbian) "Napredno spajanje konverzacija: Otklonjena pojava istih kontakata više puta na listi, uz automatsko objedinjavanje poruka, nepročitanih brojača i statusa zakačenosti" else "Advanced thread merging: Completely eliminates duplicate conversation cards for the same person, automatically consolidating messages, unread counts, and pins",
                                    if (strings.isSerbian) "Višefaktorsko prepoznavanje: Pametno uparivanje po imenu kontakta iz imenika, normalizovanim ciframa broja telefona i preklapanju brojeva unutar poruka" else "Multi-factor matching: Smart pairing using saved address book names, normalized phone digits, and overlapping message phone numbers"
                                )
                            )
                        )
                    )

                    ChangelogVersionSection(
                        versionName = "v2.1",
                        releaseDate = "09.11.2026.",
                        isCurrent = false,
                        categories = listOf(
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Višestruko slanje poruka" else "Multi-Recipient Messaging",
                                items = listOf(
                                    if (strings.isSerbian) "Simultano slanje poruke: Slanje jedne poruke na više izabranih kontakata i unetih brojeva istovremeno kao pojedinačni SMS bez kreiranja grupnih MMS poruka" else "Simultaneous broadcast: Send a single message to multiple chosen contacts and typed numbers concurrently as direct 1-on-1 SMS without group MMS",
                                    if (strings.isSerbian) "Pametni birač primalaca: Biranje kontakata putem čipova, mogućnost izbora ili poništavanja svih predloga jednim dodirom, brojač karaktera i SMS segmenata (N • M SMS)" else "Smart recipient picker: Chip selection, Select All/Deselect All toggle, live character count and SMS segment calculator",
                                    if (strings.isSerbian) "Brza akcija u razgovoru: Nova opcija 'Pošalji na više brojeva' unutar menija sa tri tačkice u aktivnom razgovoru" else "Conversation shortcut: New 'Send to multiple numbers' action directly in chat details menu"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Pouzdanost biometrije i zaštite" else "Biometrics & Security Reliability",
                                items = listOf(
                                    if (strings.isSerbian) "Ispravka otključavanja: Dodata sistemska USE_BIOMETRIC dozvola i izvorna Android BiometricPrompt podrška (API 29–37) za otisak, lice i PIN ekrana" else "Unlock fix: Added native USE_BIOMETRIC permission and native BiometricPrompt (API 29–37) for fingerprint, face, and device PIN",
                                    if (strings.isSerbian) "Zaštita od blokiranja bez PIN-a: Ako uređaj nema podešeno zaključavanje ekrana, aplikacija omogućava trenutno otključavanje jednim dodirom i sprečava greške" else "No-lockout protection: If device has no screen lock or biometrics configured, the app enables instant one-tap unlocking and prevents lockouts",
                                    if (strings.isSerbian) "Sprečeno lažno ponovno zaključavanje: Rešena petlja ponovnog zaključavanja prilikom povratka iz sistemskog dijaloga" else "Lifecycle fix: Resolved re-lock loop when returning from system authentication dialogues"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Usluge, Haptika i Poliranje interfejsa" else "Services, Haptics & UI Polish",
                                items = listOf(
                                    if (strings.isSerbian) "Pametno prepoznavanje usluga: Imenovani kontakti iz imenika i prepiske sa vašim odgovorima nikada se više ne svrstavaju greškom u 'Usluge'" else "Smart service categorization: Named contacts and threads with user replies are protected from false service classification",
                                    if (strings.isSerbian) "Haptika na prevlačenju: Taktički vibracioni odziv na swipe akcije, prag prevlačenja kartica, listanje zakačenih poruka i raspoređivanje kategorija" else "Swipe haptics: Tactile vibration feedback on conversation swipes, threshold crossing, pinned message carousel, and category reordering",
                                    if (strings.isSerbian) "Poliranje srpskog prikaza: Prilagođene širine kartica kategorija tako da se 'Nepročitano' nikada ne seče ili sabija, i optimizovan tekst tastera u korpi ('Isprazni sve')" else "Serbian UI polish: Optimized category tab weights preventing 'Nepročitano' truncation, with clean button styling"
                                )
                            )
                        )
                    )

                    ChangelogVersionSection(
                        versionName = "v2.0",
                        releaseDate = "08.11.2026.",
                        isCurrent = false,
                        categories = listOf(
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Privatnost i bezbednost" else "Privacy & Security",
                                items = listOf(
                                    if (strings.isSerbian) "Šifrovanje lokalnog keša poruka: AES-256-GCM enkripcija sa nasumičnim ključem zaštićenim Android Keystore-om" else "Local message cache encryption: AES-256-GCM with a random data key protected by Android Keystore",
                                    if (strings.isSerbian) "Biometrijska zaštita i PIN: Zaključavanje aplikacije otiskom prsta ili uređajnim PIN-om uz podesiv tajmaut (Odmah, 1 min, 5 min) i namenski stakleni ekran zaključavanja" else "Biometric protection & PIN: Lock the entire app with fingerprint or device PIN, with customizable lock timeout (Immediate, 1 min, 5 min) and dedicated glass lock screen",
                                    if (strings.isSerbian) "HTTPS i TLS/SSL Pinning: Striktna upotreba šifrovanih veza uz SPKI sertifikat pinning za sprečavanje Man-in-the-Middle napada i automatsko blokiranje nešifrovanog HTTP saobraćaja" else "HTTPS & TLS/SSL Pinning: Strict encrypted connections with SPKI certificate pinning to prevent Man-in-the-Middle attacks, blocking plaintext HTTP",
                                    if (strings.isSerbian) "Minimizacija metapodataka: Automatsko uklanjanje EXIF GPS koordinata i detalja sa MMS slika, FLAG_SECURE zaštita od snimanja ekrana i cenzurisana obaveštenja na zaključanom ekranu" else "Metadata minimization: Automated EXIF GPS/camera data stripping from MMS photos, FLAG_SECURE screenshot prevention, and redacted lock-screen notifications",
                                    if (strings.isSerbian) "Detekcija Root stanja: Dubinsko skeniranje integriteta operativnog sistema (provera su binarnih fajlova, Magisk/KernelSU menadžera i test-keys potpisa)" else "Root state detection: Real-time system integrity scan checking su binaries, Magisk/KernelSU managers, and test-keys build tags",
                                    if (strings.isSerbian) "Blokirani pošiljaoci: Centralizovano upravljanje blokiranim brojevima unutar bezbednosne grupe" else "Blocked senders: Centralized blocked contacts management integrated into the security hub"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Redizajn podešavanja i Plutajući meniji" else "Settings Redesign & Floating Menus",
                                items = listOf(
                                    if (strings.isSerbian) "Kompletan redizajn podešavanja raspoređen u 7 preglednih kategorija po rednim brojevima" else "Complete settings redesign structured into 7 clear numbered thematic categories",
                                    if (strings.isSerbian) "Izbornici se otvaraju kao kompaktni plutajući mini-meniji sa desne strane svake opcije umesto iskačućih dijaloga" else "Option selectors open as compact floating dropdown menus to the right of each row instead of popup dialogs",
                                    if (strings.isSerbian) "Uklonjeni suvišni opisi sa glavnog menija radi čistijeg i preglednijeg izgleda bez pretrpanosti" else "Removed clutter and redundant subtitles from main category rows for a cleaner interface"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Poruke, Interakcija i Ažuriranja" else "Messaging, Interaction & Updates",
                                items = listOf(
                                    if (strings.isSerbian) "Poništavanje slanja: Ikonica za slanje se tokom 6s odbrojavanja zamenjuje crvenim X tasterom uz autentični zvučni efekat poništavanja" else "Send recall: Send button is replaced by a red X cancel button during the 6s countdown with an authentic cancel sound effect",
                                    if (strings.isSerbian) "Pametni birač za novu poruku: Pretraga kontakata u realnom vremenu, unos novog broja i direktan prelazak u razgovor preko tastera Sledeće" else "Smart new message picker: Real-time contact matching, manual phone entry, and direct transition to chat via Next button",
                                    if (strings.isSerbian) "Automatska provera ažuriranja pri svakom ulasku u aplikaciju uz sistemsko obaveštenje i direktno otvaranje sekcije za ažuriranje" else "Automatic update check on every app launch with push notification and one-tap navigation to the update hub"
                                )
                            )
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = strings.changelogPreviousVersionsHeader,
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textSecondary,
                        fontWeight = FontWeight.Bold
                    )

                    ChangelogVersionSection(
                        versionName = "v1.5.1",
                        releaseDate = "08.11.2026.",
                        isCurrent = false,
                        categories = listOf(
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Upravljanje porukama i Korpa" else "Message Handling & Trash",
                                items = listOf(
                                    if (strings.isSerbian) "Pojedinačne i označene poruke koje se brišu unutar samog četa premeštaju se u Obrisane poruke (korpu) umesto trajnog brisanja" else "Individual and multi-selected messages deleted directly from inside a chat are now moved to Trash instead of immediate permanent deletion",
                                    if (strings.isSerbian) "Podrška za vraćanje (Restore) obrisanih poruka nazad u aktivni razgovor i automatsko uklanjanje nakon isteka definisanog roka" else "Full support for restoring trashed messages back into the active thread and auto-delete lifecycle purging",
                                    if (strings.isSerbian) "Učitavanje kompletne istorije: uklonjena veštačka ograničenja (LIMIT) tako da se uvek učitavaju sve poruke iz memorije uređaja" else "Full message history loading: removed query LIMIT constraints to always load all stored SMS/MMS messages from device memory"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Korisnički interfejs i Podešavanja" else "User Interface & Settings",
                                items = listOf(
                                    if (strings.isSerbian) "Donji navigacioni bar i traka za masovno označavanje prošireni na punu širinu ekrana radi savršenog uklapanja sa karticama" else "Bottom dock bar and selection bar expanded to full width to seamlessly match the rest of the interface",
                                    if (strings.isSerbian) "Povećane ikonice i komfornije dodirne zone na donjem navigacionom baru" else "Enlarged icons and more comfortable touch targets on the bottom dock bar",
                                    if (strings.isSerbian) "Objedinjena podešavanja za Mehuriće i Eksterne linkove u pregledne padajuće kartice ispod opcija automatskog uklanjanja poruka" else "Consolidated Bubbles and External Links options into elegant collapsible cards under auto-delete settings"
                                )
                            )
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = strings.changelogPreviousVersionsHeader,
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textSecondary,
                        fontWeight = FontWeight.Bold
                    )

                    ChangelogVersionSection(
                        versionName = "v1.5",
                        releaseDate = "08.11.2026.",
                        isCurrent = false,
                        categories = listOf(
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Pretraga, Isticanje i Nacrti" else "Search, Pin, Highlights & Drafts",
                                items = listOf(
                                    if (strings.isSerbian) "Interaktivna pretraga unutar razgovora sa brojačem pogodaka i brzom navigacijom" else "Interactive in-chat message search with match counter and navigation",
                                    if (strings.isSerbian) "Centralizovan pregled svih hajlajtovanih poruka od svih pošiljalaca na jednom mestu" else "Unified hub for all highlighted messages across all chats with jump-to-message",
                                    if (strings.isSerbian) "Kačenje važnih poruka na vrh konverzacije (Pin / Unpin)" else "Pin and unpin important messages to the top of conversation",
                                    if (strings.isSerbian) "Automatsko lokalno čuvanje nacrta (Draft) po razgovoru sa prikazom u listi poruka" else "Automatic per-thread draft saving with clear preview badges in thread list"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Grupno slanje i Pametne radnje" else "Broadcast Messaging & Smart Actions",
                                items = listOf(
                                    if (strings.isSerbian) "Slanje jedne poruke većem broju primalaca u zasebnim 1-na-1 privatnim razgovorima" else "Multi-recipient broadcast messaging in separate private 1-on-1 chats",
                                    if (strings.isSerbian) "Prepoznavanje brojeva i email adresa u porukama uz brze akcije (poziv, mail, kontakti)" else "Smart recognition of phone numbers and emails in received messages with quick actions",
                                    if (strings.isSerbian) "Brzo deljenje kontakata iz imenika telefona direktno u polje za unos poruke" else "Quick contact sharing from device phonebook directly into compose bar",
                                    if (strings.isSerbian) "Brzi taster za planiranje čestitki u ponoć za rođendan (00:00) sa gotovom porukom" else "Birthday midnight preset (00:00) with ready-made greeting for scheduled sending"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Interakcija, Zvuk i Mehurići" else "Interaction, In-App Audio & Bubbles",
                                items = listOf(
                                    if (strings.isSerbian) "Zvučni efekti pri slanju i prijemu poruka dok je razgovor otvoren" else "In-app sound effects for outgoing and incoming messages while chatting",
                                    if (strings.isSerbian) "Uštipnite ekran (Pinch-to-zoom) za trenutno podešavanje veličine teksta razgovora" else "Pinch-to-zoom gesture to dynamically resize conversation text bubbles",
                                    if (strings.isSerbian) "Izražajne fizike animacija i integracija sa sistemskim plutajućim mehurićima" else "Expressive bubble physics and system floating notification bubbles integration",
                                    if (strings.isSerbian) "Prilagođavanje automatskih pregleda linkova i multimedije (Wi-Fi režim, veb-linkovi)" else "Customizable automatic link and rich media preview controls (Wi-Fi only, web only)"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Bezbednost, SIM i Personalizacija" else "Security, SIM & Personalization",
                                items = listOf(
                                    if (strings.isSerbian) "Zaštita od fišinga i sumnjivih sajtova u realnom vremenu preko Cloudflare DoH" else "Real-time anti-phishing and malicious link security inspection via Cloudflare DoH",
                                    if (strings.isSerbian) "Opoziv slanja (Undo) i odloženo slanje poruka pre prenosa preko mreže" else "Delayed send undo and recall mechanism before network transmission",
                                    if (strings.isSerbian) "Pregled, kopiranje u sanduče i brisanje poruka sačuvanih na SIM kartici" else "View, copy to inbox, and delete messages stored on the SIM card",
                                    if (strings.isSerbian) "Prikaz i podešavanje broja SMS centra mobilnog operatera (SMSC)" else "Carrier SMS center number (SMSC) viewing and custom configuration",
                                    if (strings.isSerbian) "Minimalistički pravi AMOLED crni i čisto beli režim za uštedu baterije" else "Minimalist pure AMOLED black and clean white theme presets for power saving",
                                    if (strings.isSerbian) "Ručna promena kategorije pošiljaoca (Osobe ↔ Usluge) direktno iz opcija četa" else "Manual sender categorization toggle between People and Services from chat menu"
                                )
                            )
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = strings.changelogPreviousVersionsHeader,
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textSecondary,
                        fontWeight = FontWeight.Bold
                    )

                    ChangelogVersionSection(
                        versionName = "v1.0",
                        releaseDate = "08.11.2026.",
                        isCurrent = false,
                        categories = listOf(
                            ChangelogCategory(
                                title = if (strings.isSerbian) "SMS i MMS napredne opcije" else "SMS & MMS Advanced Options",
                                items = listOf(
                                    if (strings.isSerbian) "Izveštaji o isporuci za svaku poslatu SMS i MMS poruku" else "Delivery reports for sent SMS and MMS messages",
                                    if (strings.isSerbian) "Potvrde o čitanju za poslate MMS poruke" else "Read receipts for sent MMS messages",
                                    if (strings.isSerbian) "Slanje potvrde pošiljaocu da je MMS pročitan" else "Send read confirmation to MMS sender when requested",
                                    if (strings.isSerbian) "Automatsko preuzimanje MMS-a (uvek, nikad ili van rominga)" else "Auto retrieve MMS (always, never or not in roaming)",
                                    if (strings.isSerbian) "Preuzimanje i slanje MMS-a i kada su mobilni podaci isključeni" else "MMS transfer even when mobile data is disabled",
                                    if (strings.isSerbian) "Režim kreiranja MMS-a: Slobodno, Upozorenje, Ograničeno" else "MMS creation modes: Free, Warning, Restricted",
                                    if (strings.isSerbian) "Izbor tona zvona za poruke sa integrisanim biračem sistema" else "System message ringtone picker integration",
                                    if (strings.isSerbian) "Prečica za bežična bezbednosna upozorenja u vanrednim situacijama" else "Emergency and safety alerts shortcut"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Korisnički interfejs i Teme" else "User Interface & Themes",
                                items = listOf(
                                    if (strings.isSerbian) "Optimizovan prikaz kategorija (Osobe, Usluge, Nepročitano, Sve) u punoj širini" else "Optimized full-width message category tabs",
                                    if (strings.isSerbian) "Ikonica za slanje u potpunosti usklađena sa izabranom temom i akcentima" else "Send icon dynamically adapted to the active theme accent",
                                    if (strings.isSerbian) "Pojednostavljeno i preglednije planiranje slanja poruka" else "Streamlined scheduled messaging selector",
                                    if (strings.isSerbian) "Poboljšan kontrast teksta i stakleni elementi preko pozadine" else "Enhanced contrast and frosted glass hierarchy"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Upravljanje porukama i Korpa" else "Message Handling & Recycle Bin",
                                items = listOf(
                                    if (strings.isSerbian) "Zasebno brisanje pojedinačnih poruka u razgovoru i slanje u korpu" else "Individual message deletion to trash from conversation thread",
                                    if (strings.isSerbian) "Automatsko zadržavanje u korpi 30 dana uz mogućnost vraćanja ili trajnog brisanja" else "Trash retention for 30 days with restore or permanent delete",
                                    if (strings.isSerbian) "Potpuno uklonjeni testni primeri iz produkcione baze" else "Removed mock sample data from production view"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Zaštita privatnosti i Ažuriranja" else "Privacy Protection & Updates",
                                items = listOf(
                                    if (strings.isSerbian) "Automatska provera novih verzija sa GitHub-a na svakih 6 sati" else "Periodic update checks via GitHub every 6 hours",
                                    if (strings.isSerbian) "Direktno preuzimanje i instalacija novih izdanja unutar aplikacije" else "In-app download and installation of app updates",
                                    if (strings.isSerbian) "Zero telemetry: Vaše poruke i kontakti nikada ne napuštaju telefon" else "Zero telemetry: messages and contacts stay local on device"
                                )
                            )
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = strings.changelogPreviousVersionsHeader,
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textSecondary,
                        fontWeight = FontWeight.Bold
                    )

                    ChangelogVersionSection(
                        versionName = "v0.9 Beta",
                        releaseDate = "05.11.2026.",
                        isCurrent = false,
                        categories = listOf(
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Pametne kategorije i Svajp radnje" else "Smart Categories & Swipe Actions",
                                items = listOf(
                                    if (strings.isSerbian) "Automatsko sortiranje poruka u Osobe, Usluge i Nepročitano" else "Automatic conversation categorization into People, Services and Unread",
                                    if (strings.isSerbian) "Prilagodljive svajp radnje (arhiviranje, brisanje, blokiranje, pročitano)" else "Customizable swipe actions (archive, delete, block, read)",
                                    if (strings.isSerbian) "Sistem za blokiranje neželjenih brojeva i spam pošiljalaca" else "Spam protection and blacklist sender manager"
                                )
                            )
                        )
                    )

                    ChangelogVersionSection(
                        versionName = "v0.8 Alpha",
                        releaseDate = "28.10.2026.",
                        isCurrent = false,
                        categories = listOf(
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Početna verzija i Arhitektura" else "Initial Release & Architecture",
                                items = listOf(
                                    if (strings.isSerbian) "Slanje i prijem SMS i MMS poruka sa podrškom za dve SIM kartice" else "Dual-SIM SMS/MMS messaging engine",
                                    if (strings.isSerbian) "Lokalni bekap i vraćanje poruka u bezbednom JSON formatu" else "Secure offline JSON backup and restore",
                                    if (strings.isSerbian) "Originalni Glassmorphic dizajn sa izborom 7 tema i prilagođenih slika" else "Original Glassmorphic UI with 7 color themes and custom wallpaper support"
                                )
                            )
                        )
                    )
                }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = strings.back,
                    color = palette.accentPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
private fun SettingsCategoryWindowCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    accentHighlight: Color?,
    pressedIconTint: Color = iconTint,
    onClick: () -> Unit
) {
    val palette = LocalGlassPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val activeIconTint = if (isPressed) pressedIconTint else iconTint
    val activeHighlight = if (isPressed && pressedIconTint != iconTint) pressedIconTint else accentHighlight

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevated = true,
        accentHighlight = activeHighlight,
        contentPadding = PaddingValues(16.dp),
        interactionSource = interactionSource,
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(activeIconTint.copy(alpha = 0.14f))
                        .border(1.dp, activeIconTint.copy(alpha = 0.45f), CircleShape)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = activeIconTint,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = TxturaIcons.ChevronRight,
                contentDescription = null,
                tint = palette.accentPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ConversationThreadCard(
    thread: ConversationThread,
    showContactPhotos: Boolean,
    listLayoutMode: MainListLayoutMode = MainListLayoutMode.DEFAULT,
    isSelected: Boolean = false,
    showPinAction: Boolean = true,
    strings: AppStrings,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onTogglePin: () -> Unit,
    onOpenContact: () -> Unit,
    onCopyOtp: (String) -> Unit
) {
    val palette = LocalGlassPalette.current
    val lastMsg = thread.lastMessage
    val risk = thread.highestRiskLevel
    val otpCode = lastMsg?.securityAnalysis?.extractedOtpCode
    val isCompact = listLayoutMode == MainListLayoutMode.COMPACT

    val borderAccent = when {
        isSelected -> palette.accentPrimary
        risk == SecurityRiskLevel.SUSPICIOUS -> palette.dangerColor
        thread.isPinned -> palette.accentPrimary
        thread.unreadCount > 0 -> palette.accentPrimary
        else -> null
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (isCompact) 14.dp else 20.dp),
        elevated = isSelected || (thread.unreadCount > 0) || thread.isPinned,
        accentHighlight = borderAccent,
        contentPadding = PaddingValues(
            horizontal = if (isCompact) 10.dp else 14.dp,
            vertical = if (isCompact) 6.dp else 11.dp
        ),
        onClick = onClick,
        onLongClick = onLongClick
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (showContactPhotos) {
                    ContactAvatarView(
                        thread = thread,
                        size = if (isCompact) 34.dp else 46.dp,
                        onClick = onOpenContact
                    )
                    Spacer(modifier = Modifier.width(if (isCompact) 8.dp else 12.dp))
                }

                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    // Top Row: Contact Name + Highlight indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = thread.contactName,
                            style = if (isCompact) {
                                MaterialTheme.typography.bodyLarge
                            } else {
                                MaterialTheme.typography.titleMedium
                            },
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (thread.messages.any { it.isHighlighted }) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = TxturaIcons.HighlightStar,
                                contentDescription = strings.highlightedMessagesTitle,
                                tint = palette.cautionColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(if (isCompact) 2.dp else 4.dp))

                    // Bottom Row: Message preview or Draft preview across available width (strictly 1 line)
                    val hasDraft = !thread.draftText.isNullOrBlank()
                    val previewText = if (hasDraft) {
                        "${strings.draftPrefix} ${thread.draftText}"
                    } else {
                        lastMsg?.body ?: ""
                    }
                    Text(
                        text = previewText,
                        style = if (isCompact) {
                            MaterialTheme.typography.labelMedium
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                        color = if (hasDraft) {
                            palette.accentPrimary
                        } else if (thread.unreadCount > 0) {
                            palette.textPrimary
                        } else {
                            palette.textSecondary
                        },
                        fontWeight = if (hasDraft || thread.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Right side: Unread count badge to the left of the Pin icon, followed by message arrival date & time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    if (thread.unreadCount > 0) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(19.dp)
                                .clip(CircleShape)
                                .background(palette.accentPrimary)
                        ) {
                            Text(
                                text = thread.unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (palette.isDark) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    if (showPinAction) {
                        var isPinnedOptimistic by remember(thread.id, thread.isPinned) { mutableStateOf(thread.isPinned) }
                        val handlePinToggle = {
                            isPinnedOptimistic = !isPinnedOptimistic
                            onTogglePin()
                        }
                        if (isPinnedOptimistic) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(palette.accentPrimary.copy(alpha = 0.22f))
                                    .border(1.dp, palette.accentPrimary.copy(alpha = 0.70f), CircleShape)
                                    .clickable(onClick = handlePinToggle)
                            ) {
                                Icon(
                                    imageVector = TxturaIcons.PinFilled,
                                    contentDescription = strings.unpinContactThread,
                                    tint = palette.accentPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = handlePinToggle)
                            ) {
                                Icon(
                                    imageVector = TxturaIcons.PinOutline,
                                    contentDescription = strings.pinContactThread,
                                    tint = palette.textSecondary.copy(alpha = 0.38f),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    lastMsg?.timestampMillis?.let { formatThreadTimestamp(it) }?.let { (primaryTime, secondaryDate) ->
                        if (secondaryDate == null) {
                            Text(
                                text = primaryTime,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (thread.unreadCount > 0) palette.accentPrimary else palette.textSecondary,
                                fontWeight = if (thread.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = primaryTime,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (thread.unreadCount > 0) palette.accentPrimary else palette.textSecondary,
                                    fontWeight = if (thread.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = secondaryDate,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = palette.textSecondary.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.Normal,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            if (otpCode != null || risk == SecurityRiskLevel.SUSPICIOUS) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (otpCode != null) {
                        GlassCard(
                            shape = RoundedCornerShape(50),
                            elevated = true,
                            accentHighlight = palette.safeColor,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            onClick = { onCopyOtp(otpCode) }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = TxturaIcons.CopyKey,
                                    contentDescription = null,
                                    tint = palette.safeColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "${strings.copyCodePrefix}$otpCode",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.safeColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = TxturaIcons.Warning,
                                contentDescription = null,
                                tint = palette.dangerColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = strings.shieldSuspiciousBlocked,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.dangerColor,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class PendingDelayedMessage(
    val id: Long,
    val body: String,
    val attachmentUri: Uri?,
    val startedAtMillis: Long,
    val durationMillis: Long = 6_000L,
)

@SuppressLint("MutableCollectionMutableState")
@Composable
fun ConversationDetailScreen(
    thread: ConversationThread,
    showContactPhotos: Boolean = true,
    recallMessagesEnabled: Boolean = true,
    bubbleZoomScale: Float = 1.0f,
    onUpdateBubbleZoomScale: (Float) -> Unit = {},
    pinchToZoomEnabled: Boolean = true,
    expressiveAnimationsEnabled: Boolean = true,
    autoPreviewsShowAll: Boolean = true,
    autoPreviewsWebLinksOnly: Boolean = false,
    autoPreviewsWifiOnly: Boolean = false,
    wallpaperConfig: WallpaperConfig = WallpaperConfig(),
    pendingAttachmentUri: Uri?,
    availableContacts: List<ContactPickerItem> = emptyList(),
    scheduledMessages: List<ScheduledSmsMessage> = emptyList(),
    onBack: () -> Unit,
    onTogglePinThread: () -> Unit,
    onTogglePinMessage: (Long) -> Unit,
    onToggleHighlightMessage: (Long) -> Unit = {},
    onDeleteMessage: (Long) -> Unit = {},
    onDeleteMessages: (List<Long>) -> Unit = { ids -> ids.forEach(onDeleteMessage) },
    onCopyMessageText: (String) -> Unit = {},
    onForwardMessage: (ChatMessage, String, String?) -> Unit = { _, _, _ -> },
    onOpenChatWallpaperPicker: () -> Unit = {},
    onCallSender: () -> Unit,
    onOpenContact: () -> Unit,
    onBlockContact: () -> Unit = {},
    onToggleSenderCategory: () -> Unit = {},
    onPickMmsPhoto: () -> Unit,
    onAttachUri: (Uri) -> Unit = {},
    onClearAttachment: () -> Unit,
    onSendMessage: (String, Uri?) -> Unit,
    onScheduleMessage: (String, Uri?, Long) -> Unit = { _, _, _ -> },
    onCancelScheduledMessage: (Long) -> Unit = {},
    inAppSoundsEnabled: Boolean = true,
    onCopyOtp: (String) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val strings = TxturaStrings.current()

    // Prevent device screen from turning off while inside conversation window
    val activity = remember(context) {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return@remember ctx
            ctx = ctx.baseContext
        }
        null
    }
    DisposableEffect(activity) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    var draftText by remember(thread.id) {
        mutableStateOf(SmsMmsRepository.getDraftForThread(context, thread.id))
    }

    LaunchedEffect(draftText, thread.id) {
        SmsMmsRepository.saveDraftForThread(context, thread.id, draftText)
    }

    // In-Chat Search state (located right above the writing box)
    var isChatSearchVisible by remember(thread.id) { mutableStateOf(false) }
    var chatSearchQuery by remember(thread.id) { mutableStateOf("") }
    var activeSearchMatchIndex by remember(thread.id) { mutableIntStateOf(0) }
    var showAttachmentActions by remember(thread.id) { mutableStateOf(false) }

    val matchingMessageIndices = remember(thread.messages, chatSearchQuery) {
        if (chatSearchQuery.isBlank()) {
            emptyList()
        } else {
            thread.messages.mapIndexedNotNull { index, msg ->
                if (msg.body.contains(chatSearchQuery, ignoreCase = true)) index else null
            }
        }
    }

    var showShareContactDialog by remember { mutableStateOf(false) }
    var showBroadcastDialog by remember { mutableStateOf(false) }
    var showCalendarPickerDialog by remember { mutableStateOf(false) }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showCalendarPickerDialog = true
        } else {
            Toast.makeText(
                context,
                if (strings.isSerbian) "Dozvola za kalendar je odbijena." else "Calendar permission denied.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showShareContactDialog = true
        } else {
            Toast.makeText(
                context,
                if (strings.isSerbian) "Dozvola za kontakte je odbijena." else "Contacts permission denied.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val handleKeyboardMedia: (Uri) -> Boolean = { mediaUri ->
        val isGif = isGifUri(context, mediaUri)
        val sizeBytes = try {
            context.contentResolver.openAssetFileDescriptor(mediaUri, "r")?.use { it.length }
                ?: context.contentResolver.openInputStream(mediaUri)?.use { it.available().toLong() }
                ?: 0L
        } catch (_: Exception) { 0L }

        val maxBytes = MmsImageCompressor.MAX_MMS_IMAGE_BYTES // 299_900L
        if (sizeBytes > maxBytes) {
            val sizeKb = (sizeBytes / 1024.0).let { "%.1f".format(it) }
            Toast.makeText(
                context,
                if (strings.isSerbian) "GIF animacija prelazi maksimalnu veličinu od 299.9 kB ($sizeKb kB). Izaberite manji GIF."
                else "GIF animation exceeds maximum allowed size of 299.9 kB ($sizeKb kB). Please choose a smaller GIF.",
                Toast.LENGTH_LONG
            ).show()
            false
        } else {
            val cacheDir = File(context.cacheDir, "mms_attachments").apply { mkdirs() }
            val ext = if (isGif) "gif" else "jpg"
            val cachedFile = File(cacheDir, "mms_keyboard_${System.currentTimeMillis()}.$ext")
            try {
                context.contentResolver.openInputStream(mediaUri)?.use { input ->
                    cachedFile.outputStream().use { output -> input.copyTo(output) }
                }
                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    cachedFile
                )
                onAttachUri(contentUri)
                val sizeKb = (cachedFile.length() / 1024.0).let { "%.1f".format(it) }
                Toast.makeText(
                    context,
                    if (strings.isSerbian) "GIF animacija dodata kao MMS prilog ($sizeKb kB)"
                    else "GIF animation added as MMS attachment ($sizeKb kB)",
                    Toast.LENGTH_SHORT
                ).show()
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    DisposableEffect(view) {
        val mimeTypes = arrayOf("image/gif", "image/*")
        ViewCompat.setOnReceiveContentListener(view, mimeTypes) { _, payload ->
            val split = payload.partition { item -> item.uri != null }
            val mediaContent = split.first
            if (mediaContent != null) {
                val clipData = mediaContent.clip
                for (i in 0 until clipData.itemCount) {
                    val u = clipData.getItemAt(i).uri
                    if (u != null) {
                        handleKeyboardMedia(u)
                    }
                }
            }
            split.second
        }
        onDispose {
            ViewCompat.setOnReceiveContentListener(view, mimeTypes, null)
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val (cachedUri, error) = DocumentAttachmentHelper.processDocumentUri(context, uri)
            if (cachedUri != null) {
                onAttachUri(cachedUri)
            } else if (error != null) {
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            }
        }
    }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            Toast.makeText(
                context,
                if (strings.isSerbian) "Dozvola za mikrofon odobrena. Zadržite mikrofon za snimanje."
                else "Microphone permission granted. Hold microphone to record.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                context,
                strings.voiceMessagePermissionRequired,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val systemShareContactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val contactUri = result.data?.data ?: return@rememberLauncherForActivityResult
        try {
            context.contentResolver.query(
                contactUri,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                ),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayName = cursor.getString(0).orEmpty().trim()
                    val rawNumber = cursor.getString(1).orEmpty().trim()
                    val formatted = if (displayName.isNotBlank()) "$displayName $rawNumber" else rawNumber
                    if (formatted.isNotBlank()) {
                        draftText = if (draftText.isBlank()) formatted else "$draftText\n$formatted"
                        showShareContactDialog = false
                        Toast.makeText(
                            context,
                            strings.shareContactAddedToast(displayName.ifBlank { rawNumber }),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore malformed contact URI
        }
    }

    val inputFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val initialMessageScrollIndex = remember(thread.id) {
        (thread.messages.size - 1).coerceAtLeast(0)
    }
    val listState = key(thread.id) {
        rememberLazyListState(initialFirstVisibleItemIndex = initialMessageScrollIndex)
    }
    val coroutineScope = rememberCoroutineScope()
    val pinnedMessages = thread.pinnedMessages
    var activePinnedIndex by remember(thread.id) { mutableIntStateOf(0) }
    var prevPinnedCount by remember(thread.id) { mutableIntStateOf(pinnedMessages.size) }
    var previousPinnedMessageIds by remember(thread.id) {
        mutableStateOf(pinnedMessages.mapTo(mutableSetOf()) { it.id })
    }
    var jumpedToMessageId by remember(thread.id) { mutableStateOf<Long?>(null) }
    var showPinnedMessagesManageDialog by remember(thread.id) { mutableStateOf(value = false) }
    var selectedPinnedIdsForUnpin by remember(thread.id) { mutableStateOf<Set<Long>>(emptySet()) }
    val currentZoom by rememberUpdatedState(bubbleZoomScale)

    LaunchedEffect(pinnedMessages.map { it.id }) {
        val currentPinnedIds = pinnedMessages.mapTo(mutableSetOf()) { it.id }
        val unpinnedMessageStillInThread = previousPinnedMessageIds.any { messageId ->
            messageId !in currentPinnedIds && thread.messages.any { it.id == messageId }
        }
        previousPinnedMessageIds = currentPinnedIds

        if (unpinnedMessageStillInThread && thread.messages.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(pinnedMessages.size) {
        activePinnedIndex = if (pinnedMessages.isNotEmpty()) {
            if (pinnedMessages.size > prevPinnedCount) {
                pinnedMessages.lastIndex
            } else {
                activePinnedIndex.coerceIn(0, pinnedMessages.lastIndex)
            }
        } else {
            0
        }
        prevPinnedCount = pinnedMessages.size
    }

    LaunchedEffect(jumpedToMessageId) {
        if (jumpedToMessageId != null) {
            delay(1600.milliseconds)
            jumpedToMessageId = null
        }
    }

    // 6-second delayed send countdown state (triggered on Send button tap)
    var pendingDelayedSend by remember { mutableStateOf<PendingDelayedMessage?>(null) }
    var delayedRemainingMillis by remember { mutableLongStateOf(0L) }
    val currentOnSendMessage by rememberUpdatedState(onSendMessage)
    val currentPendingDelayed by rememberUpdatedState(pendingDelayedSend)

    // Schedule message dialog state (triggered on Send button long-press)
    var showScheduleMessageDialog by remember { mutableStateOf(value = false) }

    // Tick the 6-second countdown timer and automatically send when it reaches 0
    LaunchedEffect(pendingDelayedSend?.id) {
        val activeDelayed = pendingDelayedSend ?: return@LaunchedEffect
        while (true) {
            val elapsed = System.currentTimeMillis() - activeDelayed.startedAtMillis
            val remaining = (activeDelayed.durationMillis - elapsed).coerceAtLeast(0L)
            delayedRemainingMillis = remaining
            if (remaining <= 0L) {
                break
            }
            delay(50.milliseconds)
        }
        if (pendingDelayedSend?.id == activeDelayed.id) {
            pendingDelayedSend = null
            delayedRemainingMillis = 0L
            currentOnSendMessage(activeDelayed.body, activeDelayed.attachmentUri)
        }
    }

    // If user leaves the conversation while a 6s countdown was NOT canceled, dispatch the message so it isn't lost
    DisposableEffect(Unit) {
        onDispose {
            VoiceMessageManager.stopPlayback()
            VoiceMessageManager.cancelRecording()
            currentPendingDelayed?.let { uncancelled ->
                currentOnSendMessage(uncancelled.body, uncancelled.attachmentUri)
            }
        }
    }

    // Message selection state (triggered on message long-press, action bar appears above search bar)
    var selectedMessageIds by remember(thread.id) { mutableStateOf<Set<Long>>(emptySet()) }
    val isSelectionMode = selectedMessageIds.isNotEmpty()

    BackHandler(enabled = isSelectionMode) {
        selectedMessageIds = emptySet()
    }

    var messagePendingDelete by remember { mutableStateOf<ChatMessage?>(null) }
    var messagePendingForward by remember { mutableStateOf<ChatMessage?>(null) }

    // Preview outgoing message bubble displayed during the 6-second delayed send countdown
    val pendingOutgoingPreviewMessage = remember(pendingDelayedSend, thread.id, thread.address) {
        pendingDelayedSend?.let { pending ->
            ChatMessage(
                id = -abs(pending.id).coerceAtLeast(1L),
                threadId = thread.id,
                address = thread.address,
                body = pending.body,
                timestampMillis = pending.startedAtMillis,
                isIncoming = false,
                type = if (pending.attachmentUri != null) MessageType.MMS else MessageType.SMS,
                status = DeliveryStatus.SENT,
                securityAnalysis = MessageSecurityAnalyzer.analyzeMessage(pending.body),
                attachment = pending.attachmentUri?.let { uri ->
                    val isAudio = uri.path?.endsWith(".amr", ignoreCase = true) == true ||
                        uri.toString().contains("voice_") ||
                        pending.body == strings.voiceMessageTitle
                    MmsAttachment(
                        uri = uri,
                        mimeType = if (isAudio) "audio/amr" else "image/jpeg",
                        caption = pending.body
                    )
                }
            )
        }
    }

    // Top-right 3-dots overflow dropdown menu & block contact confirmation dialog state
    var showMoreMenu by remember { mutableStateOf(false) }
    var showConfirmBlockContactDialog by remember { mutableStateOf(false) }

    var isInitialScrollDone by remember(thread.id) { mutableStateOf(false) }

    LaunchedEffect(thread.id, thread.messages.size, pendingOutgoingPreviewMessage?.id) {
        val totalItems = thread.messages.size + (if (pendingOutgoingPreviewMessage != null) 1 else 0)
        if (totalItems > 0) {
            val target = totalItems - 1
            if (!isInitialScrollDone) {
                isInitialScrollDone = true
                listState.scrollToItem(target)
            } else {
                listState.animateScrollToItem(target)
            }
        }
    }

    val isMmsMode = pendingAttachmentUri != null
    val hasNonAsciiOrEmoji = draftText.any { it.code > 127 }
    val charsPerSegment = if (hasNonAsciiOrEmoji) 70 else 160
    val smsSegments = if (draftText.isEmpty()) 1 else ((draftText.length - 1) / charsPerSegment) + 1

    GlassBackground(wallpaperConfig = wallpaperConfig) {
        val palette = LocalGlassPalette.current
        val metrics = LocalAdaptiveMetrics.current

        // Confirmation dialog before blocking contact from the 3-dots menu
        if (showConfirmBlockContactDialog) {
            GlassAlertDialog(
                onDismissRequest = { showConfirmBlockContactDialog = false },
                accentBorderColor = palette.dangerColor,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = TxturaIcons.Block,
                            contentDescription = null,
                            tint = palette.dangerColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.confirmBlockContactTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.textPrimary
                        )
                    }
                },
                text = {
                    Text(
                        text = strings.confirmBlockContactMessage(thread.contactName, thread.address),
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textPrimary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showConfirmBlockContactDialog = false
                            onBlockContact()
                        }
                    ) {
                        Text(
                            text = strings.confirm,
                            color = palette.dangerColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmBlockContactDialog = false }) {
                        Text(strings.cancel, color = palette.textSecondary)
                    }
                }
            )
        }

        // Confirm single message deletion
        val msgToDelete = messagePendingDelete
        if (msgToDelete != null) {
            GlassAlertDialog(
                onDismissRequest = { messagePendingDelete = null },
                accentBorderColor = palette.dangerColor,
                title = {
                    Text(strings.confirmDeleteSingleMessageTitle, color = palette.textPrimary)
                },
                text = {
                    Text(strings.confirmDeleteSingleMessageBody, color = palette.textPrimary)
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            messagePendingDelete = null
                            onDeleteMessage(msgToDelete.id)
                        }
                    ) {
                        Text(strings.deleteMessageOption, color = palette.dangerColor, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { messagePendingDelete = null }) {
                        Text(strings.cancel, color = palette.textSecondary)
                    }
                }
            )
        }

        // Forward message recipient picker
        val msgToForward = messagePendingForward
        if (msgToForward != null) {
            NewMessageDialog(
                title = strings.forwardMessageDialogTitle,
                availableContacts = availableContacts,
                onPickContact = {},
                onDismiss = { messagePendingForward = null },
                onSelectRecipient = { recipientAddr, recipientName ->
                    messagePendingForward = null
                    onForwardMessage(msgToForward, recipientAddr, recipientName)
                }
            )
        }

        // Mini-window for selecting which pinned messages to unpin (or Unpin All) when holding touch on multiple pinned messages
        if (showPinnedMessagesManageDialog && pinnedMessages.isNotEmpty()) {
            GlassAlertDialog(
                onDismissRequest = {
                    showPinnedMessagesManageDialog = false
                    selectedPinnedIdsForUnpin = emptySet()
                },
                accentBorderColor = palette.accentPrimary,
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = TxturaIcons.Pin,
                                contentDescription = null,
                                tint = palette.accentPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${strings.pinnedMessagesTitle} (${pinnedMessages.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = strings.managePinnedMessagesSub,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        pinnedMessages.forEach { pinnedMsg ->
                            val isSelectedForUnpin = pinnedMsg.id in selectedPinnedIdsForUnpin
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                elevated = isSelectedForUnpin,
                                accentHighlight = if (isSelectedForUnpin) palette.accentPrimary else null,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                onClick = {
                                    selectedPinnedIdsForUnpin = if (isSelectedForUnpin) {
                                        selectedPinnedIdsForUnpin - pinnedMsg.id
                                    } else {
                                        selectedPinnedIdsForUnpin + pinnedMsg.id
                                    }
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelectedForUnpin) {
                                                    palette.accentPrimary
                                                } else {
                                                    palette.glassSurfaceElevated
                                                }
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelectedForUnpin) {
                                                    palette.accentPrimary
                                                } else {
                                                    palette.textSecondary.copy(alpha = 0.5f)
                                                },
                                                shape = CircleShape
                                            )
                                    ) {
                                        if (isSelectedForUnpin) {
                                            Icon(
                                                imageVector = TxturaIcons.Check,
                                                contentDescription = null,
                                                tint = if (palette.isDark) Color.Black else Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Text(
                                        text = pinnedMsg.body.ifBlank { pinnedMsg.attachment?.caption ?: "MMS" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.textPrimary,
                                        fontWeight = if (isSelectedForUnpin) FontWeight.SemiBold else FontWeight.Normal,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Icon(
                                        imageVector = TxturaIcons.Close,
                                        contentDescription = strings.unpinMessage,
                                        tint = palette.textSecondary,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable {
                                                selectedPinnedIdsForUnpin = selectedPinnedIdsForUnpin - pinnedMsg.id
                                                val remainingCount = pinnedMessages.size - 1
                                                onTogglePinMessage(pinnedMsg.id)
                                                if (remainingCount <= 0) {
                                                    showPinnedMessagesManageDialog = false
                                                }
                                            }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                val allIds = pinnedMessages.map { it.id }
                                showPinnedMessagesManageDialog = false
                                selectedPinnedIdsForUnpin = emptySet()
                                allIds.forEach { msgId -> onTogglePinMessage(msgId) }
                            }
                        ) {
                            Text(
                                text = strings.unpinAllMessages,
                                color = palette.dangerColor,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (selectedPinnedIdsForUnpin.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    val idsToUnpin = selectedPinnedIdsForUnpin.toList()
                                    showPinnedMessagesManageDialog = false
                                    selectedPinnedIdsForUnpin = emptySet()
                                    idsToUnpin.forEach { msgId ->
                                        if (pinnedMessages.any { it.id == msgId }) {
                                            onTogglePinMessage(msgId)
                                        }
                                    }
                                }
                            ) {
                                Text(
                                    text = strings.unpinSelectedMessages(selectedPinnedIdsForUnpin.size),
                                    color = palette.accentPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showPinnedMessagesManageDialog = false
                            selectedPinnedIdsForUnpin = emptySet()
                        }
                    ) {
                        Text(
                            text = strings.cancel,
                            color = palette.textSecondary
                        )
                    }
                }
            )
        }

        // Schedule Message Dialog (opened when holding/long-pressing the Send button)
        if (showScheduleMessageDialog) {
            ScheduleMessageDialog(
                initialMessageText = draftText,
                hasAttachment = pendingAttachmentUri != null,
                strings = strings,
                onDismiss = { showScheduleMessageDialog = false },
                onConfirmSchedule = { messageBody, scheduledAtMillis ->
                    showScheduleMessageDialog = false
                    onScheduleMessage(messageBody, pendingAttachmentUri, scheduledAtMillis)
                    draftText = ""
                    SmsMmsRepository.clearDraftForThread(context, thread.id)
                    onClearAttachment()
                    val formattedTime = SimpleDateFormat("dd.MM. HH:mm", Locale.getDefault())
                        .format(Date(scheduledAtMillis))
                    Toast.makeText(
                        context,
                        strings.messageScheduledToast(formattedTime),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }

        if (showShareContactDialog) {
            ShareContactDialog(
                availableContacts = availableContacts,
                onPickFromSystemContacts = {
                    try {
                        val pickIntent = Intent(
                            Intent.ACTION_PICK,
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                        )
                        systemShareContactPickerLauncher.launch(pickIntent)
                    } catch (_: Exception) {
                        Toast.makeText(context, strings.contactsErrorToast, Toast.LENGTH_SHORT).show()
                    }
                },
                onSelectContact = { contact ->
                    val contactPayload = "${contact.name} ${contact.address}".trim()
                    draftText = if (draftText.isBlank()) contactPayload else "$draftText\n$contactPayload"
                    showShareContactDialog = false
                    Toast.makeText(
                        context,
                        strings.shareContactAddedToast(contact.name),
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onDismiss = { showShareContactDialog = false }
            )
        }

        if (showCalendarPickerDialog) {
            CalendarEventPickerDialog(
                onSelectEventUri = { calUri ->
                    onAttachUri(calUri)
                    showCalendarPickerDialog = false
                },
                onDismiss = { showCalendarPickerDialog = false }
            )
        }

        if (showBroadcastDialog) {
            MultiRecipientBroadcastDialog(
                initialMessageBody = draftText,
                availableContacts = availableContacts,
                onDismiss = { showBroadcastDialog = false },
                onSendBroadcast = { recipients, messageBody ->
                    recipients.forEach { rec ->
                        SmsMmsRepository.sendMessage(
                            context = context,
                            recipientAddress = rec.address,
                            rawBody = messageBody
                        )
                    }
                    val locStrings = TxturaStrings.forLocale()
                    Toast.makeText(
                        context,
                        locStrings.broadcastMessagesSentToast(recipients.size),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Sticky Glass Top Bar
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                elevated = true,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GlassIconButton(
                        icon = TxturaIcons.ArrowBack,
                        contentDescription = strings.back,
                        onClick = onBack,
                        size = 38.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (showContactPhotos) {
                        ContactAvatarView(
                            thread = thread,
                            size = 36.dp,
                            onClick = onOpenContact
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onOpenContact)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OverflowTooltipText(
                                text = thread.contactName,
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary,
                                maxLines = 1
                            )
                            if (thread.isPinned) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = TxturaIcons.Pin,
                                    contentDescription = null,
                                    tint = palette.accentPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = TxturaIcons.ShieldCheck,
                                contentDescription = null,
                                tint = palette.safeColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            OverflowTooltipText(
                                text = "${thread.address} • ${strings.protectedChannelSuffix}",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Top-right corner: ONLY Call button and 3-dots menu button next to it
                    GlassIconButton(
                        icon = TxturaIcons.PhoneCall,
                        contentDescription = strings.callSender,
                        tint = palette.safeColor,
                        accentHighlight = palette.safeColor,
                        onClick = onCallSender,
                        size = 36.dp
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Box {
                        GlassIconButton(
                            icon = TxturaIcons.MoreVert,
                            contentDescription = strings.moreOptionsTooltip,
                            tint = palette.accentPrimary,
                            onClick = { showMoreMenu = true },
                            size = 36.dp
                        )

                        val menuShape = RoundedCornerShape(20.dp)
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            modifier = Modifier.widthIn(min = 250.dp, max = 340.dp),
                            shape = menuShape,
                            containerColor = palette.popupContainerColor,
                            tonalElevation = 0.dp,
                            shadowElevation = 8.dp,
                            border = BorderStroke(
                                width = 1.dp,
                                brush = Brush.linearGradient(listOf(palette.glassBorderTop, palette.glassBorderBottom))
                            )
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = strings.chatWallpaperTooltip,
                                        color = palette.textPrimary
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                                leadingIcon = {
                                    Icon(
                                        imageVector = TxturaIcons.WallpaperLandscape,
                                        contentDescription = null,
                                        tint = palette.accentPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    TxturaHaptics.click(view, context)
                                    showMoreMenu = false
                                    onOpenChatWallpaperPicker()
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (thread.isPinned) strings.unpinContactThread else strings.pinContactThread,
                                        color = palette.textPrimary
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                                leadingIcon = {
                                    Icon(
                                        imageVector = TxturaIcons.Pin,
                                        contentDescription = null,
                                        tint = if (thread.isPinned) palette.accentPrimary else palette.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    TxturaHaptics.click(view, context)
                                    showMoreMenu = false
                                    onTogglePinThread()
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = strings.toggleSenderCategoryOption,
                                            color = palette.textPrimary
                                        )
                                        Text(
                                            text = if (thread.isService) strings.currentCategoryServiceSub else strings.currentCategoryPeopleSub,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.textSecondary
                                        )
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (thread.isService) TxturaIcons.ContactPerson else TxturaIcons.BusinessBuilding,
                                        contentDescription = null,
                                        tint = palette.accentPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    TxturaHaptics.click(view, context)
                                    showMoreMenu = false
                                    onToggleSenderCategory()
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = strings.viewContactDetails,
                                        color = palette.textPrimary
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                                leadingIcon = {
                                    Icon(
                                        imageVector = TxturaIcons.ContactPerson,
                                        contentDescription = null,
                                        tint = palette.accentPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    TxturaHaptics.click(view, context)
                                    showMoreMenu = false
                                    onOpenContact()
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = strings.blockContactOption,
                                        color = palette.dangerColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                                leadingIcon = {
                                    Icon(
                                        imageVector = TxturaIcons.Block,
                                        contentDescription = null,
                                        tint = palette.dangerColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    TxturaHaptics.click(view, context)
                                    showMoreMenu = false
                                    showConfirmBlockContactDialog = true
                                }
                            )
                        }
                    }
                }
            }

            // Pinned Messages Banner (WhatsApp/Telegram style with vertical segment bars, our Pin icon & theme, swipe/tap listing, and scroll-to-message)
            if (pinnedMessages.isNotEmpty()) {
                val safePinnedIndex = activePinnedIndex.coerceIn(0, pinnedMessages.lastIndex)
                val currentPinnedMsg = pinnedMessages[safePinnedIndex]
                var dragAccumY by remember { mutableFloatStateOf(0f) }
                var dragAccumX by remember { mutableFloatStateOf(0f) }

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .pointerInput(pinnedMessages.size, safePinnedIndex) {
                            if (pinnedMessages.size > 1) {
                                detectVerticalDragGestures(
                                    onDragStart = {
                                        dragAccumY = 0f
                                        TxturaHaptics.tick(view, context)
                                    },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        dragAccumY += dragAmount
                                        if (dragAccumY <= -24f) {
                                            activePinnedIndex = (safePinnedIndex + 1) % pinnedMessages.size
                                            dragAccumY = 0f
                                            TxturaHaptics.tick(view, context)
                                        } else if (dragAccumY >= 24f) {
                                            activePinnedIndex = (safePinnedIndex - 1 + pinnedMessages.size) % pinnedMessages.size
                                            dragAccumY = 0f
                                            TxturaHaptics.tick(view, context)
                                        }
                                    }
                                )
                            }
                        }
                        .pointerInput(pinnedMessages.size, safePinnedIndex) {
                            if (pinnedMessages.size > 1) {
                                detectHorizontalDragGestures(
                                    onDragStart = {
                                        dragAccumX = 0f
                                        TxturaHaptics.tick(view, context)
                                    },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        dragAccumX += dragAmount
                                        if (dragAccumX <= -32f) {
                                            activePinnedIndex = (safePinnedIndex + 1) % pinnedMessages.size
                                            dragAccumX = 0f
                                            TxturaHaptics.tick(view, context)
                                        } else if (dragAccumX >= 32f) {
                                            activePinnedIndex = (safePinnedIndex - 1 + pinnedMessages.size) % pinnedMessages.size
                                            dragAccumX = 0f
                                            TxturaHaptics.tick(view, context)
                                        }
                                    }
                                )
                            }
                        },
                    shape = RoundedCornerShape(16.dp),
                    elevated = true,
                    accentHighlight = palette.accentPrimary,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                    onClick = {
                        val targetIndexToJump = if (pinnedMessages.size > 1 && jumpedToMessageId == currentPinnedMsg.id) {
                            val nextIdx = (safePinnedIndex + 1) % pinnedMessages.size
                            activePinnedIndex = nextIdx
                            nextIdx
                        } else {
                            safePinnedIndex
                        }
                        val targetMsg = pinnedMessages[targetIndexToJump]
                        val msgPos = thread.messages.indexOfFirst { it.id == targetMsg.id }
                        if (msgPos >= 0) {
                            jumpedToMessageId = targetMsg.id
                            coroutineScope.launch {
                                listState.animateScrollToItem(
                                    index = msgPos,
                                    scrollOffset = -120
                                )
                            }
                        }
                    },
                    onLongClick = if (pinnedMessages.size > 1) {
                        {
                            selectedPinnedIdsForUnpin = emptySet()
                            showPinnedMessagesManageDialog = true
                        }
                    } else {
                        null
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Left vertical segmented indicator bar
                        Column(
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier
                                .height(30.dp)
                                .width(3.5.dp)
                        ) {
                            pinnedMessages.forEachIndexed { idx, _ ->
                                val isCurrentSegment = idx == safePinnedIndex
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .clip(CircleShape)
                                        .background(
                                            if (isCurrentSegment) {
                                                palette.accentPrimary
                                            } else {
                                                palette.textSecondary.copy(alpha = 0.35f)
                                            }
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Rounded square badge with Txtura Pin icon
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(palette.glassSurfaceElevated)
                                .border(
                                    width = 1.dp,
                                    color = palette.glassBorderTop.copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(9.dp)
                                )
                                .combinedClickable(
                                    onClick = {
                                        if (pinnedMessages.size > 1) {
                                            val nextIdx = (safePinnedIndex + 1) % pinnedMessages.size
                                            activePinnedIndex = nextIdx
                                            val nextMsg = pinnedMessages[nextIdx]
                                            val msgPos = thread.messages.indexOfFirst { it.id == nextMsg.id }
                                            if (msgPos >= 0) {
                                                jumpedToMessageId = nextMsg.id
                                                coroutineScope.launch {
                                                    listState.animateScrollToItem(
                                                        index = msgPos,
                                                        scrollOffset = -120
                                                    )
                                                }
                                            }
                                        } else {
                                            val msgPos = thread.messages.indexOfFirst { it.id == currentPinnedMsg.id }
                                            if (msgPos >= 0) {
                                                jumpedToMessageId = currentPinnedMsg.id
                                                coroutineScope.launch {
                                                    listState.animateScrollToItem(
                                                        index = msgPos,
                                                        scrollOffset = -120
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    onLongClick = if (pinnedMessages.size > 1) {
                                        {
                                            selectedPinnedIdsForUnpin = emptySet()
                                            showPinnedMessagesManageDialog = true
                                        }
                                    } else {
                                        null
                                    }
                                )
                        ) {
                            Icon(
                                imageVector = TxturaIcons.Pin,
                                contentDescription = strings.pinnedMessagesTitle,
                                tint = palette.accentPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Active pinned message text
                        AnimatedContent(
                            targetState = currentPinnedMsg,
                            transitionSpec = {
                                (fadeIn() + expandVertically(expandFrom = Alignment.Bottom))
                                    .togetherWith(fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top))
                            },
                            label = "pinnedBannerMessageText",
                            modifier = Modifier.weight(1f)
                        ) { msgItem ->
                            Text(
                                text = msgItem.body.ifBlank { msgItem.attachment?.caption ?: "MMS" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (pinnedMessages.size > 1) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(palette.glassSurfaceElevated)
                                    .border(
                                        width = 1.dp,
                                        color = palette.accentPrimary.copy(alpha = 0.40f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            val nextIdx = (safePinnedIndex + 1) % pinnedMessages.size
                                            activePinnedIndex = nextIdx
                                            val nextMsg = pinnedMessages[nextIdx]
                                            val msgPos = thread.messages.indexOfFirst { it.id == nextMsg.id }
                                            if (msgPos >= 0) {
                                                jumpedToMessageId = nextMsg.id
                                                coroutineScope.launch {
                                                    listState.animateScrollToItem(
                                                        index = msgPos,
                                                        scrollOffset = -120
                                                    )
                                                }
                                            }
                                        },
                                        onLongClick = {
                                            selectedPinnedIdsForUnpin = emptySet()
                                            showPinnedMessagesManageDialog = true
                                        }
                                    )
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "${safePinnedIndex + 1}/${pinnedMessages.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = palette.accentPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Icon(
                            imageVector = TxturaIcons.Close,
                            contentDescription = strings.unpinMessage,
                            tint = palette.textSecondary,
                            modifier = Modifier
                                .size(15.dp)
                                .clickable { onTogglePinMessage(currentPinnedMsg.id) }
                        )
                    }
                }
            }

            // Scheduled Messages Banner
            if (scheduledMessages.isNotEmpty()) {
                val scheduleFormat = remember { SimpleDateFormat("dd.MM. HH:mm", Locale.getDefault()) }
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(18.dp),
                    accentHighlight = palette.accentSecondary,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = TxturaIcons.ClockTimer,
                                contentDescription = null,
                                tint = palette.accentSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${strings.scheduledMessagesBannerTitle} (${scheduledMessages.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.accentSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        scheduledMessages.take(3).forEach { scheduledItem ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OverflowTooltipText(
                                    text = "${scheduleFormat.format(Date(scheduledItem.scheduledAtMillis))} • ${scheduledItem.body.ifBlank { "MMS" }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textPrimary,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = TxturaIcons.Close,
                                    contentDescription = strings.cancelScheduledMessageTooltip,
                                    tint = palette.textSecondary,
                                    modifier = Modifier
                                        .size(15.dp)
                                        .clickable {
                                            if (inAppSoundsEnabled) {
                                                SmsMmsRepository.playInAppMessageCancelledSound(context)
                                            }
                                            onCancelScheduledMessage(scheduledItem.id)
                                            Toast.makeText(
                                                context,
                                                strings.messageSendCancelledToast,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                )
                            }
                        }
                    }
                }
            }

            val displayMessages = remember(thread.messages) {
                deduplicateMessageList(thread.messages)
            }

            // Messages Thread with two-finger Pinch-to-Zoom and Long-Press Context Menu on each bubble
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy((7f * bubbleZoomScale.coerceIn(0.75f, 1.65f)).dp, Alignment.Bottom),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(pinchToZoomEnabled) {
                        if (!pinchToZoomEnabled) return@pointerInput
                        awaitEachGesture {
                            var localScale = currentZoom
                            do {
                                val event = awaitPointerEvent()
                                val pressedCount = event.changes.count { it.pressed }
                                if (pressedCount >= 2) {
                                    val zoomChange = event.calculateZoom()
                                    if (zoomChange != 1f && zoomChange > 0f) {
                                        localScale = (localScale * zoomChange).coerceIn(0.75f, 1.65f)
                                        onUpdateBubbleZoomScale(localScale)
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
            ) {
                itemsIndexed(
                    items = displayMessages,
                    key = { _, msg -> msg.id },
                    contentType = { _, msg -> if (msg.attachment != null) "mms_bubble" else "sms_bubble" }
                ) { index, msg ->
                    val showDateHeader = index == 0 || !isSameDay(displayMessages[index - 1].timestampMillis, msg.timestampMillis)
                    if (showDateHeader) {
                        ChatDateHeaderPill(
                            timestampMillis = msg.timestampMillis,
                            strings = strings,
                            zoomScale = bubbleZoomScale
                        )
                    }

                    val isMsgSelected = selectedMessageIds.contains(msg.id)
                    MessageBubbleItem(
                        message = msg,
                        zoomScale = bubbleZoomScale,
                        expressiveAnimationsEnabled = expressiveAnimationsEnabled,
                        autoPreviewsShowAll = autoPreviewsShowAll,
                        autoPreviewsWebLinksOnly = autoPreviewsWebLinksOnly,
                        autoPreviewsWifiOnly = autoPreviewsWifiOnly,
                        strings = strings,
                        isSelected = isMsgSelected,
                        isSelectionMode = isSelectionMode,
                        isJumpHighlighted = jumpedToMessageId == msg.id,
                        onClickMessage = {
                            if (isSelectionMode) {
                                selectedMessageIds = if (msg.id in selectedMessageIds) {
                                    selectedMessageIds - msg.id
                                } else {
                                    selectedMessageIds + msg.id
                                }
                            }
                        },
                        onLongPressMessage = {
                            selectedMessageIds = if (msg.id in selectedMessageIds) {
                                selectedMessageIds - msg.id
                            } else {
                                selectedMessageIds + msg.id
                            }
                        },
                        onCopyOtp = onCopyOtp
                    )
                }

                // Show pending delayed message in an outgoing sender bubble during the 6s countdown
                if (pendingOutgoingPreviewMessage != null) {
                    item(
                        key = "pending_delayed_preview_${pendingOutgoingPreviewMessage.id}",
                        contentType = "pending_sms_bubble"
                    ) {
                        MessageBubbleItem(
                            message = pendingOutgoingPreviewMessage,
                            zoomScale = bubbleZoomScale,
                            expressiveAnimationsEnabled = expressiveAnimationsEnabled,
                            autoPreviewsShowAll = autoPreviewsShowAll,
                            autoPreviewsWebLinksOnly = autoPreviewsWebLinksOnly,
                            autoPreviewsWifiOnly = autoPreviewsWifiOnly,
                            strings = strings,
                            isSelected = false,
                            isSelectionMode = false,
                            onClickMessage = {},
                            onLongPressMessage = {},
                            onCopyOtp = onCopyOtp
                        )
                    }
                }
            }

            // Multi-Message Selection Action Bar located above the in-chat search bar
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val selectedMessages = remember(thread.messages, selectedMessageIds) {
                    thread.messages.filter { it.id in selectedMessageIds }
                }
                val allSelectedPinned = selectedMessages.isNotEmpty() && selectedMessages.all { it.isPinned }
                val allSelectedHighlighted = selectedMessages.isNotEmpty() && selectedMessages.all { it.isHighlighted }
                val selCount = selectedMessageIds.size

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(22.dp),
                    elevated = true,
                    accentHighlight = palette.accentPrimary,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 1. Cancel / Deselect with counter
                        GlassCard(
                            shape = RoundedCornerShape(50),
                            elevated = true,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            onClick = { selectedMessageIds = emptySet() }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = TxturaIcons.Close,
                                    contentDescription = strings.cancel,
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = selCount.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.accentPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 2. Delete (Obriši)
                        GlassTooltipIconButton(
                            icon = TxturaIcons.Delete,
                            tooltipText = strings.deleteMessageOption,
                            tint = palette.dangerColor,
                            pressedTint = palette.dangerColor,
                            size = 38.dp,
                            onClick = {
                                TxturaHaptics.reject(view, context)
                                val idsToDelete = selectedMessageIds.toList()
                                selectedMessageIds = emptySet()
                                onDeleteMessages(idsToDelete)
                            }
                        )

                        // 3. Pin / Unpin (Prikači / Otkači)
                        GlassTooltipIconButton(
                            icon = if (allSelectedPinned) TxturaIcons.PinFilled else TxturaIcons.PinOutline,
                            tooltipText = if (allSelectedPinned) strings.unpinMessage else strings.pinMessage,
                            tint = if (allSelectedPinned) palette.accentPrimary else palette.textPrimary,
                            pressedTint = palette.accentPrimary,
                            size = 38.dp,
                            onClick = {
                                val ids = selectedMessageIds.toList()
                                selectedMessageIds = emptySet()
                                ids.forEach { id -> onTogglePinMessage(id) }
                                val toastMsg = if (allSelectedPinned) strings.messagesUnpinnedToast(ids.size) else strings.messagesPinnedToast(ids.size)
                                Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                            }
                        )

                        // 4. Highlight / Unhighlight (Hajlajtuj / Ukloni hajlajt)
                        GlassTooltipIconButton(
                            icon = TxturaIcons.HighlightStar,
                            tooltipText = if (allSelectedHighlighted) strings.unhighlightMessageOption else strings.highlightMessageOption,
                            tint = if (allSelectedHighlighted) palette.cautionColor else palette.textPrimary,
                            pressedTint = palette.cautionColor,
                            size = 38.dp,
                            onClick = {
                                val ids = selectedMessageIds.toList()
                                selectedMessageIds = emptySet()
                                ids.forEach { id -> onToggleHighlightMessage(id) }
                                val toastMsg = if (allSelectedHighlighted) strings.messagesUnhighlightedToast(ids.size) else strings.messagesHighlightedToast(ids.size)
                                Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                            }
                        )

                        // 5. Forward (Prosledi)
                        GlassTooltipIconButton(
                            icon = TxturaIcons.Forward,
                            tooltipText = strings.forwardMessageOption,
                            tint = palette.accentPrimary,
                            pressedTint = palette.accentPrimary,
                            size = 38.dp,
                            onClick = {
                                if (selectedMessages.isNotEmpty()) {
                                    val combinedBody = selectedMessages.joinToString("\n\n") { it.body }
                                    val firstMsg = selectedMessages.first()
                                    messagePendingForward = firstMsg.copy(
                                        body = combinedBody,
                                        attachment = selectedMessages.firstNotNullOfOrNull { it.attachment }
                                    )
                                    selectedMessageIds = emptySet()
                                }
                            }
                        )

                        // 6. Copy (Kopiraj)
                        GlassTooltipIconButton(
                            icon = TxturaIcons.CopyKey,
                            tooltipText = strings.copyMessageOption,
                            tint = palette.safeColor,
                            pressedTint = palette.safeColor,
                            size = 38.dp,
                            onClick = {
                                TxturaHaptics.confirm(view, context)
                                val combinedBody = selectedMessages.joinToString("\n\n") { it.body }
                                onCopyMessageText(combinedBody)
                                selectedMessageIds = emptySet()
                                Toast.makeText(context, strings.messageCopiedToast, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            // In-Chat Search Bar located right above the writing box (toggleable via the lupa icon)
            AnimatedVisibility(
                visible = isChatSearchVisible,
                enter = TxturaMotion.verticalExpandEnter(isExpressive = expressiveAnimationsEnabled),
                exit = TxturaMotion.verticalCollapseExit(isExpressive = expressiveAnimationsEnabled)
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevated = true,
                    accentHighlight = palette.accentPrimary,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = TxturaIcons.Search,
                            contentDescription = null,
                            tint = palette.accentPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (chatSearchQuery.isEmpty()) {
                                Text(
                                    text = strings.searchInChatPlaceholder,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary
                                )
                            }
                            BasicTextField(
                                value = chatSearchQuery,
                                onValueChange = { query ->
                                    chatSearchQuery = query
                                    activeSearchMatchIndex = 0
                                    if (query.isNotBlank()) {
                                        val firstMatch = thread.messages.indexOfFirst { msg ->
                                            msg.body.contains(query, ignoreCase = true)
                                        }
                                        if (firstMatch >= 0) {
                                            coroutineScope.launch {
                                                listState.animateScrollToItem(firstMatch)
                                                jumpedToMessageId = thread.messages[firstMatch].id
                                            }
                                        }
                                    }
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = palette.textPrimary),
                                cursorBrush = SolidColor(palette.accentPrimary),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (chatSearchQuery.isNotEmpty()) {
                            val matchCount = matchingMessageIndices.size
                            Text(
                                text = if (matchCount > 0) {
                                    strings.searchMatchesCounter(activeSearchMatchIndex + 1, matchCount)
                                } else {
                                    strings.searchNoMatches
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (matchCount > 0) palette.accentPrimary else palette.dangerColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            if (matchCount > 1) {
                                Icon(
                                    imageVector = TxturaIcons.ArrowUp,
                                    contentDescription = strings.searchPrevTooltip,
                                    tint = palette.accentPrimary,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            val prevIdx = if (activeSearchMatchIndex > 0) {
                                                activeSearchMatchIndex - 1
                                            } else {
                                                matchingMessageIndices.lastIndex
                                            }
                                            activeSearchMatchIndex = prevIdx
                                            val listIdx = matchingMessageIndices[prevIdx]
                                            coroutineScope.launch {
                                                listState.animateScrollToItem(listIdx)
                                                jumpedToMessageId = thread.messages[listIdx].id
                                            }
                                        }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = TxturaIcons.ArrowDown,
                                    contentDescription = strings.searchNextTooltip,
                                    tint = palette.accentPrimary,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            val nextIdx = if (activeSearchMatchIndex < matchingMessageIndices.lastIndex) {
                                                activeSearchMatchIndex + 1
                                            } else {
                                                0
                                            }
                                            activeSearchMatchIndex = nextIdx
                                            val listIdx = matchingMessageIndices[nextIdx]
                                            coroutineScope.launch {
                                                listState.animateScrollToItem(listIdx)
                                                jumpedToMessageId = thread.messages[listIdx].id
                                            }
                                        }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        }
                        Icon(
                            imageVector = TxturaIcons.Close,
                            contentDescription = strings.closeSearchTooltip,
                            tint = if (chatSearchQuery.isNotEmpty()) palette.textSecondary else palette.textSecondary.copy(alpha = 0.4f),
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .clickable {
                                    isChatSearchVisible = false
                                    coroutineScope.launch {
                                        delay(150.milliseconds)
                                        chatSearchQuery = ""
                                        activeSearchMatchIndex = 0
                                    }
                                }
                        )
                    }
                }
            }

            // Glassmorphic Input Bar (SMS & MMS Composer)
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = (12 * metrics.widthScale).dp,
                        end = (12 * metrics.widthScale).dp,
                        top = (6 * metrics.scaleFactor).dp,
                        bottom = metrics.safeBottomClearance
                    ),
                shape = RoundedCornerShape((24 * metrics.scaleFactor).dp),
                elevated = true,
                accentHighlight = if (isMmsMode) palette.accentSecondary else null,
                contentPadding = PaddingValues((10 * metrics.scaleFactor).dp)
            ) {
                Column {
                    AnimatedVisibility(
                        visible = pendingAttachmentUri != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        if (pendingAttachmentUri != null) {
                            val uriStr = pendingAttachmentUri.toString()
                            val isPendingCal = uriStr.contains("mms_cal") || uriStr.endsWith(".ics", ignoreCase = true) || uriStr.endsWith(".vcs", ignoreCase = true)
                            val isPendingDoc = !isPendingCal && (uriStr.contains("mms_doc") ||
                                (!uriStr.endsWith(".jpg", true) && !uriStr.endsWith(".jpeg", true) &&
                                 !uriStr.endsWith(".png", true) && !uriStr.endsWith(".webp", true)))
                            val previewTitle = when {
                                isPendingCal -> strings.calendarEventTitle
                                isPendingDoc -> DocumentAttachmentHelper.getFileName(context, pendingAttachmentUri)
                                else -> strings.mmsAttachedTitle
                            }
                            val previewSub = when {
                                isPendingCal -> strings.calendarEventSub
                                isPendingDoc -> strings.documentFileSub
                                else -> strings.mmsAutoCompressedSub
                            }
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp),
                                shape = RoundedCornerShape(16.dp),
                                accentHighlight = palette.accentSecondary,
                                contentPadding = PaddingValues(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                                        if (isPendingCal || isPendingDoc) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(palette.accentSecondary.copy(alpha = 0.15f))
                                            ) {
                                                Icon(
                                                    imageVector = if (isPendingCal) TxturaIcons.CalendarEvent else TxturaIcons.DocumentFile,
                                                    contentDescription = previewTitle,
                                                    tint = palette.accentSecondary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        } else {
                                            MmsImageThumbnail(
                                                uri = pendingAttachmentUri,
                                                hdrLabel = strings.hdrMmsPhoto,
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f, fill = false)) {
                                            Text(
                                                text = previewTitle,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = palette.accentSecondary,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = previewSub,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.textSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    GlassIconButton(
                                        icon = TxturaIcons.Close,
                                        contentDescription = strings.removeMmsAttachment,
                                        onClick = onClearAttachment,
                                        size = 30.dp
                                    )
                                }
                            }
                        }
                    }

                    val isVoiceRecording by VoiceMessageManager.isRecording
                    var recordDragOffsetXPx by remember { mutableFloatStateOf(0f) }
                    val cancelThresholdPx = with(LocalDensity.current) { 45.dp.toPx() }
                    val isCancelTargetReached = recordDragOffsetXPx > cancelThresholdPx
                    val canSend = draftText.isNotBlank() || pendingAttachmentUri != null
                    val isCountingDown = pendingDelayedSend != null

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                    ) {
                        if (isVoiceRecording) {
                            // 1. Levi deo: Povećan REC brojač i tačno ispod remaining "Slide to cancel"
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp)
                                    .padding(start = 8.dp, end = 94.dp),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                val recPulseTransition = rememberInfiniteTransition(label = "rec_pulse")
                                val recDotAlpha by recPulseTransition.animateFloat(
                                    initialValue = 0.3f,
                                    targetValue = 1.0f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(500),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "dot_alpha"
                                )

                                val elapsedMs = VoiceMessageManager.recordingDurationMillis.longValue
                                val remSecs = VoiceMessageManager.recordingRemainingSeconds.longValue
                                val remText = VoiceMessageManager.formatDuration(remSecs * 1000L)
                                val elapsedText = VoiceMessageManager.formatDuration(elapsedMs)

                                // Povećan REC brojač sa pulsirajućom tačkom
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(11.dp)
                                            .clip(CircleShape)
                                            .background(palette.dangerColor.copy(alpha = recDotAlpha))
                                    )
                                    Text(
                                        text = elapsedText,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 19.sp
                                        ),
                                        color = palette.textPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Remaining tekst i tačno ispod njega centriran "Slide to cancel"
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = strings.voiceMessageTimeRemaining(remText),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = palette.accentSecondary,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = if (isCancelTargetReached) (if (strings.isSerbian) "Otpustite za poništavanje" else "Release to cancel") else strings.voiceMessageSlideToCancel,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.5.sp,
                                            fontWeight = if (isCancelTargetReached) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isCancelTargetReached) palette.dangerColor else palette.textSecondary,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // 2. Crveni 'X' cilj za poništavanje snimanja: postavljen DESNO od mikrofona
                            val cancelScale by animateFloatAsState(
                                targetValue = if (isCancelTargetReached) 1.25f else 1.0f,
                                animationSpec = TxturaMotion.pressSpec(isCancelTargetReached),
                                label = "cancel_scale"
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 4.dp)
                                    .size(38.dp)
                                    .scale(cancelScale)
                                    .clip(CircleShape)
                                    .background(
                                        if (isCancelTargetReached) palette.dangerColor.copy(alpha = 0.35f)
                                        else palette.dangerColor.copy(alpha = 0.15f)
                                    )
                                    .border(
                                        width = if (isCancelTargetReached) 2.dp else 1.dp,
                                        color = palette.dangerColor,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        VoiceMessageManager.cancelRecording()
                                        if (inAppSoundsEnabled) {
                                            SmsMmsRepository.playVoiceRecordCancelSound(context)
                                        }
                                        TxturaHaptics.reject(view, context)
                                        recordDragOffsetXPx = 0f
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = TxturaIcons.Close,
                                    contentDescription = strings.cancel,
                                    tint = palette.dangerColor,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (isVoiceRecording) 0f else 1f)
                        ) {
                            val plusRotation by animateFloatAsState(
                                targetValue = if (showAttachmentActions) 45f else 0f,
                                animationSpec = if (expressiveAnimationsEnabled) {
                                    TxturaMotion.rotateSpec()
                                } else {
                                    tween(150)
                                },
                                label = "plus_rotation"
                            )

                            // 1. Plus (+) icon toggle for attachment options
                            GlassIconButton(
                                icon = TxturaIcons.Plus,
                                contentDescription = strings.attachMmsImage,
                                tint = if (showAttachmentActions) palette.accentPrimary else palette.accentSecondary,
                                onClick = { showAttachmentActions = !showAttachmentActions },
                                size = if (showAttachmentActions) 34.dp else 38.dp,
                                modifier = Modifier.rotate(plusRotation)
                            )

                            // 2. Expandable attachment actions (Image & Contact)
                            AnimatedVisibility(
                                visible = showAttachmentActions,
                                enter = TxturaMotion.horizontalExpandEnter(isExpressive = expressiveAnimationsEnabled),
                                exit = TxturaMotion.horizontalCollapseExit(isExpressive = expressiveAnimationsEnabled)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.padding(start = 3.dp)
                                ) {
                                    GlassIconButton(
                                        icon = TxturaIcons.AttachImage,
                                        contentDescription = strings.attachMmsImage,
                                        tint = palette.accentSecondary,
                                        onClick = {
                                            showAttachmentActions = false
                                            onPickMmsPhoto()
                                        },
                                        size = 34.dp
                                    )

                                    GlassIconButton(
                                        icon = TxturaIcons.CalendarEvent,
                                        contentDescription = strings.attachCalendarEvent,
                                        tint = palette.accentSecondary,
                                        onClick = {
                                            showAttachmentActions = false
                                            DynamicPermissionManager.checkAndRequest(
                                                context = context,
                                                permission = Manifest.permission.READ_CALENDAR,
                                                companionMessage = if (strings.isSerbian) "Potrebna je dozvola za kalendar radi odabira događaja" else "Calendar permission required to pick events",
                                                permanentlyDeniedMessage = if (strings.isSerbian) "Dozvola za kalendar je trajno odbijena. Uključite je u podešavanjima." else "Calendar permission permanently denied. Enable in Settings.",
                                                requestLauncher = { calendarPermissionLauncher.launch(it) },
                                                onAlreadyGranted = { showCalendarPickerDialog = true }
                                            )
                                        },
                                        size = 34.dp
                                    )

                                    GlassIconButton(
                                        icon = TxturaIcons.DocumentFile,
                                        contentDescription = strings.attachDocumentFile,
                                        tint = palette.accentSecondary,
                                        onClick = {
                                            showAttachmentActions = false
                                            documentPickerLauncher.launch(arrayOf("*/*"))
                                        },
                                        size = 34.dp
                                    )

                                    GlassIconButton(
                                        icon = TxturaIcons.ContactPerson,
                                        contentDescription = strings.shareContactTooltip,
                                        tint = palette.accentSecondary,
                                        onClick = {
                                            showAttachmentActions = false
                                            DynamicPermissionManager.checkAndRequest(
                                                context = context,
                                                permission = Manifest.permission.READ_CONTACTS,
                                                companionMessage = if (strings.isSerbian) "Potrebna je dozvola za kontakte radi deljenja" else "Contacts permission required to share contacts",
                                                permanentlyDeniedMessage = if (strings.isSerbian) "Dozvola za kontakte je trajno odbijena. Uključite je u podešavanjima." else "Contacts permission permanently denied. Enable in Settings.",
                                                requestLauncher = { contactsPermissionLauncher.launch(it) },
                                                onAlreadyGranted = { showShareContactDialog = true }
                                            )
                                        },
                                        size = 34.dp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(if (showAttachmentActions) 3.dp else 4.dp))

                            // 3. Lupa (Search) icon to toggle search bar
                            GlassIconButton(
                                icon = TxturaIcons.Search,
                                contentDescription = strings.searchInChatPlaceholder,
                                tint = if (isChatSearchVisible) palette.accentPrimary else palette.textSecondary,
                                onClick = {
                                    isChatSearchVisible = !isChatSearchVisible
                                    if (!isChatSearchVisible) {
                                        chatSearchQuery = ""
                                        activeSearchMatchIndex = 0
                                    }
                                },
                                size = if (showAttachmentActions) 34.dp else 38.dp
                            )

                            Spacer(modifier = Modifier.width(if (showAttachmentActions) 6.dp else 8.dp))

                            if (!showAttachmentActions) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Box {
                                        if (draftText.isEmpty()) {
                                            Text(
                                                text = if (isMmsMode) strings.addMmsCaptionPlaceholder else strings.writeSmsPlaceholder,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontSize = 15.sp
                                                ),
                                                color = palette.textSecondary,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        BasicTextField(
                                            value = draftText,
                                            onValueChange = { draftText = it },
                                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                                color = palette.textPrimary,
                                                fontSize = 15.5.sp
                                            ),
                                            cursorBrush = SolidColor(palette.accentPrimary),
                                            maxLines = 4,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .focusRequester(inputFocusRequester)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = if (isMmsMode) {
                                            strings.mmsModeIndicator
                                        } else {
                                            "${draftText.length}/$charsPerSegment ($smsSegments) • SMS"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isMmsMode) palette.accentSecondary else palette.textSecondary
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            val canSend = draftText.isNotBlank() || pendingAttachmentUri != null
                            val isCountingDown = pendingDelayedSend != null

                            AnimatedContent(
                                targetState = isCountingDown,
                                transitionSpec = {
                                    (fadeIn(tween(180)) + scaleIn(initialScale = 0.92f))
                                        .togetherWith(fadeOut(tween(180)) + scaleOut(targetScale = 0.92f))
                                },
                                label = "send_action_slot"
                            ) { countingDown ->
                                if (countingDown) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val activeDelayed = pendingDelayedSend
                                        val totalMs = activeDelayed?.durationMillis ?: 6_000L
                                        val secondsLeft = ceil(delayedRemainingMillis / 1000.0).toInt().coerceIn(1, 6)
                                        val fraction = (delayedRemainingMillis.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)

                                        SendCountdownTimerBadge(
                                            secondsLeft = secondsLeft,
                                            progressFraction = fraction,
                                            contentDescription = "${secondsLeft}s"
                                        )

                                        // Red "X" (Close) button to cancel sending
                                        GlassIconButton(
                                            icon = TxturaIcons.Close,
                                            contentDescription = strings.cancelDelayedSendTooltip,
                                            tint = palette.dangerColor,
                                            accentHighlight = palette.dangerColor,
                                            onClick = {
                                                TxturaHaptics.reject(view, context)
                                                val cancelled = pendingDelayedSend
                                                pendingDelayedSend = null
                                                delayedRemainingMillis = 0L

                                                if (cancelled?.attachmentUri != null) {
                                                    try {
                                                        val path = cancelled.attachmentUri.path
                                                        if (path != null && path.contains("voice_msg")) {
                                                            File(context.cacheDir, "voice_messages/${File(path).name}").delete()
                                                        }
                                                    } catch (_: Exception) {}
                                                }

                                                if (inAppSoundsEnabled) {
                                                    SmsMmsRepository.playInAppMessageCancelledSound(context)
                                                }

                                                // Wait for exit animations (countdown badge & icons) to 100% finish before restoring draft text
                                                coroutineScope.launch {
                                                    delay(250.milliseconds)
                                                    if (cancelled != null && cancelled.body.isNotEmpty() && cancelled.attachmentUri == null) {
                                                        draftText = if (draftText.isEmpty()) {
                                                            cancelled.body
                                                        } else {
                                                            "${cancelled.body} $draftText"
                                                        }
                                                    }
                                                    try {
                                                        inputFocusRequester.requestFocus()
                                                        keyboardController?.show()
                                                    } catch (_: Exception) { }
                                                    Toast.makeText(
                                                        context,
                                                        strings.messageSendCancelledToast,
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            },
                                            size = 42.dp
                                        )
                                    }
                                } else {
                                    // Normal Send button
                                    GlassIconButton(
                                        icon = TxturaIcons.Send,
                                        contentDescription = strings.send,
                                        tint = if (canSend) palette.sendButtonColor else palette.textSecondary,
                                        accentHighlight = if (canSend) palette.accentPrimary else null,
                                        onClick = {
                                            if (canSend) {
                                                TxturaHaptics.confirm(view, context)
                                                // Flush any already-counting message before handling the new message
                                                pendingDelayedSend?.let { prev ->
                                                    onSendMessage(prev.body, prev.attachmentUri)
                                                    pendingDelayedSend = null
                                                    delayedRemainingMillis = 0L
                                                }
                                                val msgBodyToSend = draftText
                                                draftText = ""
                                                SmsMmsRepository.clearDraftForThread(context, thread.id)
                                                if (pendingAttachmentUri != null) {
                                                    onClearAttachment()
                                                }
                                                if (recallMessagesEnabled) {
                                                    delayedRemainingMillis = 6_000L
                                                    pendingDelayedSend = PendingDelayedMessage(
                                                        id = System.nanoTime(),
                                                        body = msgBodyToSend,
                                                        attachmentUri = pendingAttachmentUri,
                                                        startedAtMillis = System.currentTimeMillis(),
                                                        durationMillis = 6_000L
                                                    )
                                                } else {
                                                    onSendMessage(msgBodyToSend, pendingAttachmentUri)
                                                }
                                            }
                                        },
                                        onLongClick = {
                                            TxturaHaptics.longPress(view, context)
                                            showScheduleMessageDialog = true
                                        },
                                        size = 42.dp
                                    )
                                }
                            }
                        }

                        // 3. Persistent Microphone button (stays alive and tracks gesture smoothly)
                        if (!isCountingDown) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 50.dp)
                                    .offset { IntOffset(recordDragOffsetXPx.roundToInt().coerceIn(0, (cancelThresholdPx * 1.25f).toInt()), 0) }
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isVoiceRecording) {
                                            if (isCancelTargetReached) SolidColor(palette.dangerColor.copy(alpha = 0.85f))
                                            else Brush.linearGradient(listOf(palette.accentPrimary, palette.accentSecondary))
                                        } else {
                                            SolidColor(palette.glassSurfaceElevated.copy(alpha = 0.6f))
                                        }
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isVoiceRecording && isCancelTargetReached) palette.dangerColor else palette.glassBorderTop,
                                        shape = CircleShape
                                    )
                                    .pointerInput(canSend, recallMessagesEnabled) {
                                        awaitEachGesture {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            val granted = DynamicPermissionManager.hasPermission(context, Manifest.permission.RECORD_AUDIO)
                                            if (!granted) {
                                                hasMicPermission = false
                                                DynamicPermissionManager.checkAndRequest(
                                                    context = context,
                                                    permission = Manifest.permission.RECORD_AUDIO,
                                                    companionMessage = strings.voiceMessagePermissionRequired,
                                                    permanentlyDeniedMessage = if (strings.isSerbian) "Dozvola za mikrofon je trajno odbijena. Uključite je u podešavanjima." else "Microphone permission permanently denied. Enable in Settings.",
                                                    requestLauncher = { recordAudioLauncher.launch(it) },
                                                    onAlreadyGranted = { /* proceed */ }
                                                )
                                                return@awaitEachGesture
                                            }
                                            hasMicPermission = true

                                            recordDragOffsetXPx = 0f
                                            val started = VoiceMessageManager.startRecording(context) { limitFile ->
                                                TxturaHaptics.confirm(view, context)
                                                val audioUri = FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.fileprovider",
                                                    limitFile
                                                )
                                                val voiceTitle = if (strings.isSerbian) "Govorna poruka" else "Voice message"
                                                if (recallMessagesEnabled) {
                                                    delayedRemainingMillis = 6_000L
                                                    pendingDelayedSend = PendingDelayedMessage(
                                                        id = System.nanoTime(),
                                                        body = voiceTitle,
                                                        attachmentUri = audioUri,
                                                        startedAtMillis = System.currentTimeMillis(),
                                                        durationMillis = 6_000L
                                                    )
                                                } else {
                                                    onSendMessage(voiceTitle, audioUri)
                                                }
                                                if (inAppSoundsEnabled) {
                                                    SmsMmsRepository.playInAppMessageSentSound(context)
                                                }
                                            }

                                            if (!started) {
                                                Toast.makeText(
                                                    context,
                                                    if (strings.isSerbian) "Greška pri pokretanju mikrofona." else "Failed to start microphone.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                return@awaitEachGesture
                                            }

                                            if (inAppSoundsEnabled) {
                                                SmsMmsRepository.playVoiceRecordStartSound(context)
                                            }
                                            TxturaHaptics.confirm(view, context)

                                            var cancelled = false
                                            var currentDragX = 0f

                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                                if (!change.pressed) {
                                                    change.consume()
                                                    break
                                                }
                                                val dragX = change.position.x - change.previousPosition.x
                                                currentDragX = (currentDragX + dragX).coerceAtLeast(0f)
                                                recordDragOffsetXPx = currentDragX
                                                cancelled = currentDragX > cancelThresholdPx
                                            }

                                            if (cancelled) {
                                                VoiceMessageManager.cancelRecording()
                                                if (inAppSoundsEnabled) {
                                                    SmsMmsRepository.playVoiceRecordCancelSound(context)
                                                }
                                                TxturaHaptics.reject(view, context)
                                            } else {
                                                val recDur = VoiceMessageManager.recordingDurationMillis.longValue
                                                val recFile = VoiceMessageManager.stopRecording(context)
                                                if (recFile != null && recFile.exists() && recFile.length() > 200L && recDur >= 500L) {
                                                    TxturaHaptics.confirm(view, context)
                                                    val audioUri = FileProvider.getUriForFile(
                                                        context,
                                                        "${context.packageName}.fileprovider",
                                                        recFile
                                                    )
                                                    val voiceTitle = if (strings.isSerbian) "Govorna poruka" else "Voice message"
                                                    if (recallMessagesEnabled) {
                                                        delayedRemainingMillis = 6_000L
                                                        pendingDelayedSend = PendingDelayedMessage(
                                                            id = System.nanoTime(),
                                                            body = voiceTitle,
                                                            attachmentUri = audioUri,
                                                            startedAtMillis = System.currentTimeMillis(),
                                                            durationMillis = 6_000L
                                                        )
                                                    } else {
                                                        onSendMessage(voiceTitle, audioUri)
                                                    }
                                                    if (inAppSoundsEnabled) {
                                                        SmsMmsRepository.playInAppMessageSentSound(context)
                                                    }
                                                } else {
                                                    try { recFile?.delete() } catch (_: Exception) {}
                                                    if (recDur < 500L) {
                                                        Toast.makeText(
                                                            context,
                                                            strings.voiceMessageHoldToRecordHint,
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            }
                                            recordDragOffsetXPx = 0f
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = TxturaIcons.Microphone,
                                    contentDescription = strings.voiceMessageTooltip,
                                    tint = if (isVoiceRecording) Color.White else palette.accentSecondary,
                                    modifier = Modifier.size(if (isVoiceRecording) 22.dp else 20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Circular 6-second countdown timer badge displayed directly to the left of the Send/Cancel button.
 * Visual indicator of remaining time before the message is sent. (Cancellation is performed via the red X button).
 */
@Composable
private fun SendCountdownTimerBadge(
    secondsLeft: Int,
    progressFraction: Float,
    contentDescription: String,
) {
    val palette = LocalGlassPalette.current
    GlassCard(
        modifier = Modifier
            .size(42.dp)
            .drawBehind {
                val strokeWidthPx = 2.6.dp.toPx()
                val inset = strokeWidthPx / 2f + 2.dp.toPx()
                val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
                // Background track ring
                drawArc(
                    color = palette.cautionColor.copy(alpha = 0.22f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
                // Active remaining time ring
                drawArc(
                    color = palette.cautionColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progressFraction,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            },
        shape = CircleShape,
        elevated = true,
        accentHighlight = palette.cautionColor,
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = TxturaIcons.ClockTimer,
                contentDescription = contentDescription,
                tint = palette.cautionColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "${secondsLeft}s",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = palette.textPrimary
            )
        }
    }
}

/**
 * Glassmorphic dialog for scheduling a message when long-pressing the Send button.
 * Allows entering any exact future date and time manually via numeric input (day, month, year, hour, minute)
 * or picking a quick preset, without any day/time restrictions.
 */
@Composable
private fun ScheduleMessageDialog(
    initialMessageText: String,
    hasAttachment: Boolean,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onConfirmSchedule: (messageBody: String, scheduledAtMillis: Long) -> Unit,
) {
    val palette = LocalGlassPalette.current
    val context = LocalContext.current
    var messageText by remember { mutableStateOf(initialMessageText) }

    val defaultCal = remember {
        Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
        }
    }
    var dayInput by remember { mutableStateOf(defaultCal[Calendar.DAY_OF_MONTH].toString()) }
    var monthInput by remember { mutableStateOf((defaultCal[Calendar.MONTH] + 1).toString()) }
    var yearInput by remember { mutableStateOf(defaultCal[Calendar.YEAR].toString()) }
    var hourInput by remember {
        mutableStateOf(String.format(Locale.ROOT, "%02d", defaultCal[Calendar.HOUR_OF_DAY]))
    }
    var minuteInput by remember {
        mutableStateOf(String.format(Locale.ROOT, "%02d", defaultCal[Calendar.MINUTE]))
    }

    val canSchedule = messageText.isNotBlank() || hasAttachment

    fun parseAndComputeTimestamp(): Long? {
        val d = dayInput.trim().toIntOrNull() ?: return null
        val m = monthInput.trim().toIntOrNull() ?: return null
        val y = yearInput.trim().toIntOrNull() ?: return null
        val h = hourInput.trim().toIntOrNull() ?: return null
        val min = minuteInput.trim().toIntOrNull() ?: return null

        if (y !in 2024..2100) return null
        if (m !in 1..12) return null
        if (h !in 0..23) return null
        if (min !in 0..59) return null

        val cal = Calendar.getInstance()
        cal.isLenient = false
        return try {
            cal[Calendar.YEAR] = y
            cal[Calendar.MONTH] = m - 1
            val maxDayInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            if (d !in 1..maxDayInMonth) return null
            cal[Calendar.DAY_OF_MONTH] = d
            cal[Calendar.HOUR_OF_DAY] = h
            cal[Calendar.MINUTE] = min
            cal[Calendar.SECOND] = 0
            cal[Calendar.MILLISECOND] = 0
            val computed = cal.timeInMillis
            if (computed > System.currentTimeMillis() + 10_000L) computed else null
        } catch (_: Exception) {
            null
        }
    }

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        accentBorderColor = palette.accentPrimary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = TxturaIcons.ClockTimer,
                    contentDescription = null,
                    tint = palette.accentPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.scheduleMessageTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = strings.scheduleMessageSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )

                // Editable message text box inside the schedule dialog
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.glassSurface.copy(alpha = (palette.glassSurface.alpha * 0.45f).coerceIn(0.06f, 0.35f)))
                        .border(1.dp, palette.glassBorderBottom.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Box {
                        if (messageText.isEmpty()) {
                            Text(
                                text = strings.writeSmsPlaceholder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                        }
                        BasicTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = palette.textPrimary
                            ),
                            cursorBrush = SolidColor(palette.accentPrimary),
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Proposal: Midnight for Birthday (00:00)
                val isMidnightSelected = hourInput == "00" && minuteInput == "00"
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    elevated = isMidnightSelected,
                    accentHighlight = if (isMidnightSelected) palette.accentPrimary else null,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    onClick = {
                        hourInput = "00"
                        minuteInput = "00"
                        val nowCal = Calendar.getInstance()
                        val currentDay = nowCal[Calendar.DAY_OF_MONTH]
                        val currentMonth = nowCal[Calendar.MONTH] + 1
                        val currentYear = nowCal[Calendar.YEAR]
                        val inputD = dayInput.toIntOrNull() ?: currentDay
                        val inputM = monthInput.toIntOrNull() ?: currentMonth
                        val inputY = yearInput.toIntOrNull() ?: currentYear
                        // If selected date is today, roll to tomorrow because today's 00:00 is in the past
                        if (inputD == currentDay && inputM == currentMonth && inputY == currentYear) {
                            nowCal.add(Calendar.DAY_OF_MONTH, 1)
                            dayInput = nowCal[Calendar.DAY_OF_MONTH].toString()
                            monthInput = (nowCal[Calendar.MONTH] + 1).toString()
                            yearInput = nowCal[Calendar.YEAR].toString()
                        }
                        if (messageText.isBlank()) {
                            messageText = strings.scheduleBirthdayDefaultGreeting
                        }
                        Toast.makeText(context, strings.scheduleBirthdayMidnightApplied, Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = TxturaIcons.ClockTimer,
                                contentDescription = null,
                                tint = if (isMidnightSelected) palette.accentPrimary else palette.accentSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.scheduleBirthdayMidnightChip,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isMidnightSelected) palette.accentPrimary else palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (isMidnightSelected) {
                            Icon(
                                imageVector = TxturaIcons.Check,
                                contentDescription = null,
                                tint = palette.accentPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Exact Date & Time manual numeric inputs
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = strings.scheduleCustomTimeTitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            val parsedTs = parseAndComputeTimestamp()
                            if (parsedTs != null) {
                                val fmt = remember { SimpleDateFormat("dd.MM.yyyy. HH:mm", Locale.ROOT) }
                                Text(
                                    text = fmt.format(Date(parsedTs)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.accentPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Row 1: Date (Day, Month, Year)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ScheduleNumericInputField(
                                value = dayInput,
                                onValueChange = { dayInput = it.take(2).filter(Char::isDigit) },
                                label = strings.scheduleDayLabel,
                                placeholder = "01",
                                modifier = Modifier.weight(1f)
                            )
                            ScheduleNumericInputField(
                                value = monthInput,
                                onValueChange = { monthInput = it.take(2).filter(Char::isDigit) },
                                label = strings.scheduleMonthLabel,
                                placeholder = "01",
                                modifier = Modifier.weight(1f)
                            )
                            ScheduleNumericInputField(
                                value = yearInput,
                                onValueChange = { yearInput = it.take(4).filter(Char::isDigit) },
                                label = strings.scheduleYearLabel,
                                placeholder = "2026",
                                modifier = Modifier.weight(1.35f)
                            )
                        }

                        // Row 2: Time (Hour, Minute)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ScheduleNumericInputField(
                                value = hourInput,
                                onValueChange = { hourInput = it.take(2).filter(Char::isDigit) },
                                label = strings.scheduleHourLabel,
                                placeholder = "12",
                                modifier = Modifier.weight(1f)
                            )
                            ScheduleNumericInputField(
                                value = minuteInput,
                                onValueChange = { minuteInput = it.take(2).filter(Char::isDigit) },
                                label = strings.scheduleMinuteLabel,
                                placeholder = "00",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSchedule,
                onClick = {
                    if (canSchedule) {
                        val timestamp = parseAndComputeTimestamp()
                        if (timestamp != null) {
                            onConfirmSchedule(messageText, timestamp)
                        } else {
                            Toast.makeText(
                                context,
                                strings.scheduleInvalidDateTime,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            ) {
                Text(
                    text = strings.scheduleConfirmButton,
                    color = if (canSchedule) palette.accentPrimary else palette.textSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel, color = palette.textSecondary)
            }
        }
    )
}

/**
 * Compact numeric entry box for day, month, year, hour, or minute in [ScheduleMessageDialog].
 */
@Composable
private fun ScheduleNumericInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val palette = LocalGlassPalette.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(palette.glassSurface.copy(alpha = (palette.glassSurface.alpha * 0.45f).coerceIn(0.06f, 0.35f)))
            .border(1.dp, palette.glassBorderBottom.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textSecondary.copy(alpha = 0.5f)
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold
                    ),
                    cursorBrush = SolidColor(palette.accentPrimary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}


private fun openSafeUrlInDefaultBrowser(context: Context, rawUrl: String) {
    val strings = TxturaStrings.forLocale()
    try {
        val normalized = if (
            rawUrl.startsWith("https://", ignoreCase = true) ||
            rawUrl.startsWith("http://", ignoreCase = true)
        ) {
            rawUrl
        } else {
            "https://$rawUrl"
        }
        val uri = normalized.toUri()
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        if (scheme != "https" && scheme != "http") return
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, strings.browserOpenErrorToast, Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun MessageBubbleItem(
    message: ChatMessage,
    zoomScale: Float = 1.0f,
    expressiveAnimationsEnabled: Boolean = true,
    autoPreviewsShowAll: Boolean = true,
    autoPreviewsWebLinksOnly: Boolean = false,
    autoPreviewsWifiOnly: Boolean = false,
    strings: AppStrings,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    isJumpHighlighted: Boolean = false,
    onClickMessage: () -> Unit = {},
    onLongPressMessage: () -> Unit = {},
    onCopyOtp: (String) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val palette = LocalGlassPalette.current
    val isOutgoing = !message.isIncoming
    val analysis = message.securityAnalysis
    val s = zoomScale.coerceIn(0.75f, 1.65f)
    val cornerRadius = (18f * s).coerceIn(12f, 26f).dp
    val tailRadius = (4f * s).coerceIn(3f, 8f).dp
    val currentOnLongPress by rememberUpdatedState(onLongPressMessage)
    val currentOnClick by rememberUpdatedState(onClickMessage)

    var activeDetectedEntity by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    val detectedPhones = remember(message.body) {
        MessageSecurityAnalyzer.extractPhoneNumbers(message.body)
    }
    val detectedEmails = remember(message.body) {
        MessageSecurityAnalyzer.extractEmailAddresses(message.body)
    }
    val detectedBankAccounts = remember(message.body) {
        MessageSecurityAnalyzer.extractBankAccounts(message.body)
    }

    val bubbleShape = if (isOutgoing) {
        RoundedCornerShape(
            topStart = cornerRadius,
            topEnd = cornerRadius,
            bottomStart = cornerRadius,
            bottomEnd = tailRadius
        )
    } else {
        RoundedCornerShape(
            topStart = cornerRadius,
            topEnd = cornerRadius,
            bottomStart = tailRadius,
            bottomEnd = cornerRadius
        )
    }

    val bubbleBrush = remember(
        isOutgoing,
        isSelected,
        message.isHighlighted,
        isJumpHighlighted,
        palette.outgoingBubbleStart,
        palette.outgoingBubbleEnd,
        palette.incomingBubbleSurface,
        palette.cautionColor,
        palette.accentPrimary
    ) {
        if (isSelected) {
            Brush.linearGradient(
                colors = listOf(
                    palette.accentPrimary.copy(alpha = 0.46f),
                    if (isOutgoing) palette.outgoingBubbleEnd else palette.incomingBubbleSurface
                )
            )
        } else if (isJumpHighlighted) {
            Brush.linearGradient(
                colors = listOf(
                    palette.accentPrimary.copy(alpha = 0.42f),
                    if (isOutgoing) palette.outgoingBubbleEnd else palette.incomingBubbleSurface
                )
            )
        } else if (message.isHighlighted) {
            Brush.linearGradient(
                colors = listOf(
                    palette.cautionColor.copy(alpha = 0.42f),
                    if (isOutgoing) palette.outgoingBubbleEnd else palette.incomingBubbleSurface
                )
            )
        } else if (isOutgoing) {
            Brush.linearGradient(
                colors = listOf(palette.outgoingBubbleStart, palette.outgoingBubbleEnd)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    palette.incomingBubbleSurface,
                    palette.incomingBubbleSurface.copy(
                        alpha = (palette.incomingBubbleSurface.alpha * 0.8f).coerceIn(0.08f, 0.95f)
                    )
                )
            )
        }
    }

    val borderColor = when {
        isSelected -> palette.accentPrimary
        isJumpHighlighted -> palette.accentPrimary
        analysis.riskLevel == SecurityRiskLevel.SUSPICIOUS -> palette.dangerColor
        message.isHighlighted -> palette.cautionColor
        message.isPinned -> palette.accentPrimary
        isOutgoing -> palette.accentPrimary.copy(alpha = 0.55f)
        else -> palette.glassBorderTop
    }

    // Automatically recognize safe links and make them clickable to open in the default browser
    val safeLinks = remember(message.body, analysis) {
        MessageSecurityAnalyzer.extractSafeLinks(message.body, analysis)
    }
    val linkColor = if (isOutgoing) Color(0xFFBAE6FD) else palette.accentPrimary

    val annotatedMessageText = remember(message.body, safeLinks, linkColor) {
        buildAnnotatedString {
            append(message.body)
            safeLinks.forEach { link ->
                addLink(
                    clickable = LinkAnnotation.Clickable(
                        tag = link.browserUrl,
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.SemiBold
                            )
                        ),
                        linkInteractionListener = {
                            openSafeUrlInDefaultBrowser(context, link.browserUrl)
                        }
                    ),
                    start = link.startIndex,
                    end = link.endIndex
                )
            }
        }
    }

    val targetScale = if (isSelected) 1.025f else 1.0f
    val animatedBubbleScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = if (expressiveAnimationsEnabled) {
            TxturaMotion.pressSpec(false)
        } else {
            tween(durationMillis = 80)
        },
        label = "message_bubble_scale"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = (300f * s).coerceIn(210f, 380f).dp)
                .graphicsLayer {
                    scaleX = animatedBubbleScale
                    scaleY = animatedBubbleScale
                }
                .clip(bubbleShape)
                .background(brush = bubbleBrush, shape = bubbleShape)
                .border(
                    width = if (isSelected || isJumpHighlighted) 2.dp else if (message.isHighlighted) 1.5.dp else 1.dp,
                    color = borderColor,
                    shape = bubbleShape
                )
                .pointerInput(isSelectionMode) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downPos = down.position

                        val outcome = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                val current = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!current.pressed) {
                                    return@withTimeoutOrNull 1 // RELEASED
                                }
                                if ((current.position - downPos).getDistance() > viewConfiguration.touchSlop) {
                                    return@withTimeoutOrNull 2 // MOVED (scrolling)
                                }
                            }
                            1
                        } ?: 3 // 3 = TIMED_OUT (real long press)

                        when (outcome) {
                            3 -> {
                                TxturaHaptics.longPress(view, context)
                                currentOnLongPress()
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Main)
                                    val current = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!current.pressed) break
                                }
                            }
                            1 -> {
                                if (isSelectionMode) {
                                    TxturaHaptics.tick(view, context)
                                    currentOnClick()
                                }
                            }
                            2 -> {
                                // Finger moved > touchSlop (scrolling). Do nothing!
                            }
                        }
                    }
                }
                .padding(horizontal = (12f * s).dp, vertical = (8f * s).dp)
        ) {
            Column {
                if (message.isHighlighted) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = (4f * s).dp)
                    ) {
                        Icon(
                            imageVector = TxturaIcons.HighlightStar,
                            contentDescription = null,
                            tint = palette.cautionColor,
                            modifier = Modifier.size((13f * s).dp)
                        )
                        Spacer(modifier = Modifier.width((4f * s).dp))
                        Text(
                            text = strings.highlightedMessagesTitle,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = (10f * s).sp),
                            color = palette.cautionColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                val isAudioAttachment = message.attachment?.isAudio == true ||
                    message.attachment?.mimeType?.startsWith("audio/", ignoreCase = true) == true
                val isPendingMms = !isAudioAttachment && (message.status == DeliveryStatus.PENDING ||
                    message.status == DeliveryStatus.DOWNLOADING ||
                    (message.status == DeliveryStatus.FAILED && message.attachment?.mimeType == "application/vnd.wap.mms-message") ||
                    message.attachment?.mimeType == "application/vnd.wap.mms-message")

                if (message.attachment != null) {
                    if (isPendingMms) {
                        MmsPendingDownloadCard(
                            message = message,
                            strings = strings,
                            palette = palette,
                            fontScale = s
                        )
                    } else if (isAudioAttachment) {
                        VoiceMessagePlayerCard(
                            uri = message.attachment.uri,
                            isOutgoing = isOutgoing,
                            palette = palette,
                            fontScale = s
                        )
                    } else if (message.attachment.isCalendar) {
                        MmsCalendarEventCard(
                            attachment = message.attachment,
                            isOutgoing = isOutgoing,
                            palette = palette,
                            fontScale = s
                        )
                    } else if (message.attachment.isDocument) {
                        MmsDocumentCard(
                            attachment = message.attachment,
                            isOutgoing = isOutgoing,
                            palette = palette,
                            fontScale = s
                        )
                    } else {
                        MmsAttachmentCard(
                            uri = message.attachment.uri,
                            caption = message.attachment.caption,
                            hdrLabel = strings.hdrMmsPhoto
                        )
                    }
                    Spacer(modifier = Modifier.height((6f * s).dp))
                }

                val isPureVoiceOrAttachment = (isAudioAttachment && (
                    message.body.isBlank() ||
                    message.body == "Govorna poruka" ||
                    message.body == "Voice message" ||
                    message.body == strings.voiceMessageTitle
                )) || (message.attachment?.isCalendar == true && (
                    message.body.isBlank() ||
                    message.body == "Kalendarski događaj" ||
                    message.body == "Calendar event" ||
                    message.body == strings.calendarEventTitle
                )) || (message.attachment?.isDocument == true && (
                    message.body.isBlank() ||
                    message.body == "Dokument" ||
                    message.body == "Document" ||
                    message.body == strings.documentFileTitle ||
                    message.body == message.attachment.caption
                ))

                if (!isPendingMms && !isPureVoiceOrAttachment) {
                    Text(
                        text = annotatedMessageText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = (14f * s).sp,
                            lineHeight = (19f * s).sp
                        ),
                        color = if (isOutgoing) Color.White else palette.textPrimary
                    )
                }

                // Automatic preview cards for web-links (Screenshot 3)
                val isWifi = remember(context) { SmsMmsRepository.isWifiConnected(context) }
                val canShowPreviews = remember(autoPreviewsShowAll, autoPreviewsWebLinksOnly, autoPreviewsWifiOnly, isWifi) {
                    !(!autoPreviewsShowAll && !autoPreviewsWebLinksOnly) && !(autoPreviewsWifiOnly && !isWifi)
                }
                if (canShowPreviews && safeLinks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height((6f * s).dp))
                    safeLinks.forEach { link ->
                        val host = remember(link.browserUrl) {
                            runCatching { link.browserUrl.toUri().host ?: link.browserUrl }.getOrDefault(link.browserUrl)
                        }
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape((10f * s).dp),
                            elevated = true,
                            accentHighlight = if (isOutgoing) Color(0xFFBAE6FD) else palette.accentPrimary,
                            contentPadding = PaddingValues(horizontal = (10f * s).dp, vertical = (6f * s).dp),
                            onClick = { openSafeUrlInDefaultBrowser(context, link.browserUrl) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size((26f * s).dp)
                                        .clip(CircleShape)
                                        .background(palette.accentPrimary.copy(alpha = 0.15f))
                                ) {
                                    Icon(
                                        imageVector = TxturaIcons.Link,
                                        contentDescription = null,
                                        tint = if (isOutgoing) Color(0xFFBAE6FD) else palette.accentPrimary,
                                        modifier = Modifier.size((13f * s).dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width((8f * s).dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = host,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = (11f * s).sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (isOutgoing) Color.White else palette.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = link.browserUrl,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = (9f * s).sp
                                        ),
                                        color = if (isOutgoing) Color.White.copy(alpha = 0.7f) else palette.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Automatically detected phone numbers, email addresses, and bank accounts with quick actions
                if (detectedPhones.isNotEmpty() || detectedEmails.isNotEmpty() || detectedBankAccounts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height((6f * s).dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy((6f * s).dp),
                        verticalArrangement = Arrangement.spacedBy((4f * s).dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        detectedBankAccounts.forEach { bankAcc ->
                            GlassCard(
                                shape = RoundedCornerShape(50),
                                elevated = true,
                                accentHighlight = palette.safeColor,
                                contentPadding = PaddingValues(horizontal = (8f * s).dp, vertical = (4f * s).dp),
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clip = ClipData.newPlainText("Bank Account", bankAcc.accountNumber)
                                    clipboard?.setPrimaryClip(clip)
                                    Toast.makeText(
                                        context,
                                        strings.bankAccountCopiedToast(bankAcc.formattedNumber, bankAcc.bankName),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = TxturaIcons.CreditCard,
                                        contentDescription = null,
                                        tint = palette.safeColor,
                                        modifier = Modifier.size((12f * s).dp)
                                    )
                                    Spacer(modifier = Modifier.width((4f * s).dp))
                                    val chipLabel = if (!bankAcc.bankName.isNullOrBlank()) {
                                        "${bankAcc.formattedNumber} • ${bankAcc.bankName}"
                                    } else {
                                        bankAcc.formattedNumber
                                    }
                                    Text(
                                        text = chipLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = (11f * s).sp),
                                        color = palette.safeColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        detectedPhones.forEach { phone ->
                            GlassCard(
                                shape = RoundedCornerShape(50),
                                elevated = true,
                                accentHighlight = palette.accentPrimary,
                                contentPadding = PaddingValues(horizontal = (8f * s).dp, vertical = (4f * s).dp),
                                onClick = { activeDetectedEntity = Pair(phone, true) }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = TxturaIcons.PhoneCall,
                                        contentDescription = null,
                                        tint = palette.accentPrimary,
                                        modifier = Modifier.size((12f * s).dp)
                                    )
                                    Spacer(modifier = Modifier.width((4f * s).dp))
                                    Text(
                                        text = phone,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = (11f * s).sp),
                                        color = palette.accentPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        detectedEmails.forEach { email ->
                            GlassCard(
                                shape = RoundedCornerShape(50),
                                elevated = true,
                                accentHighlight = palette.accentSecondary,
                                contentPadding = PaddingValues(horizontal = (8f * s).dp, vertical = (4f * s).dp),
                                onClick = { activeDetectedEntity = Pair(email, false) }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = TxturaIcons.Mail,
                                        contentDescription = null,
                                        tint = palette.accentSecondary,
                                        modifier = Modifier.size((12f * s).dp)
                                    )
                                    Spacer(modifier = Modifier.width((4f * s).dp))
                                    Text(
                                        text = email,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = (11f * s).sp),
                                        color = palette.accentSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                if (analysis.riskLevel == SecurityRiskLevel.SUSPICIOUS) {
                    Spacer(modifier = Modifier.height((6f * s).dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape((10f * s).dp))
                            .background(palette.dangerColor.copy(alpha = 0.16f))
                            .border(1.dp, palette.dangerColor.copy(alpha = 0.5f), RoundedCornerShape((10f * s).dp))
                            .padding((8f * s).dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = TxturaIcons.Warning,
                                contentDescription = null,
                                tint = palette.dangerColor,
                                modifier = Modifier.size((14f * s).dp)
                            )
                            Spacer(modifier = Modifier.width((5f * s).dp))
                            Text(
                                text = strings.riskSuspicious,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = (11f * s).sp,
                                    lineHeight = (15f * s).sp
                                ),
                                color = palette.dangerColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        analysis.reasons.forEach { reason ->
                            Text(
                                text = "• $reason",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = (11f * s).sp,
                                    lineHeight = (15f * s).sp
                                ),
                                color = if (isOutgoing) Color.White else palette.textPrimary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                if (analysis.extractedOtpCode != null) {
                    Spacer(modifier = Modifier.height((6f * s).dp))
                    GlassCard(
                        shape = RoundedCornerShape(50),
                        elevated = true,
                        accentHighlight = palette.safeColor,
                        contentPadding = PaddingValues(horizontal = (10f * s).dp, vertical = (4f * s).dp),
                        onClick = { onCopyOtp(analysis.extractedOtpCode) }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = TxturaIcons.CopyKey,
                                contentDescription = null,
                                tint = palette.safeColor,
                                modifier = Modifier.size((13f * s).dp)
                            )
                            Spacer(modifier = Modifier.width((5f * s).dp))
                            Text(
                                text = "${strings.copyVerificationCodePrefix}${analysis.extractedOtpCode}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = (11f * s).sp,
                                    lineHeight = (15f * s).sp
                                ),
                                color = palette.safeColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height((3f * s).dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = TxturaIcons.Check,
                            contentDescription = null,
                            tint = if (isOutgoing) Color.White else palette.accentPrimary,
                            modifier = Modifier.size((13f * s).dp)
                        )
                        Spacer(modifier = Modifier.width((5f * s).dp))
                    }
                    if (message.isHighlighted) {
                        Icon(
                            imageVector = TxturaIcons.HighlightStar,
                            contentDescription = strings.highlightedMessagesTitle,
                            tint = if (isOutgoing) Color.White.copy(alpha = 0.9f) else palette.cautionColor,
                            modifier = Modifier.size((13f * s).dp)
                        )
                        Spacer(modifier = Modifier.width((5f * s).dp))
                    }
                    if (message.isPinned) {
                        Icon(
                            imageVector = TxturaIcons.PinFilled,
                            contentDescription = strings.pinMessage,
                            tint = if (isOutgoing) Color.White.copy(alpha = 0.9f) else palette.accentPrimary,
                            modifier = Modifier.size((13f * s).dp)
                        )
                        Spacer(modifier = Modifier.width((5f * s).dp))
                    }
                    if (message.attachment != null) {
                        Text(
                            text = "MMS • ",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = (11f * s).sp
                            ),
                            color = if (isOutgoing) Color.White.copy(alpha = 0.85f) else palette.accentSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = formatMessageTimestamp(message.timestampMillis, strings.isSerbian),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = (11f * s).sp
                        ),
                        color = if (isOutgoing) Color.White.copy(alpha = 0.78f) else palette.textSecondary
                    )
                    if (isOutgoing) {
                        Spacer(modifier = Modifier.width((5f * s).dp))
                        Icon(
                            imageVector = TxturaIcons.CheckDouble,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size((14f * s).dp)
                        )
                    }
                }
            }
        }
    }

    activeDetectedEntity?.let { (value, isPhone) ->
        DetectedEntityActionDialog(
            value = value,
            isPhone = isPhone,
            onDismiss = { activeDetectedEntity = null }
        )
    }
}

@Composable
private fun MmsPendingDownloadCard(
    message: ChatMessage,
    strings: AppStrings,
    palette: GlassPalette,
    fontScale: Float
) {
    val context = LocalContext.current
    val isDownloading = message.status == DeliveryStatus.DOWNLOADING
    val isFailed = message.status == DeliveryStatus.FAILED

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape((14f * fontScale).dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        palette.accentPrimary.copy(alpha = 0.25f),
                        palette.accentSecondary.copy(alpha = 0.18f)
                    )
                )
            )
            .border(1.dp, palette.glassBorderTop, RoundedCornerShape((14f * fontScale).dp))
            .padding((12f * fontScale).dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size((36f * fontScale).dp)
                    .clip(CircleShape)
                    .background(if (isFailed) palette.cautionColor.copy(alpha = 0.2f) else palette.accentPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFailed) TxturaIcons.Warning else TxturaIcons.AttachImage,
                    contentDescription = null,
                    tint = if (isFailed) palette.cautionColor else palette.accentPrimary,
                    modifier = Modifier.size((18f * fontScale).dp)
                )
            }
            Spacer(modifier = Modifier.width((10f * fontScale).dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.mmsPendingTitle,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = (13f * fontScale).sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = palette.textPrimary
                )
                val caption = message.attachment?.caption
                if (!caption.isNullOrBlank()) {
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = (11f * fontScale).sp
                        ),
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height((10f * fontScale).dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape((10f * fontScale).dp))
                .background(
                    if (isFailed) palette.cautionColor.copy(alpha = 0.85f)
                    else if (isDownloading) palette.accentPrimary.copy(alpha = 0.5f)
                    else palette.accentPrimary
                )
                .clickable(enabled = !isDownloading) {
                    SmsMmsRepository.downloadPendingMms(context, message.id)
                }
                .padding(vertical = (8f * fontScale).dp, horizontal = (12f * fontScale).dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size((14f * fontScale).dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width((8f * fontScale).dp))
                    Text(
                        text = strings.mmsDownloadingAction,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = (12f * fontScale).sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = if (isFailed) TxturaIcons.Restore else TxturaIcons.CloudDownload,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size((16f * fontScale).dp)
                    )
                    Spacer(modifier = Modifier.width((8f * fontScale).dp))
                    Text(
                        text = if (isFailed) strings.mmsDownloadFailed else strings.mmsDownloadAction,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = (12f * fontScale).sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceMessagePlayerCard(
    uri: Uri,
    isOutgoing: Boolean,
    palette: GlassPalette,
    fontScale: Float
) {
    val context = LocalContext.current
    val strings = TxturaStrings.current()
    val isPlayingThis = VoiceMessageManager.playingUri.value == uri && VoiceMessageManager.isPlaying.value
    val isLoadedThis = VoiceMessageManager.playingUri.value == uri
    val progress = if (isLoadedThis) VoiceMessageManager.progressFraction.value else 0f
    val currentMs = if (isLoadedThis) VoiceMessageManager.currentPositionMs.value else 0

    var durationMs by remember(uri) {
        mutableLongStateOf(VoiceMessageManager.getAudioDurationMs(context, uri))
    }
    LaunchedEffect(uri) {
        if (durationMs <= 0L) {
            val dur = VoiceMessageManager.getAudioDurationMs(context, uri)
            if (dur > 0L) durationMs = dur
        }
    }

    val displayDuration = if (isPlayingThis) {
        VoiceMessageManager.formatDuration(currentMs.toLong())
    } else {
        val ms = if (isLoadedThis && VoiceMessageManager.totalDurationMs.value > 0) {
            VoiceMessageManager.totalDurationMs.value.toLong()
        } else if (durationMs > 0L) {
            durationMs
        } else {
            0L
        }
        VoiceMessageManager.formatDuration(ms)
    }

    val activeColor = if (isOutgoing) Color.White else palette.accentPrimary
    val inactiveColor = if (isOutgoing) Color.White.copy(alpha = 0.35f) else palette.textSecondary.copy(alpha = 0.35f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isOutgoing) {
                    Color.White.copy(alpha = 0.12f)
                } else {
                    palette.glassSurfaceElevated.copy(alpha = 0.55f)
                }
            )
            .border(
                width = 1.dp,
                color = if (isOutgoing) Color.White.copy(alpha = 0.2f) else palette.glassBorderTop,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size((38f * fontScale).dp)
                .clip(CircleShape)
                .background(
                    if (isOutgoing) {
                        SolidColor(Color.White.copy(alpha = 0.25f))
                    } else {
                        Brush.linearGradient(
                            listOf(palette.accentPrimary, palette.accentSecondary)
                        )
                    }
                )
                .clickable {
                    VoiceMessageManager.togglePlayPause(context, uri)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlayingThis) TxturaIcons.Pause else TxturaIcons.Play,
                contentDescription = if (isPlayingThis) "Pauza" else "Pusti",
                tint = Color.White,
                modifier = Modifier.size((18f * fontScale).dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .pointerInput(uri) {
                    detectTapGestures { offset ->
                        val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        VoiceMessageManager.seekTo(uri, fraction)
                    }
                }
        ) {
            val barCount = 24
            val barHeights = remember(uri) {
                val seed = uri.toString().hashCode()
                val random = java.util.Random(seed.toLong())
                List(barCount) {
                    (4 + random.nextInt(18)).dp
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.5.dp)
            ) {
                barHeights.forEachIndexed { index, height ->
                    val barFraction = (index + 1).toFloat() / barCount.toFloat()
                    val isPassed = progress >= barFraction
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(height)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isPassed) activeColor else inactiveColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayDuration,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = (11.5f * fontScale).sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (isOutgoing) Color.White.copy(alpha = 0.9f) else palette.textPrimary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = TxturaIcons.Microphone,
                        contentDescription = null,
                        tint = if (isOutgoing) Color.White.copy(alpha = 0.7f) else palette.accentSecondary,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = strings.voiceMessageTitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = (10f * fontScale).sp
                        ),
                        color = if (isOutgoing) Color.White.copy(alpha = 0.7f) else palette.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun MmsCalendarEventCard(
    attachment: MmsAttachment,
    isOutgoing: Boolean,
    palette: GlassPalette,
    fontScale: Float
) {
    val context = LocalContext.current
    val view = LocalView.current
    val strings = TxturaStrings.current()
    val eventTitle = attachment.caption.ifBlank { strings.calendarEventTitle }
    val activeColor = if (isOutgoing) Color.White else palette.accentPrimary
    val subColor = if (isOutgoing) Color.White.copy(alpha = 0.75f) else palette.textSecondary

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isOutgoing) Color.White.copy(alpha = 0.14f)
                else palette.glassSurfaceElevated.copy(alpha = 0.65f)
            )
            .border(
                width = 1.dp,
                color = if (isOutgoing) Color.White.copy(alpha = 0.25f) else palette.glassBorderTop,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable {
                TxturaHaptics.click(view, context)
                DocumentAttachmentHelper.openAttachmentInExternalApp(
                    context = context,
                    uri = attachment.uri,
                    mimeType = "text/calendar",
                    fallbackName = attachment.caption
                )
            }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    if (isOutgoing) Color.White.copy(alpha = 0.20f)
                    else palette.accentPrimary.copy(alpha = 0.18f)
                )
        ) {
            Icon(
                imageVector = TxturaIcons.CalendarEvent,
                contentDescription = strings.calendarEventTitle,
                tint = activeColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = eventTitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = (14f * fontScale).sp,
                    fontWeight = FontWeight.Bold
                ),
                color = if (isOutgoing) Color.White else palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "vCalendar (.ics) • " + if (strings.isSerbian) "Klikni za otvaranje" else "Tap to open",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = (11f * fontScale).sp
                ),
                color = subColor
            )
        }

        Icon(
            imageVector = TxturaIcons.Forward,
            contentDescription = null,
            tint = subColor,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun MmsDocumentCard(
    attachment: MmsAttachment,
    isOutgoing: Boolean,
    palette: GlassPalette,
    fontScale: Float
) {
    val context = LocalContext.current
    val view = LocalView.current
    val strings = TxturaStrings.current()
    val docName = attachment.caption.ifBlank { strings.documentFileTitle }
    val ext = docName.substringAfterLast('.', "").uppercase().take(4)
    val extLabel = if (ext.isNotBlank()) "$ext fajl" else strings.documentFileTitle
    val activeColor = if (isOutgoing) Color.White else palette.accentSecondary
    val subColor = if (isOutgoing) Color.White.copy(alpha = 0.75f) else palette.textSecondary

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isOutgoing) Color.White.copy(alpha = 0.14f)
                else palette.glassSurfaceElevated.copy(alpha = 0.65f)
            )
            .border(
                width = 1.dp,
                color = if (isOutgoing) Color.White.copy(alpha = 0.25f) else palette.glassBorderTop,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable {
                TxturaHaptics.click(view, context)
                DocumentAttachmentHelper.openAttachmentInExternalApp(
                    context = context,
                    uri = attachment.uri,
                    mimeType = attachment.mimeType,
                    fallbackName = docName
                )
            }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    if (isOutgoing) Color.White.copy(alpha = 0.20f)
                    else palette.accentSecondary.copy(alpha = 0.18f)
                )
        ) {
            Icon(
                imageVector = TxturaIcons.DocumentFile,
                contentDescription = strings.documentFileTitle,
                tint = activeColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = docName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = (14f * fontScale).sp,
                    fontWeight = FontWeight.Bold
                ),
                color = if (isOutgoing) Color.White else palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$extLabel • " + if (strings.isSerbian) "Klikni za otvaranje" else "Tap to open",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = (11f * fontScale).sp
                ),
                color = subColor
            )
        }

        Icon(
            imageVector = TxturaIcons.Forward,
            contentDescription = null,
            tint = subColor,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun CalendarEventPickerDialog(
    onSelectEventUri: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalGlassPalette.current
    val strings = TxturaStrings.current()
    val isSr = strings.isSerbian

    val events = remember { CalendarEventManager.getDeviceCalendarEvents(context) }
    var searchQuery by remember { mutableStateOf("") }
    var quickInviteTitle by remember { mutableStateOf("") }

    val filteredEvents = remember(events, searchQuery) {
        if (searchQuery.isBlank()) events
        else events.filter { it.title.contains(searchQuery, ignoreCase = true) || it.location?.contains(searchQuery, ignoreCase = true) == true }
    }

    val dateFormat = remember { SimpleDateFormat("dd. MMM yyyy. HH:mm", Locale.getDefault()) }

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        accentBorderColor = palette.accentPrimary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = TxturaIcons.CalendarEvent,
                    contentDescription = null,
                    tint = palette.accentPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.calendarPickerTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (events.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.glassSurface.copy(alpha = 0.3f))
                            .border(1.dp, palette.glassBorderBottom.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = if (isSr) "Pretraži događaje..." else "Search events...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = palette.textPrimary),
                            cursorBrush = SolidColor(palette.accentPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredEvents) { event ->
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                elevated = false,
                                contentPadding = PaddingValues(10.dp),
                                onClick = {
                                    val (uri, _) = CalendarEventManager.createVCalendarFileUri(context, event)
                                    onSelectEventUri(uri)
                                }
                            ) {
                                Column {
                                    Text(
                                        text = event.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = dateFormat.format(Date(event.startMillis)) + if (!event.location.isNullOrBlank()) " • ${event.location}" else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = strings.noCalendarEventsFound,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }

                // Quick Invitation creation section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.glassSurfaceElevated.copy(alpha = 0.4f))
                        .border(1.dp, palette.accentSecondary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = strings.quickInviteOption,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.accentSecondary,
                        fontWeight = FontWeight.Bold
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.glassSurface.copy(alpha = 0.3f))
                            .border(1.dp, palette.glassBorderBottom.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        if (quickInviteTitle.isEmpty()) {
                            Text(
                                text = if (isSr) "Naziv: npr. Sastanak, Rođendan..." else "Title: e.g. Meeting, Birthday...",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                        BasicTextField(
                            value = quickInviteTitle,
                            onValueChange = { quickInviteTitle = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = palette.textPrimary),
                            cursorBrush = SolidColor(palette.accentSecondary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.accentSecondary)
                                .clickable {
                                    val title = quickInviteTitle.ifBlank { if (isSr) "Sastanak" else "Meeting" }
                                    val (uri, _) = CalendarEventManager.createQuickInvitationUri(
                                        context = context,
                                        title = title,
                                        startMillis = System.currentTimeMillis() + 3600_000L
                                    )
                                    onSelectEventUri(uri)
                                }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = if (isSr) "Kreiraj .ics" else "Create .ics",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = strings.cancel,
                    color = palette.textSecondary
                )
            }
        }
    )
}

@Composable
private fun MmsAttachmentCard(
    uri: Uri,
    caption: String,
    hdrLabel: String,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val palette = LocalGlassPalette.current
    val handleClick = onClick ?: { SmsMmsRepository.openImageInGallery(context, uri) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        palette.accentPrimary.copy(alpha = 0.28f),
                        palette.accentSecondary.copy(alpha = 0.22f)
                    )
                )
            )
            .border(1.dp, palette.glassBorderTop, RoundedCornerShape(14.dp))
            .clickable(onClick = handleClick)
            .padding(10.dp)
    ) {
        Box {
            MmsImageThumbnail(
                uri = uri,
                hdrLabel = hdrLabel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = TxturaIcons.EyePreview,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = TxturaIcons.AttachImage,
                contentDescription = null,
                tint = palette.accentSecondary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = caption,
                style = MaterialTheme.typography.labelMedium,
                color = palette.textPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun isGifUri(context: Context, uri: Uri): Boolean {
    val mime = try { context.contentResolver.getType(uri) } catch (_: Exception) { null }
    if (mime?.equals("image/gif", ignoreCase = true) == true) return true
    val path = uri.path?.lowercase()
    if (path?.endsWith(".gif") == true) return true
    val uriStr = uri.toString().lowercase()
    return uriStr.endsWith(".gif") || uriStr.contains("image%2fgif") || uriStr.contains("image/gif") || runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val header = ByteArray(6)
            val read = stream.read(header)
            read >= 3 && header[0] == 'G'.code.toByte() && header[1] == 'I'.code.toByte() && header[2] == 'F'.code.toByte()
        } ?: false
    }.getOrDefault(false)
}

@Composable
private fun MmsImageThumbnail(
    uri: Uri,
    hdrLabel: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isGif by produceState(initialValue = false, key1 = uri) {
        value = withContext(Dispatchers.IO) {
            isGifUri(context, uri)
        }
    }

    val gifDrawable by produceState<android.graphics.drawable.Drawable?>(initialValue = null, key1 = uri, key2 = isGif) {
        if (isGif) {
            value = withContext(Dispatchers.IO) {
                try {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeDrawable(source).also { d ->
                        if (d is AnimatedImageDrawable) {
                            d.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                        }
                    }
                } catch (_: Exception) {
                    null
                }
            }
        } else {
            value = null
        }
    }

    if (isGif) {
        Box(modifier = modifier) {
            AndroidView(
                factory = { ctx ->
                    ImageView(ctx).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                },
                update = { imageView ->
                    val d = gifDrawable
                    imageView.setImageDrawable(d)
                    (d as? android.graphics.drawable.Animatable)?.start()
                },
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "GIF",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }
    } else {
        val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, key1 = uri) {
            value = withContext(Dispatchers.IO) {
                if (uri.isDemoUri()) {
                    null
                } else {
                    try {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                            BitmapFactory.decodeStream(stream, null, opts)
                        }
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        }

        val loadedBitmap = bitmap
        if (loadedBitmap != null) {
            Image(
                bitmap = loadedBitmap.asImageBitmap(),
                contentDescription = hdrLabel,
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = modifier.background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0284C7).copy(alpha = 0.55f),
                            Color(0xFF6366F1).copy(alpha = 0.55f),
                            Color(0xFFA855F7).copy(alpha = 0.55f)
                        )
                    )
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = TxturaIcons.AttachImage,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = hdrLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun Uri.isDemoUri(): Boolean {
    return authority == "com.txtura.demo"
}

private fun decodeSampledBitmap(
    context: Context,
    uri: Uri,
    requestedWidth: Int,
    requestedHeight: Int
): android.graphics.Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (bounds.outWidth / (sampleSize * 2) >= requestedWidth &&
        bounds.outHeight / (sampleSize * 2) >= requestedHeight
    ) {
        sampleSize *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    }
}

@Composable
private fun ContactAvatarView(
    thread: ConversationThread,
    size: Dp = 48.dp,
    onClick: () -> Unit
) {
    val palette = LocalGlassPalette.current
    val context = LocalContext.current
    val photoUri = thread.contactPhotoUri
    val avatarColor = Color(thread.avatarAccentColorHex)

    val targetBitmapSizePx = with(LocalDensity.current) { size.roundToPx().coerceAtLeast(1) }
    val contactBitmap by produceState<android.graphics.Bitmap?>(initialValue = null, key1 = photoUri, key2 = targetBitmapSizePx) {
        value = if (photoUri == null) null else withContext(Dispatchers.IO) {
            try {
                decodeSampledBitmap(context, photoUri, targetBitmapSizePx * 2, targetBitmapSizePx * 2)
            } catch (_: Exception) {
                null
            }
        }
    }

    val loadedBitmap = contactBitmap
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        avatarColor.copy(alpha = 0.35f),
                        avatarColor.copy(alpha = 0.10f)
                    )
                )
            )
            .border(1.dp, avatarColor.copy(alpha = 0.65f), CircleShape)
            .clickable(onClick = onClick)
    ) {
        if (loadedBitmap != null) {
            Image(
                bitmap = loadedBitmap.asImageBitmap(),
                contentDescription = thread.contactName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            Text(
                text = thread.avatarInitials,
                style = if (size < 42.dp) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
                color = if (palette.isDark) Color.White else palette.textPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * When the user taps "Nova poruka", immediately prompts them to select a contact/sender
 * (or enter a phone number). Selecting a contact immediately opens the existing chat
 * with that person (or creates a new chat if none exists yet).
 */
@Composable
fun NewMessageDialog(
    title: String? = null,
    initialRecipient: String = "",
    availableContacts: List<ContactPickerItem> = emptyList(),
    inAppSoundsEnabled: Boolean = true,
    onPickContact: () -> Unit,
    onDismiss: () -> Unit,
    onSelectRecipient: (recipientAddress: String, displayName: String?) -> Unit,
    onSendBroadcastMessage: ((recipients: List<ContactPickerItem>, messageBody: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    val palette = LocalGlassPalette.current
    val strings = TxturaStrings.current()

    var queryOrNumber by remember(initialRecipient) { mutableStateOf(initialRecipient) }
    var messageBody by remember { mutableStateOf("") }
    val selectedRecipients = remember { mutableStateListOf<ContactPickerItem>() }

    val trimmedQuery = queryOrNumber.trim()

    val filteredContacts = remember(availableContacts, trimmedQuery) {
        if (trimmedQuery.isBlank()) {
            availableContacts
        } else {
            val q = trimmedQuery.lowercase()
            val qDigits = q.filter { it.isDigit() || it == '+' }
            availableContacts.asSequence()
                .map { contact ->
                    val nameLower = contact.name.lowercase()
                    val addrDigits = contact.address.filter { it.isDigit() || it == '+' }

                    val score = when {
                        nameLower == q -> 0
                        nameLower.startsWith(q) -> 1
                        nameLower.split(" ", "-", "_", ".").any { it.startsWith(q) } -> 2
                        nameLower.contains(q) -> 3
                        qDigits.isNotEmpty() && addrDigits == qDigits -> 4
                        qDigits.isNotEmpty() && addrDigits.startsWith(qDigits) -> 5
                        contact.address.contains(q, ignoreCase = true) || (qDigits.isNotEmpty() && addrDigits.contains(qDigits)) -> 6
                        else -> null
                    }
                    contact to score
                }
                .filter { it.second != null }
                .sortedWith(
                    compareBy<Pair<ContactPickerItem, Int?>> { it.second }
                        .thenBy { it.first.name.lowercase() }
                )
                .map { it.first }
                .toList()
        }
    }

    val isExactMatchInContacts = availableContacts.any {
        it.address.equals(trimmedQuery, ignoreCase = true) || it.name.equals(trimmedQuery, ignoreCase = true)
    }

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        accentBorderColor = palette.accentPrimary,
        title = {
            Column {
                Text(
                    text = title ?: strings.newMessage,
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                if (selectedRecipients.size > 1) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = strings.multiRecipientCountBadge(selectedRecipients.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.accentPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. Sleek "Za:" / "To:" bar with full visibility of recipient names (FlowRow) and input
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            palette.glassSurfaceElevated.copy(
                                alpha = (palette.glassSurfaceElevated.alpha * 0.95f).coerceIn(0.25f, 0.90f)
                            )
                        )
                        .border(
                            width = 1.2.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    palette.glassBorderTop.copy(alpha = 0.55f),
                                    palette.glassBorderBottom.copy(alpha = 0.25f)
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                    ) {
                        if (selectedRecipients.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = strings.toRecipientPrefix,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.accentPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(palette.accentPrimary.copy(alpha = 0.16f))
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = selectedRecipients.size.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.accentPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                if (selectedRecipients.size > 1) {
                                    Text(
                                        text = if (strings.isSerbian) "Ukloni sve" else "Clear all",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                TxturaHaptics.tick(view, context)
                                                selectedRecipients.clear()
                                            }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Wrapping container so every selected recipient name is clearly visible
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 140.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    selectedRecipients.forEach { rec ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(palette.accentPrimary.copy(alpha = 0.20f))
                                                .border(
                                                    0.8.dp,
                                                    palette.accentPrimary.copy(alpha = 0.65f),
                                                    RoundedCornerShape(50)
                                                )
                                                .clickable {
                                                    TxturaHaptics.tick(view, context)
                                                    selectedRecipients.remove(rec)
                                                }
                                                .padding(horizontal = 9.dp, vertical = 5.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = rec.name,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = palette.textPrimary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Icon(
                                                    imageVector = TxturaIcons.Close,
                                                    contentDescription = strings.cancel,
                                                    tint = palette.textSecondary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Input field for recipient name or phone number (with no cramped contacts icon)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (selectedRecipients.isEmpty()) {
                                Text(
                                    text = strings.toRecipientPrefix,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = palette.textSecondary,
                                    fontWeight = FontWeight.Normal
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                if (queryOrNumber.isEmpty()) {
                                    Text(
                                        text = if (selectedRecipients.isEmpty()) strings.enterNameOrPhoneNumber else (if (strings.isSerbian) "Dodaj još..." else "Add more..."),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.textSecondary.copy(alpha = 0.50f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                BasicTextField(
                                    value = queryOrNumber,
                                    onValueChange = { queryOrNumber = it },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = palette.textPrimary),
                                    cursorBrush = SolidColor(palette.accentPrimary),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (queryOrNumber.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = TxturaIcons.Close,
                                    contentDescription = strings.cancel,
                                    tint = palette.textSecondary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .clickable { queryOrNumber = "" }
                                )
                            }
                        }
                    }
                }

                // 2. Direct Contacts / Suggestions List
                if (availableContacts.isNotEmpty() || trimmedQuery.isNotBlank()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Header with match count + contact picker + Select All / Deselect All
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (trimmedQuery.isNotBlank()) {
                                        "${filteredContacts.size} ${if (strings.isSerbian) "predloga" else "suggestions"}"
                                    } else {
                                        "${availableContacts.size} ${if (strings.isSerbian) "kontakata" else "contacts"}"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                GlassIconButton(
                                    icon = TxturaIcons.ContactPerson,
                                    contentDescription = strings.pickFromContacts,
                                    tint = palette.accentPrimary,
                                    onClick = {
                                        TxturaHaptics.tick(view, context)
                                        onPickContact()
                                    },
                                    size = 26.dp
                                )
                            }
                            if (filteredContacts.isNotEmpty()) {
                                val allFiltered = filteredContacts.all { c -> selectedRecipients.any { it.address == c.address } }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            TxturaHaptics.tick(view, context)
                                            if (allFiltered) {
                                                filteredContacts.forEach { fc ->
                                                    selectedRecipients.removeAll { it.address == fc.address }
                                                }
                                            } else {
                                                filteredContacts.forEach { fc ->
                                                    if (selectedRecipients.none { it.address == fc.address }) {
                                                        selectedRecipients.add(fc)
                                                    }
                                                }
                                                queryOrNumber = ""
                                            }
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (allFiltered) TxturaIcons.DeselectAll else TxturaIcons.SelectAll,
                                        contentDescription = null,
                                        tint = palette.accentPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (allFiltered) strings.deselectAllContactsLabel else strings.selectAllContactsLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.accentPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Suggestions / Contacts List
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp, max = 220.dp)
                        ) {
                            // Unmemorized / typed number suggestion
                            if (trimmedQuery.isNotBlank() && !isExactMatchInContacts) {
                                item(key = "typed_custom_recipient") {
                                    val isSelected = selectedRecipients.any { it.address == trimmedQuery }
                                    val isNumber = trimmedQuery.any { it.isDigit() }
                                    val selectCustom: () -> Unit = {
                                        TxturaHaptics.tick(view, context)
                                        if (isSelected) {
                                            selectedRecipients.removeAll { it.address == trimmedQuery }
                                        } else {
                                            selectedRecipients.add(
                                                ContactPickerItem(
                                                    name = trimmedQuery,
                                                    address = trimmedQuery,
                                                    initials = if (isNumber) "#" else trimmedQuery.take(2).uppercase()
                                                )
                                            )
                                            queryOrNumber = ""
                                        }
                                    }

                                    GlassCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        elevated = isSelected,
                                        accentHighlight = if (isSelected) palette.accentPrimary else null,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                        onClick = selectCustom
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            GlassSelectionCheckbox(
                                                checked = isSelected,
                                                onToggle = selectCustom
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelected) palette.accentPrimary.copy(alpha = 0.30f)
                                                        else palette.accentPrimary.copy(alpha = 0.15f)
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (isSelected) palette.accentPrimary else palette.accentPrimary.copy(alpha = 0.38f),
                                                        CircleShape
                                                    )
                                            ) {
                                                Icon(
                                                    imageVector = if (isNumber) TxturaIcons.PhoneCall else TxturaIcons.ContactPerson,
                                                    contentDescription = null,
                                                    tint = palette.accentPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = trimmedQuery,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = palette.textPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = if (isNumber) strings.addTypedNumberToRecipients(trimmedQuery) else strings.newChatBadge,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = palette.accentPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            items(
                                items = filteredContacts,
                                key = { "${it.name}_${it.address}" }
                            ) { item ->
                                val isSelected = selectedRecipients.any { it.address == item.address }
                                val toggleItem: () -> Unit = {
                                    TxturaHaptics.tick(view, context)
                                    if (isSelected) {
                                        selectedRecipients.removeAll { it.address == item.address }
                                    } else {
                                        selectedRecipients.add(item)
                                        queryOrNumber = ""
                                    }
                                }

                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    elevated = isSelected,
                                    accentHighlight = if (isSelected) palette.accentPrimary else null,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    onClick = toggleItem
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        GlassSelectionCheckbox(
                                            checked = isSelected,
                                            onToggle = toggleItem
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) palette.accentPrimary.copy(alpha = 0.30f)
                                                    else palette.accentPrimary.copy(alpha = 0.15f)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isSelected) palette.accentPrimary else palette.accentPrimary.copy(alpha = 0.38f),
                                                    CircleShape
                                                )
                                        ) {
                                            Text(
                                                text = item.initials,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.textPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            OverflowTooltipText(
                                                text = item.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = palette.textPrimary,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = item.address,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.textSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Message Text Input Field
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    elevated = true,
                    contentPadding = PaddingValues(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (messageBody.isEmpty()) {
                                Text(
                                    text = if (selectedRecipients.size > 1) {
                                        strings.broadcastMessageTextPlaceholder
                                    } else {
                                        strings.writeSmsPlaceholder
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary.copy(alpha = 0.50f)
                                )
                            }
                            BasicTextField(
                                value = messageBody,
                                onValueChange = { messageBody = it },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = palette.textPrimary),
                                cursorBrush = SolidColor(palette.accentPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp, max = 88.dp)
                            )
                        }
                        if (messageBody.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val smsCount = ((messageBody.length - 1) / 160) + 1
                                Text(
                                    text = "${messageBody.length} • $smsCount SMS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                    }
                }

                // 4. Multi-recipient notice if 2 or more selected
                if (selectedRecipients.size > 1) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.accentPrimary.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = TxturaIcons.ShieldCheck,
                            contentDescription = null,
                            tint = palette.accentPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.broadcastSeparateNotice,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.accentPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            val hasMultiple = selectedRecipients.size > 1
            val hasSingle = selectedRecipients.size == 1
            val hasQueryOnly = selectedRecipients.isEmpty() && trimmedQuery.isNotBlank()
            val canProceed = when {
                hasMultiple -> messageBody.isNotBlank()
                hasSingle -> true
                hasQueryOnly -> true
                else -> false
            }

            TextButton(
                enabled = canProceed,
                onClick = {
                    TxturaHaptics.tick(view, context)
                    when {
                        hasMultiple -> {
                            TxturaHaptics.confirm(view, context)
                            onSendBroadcastMessage?.invoke(selectedRecipients.toList(), messageBody)
                            onDismiss()
                        }
                        hasSingle -> {
                            val singleRecipient = selectedRecipients.first()
                            if (messageBody.isNotBlank()) {
                                onSendBroadcastMessage?.invoke(listOf(singleRecipient), messageBody)
                            } else {
                                onSelectRecipient(singleRecipient.address, singleRecipient.name)
                            }
                            onDismiss()
                        }
                        hasQueryOnly -> {
                            val matched = filteredContacts.firstOrNull {
                                it.name.equals(trimmedQuery, ignoreCase = true) || it.address.equals(trimmedQuery, ignoreCase = true)
                            } ?: if (filteredContacts.size == 1) filteredContacts.first() else null

                            val address = matched?.address ?: trimmedQuery
                            val name = matched?.name
                            if (messageBody.isNotBlank()) {
                                onSendBroadcastMessage?.invoke(
                                    listOf(ContactPickerItem(name = name ?: address, address = address, initials = "#")),
                                    messageBody
                                )
                            } else {
                                onSelectRecipient(address, name)
                            }
                            onDismiss()
                        }
                    }
                }
            ) {
                val buttonText = when {
                    hasMultiple -> strings.sendToAllButton(selectedRecipients.size)
                    messageBody.isNotBlank() -> strings.send
                    else -> strings.next
                }
                Text(
                    text = buttonText,
                    color = if (canProceed) palette.accentPrimary else palette.textSecondary.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (inAppSoundsEnabled) {
                        SmsMmsRepository.playInAppMessageCancelledSound(context)
                    }
                    onDismiss()
                }
            ) {
                Text(strings.cancel, color = palette.textSecondary)
            }
        }
    )
}

@Composable
fun ShareContactDialog(
    availableContacts: List<ContactPickerItem>,
    onPickFromSystemContacts: () -> Unit,
    onSelectContact: (ContactPickerItem) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalGlassPalette.current
    val strings = TxturaStrings.current()
    var searchQuery by remember { mutableStateOf("") }

    val filteredContacts = remember(availableContacts, searchQuery) {
        if (searchQuery.isBlank()) {
            availableContacts
        } else {
            availableContacts.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                    it.address.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        accentBorderColor = palette.accentPrimary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = TxturaIcons.ContactPerson,
                    contentDescription = null,
                    tint = palette.accentPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.shareContactDialogTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Search bar + Pick from device contacts button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.glassSurface.copy(alpha = (palette.glassSurface.alpha * 0.45f).coerceIn(0.06f, 0.35f)))
                        .border(1.dp, palette.glassBorderBottom.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Box {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = strings.searchOrEnterNumberPlaceholder,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.textSecondary
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.textPrimary),
                                    cursorBrush = SolidColor(palette.accentPrimary),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        GlassIconButton(
                            icon = TxturaIcons.ContactPerson,
                            contentDescription = strings.pickFromPhonebookButton,
                            tint = palette.accentPrimary,
                            onClick = onPickFromSystemContacts,
                            size = 38.dp
                        )
                    }
                }

                // Pick from system phonebook action card
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevated = true,
                    accentHighlight = palette.accentPrimary,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    onClick = onPickFromSystemContacts
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = TxturaIcons.ContactPerson,
                            contentDescription = null,
                            tint = palette.accentPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = strings.pickFromPhonebookButton,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.accentPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Contact list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    items(filteredContacts, key = { "${it.name}_${it.address}" }) { contact ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            onClick = { onSelectContact(contact) }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(palette.accentPrimary.copy(alpha = 0.2f))
                                        .border(1.dp, palette.accentPrimary.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Text(
                                        text = contact.initials,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = contact.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = contact.address,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel, color = palette.textSecondary)
            }
        }
    )
}

@Composable
fun MultiRecipientBroadcastDialog(
    initialMessageBody: String,
    availableContacts: List<ContactPickerItem>,
    onDismiss: () -> Unit,
    onSendBroadcast: (recipients: List<ContactPickerItem>, messageBody: String) -> Unit
) {
    val palette = LocalGlassPalette.current
    val strings = TxturaStrings.current()
    var messageBody by remember(initialMessageBody) { mutableStateOf(initialMessageBody) }
    val selectedRecipients = remember { mutableStateListOf<ContactPickerItem>() }
    var searchQuery by remember { mutableStateOf("") }

    val filteredContacts = remember(availableContacts, searchQuery) {
        if (searchQuery.isBlank()) {
            availableContacts
        } else {
            availableContacts.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                    it.address.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        accentBorderColor = palette.accentPrimary,
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = TxturaIcons.Forward,
                        contentDescription = null,
                        tint = palette.accentPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.sendToOtherContactsOption,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.broadcastSeparateNotice,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.accentSecondary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Message body preview / edit box
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(10.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (messageBody.isEmpty()) {
                            Text(
                                text = strings.writeSmsPlaceholder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary.copy(alpha = 0.50f)
                            )
                        }
                        BasicTextField(
                            value = messageBody,
                            onValueChange = { messageBody = it },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = palette.textPrimary),
                            cursorBrush = SolidColor(palette.accentPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 40.dp, max = 100.dp)
                        )
                    }
                }

                // Selected recipients chips
                if (selectedRecipients.isNotEmpty()) {
                    Text(
                        text = strings.multiRecipientBadge(selectedRecipients.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.accentPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(selectedRecipients, key = { it.address }) { rec ->
                            GlassCard(
                                shape = RoundedCornerShape(50),
                                elevated = true,
                                accentHighlight = palette.accentPrimary,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                onClick = { selectedRecipients.remove(rec) }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = rec.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = TxturaIcons.Close,
                                        contentDescription = null,
                                        tint = palette.textSecondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Search contacts
                GlassCard(
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(10.dp)
                ) {
                    Box {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = strings.searchOrEnterNumberPlaceholder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = palette.textPrimary),
                            cursorBrush = SolidColor(palette.accentPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // If user typed a number in broadcast search, offer "+ Dodaj broj"
                val trimmedSearch = searchQuery.trim()
                if (trimmedSearch.isNotBlank() && trimmedSearch.any { it.isDigit() }) {
                    val alreadyIn = selectedRecipients.any { it.address == trimmedSearch }
                    if (!alreadyIn) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            elevated = true,
                            accentHighlight = palette.accentPrimary,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            onClick = {
                                selectedRecipients.add(
                                    ContactPickerItem(
                                        name = trimmedSearch,
                                        address = trimmedSearch,
                                        initials = "#"
                                    )
                                )
                                searchQuery = ""
                            }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = TxturaIcons.Plus,
                                    contentDescription = null,
                                    tint = palette.accentPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.addTypedNumberToRecipients(trimmedSearch),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.accentPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Select all / deselect all
                if (filteredContacts.isNotEmpty()) {
                    val allFiltered = filteredContacts.all { c -> selectedRecipients.any { it.address == c.address } }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${filteredContacts.size} ${if (searchQuery.isNotBlank()) "pronađenih" else "kontakata"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (allFiltered) {
                                        filteredContacts.forEach { fc ->
                                            selectedRecipients.removeAll { it.address == fc.address }
                                        }
                                    } else {
                                        filteredContacts.forEach { fc ->
                                            if (selectedRecipients.none { it.address == fc.address }) {
                                                selectedRecipients.add(fc)
                                            }
                                        }
                                        searchQuery = ""
                                    }
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (allFiltered) TxturaIcons.DeselectAll else TxturaIcons.SelectAll,
                                contentDescription = null,
                                tint = palette.accentPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (allFiltered) strings.deselectAllContactsLabel else strings.selectAllContactsLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.accentPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Contacts list with toggle selection
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                ) {
                    items(filteredContacts, key = { "${it.name}_${it.address}" }) { contact ->
                        val isSelected = selectedRecipients.any { it.address == contact.address }
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            elevated = isSelected,
                            accentHighlight = if (isSelected) palette.accentPrimary else null,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            onClick = {
                                if (isSelected) {
                                    selectedRecipients.removeAll { it.address == contact.address }
                                } else {
                                    selectedRecipients.add(contact)
                                    searchQuery = ""
                                }
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    GlassSelectionCheckbox(
                                        checked = isSelected,
                                        onToggle = {
                                            if (isSelected) {
                                                selectedRecipients.removeAll { it.address == contact.address }
                                            } else {
                                                selectedRecipients.add(contact)
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) palette.accentPrimary.copy(alpha = 0.3f)
                                                else palette.accentPrimary.copy(alpha = 0.15f)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) palette.accentPrimary else palette.accentPrimary.copy(alpha = 0.4f),
                                                CircleShape
                                            )
                                    ) {
                                        Text(
                                            text = contact.initials,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.textPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = contact.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = palette.textPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = contact.address,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.textSecondary
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = TxturaIcons.CheckDouble,
                                        contentDescription = null,
                                        tint = palette.accentPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedRecipients.isNotEmpty() && messageBody.isNotBlank()) {
                TextButton(
                    onClick = {
                        onSendBroadcast(selectedRecipients.toList(), messageBody.trim())
                        onDismiss()
                    }
                ) {
                    Text(
                        text = strings.sendToAllButton(selectedRecipients.size),
                        color = palette.accentPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel, color = palette.textSecondary)
            }
        }
    )
}

@Composable
fun DetectedEntityActionDialog(
    value: String,
    isPhone: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalGlassPalette.current
    val strings = TxturaStrings.current()

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        accentBorderColor = palette.accentPrimary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isPhone) TxturaIcons.PhoneCall else TxturaIcons.Mail,
                    contentDescription = null,
                    tint = palette.accentPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isPhone) strings.detectedPhoneNumberLabel else strings.detectedEmailLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // 1. Save to contacts (ACTION_INSERT_OR_EDIT)
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevated = true,
                    accentHighlight = palette.accentPrimary,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_INSERT_OR_EDIT).apply {
                                type = ContactsContract.Contacts.CONTENT_ITEM_TYPE
                                if (isPhone) {
                                    putExtra(ContactsContract.Intents.Insert.PHONE, value)
                                } else {
                                    putExtra(ContactsContract.Intents.Insert.EMAIL, value)
                                }
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(context, strings.contactsErrorToast, Toast.LENGTH_SHORT).show()
                        }
                        onDismiss()
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = TxturaIcons.ContactPerson,
                            contentDescription = null,
                            tint = palette.accentPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = strings.saveToContactsAction,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // 2. Call or Send Email
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevated = true,
                    accentHighlight = palette.safeColor,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    onClick = {
                        try {
                            val intent = if (isPhone) {
                                Intent(Intent.ACTION_DIAL, "tel:$value".toUri())
                            } else {
                                Intent(Intent.ACTION_SENDTO, "mailto:$value".toUri())
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            // ignore
                        }
                        onDismiss()
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isPhone) TxturaIcons.PhoneCall else TxturaIcons.Mail,
                            contentDescription = null,
                            tint = palette.safeColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isPhone) strings.callNumberAction else strings.sendEmailAction,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // 3. Copy to clipboard
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevated = true,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = ClipData.newPlainText(if (isPhone) "phone" else "email", value)
                        clipboard?.setPrimaryClip(clip)
                        Toast.makeText(
                            context,
                            if (isPhone) strings.phoneSavedToClipboardToast else strings.emailSavedToClipboardToast,
                            Toast.LENGTH_SHORT
                        ).show()
                        onDismiss()
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = TxturaIcons.CopyKey,
                            contentDescription = null,
                            tint = palette.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isPhone) strings.copyPhoneNumberAction else strings.copyEmailAction,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel, color = palette.textSecondary)
            }
        }
    )
}

@Composable
fun DefaultSmsRolePromptDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalGlassPalette.current
    val strings = TxturaStrings.current()

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        accentBorderColor = palette.accentPrimary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = TxturaIcons.ShieldCheck,
                    contentDescription = null,
                    tint = palette.accentPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.defaultSmsPromptTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )
            }
        },
        text = {
            Text(
                text = strings.defaultSmsPromptMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = palette.textSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = strings.defaultSmsPromptConfirm,
                    color = palette.accentPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = strings.defaultSmsPromptLater,
                    color = palette.textSecondary
                )
            }
        }
    )
}

@Composable
private fun SecurityItemRow(
    title: String,
    subtitle: String,
    isActive: Boolean,
    statusOverride: String? = null,
    isConfigurable: Boolean = true,
    onClick: () -> Unit
) {
    val palette = LocalGlassPalette.current
    val strings = TxturaStrings.current()
    val isPending = statusOverride == strings.rootScanInProgress
    val itemColor = when {
        isPending -> palette.textSecondary
        isActive -> palette.safeColor
        else -> palette.cautionColor
    }
    val iconChar = when {
        isPending -> "…"
        isActive -> "✓"
        else -> "✕"
    }
    val baseStatusText = statusOverride ?: if (isActive) strings.securityStatusEnabled else strings.securityStatusDisabled
    val statusText = if (isConfigurable) {
        "$baseStatusText • ${strings.securityStatusTapToConfigure}"
    } else {
        strings.securityStatusConfigured
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        accentHighlight = if (!isActive) palette.cautionColor else null,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = iconChar,
                    style = MaterialTheme.typography.titleMedium,
                    color = itemColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = itemColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isActive || isPending) palette.textSecondary else palette.cautionColor.copy(alpha = 0.85f),
                        fontSize = 11.sp
                    )
                }
            }
            if (isConfigurable) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "→",
                    style = MaterialTheme.typography.titleMedium,
                    color = itemColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SwipeableConversationCard(
    thread: ConversationThread,
    strings: AppStrings,
    swipeRightAction: SwipeAction,
    swipeLeftAction: SwipeAction,
    thresholdPx: Float,
    maxDragPx: Float,
    onExecuteSwipeAction: (SwipeAction) -> Unit,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    var hasSnappedThreshold by remember { mutableStateOf(false) }

    val currentOffset = offsetX.value
    val activeAction = when {
        currentOffset > 12f -> swipeRightAction
        currentOffset < -12f -> swipeLeftAction
        else -> SwipeAction.NONE
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(thread.id, swipeRightAction, swipeLeftAction) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        TxturaHaptics.tick(view, context)
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val proposed = offsetX.value + dragAmount
                        val clamped = when {
                            proposed > 0f && swipeRightAction == SwipeAction.NONE -> proposed * 0.15f
                            proposed < 0f && swipeLeftAction == SwipeAction.NONE -> proposed * 0.15f
                            else -> proposed.coerceIn(-maxDragPx, maxDragPx)
                        }
                        val isTriggered = abs(clamped) >= thresholdPx && activeAction != SwipeAction.NONE
                        if (isTriggered && !hasSnappedThreshold) {
                            hasSnappedThreshold = true
                            TxturaHaptics.swipeSnap(view, context)
                        } else if (!isTriggered && hasSnappedThreshold) {
                            hasSnappedThreshold = false
                            TxturaHaptics.tick(view, context)
                        }
                        scope.launch {
                            offsetX.snapTo(clamped)
                        }
                    },
                    onDragEnd = {
                        hasSnappedThreshold = false
                        val finalOffset = offsetX.value
                        when {
                            finalOffset >= thresholdPx && swipeRightAction != SwipeAction.NONE -> {
                                if (swipeRightAction == SwipeAction.DELETE) {
                                    TxturaHaptics.reject(view, context)
                                } else {
                                    TxturaHaptics.confirm(view, context)
                                }
                                onExecuteSwipeAction(swipeRightAction)
                            }
                            finalOffset <= -thresholdPx && swipeLeftAction != SwipeAction.NONE -> {
                                if (swipeLeftAction == SwipeAction.DELETE) {
                                    TxturaHaptics.reject(view, context)
                                } else {
                                    TxturaHaptics.confirm(view, context)
                                }
                                onExecuteSwipeAction(swipeLeftAction)
                            }
                        }
                        scope.launch {
                            offsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = TxturaMotion.swipeSnapBackSpec()
                            )
                        }
                    },
                    onDragCancel = {
                        hasSnappedThreshold = false
                        scope.launch {
                            offsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = TxturaMotion.swipeSnapBackSpec()
                            )
                        }
                    }
                )
            }
    ) {
        if (abs(currentOffset) > 10f && activeAction != SwipeAction.NONE) {
            val isUnread = thread.unreadCount > 0
            val actionColor = swipeActionColor(activeAction, isUnread = isUnread)
            val isTriggered = abs(currentOffset) >= thresholdPx
            val isSwipingRight = currentOffset > 0f

            Box(
                contentAlignment = if (isSwipingRight) Alignment.CenterStart else Alignment.CenterEnd,
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        actionColor.copy(alpha = if (isTriggered) 0.28f else 0.14f)
                    )
                    .border(
                        width = 1.dp,
                        color = actionColor.copy(alpha = if (isTriggered) 0.75f else 0.35f),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .padding(horizontal = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = swipeActionIcon(activeAction, isPinned = thread.isPinned, isUnread = isUnread),
                        contentDescription = null,
                        tint = actionColor,
                        modifier = Modifier.size(if (isTriggered) 22.dp else 18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val activeActionText = when (activeAction) {
                        SwipeAction.PIN -> {
                            if (thread.isPinned) strings.swipeGestureUnpin else strings.swipeGesturePin
                        }
                        SwipeAction.MARK_READ -> {
                            if (isUnread) strings.swipeGestureMarkRead else strings.swipeGestureMarkUnread
                        }
                        else -> swipeActionLabel(activeAction, strings)
                    }
                    Text(
                        text = activeActionText,
                        style = MaterialTheme.typography.labelLarge,
                        color = actionColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Box(
            modifier = Modifier.offset { IntOffset(offsetX.value.roundToInt(), 0) }
        ) {
            content()
        }
    }
}

private fun swipeActionLabel(action: SwipeAction, strings: AppStrings): String {
    return when (action) {
        SwipeAction.PIN -> strings.swipeActionPin
        SwipeAction.DELETE -> strings.swipeActionDelete
        SwipeAction.CALL -> strings.swipeActionCall
        SwipeAction.MARK_READ -> strings.swipeActionMarkRead
        SwipeAction.NONE -> strings.swipeActionNone
    }
}

private fun swipeActionIcon(
    action: SwipeAction,
    isPinned: Boolean = false,
    isUnread: Boolean = true,
): ImageVector {
    return when (action) {
        SwipeAction.PIN -> if (isPinned) TxturaIcons.PinFilled else TxturaIcons.PinOutline
        SwipeAction.DELETE -> TxturaIcons.Delete
        SwipeAction.CALL -> TxturaIcons.PhoneCall
        SwipeAction.MARK_READ -> if (isUnread) TxturaIcons.CheckDouble else TxturaIcons.Mail
        SwipeAction.NONE -> TxturaIcons.Close
    }
}

@Composable
private fun swipeActionColor(action: SwipeAction, isUnread: Boolean = true): Color {
    val palette = LocalGlassPalette.current
    return when (action) {
        SwipeAction.PIN -> palette.accentPrimary
        SwipeAction.DELETE -> palette.dangerColor
        SwipeAction.CALL -> palette.safeColor
        SwipeAction.MARK_READ -> if (isUnread) palette.safeColor else palette.accentSecondary
        SwipeAction.NONE -> palette.textSecondary
    }
}

@Composable
fun ConfirmMoveToTrashDialog(
    contactName: String,
    retentionDays: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalGlassPalette.current
    val strings = TxturaStrings.current()

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        accentBorderColor = palette.cautionColor,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = TxturaIcons.Delete,
                    contentDescription = null,
                    tint = palette.cautionColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.confirmDeleteTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )
            }
        },
        text = {
            Text(
                text = strings.confirmDeleteMessage(contactName, retentionDays),
                style = MaterialTheme.typography.bodyLarge,
                color = palette.textSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = strings.confirmMoveToTrashButton,
                    color = palette.dangerColor,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = strings.cancel,
                    color = palette.textSecondary
                )
            }
        }
    )
}

@Composable
fun ConfirmRestoreDialog(
    payload: BackupRestorePayload,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalGlassPalette.current
    val strings = TxturaStrings.current()
    val dateStr = formatTimestamp(payload.createdAtMillis)

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        accentBorderColor = palette.accentPrimary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = TxturaIcons.CloudDownload,
                    contentDescription = null,
                    tint = palette.accentPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.confirmRestoreTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strings.confirmRestoreMessage(
                        dateStr,
                        payload.threads.size,
                        payload.totalMessagesCount
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.textSecondary
                )
                Text(
                    text = "• ${payload.deviceModel} • v${payload.version}",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = strings.confirmRestoreButton,
                    color = palette.accentPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = strings.cancel,
                    color = palette.textSecondary
                )
            }
        }
    )
}

@Composable
fun CreateEncryptedBackupDialog(
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalGlassPalette.current
    val strings = TxturaStrings.current()
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        accentBorderColor = palette.safeColor,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = TxturaIcons.ShieldCheck,
                    contentDescription = null,
                    tint = palette.safeColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.createEncryptedBackupDialogTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.createEncryptedBackupDialogDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    lineHeight = 16.sp
                )
                BasicTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = palette.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    cursorBrush = SolidColor(palette.accentPrimary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    decorationBox = { innerTextField ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(palette.glassSurfaceElevated)
                                .border(
                                    1.dp,
                                    palette.accentPrimary.copy(alpha = 0.5f),
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (passwordInput.isEmpty()) {
                                    Text(
                                        text = strings.backupPasswordPlaceholder,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = palette.textSecondary.copy(alpha = 0.5f)
                                    )
                                }
                                innerTextField()
                            }
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = TxturaIcons.EyePreview,
                                    contentDescription = null,
                                    tint = if (passwordVisible) palette.accentPrimary else palette.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(passwordInput.trim().ifBlank { null }) }) {
                Text(
                    text = strings.backupPasswordConfirmButton,
                    color = palette.safeColor,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = strings.cancel,
                    color = palette.textSecondary
                )
            }
        }
    )
}

@Composable
fun RestoreEncryptedBackupPasswordDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalGlassPalette.current
    val strings = TxturaStrings.current()
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        accentBorderColor = palette.accentPrimary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = TxturaIcons.PermissionsKey,
                    contentDescription = null,
                    tint = palette.accentPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.restorePasswordDialogTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.restorePasswordDialogDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    lineHeight = 16.sp
                )
                BasicTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = palette.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    cursorBrush = SolidColor(palette.accentPrimary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    decorationBox = { innerTextField ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(palette.glassSurfaceElevated)
                                .border(
                                    1.dp,
                                    palette.accentPrimary.copy(alpha = 0.5f),
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (passwordInput.isEmpty()) {
                                    Text(
                                        text = strings.restorePasswordPlaceholder,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = palette.textSecondary.copy(alpha = 0.5f)
                                    )
                                }
                                innerTextField()
                            }
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = TxturaIcons.EyePreview,
                                    contentDescription = null,
                                    tint = if (passwordVisible) palette.accentPrimary else palette.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(passwordInput.trim()) },
                enabled = passwordInput.isNotBlank()
            ) {
                Text(
                    text = strings.restorePasswordConfirmButton,
                    color = if (passwordInput.isNotBlank()) palette.accentPrimary else palette.textSecondary.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = strings.cancel,
                    color = palette.textSecondary
                )
            }
        }
    )
}

@Composable
fun DeletedMessagesScreen(
    deletedEntries: List<DeletedConversationEntry>,
    retentionDays: Int,
    groupingMode: DeletedGroupingMode,
    showContactPhotos: Boolean,
    mainWallpaper: WallpaperConfig = WallpaperConfig(),
    onBack: () -> Unit,
    onSelectGroupingMode: (DeletedGroupingMode) -> Unit,
    onRestoreThread: (Long) -> Unit,
    onRestoreMessages: (Long, Set<Long>) -> Unit = { _, _ -> },
    onPermanentlyDeleteThread: (Long) -> Unit,
    onPermanentlyDeleteMessages: (Long, Set<Long>) -> Unit = { _, _ -> },
    onEmptyTrash: () -> Unit,
    highlightedCount: Int = 0,
    dockBarOrder: List<DockBarItem> = DockBarItem.defaultOrder,
    onUpdateDockBarOrder: (List<DockBarItem>) -> Unit = {},
    onOpenHighlightedMessages: () -> Unit = {},
    onOpenNewMessage: () -> Unit = {},
    onMarkAllRead: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val strings = TxturaStrings.current()
    var pendingPermanentDeleteEntry by remember { mutableStateOf<DeletedConversationEntry?>(null) }
    var inspectingEntry by remember { mutableStateOf<DeletedConversationEntry?>(null) }

    val groupedSections: List<Pair<String, List<DeletedConversationEntry>>> =
        remember(deletedEntries, groupingMode) {
            when (groupingMode) {
                DeletedGroupingMode.BY_DATE -> {
                    val dateFmt = SimpleDateFormat("dd.MM.yyyy.", Locale.getDefault())
                    deletedEntries
                        .sortedByDescending { it.deletedAtMillis }
                        .groupBy { dateFmt.format(Date(it.deletedAtMillis)) }
                        .entries
                        .map { it.key to it.value }
                }
                DeletedGroupingMode.BY_CONTACT -> {
                    deletedEntries
                        .sortedWith(
                            compareBy<DeletedConversationEntry> { it.thread.contactName.lowercase(Locale.getDefault()) }
                                .thenByDescending { it.deletedAtMillis }
                        )
                        .groupBy { "${it.thread.contactName} (${it.thread.address})" }
                        .entries
                        .map { it.key to it.value }
                }
            }
        }

    Box(modifier = Modifier.fillMaxSize()) {
        val palette = LocalGlassPalette.current
        val metrics = LocalAdaptiveMetrics.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevated = true,
                accentHighlight = palette.cautionColor,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        GlassIconButton(
                            icon = TxturaIcons.ArrowBack,
                            contentDescription = strings.back,
                            onClick = onBack,
                            size = 38.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.deletedMessagesTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = strings.deletedMessagesSubtitle(deletedEntries.size, retentionDays),
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (deletedEntries.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        GlassCard(
                            shape = RoundedCornerShape(50),
                            accentHighlight = palette.dangerColor,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            onClick = onEmptyTrash
                        ) {
                            Text(
                                text = strings.emptyTrashButton,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.dangerColor,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                DeletedGroupingMode.entries.forEach { mode ->
                    val selected = mode == groupingMode
                    val label = when (mode) {
                        DeletedGroupingMode.BY_DATE -> strings.groupByDateLabel
                        DeletedGroupingMode.BY_CONTACT -> strings.groupByContactLabel
                    }
                    GlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevated = selected,
                        accentHighlight = if (selected) palette.accentPrimary else null,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        onClick = { onSelectGroupingMode(mode) }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = if (mode == DeletedGroupingMode.BY_CONTACT) {
                                        TxturaIcons.ContactPerson
                                    } else {
                                        TxturaIcons.Restore
                                    },
                                    contentDescription = null,
                                    tint = if (selected) palette.accentPrimary else palette.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) palette.accentPrimary else palette.textSecondary,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (deletedEntries.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    GlassCard(
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(24.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = TxturaIcons.Delete,
                                contentDescription = null,
                                tint = palette.textSecondary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = strings.emptyTrashMessage,
                                style = MaterialTheme.typography.bodyLarge,
                                color = palette.textSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy((12 * metrics.scaleFactor).dp),
                    contentPadding = PaddingValues(bottom = (16 * metrics.scaleFactor).dp + metrics.safeBottomClearance),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    groupedSections.forEach { (sectionTitle, sectionItems) ->
                        item(key = "header_$sectionTitle", contentType = "header") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 2.dp, start = 4.dp)
                            ) {
                                Text(
                                    text = sectionTitle,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = palette.accentPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "(${sectionItems.sumOf { it.thread.messages.size }})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }

                        items(
                            items = sectionItems,
                            key = { it.thread.id },
                            contentType = { "deleted_entry" }
                        ) { entry ->
                            val remainingDays = entry.daysRemaining(retentionDays)
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                elevated = true,
                                accentHighlight = palette.cautionColor,
                                contentPadding = PaddingValues(14.dp)
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (showContactPhotos) {
                                            ContactAvatarView(
                                                thread = entry.thread,
                                                size = 42.dp,
                                                onClick = {}
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            OverflowTooltipText(
                                                text = entry.thread.contactName,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = palette.textPrimary,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "${entry.thread.address} • ${strings.messagesCountLabel(entry.thread.messages.size)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.textSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(palette.cautionColor.copy(alpha = 0.18f))
                                                .border(
                                                    1.dp,
                                                    palette.cautionColor.copy(alpha = 0.5f),
                                                    RoundedCornerShape(50)
                                                )
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = strings.daysRemainingBadge(remainingDays),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.cautionColor,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    entry.thread.messages.takeLast(2).forEach { msg ->
                                        OverflowTooltipText(
                                            text = "• ${msg.body}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = palette.textSecondary,
                                            maxLines = 2,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "${strings.deletedOnPrefix}${formatTimestamp(entry.deletedAtMillis)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        GlassCard(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(40.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            elevated = true,
                                            accentHighlight = palette.accentPrimary,
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                            onClick = { inspectingEntry = entry }
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = TxturaIcons.Search,
                                                        contentDescription = null,
                                                        tint = palette.accentPrimary,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = strings.inspectTrashMessagesButton,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = palette.accentPrimary,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }

                                        GlassCard(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(40.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            elevated = true,
                                            accentHighlight = palette.safeColor,
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                            onClick = { onRestoreThread(entry.thread.id) }
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = TxturaIcons.Restore,
                                                        contentDescription = null,
                                                        tint = palette.safeColor,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = strings.restoreButton,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = palette.safeColor,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }

                                        GlassCard(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(40.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            accentHighlight = palette.dangerColor,
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                            onClick = { pendingPermanentDeleteEntry = entry }
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = TxturaIcons.Delete,
                                                        contentDescription = null,
                                                        tint = palette.dangerColor,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = strings.deletePermanentlyButton,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = palette.dangerColor,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val entryToConfirm = pendingPermanentDeleteEntry
        if (entryToConfirm != null) {
            GlassAlertDialog(
                onDismissRequest = { pendingPermanentDeleteEntry = null },
                accentBorderColor = palette.dangerColor,
                title = {
                    Text(
                        text = strings.confirmPermanentDeleteTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.textPrimary
                    )
                },
                text = {
                    Text(
                        text = strings.confirmPermanentDeleteMessage(entryToConfirm.thread.contactName),
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textPrimary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onPermanentlyDeleteThread(entryToConfirm.thread.id)
                            pendingPermanentDeleteEntry = null
                        }
                    ) {
                        Text(
                            text = strings.confirmPermanentDeleteButton,
                            color = palette.dangerColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingPermanentDeleteEntry = null }) {
                        Text(
                            text = strings.cancel,
                            color = palette.textSecondary
                        )
                    }
                }
            )
        }

        val activeInspectEntry = inspectingEntry
        if (activeInspectEntry != null) {
            val currentEntryInTrash = deletedEntries.find { it.thread.id == activeInspectEntry.thread.id }
            if (currentEntryInTrash != null) {
                InspectTrashConversationDialog(
                    entry = currentEntryInTrash,
                    retentionDays = retentionDays,
                    onDismiss = { inspectingEntry = null },
                    onRestoreMessages = onRestoreMessages,
                    onPermanentlyDeleteMessages = onPermanentlyDeleteMessages
                )
            } else {
                inspectingEntry = null
            }
        }
    }
}

@Composable
private fun InspectTrashConversationDialog(
    entry: DeletedConversationEntry,
    retentionDays: Int,
    onDismiss: () -> Unit,
    onRestoreMessages: (Long, Set<Long>) -> Unit,
    onPermanentlyDeleteMessages: (Long, Set<Long>) -> Unit
) {
    val palette = LocalGlassPalette.current
    val strings = TxturaStrings.current()
    val allMessageIds = remember(entry.thread.messages) { entry.thread.messages.map { it.id }.toSet() }
    var selectedMsgIds by remember(entry.thread.messages) { mutableStateOf(allMessageIds) }

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        accentBorderColor = palette.cautionColor,
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(palette.cautionColor.copy(alpha = 0.16f))
                            .border(1.dp, palette.cautionColor.copy(alpha = 0.45f), CircleShape)
                    ) {
                        Icon(
                            imageVector = TxturaIcons.Delete,
                            contentDescription = null,
                            tint = palette.cautionColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.inspectTrashDialogTitle(entry.thread.contactName),
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${entry.thread.address} • ${strings.daysRemainingBadge(entry.daysRemaining(retentionDays))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = strings.selectedCountLabel(selectedMsgIds.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.accentPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    GlassCard(
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        onClick = {
                            selectedMsgIds = if (selectedMsgIds.size == allMessageIds.size) {
                                emptySet()
                            } else {
                                allMessageIds
                            }
                        }
                    ) {
                        Text(
                            text = if (selectedMsgIds.size == allMessageIds.size) strings.deselectAllContactsLabel else strings.selectAllContactsLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.accentPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                val scrollState = rememberScrollState()
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(scrollState)
                ) {
                    entry.thread.messages.forEach { msg ->
                        val isChecked = msg.id in selectedMsgIds
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            elevated = isChecked,
                            accentHighlight = if (isChecked) palette.accentPrimary else null,
                            contentPadding = PaddingValues(10.dp),
                            onClick = {
                                selectedMsgIds = if (isChecked) {
                                    selectedMsgIds - msg.id
                                } else {
                                    selectedMsgIds + msg.id
                                }
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = if (isChecked) TxturaIcons.Check else TxturaIcons.Close,
                                    contentDescription = null,
                                    tint = if (isChecked) palette.accentPrimary else palette.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = msg.body,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.textPrimary,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = formatTimestamp(msg.timestampMillis),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                GlassIconButton(
                                    icon = TxturaIcons.Restore,
                                    contentDescription = strings.restoreButton,
                                    tint = palette.safeColor,
                                    onClick = {
                                        onRestoreMessages(entry.thread.id, setOf(msg.id))
                                        if (entry.thread.messages.size <= 1) {
                                            onDismiss()
                                        }
                                    },
                                    size = 32.dp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                GlassIconButton(
                                    icon = TxturaIcons.Delete,
                                    contentDescription = strings.deletePermanentlyButton,
                                    tint = palette.dangerColor,
                                    onClick = {
                                        onPermanentlyDeleteMessages(entry.thread.id, setOf(msg.id))
                                        if (entry.thread.messages.size <= 1) {
                                            onDismiss()
                                        }
                                    },
                                    size = 32.dp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    shape = RoundedCornerShape(14.dp),
                    elevated = selectedMsgIds.isNotEmpty(),
                    accentHighlight = palette.safeColor,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                    onClick = {
                        if (selectedMsgIds.isNotEmpty()) {
                            onRestoreMessages(entry.thread.id, selectedMsgIds)
                            onDismiss()
                        }
                    }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = TxturaIcons.Restore,
                                contentDescription = null,
                                tint = if (selectedMsgIds.isNotEmpty()) palette.safeColor else palette.textSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = strings.restoreSelectedMessagesButton,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selectedMsgIds.isNotEmpty()) palette.safeColor else palette.textSecondary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    shape = RoundedCornerShape(14.dp),
                    elevated = selectedMsgIds.isNotEmpty(),
                    accentHighlight = palette.dangerColor,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                    onClick = {
                        if (selectedMsgIds.isNotEmpty()) {
                            onPermanentlyDeleteMessages(entry.thread.id, selectedMsgIds)
                            onDismiss()
                        }
                    }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = TxturaIcons.Delete,
                                contentDescription = null,
                                tint = if (selectedMsgIds.isNotEmpty()) palette.dangerColor else palette.textSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = strings.deleteSelectedMessagesButton,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selectedMsgIds.isNotEmpty()) palette.dangerColor else palette.textSecondary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        dismissButton = {
            GlassCard(
                modifier = Modifier.height(40.dp),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                onClick = onDismiss
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = strings.cancel,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    )
}

private val shortTimeFormatter = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
}

private val shortDateFormatter = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("dd.MM.", Locale.getDefault())
}

private val yearDateFormatter = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("dd.MM.yyyy.", Locale.getDefault())
}

private val fullDateAndTimeFormatter = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("dd.MM.yyyy. HH:mm", Locale.getDefault())
}

private fun formatTimestamp(millis: Long): String {
    return fullDateAndTimeFormatter.get()?.format(Date(millis)).orEmpty()
}

private fun formatThreadTimestamp(millis: Long): Pair<String, String?> {
    val now = System.currentTimeMillis()
    val nowCal = Calendar.getInstance().apply { timeInMillis = now }
    val msgCal = Calendar.getInstance().apply { timeInMillis = millis }
    val isSameYear = nowCal.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR)
    val isSameDay = isSameYear && (nowCal.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR))
    val timeStr = shortTimeFormatter.get()?.format(Date(millis)).orEmpty()

    return if (isSameYear) {
        if (isSameDay) {
            Pair(timeStr, null)
        } else {
            val dateStr = shortDateFormatter.get()?.format(Date(millis)).orEmpty()
            Pair(timeStr, dateStr)
        }
    } else {
        val dateStr = yearDateFormatter.get()?.format(Date(millis)).orEmpty()
        Pair(timeStr, dateStr)
    }
}

private fun isSameDay(ms1: Long, ms2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = ms1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = ms2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
        cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun cyrillicToLatin(text: String): String {
    val mapping = mapOf(
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d", 'ђ' to "đ",
        'е' to "e", 'ж' to "ž", 'з' to "z", 'и' to "i", 'ј' to "j", 'к' to "k",
        'л' to "l", 'љ' to "lj", 'м' to "m", 'н' to "n", 'њ' to "nj", 'о' to "o",
        'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t", 'ћ' to "ć", 'у' to "u",
        'ф' to "f", 'х' to "h", 'ц' to "c", 'ч' to "č", 'џ' to "dž", 'š' to "š", 'ш' to "š",
        'А' to "A", 'Б' to "B", 'В' to "V", 'Г' to "G", 'Д' to "D", 'Ђ' to "Đ",
        'Е' to "E", 'Ж' to "Ž", 'З' to "Z", 'И' to "I", 'Ј' to "J", 'К' to "K",
        'Л' to "L", 'Љ' to "Lj", 'М' to "M", 'Н' to "N", 'Њ' to "Nj", 'О' to "O",
        'П' to "P", 'Р' to "R", 'С' to "S", 'Т' to "T", 'Ћ' to "Ć", 'У' to "U",
        'Ф' to "F", 'Х' to "H", 'Ц' to "C", 'Ч' to "Č", 'Џ' to "Dž", 'Ш' to "Š"
    )
    val sb = StringBuilder(text.length)
    for (ch in text) {
        sb.append(mapping[ch] ?: ch.toString())
    }
    return sb.toString()
}

private fun formatChatDateHeader(timestampMillis: Long, isSerbian: Boolean): String {
    val now = Calendar.getInstance()
    val msgCal = Calendar.getInstance().apply { timeInMillis = timestampMillis }

    val isSameYear = now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR)
    val isToday = isSameYear && now.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)

    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val isYesterday = yesterday.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
        yesterday.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR)

    return when {
        isToday -> if (isSerbian) "danas" else "today"
        isYesterday -> if (isSerbian) "juče" else "yesterday"
        else -> {
            val locale = if (isSerbian) Locale.forLanguageTag("sr-Latn-RS") else Locale.getDefault()
            val pattern = if (isSameYear) "EEEE, d. MMMM" else "EEEE, d. MMMM yyyy."
            val sdf = SimpleDateFormat(pattern, locale)
            val rawDate = sdf.format(Date(timestampMillis)).lowercase(locale)
            if (isSerbian) cyrillicToLatin(rawDate) else rawDate
        }
    }
}

private fun formatMessageTimestamp(timestampMillis: Long, isSerbian: Boolean): String {
    val now = System.currentTimeMillis()
    val diffMs = now - timestampMillis

    if (diffMs < 0) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestampMillis))
    }

    val diffMinutes = diffMs / 60000L
    val diffHours = diffMs / 3600000L

    return when {
        diffMinutes < 1 -> if (isSerbian) "Upravo sada" else "Just now"
        diffMinutes < 60 -> {
            val m = diffMinutes.toInt()
            if (isSerbian) {
                if (m == 1) "Pre 1 minut" else "Pre $m minuta"
            } else {
                if (m == 1) "1 min ago" else "$m mins ago"
            }
        }
        diffHours < 6 -> {
            val h = diffHours.toInt()
            if (isSerbian) {
                if (h == 1) "Pre 1 sat" else if (h in 2..4) "Pre $h sata" else "Pre $h sati"
            } else {
                if (h == 1) "1 hour ago" else "$h hours ago"
            }
        }
        else -> {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            sdf.format(Date(timestampMillis))
        }
    }
}

@Composable
private fun ChatDateHeaderPill(
    timestampMillis: Long,
    strings: AppStrings,
    zoomScale: Float = 1.0f
) {
    val palette = LocalGlassPalette.current
    val dateText = remember(timestampMillis, strings.isSerbian) {
        formatChatDateHeader(timestampMillis, strings.isSerbian)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = (6f * zoomScale).dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(palette.incomingBubbleSurface.copy(alpha = 0.55f))
                .border(
                    width = 0.5.dp,
                    color = palette.glassBorderTop.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(50)
                )
                .padding(horizontal = (14f * zoomScale).dp, vertical = (4f * zoomScale).dp)
        ) {
            Text(
                text = dateText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = (11f * zoomScale).sp,
                    fontWeight = FontWeight.Medium
                ),
                color = palette.textSecondary.copy(alpha = 0.9f)
            )
        }
    }
}

/**
 * Displays an expanded, easy-to-browse Backgrounds & Themes selector with a 2-column visual
 * grid for Themes (Colorful, Monochrome, Minimalist) and a 2-column vertical grid for all
 * Txtura background textures (no horizontal swiping required).
 */
@Composable
fun WallpaperThemesSelectorGrid(
    currentConfig: WallpaperConfig,
    uiColorMode: UiColorMode = when {
        currentConfig.preset.isMinimalist -> UiColorMode.MINIMALIST
        currentConfig.preset.isMonochrome -> UiColorMode.MONOCHROME
        else -> UiColorMode.COLORFUL
    },
    strings: AppStrings,
    onSelectPreset: (WallpaperPreset) -> Unit,
    onPickFromGallery: () -> Unit = {},
    onSwitchColorMode: (UiColorMode) -> Unit = {}
) {
    val palette = LocalGlassPalette.current
    var selectedCategory by remember(currentConfig.preset, uiColorMode) {
        mutableStateOf(uiColorMode)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 1. Theme Category Tabs (Colorful vs Monochrome vs Minimalist)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val colorfulActive = selectedCategory == UiColorMode.COLORFUL
            GlassCard(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                elevated = colorfulActive,
                accentHighlight = if (colorfulActive) palette.accentPrimary else null,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                onClick = {
                    selectedCategory = UiColorMode.COLORFUL
                    onSwitchColorMode(UiColorMode.COLORFUL)
                }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = strings.wallpaperTabColorful,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (colorfulActive) palette.accentPrimary else palette.textSecondary,
                        fontWeight = if (colorfulActive) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            val monoActive = selectedCategory == UiColorMode.MONOCHROME
            GlassCard(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                elevated = monoActive,
                accentHighlight = if (monoActive) palette.accentPrimary else null,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                onClick = {
                    selectedCategory = UiColorMode.MONOCHROME
                    onSwitchColorMode(UiColorMode.MONOCHROME)
                }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = strings.wallpaperTabMonochrome,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (monoActive) palette.accentPrimary else palette.textSecondary,
                        fontWeight = if (monoActive) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            val miniActive = selectedCategory == UiColorMode.MINIMALIST
            GlassCard(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                elevated = miniActive,
                accentHighlight = if (miniActive) palette.accentPrimary else null,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                onClick = {
                    selectedCategory = UiColorMode.MINIMALIST
                    onSwitchColorMode(UiColorMode.MINIMALIST)
                }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = strings.wallpaperTabMinimalist,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (miniActive) palette.accentPrimary else palette.textSecondary,
                        fontWeight = if (miniActive) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        val presets = when (selectedCategory) {
            UiColorMode.COLORFUL -> WallpaperPreset.colorfulPresets
            UiColorMode.MONOCHROME -> WallpaperPreset.monochromePresets
            UiColorMode.MINIMALIST -> WallpaperPreset.minimalistPresets
        }
        val gridItems: List<WallpaperPreset?> = presets + listOf<WallpaperPreset?>(null)

        // 2. 2-Column Visual Grid of Themes so options are easy to scan and tap
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            gridItems.chunked(2).forEach { rowItems ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rowItems.forEach { preset ->
                        if (preset != null) {
                            val selected = !currentConfig.isCustomGalleryImage && currentConfig.preset == preset
                            val label = wallpaperPresetTitle(preset, strings)
                            val description = wallpaperPresetDescription(preset, strings)
                            val matchedTexture = if (currentConfig.texture == WallpaperTexture.AUTO_THEME) {
                                preset.defaultTexture
                            } else {
                                currentConfig.texture
                            }
                            val compAccent = when (preset) {
                                WallpaperPreset.ALL_BLACK, WallpaperPreset.ALL_WHITE -> if (palette.isDark) Color.White else Color.Black
                                else -> WallpaperHarmonyEngine.presetComplementaryAccent(preset, palette.isDark)
                            }
                            val accentDotColor = when {
                                preset.isMinimalist -> palette.sendButtonColor
                                else -> compAccent
                            }
                            val previewColors = when {
                                preset == WallpaperPreset.ALL_BLACK -> listOf(Color.Black, Color(0xFF141416), Color(0xFF27272A))
                                preset == WallpaperPreset.ALL_WHITE -> listOf(Color.White, Color(0xFFF4F4F5), Color(0xFFE4E4E7))
                                preset.isMonochrome -> {
                                    val monoBase = when {
                                        !palette.isDark -> Color(0xFFF1F5F9)
                                        palette.isAmoled -> Color.Black
                                        else -> Color(0xFF182130)
                                    }
                                    val monoMid = when {
                                        !palette.isDark -> Color(0xFFE2E8F0)
                                        palette.isAmoled -> Color(0xFF0E131D)
                                        else -> Color(0xFF263348)
                                    }
                                    listOf(monoBase, monoMid, compAccent.copy(alpha = 0.55f))
                                }
                                else -> listOf(
                                    if (palette.isAmoled) Color.Black else Color(preset.baseColorHex),
                                    Color(preset.primaryOrbHex),
                                    Color(preset.secondaryOrbHex)
                                )
                            }
                            val pCol = compAccent.copy(alpha = 0.72f)
                            val sCol = Color.White.copy(alpha = 0.42f)
                            val aCol = compAccent.copy(alpha = 0.90f)

                            GlassCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(110.dp),
                                shape = RoundedCornerShape(16.dp),
                                elevated = selected,
                                accentHighlight = if (selected) compAccent else null,
                                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 9.dp),
                                onClick = { onSelectPreset(preset) }
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        contentAlignment = Alignment.BottomEnd,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Brush.linearGradient(colors = previewColors))
                                            .drawBehind {
                                                drawTxturaTexture(
                                                    texture = matchedTexture,
                                                    primaryColor = pCol,
                                                    secondaryColor = sCol,
                                                    accentColor = aCol,
                                                    isCompactPreview = true
                                                )
                                            }
                                            .border(
                                                width = if (selected) 1.5.dp else 1.dp,
                                                color = if (selected) compAccent else Color.White.copy(alpha = 0.32f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .padding(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(accentDotColor)
                                                .border(1.dp, Color.Black.copy(alpha = 0.55f), CircleShape)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OverflowTooltipText(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (selected) compAccent else palette.textPrimary,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = description,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            lineHeight = 13.5.sp
                                        ),
                                        color = if (selected) compAccent.copy(alpha = 0.88f) else palette.textSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        } else {
                            val isGallerySelected = currentConfig.isCustomGalleryImage
                            GlassCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(110.dp),
                                shape = RoundedCornerShape(16.dp),
                                elevated = isGallerySelected,
                                accentHighlight = if (isGallerySelected) palette.accentPrimary else palette.accentSecondary,
                                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 9.dp),
                                onClick = onPickFromGallery
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        palette.accentPrimary.copy(alpha = 0.25f),
                                                        palette.accentSecondary.copy(alpha = 0.22f),
                                                        palette.glassSurfaceElevated
                                                    )
                                                )
                                            )
                                            .border(
                                                width = if (isGallerySelected) 1.5.dp else 1.dp,
                                                color = if (isGallerySelected) palette.accentPrimary else Color.White.copy(alpha = 0.32f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = TxturaIcons.AttachImage,
                                                contentDescription = null,
                                                tint = palette.accentPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "+",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = palette.accentPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OverflowTooltipText(
                                        text = strings.pickWallpaperFromGallery,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isGallerySelected) palette.accentPrimary else palette.textPrimary,
                                        fontWeight = if (isGallerySelected) FontWeight.Bold else FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isGallerySelected) "✓ ${strings.customGalleryWallpaperActive}" else "+",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            lineHeight = 13.5.sp
                                        ),
                                        color = if (isGallerySelected) palette.safeColor else palette.textSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun WallpaperTexturesSelectorGrid(
    currentConfig: WallpaperConfig,
    strings: AppStrings,
    showInternalHeader: Boolean = true,
    onSelectTexture: (WallpaperTexture) -> Unit = {},
    onSelectAccentColor: (Long?) -> Unit = {}
) {
    val palette = LocalGlassPalette.current
    if (currentConfig.isCustomGalleryImage) {
        // Offered Accent Colors for Custom Gallery Wallpaper (No textures applied)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (showInternalHeader) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.customWallpaperAccentTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        OverflowTooltipText(
                            text = strings.customWallpaperAccentSub,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                            maxLines = 2
                        )
                    }
                }
            }

            val offeredAccents = listOf(
                Triple(if (strings.isSerbian) "Kraljevsko plava" else "Royal Blue", 0xFF2563EBL, Color(0xFF2563EB)),
                Triple(if (strings.isSerbian) "Safirna" else "Sapphire", 0xFF0284C7L, Color(0xFF0284C7)),
                Triple(if (strings.isSerbian) "Smaragdno zelena" else "Emerald", 0xFF059669L, Color(0xFF059669)),
                Triple(if (strings.isSerbian) "Tirkizna" else "Teal", 0xFF0D9488L, Color(0xFF0D9488)),
                Triple(if (strings.isSerbian) "Ametist ljubičasta" else "Amethyst", 0xFF7C3AEDL, Color(0xFF7C3AED)),
                Triple(if (strings.isSerbian) "Rubin crvena" else "Ruby Red", 0xFFDC2626L, Color(0xFFDC2626)),
                Triple(if (strings.isSerbian) "Koralna" else "Coral", 0xFFEA580CL, Color(0xFFEA580C)),
                Triple(if (strings.isSerbian) "Ćilibar zlatna" else "Amber Gold", 0xFFD97706L, Color(0xFFD97706)),
                Triple(if (strings.isSerbian) "Ruža roze" else "Rose Pink", 0xFFDB2777L, Color(0xFFDB2777)),
                Triple(if (strings.isSerbian) "Lajm sveža" else "Lime Mint", 0xFF65A30DL, Color(0xFF65A30D)),
                Triple(if (strings.isSerbian) "Srebrno bela" else "Silver White", 0xFFF1F5F9L, Color(0xFFF1F5F9)),
                Triple(if (strings.isSerbian) "Grafit siva" else "Charcoal Slate", 0xFF475569L, Color(0xFF475569))
            )

            offeredAccents.chunked(2).forEach { rowAccents ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rowAccents.forEach { (name, hex, col) ->
                        val isSelected = currentConfig.customAccentColorHex == hex
                        GlassCard(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(14.dp),
                            elevated = isSelected,
                            accentHighlight = if (isSelected) col else null,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            onClick = { onSelectAccentColor(hex) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(col)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) Color.White else Color.Black.copy(alpha = 0.25f),
                                            shape = CircleShape
                                        )
                                ) {
                                    if (isSelected) {
                                        Text(
                                            text = "✓",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (col == Color(0xFFF1F5F9)) Color.Black else Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OverflowTooltipText(
                                    text = name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) col else palette.textPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    if (rowAccents.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    } else {
        // Expanded 2-Column Txtura Texture Grid (no left/right swiping needed)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (showInternalHeader) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.textureSectionTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        OverflowTooltipText(
                            text = strings.textureSectionSub,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                            maxLines = 2
                        )
                    }
                }
            }

            val activePreset = currentConfig.preset
            val activeCompAccent = when (activePreset) {
                WallpaperPreset.ALL_BLACK, WallpaperPreset.ALL_WHITE -> if (palette.isDark) Color.White else Color.Black
                else -> WallpaperHarmonyEngine.presetComplementaryAccent(activePreset, palette.isDark)
            }
            val activePreviewColors = when {
                activePreset == WallpaperPreset.ALL_BLACK -> listOf(Color.Black, Color(0xFF141416), Color(0xFF27272A))
                activePreset == WallpaperPreset.ALL_WHITE -> listOf(Color.White, Color(0xFFF4F4F5), Color(0xFFE4E4E7))
                activePreset.isMonochrome -> {
                    val monoBase = when {
                        !palette.isDark -> Color(0xFFF1F5F9)
                        palette.isAmoled -> Color.Black
                        else -> Color(0xFF182130)
                    }
                    val monoMid = when {
                        !palette.isDark -> Color(0xFFE2E8F0)
                        palette.isAmoled -> Color.Black
                        else -> Color(0xFF263348)
                    }
                    listOf(monoBase, monoMid, activeCompAccent.copy(alpha = 0.55f))
                }
                else -> listOf(
                    if (palette.isAmoled) Color.Black else Color(activePreset.baseColorHex),
                    Color(activePreset.primaryOrbHex),
                    Color(activePreset.secondaryOrbHex)
                )
            }

            WallpaperTexture.entries.chunked(2).forEach { rowTextures ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rowTextures.forEach { tex ->
                        val isSelected = currentConfig.texture == tex
                        val previewTex = if (tex == WallpaperTexture.AUTO_THEME) {
                            currentConfig.preset.defaultTexture
                        } else {
                            tex
                        }
                        val texTitle = wallpaperTextureTitle(tex, strings)
                        val pCol = activeCompAccent.copy(alpha = 0.72f)
                        val sCol = Color.White.copy(alpha = 0.42f)
                        val aCol = activeCompAccent.copy(alpha = 0.90f)

                        GlassCard(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            elevated = isSelected,
                            accentHighlight = if (isSelected) palette.accentPrimary else null,
                            contentPadding = PaddingValues(10.dp),
                            onClick = { onSelectTexture(tex) }
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Brush.linearGradient(colors = activePreviewColors))
                                        .drawBehind {
                                            drawTxturaTexture(
                                                texture = previewTex,
                                                primaryColor = pCol,
                                                secondaryColor = sCol,
                                                accentColor = aCol,
                                                isCompactPreview = true
                                            )
                                        }
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) palette.accentPrimary else Color.White.copy(alpha = 0.32f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                )
                                Spacer(modifier = Modifier.height(7.dp))
                                OverflowTooltipText(
                                    text = texTitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) palette.accentPrimary else palette.textPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    if (rowTextures.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun WallpaperPresetSelectorGrid(
    currentConfig: WallpaperConfig,
    uiColorMode: UiColorMode = when {
        currentConfig.preset.isMinimalist -> UiColorMode.MINIMALIST
        currentConfig.preset.isMonochrome -> UiColorMode.MONOCHROME
        else -> UiColorMode.COLORFUL
    },
    strings: AppStrings,
    onSelectPreset: (WallpaperPreset) -> Unit,
    onSelectTexture: (WallpaperTexture) -> Unit = {},
    onSelectAccentColor: (Long?) -> Unit = {},
    onPickFromGallery: () -> Unit = {},
    onSwitchColorMode: (UiColorMode) -> Unit = {}
) {
    val palette = LocalGlassPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        WallpaperThemesSelectorGrid(
            currentConfig = currentConfig,
            uiColorMode = uiColorMode,
            strings = strings,
            onSelectPreset = onSelectPreset,
            onPickFromGallery = onPickFromGallery,
            onSwitchColorMode = onSwitchColorMode
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.8.dp)
                .background(palette.glassBorderTop.copy(alpha = 0.28f))
        )

        WallpaperTexturesSelectorGrid(
            currentConfig = currentConfig,
            strings = strings,
            showInternalHeader = true,
            onSelectTexture = onSelectTexture,
            onSelectAccentColor = onSelectAccentColor
        )
    }
}

@Composable
fun EmergencyAlertsDialog(
    strings: AppStrings,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalGlassPalette.current
    val scrollState = rememberScrollState()

    val alerts by EmergencyAlertManager.alerts.collectAsState()
    val deviceCountry = remember { EmergencyAlertManager.getDeviceCountryIso(context) }
    val countryName = remember(deviceCountry, strings.isSerbian) {
        EmergencyAlertManager.getCountryDisplayName(deviceCountry, strings.isSerbian)
    }

    var extremeEnabled by remember { mutableStateOf(EmergencyAlertManager.isExtremeAlertsEnabled(context)) }
    var severeEnabled by remember { mutableStateOf(EmergencyAlertManager.isSevereAlertsEnabled(context)) }
    var amberEnabled by remember { mutableStateOf(EmergencyAlertManager.isAmberAlertsEnabled(context)) }
    var soundEnabled by remember { mutableStateOf(EmergencyAlertManager.isSoundAlertsEnabled(context)) }

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        accentBorderColor = palette.accentPrimary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = TxturaIcons.Warning,
                    contentDescription = null,
                    tint = palette.accentPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.emergencyAlertsTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(scrollState)
            ) {
                // 1. Current Country Location & Filtering Rule Banner
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    accentHighlight = palette.accentPrimary,
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = TxturaIcons.ShieldCheck,
                                contentDescription = null,
                                tint = palette.accentPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.emergencyLocationLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = countryName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = strings.emergencyFilterDesc,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            ),
                            color = palette.textSecondary
                        )
                    }
                }

                // 2. Alert Channels & Categories
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Extreme threats
                        SettingsSwitchRow(
                            title = strings.emergencyExtremeTitle,
                            subtitle = strings.emergencyExtremeSub,
                            checked = extremeEnabled,
                            onCheckedChange = { checked ->
                                extremeEnabled = checked
                                EmergencyAlertManager.setExtremeAlertsEnabled(context, checked)
                            }
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.8.dp)
                                .background(palette.glassBorderTop.copy(alpha = 0.28f))
                        )

                        // Severe threats
                        SettingsSwitchRow(
                            title = strings.emergencySevereTitle,
                            subtitle = strings.emergencySevereSub,
                            checked = severeEnabled,
                            onCheckedChange = { checked ->
                                severeEnabled = checked
                                EmergencyAlertManager.setSevereAlertsEnabled(context, checked)
                            }
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.8.dp)
                                .background(palette.glassBorderTop.copy(alpha = 0.28f))
                        )

                        // Amber alert
                        SettingsSwitchRow(
                            title = strings.emergencyAmberTitle,
                            subtitle = strings.emergencyAmberSub,
                            checked = amberEnabled,
                            onCheckedChange = { checked ->
                                amberEnabled = checked
                                EmergencyAlertManager.setAmberAlertsEnabled(context, checked)
                            }
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.8.dp)
                                .background(palette.glassBorderTop.copy(alpha = 0.28f))
                        )

                        // Global catastrophe (ALWAYS ON - enforced for planetary / global threats)
                        SettingsSwitchRow(
                            title = strings.emergencyGlobalTitle,
                            subtitle = strings.emergencyGlobalSub,
                            checked = true,
                            enabled = false,
                            onCheckedChange = {}
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.8.dp)
                                .background(palette.glassBorderTop.copy(alpha = 0.28f))
                        )

                        // Sound signal
                        SettingsSwitchRow(
                            title = strings.emergencySoundTitle,
                            subtitle = strings.emergencySoundSub,
                            checked = soundEnabled,
                            onCheckedChange = { checked ->
                                soundEnabled = checked
                                EmergencyAlertManager.setSoundAlertsEnabled(context, checked)
                            }
                        )
                    }
                }

                // 3. Action Button (Trigger Test Alert)
                GlassCard(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    shape = RoundedCornerShape(50),
                    accentHighlight = palette.accentSecondary,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    onClick = {
                        EmergencyAlertManager.triggerTestAlert(context)
                        Toast.makeText(context, strings.emergencyTestToast, Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(
                        text = strings.emergencyTestButton,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.accentSecondary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                // 4. Alert History / Feed
                if (alerts.isEmpty()) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = strings.emergencyNoAlertsTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = strings.emergencyNoAlertsSub,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Text(
                        text = if (strings.isSerbian) "Primljena upozorenja" else "Received Alerts",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    alerts.forEach { alert ->
                        val alertColor = when (alert.severity) {
                            EmergencySeverity.GLOBAL_CATASTROPHE,
                            EmergencySeverity.PRESIDENTIAL,
                            EmergencySeverity.EXTREME -> Color(0xFFDC2626)
                            EmergencySeverity.SEVERE -> Color(0xFFEA580C)
                            EmergencySeverity.AMBER_ALERT -> Color(0xFFD97706)
                            EmergencySeverity.TEST -> palette.accentPrimary
                            EmergencySeverity.INFO -> palette.accentSecondary
                        }
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            accentHighlight = alertColor,
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = alert.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = alertColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = alert.countryIso,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = alertColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = alert.body,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = formatTimestamp(alert.timestampMillis),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = strings.closeDialog,
                    color = palette.accentPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
fun WallpaperPickerDialog(
    title: String,
    currentConfig: WallpaperConfig,
    showResetToMainButton: Boolean = false,
    onSelectPreset: (WallpaperPreset) -> Unit,
    onSelectTexture: (WallpaperTexture) -> Unit = {},
    onSelectAccentColor: (Long?) -> Unit = {},
    onPickFromGallery: () -> Unit,
    onResetToMainWallpaper: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val palette = LocalGlassPalette.current
    val strings = TxturaStrings.current()
    val scrollState = rememberScrollState()

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        accentBorderColor = palette.accentPrimary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = TxturaIcons.AttachImage,
                    contentDescription = null,
                    tint = palette.accentPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .height(420.dp)
                    .verticalScroll(scrollState)
            ) {
                if (showResetToMainButton) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        onClick = onResetToMainWallpaper
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = TxturaIcons.Restore,
                                contentDescription = null,
                                tint = palette.accentSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.useMainWallpaperDefault,
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.accentSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                WallpaperPresetSelectorGrid(
                    currentConfig = currentConfig,
                    strings = strings,
                    onSelectPreset = onSelectPreset,
                    onSelectTexture = onSelectTexture,
                    onSelectAccentColor = onSelectAccentColor,
                    onPickFromGallery = onPickFromGallery
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = strings.cancel,
                    color = palette.accentPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

private fun wallpaperPresetTitle(preset: WallpaperPreset, strings: AppStrings): String {
    return when (preset) {
        WallpaperPreset.DEFAULT -> strings.wpDefault
        WallpaperPreset.AURORA_BOREALIS -> strings.wpAurora
        WallpaperPreset.VELVET_PLUM -> strings.wpVelvetPlum
        WallpaperPreset.MIDNIGHT_OCEAN -> strings.wpMidnightOcean
        WallpaperPreset.SUNSET_HORIZON -> strings.wpSunsetHorizon
        WallpaperPreset.EMERALD_FOREST -> strings.wpEmeraldForest
        WallpaperPreset.ROYAL_AMETHYST -> strings.wpRoyalAmethyst
        WallpaperPreset.STARLIGHT_GALAXY -> strings.wpStarlightGalaxy
        WallpaperPreset.CRIMSON_VELVET -> strings.wpCrimsonVelvet
        WallpaperPreset.BURGUNDY_ANITA -> strings.wpBurgundyAnita
        WallpaperPreset.ARCTIC_FROST -> strings.wpArcticFrost
        WallpaperPreset.OBSIDIAN_GOLD -> strings.wpObsidianGold
        WallpaperPreset.LAVENDER_DREAM -> strings.wpLavenderDream
        WallpaperPreset.ESPRESSO_CREAM -> strings.wpEspressoCream
        WallpaperPreset.MONO_SLATE -> strings.wpMonoSlate
        WallpaperPreset.MONO_SAPPHIRE -> strings.wpMonoSapphire
        WallpaperPreset.MONO_SAGE -> strings.wpMonoSage
        WallpaperPreset.MONO_CHAMPAGNE -> strings.wpMonoChampagne
        WallpaperPreset.MONO_PLUM -> strings.wpMonoPlum
        WallpaperPreset.MONO_GRAPHITE -> strings.wpMonoGraphite
        WallpaperPreset.MONO_BURGUNDY -> strings.wpMonoBurgundy
        WallpaperPreset.MONO_NAVY -> strings.wpMonoNavy
        WallpaperPreset.MONO_SMOKE -> strings.wpMonoSmoke
        WallpaperPreset.MONO_ESPRESSO -> strings.wpMonoEspresso
        WallpaperPreset.MONO_LAVENDER -> strings.wpMonoLavender
        WallpaperPreset.ALL_BLACK -> strings.wpAllBlack
        WallpaperPreset.ALL_WHITE -> strings.wpAllWhite
    }
}

private fun wallpaperPresetDescription(preset: WallpaperPreset, strings: AppStrings): String {
    return when (preset) {
        WallpaperPreset.DEFAULT -> strings.wpDefaultDesc
        WallpaperPreset.AURORA_BOREALIS -> strings.wpAuroraDesc
        WallpaperPreset.VELVET_PLUM -> strings.wpVelvetPlumDesc
        WallpaperPreset.MIDNIGHT_OCEAN -> strings.wpMidnightOceanDesc
        WallpaperPreset.SUNSET_HORIZON -> strings.wpSunsetHorizonDesc
        WallpaperPreset.EMERALD_FOREST -> strings.wpEmeraldForestDesc
        WallpaperPreset.ROYAL_AMETHYST -> strings.wpRoyalAmethystDesc
        WallpaperPreset.STARLIGHT_GALAXY -> strings.wpStarlightGalaxyDesc
        WallpaperPreset.CRIMSON_VELVET -> strings.wpCrimsonVelvetDesc
        WallpaperPreset.BURGUNDY_ANITA -> strings.wpBurgundyAnitaDesc
        WallpaperPreset.ARCTIC_FROST -> strings.wpArcticFrostDesc
        WallpaperPreset.OBSIDIAN_GOLD -> strings.wpObsidianGoldDesc
        WallpaperPreset.LAVENDER_DREAM -> strings.wpLavenderDreamDesc
        WallpaperPreset.ESPRESSO_CREAM -> strings.wpEspressoCreamDesc
        WallpaperPreset.MONO_SLATE -> strings.wpMonoSlateDesc
        WallpaperPreset.MONO_SAPPHIRE -> strings.wpMonoSapphireDesc
        WallpaperPreset.MONO_SAGE -> strings.wpMonoSageDesc
        WallpaperPreset.MONO_CHAMPAGNE -> strings.wpMonoChampagneDesc
        WallpaperPreset.MONO_PLUM -> strings.wpMonoPlumDesc
        WallpaperPreset.MONO_GRAPHITE -> strings.wpMonoGraphiteDesc
        WallpaperPreset.MONO_BURGUNDY -> strings.wpMonoBurgundyDesc
        WallpaperPreset.MONO_NAVY -> strings.wpMonoNavyDesc
        WallpaperPreset.MONO_SMOKE -> strings.wpMonoSmokeDesc
        WallpaperPreset.MONO_ESPRESSO -> strings.wpMonoEspressoDesc
        WallpaperPreset.MONO_LAVENDER -> strings.wpMonoLavenderDesc
        WallpaperPreset.ALL_BLACK -> strings.wpAllBlackDesc
        WallpaperPreset.ALL_WHITE -> strings.wpAllWhiteDesc
    }
}

private fun wallpaperTextureTitle(texture: WallpaperTexture, strings: AppStrings): String {
    return when (texture) {
        WallpaperTexture.AUTO_THEME -> strings.texAutoTheme
        WallpaperTexture.SILK_WEAVE -> strings.texSilkWeave
        WallpaperTexture.AURORA_RIBBONS -> strings.texAuroraRibbons
        WallpaperTexture.VELVET_NAP -> strings.texVelvetNap
        WallpaperTexture.OCEAN_CAUSTICS -> strings.texOceanCaustics
        WallpaperTexture.SAND_DUNES -> strings.texSandDunes
        WallpaperTexture.ORGANIC_BOTANICAL -> strings.texOrganicBotanical
        WallpaperTexture.CRYSTAL_FACETS -> strings.texCrystalFacets
        WallpaperTexture.CONSTELLATION_MESH -> strings.texConstellationMesh
        WallpaperTexture.DAMASK_JACQUARD -> strings.texDamaskJacquard
        WallpaperTexture.FROST_CRYSTALS -> strings.texFrostCrystals
        WallpaperTexture.KINTSUGI_MARBLE -> strings.texKintsugiMarble
        WallpaperTexture.SLATE_STRATA -> strings.texSlateStrata
        WallpaperTexture.BRUSHED_METAL -> strings.texBrushedMetal
        WallpaperTexture.LINEN_CANVAS -> strings.texLinenCanvas
        WallpaperTexture.FLUTED_GLASS -> strings.texFlutedGlass
        WallpaperTexture.SUEDE_MICROFIBER -> strings.texSuedeMicrofiber
        WallpaperTexture.CARBON_TWILL -> strings.texCarbonTwill
        WallpaperTexture.COTTON_PAPER -> strings.texCottonPaper
        WallpaperTexture.HERRINGBONE_TWILL -> strings.texHerringboneTwill
        WallpaperTexture.TOPOGRAPHIC_CONTOUR -> strings.texTopographicContour
        WallpaperTexture.PEBBLED_LEATHER -> strings.texPebbledLeather
        WallpaperTexture.LAVENDER_PETAL -> strings.texLavenderPetal
        WallpaperTexture.COFFEE_BEANS -> strings.texCoffeeBeans
        WallpaperTexture.NONE -> strings.texNone
    }
}

/**
 * Full-screen biometric and PIN lock screen overlay shown when the app is locked.
 */
@Composable
fun AppLockScreen(
    mainWallpaper: WallpaperConfig,
    onUnlock: () -> Unit
) {
    val context = LocalContext.current
    val isDeviceSecure = remember(context) { AppSecurityManager.isDeviceLockConfigured(context) }
    val palette = LocalGlassPalette.current
    val isSr = TxturaStrings.isSerbianLocale()

    GlassBackground(wallpaperConfig = mainWallpaper) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                elevated = true,
                accentHighlight = palette.accentPrimary,
                contentPadding = PaddingValues(28.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(palette.accentPrimary.copy(alpha = 0.15f))
                            .border(1.5.dp, palette.accentPrimary.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = TxturaIcons.PermissionsKey,
                            contentDescription = null,
                            tint = palette.accentPrimary,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isSr) "Txtura je zaključana" else "Txtura is Locked",
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isSr) {
                                if (isDeviceSecure) "Biometrijska zaštita i PIN su aktivni. Potvrdite identitet za pregled poruka."
                                else "Telefon nema podešeno zaključavanja ekrana. Dodirnite dugme ispod da otključate aplikaciju."
                            } else {
                                if (isDeviceSecure) "Biometric protection and PIN are active. Confirm identity to access messages."
                                else "Device has no screen lock set up. Tap below to unlock the app."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        accentHighlight = palette.accentPrimary,
                        elevated = true,
                        contentPadding = PaddingValues(vertical = 14.dp, horizontal = 20.dp),
                        onClick = onUnlock
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = TxturaIcons.ShieldCheck,
                                contentDescription = null,
                                tint = palette.accentPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isSr) {
                                    if (isDeviceSecure) "Otključaj aplikaciju" else "Pristupi aplikaciji"
                                } else {
                                    if (isDeviceSecure) "Unlock App" else "Access App"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.accentPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (!isDeviceSecure) {
                        TextButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    })
                                }
                            }
                        ) {
                            Text(
                                text = if (isSr) "Podesi zaključavanje ekrana" else "Set up screen lock",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.accentPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
