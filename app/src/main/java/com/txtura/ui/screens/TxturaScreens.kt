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
import android.provider.CalendarContract
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
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.draw.clipToBounds
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
import com.txtura.data.MessageSwipeAction
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
    listState: LazyListState = rememberLazyListState(),
    savedScrollIndex: Int = 0,
    savedScrollOffset: Int = 0,
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
    onOpenThreadWithMessage: (Long, Long, String) -> Unit = { threadId, _, _ -> onOpenThread(threadId) },
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
    var selectedFilter by rememberSaveable { mutableStateOf(value = activeCategories.firstOrNull() ?: ConversationFilter.ALL) }
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

    LaunchedEffect(filteredThreads.size) {
        if (filteredThreads.isNotEmpty() && (savedScrollIndex > 0 || savedScrollOffset > 0)) {
            if (listState.firstVisibleItemIndex == 0) {
                listState.scrollToItem(savedScrollIndex, savedScrollOffset)
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
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 17.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = palette.textPrimary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
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
            val batchIcon = when (batchAction) {
                BatchSelectionAction.DELETE -> TxturaIcons.Delete
                BatchSelectionAction.MARK_READ -> TxturaIcons.CheckDouble
            }

            GlassAlertDialog(
                onDismissRequest = { pendingBatchAction = null },
                accentBorderColor = accent,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = batchIcon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 17.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = palette.textPrimary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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

            // By default, LazyColumn keeps its scroll position anchored to the KEY of the first
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
                                val trimmedSearch = searchQuery.trim()
                                val matchedSearchMessage = remember(thread.messages, trimmedSearch) {
                                    if (trimmedSearch.isEmpty()) {
                                        null
                                    } else {
                                        thread.messages.lastOrNull { msg ->
                                            msg.body.contains(trimmedSearch, ignoreCase = true)
                                        }
                                    }
                                }

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
                                        matchedSearchMessage = matchedSearchMessage,
                                        strings = strings,
                                        onClick = {
                                            if (isSelectionMode) {
                                                toggleSelection()
                                            } else if (trimmedSearch.isNotEmpty() &&
                                                (matchedSearchMessage != null || thread.messages.any { it.body.contains(trimmedSearch, ignoreCase = true) })
                                            ) {
                                                onOpenThreadWithMessage(
                                                    thread.id,
                                                    matchedSearchMessage?.id ?: -1L,
                                                    trimmedSearch
                                                )
                                            } else {
                                                onOpenThread(thread.id)
                                            }
                                        },
                                        onOpenThreadWithMessage = { threadId, msgId ->
                                            if (isSelectionMode) {
                                                toggleSelection()
                                            } else {
                                                onOpenThreadWithMessage(threadId, msgId, trimmedSearch)
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
                    onMarkAllRead = { showMarkAllReadConfirmation = true },
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

    val isExpressive = LocalExpressiveAnimations.current
    val targetUnreadWeight = when {
        totalUnreadCount <= 0 -> 1.0f
        totalUnreadCount < 10 -> 1.34f
        else -> 1.42f
    }
    val animatedUnreadWeight by animateFloatAsState(
        targetValue = targetUnreadWeight,
        animationSpec = if (isExpressive) spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium) else tween(220),
        label = "category_unread_weight"
    )
    val otherCategoriesCount = (localOrder.size - 1).coerceAtLeast(1)
    val otherCategoryWeight = (localOrder.size.toFloat() - animatedUnreadWeight) / otherCategoriesCount.toFloat()

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

                val itemWeight = if (filter == ConversationFilter.UNREAD) animatedUnreadWeight else otherCategoryWeight

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
    SWIPE_GESTURES,
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
    flashAlertMode: com.txtura.data.FlashAlertMode = com.txtura.data.FlashAlertMode.ALWAYS,
    onSelectFlashAlertMode: (com.txtura.data.FlashAlertMode) -> Unit = {},
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
    val view = LocalView.current
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
    var pendingConfirmUpdateRelease by remember { mutableStateOf<com.txtura.data.GitHubReleaseInfo?>(null) }

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
        if (activeCategoryWindow == SettingsCategoryWindow.SWIPE_GESTURES) {
            activeCategoryWindow = SettingsCategoryWindow.CHAT_AND_GESTURES
        } else {
            activeCategoryWindow = null
        }
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
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    TxturaHaptics.tick(view, context)
                                    showSecurityMeasuresDialog = true
                                }
                                .padding(horizontal = 12.dp, vertical = 2.dp),
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
                        SettingsCategoryWindow.SWIPE_GESTURES -> Triple(
                            TxturaIcons.SwipeHoriz,
                            palette.accentPrimary,
                            strings.swipeGesturesTitle
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
                                onClick = {
                                    if (currentWindow == SettingsCategoryWindow.SWIPE_GESTURES) {
                                        activeCategoryWindow = SettingsCategoryWindow.CHAT_AND_GESTURES
                                    } else {
                                        activeCategoryWindow = null
                                    }
                                },
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

                                // 2B-2. Haptički odzivi (Haptic Feedback)
                                SettingsSwitchRow(
                                    title = strings.hapticsTitle,
                                    subtitle = strings.hapticsSub,
                                    checked = hapticsEnabled,
                                    onCheckedChange = onToggleHaptics
                                )

                                // 2C. Svetlosni odziv (Flash Alert - rhythm synchronized torch notification + activation mode)
                                FlashAlertSettingsCard(
                                    flashAlertEnabled = flashAlertEnabled,
                                    onToggleFlashAlert = onToggleFlashAlert,
                                    flashAlertMode = flashAlertMode,
                                    onSelectFlashAlertMode = onSelectFlashAlertMode,
                                    strings = strings
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
                                // 3A. Pokreti prevlačenja (Swipe Gestures) - zaseban meni
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    contentPadding = PaddingValues(14.dp),
                                    onClick = { activeCategoryWindow = SettingsCategoryWindow.SWIPE_GESTURES }
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
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(palette.accentPrimary.copy(alpha = 0.14f))
                                                    .border(1.dp, palette.accentPrimary.copy(alpha = 0.42f), CircleShape)
                                            ) {
                                                Icon(
                                                    imageVector = TxturaIcons.SwipeHoriz,
                                                    contentDescription = null,
                                                    tint = palette.accentPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = strings.swipeGesturesTitle,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = palette.textPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = strings.swipeGesturesSub,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = palette.textSecondary
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = TxturaIcons.ChevronRight,
                                            contentDescription = null,
                                            tint = palette.textSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

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

                            SettingsCategoryWindow.SWIPE_GESTURES -> {
                                var messageSwipeRightAction by remember {
                                    mutableStateOf(SmsMmsRepository.getMessageSwipeRightAction(context))
                                }
                                var messageSwipeLeftAction by remember {
                                    mutableStateOf(SmsMmsRepository.getMessageSwipeLeftAction(context))
                                }

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

                                MessageSwipeDirectionInteractiveCard(
                                    title = strings.messageSwipeRightLabel,
                                    currentAction = messageSwipeRightAction,
                                    defaultFallbackAction = MessageSwipeAction.PIN,
                                    strings = strings,
                                    onSelectAction = { action ->
                                        messageSwipeRightAction = action
                                        SmsMmsRepository.setMessageSwipeRightAction(context, action)
                                    }
                                )

                                MessageSwipeDirectionInteractiveCard(
                                    title = strings.messageSwipeLeftLabel,
                                    currentAction = messageSwipeLeftAction,
                                    defaultFallbackAction = MessageSwipeAction.HIGHLIGHT,
                                    strings = strings,
                                    onSelectAction = { action ->
                                        messageSwipeLeftAction = action
                                        SmsMmsRepository.setMessageSwipeLeftAction(context, action)
                                    }
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
                                                            if (!AppUpdateManager.hasActiveInternetConnection(context)) {
                                                                Toast.makeText(
                                                                    context,
                                                                    strings.updateNoInternetToast,
                                                                    Toast.LENGTH_SHORT,
                                                                ).show()
                                                            } else {
                                                                pendingConfirmUpdateRelease = releaseToInstall
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
                                var chatScreenshotProtectionEnabled by remember {
                                    mutableStateOf(AppSecurityManager.isChatScreenshotProtectionEnabled(context))
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

                                // 1b. Sprečavanje skrinšotovanja konverzacija (Prevent Conversation Screenshots)
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    contentPadding = PaddingValues(14.dp)
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        SettingsSwitchRow(
                                            title = strings.chatScreenshotProtectionTitle,
                                            subtitle = strings.chatScreenshotProtectionSub,
                                            checked = chatScreenshotProtectionEnabled,
                                            onCheckedChange = { enabled ->
                                                chatScreenshotProtectionEnabled = enabled
                                                AppSecurityManager.setChatScreenshotProtectionEnabled(context, enabled)
                                            }
                                        )
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

        pendingConfirmUpdateRelease?.let { releaseToConfirm ->
            val isCellular = remember(context, releaseToConfirm) {
                AppUpdateManager.isCellularConnection(context)
            }
            GlassAlertDialog(
                onDismissRequest = { pendingConfirmUpdateRelease = null },
                accentBorderColor = if (isCellular) palette.cautionColor else palette.safeColor,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = TxturaIcons.ClockTimer,
                            contentDescription = null,
                            tint = if (isCellular) palette.cautionColor else palette.safeColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.updateConfirmDialogTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.textPrimary
                        )
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = strings.updateConfirmVersionInfo(appVersionName, releaseToConfirm.versionTag),
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    (if (isCellular) palette.cautionColor else palette.safeColor).copy(alpha = 0.14f)
                                )
                                .border(
                                    0.8.dp,
                                    (if (isCellular) palette.cautionColor else palette.safeColor).copy(alpha = 0.45f),
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = strings.updateNetworkTypeLabel(isCellular),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCellular) palette.cautionColor else palette.safeColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = strings.updateConfirmNetworkPrompt(isCellular),
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                            lineHeight = 18.sp
                        )
                        if (releaseToConfirm.releaseNotes.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(palette.glassSurfaceElevated)
                                    .border(0.8.dp, palette.glassBorderTop.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = releaseToConfirm.releaseNotes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textPrimary.copy(alpha = 0.9f),
                                    maxLines = 6,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val targetRelease = releaseToConfirm
                            pendingConfirmUpdateRelease = null
                            if (downloadProgressPct == null) {
                                coroutineScope.launch {
                                    downloadProgressPct = 0
                                    AppUpdateManager.downloadAndInstallUpdate(
                                        context = context,
                                        releaseInfo = targetRelease,
                                        onProgress = { pct -> downloadProgressPct = pct },
                                    )
                                    downloadProgressPct = null
                                }
                            }
                        }
                    ) {
                        Text(
                            text = strings.updateConfirmProceedButton,
                            color = palette.safeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingConfirmUpdateRelease = null }) {
                        Text(
                            text = strings.cancel,
                            color = palette.textSecondary
                        )
                    }
                }
            )
        }
    }
    }
}

@Composable
private fun FlashAlertSettingsCard(
    flashAlertEnabled: Boolean,
    onToggleFlashAlert: (Boolean) -> Unit,
    flashAlertMode: com.txtura.data.FlashAlertMode,
    onSelectFlashAlertMode: (com.txtura.data.FlashAlertMode) -> Unit,
    strings: AppStrings
) {
    val palette = LocalGlassPalette.current
    val view = LocalView.current
    val context = LocalContext.current
    val isExpressive = LocalExpressiveAnimations.current
    val activeDropdown = LocalActiveDropdownId.current
    val dropdownKey = "selector_flash_alert_mode"
    val menuExpanded = activeDropdown.value == dropdownKey

    val handleToggle: (Boolean) -> Unit = { newChecked ->
        TxturaHaptics.toggle(view, context, newChecked)
        onToggleFlashAlert(newChecked)
    }

    val options = remember(strings) {
        listOf(
            com.txtura.data.FlashAlertMode.ALWAYS to strings.flashAlertModeAlways,
            com.txtura.data.FlashAlertMode.VIBRATE_ONLY to strings.flashAlertModeVibrate,
            com.txtura.data.FlashAlertMode.SILENT_ONLY to strings.flashAlertModeSilent,
            com.txtura.data.FlashAlertMode.SOUND_ONLY to strings.flashAlertModeSound
        )
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevated = flashAlertEnabled,
        accentHighlight = if (flashAlertEnabled) palette.accentPrimary else null,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { handleToggle(!flashAlertEnabled) }
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.flashAlertTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (strings.flashAlertSub.isNotBlank()) {
                        OverflowTooltipText(
                            text = strings.flashAlertSub,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (flashAlertEnabled) palette.accentPrimary else palette.textSecondary,
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Switch(
                    checked = flashAlertEnabled,
                    onCheckedChange = handleToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = if (palette.isDark) Color.Black else Color.White,
                        checkedTrackColor = palette.accentPrimary,
                        uncheckedThumbColor = palette.textSecondary,
                        uncheckedTrackColor = palette.glassSurfaceElevated
                    )
                )
            }

            AnimatedVisibility(
                visible = flashAlertEnabled,
                enter = TxturaMotion.verticalExpandEnter(isExpressive = isExpressive),
                exit = TxturaMotion.verticalCollapseExit(isExpressive = isExpressive)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.8.dp)
                            .background(palette.glassBorderTop.copy(alpha = 0.28f))
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                activeDropdown.value = if (activeDropdown.value == dropdownKey) null else dropdownKey
                            }
                            .padding(vertical = 2.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.flashAlertModeTitle,
                                style = MaterialTheme.typography.bodyLarge,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = flashAlertMode.getDisplayName(strings.isSerbian),
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary,
                                lineHeight = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Box {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = flashAlertMode.getShortDisplayName(strings.isSerbian),
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
                                    val isSelected = value == flashAlertMode
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
                                            onSelectFlashAlertMode(value)
                                            activeDropdown.value = null
                                        }
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
    val view = LocalView.current
    val palette = LocalGlassPalette.current
    var simMessages by remember { mutableStateOf<List<SimCardMessage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val activity = remember(context) {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return@remember ctx
            ctx = ctx.baseContext
        }
        null
    }
    val chatScreenshotProtectionEnabled by AppSecurityManager.chatScreenshotProtectionEnabled.collectAsState()
    DisposableEffect(activity, view, chatScreenshotProtectionEnabled) {
        val dialogWindow = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        if (chatScreenshotProtectionEnabled) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            dialogWindow?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            if (chatScreenshotProtectionEnabled) {
                dialogWindow?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

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
private fun MessageSwipeDirectionInteractiveCard(
    title: String,
    currentAction: MessageSwipeAction,
    defaultFallbackAction: MessageSwipeAction,
    strings: AppStrings,
    onSelectAction: (MessageSwipeAction) -> Unit
) {
    val palette = LocalGlassPalette.current
    val enabled = currentAction != MessageSwipeAction.NONE
    val activeDropdown = LocalActiveDropdownId.current
    val dropdownKey = "msg_swipe_${title.hashCode()}"
    val expanded = activeDropdown.value == dropdownKey
    val selectableActions = remember {
        MessageSwipeAction.entries.filter { it != MessageSwipeAction.NONE }
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
                        text = messageSwipeActionLabel(currentAction, strings),
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
                            onSelectAction(MessageSwipeAction.NONE)
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
                                val actionTint = when (action) {
                                    MessageSwipeAction.HIGHLIGHT -> palette.cautionColor
                                    MessageSwipeAction.DELETE -> palette.dangerColor
                                    else -> palette.accentPrimary
                                }
                                val actionIcon = when (action) {
                                    MessageSwipeAction.HIGHLIGHT -> TxturaIcons.HighlightStar
                                    MessageSwipeAction.DELETE -> TxturaIcons.Delete
                                    else -> TxturaIcons.PinOutline
                                }
                                GlassCard(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    elevated = selected,
                                    accentHighlight = if (selected) actionTint else null,
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
                                                imageVector = actionIcon,
                                                contentDescription = null,
                                                tint = if (selected) actionTint else palette.textSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(7.dp))
                                            Text(
                                                text = messageSwipeActionLabel(action, strings),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 11.sp,
                                                lineHeight = 13.sp,
                                                color = if (selected) actionTint else palette.textPrimary.copy(alpha = 0.88f),
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

private fun messageSwipeActionLabel(action: MessageSwipeAction, strings: AppStrings): String {
    return when (action) {
        MessageSwipeAction.PIN -> strings.swipeActionPin
        MessageSwipeAction.HIGHLIGHT -> strings.swipeActionHighlight
        MessageSwipeAction.DELETE -> strings.swipeActionDelete
        MessageSwipeAction.NONE -> strings.swipeActionNone
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
                        versionName = "v3.8",
                        releaseDate = if (strings.isSerbian) "14.11.2026. • Trenutno izdanje" else "14.11.2026. • Current Release",
                        isCurrent = true,
                        categories = listOf(
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Dodate opcije" else "Added Options",
                                items = listOf(
                                    if (strings.isSerbian) "Direktno otvaranje pregleda zaštite klikom na štit u zaglavlju razgovora i u podnožju podešavanja: Klik na zeleni štit u zaglavlju razgovora ili na tekst „Zaštićena SMS/MMS aplikacija“ u podnožju podešavanja direktno otvara prozor pregleda ugrađenih bezbednosnih mera (Security Configuration Summary)" else "Direct Security Overview Access from Conversation Header Shield & Settings Footer: Tapping the green security shield in the conversation header or the 'Protected SMS/MMS' badge in the settings footer opens the Security Configuration Summary dialog",
                                    if (strings.isSerbian) "Zaseban ekran podešavanja „Pokreti prevlačenja“ (Swipe gestures): Sve 4 opcije prevlačenja (svajp ulevo i udesno na listi konverzacija i prozorčićima poruka) smeštene su u zaseban podmeni unutar podešavanja Razgovor i gestovi uz namensko zaglavlje i navigaciju nazad" else "Dedicated 'Swipe gestures' Sub-Menu Screen: Grouped all 4 swipe actions (left and right swipes for conversation list and message bubbles) into a dedicated glass sub-menu screen in Chat & Gestures settings with seamless back navigation",
                                    if (strings.isSerbian) "Ujednačene širine kategorija sa srazmernim širenjem nepročitanih: Širine kategorija na početnom ekranu su ujednačene; kada pristignu nepročitane poruke, kategorija Nepročitano se dinamički širi a ostale tri srazmerno skupljaju bez prelivanja i horizontalnog skrolovanja, a po čitanju se vraćaju u ravnomernu širinu" else "Dynamic Category Widths & Proportional Unread Expansion: Category chips on the home screen have equal widths; when unread messages arrive, Unread expands dynamically while the other three categories shrink proportionally to match search bar width without horizontal overflow, reverting to equal widths when read",
                                    if (strings.isSerbian) "Kategorija „Sve“ (All) po defaultu na prvom mestu: Kategorija „Sve“ je podrazumevano prva na početnom ekranu (Sve, Ljudi, Servisi, Nepročitano), uz potpuno očuvanje korisničkog redosleda prevlačenjem (drag-and-drop)" else "'All' Category First by Default: The 'All' category is displayed first by default on the home screen (All, People, Services, Unread), with custom drag-and-drop reordering fully preserved",
                                    if (strings.isSerbian) "Opcija „Obriši“ (Trash) za svajp na prozorčić poruke uz dijalog za potvrdu: U podešavanjima svajpa ulevo i udesno na prozorčić poruke dodata je opcija premeštanja poruke u obrisane poruke (Trash) uz prikaz prozora za potvrdu pre brisanja" else "Message Bubble Swipe 'Trash' Option with Confirmation Dialog: Added the 'Trash' option to Message Bubble Swipe Right and Swipe Left settings with a confirmation prompt before moving the message to Deleted messages",
                                    if (strings.isSerbian) "Pametno prepoznavanje poziva na broj, iznosa za uplatu, rokova za kalendar, brojeva pošiljki i brzo slanje SMS-a: Automatska detekcija i brzi čipovi u porukama za kopiranje poziva na broj (uz prikaz modela, npr. Model 97), kopiranje tačnog iznosa računa/transakcije za m-banking, dodavanje roka za uplatu ili termina direktno u kalendar telefona, kopiranje broja pošiljke (Post Express / kurirske službe) i opcija „Pošalji SMS poruku“ pri kliku na prepoznat broj telefona" else "Smart Payment Reference, Amount, Calendar Deadline, Package Tracking & Send SMS Actions: Automatic detection and quick-action chips in messages for payment reference numbers (with model), payment/transaction amounts, calendar deadline reminders, package tracking numbers, and a 'Send SMS message' option on detected phone numbers",
                                    if (strings.isSerbian) "Potvrda pre označavanja štetne poruke kao bezbedne: Dijalog za potvrdu koji pita korisnika da li zaista želi da označi upozorenu poruku kao bezbednu pre primene izuzetka (Harmful message override)" else "Harmful Message Override Confirmation Dialog: Confirmation prompt asking the user to verify before marking a flagged message as safe",
                                    if (strings.isSerbian) "Trenutni zvuk „poslato.wav“ na pritisak tastera Pošalji: Zvučni efekat res/raw/poslato.wav koji se reprodukuje odmah čim korisnik pritisne dugme za slanje poruke" else "Instant 'poslato.wav' Sound on Send Button Tap: Plays res/raw/poslato.wav immediately when the Send button is pressed",
                                    if (strings.isSerbian) "Trajno čuvanje zakazanih poruka i obnavljanje pri pokretanju uređaja: Zakazane SMS i MMS poruke se trajno čuvaju u memoriji i automatski nastavljaju odbrojavanje nakon ponovnog pokretanja aplikacije ili restarta telefona" else "Persistent Scheduled Messages & Boot Restoration: Scheduled SMS and MMS messages persist across app restarts and device reboots via BootCompletedReceiver",
                                    if (strings.isSerbian) "Dijalozi za potvrdu za akcije u korpi i direktan ulazak u pregled: Dodati dijalozi za potvrdu pre vraćanja konverzacije iz korpe, kao i pre vraćanja ili trajnog brisanja izabranih poruka u prozoru pregleda, dok se u pregled poruka ulazi direktno klikom na samu karticu (ili avatar) kao i preko dugmeta „Odaberi“" else "Confirmation Dialogs for Trash Actions & Direct Inspection on Card Tap: Added confirmation prompts before restoring a thread from Trash, as well as before restoring or permanently deleting selected messages in the inspection dialog, while tapping the conversation card (or avatar) or the 'Choose' button enters inspection directly"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Prepravljene opcije" else "Modified & Fixed Options",
                                items = listOf(
                                    if (strings.isSerbian) "Čisto ime kontakta u zaglavlju pregleda obrisanih poruka: Uklonjen suvišni prefiks „Obrisane poruke • “ iz naslova prozora pregleda, tako da se u tom redu prikazuje isključivo ime kontakta ili broj telefona" else "Clean Contact Name in Trash Inspection Header: Removed the redundant 'Deleted messages • ' window title prefix, displaying exclusively the contact's name or phone number",
                                    if (strings.isSerbian) "Uklonjene suvišne prečice u listi obrisanih poruka: Uklonjene pojedinačne ikonice za vraćanje i brisanje sa svake poruke u prozoru pregleda radi sprečavanja slučajnih dodira i veće preglednosti, uz jedinstvenu kontrolu preko donjih dugmadi" else "Streamlined Message Cards in Trash Inspection Dialog: Removed the inline restore and delete shortcut icon buttons from individual message rows to prevent accidental taps and maximize reading space, keeping action control unified under the bottom batch action buttons",
                                    if (strings.isSerbian) "Očuvanje pozicije liste razgovora pri povratku iz razgovora: Povratak iz otvorenog razgovora na listu konverzacija vraća na tačno mesto (i izabranu kategoriju) gde se lista nalazila umesto automatskog skrolovanja na vrh" else "Preserved Conversation List Scroll Position on Back Navigation: Navigating back from an open conversation returns to the exact scroll position and active category filter instead of resetting to the top of the list",
                                    if (strings.isSerbian) "Namenski zvuk „dolazna.wav“ u razgovoru i ton obaveštenja van razgovora: Zvučni efekat res/raw/dolazna.wav reprodukuje se isključivo dok je korisnik u prozoru konverzacije, dok se van konverzacije reprodukuje izabrani ton obaveštenja poruke" else "Dedicated In-Chat 'dolazna.wav' Sound vs. External Notification Tone: res/raw/dolazna.wav plays strictly while the user is inside an open conversation window, while incoming messages received outside the conversation play the standard message notification tone",
                                    if (strings.isSerbian) "Precizan detektor brojeva telefona, datuma, poziva na broj, linkova i skraćenica: Otklonjeno pogrešno prepoznavanje datuma i vremena (npr. 05/10/2026 17:42:27), poziva na broj i brojeva unutar URL linkova kao brojeva telefona, kao i sprečeno prepoznavanje skraćenica poput „br.kartice*5431“ kao veb-linkova" else "Precision Phone Number & Link Detector Filtering: Eliminated false-positive phone number detections on dates/timestamps, payment references, and numeric substrings inside URLs, and prevented abbreviations like 'br.kartice*5431' from being linkified as URLs",
                                    if (strings.isSerbian) "Bezbedan FileProvider uz podršku za direktno GitHub APK ažuriranje: Ograničene putanje deljenja fajlova (file_paths.xml) isključivo na namenske podfascikle uz zadržanu podršku za direktno preuzimanje i instalaciju ažuriranja sa GitHub-a" else "Scoped FileProvider Security & Direct GitHub APK Updates: Restricted file_paths.xml strictly to dedicated subdirectories while retaining direct in-app GitHub APK updates",
                                    if (strings.isSerbian) "Standardizovano MMS slanje i primanje govornih poruka i dokumenata: Unapređen MmsPduHelper po OMA MMS standardu sa korenskim SMIL delom, ispravnim WSP zaglavljima, trajnim čuvanjem govornih poruka, prepoznavanjem svih audio formata i .txt priloga, uz podrazumevani SLOBODAN (300 KB) MMS režim" else "OMA-Compliant MMS PDU Encoding, Voice Messages & Document Attachments: Upgraded MmsPduHelper with root SMIL presentation, WSP Value-length headers, persistent voice message storage, multi-codec audio & .txt attachment parsing, and FREE (300 KB) MMS mode by default",
                                    if (strings.isSerbian) "Trenutno asinhrono slanje poruka i serijsko učitavanje MMS-a: Slanje SMS i MMS poruka više ne zadržava glavni ekran (kompresija slike i kreiranje PDU-a rade u pozadini), a učitavanje MMS delova pri pokretanju izvršava se jednim grupnim upitom" else "Non-Blocking Asynchronous SMS/MMS Send & Batch MMS Loading: Outgoing messages appear instantaneously while image compression and PDU creation run on background threads, and MMS parts load via a single batched query",
                                    if (strings.isSerbian) "Aktivna primena blokiranih pošiljalaca, MMS potvrda čitanja i režima kreiranja: Ručno blokirani brojevi su uvek blokirani nezavisno od spam filtera, slanje potvrda o čitanju MMS-a i režimi kreiranja (Ograničeno 100 KB / Upozorenje / Slobodno 300 KB) aktivno se primenjuju, a ručno preuzimanje MMS-a čuva tačnu SIM karticu" else "Active Enforcement of Blocked Senders, MMS Read Reports & Creation Modes: Manually blocked numbers are always blocked, MMS read report status and creation mode size limits are actively enforced, and manual MMS download preserves the target SIM subId",
                                    if (strings.isSerbian) "Ograničen svajp na listi konverzacija prema širini teksta opcije: Prevlačenje kartice konverzacije ulevo i udesno na glavnom prozoru ograničeno je tačno na širinu potrebnu da se prikaže cela ikonica i naziv opcije" else "Exact-Width Conversation List Swipe Clamping: Left and right swipe distance on the main conversation list is dynamically clamped to the exact width needed to reveal the full action icon and text",
                                    if (strings.isSerbian) "Zabrana skrinšotovanja na svim ekranima sa sadržajem poruka: Kada je uključeno sprečavanje skrinšotovanja, blokirano je slikanje i snimanje ekrana (FLAG_SECURE) na listi konverzacija, u razgovorima, istaknutim porukama, obrisanim porukama (korpi) i porukama na SIM kartici" else "Comprehensive Message Content Screenshot Blocking: When screenshot protection is enabled, FLAG_SECURE blocks screenshots and screen recordings across the conversation list, chats, highlighted messages, deleted messages (Trash), and SIM card messages"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Uklonjene opcije" else "Removed Options",
                                items = listOf(
                                    if (strings.isSerbian) "Usluga Over-The-Air lokalizacije (OtaLocalizationManager): Potpuno uklonjena klasa OtaLocalizationManager.kt, PendingLocaleWorker.kt i sve poveznice sa udaljenim preuzimanjem prevoda" else "OTA Localization Service (OtaLocalizationManager): Completely removed OtaLocalizationManager.kt, PendingLocaleWorker.kt, and all remote localization references",
                                    if (strings.isSerbian) "Zvučni efekat „odlazna.wav“: Uklonjena datoteka res/raw/odlazna.wav u korist zvuka res/raw/poslato.wav pri slanju poruke" else "'odlazna.wav' Outgoing Sound File: Removed unused res/raw/odlazna.wav in favor of res/raw/poslato.wav when pressing Send"
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
                        versionName = "v3.7",
                        releaseDate = "13.11.2026.",
                        isCurrent = false,
                        categories = listOf(
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Dodate opcije" else "Added Options",
                                items = listOf(
                                    if (strings.isSerbian) "Svajp ulevo i udesno na prozorčić poruke: Podesive opcije prevlačenja ulevo i udesno direktno na oblačić poruke u razgovoru za (Un)pin i (Un)highlight, uz prikaz ikonice iznad naziva opcije i kompaktan razmak pored oblačića" else "Message Bubble Swipe Left & Right: Configurable left and right swipe gestures directly on individual message bubbles for (Un)pin and (Un)highlight, with the icon placed above the label and a compact gap next to the bubble",
                                    if (strings.isSerbian) "Režimi aktivacije svetlosnog odziva: Izbor uslova pod kojima se uključuje blic (Uvek, U režimu samo vibracija, U režimu nečujno, U režimu zvuk)" else "Light Feedback Activation Modes: Selectable ringer-mode conditions for camera flash alerts (Always, In vibrate mode, In silent mode, In sound mode)",
                                    if (strings.isSerbian) "Direktno ažuriranje sa bilo koje starije verzije i preko svake mreže: Podrška za direktan prelazak sa bilo koje prethodne verzije na najnoviju GitHub verziju i preuzimanje preko Wi-Fi ili mobilnih podataka uz dijalog za potvrdu korisnika" else "Universal Direct Update & Any-Network Confirmation: Direct update from any previous version to the newest GitHub release and downloading over Wi-Fi or Mobile Data with user confirmation",
                                    if (strings.isSerbian) "Trenutna pozadinska obaveštenja o ažuriranju: Pozadinska provera (WorkManager i mrežni osluškivač) koja šalje sistemsko obaveštenje čim se detektuje nova verzija, bez čekanja na ulazak u aplikaciju" else "Immediate Background Update Notifications: Background WorkManager and connectivity checks that send a notification as soon as a new release is detected without waiting for app launch",
                                    if (strings.isSerbian) "Označavanje poruke da nije štetna: Mogućnost označavanja upozorenih poruka kao bezbednih („Označi da nije štetna“), dostupna isključivo na porukama koje su detektovane kao štetne" else "Mark Harmful Message as Safe: Option to mark flagged messages as not harmful ('Mark as not harmful'), available strictly on messages detected as harmful",
                                    if (strings.isSerbian) "Sprečavanje skrinšotovanja konverzacija: On/Off prekidač u podešavanjima Privatnost i bezbednost (ispod kartice Blokirani pošiljaoci) koji zabranjuje slikanje i snimanje ekrana na listi konverzacija i u samim konverzacijama" else "Conversation Screenshot Protection: On/Off toggle in Privacy & Security settings (below Blocked Senders) that blocks screenshots and screen recordings on the conversation list and inside conversations"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Prepravljene opcije" else "Modified & Fixed Options",
                                items = listOf(
                                    if (strings.isSerbian) "Pretraga u glavnom meniju i razgovoru sa skrolovanjem i signalizacijom: Klik na konverzaciju u pretrazi glavnog menija i pretraga u okviru razgovora (isključivo za tu konverzaciju) automatski skroluju do pronađene poruke i vizuelno je signaliziraju oznakom „Pronađena poruka“, svetlećim okvirom i obojenim pojmom" else "Main Menu & In-Chat Search Scroll and Visual Signaling: Tapping a conversation from main menu search or searching inside a chat (scoped strictly to that conversation) automatically scrolls to the matched message and highlights both the bubble and search term",
                                    if (strings.isSerbian) "Navigacija sa upozorenja o štetnoj poruci na glavnom meniju: Klik na crveno upozorenje u pregledu konverzacije direktno otvara razgovor, učitava istoriju, skroluje do štetne poruke i vizuelno je ističe uz opciju da se označi da nije štetna" else "Harmful Message Warning Deep Navigation: Clicking the red warning in the main menu conversation preview opens the chat directly at the harmful message, scrolls to it, and highlights it with the option to mark it as not harmful",
                                    if (strings.isSerbian) "Objedinjeno podešavanje svetlosnog odziva i raspored haptike: Prekidač svetlosnog odziva i izbor režima aktivacije spojeni su u jednu karticu, a podešavanje „Haptički odzivi“ je premešteno iznad „Svetlosnog odziva“" else "Consolidated Light Feedback Card & Haptics Reordering: Merged the light feedback toggle and activation mode selector into one card, and moved Haptic feedback directly above Light feedback",
                                    if (strings.isSerbian) "Boja vremenske odrednice u razgovoru: Boja plutajuće vremenske odrednice (datuma) u četu usklađena je sa bojom strelice nadole koja vraća konverzaciju na najskorije poruke" else "Conversation Date Pill Color Alignment: Matched the floating conversation date header color and border to the scroll-to-bottom down-arrow button",
                                    if (strings.isSerbian) "Stabilnost polja za unos i zatvaranja priloga (+ / x): Fiksiran horizontalni raspored ikonica pri višelinijskom kucanju bez prekrivanja mikrofona i otklonjen vizuelni ispad pri dodirivanju ikonice x za zatvaranje dodatnih kategorija datoteka" else "Composer Multi-Line & Attachment Close (+ / x) Stability: Fixed horizontal icon alignment during multi-line entry without overlapping the microphone button and eliminated layout jumping when closing extra attachment categories",
                                    if (strings.isSerbian) "Jednolinijski naslovi dijaloga i nazivi dugmadi: Dijalog „Podrazumevana SMS i MMS aplikacija“ i dijalozi potvrde prikazuju ceo naslov u jednom redu i pune nazive dugmadi („Ne sada“ i „Podesi kao podrazumevano“) u istom redu, uz skraćene tastere u korpi („Odaberi“ i „Vrati“)" else "Single-Line Dialog Titles & Concise Action Labels: Full single-row title and horizontal button labels for the default SMS/MMS dialog and confirmation prompts, with concise Choose and Restore buttons in Trash",
                                    if (strings.isSerbian) "Unapred učitani zvučni efekti (SoundPool): Zvučni motor unapred učitava svih 19 .wav efekata iz res/raw bez kašnjenja (forceReload) i osvežava keš ritma blica" else "Preloaded SoundPool Audio Engine: Preloads all 19 custom .wav sound effects from res/raw into SoundPool with forceReload support and refreshes flash rhythm caches"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Uklonjene opcije" else "Removed Options",
                                items = listOf(
                                    if (strings.isSerbian) "Zasebna kartica „Kada aktivirati svetlosni odziv“: Uklonjena kao posebna kartica jer je izbor režima ugrađen direktno u karticu „Svetlosni odziv“" else "Separate 'When to activate light feedback' Card: Removed as a standalone settings card after integrating its selector into the Light feedback card",
                                    if (strings.isSerbian) "Tekstualni natpis „• Protected conversation“ u zaglavlju razgovora: Uklonjen tekst pored broja telefona u zaglavlju četa — zadržan je samo zeleni štit pre broja kao indikator bezbednosti" else "'• Protected conversation' Header Text: Removed the text suffix next to the phone number in the conversation header, keeping only the green shield icon before the number"
                                )
                            )
                        )
                    )

                    ChangelogVersionSection(
                        versionName = "v3.6",
                        releaseDate = "12.11.2026.",
                        isCurrent = false,
                        categories = listOf(
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Dodate opcije" else "Added Options",
                                items = listOf(
                                    if (strings.isSerbian) "Preusmeravanje za više SIM kartica i eSIM: Dinamičko slanje i prijem SMS/MMS poruka sa aktivnog fizičkog ili eSIM slota uz SubscriptionManager" else "Multi-SIM & eSIM Subscription Routing: Dynamic SMS/MMS sending and receiving from the active physical SIM or eSIM slot via SubscriptionManager",
                                    if (strings.isSerbian) "Centralizovani MmsPayloadValidator (do 300 KB) i IANA MIME mapiranje: Provera veličine MMS priloga uz podršku za kancelarijske dokumente, tabele, prezentacije, beleške, audio, kontakte i kalendar, kao i provera aktivnih mobilnih podataka (FAILED_NO_DATA)" else "Centralized MmsPayloadValidator (300 KB) & IANA MIME Mapping: Strict 300 KB PDU validation, lossless MIME mapping for office/media/contact/calendar files, and cellular data pre-flight check (FAILED_NO_DATA)",
                                    if (strings.isSerbian) "Over-The-Air lokalizacija i teme Espreso i Lavanda: Pozadinska sinhronizacija prevoda (PendingLocaleWorker) i nove teme Espreso krem, Monohrom espreso, Lavanda sanja i Monohrom lavanda sa teksturama zrna kafe i latica lavande" else "OTA Localization & Espresso / Lavender Themes: Background PendingLocaleWorker translation sync and new Espresso and Lavender themes with procedural coffee bean and lavender petal textures"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Prepravljene opcije" else "Modified & Fixed Options",
                                items = listOf(
                                    if (strings.isSerbian) "EXIF orijentacija slika i Compose stabilnost (@Immutable): Ispravljena rotacija portretnih MMS fotografija pre kompresije, modeli označeni sa @Immutable i uključen StrictMode u debug režimu" else "EXIF Image Orientation & Compose @Immutable Stability: Preserved portrait photo rotation before MMS compression, annotated UI models with @Immutable, and enabled debug StrictMode",
                                    if (strings.isSerbian) "Glatka animacija poništavanja odbrojavanja slanja: Prepravljen prelaz tastera za slanje/otkazivanje (AnimatedContent) sa odlaganjem vraćanja nacrta od 250ms bez skakanja rasporeda" else "Synchronized Send Countdown Cancel Animation: Refactored send/cancel slot with AnimatedContent and a 250ms draft restoration delay to prevent layout jumps",
                                    if (strings.isSerbian) "Automatski prikaz SMSC broja: Kartica SMSC centra prikazuje broj automatski očitan sa SIM kartice ili mreže operatera" else "Automatic SMSC Display: SMSC settings card automatically displays the carrier/SIM-detected service center number"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Uklonjene opcije" else "Removed Options",
                                items = listOf(
                                    if (strings.isSerbian) "Ručne APN kartice i zastareli prekidači mobilnih podataka: Uklonjeni iz podešavanja mrežnih servisa u korist automatskog rutiranja" else "Manual Carrier APN Cards & Legacy Mobile Data Toggles: Removed from Messaging & Network settings in favor of automatic carrier routing",
                                    if (strings.isSerbian) "Periodični 3-minutni MMS servis i naknadno transkodovanje zvuka: Uklonjeno 3-minutno pozadinsko prozivanje (zamenjeno WAP_PUSH_DELIVER prijemom u realnom vremenu) i uklonjeno naknadno audio transkodovanje (zamenjeno direktnim AMR-NB snimanjem)" else "3-Minute MMS Polling & Offline Audio Transcoding: Removed periodic 3-minute polling in favor of real-time WAP_PUSH_DELIVER broadcasts and replaced offline audio transcoding with direct native AMR-NB recording"
                                )
                            )
                        )
                    )

                    ChangelogVersionSection(
                        versionName = "v3.5",
                        releaseDate = "12.11.2026.",
                        isCurrent = false,
                        categories = listOf(
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Dodate opcije" else "Added Options",
                                items = listOf(
                                    if (strings.isSerbian) "Automatska detekcija MMS parametara prema SMSC broju i SIM operateru: Automatsko prepoznavanje MMSC URL-a, proksija i porta za regionalne i svetske operatere (mts, Yettel, A1, Globaltel, HT Eronet, BH Telecom, m:tel, Hrvatski Telekom, Telekom Slovenije, T-Mobile, Vodafone, AT&T, Verizon)" else "SMSC & SIM Operator MMS Auto-Detection: Automatic resolution of carrier MMSC URL, proxy, and port based on SMSC number and SIM operator",
                                    if (strings.isSerbian) "Provera #!AMR\\n zaglavlja glasovnih poruka (AmrAudioConverter): Validacija standardnog AMR-NB formata glasovnih zapisa pre MMS slanja" else "AMR-NB Voice Header Validator (AmrAudioConverter): Validates standard #!AMR\\n header compliance for voice recordings prior to MMS transmission"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Prepravljene opcije" else "Modified & Fixed Options",
                                items = listOf(
                                    if (strings.isSerbian) "Podrazumevane vrednosti zaštite pri prvoj instalaciji: Napredne opcije (lokalni fajervol, šifrovanje baze, SSL pinning, minimizacija metapodataka, root provera) podrazumevano su isključene pri prvoj instalaciji, dok je detekcija zlonamernih sajtova uključena" else "First-Install Security Defaults: Optional advanced security toggles start disabled on clean installs while malicious website detection starts enabled",
                                    if (strings.isSerbian) "Statična kartica eksternih linkova i jednolinijski naslovi podešavanja: Naslovi kartica u podešavanjima prikazuju se u jednom redu uz stabilan raspored kartice za eksterne linkove" else "Static External Links Card & Single-Line Settings Titles: Single-line titles across settings cards and static layout for the External Links card"
                                )
                            )
                        )
                    )

                    ChangelogVersionSection(
                        versionName = "v3.4",
                        releaseDate = "12.11.2026.",
                        isCurrent = false,
                        categories = listOf(
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Dodate opcije" else "Added Options",
                                items = listOf(
                                    if (strings.isSerbian) "Direktno preuzimanje i pokretanje instalacije APK-a iz aplikacije: Preuzimanje GitHub izdanja sa prikazom napretka u procentima i automatskim otvaranjem Android instalatera bez eksternog pretraživača" else "In-App GitHub APK Downloader & Installer: Direct APK download from GitHub Releases with live percentage progress and native package installer launch"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Prepravljene opcije" else "Modified & Fixed Options",
                                items = listOf(
                                    if (strings.isSerbian) "Animacija opruge pri kačenju i otkačinjanju razgovora: Glatka spring animacija premeštanja razgovora na vrh ili povratka na hronološko mesto uz zadržavanje pozicije skrolovanja" else "Pin & Unpin Spring Placement Motion: Smooth spring placement animation when pinning or unpinning conversations while preserving scroll position",
                                    if (strings.isSerbian) "Podrazumevana tema i pozadina: Sistemska tema i Starlight Galaxy pozadina postavljene kao podrazumevani izbor pri instalaciji" else "Default Theme & Wallpaper Presets: System theme mode and Starlight Galaxy wallpaper set as clean-install defaults"
                                )
                            )
                        )
                    )

                    ChangelogVersionSection(
                        versionName = "v3.3.1",
                        releaseDate = "12.11.2026.",
                        isCurrent = false,
                        categories = listOf(
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Dodate opcije" else "Added Options",
                                items = listOf(
                                    if (strings.isSerbian) "Selektivni pregled, vraćanje i brisanje poruka u korpi: Pojedinačni izbor obrisanih poruka unutar razgovora u korpi za vraćanje u sanduče ili trajno brisanje, uz komande Označi sve / Odznači sve" else "Selective Trash Message Inspection & Recovery: Inspect deleted threads in Trash and select individual messages to restore or permanently delete with Select All / Deselect All controls",
                                    if (strings.isSerbian) "Brojač nepročitanih poruka na kategoriji: Kategorija „Nepročitano“ prikazuje ukupan broj nepročitanih poruka u realnom vremenu (npr. Nepročitano (3))" else "Unread Category Counter Badge: The Unread category chip displays the live count of unread messages (e.g., Unread (3))",
                                    if (strings.isSerbian) "Ikonica medveda za Burgundi teme: Prilagođena ikonica u glavnom zaglavlju za teme Burgundi Anita i Monohrom sa burgundi akcentom uz automatsku prilagodljivost svetlom i tamnom režimu" else "Burgundy Theme Header Branding: Custom bear icon rendered in the main header for Burgundy Anita and Monochrome Burgundy themes with light/dark mode adaptation"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Prepravljene opcije" else "Modified & Fixed Options",
                                items = listOf(
                                    if (strings.isSerbian) "Datumski zaglavci na srpskoj latinici: Datumski razdelnici u razgovoru prikazuju se isključivo na srpskoj latinici (npr. utorak, 19. maj) umesto ćirilice" else "Serbian Latin Conversation Date Headers: Conversation date dividers always format in Serbian Latin script (e.g., utorak, 19. maj)",
                                    if (strings.isSerbian) "Razdvajanje skrolovanja od dugog pritiska: Skrolovanje preko oblačića poruke nikada ne pokreće skupljanje ili slučajno označavanje (označavanje zahteva stacionarni pritisak od 500ms)" else "Scroll Gesture Isolation from Long-Press Selection: Scrolling over message bubbles never triggers press scaling or accidental selection (requires a stationary 500ms long-press)",
                                    if (strings.isSerbian) "Sprečeno lažno okidanje zvuka i blica pri ulasku u razgovor: Otvaranje postojećeg razgovora i učitavanje istorije više ne aktivira zvučni niti svetlosni signal dolazne poruke" else "Suppressed False Alert Sounds on Thread Open: Opening a thread or loading message history no longer triggers incoming message sounds or flash alerts"
                                )
                            )
                        )
                    )

                    ChangelogVersionSection(
                        versionName = "v3.3",
                        releaseDate = "11.11.2026.",
                        isCurrent = false,
                        categories = listOf(
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Dodate opcije" else "Added Options",
                                items = listOf(
                                    if (strings.isSerbian) "SQLCipher Room šifrovana baza podataka: Skladištenje lokalne istorije razgovora (ConversationHistoryEntity) u Room bazi šifrovanoj pomoću SQLCipher-a uz selektivno učitavanje po razgovoru" else "SQLCipher Room Encrypted Database: Encrypted Room database storage (ConversationHistoryEntity) with SQLCipher and per-thread lazy history loading",
                                    if (strings.isSerbian) "Vidljivost paketa za root proveru: Dodate eksplicitne <queries> deklaracije u AndroidManifest.xml za detekciju root menadžera" else "Explicit Root Package Visibility Queries: Added <queries> declarations in AndroidManifest.xml for accurate root manager detection"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Prepravljene opcije" else "Modified & Fixed Options",
                                items = listOf(
                                    if (strings.isSerbian) "Deduplikacija poruka i kontrola sinhronizacije: Automatsko uklanjanje duplikata poruka po ID-u i sadržaju između lokalne baze i sistemske telefonije" else "Message Deduplication & Sync Throttling: Automated message ID and content deduplication between local snapshots and telephony syncs"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Uklonjene opcije" else "Removed Options",
                                items = listOf(
                                    if (strings.isSerbian) "Zastarela blokirajuća inicijalizacija pri startu: Uklonjeno blokirajuće podizanje repozitorijuma pri hladnom pokretanju u korist asinhrone inicijalizacije" else "Legacy Blocking Startup Initialization: Removed blocking cold-start repository initialization in favor of asynchronous startup loading"
                                )
                            )
                        )
                    )

                    ChangelogVersionSection(
                        versionName = "v3.2",
                        releaseDate = "11.11.2026.",
                        isCurrent = false,
                        categories = listOf(
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Dodate opcije" else "Added Options",
                                items = listOf(
                                    if (strings.isSerbian) "Sistemski pokret za nazad (BackHandler): Podrška za Android gest prevlačenja za nazad na svim ekranima sa istim ponašanjem kao strelica u aplikaciji" else "System Back Gesture Navigation: Added BackHandler support across screens matching in-app back navigation"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Prepravljene opcije" else "Modified & Fixed Options",
                                items = listOf(
                                    if (strings.isSerbian) "Asinhrono pokretanje, v2 migracija ključa keša i pozadinski bekap: Učitavanje podešavanja, migracija Android Keystore ključa keša sa v1 na v2 i PBKDF2 šifrovanje/dešifrovanje bekapa izvršavaju se van glavne niti" else "Non-Blocking Startup, v2 Cache Key Migration & Background Backup Crypto: Preference loading, v1-to-v2 Keystore key migration, and PBKDF2 backup cryptography run off the main thread",
                                    if (strings.isSerbian) "Asinhrona obrada isporuke i WAP Push paketa: Povratni događaji slanja/preuzimanja i provera dolaznih WAP Push MMS paketa izvršavaju se asinhrono" else "Asynchronous SMS/MMS Delivery & WAP Push Handling: Sent/downloaded callbacks and incoming WAP Push validation process asynchronously"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Uklonjene opcije" else "Removed Options",
                                items = listOf(
                                    if (strings.isSerbian) "Dupla ikonica čiode na kartici razgovora: Ikonica čiode na kartici razgovora automatski se skriva kada je kačenje/otkačinjanje već dodeljeno svajp pokretu" else "Redundant Pin Icon on Conversation Cards When Assigned to Swipe: Automatically hides the card pin icon when pin/unpin is already assigned to left or right swipe"
                                )
                            )
                        )
                    )

                    ChangelogVersionSection(
                        versionName = "v3.1",
                        releaseDate = "10.11.2026.",
                        isCurrent = false,
                        categories = listOf(
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Dodate opcije" else "Added Options",
                                items = listOf(
                                    if (strings.isSerbian) "Prilagodljivi donji dok bar: Slobodno premeštanje ikonica na donjem navigacionom baru prevlačenjem i prikaz dok bara na ekranima Obrisane i Hajlajtovane poruke" else "Customizable Drag-and-Drop Dock Bar: Reorder bottom dock icons via drag-and-drop, with the dock visible on Deleted and Highlighted screens",
                                    if (strings.isSerbian) "GIF animacije sa tastature (do 299.9 KB): Direktno slanje i animirana reprodukcija GIF-ova umetnutih sa tastature u oblačiću poruke" else "Keyboard GIF Support up to 299.9 KB: Direct sending and animated in-bubble playback for GIFs inserted from the keyboard"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Prepravljene opcije" else "Modified & Fixed Options",
                                items = listOf(
                                    if (strings.isSerbian) "Fizičko brisanje poruka i fajlova pri pražnjenju korpe: Trajno brisanje uklanja SMS/MMS zapise iz baze uređaja i sve keširane multimedijalne priloge iz interne memorije" else "Physical Storage Purge on Empty Trash: Permanent deletion removes SMS/MMS telephony records and cached media attachments from internal storage",
                                    if (strings.isSerbian) "Dozvola za kalendar pri pokretanju i PCM sinhronizacija blica: Dozvola za kalendar traži se pri inicijalnom pokretanju, a blic treperi u tačnom ritmu PCM audio analize tona obaveštenja" else "Startup Calendar Permission & Audio-Synchronized Flash Timing: Requests calendar permission on initial launch and synchronizes flash pulses with the PCM audio rhythm of the notification tone"
                                )
                            )
                        )
                    )

                    ChangelogVersionSection(
                        versionName = "v3.0",
                        releaseDate = "10.11.2026.",
                        isCurrent = false,
                        categories = listOf(
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Dodate opcije" else "Added Options",
                                items = listOf(
                                    if (strings.isSerbian) "Glasovne poruke u razgovoru: Snimanje glasovnih poruka sa tajmerom, preslušavanjem i otkazivanjem pre slanja" else "In-Chat Voice Messages: Voice message recording with timer, playback preview, and cancellation inside conversations",
                                    if (strings.isSerbian) "Deljenje kalendarskih događaja (.ics), dokumenata i kompresija slika: Birač događaja iz kalendara sa generisanjem .ics fajla, slanje dokumenata i automatska kompresija fotografija za MMS" else "Calendar Event (.ics) Sharing, Document Attachments & Image Compression: Calendar event picker with .ics generation, document attachments, and automatic MMS photo compression",
                                    if (strings.isSerbian) "Obaveštenja blicem kamere: Svetlosna signalizacija blicem kamere za dolazne poruke" else "Camera Flash Notifications: Camera flashlight alerts for incoming messages"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Prepravljene opcije" else "Modified & Fixed Options",
                                items = listOf(
                                    if (strings.isSerbian) "Asinhrono preuzimanje MMS-a i pojedinačne dozvole: Preuzimanje dolaznih MMS priloga u pozadini i traženje dozvola prema funkciji koju korisnik pokrene" else "Asynchronous Incoming MMS Downloads & Feature-Based Permissions: Background MMS attachment downloading and on-demand runtime permission prompts"
                                )
                            )
                        )
                    )

                    ChangelogVersionSection(
                        versionName = "v2.4",
                        releaseDate = "09.11.2026.",
                        isCurrent = false,
                        categories = listOf(
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Dodate opcije" else "Added Options",
                                items = listOf(
                                    if (strings.isSerbian) "6 prilagođenih tonova obaveštenja, zvučni efekti akcija i prečica za sistemski zvuk: Melodije (Dupli puls, Kristalni zvončić, Meki pop, Ritam kod, Topla kalimba, Diskretni oblak) uz trenutni pregled, zvuci za akcije nad porukama i opcija „Podesi zvuk sistema“" else "6 Studio Notification Tones, Action Audio Effects & System Sound Shortcut: 6 custom melodies with instant preview, tactile action sound effects, and a direct shortcut to Android notification channel sound settings",
                                    if (strings.isSerbian) "Selektivna zabrana skrinšotovanja četova (FLAG_SECURE) i budan ekran u razgovoru: Posebno podešavanje koje sprečava snimanje ekrana samo u četovima i listi poruka, i drži ekran uključenim dok je razgovor otvoren" else "Selective Chat Screenshot Protection & Chat Keep-Awake: Dedicated toggle enforcing FLAG_SECURE only on chats and message lists, plus FLAG_KEEP_SCREEN_ON inside active conversations"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Prepravljene opcije" else "Modified & Fixed Options",
                                items = listOf(
                                    if (strings.isSerbian) "Isključivost padajućih menija i brz povratak na glavni ekran: Onemogućeno preklapanje više padajućih menija istovremeno i uklonjeno zadržavanje od 2–3s pri povratku iz razgovora" else "Single Active Dropdown Menu & Fast Return to Main Screen: Enforced single-menu exclusivity and eliminated the 2–3s delay when returning to the conversation list"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Uklonjene opcije" else "Removed Options",
                                items = listOf(
                                    if (strings.isSerbian) "Bezuslovna zabrana skrinšotovanja cele aplikacije i ikonica u polju primaoca: Uklonjena globalna blokada snimanja ekrana u podešavanjima i uklonjena ikonica kontakta unutar polja primaoca radi pune širine" else "Unconditional Whole-App Screenshot Blocking & Recipient Field Contact Icon: Removed global screenshot blocking on settings screens and removed the contact icon inside the recipient input field"
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
                                title = if (strings.isSerbian) "Dodate opcije" else "Added Options",
                                items = listOf(
                                    if (strings.isSerbian) "Haptika pri prevlačenju i promeni rasporeda kategorija: Taktilni vibracioni odziv na prag prevlačenja kartica, listanje zakačenih poruka i premeštanje kategorija" else "Swipe Threshold & Category Reordering Haptics: Tactile vibration feedback on conversation swipe thresholds, pinned message carousel switching, and category drag-reordering"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Prepravljene opcije" else "Modified & Fixed Options",
                                items = listOf(
                                    if (strings.isSerbian) "Višefaktorsko spajanje duplih konverzacija: Objedinjavanje razdvojenih niti razgovora za isti kontakt prema imenu iz imenika i normalizovanim ciframa broja" else "Multi-Factor Conversation Deduplication: Consolidates fragmented telephony thread IDs for the same person using phonebook names and normalized phone digits",
                                    if (strings.isSerbian) "Tačna kategorizacija „Usluge“ i širina kartica na srpskom: Imenovani kontakti i razgovori sa odgovorima korisnika zaštićeni su od lažnog svrstavanja u „Usluge“, a širine kartica prilagođene su tako da se „Nepročitano“ ne skraćuje" else "Smart Service Categorization & Serbian Tab Sizing: Named contacts and replied-to threads are protected from false Services classification, and category tab widths prevent truncation"
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
                                title = if (strings.isSerbian) "Dodate opcije" else "Added Options",
                                items = listOf(
                                    if (strings.isSerbian) "Višestruko pojedinačno slanje SMS poruka (Broadcast SMS): Slanje jedne poruke većem broju izabranih kontakata i unetih brojeva kao zasebnih 1-na-1 SMS poruka bez grupnog MMS-a, uz Označi sve / Odznači sve, brojač segmenata i prečicu u meniju razgovora" else "Multi-Recipient Broadcast SMS: Send a single message to multiple selected contacts and entered numbers as separate 1-on-1 SMS chats without group MMS, with Select All / Deselect All, segment counter, and chat menu shortcut"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Prepravljene opcije" else "Modified & Fixed Options",
                                items = listOf(
                                    if (strings.isSerbian) "Pouzdanost biometrijskog otključavanja: Dodata USE_BIOMETRIC dozvola i izvorna BiometricPrompt podrška (otisak, lice, PIN), zaštita od blokiranja kada telefon nema PIN i ispravka petlje ponovnog zaključavanja" else "Biometric Unlock Reliability: Added USE_BIOMETRIC permission and native BiometricPrompt support, safe fallback when no device lock is configured, and fixed re-lock loops after authentication"
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
                                title = if (strings.isSerbian) "Dodate opcije" else "Added Options",
                                items = listOf(
                                    if (strings.isSerbian) "Bezbednosni paket i aktivni štit: Zaključavanje aplikacije sa podesivim tajmautom (Odmah, 1 min, 5 min), AES-256-GCM šifrovanje lokalnog keša pomoću Android Keystore-a, SPKI SHA-256 sertifikat pinning, uklanjanje EXIF GPS metapodataka, detekcija root stanja, šifrovani prenosivi bekapi i pregled aktivnih mera klikom na štit u zaglavlju podešavanja" else "Security Suite & Active Shield Hub: App lock with configurable timeouts (Immediate, 1 min, 5 min), Android Keystore AES-256-GCM local cache encryption, SPKI SHA-256 certificate pinning, EXIF GPS stripping, root detection, encrypted portable backups, and live security status hub",
                                    if (strings.isSerbian) "Pametni birač kontakata za novu poruku i provera ažuriranja sa GitHub-a: Pretraga imena i brojeva u realnom vremenu pri pisanju nove poruke i provera novih izdanja u sekciji Sistem i aplikacija" else "Smart Contact Picker & GitHub Release Checker: Real-time fuzzy contact search when composing a new message and GitHub release checking in System & About"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Prepravljene opcije" else "Modified & Fixed Options",
                                items = listOf(
                                    if (strings.isSerbian) "Redizajn podešavanja u 7 kategorija, plutajući meniji i crveni „X“ za opoziv slanja: Podešavanja organizovana u 7 numerisanih oblasti sa kompaktnim plutajućim izbornicima i crvenim „X“ tasterom sa zvukom cancel.wav tokom odbrojavanja slanja" else "7-Category Settings Redesign, Floating Menus & Red 'X' Send Recall: Settings structured into 7 numbered categories with compact floating dropdowns and a red 'X' cancel button with cancel.wav audio during the send countdown"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Uklonjene opcije" else "Removed Options",
                                items = listOf(
                                    if (strings.isSerbian) "Podnaslovi glavnih kategorija podešavanja i modalni dijalozi izbora: Uklonjeni suvišni opisi sa glavnih kartica podešavanja i zamenjeni klasični iskačući dijalozi kompaktnim plutajućim menijima" else "Top-Level Settings Category Subtitles & Modal Picker Dialogs: Removed redundant subtitles from main settings category rows and replaced popup selection dialogs with floating menus"
                                )
                            )
                        )
                    )

                    ChangelogVersionSection(
                        versionName = "v1.5.1",
                        releaseDate = "08.11.2026.",
                        isCurrent = false,
                        categories = listOf(
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Dodate opcije" else "Added Options",
                                items = listOf(
                                    if (strings.isSerbian) "Premeštanje obrisanih poruka u Korpu sa rokom od 30 dana: Pojedinačne i označene poruke obrisane u četu premeštaju se u Obrisane poruke uz mogućnost vraćanja u razgovor i automatsko čišćenje nakon 30 dana" else "30-Day Trash Folder & Message Restore: Individual and multi-selected messages deleted in chat move to Trash with restore support and 30-day auto-purging"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Prepravljene opcije" else "Modified & Fixed Options",
                                items = listOf(
                                    if (strings.isSerbian) "Donji bar u punoj širini i sklopljive kartice za Mehuriće i Linkove: Proširen donji navigacioni bar i traka za označavanje na punu širinu ekrana i objedinjene opcije za Mehuriće i Eksterne linkove u padajuće kartice" else "Full-Width Bottom Dock & Collapsible Bubble / Link Cards: Expanded bottom navigation dock and selection bar to full width and consolidated Bubbles and External Links into collapsible cards"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Uklonjene opcije" else "Removed Options",
                                items = listOf(
                                    if (strings.isSerbian) "Veštačko LIMIT ograničenje pri učitavanju poruka: Uklonjeno ograničenje broja učitanih poruka tako da se uvek učitava kompletna SMS/MMS istorija sa uređaja" else "Artificial Message Query LIMIT: Removed database query LIMIT constraints so all stored SMS/MMS messages load completely"
                                )
                            )
                        )
                    )

                    ChangelogVersionSection(
                        versionName = "v1.5",
                        releaseDate = "08.11.2026.",
                        isCurrent = false,
                        categories = listOf(
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Dodate opcije" else "Added Options",
                                items = listOf(
                                    if (strings.isSerbian) "Pretraga u razgovoru, Hajlajtovane poruke, Pinovanje i Nacrti: Pretraga sa brojačem pogodaka, centralni pregled svih hajlajtovanih poruka sa skokom na poruku, kačenje važnih poruka na vrh četa i čuvanje nacrta po razgovoru" else "In-Chat Search, Highlighted Hub, Message Pinning & Drafts: In-chat search with match counter, unified Highlighted Messages hub with jump-to-message, message pinning, and per-thread drafts",
                                    if (strings.isSerbian) "Prepoznavanje brojeva, emailova i tekućih računa + deljenje kontakata i rođendanski šablon: Brze akcije za prepoznate telefone, email adrese i 18-cifrene tekuće račune, deljenje kontakta u polje za unos i ponoćni šablon za rođendan (00:00)" else "Phone, Email & Bank Account Recognition + Contact Sharing & Birthday Preset: Quick actions for phone numbers, emails, and 18-digit bank accounts, contact sharing into compose bar, and midnight (00:00) birthday preset",
                                    if (strings.isSerbian) "Cloudflare DoH zaštita, SIM poruke, odloženo slanje i AMOLED teme: Provera sumnjivih linkova preko Cloudflare DoH-a, upravljanje porukama na SIM kartici, opoziv slanja u roku od 6s, Pinch-to-zoom veličina oblačića, plutajući mehurići obaveštenja i AMOLED crna i bela tema" else "Cloudflare DoH Link Guard, SIM Manager, 6s Delayed Send & AMOLED Themes: Real-time Cloudflare DoH phishing checks, SIM card message management, 6s delayed send, pinch-to-zoom bubble sizing, notification bubbles, and AMOLED black/white themes"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Prepravljene opcije" else "Modified & Fixed Options",
                                items = listOf(
                                    if (strings.isSerbian) "Ručna promena kategorije pošiljaoca i filteri pregleda linkova: Ručno prebacivanje kontakta između kategorija Osobe i Usluge iz menija četa i filtriranje automatskih pregleda (samo Wi-Fi, samo veb-linkovi)" else "Manual People ↔ Services Override & Link Preview Filters: Manual sender category switching from chat menu and configurable automatic link previews (Wi-Fi only, web links only)"
                                )
                            )
                        )
                    )

                    ChangelogVersionSection(
                        versionName = "v1.0",
                        releaseDate = "08.11.2026.",
                        isCurrent = false,
                        categories = listOf(
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Dodate opcije" else "Added Options",
                                items = listOf(
                                    if (strings.isSerbian) "Napredne SMS/MMS opcije i prečica za hitna upozorenja: Izveštaji o isporuci, potvrde o čitanju za MMS, automatsko preuzimanje MMS-a (uvek, nikad, van rominga), režimi kreiranja MMS-a (Slobodno, Upozorenje, Ograničeno), planirano slanje poruka i prečica za bežična hitna upozorenja" else "Advanced SMS/MMS Controls & Emergency Alerts Shortcut: Delivery reports, MMS read receipts, configurable MMS auto-retrieve, MMS creation modes (Free, Warning, Restricted), scheduled messages, and wireless emergency alerts shortcut"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Prepravljene opcije" else "Modified & Fixed Options",
                                items = listOf(
                                    if (strings.isSerbian) "Kartice kategorija u punoj širini i stakleni kontrast: Optimizovan prikaz kategorija (Osobe, Usluge, Nepročitano, Sve), ikonica slanja usklađena sa temom i poboljšan kontrast teksta preko pozadine" else "Full-Width Category Tabs & Frosted Glass Contrast: Optimized full-width category tabs, theme-adapted send icon, and enhanced frosted glass text contrast"
                                )
                            ),
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Uklonjene opcije" else "Removed Options",
                                items = listOf(
                                    if (strings.isSerbian) "Testni primeri razgovora: Potpuno uklonjeni svi testni (mock) podaci iz produkcione baze" else "Mock Sample Conversations: Completely removed all mock sample messages from production builds"
                                )
                            )
                        )
                    )

                    ChangelogVersionSection(
                        versionName = "v0.9 Beta",
                        releaseDate = "05.11.2026.",
                        isCurrent = false,
                        categories = listOf(
                            ChangelogCategory(
                                title = if (strings.isSerbian) "Dodate opcije" else "Added Options",
                                items = listOf(
                                    if (strings.isSerbian) "Pametne kategorije, svajp radnje nad razgovorima i blokiranje pošiljalaca: Automatsko sortiranje razgovora u Osobe, Usluge i Nepročitano, podesive svajp radnje na listi razgovora i blokiranje neželjenih brojeva" else "Smart Categories, Conversation Swipe Actions & Blocked Senders: Automatic conversation sorting into People, Services, and Unread, customizable conversation card swipe actions, and sender blocking"
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
                                title = if (strings.isSerbian) "Dodate opcije" else "Added Options",
                                items = listOf(
                                    if (strings.isSerbian) "Početno izdanje i osnovna arhitektura: Slanje i prijem SMS i MMS poruka sa podrškom za dve SIM kartice, lokalni JSON bekap i vraćanje poruka, i Glassmorphism interfejs sa 7 tema i prilagođenim pozadinama" else "Initial Release & Core Architecture: Dual-SIM SMS/MMS messaging engine, offline JSON backup and restore, and original Glassmorphism UI with 7 themes and custom wallpapers"
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
    matchedSearchMessage: ChatMessage? = null,
    strings: AppStrings,
    onClick: () -> Unit,
    onOpenThreadWithMessage: ((Long, Long) -> Unit)? = null,
    onLongClick: () -> Unit = {},
    onTogglePin: () -> Unit,
    onOpenContact: () -> Unit,
    onCopyOtp: (String) -> Unit
) {
    val palette = LocalGlassPalette.current
    val lastMsg = matchedSearchMessage ?: thread.lastMessage
    val risk = thread.highestRiskLevel
    val otpCode = lastMsg?.securityAnalysis?.extractedOtpCode
    val isCompact = listLayoutMode == MainListLayoutMode.COMPACT

    val borderAccent = when {
        isSelected -> palette.accentPrimary
        matchedSearchMessage != null -> palette.accentPrimary
        risk == SecurityRiskLevel.SUSPICIOUS -> palette.dangerColor
        thread.isPinned -> palette.accentPrimary
        thread.unreadCount > 0 -> palette.accentPrimary
        else -> null
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (isCompact) 14.dp else 20.dp),
        elevated = isSelected || matchedSearchMessage != null || (thread.unreadCount > 0) || thread.isPinned,
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
                    val hasDraft = matchedSearchMessage == null && !thread.draftText.isNullOrBlank()
                    val previewText = if (matchedSearchMessage != null) {
                        matchedSearchMessage.body
                    } else if (hasDraft) {
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
                        color = if (matchedSearchMessage != null || hasDraft) {
                            palette.accentPrimary
                        } else if (thread.unreadCount > 0) {
                            palette.textPrimary
                        } else {
                            palette.textSecondary
                        },
                        fontWeight = if (matchedSearchMessage != null || hasDraft || thread.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
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
                        val suspiciousMsg = remember(thread.messages) {
                            thread.messages.lastOrNull { it.securityAnalysis.riskLevel == SecurityRiskLevel.SUSPICIOUS }
                                ?: thread.messages.firstOrNull { it.securityAnalysis.riskLevel == SecurityRiskLevel.SUSPICIOUS }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    if (onOpenThreadWithMessage != null) {
                                        onOpenThreadWithMessage(thread.id, suspiciousMsg?.id ?: -2L)
                                    } else {
                                        onClick()
                                    }
                                }
                                .padding(vertical = 2.dp, horizontal = 4.dp)
                        ) {
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
    initialTargetMessageId: Long? = null,
    initialSearchQuery: String = "",
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
    onMarkMessageSafe: (Long) -> Unit = {},
    onMarkMessagesSafe: (List<Long>) -> Unit = { ids -> ids.forEach(onMarkMessageSafe) },
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

    // Deduplicated messages strictly belonging to this conversation thread
    val displayMessages = remember(thread.id, thread.messages) {
        deduplicateMessageList(thread.messages.filter { it.threadId == thread.id || thread.messages.isNotEmpty() })
    }

    // In-Chat Search state (searches ONLY within this active conversation thread)
    var isChatSearchVisible by remember(thread.id) { mutableStateOf(false) }
    var chatSearchQuery by remember(thread.id) { mutableStateOf("") }
    var activeSearchMatchIndex by remember(thread.id) { mutableIntStateOf(0) }
    var showAttachmentActions by remember(thread.id) { mutableStateOf(false) }

    LaunchedEffect(isChatSearchVisible, thread.id, initialTargetMessageId, initialSearchQuery) {
        if ((isChatSearchVisible || initialTargetMessageId != null || initialSearchQuery.isNotBlank()) && !thread.isHistoryLoaded) {
            SmsMmsRepository.loadCachedThreadHistory(context, thread.id)
        }
    }

    val matchingMessageIndices = remember(displayMessages, chatSearchQuery) {
        val trimmed = chatSearchQuery.trim()
        if (trimmed.isEmpty()) {
            emptyList()
        } else {
            displayMessages.mapIndexedNotNull { index, msg ->
                if (msg.body.contains(trimmed, ignoreCase = true)) index else null
            }
        }
    }

    var showShareContactDialog by remember { mutableStateOf(false) }
    var showBroadcastDialog by remember { mutableStateOf(false) }
    var showCalendarPickerDialog by remember { mutableStateOf(false) }
    var showSecurityMeasuresDialog by remember { mutableStateOf(false) }

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

    val initialMessageScrollIndex = remember(thread.id, initialTargetMessageId, initialSearchQuery) {
        when {
            initialTargetMessageId != null -> {
                val foundIdx = if (initialTargetMessageId > 0L) {
                    val byId = displayMessages.indexOfFirst { it.id == initialTargetMessageId }
                    if (byId != -1) byId else displayMessages.indexOfLast { it.securityAnalysis.riskLevel == SecurityRiskLevel.SUSPICIOUS }
                } else {
                    displayMessages.indexOfLast { it.securityAnalysis.riskLevel == SecurityRiskLevel.SUSPICIOUS }
                }
                if (foundIdx != -1) foundIdx else (displayMessages.size - 1).coerceAtLeast(0)
            }
            initialSearchQuery.isNotBlank() -> {
                val trimmed = initialSearchQuery.trim()
                val foundIdx = displayMessages.indexOfLast { it.body.contains(trimmed, ignoreCase = true) }
                if (foundIdx != -1) foundIdx else (displayMessages.size - 1).coerceAtLeast(0)
            }
            else -> (displayMessages.size - 1).coerceAtLeast(0)
        }
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
    var jumpedToMessageId by remember(thread.id, initialTargetMessageId, initialSearchQuery) {
        mutableStateOf<Long?>(initialTargetMessageId)
    }
    var showPinnedMessagesManageDialog by remember(thread.id) { mutableStateOf(value = false) }
    var selectedPinnedIdsForUnpin by remember(thread.id) { mutableStateOf<Set<Long>>(emptySet()) }
    val currentZoom by rememberUpdatedState(bubbleZoomScale)

    val activeInChatSearchMatchedMessageId = remember(
        isChatSearchVisible,
        chatSearchQuery,
        matchingMessageIndices,
        activeSearchMatchIndex,
        displayMessages
    ) {
        if (isChatSearchVisible && chatSearchQuery.isNotBlank() && matchingMessageIndices.isNotEmpty()) {
            val safeIdx = activeSearchMatchIndex.coerceIn(0, matchingMessageIndices.lastIndex)
            displayMessages.getOrNull(matchingMessageIndices[safeIdx])?.id
        } else {
            null
        }
    }

    LaunchedEffect(chatSearchQuery.trim(), matchingMessageIndices.size) {
        val trimmed = chatSearchQuery.trim()
        if (isChatSearchVisible && trimmed.isNotEmpty() && matchingMessageIndices.isNotEmpty()) {
            val matchPos = matchingMessageIndices.lastIndex
            activeSearchMatchIndex = matchPos
            val listIdx = matchingMessageIndices[matchPos]
            val matchedMsg = displayMessages.getOrNull(listIdx)
            if (matchedMsg != null) {
                jumpedToMessageId = matchedMsg.id
                listState.animateScrollToItem(
                    index = listIdx,
                    scrollOffset = -120
                )
            }
        }
    }

    LaunchedEffect(pinnedMessages.map { it.id }) {
        val currentPinnedIds = pinnedMessages.mapTo(mutableSetOf()) { it.id }
        val unpinnedMessageStillInThread = previousPinnedMessageIds.any { messageId ->
            messageId !in currentPinnedIds && displayMessages.any { it.id == messageId }
        }
        previousPinnedMessageIds = currentPinnedIds

        if (unpinnedMessageStillInThread && displayMessages.isNotEmpty()) {
            listState.animateScrollToItem(displayMessages.size - 1)
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
            delay(3500.milliseconds)
            if (activeInChatSearchMatchedMessageId == null) {
                jumpedToMessageId = null
            }
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
    var pendingMarkSafeMessageIds by remember { mutableStateOf<List<Long>>(emptyList()) }

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
                    val uriStr = uri.toString()
                    val isAudio = com.txtura.telephony.MmsPduHelper.isAudioContentType(null, uri.path ?: uriStr) ||
                        uriStr.contains("voice_") ||
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

    var isInitialScrollDone by remember(thread.id, initialTargetMessageId, initialSearchQuery) { mutableStateOf(false) }
    var hasScrolledToTargetMessage by remember(thread.id, initialTargetMessageId, initialSearchQuery) { mutableStateOf(false) }

    LaunchedEffect(thread.id, displayMessages.size, pendingOutgoingPreviewMessage?.id, initialTargetMessageId, initialSearchQuery) {
        val totalItems = displayMessages.size + (if (pendingOutgoingPreviewMessage != null) 1 else 0)
        if (totalItems > 0) {
            val trimmedInitialQuery = initialSearchQuery.trim()
            val hasInitialTarget = initialTargetMessageId != null || trimmedInitialQuery.isNotEmpty()
            if (hasInitialTarget) {
                val foundIdx = when {
                    initialTargetMessageId != null -> {
                        val byId = if (initialTargetMessageId > 0L) {
                            displayMessages.indexOfFirst { it.id == initialTargetMessageId }
                        } else {
                            -1
                        }
                        if (byId >= 0) {
                            byId
                        } else {
                            val suspiciousIdx = displayMessages.indexOfLast {
                                it.securityAnalysis.riskLevel == SecurityRiskLevel.SUSPICIOUS
                            }
                            if (suspiciousIdx >= 0) {
                                suspiciousIdx
                            } else if (trimmedInitialQuery.isNotEmpty()) {
                                displayMessages.indexOfLast { it.body.contains(trimmedInitialQuery, ignoreCase = true) }
                            } else {
                                -1
                            }
                        }
                    }
                    else -> displayMessages.indexOfLast { it.body.contains(trimmedInitialQuery, ignoreCase = true) }
                }
                if (foundIdx >= 0 && !hasScrolledToTargetMessage) {
                    hasScrolledToTargetMessage = true
                    isInitialScrollDone = true
                    val targetMsg = displayMessages[foundIdx]
                    jumpedToMessageId = targetMsg.id
                    listState.animateScrollToItem(
                        index = foundIdx,
                        scrollOffset = -120
                    )
                }
            } else {
                val target = totalItems - 1
                if (!isInitialScrollDone) {
                    isInitialScrollDone = true
                    listState.scrollToItem(target)
                } else {
                    listState.animateScrollToItem(target)
                }
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
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 17.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = palette.textPrimary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
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

        // Confirmation dialog before marking harmful message(s) as safe (Harmful message override)
        if (pendingMarkSafeMessageIds.isNotEmpty()) {
            val idsToConfirm = pendingMarkSafeMessageIds
            GlassAlertDialog(
                onDismissRequest = { pendingMarkSafeMessageIds = emptyList() },
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
                            text = strings.confirmMarkSafeDialogTitle(idsToConfirm.size),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 17.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = palette.textPrimary
                        )
                    }
                },
                text = {
                    Text(
                        text = strings.confirmMarkSafeDialogBody(idsToConfirm.size),
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textPrimary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            TxturaHaptics.confirm(view, context)
                            pendingMarkSafeMessageIds = emptyList()
                            selectedMessageIds = emptySet()
                            if (idsToConfirm.size == 1) {
                                onMarkMessageSafe(idsToConfirm.first())
                                Toast.makeText(
                                    context,
                                    strings.messageMarkedNotHarmfulToast,
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                onMarkMessagesSafe(idsToConfirm)
                                Toast.makeText(
                                    context,
                                    strings.messagesMarkedNotHarmfulToast(idsToConfirm.size),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    ) {
                        Text(
                            text = strings.confirmMarkSafeButton,
                            color = palette.safeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingMarkSafeMessageIds = emptyList() }) {
                        Text(strings.cancel, color = palette.textSecondary)
                    }
                }
            )
        }

        // Confirm single message deletion (Move to Trash)
        val msgToDelete = messagePendingDelete
        if (msgToDelete != null) {
            GlassAlertDialog(
                onDismissRequest = { messagePendingDelete = null },
                accentBorderColor = palette.dangerColor,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = TxturaIcons.Delete,
                            contentDescription = null,
                            tint = palette.dangerColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.confirmDeleteSingleMessageTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 17.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = palette.textPrimary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                text = {
                    Text(
                        text = strings.confirmDeleteSingleMessageBody,
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textSecondary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            TxturaHaptics.reject(view, context)
                            messagePendingDelete = null
                            onDeleteMessage(msgToDelete.id)
                        }
                    ) {
                        Text(
                            text = strings.confirmMoveToTrashButton,
                            color = palette.dangerColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { messagePendingDelete = null }) {
                        Text(strings.cancel, color = palette.textSecondary)
                    }
                }
            )
        }

        // Security measures summary dialog
        if (showSecurityMeasuresDialog) {
            SecurityMeasuresDialog(
                onDismiss = { showSecurityMeasuresDialog = false },
                identifyMaliciousWebsites = MessageSecurityAnalyzer.isOnlineMaliciousWebsiteCheckEnabled
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
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        TxturaHaptics.tick(view, context)
                                        showSecurityMeasuresDialog = true
                                    }
                                    .padding(end = 4.dp, top = 2.dp, bottom = 2.dp)
                            ) {
                                Icon(
                                    imageVector = TxturaIcons.ShieldCheck,
                                    contentDescription = strings.securitySectionTitle,
                                    tint = palette.safeColor,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            OverflowTooltipText(
                                text = thread.address,
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

            // Precompute the index of the first message of the same calendar day for each message in O(N)
            val dayGroupFirstIndex = remember(displayMessages) {
                IntArray(displayMessages.size).apply {
                    for (i in displayMessages.indices) {
                        this[i] = if (i == 0 || !isSameDay(displayMessages[i - 1].timestampMillis, displayMessages[i].timestampMillis)) {
                            i
                        } else {
                            this[i - 1]
                        }
                    }
                }
            }

            val defaultHeaderHeightPx = with(LocalDensity.current) { (30f * bubbleZoomScale.coerceIn(0.75f, 1.65f)).dp.roundToPx() }
            var stickyHeaderHeightPx by remember { mutableIntStateOf(defaultHeaderHeightPx) }

            // Sticky date header state: (activeDayStartIdx, activeTimestampMillis, pushUpTranslationY)
            val stickyDateHeaderState by remember(listState, displayMessages, dayGroupFirstIndex) {
                derivedStateOf {
                    if (displayMessages.isEmpty()) return@derivedStateOf null
                    val visibleMsgItems = listState.layoutInfo.visibleItemsInfo.filter { it.index in displayMessages.indices }
                    if (visibleMsgItems.isEmpty()) return@derivedStateOf null

                    val activeTopItem = visibleMsgItems.lastOrNull { it.offset <= 0 } ?: visibleMsgItems.first()
                    val activeDayStartIdx = dayGroupFirstIndex[activeTopItem.index]
                    val activeTimestamp = displayMessages[activeDayStartIdx].timestampMillis

                    val nextDayStartItem = visibleMsgItems.firstOrNull { item ->
                        item.index > activeTopItem.index && dayGroupFirstIndex[item.index] == item.index
                    }

                    val headerH = stickyHeaderHeightPx.coerceAtLeast(1)
                    val pushUpOffsetPx = if (nextDayStartItem != null && nextDayStartItem.offset in 1 until headerH) {
                        (nextDayStartItem.offset - headerH).toFloat()
                    } else {
                        0f
                    }

                    Triple(activeDayStartIdx, activeTimestamp, pushUpOffsetPx)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Messages Thread with two-finger Pinch-to-Zoom and Long-Press Context Menu on each bubble
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy((7f * bubbleZoomScale.coerceIn(0.75f, 1.65f)).dp, Alignment.Bottom),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier
                        .fillMaxSize()
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
                        val showDateHeader = dayGroupFirstIndex[index] == index
                        val isHiddenByStickyOverlay = stickyDateHeaderState?.first == index
                        val messageSwipeRightAction = remember(context) {
                            SmsMmsRepository.getMessageSwipeRightAction(context)
                        }
                        val messageSwipeLeftAction = remember(context) {
                            SmsMmsRepository.getMessageSwipeLeftAction(context)
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (showDateHeader) {
                                ChatDateHeaderPill(
                                    timestampMillis = msg.timestampMillis,
                                    strings = strings,
                                    zoomScale = bubbleZoomScale,
                                    modifier = Modifier.alpha(if (isHiddenByStickyOverlay) 0f else 1f)
                                )
                            }

                            val isMsgSelected = selectedMessageIds.contains(msg.id)
                            val activeHighlightQuery = if (isChatSearchVisible && chatSearchQuery.isNotBlank()) {
                                chatSearchQuery.trim()
                            } else {
                                initialSearchQuery.trim()
                            }
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
                                isJumpHighlighted = (jumpedToMessageId == msg.id) || (activeInChatSearchMatchedMessageId == msg.id),
                                searchHighlightQuery = activeHighlightQuery,
                                swipeRightAction = if (isSelectionMode) MessageSwipeAction.NONE else messageSwipeRightAction,
                                swipeLeftAction = if (isSelectionMode) MessageSwipeAction.NONE else messageSwipeLeftAction,
                                onExecuteMessageSwipeAction = { action ->
                                    when (action) {
                                        MessageSwipeAction.PIN -> {
                                            val wasPinned = msg.isPinned
                                            onTogglePinMessage(msg.id)
                                            val toastMsg = if (wasPinned) {
                                                strings.messagesUnpinnedToast(1)
                                            } else {
                                                strings.messagesPinnedToast(1)
                                            }
                                            Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                        }
                                        MessageSwipeAction.HIGHLIGHT -> {
                                            val wasHighlighted = msg.isHighlighted
                                            onToggleHighlightMessage(msg.id)
                                            val toastMsg = if (wasHighlighted) {
                                                strings.messagesUnhighlightedToast(1)
                                            } else {
                                                strings.messagesHighlightedToast(1)
                                            }
                                            Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                        }
                                        MessageSwipeAction.DELETE -> {
                                            messagePendingDelete = msg
                                        }
                                        MessageSwipeAction.NONE -> Unit
                                    }
                                },
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
                                onMarkMessageSafe = if (msg.securityAnalysis.riskLevel == SecurityRiskLevel.SUSPICIOUS) {
                                    {
                                        TxturaHaptics.tick(view, context)
                                        pendingMarkSafeMessageIds = listOf(msg.id)
                                    }
                                } else {
                                    null
                                },
                                onCopyOtp = onCopyOtp
                            )
                        }
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
                                swipeRightAction = MessageSwipeAction.NONE,
                                swipeLeftAction = MessageSwipeAction.NONE,
                                onClickMessage = {},
                                onLongPressMessage = {},
                                onCopyOtp = onCopyOtp
                            )
                        }
                    }
                }

                // Sticky top-center date header pill below the contact header card:
                // stays pinned while scrolling messages of the active date and scrolls out when pushed by the next date's pill
                stickyDateHeaderState?.let { (_, activeTimestamp, pushUpOffsetPx) ->
                    Box(
                        contentAlignment = Alignment.TopCenter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clipToBounds()
                            .align(Alignment.TopCenter)
                    ) {
                        ChatDateHeaderPill(
                            timestampMillis = activeTimestamp,
                            strings = strings,
                            zoomScale = bubbleZoomScale,
                            modifier = Modifier
                                .onSizeChanged { size ->
                                    if (size.height > 0) {
                                        stickyHeaderHeightPx = size.height
                                    }
                                }
                                .graphicsLayer {
                                    translationY = pushUpOffsetPx
                                }
                        )
                    }
                }

                // Derived state: true if the user has scrolled up and is not at the very bottom.
                val isScrolledUp by remember(listState) {
                    derivedStateOf {
                        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                        if (lastVisible != null) {
                            lastVisible.index < listState.layoutInfo.totalItemsCount - 1
                        } else {
                            false
                        }
                    }
                }

                // Scroll to bottom floating button
                androidx.compose.animation.AnimatedVisibility(
                    visible = isScrolledUp,
                    enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.8f),
                    exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.8f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = if (isChatSearchVisible) 56.dp else 0.dp) // Adjust padding if search is open
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(palette.glassSurfaceElevated.copy(alpha = 0.95f))
                            .border(1.dp, palette.accentPrimary.copy(alpha = 0.5f), CircleShape)
                            .clickable {
                                coroutineScope.launch {
                                    val totalItems = thread.messages.size + (if (pendingOutgoingPreviewMessage != null) 1 else 0)
                                    if (totalItems > 0) {
                                        listState.animateScrollToItem(totalItems - 1)
                                    }
                                }
                            }
                    ) {
                        Icon(
                            imageVector = TxturaIcons.ArrowDown,
                            contentDescription = null,
                            tint = palette.accentPrimary,
                            modifier = Modifier.size(18.dp)
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
                val allSelectedSuspicious = selectedMessages.isNotEmpty() &&
                    selectedMessages.all { it.securityAnalysis.riskLevel == SecurityRiskLevel.SUSPICIOUS }
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

                        var showConfirmBatchDeleteDialog by remember { mutableStateOf(false) }

                        if (showConfirmBatchDeleteDialog) {
                            GlassAlertDialog(
                                onDismissRequest = { showConfirmBatchDeleteDialog = false },
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
                                            showConfirmBatchDeleteDialog = false
                                            TxturaHaptics.reject(view, context)
                                            val idsToDelete = selectedMessageIds.toList()
                                            selectedMessageIds = emptySet()
                                            onDeleteMessages(idsToDelete)
                                        }
                                    ) {
                                        Text(strings.deleteMessageOption, color = palette.dangerColor, fontWeight = FontWeight.Bold)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showConfirmBatchDeleteDialog = false }) {
                                        Text(strings.cancel, color = palette.textSecondary)
                                    }
                                }
                            )
                        }

                        // 2. Delete (Obriši)
                        GlassTooltipIconButton(
                            icon = TxturaIcons.Delete,
                            tooltipText = strings.deleteMessageOption,
                            tint = palette.dangerColor,
                            pressedTint = palette.dangerColor,
                            size = 38.dp,
                            onClick = { showConfirmBatchDeleteDialog = true }
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

                        // 4b. Mark as Not Harmful (Označi da nije štetna) - ONLY when selected message(s) are harmful!
                        if (allSelectedSuspicious) {
                            GlassTooltipIconButton(
                                icon = TxturaIcons.ShieldCheck,
                                tooltipText = strings.markMessageNotHarmfulAction,
                                tint = palette.safeColor,
                                pressedTint = palette.safeColor,
                                size = 38.dp,
                                onClick = {
                                    TxturaHaptics.tick(view, context)
                                    pendingMarkSafeMessageIds = selectedMessageIds.toList()
                                }
                            )
                        }

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
                                    val trimmed = query.trim()
                                    if (trimmed.isEmpty()) {
                                        activeSearchMatchIndex = 0
                                        jumpedToMessageId = null
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
                                    strings.searchMatchesCounter(
                                        activeSearchMatchIndex.coerceIn(0, (matchCount - 1).coerceAtLeast(0)) + 1,
                                        matchCount
                                    )
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
                                            val matchedMsg = displayMessages.getOrNull(listIdx)
                                            if (matchedMsg != null) {
                                                jumpedToMessageId = matchedMsg.id
                                                coroutineScope.launch {
                                                    listState.animateScrollToItem(
                                                        index = listIdx,
                                                        scrollOffset = -120
                                                    )
                                                }
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
                                            val matchedMsg = displayMessages.getOrNull(listIdx)
                                            if (matchedMsg != null) {
                                                jumpedToMessageId = matchedMsg.id
                                                coroutineScope.launch {
                                                    listState.animateScrollToItem(
                                                        index = listIdx,
                                                        scrollOffset = -120
                                                    )
                                                }
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
                                    jumpedToMessageId = null
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

                            GlassIconButton(
                                icon = TxturaIcons.Plus,
                                contentDescription = strings.attachMmsImage,
                                tint = if (showAttachmentActions) palette.accentPrimary else palette.accentSecondary,
                                onClick = { showAttachmentActions = !showAttachmentActions },
                                size = 34.dp,
                                modifier = Modifier.rotate(plusRotation)
                            )

                            val textBarAlpha by animateFloatAsState(
                                targetValue = if (showAttachmentActions) 0f else 1f,
                                animationSpec = tween(durationMillis = if (expressiveAnimationsEnabled) 150 else 90),
                                label = "composer_text_bar_alpha"
                            )

                            Box(
                                contentAlignment = Alignment.CenterStart,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Layer 1: Search button + Text Input Box (always measured at full width so it never squeezes or jumps)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .alpha(textBarAlpha)
                                        .pointerInput(showAttachmentActions) {
                                            if (showAttachmentActions) {
                                                awaitPointerEventScope {
                                                    while (true) {
                                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                                        event.changes.forEach { it.consume() }
                                                    }
                                                }
                                            }
                                        }
                                ) {
                                    Spacer(modifier = Modifier.width(4.dp))

                                    GlassIconButton(
                                        icon = TxturaIcons.Search,
                                        contentDescription = strings.searchInChatPlaceholder,
                                        tint = if (isChatSearchVisible) palette.accentPrimary else palette.textSecondary,
                                        onClick = {
                                            if (!showAttachmentActions) {
                                                isChatSearchVisible = !isChatSearchVisible
                                                if (!isChatSearchVisible) {
                                                    chatSearchQuery = ""
                                                    activeSearchMatchIndex = 0
                                                }
                                            }
                                        },
                                        size = 34.dp
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(palette.glassSurfaceElevated.copy(alpha = 0.42f))
                                            .border(
                                                width = 0.8.dp,
                                                color = palette.glassBorderTop.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .padding(horizontal = 11.dp, vertical = 6.dp)
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            val textScrollState = rememberScrollState()

                                            LaunchedEffect(draftText) {
                                                if (draftText.isNotEmpty()) {
                                                    textScrollState.animateScrollTo(textScrollState.maxValue)
                                                }
                                            }

                                            // Bounded text box: exactly max 2 lines (~42.dp max height) with vertical scroll
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 22.dp, max = 42.dp)
                                                    .verticalScroll(textScrollState)
                                            ) {
                                                if (draftText.isEmpty()) {
                                                    Text(
                                                        text = if (isMmsMode) strings.addMmsCaptionPlaceholder else strings.writeSmsPlaceholder,
                                                        style = MaterialTheme.typography.bodyLarge.copy(
                                                            fontSize = 14.5.sp
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
                                                    enabled = !showAttachmentActions,
                                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                                        color = palette.textPrimary,
                                                        fontSize = 15.sp,
                                                        lineHeight = 19.sp
                                                    ),
                                                    cursorBrush = SolidColor(palette.accentPrimary),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .focusRequester(inputFocusRequester)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(2.dp))

                                            // Character counter directly below text field inside the textbox card
                                            Text(
                                                text = if (isMmsMode) {
                                                    strings.mmsModeIndicator
                                                } else {
                                                    "${draftText.length}/$charsPerSegment ($smsSegments) • SMS"
                                                },
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 11.sp
                                                ),
                                                color = if (isMmsMode) palette.accentSecondary else palette.textSecondary,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Clip
                                            )
                                        }
                                    }
                                }

                                // Layer 2: Attachment categories + Search button (expands/collapses smoothly without squeezing Layer 1)
                                androidx.compose.animation.AnimatedVisibility(
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
                                            size = 32.dp
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
                                            size = 32.dp
                                        )

                                        GlassIconButton(
                                            icon = TxturaIcons.DocumentFile,
                                            contentDescription = strings.attachDocumentFile,
                                            tint = palette.accentSecondary,
                                            onClick = {
                                                showAttachmentActions = false
                                                documentPickerLauncher.launch(arrayOf("*/*"))
                                            },
                                            size = 32.dp
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
                                            size = 32.dp
                                        )

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
                                            size = 32.dp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(if (isCountingDown) 10.dp else 58.dp))

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
                                                if (inAppSoundsEnabled) {
                                                    SmsMmsRepository.playInAppMessageSentSound(context)
                                                }
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
                                                val recFile = VoiceMessageManager.stopRecording(context)
                                                val recDur = VoiceMessageManager.recordingDurationMillis.longValue
                                                if (recFile != null && recFile.exists() && recFile.length() > 60L && recDur >= 300L) {
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
    searchHighlightQuery: String = "",
    swipeRightAction: MessageSwipeAction = MessageSwipeAction.PIN,
    swipeLeftAction: MessageSwipeAction = MessageSwipeAction.HIGHLIGHT,
    onExecuteMessageSwipeAction: (MessageSwipeAction) -> Unit = {},
    onClickMessage: () -> Unit = {},
    onLongPressMessage: () -> Unit = {},
    onMarkMessageSafe: (() -> Unit)? = null,
    onCopyOtp: (String) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val palette = LocalGlassPalette.current
    val scope = rememberCoroutineScope()
    val isOutgoing = !message.isIncoming
    val analysis = message.securityAnalysis
    val s = zoomScale.coerceIn(0.75f, 1.65f)
    val cornerRadius = (18f * s).coerceIn(12f, 26f).dp
    val tailRadius = (4f * s).coerceIn(3f, 8f).dp
    val currentOnLongPress by rememberUpdatedState(onLongPressMessage)
    val currentOnClick by rememberUpdatedState(onClickMessage)
    val currentOnExecuteSwipe by rememberUpdatedState(onExecuteMessageSwipeAction)

    val swipeOffsetX = remember { Animatable(0f) }
    var hasSnappedThreshold by remember { mutableStateOf(false) }
    var badgeWidthPx by remember { mutableIntStateOf(0) }
    val badgeGapPx = remember(density) { with(density) { 6.dp.toPx() } }
    val swipeThresholdPx = remember(density) { with(density) { 56.dp.toPx() } }
    val maxSwipeDragPx = remember(density) { with(density) { 86.dp.toPx() } }
    val currentOffset = swipeOffsetX.value
    val activeSwipeAction = when {
        currentOffset > 10f -> swipeRightAction
        currentOffset < -10f -> swipeLeftAction
        else -> MessageSwipeAction.NONE
    }

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
    val detectedPaymentRefs = remember(message.body) {
        MessageSecurityAnalyzer.extractPaymentReferences(message.body)
    }
    val detectedPaymentAmounts = remember(message.body) {
        MessageSecurityAnalyzer.extractPaymentAmounts(message.body)
    }
    val detectedActionableDates = remember(message.body) {
        MessageSecurityAnalyzer.extractActionableDates(message.body)
    }
    val detectedTrackingNumbers = remember(message.body) {
        MessageSecurityAnalyzer.extractPackageTrackingNumbers(message.body)
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
                    palette.accentPrimary.copy(alpha = 0.54f),
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

    val annotatedMessageText = remember(message.body, safeLinks, linkColor, searchHighlightQuery, isJumpHighlighted, palette.cautionColor) {
        buildAnnotatedString {
            append(message.body)
            val trimmedHighlight = searchHighlightQuery.trim()
            if (trimmedHighlight.isNotEmpty()) {
                var startIdx = message.body.indexOf(trimmedHighlight, ignoreCase = true)
                while (startIdx >= 0) {
                    val endIdx = (startIdx + trimmedHighlight.length).coerceAtMost(message.body.length)
                    addStyle(
                        style = SpanStyle(
                            background = palette.cautionColor.copy(alpha = if (isJumpHighlighted) 0.55f else 0.35f),
                            color = if (isOutgoing) Color.White else palette.textPrimary,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        start = startIdx,
                        end = endIdx
                    )
                    startIdx = message.body.indexOf(trimmedHighlight, startIndex = endIdx, ignoreCase = true)
                }
            }
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

    val targetScale = when {
        isJumpHighlighted -> 1.032f
        isSelected -> 1.025f
        else -> 1.0f
    }
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
            contentAlignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            if (abs(currentOffset) > 10f && activeSwipeAction != MessageSwipeAction.NONE) {
                val actionColor = when (activeSwipeAction) {
                    MessageSwipeAction.PIN -> palette.accentPrimary
                    MessageSwipeAction.HIGHLIGHT -> palette.cautionColor
                    MessageSwipeAction.DELETE -> palette.dangerColor
                    MessageSwipeAction.NONE -> palette.accentPrimary
                }
                val actionIcon = when (activeSwipeAction) {
                    MessageSwipeAction.PIN -> if (message.isPinned) TxturaIcons.PinFilled else TxturaIcons.PinOutline
                    MessageSwipeAction.HIGHLIGHT -> TxturaIcons.HighlightStar
                    MessageSwipeAction.DELETE -> TxturaIcons.Delete
                    MessageSwipeAction.NONE -> TxturaIcons.PinOutline
                }
                val actionText = when (activeSwipeAction) {
                    MessageSwipeAction.PIN -> if (message.isPinned) strings.swipeGestureUnpin else strings.swipeGesturePin
                    MessageSwipeAction.HIGHLIGHT -> if (message.isHighlighted) strings.swipeGestureUnhighlight else strings.swipeGestureHighlight
                    MessageSwipeAction.DELETE -> strings.swipeActionDelete
                    MessageSwipeAction.NONE -> ""
                }
                val isTriggered = abs(currentOffset) >= swipeThresholdPx
                val isSwipingRight = currentOffset > 0f
                val badgeShape = RoundedCornerShape((14f * s).dp)
                val isEdgeConstrained = (isSwipingRight && !isOutgoing) || (!isSwipingRight && isOutgoing)
                val badgeFitScale = if (isEdgeConstrained && badgeWidthPx > 0) {
                    ((abs(currentOffset) - badgeGapPx).coerceAtLeast(0f) / badgeWidthPx.toFloat()).coerceIn(0f, 1f)
                } else {
                    1f
                }
                val badgeExtraOffsetX = when {
                    isSwipingRight && isOutgoing -> {
                        currentOffset - badgeWidthPx - badgeGapPx
                    }
                    !isSwipingRight && !isOutgoing -> {
                        currentOffset + badgeWidthPx + badgeGapPx
                    }
                    isSwipingRight && !isOutgoing -> {
                        if (currentOffset >= badgeWidthPx + badgeGapPx) {
                            currentOffset - badgeWidthPx - badgeGapPx
                        } else {
                            -badgeWidthPx * (1f - badgeFitScale) * 0.5f
                        }
                    }
                    else -> {
                        if (abs(currentOffset) >= badgeWidthPx + badgeGapPx) {
                            currentOffset + badgeWidthPx + badgeGapPx
                        } else {
                            badgeWidthPx * (1f - badgeFitScale) * 0.5f
                        }
                    }
                }

                Box(
                    contentAlignment = if (isSwipingRight) Alignment.CenterStart else Alignment.CenterEnd,
                    modifier = Modifier
                        .align(if (isSwipingRight) Alignment.CenterStart else Alignment.CenterEnd)
                        .offset { IntOffset(badgeExtraOffsetX.roundToInt(), 0) }
                        .graphicsLayer {
                            scaleX = badgeFitScale
                            scaleY = badgeFitScale
                            alpha = badgeFitScale.coerceIn(0f, 1f)
                        }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .onSizeChanged { badgeWidthPx = it.width }
                            .clip(badgeShape)
                            .background(actionColor.copy(alpha = if (isTriggered) 0.28f else 0.14f))
                            .border(
                                width = 1.dp,
                                color = actionColor.copy(alpha = if (isTriggered) 0.80f else 0.38f),
                                shape = badgeShape
                            )
                            .padding(horizontal = (10f * s).dp, vertical = (6f * s).dp)
                    ) {
                        Icon(
                            imageVector = actionIcon,
                            contentDescription = null,
                            tint = actionColor,
                            modifier = Modifier.size(if (isTriggered) (18f * s).dp else (15f * s).dp)
                        )
                        Spacer(modifier = Modifier.height((3f * s).dp))
                        Text(
                            text = actionText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = (10f * s).sp,
                                lineHeight = (12f * s).sp
                            ),
                            color = actionColor,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(swipeOffsetX.value.roundToInt(), 0) }
                    .widthIn(max = (300f * s).coerceIn(210f, 380f).dp)
                    .graphicsLayer {
                        scaleX = animatedBubbleScale
                        scaleY = animatedBubbleScale
                    }
                    .clip(bubbleShape)
                    .background(brush = bubbleBrush, shape = bubbleShape)
                    .border(
                        width = if (isJumpHighlighted) 2.5.dp else if (isSelected) 2.dp else if (message.isHighlighted) 1.5.dp else 1.dp,
                        color = borderColor,
                        shape = bubbleShape
                    )
                    .pointerInput(message.id, swipeRightAction, swipeLeftAction) {
                        if (swipeRightAction == MessageSwipeAction.NONE && swipeLeftAction == MessageSwipeAction.NONE) {
                            return@pointerInput
                        }
                        detectHorizontalDragGestures(
                            onDragStart = {
                                TxturaHaptics.tick(view, context)
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                val proposed = swipeOffsetX.value + dragAmount
                                val clamped = when {
                                    proposed > 0f && swipeRightAction == MessageSwipeAction.NONE -> proposed * 0.15f
                                    proposed < 0f && swipeLeftAction == MessageSwipeAction.NONE -> proposed * 0.15f
                                    else -> proposed.coerceIn(-maxSwipeDragPx, maxSwipeDragPx)
                                }
                                val active = when {
                                    clamped > 10f -> swipeRightAction
                                    clamped < -10f -> swipeLeftAction
                                    else -> MessageSwipeAction.NONE
                                }
                                val isTriggered = abs(clamped) >= swipeThresholdPx && active != MessageSwipeAction.NONE
                                if (isTriggered && !hasSnappedThreshold) {
                                    hasSnappedThreshold = true
                                    TxturaHaptics.swipeSnap(view, context)
                                } else if (!isTriggered && hasSnappedThreshold) {
                                    hasSnappedThreshold = false
                                    TxturaHaptics.tick(view, context)
                                }
                                scope.launch {
                                    swipeOffsetX.snapTo(clamped)
                                }
                            },
                            onDragEnd = {
                                hasSnappedThreshold = false
                                val finalOffset = swipeOffsetX.value
                                when {
                                    finalOffset >= swipeThresholdPx && swipeRightAction != MessageSwipeAction.NONE -> {
                                        if (swipeRightAction == MessageSwipeAction.DELETE) {
                                            TxturaHaptics.reject(view, context)
                                        } else {
                                            TxturaHaptics.confirm(view, context)
                                        }
                                        currentOnExecuteSwipe(swipeRightAction)
                                    }
                                    finalOffset <= -swipeThresholdPx && swipeLeftAction != MessageSwipeAction.NONE -> {
                                        if (swipeLeftAction == MessageSwipeAction.DELETE) {
                                            TxturaHaptics.reject(view, context)
                                        } else {
                                            TxturaHaptics.confirm(view, context)
                                        }
                                        currentOnExecuteSwipe(swipeLeftAction)
                                    }
                                }
                                scope.launch {
                                    swipeOffsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = TxturaMotion.swipeSnapBackSpec()
                                    )
                                }
                            },
                            onDragCancel = {
                                hasSnappedThreshold = false
                                scope.launch {
                                    swipeOffsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = TxturaMotion.swipeSnapBackSpec()
                                    )
                                }
                            }
                        )
                    }
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
                                        return@withTimeoutOrNull 2 // MOVED (scrolling / swiping)
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
                                        event.changes.forEach { it.consume() }
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
                                    // Finger moved > touchSlop (scrolling or swiping). Do nothing!
                                }
                            }
                        }
                    }
                    .padding(horizontal = (12f * s).dp, vertical = (8f * s).dp)
            ) {
            Column {
                if (isJumpHighlighted) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = (4f * s).dp)
                    ) {
                        Icon(
                            imageVector = TxturaIcons.Search,
                            contentDescription = null,
                            tint = if (isOutgoing) Color.White else palette.accentPrimary,
                            modifier = Modifier.size((13f * s).dp)
                        )
                        Spacer(modifier = Modifier.width((4f * s).dp))
                        Text(
                            text = if (strings.isSerbian) "Pronađena poruka" else "Matched message",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = (10f * s).sp),
                            color = if (isOutgoing) Color.White else palette.accentPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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

                // Automatically detected phone numbers, email addresses, bank accounts, payment refs, amounts, dates, and tracking numbers
                if (
                    detectedPhones.isNotEmpty() ||
                    detectedEmails.isNotEmpty() ||
                    detectedBankAccounts.isNotEmpty() ||
                    detectedPaymentRefs.isNotEmpty() ||
                    detectedPaymentAmounts.isNotEmpty() ||
                    detectedActionableDates.isNotEmpty() ||
                    detectedTrackingNumbers.isNotEmpty()
                ) {
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
                        detectedPaymentRefs.forEach { ref ->
                            GlassCard(
                                shape = RoundedCornerShape(50),
                                elevated = true,
                                accentHighlight = palette.safeColor,
                                contentPadding = PaddingValues(horizontal = (8f * s).dp, vertical = (4f * s).dp),
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clip = ClipData.newPlainText("Payment Reference", ref.referenceNumber)
                                    clipboard?.setPrimaryClip(clip)
                                    Toast.makeText(
                                        context,
                                        strings.paymentReferenceCopiedToast(ref.referenceNumber, ref.model),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = TxturaIcons.CopyKey,
                                        contentDescription = null,
                                        tint = palette.safeColor,
                                        modifier = Modifier.size((12f * s).dp)
                                    )
                                    Spacer(modifier = Modifier.width((4f * s).dp))
                                    Text(
                                        text = strings.paymentReferenceChipLabel(ref.referenceNumber, ref.model),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = (11f * s).sp),
                                        color = palette.safeColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        detectedPaymentAmounts.forEach { amt ->
                            GlassCard(
                                shape = RoundedCornerShape(50),
                                elevated = true,
                                accentHighlight = palette.safeColor,
                                contentPadding = PaddingValues(horizontal = (8f * s).dp, vertical = (4f * s).dp),
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clip = ClipData.newPlainText("Payment Amount", amt.normalizedValue)
                                    clipboard?.setPrimaryClip(clip)
                                    Toast.makeText(
                                        context,
                                        strings.paymentAmountCopiedToast(amt.normalizedValue, amt.currency),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = TxturaIcons.CopyKey,
                                        contentDescription = null,
                                        tint = palette.safeColor,
                                        modifier = Modifier.size((12f * s).dp)
                                    )
                                    Spacer(modifier = Modifier.width((4f * s).dp))
                                    Text(
                                        text = strings.paymentAmountChipLabel(amt.displayAmount),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = (11f * s).sp),
                                        color = palette.safeColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        detectedActionableDates.forEach { dateEvt ->
                            GlassCard(
                                shape = RoundedCornerShape(50),
                                elevated = true,
                                accentHighlight = palette.accentSecondary,
                                contentPadding = PaddingValues(horizontal = (8f * s).dp, vertical = (4f * s).dp),
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_INSERT).apply {
                                            data = CalendarContract.Events.CONTENT_URI
                                            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, dateEvt.startEpochMillis)
                                            putExtra(
                                                CalendarContract.EXTRA_EVENT_END_TIME,
                                                dateEvt.startEpochMillis + 3_600_000L
                                            )
                                            putExtra(
                                                CalendarContract.Events.TITLE,
                                                strings.calendarReminderDefaultTitle(dateEvt.isPaymentDeadline)
                                            )
                                            putExtra(CalendarContract.Events.DESCRIPTION, message.body)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        // ignore if no calendar app installed
                                    }
                                }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = TxturaIcons.CalendarEvent,
                                        contentDescription = null,
                                        tint = palette.accentSecondary,
                                        modifier = Modifier.size((12f * s).dp)
                                    )
                                    Spacer(modifier = Modifier.width((4f * s).dp))
                                    Text(
                                        text = strings.actionableDateChipLabel(dateEvt.displayDate, dateEvt.isPaymentDeadline),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = (11f * s).sp),
                                        color = palette.accentSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        detectedTrackingNumbers.forEach { trk ->
                            GlassCard(
                                shape = RoundedCornerShape(50),
                                elevated = true,
                                accentHighlight = palette.accentPrimary,
                                contentPadding = PaddingValues(horizontal = (8f * s).dp, vertical = (4f * s).dp),
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clip = ClipData.newPlainText("Tracking Code", trk.trackingCode)
                                    clipboard?.setPrimaryClip(clip)
                                    Toast.makeText(
                                        context,
                                        strings.packageTrackingCopiedToast(trk.trackingCode),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = TxturaIcons.DocumentFile,
                                        contentDescription = null,
                                        tint = palette.accentPrimary,
                                        modifier = Modifier.size((12f * s).dp)
                                    )
                                    Spacer(modifier = Modifier.width((4f * s).dp))
                                    Text(
                                        text = strings.packageTrackingChipLabel(trk.trackingCode, trk.courierName),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = (11f * s).sp),
                                        color = palette.accentPrimary,
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
                        if (onMarkMessageSafe != null) {
                            Spacer(modifier = Modifier.height((6f * s).dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(palette.safeColor.copy(alpha = 0.18f))
                                    .border(
                                        width = 1.dp,
                                        color = palette.safeColor.copy(alpha = 0.65f),
                                        shape = RoundedCornerShape(50)
                                    )
                                    .clickable { onMarkMessageSafe() }
                                    .padding(horizontal = (10f * s).dp, vertical = (4f * s).dp)
                            ) {
                                Icon(
                                    imageVector = TxturaIcons.ShieldCheck,
                                    contentDescription = null,
                                    tint = palette.safeColor,
                                    modifier = Modifier.size((13f * s).dp)
                                )
                                Spacer(modifier = Modifier.width((5f * s).dp))
                                Text(
                                    text = strings.markMessageNotHarmfulAction,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = (11f * s).sp
                                    ),
                                    color = palette.safeColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
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

                // 2b. Send SMS to phone number
                if (isPhone) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevated = true,
                        accentHighlight = palette.accentSecondary,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_SENDTO, "smsto:$value".toUri())
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                // ignore
                            }
                            onDismiss()
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = TxturaIcons.ChatBubble,
                                contentDescription = null,
                                tint = palette.accentSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = strings.sendSmsToNumberAction,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
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
    var titleFontSizeSp by remember(strings.defaultSmsPromptTitle) { mutableFloatStateOf(16.5f) }
    var buttonFontSizeSp by remember(strings.defaultSmsPromptConfirm) { mutableFloatStateOf(14f) }

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        accentBorderColor = palette.accentPrimary,
        usePlatformDefaultWidth = false,
        modifier = Modifier.fillMaxWidth(0.94f),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = TxturaIcons.ShieldCheck,
                    contentDescription = null,
                    tint = palette.accentPrimary,
                    modifier = Modifier.size(21.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.defaultSmsPromptTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = titleFontSizeSp.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = palette.textPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    onTextLayout = { layoutResult ->
                        if (layoutResult.didOverflowWidth && titleFontSizeSp > 11f) {
                            titleFontSizeSp -= 0.5f
                        }
                    }
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
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = strings.defaultSmsPromptLater,
                        color = palette.textSecondary,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = buttonFontSizeSp.sp
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                TextButton(
                    onClick = onConfirm,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = strings.defaultSmsPromptConfirm,
                        color = palette.accentPrimary,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = buttonFontSizeSp.sp
                        ),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        onTextLayout = { layoutResult ->
                            if (layoutResult.didOverflowWidth && buttonFontSizeSp > 11f) {
                                buttonFontSizeSp -= 0.5f
                            }
                        }
                    )
                }
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
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    var hasSnappedThreshold by remember { mutableStateOf(false) }

    val isUnread = thread.unreadCount > 0
    val rightActionText = remember(swipeRightAction, thread.isPinned, isUnread, strings) {
        when (swipeRightAction) {
            SwipeAction.PIN -> if (thread.isPinned) strings.swipeGestureUnpin else strings.swipeGesturePin
            SwipeAction.MARK_READ -> if (isUnread) strings.swipeGestureMarkRead else strings.swipeGestureMarkUnread
            else -> swipeActionLabel(swipeRightAction, strings)
        }
    }
    val leftActionText = remember(swipeLeftAction, thread.isPinned, isUnread, strings) {
        when (swipeLeftAction) {
            SwipeAction.PIN -> if (thread.isPinned) strings.swipeGestureUnpin else strings.swipeGesturePin
            SwipeAction.MARK_READ -> if (isUnread) strings.swipeGestureMarkRead else strings.swipeGestureMarkUnread
            else -> swipeActionLabel(swipeLeftAction, strings)
        }
    }

    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
    // Outer horizontal padding (16.dp) + icon (20.dp) + spacer (8.dp) + inner gap (14.dp) = 58.dp + exact text width
    val baseActionChromePx = with(density) { 58.dp.toPx() }
    val maxRightRevealPx = remember(rightActionText, labelStyle, baseActionChromePx, swipeRightAction, maxDragPx) {
        if (swipeRightAction == SwipeAction.NONE) {
            0f
        } else {
            val textWidthPx = textMeasurer.measure(
                text = rightActionText,
                style = labelStyle,
                maxLines = 1,
                softWrap = false
            ).size.width.toFloat()
            (baseActionChromePx + textWidthPx).coerceAtMost(maxDragPx)
        }
    }
    val maxLeftRevealPx = remember(leftActionText, labelStyle, baseActionChromePx, swipeLeftAction, maxDragPx) {
        if (swipeLeftAction == SwipeAction.NONE) {
            0f
        } else {
            val textWidthPx = textMeasurer.measure(
                text = leftActionText,
                style = labelStyle,
                maxLines = 1,
                softWrap = false
            ).size.width.toFloat()
            (baseActionChromePx + textWidthPx).coerceAtMost(maxDragPx)
        }
    }
    val rightTriggerPx = (maxRightRevealPx * 0.88f).coerceAtMost(thresholdPx.coerceAtLeast(maxRightRevealPx * 0.88f))
    val leftTriggerPx = (maxLeftRevealPx * 0.88f).coerceAtMost(thresholdPx.coerceAtLeast(maxLeftRevealPx * 0.88f))

    val currentOffset = offsetX.value
    val activeAction = when {
        currentOffset > 8f -> swipeRightAction
        currentOffset < -8f -> swipeLeftAction
        else -> SwipeAction.NONE
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(
                thread.id,
                swipeRightAction,
                swipeLeftAction,
                maxRightRevealPx,
                maxLeftRevealPx,
                rightTriggerPx,
                leftTriggerPx
            ) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        TxturaHaptics.tick(view, context)
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val proposed = offsetX.value + dragAmount
                        val clamped = proposed.coerceIn(-maxLeftRevealPx, maxRightRevealPx)
                        val isTriggered = (clamped > 0f && swipeRightAction != SwipeAction.NONE && clamped >= rightTriggerPx) ||
                            (clamped < 0f && swipeLeftAction != SwipeAction.NONE && abs(clamped) >= leftTriggerPx)
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
                            finalOffset >= rightTriggerPx && swipeRightAction != SwipeAction.NONE -> {
                                if (swipeRightAction == SwipeAction.DELETE) {
                                    TxturaHaptics.reject(view, context)
                                } else {
                                    TxturaHaptics.confirm(view, context)
                                }
                                onExecuteSwipeAction(swipeRightAction)
                            }
                            finalOffset <= -leftTriggerPx && swipeLeftAction != SwipeAction.NONE -> {
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
        if (abs(currentOffset) > 6f && activeAction != SwipeAction.NONE) {
            val actionColor = swipeActionColor(activeAction, isUnread = isUnread)
            val isSwipingRight = currentOffset > 0f
            val isTriggered = if (isSwipingRight) {
                currentOffset >= rightTriggerPx
            } else {
                abs(currentOffset) >= leftTriggerPx
            }
            val activeActionText = if (isSwipingRight) rightActionText else leftActionText

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
                    .padding(horizontal = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = swipeActionIcon(activeAction, isPinned = thread.isPinned, isUnread = isUnread),
                        contentDescription = null,
                        tint = actionColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = activeActionText,
                        style = labelStyle,
                        color = actionColor,
                        maxLines = 1,
                        softWrap = false
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
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = palette.textPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
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
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = palette.textPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
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
    var pendingRestoreEntry by remember { mutableStateOf<DeletedConversationEntry?>(null) }
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
                                contentPadding = PaddingValues(14.dp),
                                onClick = { inspectingEntry = entry }
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
                                                onClick = { inspectingEntry = entry }
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
                                            onClick = { pendingRestoreEntry = entry }
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

        val entryToRestore = pendingRestoreEntry
        if (entryToRestore != null) {
            val displayName = entryToRestore.thread.contactName.trim().ifBlank { entryToRestore.thread.address }
            GlassAlertDialog(
                onDismissRequest = { pendingRestoreEntry = null },
                accentBorderColor = palette.safeColor,
                title = {
                    Text(
                        text = strings.confirmRestoreThreadTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.textPrimary
                    )
                },
                text = {
                    Text(
                        text = strings.confirmRestoreThreadMessage(displayName),
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textPrimary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onRestoreThread(entryToRestore.thread.id)
                            pendingRestoreEntry = null
                        }
                    ) {
                        Text(
                            text = strings.restoreButton,
                            color = palette.safeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingRestoreEntry = null }) {
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
    var showConfirmRestoreSelected by remember { mutableStateOf(false) }
    var showConfirmDeleteSelected by remember { mutableStateOf(false) }

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
                    val contactDisplayName = entry.thread.contactName.trim().ifBlank { entry.thread.address }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = contactDisplayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subtitleText = if (contactDisplayName != entry.thread.address && entry.thread.address.isNotBlank()) {
                            "${entry.thread.address} • ${strings.daysRemainingBadge(entry.daysRemaining(retentionDays))}"
                        } else {
                            strings.daysRemainingBadge(entry.daysRemaining(retentionDays))
                        }
                        Text(
                            text = subtitleText,
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
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = formatTimestamp(msg.timestampMillis),
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
                            showConfirmRestoreSelected = true
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
                            showConfirmDeleteSelected = true
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

    if (showConfirmRestoreSelected) {
        GlassAlertDialog(
            onDismissRequest = { showConfirmRestoreSelected = false },
            accentBorderColor = palette.safeColor,
            title = {
                Text(
                    text = strings.confirmRestoreSelectedTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )
            },
            text = {
                Text(
                    text = strings.confirmRestoreSelectedMessage(selectedMsgIds.size),
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.textPrimary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRestoreMessages(entry.thread.id, selectedMsgIds)
                        showConfirmRestoreSelected = false
                        onDismiss()
                    }
                ) {
                    Text(
                        text = strings.restoreButton,
                        color = palette.safeColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmRestoreSelected = false }) {
                    Text(
                        text = strings.cancel,
                        color = palette.textSecondary
                    )
                }
            }
        )
    }

    if (showConfirmDeleteSelected) {
        GlassAlertDialog(
            onDismissRequest = { showConfirmDeleteSelected = false },
            accentBorderColor = palette.dangerColor,
            title = {
                Text(
                    text = strings.confirmDeleteSelectedTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )
            },
            text = {
                Text(
                    text = strings.confirmDeleteSelectedMessage(selectedMsgIds.size),
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.textPrimary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onPermanentlyDeleteMessages(entry.thread.id, selectedMsgIds)
                        showConfirmDeleteSelected = false
                        onDismiss()
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
                TextButton(onClick = { showConfirmDeleteSelected = false }) {
                    Text(
                        text = strings.cancel,
                        color = palette.textSecondary
                    )
                }
            }
        )
    }
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
    zoomScale: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    val dateText = remember(timestampMillis, strings.isSerbian) {
        formatChatDateHeader(timestampMillis, strings.isSerbian)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = (6f * zoomScale).dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(palette.glassSurfaceElevated.copy(alpha = 0.95f))
                .border(
                    width = 1.dp,
                    color = palette.accentPrimary.copy(alpha = 0.5f),
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
                color = palette.accentPrimary
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
