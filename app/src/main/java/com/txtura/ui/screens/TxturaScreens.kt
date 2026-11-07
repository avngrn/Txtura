@file:Suppress("SpellCheckingInspection")

package com.txtura.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.core.net.toUri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
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
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.txtura.R
import com.txtura.data.BackupRestorePayload
import com.txtura.data.ChatMessage
import com.txtura.data.ContactPickerItem
import com.txtura.data.ConversationFilter
import com.txtura.data.ConversationThread
import com.txtura.data.DeletedConversationEntry
import com.txtura.data.DeletedGroupingMode
import com.txtura.data.DeliveryStatus
import com.txtura.data.MainListLayoutMode
import com.txtura.data.MessageType
import com.txtura.data.MmsAttachment
import com.txtura.data.ScheduledSmsMessage
import com.txtura.data.SwipeAction
import com.txtura.data.UiColorMode
import com.txtura.data.WallpaperConfig
import com.txtura.data.WallpaperPreset
import com.txtura.data.WallpaperTexture
import com.txtura.security.MessageSecurityAnalyzer
import com.txtura.security.SecurityRiskLevel
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
import com.txtura.ui.theme.LocalGlassPalette
import com.txtura.ui.theme.TxturaThemeMode
import com.txtura.ui.theme.WallpaperHarmonyEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

@Composable
fun ConversationsListScreen(
    threads: List<ConversationThread>,
    deletedCount: Int = 0,
    autoDeleteDays: Int = 30,
    showContactPhotos: Boolean = true,
    listLayoutMode: MainListLayoutMode = MainListLayoutMode.DEFAULT,
    mainWallpaper: WallpaperConfig = WallpaperConfig(),
    swipeRightAction: SwipeAction = SwipeAction.PIN,
    swipeLeftAction: SwipeAction = SwipeAction.DELETE,
    categoryOrder: List<ConversationFilter> = ConversationFilter.defaultOrder,
    onUpdateCategoryOrder: (List<ConversationFilter>) -> Unit = {},
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
    onMarkAllRead: () -> Unit = {},
    onOpenSettings: () -> Unit,
) {
    val strings = TxturaStrings.current()
    var searchQuery by remember { mutableStateOf(value = "") }
    val activeCategories = remember(categoryOrder) {
        categoryOrder.ifEmpty { ConversationFilter.defaultOrder }
    }
    var selectedFilter by remember { mutableStateOf(value = activeCategories.firstOrNull() ?: ConversationFilter.PEOPLE) }
    var showMarkAllReadConfirmation by remember { mutableStateOf(value = false) }

    // Multi-selection mode triggered by long-pressing any conversation card on the main screen
    var isSelectionMode by remember { mutableStateOf(value = false) }
    var selectedThreadIds by remember { mutableStateOf<Set<Long>>(value = emptySet()) }
    var pendingBatchAction by remember { mutableStateOf<BatchSelectionAction?>(value = null) }

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedThreadIds = emptySet()
    }

    val filteredThreads = remember(threads, searchQuery, selectedFilter) {
        val list = threads.filter { (_, contactName, address, _, _, _, messages, unreadCount, _, isService) ->
            val matchesSearch = searchQuery.isBlank() ||
                contactName.contains(searchQuery, ignoreCase = true) ||
                address.contains(searchQuery, ignoreCase = true) ||
                messages.any { it.body.contains(searchQuery, ignoreCase = true) }

            val matchesFilter = when (selectedFilter) {
                ConversationFilter.PEOPLE -> !isService
                ConversationFilter.SERVICES -> isService
                ConversationFilter.UNREAD -> unreadCount > 0
                ConversationFilter.ALL -> true
            }
            matchesSearch && matchesFilter
        }
        list.sortedWith(
            compareByDescending<ConversationThread> { it.isPinned }
                .thenByDescending { it.lastMessage?.timestampMillis ?: 0L },
        )
    }

    GlassBackground(wallpaperConfig = mainWallpaper) {
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
                                    onDeleteSelectedThreads(selectedThreadIds)
                                    isSelectionMode = false
                                    selectedThreadIds = emptySet()
                                }
                                BatchSelectionAction.MARK_READ -> {
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

            // Top Header: Clean "Txtura" branding (or Select All / Deselect All + selection count) on left; actions on right
            val allSelected = filteredThreads.isNotEmpty() && filteredThreads.all { it.id in selectedThreadIds }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isSelectionMode) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GlassTooltipIconButton(
                            icon = if (allSelected) TxturaIcons.DeselectAll else TxturaIcons.SelectAll,
                            tooltipText = if (allSelected) {
                                strings.bottomBarDeselectAllTooltip
                            } else {
                                strings.bottomBarSelectAllTooltip
                            },
                            tint = if (allSelected) palette.textPrimary else palette.safeColor,
                            onClick = {
                                if (allSelected) {
                                    selectedThreadIds = emptySet()
                                    isSelectionMode = false
                                } else if (filteredThreads.isNotEmpty()) {
                                    selectedThreadIds = filteredThreads.asSequence().map { it.id }.toSet()
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = strings.selectedThreadsCountLabel(selectedThreadIds.size),
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.accentPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    val isWhiteBackground = !palette.isDark ||
                        (!mainWallpaper.isCustomGalleryImage && (mainWallpaper.preset == WallpaperPreset.ALL_WHITE))
                    val logoColorFilter = rememberTxturaLightLogoColorFilter(
                        palette = palette,
                        wallpaperConfig = mainWallpaper,
                        isWhiteBackground = isWhiteBackground
                    )
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier.height(44.dp)
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
                    }
                }

                // Top-right actions:
                // When in selection mode, hide standard top-right icons and show selection actions
                // (Delete, Mark as read, Close selection) in the top-right corner. Once selection finishes,
                // restore standard top-right icons (+, Trash, Mark All Read, Settings Gear).
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelectionMode) {
                        // 1. Izbriši (Delete selected)
                        GlassTooltipIconButton(
                            icon = TxturaIcons.Delete,
                            tooltipText = strings.bottomBarDeleteTooltip,
                            tint = palette.dangerColor,
                            onClick = {
                                if (selectedThreadIds.isNotEmpty()) {
                                    pendingBatchAction = BatchSelectionAction.DELETE
                                }
                            }
                        )

                        // 2. Označi kao pročitano (Mark selected as read)
                        GlassTooltipIconButton(
                            icon = TxturaIcons.CheckDouble,
                            tooltipText = strings.bottomBarMarkReadTooltip,
                            tint = palette.accentPrimary,
                            onClick = {
                                if (selectedThreadIds.isNotEmpty()) {
                                    pendingBatchAction = BatchSelectionAction.MARK_READ
                                }
                            }
                        )

                        // 3. Otkaži označavanje (Close selection mode)
                        GlassTooltipIconButton(
                            icon = TxturaIcons.Close,
                            tooltipText = strings.cancel,
                            tint = palette.textSecondary,
                            onClick = {
                                isSelectionMode = false
                                selectedThreadIds = emptySet()
                            }
                        )
                    } else {
                        GlassTooltipIconButton(
                            icon = TxturaIcons.Plus,
                            tooltipText = strings.newMessage,
                            tint = palette.accentPrimary,
                            onClick = onOpenNewMessage
                        )

                        Box {
                            GlassTooltipIconButton(
                                icon = TxturaIcons.Delete,
                                tooltipText = strings.deletedMessagesTitle,
                                tint = palette.accentPrimary,
                                onClick = onOpenDeletedMessages
                            )
                            if (deletedCount > 0) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(palette.accentPrimary)
                                ) {
                                    Text(
                                        text = deletedCount.coerceAtMost(99).toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (palette.isDark) Color.Black else Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Mark All As Read icon placed between Trash and Settings Gear
                        GlassTooltipIconButton(
                            icon = TxturaIcons.CheckDouble,
                            tooltipText = strings.markAllReadTooltip,
                            tint = palette.accentPrimary,
                            onClick = { showMarkAllReadConfirmation = true }
                        )

                        GlassTooltipIconButton(
                            icon = TxturaIcons.SettingsGear,
                            tooltipText = strings.settingsTooltip,
                            tint = palette.accentPrimary,
                            onClick = onOpenSettings
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

            // Single-line swipeable horizontal category bar.
            // Holding a category chip and dragging it left or right reorders categories live!
            DraggableCategoryBar(
                categories = activeCategories,
                selectedFilter = selectedFilter,
                strings = strings,
                onSelectFilter = { selectedFilter = it },
                onUpdateCategoryOrder = onUpdateCategoryOrder
            )

            Spacer(modifier = Modifier.height(10.dp))

            val density = LocalDensity.current
            val swipeThresholdPx = remember(density) { with(density) { 105.dp.toPx() } }
            val swipeMaxDragPx = remember(density) { with(density) { 165.dp.toPx() } }

            // Conversations List
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(
                        if (listLayoutMode == MainListLayoutMode.COMPACT) 6.dp else 10.dp
                    ),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = filteredThreads,
                        key = { it.id },
                        contentType = { "conversation_thread" }
                    ) { thread ->
                        val isChecked = thread.id in selectedThreadIds
                        val toggleSelection = {
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

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
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
                                            SwipeAction.PIN -> onTogglePin(thread.id)
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
                                        strings = strings,
                                        onClick = {
                                            if (isSelectionMode) {
                                                toggleSelection()
                                            } else {
                                                onOpenThread(thread.id)
                                            }
                                        },
                                        onLongClick = {
                                            isSelectionMode = true
                                            selectedThreadIds += thread.id
                                        },
                                        onTogglePin = { onTogglePin(thread.id) },
                                        onOpenContact = { onOpenContact(thread.address, thread.contactName) },
                                        onCopyOtp = onCopyOtp
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
    onSelectFilter: (ConversationFilter) -> Unit,
    onUpdateCategoryOrder: (List<ConversationFilter>) -> Unit
) {
    val palette = LocalGlassPalette.current
    val density = LocalDensity.current
    val spacingPx = remember(density) { with(density) { 8.dp.toPx() } }
    val scrollState = rememberScrollState()

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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState, enabled = draggedFilter == null)
    ) {
        localOrder.forEach { filter ->
            key(filter) {
                val selected = filter == selectedFilter
                val isDragged = filter == draggedFilter
                val label = filterLabel(filter, strings)

                Box(
                    modifier = Modifier
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
                                                onUpdateCategoryOrder(updated)
                                            }
                                        }
                                    }
                                },
                                onDragEnd = {
                                    draggedFilter = null
                                    dragOffsetX = 0f
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
                        shape = RoundedCornerShape(50),
                        elevated = selected || isDragged,
                        accentHighlight = when {
                            isDragged -> palette.safeColor
                            selected -> palette.accentPrimary
                            else -> null
                        },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        onClick = { onSelectFilter(filter) }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = when {
                                isDragged -> palette.safeColor
                                selected -> palette.accentPrimary
                                else -> palette.textSecondary
                            },
                            fontWeight = if (selected || isDragged) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

fun filterLabel(filter: ConversationFilter, strings: AppStrings): String {
    return when (filter) {
        ConversationFilter.PEOPLE -> strings.filterPeople
        ConversationFilter.SERVICES -> strings.filterServices
        ConversationFilter.UNREAD -> strings.filterUnread
        ConversationFilter.ALL -> strings.filterAll
    }
}

private enum class SettingsCategoryWindow {
    APPEARANCE,
    MESSAGES_AND_SWIPE,
    BLOCKED_SENDERS,
    BACKUP_AND_RESTORE,
    PERMISSIONS_AND_ROLE,
}

/**
 * Dedicated Settings Screen ("Settings" / "Podešavanja") organized into 5 categories.
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
    blockedSenders: List<String> = emptyList(),
    lastBackupTimestamp: Long = 0L,
    hasSmsPermission: Boolean,
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
    onRequestDefaultSmsRole: () -> Unit,
    onOpenSystemAppSettings: () -> Unit
) {
    val context = LocalContext.current
    val strings = TxturaStrings.current()
    val (appVersionName, appVersionCode) = remember(context) {
        runCatching {
            val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val vName = pkgInfo.versionName ?: "1.0"
            val vCode = pkgInfo.longVersionCode
            vName to vCode
        }.getOrDefault("1.0" to 1L)
    }

    // Active category window (null = main Settings categories overview window)
    var activeCategoryWindow by remember { mutableStateOf<SettingsCategoryWindow?>(null) }
    var showSecurityMeasuresDialog by remember { mutableStateOf(value = false) }

    BackHandler(enabled = activeCategoryWindow != null) {
        activeCategoryWindow = null
    }

    GlassBackground(wallpaperConfig = mainWallpaper) {
        val palette = LocalGlassPalette.current

        AnimatedContent(
            targetState = activeCategoryWindow,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
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

                    // List of 5 Settings Category Cards — each opens as a new window on click
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(windowScrollState)
                            .padding(bottom = 24.dp)
                    ) {
                        // 1. Izgled, Teme i Providnost
                        SettingsCategoryWindowCard(
                            icon = TxturaIcons.SparklesTheme,
                            iconTint = palette.accentPrimary,
                            title = strings.appearanceSectionTitle,
                            accentHighlight = null,
                        ) { activeCategoryWindow = SettingsCategoryWindow.APPEARANCE }

                        // 2. Poruke, Kategorije i Svajp radnje
                        SettingsCategoryWindowCard(
                            icon = TxturaIcons.SwipeHoriz,
                            iconTint = palette.accentPrimary,
                            title = strings.swipeSectionTitle,
                            accentHighlight = null,
                        ) { activeCategoryWindow = SettingsCategoryWindow.MESSAGES_AND_SWIPE }

                        // 3. Bekap i vraćanje podataka
                        SettingsCategoryWindowCard(
                            icon = TxturaIcons.FolderBackup,
                            iconTint = palette.accentPrimary,
                            title = strings.backupSectionTitle,
                            accentHighlight = null,
                        ) { activeCategoryWindow = SettingsCategoryWindow.BACKUP_AND_RESTORE }

                        // 4. Blokiranje pošiljalaca poruka (pretposlednji izbor)
                        SettingsCategoryWindowCard(
                            icon = TxturaIcons.Block,
                            iconTint = palette.accentPrimary,
                            pressedIconTint = palette.dangerColor,
                            title = strings.blockedSendersSectionTitle,
                            accentHighlight = null,
                        ) { activeCategoryWindow = SettingsCategoryWindow.BLOCKED_SENDERS }

                        // 5. Dozvole i Podrazumevana Aplikacija
                        SettingsCategoryWindowCard(
                            icon = TxturaIcons.PermissionsKey,
                            iconTint = palette.accentPrimary,
                            title = strings.permissionsSectionTitle,
                            accentHighlight = if (!hasSmsPermission || !isDefaultSmsApp) palette.accentPrimary else null,
                        ) { activeCategoryWindow = SettingsCategoryWindow.PERMISSIONS_AND_ROLE }
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
                            contentDescription = "Txtura",
                            contentScale = ContentScale.Fit,
                            colorFilter = footerLogoColorFilter,
                            modifier = Modifier.height(26.dp)
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = strings.settingsFooterVersion(appVersionName, appVersionCode),
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textPrimary.copy(alpha = 0.88f),
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = TxturaIcons.ShieldCheck,
                                contentDescription = null,
                                tint = palette.safeColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = strings.settingsFooterPrivacyBadge,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = palette.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }

                        Text(
                            text = strings.settingsFooterCopyright,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = palette.textSecondary.copy(alpha = 0.72f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    val (windowIcon, windowTint, windowTitle) = when (currentWindow) {
                        SettingsCategoryWindow.APPEARANCE -> Triple(
                            TxturaIcons.SparklesTheme,
                            palette.accentPrimary,
                            strings.appearanceSectionTitle
                        )
                        SettingsCategoryWindow.MESSAGES_AND_SWIPE -> Triple(
                            TxturaIcons.SwipeHoriz,
                            palette.accentPrimary,
                            strings.swipeSectionTitle
                        )
                        SettingsCategoryWindow.BLOCKED_SENDERS -> Triple(
                            TxturaIcons.Block,
                            palette.dangerColor,
                            strings.blockedSendersSectionTitle
                        )
                        SettingsCategoryWindow.BACKUP_AND_RESTORE -> Triple(
                            TxturaIcons.FolderBackup,
                            palette.accentPrimary,
                            strings.backupSectionTitle
                        )
                        SettingsCategoryWindow.PERMISSIONS_AND_ROLE -> Triple(
                            TxturaIcons.PermissionsKey,
                            palette.accentPrimary,
                            strings.permissionsSectionTitle
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
                            SettingsCategoryWindow.APPEARANCE -> {
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

                                // 1C. Pozadine i Teme (prikazano direktno u prozoru bez padajućeg menija)
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = strings.wallpaperSectionTitle,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = palette.textPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        WallpaperPresetSelectorGrid(
                                            currentConfig = mainWallpaper,
                                            uiColorMode = uiColorMode,
                                            strings = strings,
                                            onSelectPreset = onSelectMainWallpaperPreset,
                                            onSelectTexture = onSelectMainWallpaperTexture,
                                            onPickFromGallery = onPickMainWallpaperFromGallery,
                                            onSwitchColorMode = onSelectUiColorMode
                                        )
                                    }
                                }

                                // 1D. Kompaktni prikaz poruka u glavnom prozoru (na dnu podešavanja prikaza)
                                val isCompactMode = listLayoutMode == MainListLayoutMode.COMPACT
                                SettingsSwitchRow(
                                    title = strings.layoutModeCompact,
                                    checked = isCompactMode
                                ) { compact ->
                                    onSelectListLayoutMode(
                                        if (compact) MainListLayoutMode.COMPACT else MainListLayoutMode.DEFAULT
                                    )
                                }

                                // 1E. Prikaz slika kontakata (na dnu podešavanja prikaza)
                                SettingsSwitchRow(
                                    title = strings.showContactPhotosTitle,
                                    checked = showContactPhotos,
                                    onCheckedChange = onToggleShowContactPhotos
                                )
                            }

                            SettingsCategoryWindow.MESSAGES_AND_SWIPE -> {
                                // 2.0 Opozovi poruke (uključuje ili isključuje tajmer od 6s za slanje poruka)
                                SettingsSwitchRow(
                                    title = strings.recallMessagesTitle,
                                    subtitle = strings.recallMessagesSub,
                                    checked = recallMessagesEnabled,
                                    onCheckedChange = onToggleRecallMessages
                                )

                                // 2.0b Identifikacija zlonamernih veb-sajtova u porukama (potreban je pristup internetu)
                                SettingsSwitchRow(
                                    title = strings.maliciousWebsitesTitle,
                                    subtitle = strings.maliciousWebsitesSub,
                                    checked = identifyMaliciousWebsites,
                                    onCheckedChange = onToggleIdentifyMaliciousWebsites
                                )

                                // 2A. Svajp udesno
                                SwipeDirectionInteractiveCard(
                                    title = strings.swipeRightLabel,
                                    currentAction = swipeRightAction,
                                    defaultFallbackAction = SwipeAction.PIN,
                                    strings = strings,
                                    onSelectAction = onSelectSwipeRightAction
                                )

                                // 2B. Svajp ulevo
                                SwipeDirectionInteractiveCard(
                                    title = strings.swipeLeftLabel,
                                    currentAction = swipeLeftAction,
                                    defaultFallbackAction = SwipeAction.DELETE,
                                    strings = strings,
                                    onSelectAction = onSelectSwipeLeftAction
                                )

                                // 2C. Spojena kartica za obrisane poruke (broj dana, grupisanje/sortiranje i otvaranje obrisanih poruka)
                                var autoDeleteDaysInput by remember { mutableStateOf(autoDeleteDays.toString()) }
                                LaunchedEffect(autoDeleteDays) {
                                    if (autoDeleteDaysInput.isNotEmpty() && autoDeleteDaysInput.toIntOrNull() != autoDeleteDays) {
                                        autoDeleteDaysInput = autoDeleteDays.toString()
                                    }
                                }

                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(14.dp)
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
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

                            SettingsCategoryWindow.BLOCKED_SENDERS -> {
                                if (blockedSenders.isEmpty()) {
                                    GlassCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        contentPadding = PaddingValues(16.dp)
                                    ) {
                                        Text(
                                            text = strings.blockedSendersEmpty,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = palette.textSecondary
                                        )
                                    }
                                } else {
                                    blockedSenders.forEach { sender ->
                                        GlassCard(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
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
                                                        tint = palette.dangerColor,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        text = sender,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = palette.textPrimary,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                GlassCard(
                                                    shape = RoundedCornerShape(50),
                                                    accentHighlight = palette.safeColor,
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
                                                    onClick = { onUnblockSender(sender) }
                                                ) {
                                                    Text(
                                                        text = strings.unblockSenderButton,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = palette.safeColor,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            SettingsCategoryWindow.BACKUP_AND_RESTORE -> {
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

                            SettingsCategoryWindow.PERMISSIONS_AND_ROLE -> {
                                SettingsSwitchRow(
                                    title = strings.permissionsSectionTitle,
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
                    }
                }
            }
        }

        if (showSecurityMeasuresDialog) {
            SecurityMeasuresDialog(
                onDismiss = { showSecurityMeasuresDialog = false }
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalGlassPalette.current
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevated = checked,
        accentHighlight = if (checked) palette.accentPrimary else null,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        onClick = { onCheckedChange(!checked) }
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
                    fontWeight = FontWeight.SemiBold
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
    val selectableActions = remember {
        SwipeAction.entries.filter { it != SwipeAction.NONE }
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevated = enabled,
        accentHighlight = if (enabled) palette.accentPrimary else null,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
                        } else {
                            onSelectAction(SwipeAction.NONE)
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
                visible = enabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
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
private fun SecurityMeasuresDialog(
    onDismiss: () -> Unit
) {
    val palette = LocalGlassPalette.current
    val strings = TxturaStrings.current()
    val scrollState = rememberScrollState()

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
            GlassCard(
                shape = RoundedCornerShape(18.dp),
                accentHighlight = palette.safeColor,
                contentPadding = PaddingValues(12.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                ) {
                    SecurityItemRow(title = strings.secItem1Title, subtitle = strings.secItem1Sub)
                    SecurityItemRow(title = strings.secItem2Title, subtitle = strings.secItem2Sub)
                    SecurityItemRow(title = strings.secItem3Title, subtitle = strings.secItem3Sub)
                    SecurityItemRow(title = strings.secItem4Title, subtitle = strings.secItem4Sub)
                    SecurityItemRow(title = strings.secItem5Title, subtitle = strings.secItem5Sub)
                }
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

                    // Bottom Row: Message preview across available width (strictly 1 line)
                    Text(
                        text = lastMsg?.body ?: "",
                        style = if (isCompact) {
                            MaterialTheme.typography.labelMedium
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                        color = if (thread.unreadCount > 0) palette.textPrimary else palette.textSecondary,
                        fontWeight = if (thread.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
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

                    Icon(
                        imageVector = TxturaIcons.Pin,
                        contentDescription = if (thread.isPinned) strings.unpinContactThread else strings.pinContactThread,
                        tint = if (thread.isPinned) palette.accentPrimary else palette.textSecondary.copy(alpha = 0.72f),
                        modifier = Modifier
                            .size(15.dp)
                            .clickable(onClick = onTogglePin)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
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

@Composable
fun ConversationDetailScreen(
    thread: ConversationThread,
    showContactPhotos: Boolean = true,
    recallMessagesEnabled: Boolean = true,
    bubbleZoomScale: Float = 1.0f,
    onUpdateBubbleZoomScale: (Float) -> Unit = {},
    wallpaperConfig: WallpaperConfig = WallpaperConfig(),
    pendingAttachmentUri: Uri?,
    availableContacts: List<ContactPickerItem> = emptyList(),
    scheduledMessages: List<ScheduledSmsMessage> = emptyList(),
    onBack: () -> Unit,
    onTogglePinThread: () -> Unit,
    onTogglePinMessage: (Long) -> Unit,
    onToggleHighlightMessage: (Long) -> Unit = {},
    onDeleteMessage: (Long) -> Unit = {},
    onCopyMessageText: (String) -> Unit = {},
    onForwardMessage: (ChatMessage, String, String?) -> Unit = { _, _, _ -> },
    onOpenChatWallpaperPicker: () -> Unit = {},
    onCallSender: () -> Unit,
    onOpenContact: () -> Unit,
    onBlockContact: () -> Unit = {},
    onToggleSenderCategory: () -> Unit = {},
    onPickMmsPhoto: () -> Unit,
    onClearAttachment: () -> Unit,
    onSendMessage: (String, Uri?) -> Unit,
    onScheduleMessage: (String, Uri?, Long) -> Unit = { _, _, _ -> },
    onCancelScheduledMessage: (Long) -> Unit = {},
    onCopyOtp: (String) -> Unit
) {
    val context = LocalContext.current
    val strings = TxturaStrings.current()
    var draftText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val pinnedMessages = thread.pinnedMessages
    var activePinnedIndex by remember(thread.id) { mutableStateOf(0) }
    var prevPinnedCount by remember(thread.id) { mutableStateOf(pinnedMessages.size) }
    var jumpedToMessageId by remember(thread.id) { mutableStateOf<Long?>(null) }
    var showPinnedMessagesManageDialog by remember(thread.id) { mutableStateOf(value = false) }
    var selectedPinnedIdsForUnpin by remember(thread.id) { mutableStateOf<Set<Long>>(emptySet()) }
    val currentZoom by rememberUpdatedState(bubbleZoomScale)

    LaunchedEffect(pinnedMessages.size) {
        if (pinnedMessages.isNotEmpty()) {
            activePinnedIndex = if (pinnedMessages.size > prevPinnedCount) {
                pinnedMessages.lastIndex
            } else {
                activePinnedIndex.coerceIn(0, pinnedMessages.lastIndex)
            }
        } else {
            activePinnedIndex = 0
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
            currentPendingDelayed?.let { uncancelled ->
                currentOnSendMessage(uncancelled.body, uncancelled.attachmentUri)
            }
        }
    }

    // Message long-press inline options bubble state
    var messageWithOpenMenu by remember { mutableStateOf<ChatMessage?>(null) }
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
                    MmsAttachment(
                        uri = uri,
                        mimeType = "image/jpeg",
                        caption = pending.body
                    )
                }
            )
        }
    }

    // Top-right 3-dots overflow dropdown menu & block contact confirmation dialog state
    var showMoreMenu by remember { mutableStateOf(false) }
    var showConfirmBlockContactDialog by remember { mutableStateOf(false) }

    LaunchedEffect(thread.messages.size, pendingOutgoingPreviewMessage?.id) {
        val totalItems = thread.messages.size + (if (pendingOutgoingPreviewMessage != null) 1 else 0)
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    val isMmsMode = pendingAttachmentUri != null
    val hasNonAsciiOrEmoji = draftText.any { it.code > 127 }
    val charsPerSegment = if (hasNonAsciiOrEmoji) 70 else 160
    val smsSegments = if (draftText.isEmpty()) 1 else ((draftText.length - 1) / charsPerSegment) + 1

    GlassBackground(wallpaperConfig = wallpaperConfig) {
        val palette = LocalGlassPalette.current

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
                                        text = pinnedMsg.body.ifBlank { pinnedMsg.attachment?.caption ?: "📸 MMS" },
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
                                leadingIcon = {
                                    Icon(
                                        imageVector = TxturaIcons.AttachImage,
                                        contentDescription = null,
                                        tint = palette.accentPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
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
                                leadingIcon = {
                                    Icon(
                                        imageVector = TxturaIcons.Pin,
                                        contentDescription = null,
                                        tint = if (thread.isPinned) palette.accentPrimary else palette.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
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
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (thread.isService) TxturaIcons.ContactPerson else TxturaIcons.ShieldCheck,
                                        contentDescription = null,
                                        tint = palette.accentPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
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
                                leadingIcon = {
                                    Icon(
                                        imageVector = TxturaIcons.ContactPerson,
                                        contentDescription = null,
                                        tint = palette.accentPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
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
                                leadingIcon = {
                                    Icon(
                                        imageVector = TxturaIcons.Block,
                                        contentDescription = null,
                                        tint = palette.dangerColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
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
                                    onDragStart = { dragAccumY = 0f },
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        dragAccumY += dragAmount
                                        if (dragAccumY <= -24f) {
                                            activePinnedIndex = (safePinnedIndex + 1) % pinnedMessages.size
                                            dragAccumY = 0f
                                        } else if (dragAccumY >= 24f) {
                                            activePinnedIndex = (safePinnedIndex - 1 + pinnedMessages.size) % pinnedMessages.size
                                            dragAccumY = 0f
                                        }
                                    }
                                )
                            }
                        }
                        .pointerInput(pinnedMessages.size, safePinnedIndex) {
                            if (pinnedMessages.size > 1) {
                                detectHorizontalDragGestures(
                                    onDragStart = { dragAccumX = 0f },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        dragAccumX += dragAmount
                                        if (dragAccumX <= -32f) {
                                            activePinnedIndex = (safePinnedIndex + 1) % pinnedMessages.size
                                            dragAccumX = 0f
                                        } else if (dragAccumX >= 32f) {
                                            activePinnedIndex = (safePinnedIndex - 1 + pinnedMessages.size) % pinnedMessages.size
                                            dragAccumX = 0f
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
                                text = msgItem.body.ifBlank { msgItem.attachment?.caption ?: "📸 MMS" },
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
                                    text = "${scheduleFormat.format(Date(scheduledItem.scheduledAtMillis))} • ${scheduledItem.body.ifBlank { "📸 MMS" }}",
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

            // Messages Thread with two-finger Pinch-to-Zoom and Long-Press Context Menu on each bubble
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy((7f * bubbleZoomScale.coerceIn(0.75f, 1.65f)).dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
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
                items(
                    items = thread.messages,
                    key = { it.id },
                    contentType = { msg -> if (msg.attachment != null) "mms_bubble" else "sms_bubble" }
                ) { msg ->
                    val isMenuOpenForThisMessage = messageWithOpenMenu?.id == msg.id
                    MessageBubbleItem(
                        message = msg,
                        zoomScale = bubbleZoomScale,
                        strings = strings,
                        isOptionsBubbleVisible = isMenuOpenForThisMessage,
                        isJumpHighlighted = jumpedToMessageId == msg.id,
                        onLongPressMessage = {
                            messageWithOpenMenu = if (isMenuOpenForThisMessage) null else msg
                        },
                        onDismissOptionsBubble = {
                            if (isMenuOpenForThisMessage) {
                                messageWithOpenMenu = null
                            }
                        },
                        onForwardMessage = {
                            messageWithOpenMenu = null
                            messagePendingForward = msg
                        },
                        onDeleteMessage = {
                            messageWithOpenMenu = null
                            messagePendingDelete = msg
                        },
                        onCopyMessage = {
                            messageWithOpenMenu = null
                            onCopyMessageText(msg.body)
                        },
                        onToggleHighlight = {
                            messageWithOpenMenu = null
                            onToggleHighlightMessage(msg.id)
                        },
                        onTogglePin = {
                            messageWithOpenMenu = null
                            onTogglePinMessage(msg.id)
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
                            strings = strings,
                            isOptionsBubbleVisible = false,
                            onLongPressMessage = {},
                            onToggleHighlight = {},
                            onTogglePin = {},
                            onCopyOtp = onCopyOtp
                        )
                    }
                }
            }

            // Glassmorphic Input Bar (SMS & MMS Composer)
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(24.dp),
                elevated = true,
                accentHighlight = if (isMmsMode) palette.accentSecondary else null,
                contentPadding = PaddingValues(10.dp)
            ) {
                Column {
                    if (pendingAttachmentUri != null) {
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    MmsImageThumbnail(
                                        uri = pendingAttachmentUri,
                                        hdrLabel = strings.hdrMmsPhoto,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = strings.mmsAttachedTitle,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = palette.accentSecondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = strings.mmsZeroPermissionSub,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.textSecondary
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

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        GlassIconButton(
                            icon = TxturaIcons.AttachImage,
                            contentDescription = strings.attachMmsImage,
                            tint = palette.accentSecondary,
                            onClick = onPickMmsPhoto,
                            size = 40.dp
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Box {
                                if (draftText.isEmpty()) {
                                    Text(
                                        text = if (isMmsMode) strings.addMmsCaptionPlaceholder else strings.writeSmsPlaceholder,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = palette.textSecondary
                                    )
                                }
                                BasicTextField(
                                    value = draftText,
                                    onValueChange = { draftText = it },
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color = palette.textPrimary
                                    ),
                                    cursorBrush = SolidColor(palette.accentPrimary),
                                    maxLines = 4,
                                    modifier = Modifier.fillMaxWidth()
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

                        Spacer(modifier = Modifier.width(10.dp))

                        val canSend = draftText.isNotBlank() || pendingAttachmentUri != null

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 6-second countdown timer icon placed directly LEFT of the Send icon
                            AnimatedVisibility(
                                visible = pendingDelayedSend != null,
                                enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
                                exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End)
                            ) {
                                val activeDelayed = pendingDelayedSend
                                val totalMs = activeDelayed?.durationMillis ?: 6_000L
                                val secondsLeft = ceil(delayedRemainingMillis / 1000.0).toInt().coerceIn(1, 6)
                                val fraction = (delayedRemainingMillis.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)

                                SendCountdownTimerButton(
                                    secondsLeft = secondsLeft,
                                    progressFraction = fraction,
                                    contentDescription = strings.cancelDelayedSendTooltip,
                                    onCancel = {
                                        val cancelled = pendingDelayedSend
                                        pendingDelayedSend = null
                                        delayedRemainingMillis = 0L
                                        if (cancelled != null && cancelled.body.isNotEmpty()) {
                                            draftText = if (draftText.isEmpty()) {
                                                cancelled.body
                                            } else {
                                                "${cancelled.body} $draftText"
                                            }
                                        }
                                        Toast.makeText(
                                            context,
                                            strings.messageSendCancelledToast,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }

                            GlassIconButton(
                                icon = TxturaIcons.Send,
                                contentDescription = strings.send,
                                tint = if (canSend) palette.sendButtonColor else palette.textSecondary,
                                onClick = {
                                    if (canSend) {
                                        // Flush any already-counting message before handling the new message
                                        pendingDelayedSend?.let { prev ->
                                            onSendMessage(prev.body, prev.attachmentUri)
                                            pendingDelayedSend = null
                                            delayedRemainingMillis = 0L
                                        }
                                        val msgBodyToSend = draftText
                                        draftText = ""
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
                                    showScheduleMessageDialog = true
                                },
                                size = 42.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Circular 6-second countdown timer icon displayed directly to the left of the Send button.
 * Tapping this icon cancels the pending message and displays "Slanje poruke otkazano."
 */
@Composable
private fun SendCountdownTimerButton(
    secondsLeft: Int,
    progressFraction: Float,
    contentDescription: String,
    onCancel: () -> Unit,
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
        contentPadding = PaddingValues(0.dp),
        onClick = onCancel
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
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(12.dp)
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

                // Quick Preset Buttons (2x2 grid)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        accentHighlight = palette.accentPrimary,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        onClick = {
                            if (canSchedule) {
                                onConfirmSchedule(
                                    messageText,
                                    System.currentTimeMillis() + 15L * 60_000L
                                )
                            }
                        }
                    ) {
                        Text(
                            text = strings.schedulePresetIn15Min,
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    GlassCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        accentHighlight = palette.accentPrimary,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        onClick = {
                            if (canSchedule) {
                                onConfirmSchedule(
                                    messageText,
                                    System.currentTimeMillis() + 60L * 60_000L
                                )
                            }
                        }
                    ) {
                        Text(
                            text = strings.schedulePresetIn1Hour,
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        accentHighlight = palette.accentSecondary,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        onClick = {
                            if (canSchedule) {
                                val cal = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, 20)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                    if (timeInMillis <= System.currentTimeMillis()) {
                                        add(Calendar.DAY_OF_YEAR, 1)
                                    }
                                }
                                onConfirmSchedule(messageText, cal.timeInMillis)
                            }
                        }
                    ) {
                        Text(
                            text = strings.schedulePresetTonight20,
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    GlassCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        accentHighlight = palette.accentSecondary,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        onClick = {
                            if (canSchedule) {
                                val cal = Calendar.getInstance().apply {
                                    add(Calendar.DAY_OF_YEAR, 1)
                                    set(Calendar.HOUR_OF_DAY, 9)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                onConfirmSchedule(messageText, cal.timeInMillis)
                            }
                        }
                    ) {
                        Text(
                            text = strings.schedulePresetTomorrow09,
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.align(Alignment.Center)
                        )
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
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
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
private fun MessageActionBubbleChip(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    val palette = LocalGlassPalette.current
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevated = true,
        accentHighlight = tint,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = palette.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun MessageBubbleItem(
    message: ChatMessage,
    zoomScale: Float = 1.0f,
    strings: AppStrings,
    isOptionsBubbleVisible: Boolean = false,
    isJumpHighlighted: Boolean = false,
    onLongPressMessage: () -> Unit = {},
    onDismissOptionsBubble: () -> Unit = {},
    onForwardMessage: () -> Unit = {},
    onDeleteMessage: () -> Unit = {},
    onCopyMessage: () -> Unit = {},
    onToggleHighlight: () -> Unit = {},
    onTogglePin: () -> Unit,
    onCopyOtp: (String) -> Unit
) {
    val context = LocalContext.current
    val palette = LocalGlassPalette.current
    val isOutgoing = !message.isIncoming
    val analysis = message.securityAnalysis
    val s = zoomScale.coerceIn(0.75f, 1.65f)
    val cornerRadius = (18f * s).coerceIn(12f, 26f).dp
    val tailRadius = (4f * s).coerceIn(3f, 8f).dp
    val currentOnLongPress by rememberUpdatedState(onLongPressMessage)

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
        message.isHighlighted,
        isJumpHighlighted,
        palette.outgoingBubbleStart,
        palette.outgoingBubbleEnd,
        palette.incomingBubbleSurface,
        palette.cautionColor,
        palette.accentPrimary
    ) {
        if (isJumpHighlighted) {
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

    val selectionColors = remember(isOutgoing, palette.accentPrimary) {
        TextSelectionColors(
            handleColor = palette.accentPrimary,
            backgroundColor = if (isOutgoing) {
                Color.White.copy(alpha = 0.35f)
            } else {
                palette.accentPrimary.copy(alpha = 0.35f)
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = (300f * s).coerceIn(210f, 380f).dp)
                .clip(bubbleShape)
                .background(brush = bubbleBrush, shape = bubbleShape)
                .border(
                    width = if (isJumpHighlighted) 2.dp else if (message.isHighlighted) 1.5.dp else 1.dp,
                    color = borderColor,
                    shape = bubbleShape
                )
                // Observe single-finger long-press on Initial pass without consuming pointer events
                // so SelectionContainer inside the bubble can simultaneously select & highlight partial text!
                .pointerInput(message.id) {
                    awaitEachGesture {
                        val firstDown = awaitPointerEvent(PointerEventPass.Initial)
                            .changes
                            .firstOrNull { it.pressed } ?: return@awaitEachGesture
                        val downPos = firstDown.position
                        val downTime = System.currentTimeMillis()
                        val longPressTimeout = viewConfiguration.longPressTimeoutMillis
                        val touchSlop = viewConfiguration.touchSlop
                        while (true) {
                            val event = withTimeoutOrNull(50L) {
                                awaitPointerEvent(PointerEventPass.Initial)
                            }
                            if (event != null) {
                                val pressedChanges = event.changes.filter { it.pressed }
                                if (pressedChanges.size != 1) break
                                val currentPos = pressedChanges.first().position
                                if ((currentPos - downPos).getDistance() > touchSlop) break
                            }
                            if ((System.currentTimeMillis() - downTime) >= longPressTimeout) {
                                currentOnLongPress()
                                break
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

                if (message.attachment != null) {
                    MmsAttachmentCard(
                        uri = message.attachment.uri,
                        caption = message.attachment.caption,
                        hdrLabel = strings.hdrMmsPhoto
                    )
                    Spacer(modifier = Modifier.height((6f * s).dp))
                }

                CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
                    SelectionContainer {
                        Text(
                            text = annotatedMessageText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = (14f * s).sp,
                                lineHeight = (19f * s).sp
                            ),
                            color = if (isOutgoing) Color.White else palette.textPrimary
                        )
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
                    // Highlight icon right next to the Pin icon on the message bubble
                    Icon(
                        imageVector = TxturaIcons.HighlightStar,
                        contentDescription = if (message.isHighlighted) strings.unhighlightMessageOption else strings.highlightMessageOption,
                        tint = if (message.isHighlighted) {
                            palette.cautionColor
                        } else if (isOutgoing) {
                            Color.White.copy(alpha = 0.65f)
                        } else {
                            palette.textSecondary
                        },
                        modifier = Modifier
                            .size((14f * s).dp)
                            .clickable(onClick = onToggleHighlight)
                    )
                    Spacer(modifier = Modifier.width((6f * s).dp))
                    Icon(
                        imageVector = TxturaIcons.Pin,
                        contentDescription = if (message.isPinned) strings.unpinMessage else strings.pinMessage,
                        tint = if (message.isPinned) {
                            palette.accentPrimary
                        } else if (isOutgoing) {
                            Color.White.copy(alpha = 0.65f)
                        } else {
                            palette.textSecondary
                        },
                        modifier = Modifier
                            .size((14f * s).dp)
                            .clickable(onClick = onTogglePin)
                    )
                    Spacer(modifier = Modifier.width((6f * s).dp))
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
                        text = formatTimestamp(message.timestampMillis),
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

        // Inline Options Bubble directly below the message bubble when long-pressed (instead of a popup dialog)
        AnimatedVisibility(
            visible = isOptionsBubbleVisible,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            Column {
                Spacer(modifier = Modifier.height(6.dp))
                GlassCard(
                    modifier = Modifier.widthIn(max = (280f * s).coerceIn(200f, 340f).dp),
                    shape = RoundedCornerShape(
                        topStart = if (isOutgoing) 18.dp else 6.dp,
                        topEnd = if (isOutgoing) 6.dp else 18.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 18.dp
                    ),
                    elevated = true,
                    accentHighlight = palette.accentPrimary,
                    contentPadding = PaddingValues(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = strings.messageOptionsTitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.accentPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = TxturaIcons.Close,
                                contentDescription = strings.cancel,
                                tint = palette.textSecondary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable(onClick = onDismissOptionsBubble)
                            )
                        }

                        // 1. Prosledi poruku (Forward)
                        MessageActionBubbleChip(
                            icon = TxturaIcons.Forward,
                            label = strings.forwardMessageOption,
                            tint = palette.accentPrimary,
                            onClick = onForwardMessage
                        )
                        // 2. Izbriši poruku (Delete)
                        MessageActionBubbleChip(
                            icon = TxturaIcons.Delete,
                            label = strings.deleteMessageOption,
                            tint = palette.dangerColor,
                            onClick = onDeleteMessage
                        )
                        // 3. Kopiraj poruku (Copy)
                        MessageActionBubbleChip(
                            icon = TxturaIcons.CopyKey,
                            label = strings.copyMessageOption,
                            tint = palette.safeColor,
                            onClick = onCopyMessage
                        )
                        // 4. Hajlajtuj poruku (Highlight)
                        MessageActionBubbleChip(
                            icon = TxturaIcons.HighlightStar,
                            label = if (message.isHighlighted) strings.unhighlightMessageOption else strings.highlightMessageOption,
                            tint = palette.cautionColor,
                            onClick = onToggleHighlight
                        )
                        // 5. Prikači poruku (Pin)
                        MessageActionBubbleChip(
                            icon = TxturaIcons.Pin,
                            label = if (message.isPinned) strings.unpinMessage else strings.pinMessage,
                            tint = palette.accentSecondary,
                            onClick = onTogglePin
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MmsAttachmentCard(
    uri: Uri,
    caption: String,
    hdrLabel: String
) {
    val palette = LocalGlassPalette.current
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
            .padding(10.dp)
    ) {
        MmsImageThumbnail(
            uri = uri,
            hdrLabel = hdrLabel,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(10.dp))
        )
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

@Composable
private fun MmsImageThumbnail(
    uri: Uri,
    hdrLabel: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
    onPickContact: () -> Unit,
    onDismiss: () -> Unit,
    onSelectRecipient: (recipientAddress: String, displayName: String?) -> Unit
) {
    val palette = LocalGlassPalette.current
    val strings = TxturaStrings.current()
    var queryOrNumber by remember(initialRecipient) { mutableStateOf(initialRecipient) }

    val filteredContacts = remember(availableContacts, queryOrNumber) {
        if (queryOrNumber.isBlank()) {
            availableContacts
        } else {
            availableContacts.filter {
                it.name.contains(queryOrNumber, ignoreCase = true) ||
                    it.address.contains(queryOrNumber, ignoreCase = true)
            }
        }
    }

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        accentBorderColor = palette.accentPrimary,
        title = {
            Column {
                Text(
                    text = title ?: strings.newSmsMmsDialogTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = strings.selectSenderSubtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Search / Enter phone number bar + System Contacts Picker button
                GlassCard(
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.recipientLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.accentPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box {
                                if (queryOrNumber.isEmpty()) {
                                    Text(
                                        text = strings.searchOrEnterNumberPlaceholder,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.textSecondary
                                    )
                                }
                                BasicTextField(
                                    value = queryOrNumber,
                                    onValueChange = { queryOrNumber = it },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
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
                            contentDescription = strings.pickFromContacts,
                            tint = palette.accentPrimary,
                            onClick = onPickContact,
                            size = 38.dp
                        )
                    }
                }

                // Direct button to open/create conversation with typed phone number
                val trimmedInput = queryOrNumber.trim()
                if (trimmedInput.isNotBlank() && trimmedInput.any { it.isDigit() }) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        elevated = true,
                        accentHighlight = palette.accentPrimary,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        onClick = { onSelectRecipient(trimmedInput, null) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = strings.openChatWithNumberButton(trimmedInput),
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.accentPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "→",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.accentPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Contact list: tapping any contact immediately opens their existing chat (or creates a new one)
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                ) {
                    items(
                        items = filteredContacts,
                        key = { "${it.name}_${it.address}" }
                    ) { item ->
                        val hasExisting = item.existingThreadId != null
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            elevated = hasExisting,
                            accentHighlight = if (hasExisting) palette.accentPrimary else null,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            onClick = {
                                onSelectRecipient(item.address, item.name)
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
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(palette.accentPrimary.copy(alpha = 0.2f))
                                            .border(1.dp, palette.accentPrimary.copy(alpha = 0.5f), CircleShape)
                                    ) {
                                        Text(
                                            text = item.initials,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = palette.textPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
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
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (hasExisting) strings.existingChatBadge else strings.newChatBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (hasExisting) palette.accentPrimary else palette.safeColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (queryOrNumber.isNotBlank()) {
                TextButton(
                    onClick = { onSelectRecipient(queryOrNumber.trim(), null) }
                ) {
                    Text(strings.confirm, color = palette.accentPrimary, fontWeight = FontWeight.Bold)
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
            GlassCard(
                shape = RoundedCornerShape(16.dp),
                accentHighlight = palette.accentPrimary,
                contentPadding = PaddingValues(14.dp)
            ) {
                Text(
                    text = strings.defaultSmsPromptMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.textPrimary
                )
            }
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
private fun SecurityItemRow(title: String, subtitle: String) {
    val palette = LocalGlassPalette.current
    var expanded by remember { mutableStateOf(value = false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { expanded = !expanded }
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        Text(
            text = "✓ $title",
            style = MaterialTheme.typography.labelMedium,
            color = palette.safeColor,
            fontWeight = FontWeight.Bold
        )
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textPrimary.copy(alpha = 0.88f),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
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
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }

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
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val proposed = offsetX.value + dragAmount
                        val clamped = when {
                            proposed > 0f && swipeRightAction == SwipeAction.NONE -> proposed * 0.15f
                            proposed < 0f && swipeLeftAction == SwipeAction.NONE -> proposed * 0.15f
                            else -> proposed.coerceIn(-maxDragPx, maxDragPx)
                        }
                        scope.launch {
                            offsetX.snapTo(clamped)
                        }
                    },
                    onDragEnd = {
                        val finalOffset = offsetX.value
                        when {
                            finalOffset >= thresholdPx && swipeRightAction != SwipeAction.NONE -> {
                                onExecuteSwipeAction(swipeRightAction)
                            }
                            finalOffset <= -thresholdPx && swipeLeftAction != SwipeAction.NONE -> {
                                onExecuteSwipeAction(swipeLeftAction)
                            }
                        }
                        scope.launch {
                            offsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            offsetX.animateTo(0f)
                        }
                    }
                )
            }
    ) {
        if (abs(currentOffset) > 10f && activeAction != SwipeAction.NONE) {
            val actionColor = swipeActionColor(activeAction)
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
                        imageVector = swipeActionIcon(activeAction),
                        contentDescription = null,
                        tint = actionColor,
                        modifier = Modifier.size(if (isTriggered) 22.dp else 18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val activeActionText = if (activeAction == SwipeAction.PIN) {
                        if (thread.isPinned) strings.swipeGestureUnpin else strings.swipeGesturePin
                    } else {
                        swipeActionLabel(activeAction, strings)
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

private fun swipeActionIcon(action: SwipeAction): ImageVector {
    return when (action) {
        SwipeAction.PIN -> TxturaIcons.Pin
        SwipeAction.DELETE -> TxturaIcons.Delete
        SwipeAction.CALL -> TxturaIcons.PhoneCall
        SwipeAction.MARK_READ -> TxturaIcons.CheckDouble
        SwipeAction.NONE -> TxturaIcons.Close
    }
}

@Composable
private fun swipeActionColor(action: SwipeAction): Color {
    val palette = LocalGlassPalette.current
    return when (action) {
        SwipeAction.PIN -> palette.accentPrimary
        SwipeAction.DELETE -> palette.dangerColor
        SwipeAction.CALL -> palette.safeColor
        SwipeAction.MARK_READ -> palette.accentSecondary
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
            GlassCard(
                shape = RoundedCornerShape(16.dp),
                accentHighlight = palette.cautionColor,
                contentPadding = PaddingValues(14.dp)
            ) {
                Text(
                    text = strings.confirmDeleteMessage(contactName, retentionDays),
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.textPrimary
                )
            }
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
            GlassCard(
                shape = RoundedCornerShape(16.dp),
                accentHighlight = palette.accentPrimary,
                contentPadding = PaddingValues(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = strings.confirmRestoreMessage(
                            dateStr,
                            payload.threads.size,
                            payload.totalMessagesCount
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "• ${payload.deviceModel} • v${payload.version}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }
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
fun DeletedMessagesScreen(
    deletedEntries: List<DeletedConversationEntry>,
    retentionDays: Int,
    groupingMode: DeletedGroupingMode,
    showContactPhotos: Boolean,
    mainWallpaper: WallpaperConfig = WallpaperConfig(),
    onBack: () -> Unit,
    onSelectGroupingMode: (DeletedGroupingMode) -> Unit,
    onRestoreThread: (Long) -> Unit,
    onPermanentlyDeleteThread: (Long) -> Unit,
    onEmptyTrash: () -> Unit
) {
    val strings = TxturaStrings.current()
    var pendingPermanentDeleteEntry by remember { mutableStateOf<DeletedConversationEntry?>(null) }

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

    GlassBackground(wallpaperConfig = mainWallpaper) {
        val palette = LocalGlassPalette.current
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
                                softWrap = false
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
                                color = palette.textSecondary
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
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
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        GlassCard(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(40.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            elevated = true,
                                            accentHighlight = palette.safeColor,
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
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
                                                    Spacer(modifier = Modifier.width(6.dp))
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
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
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
                                                    Spacer(modifier = Modifier.width(6.dp))
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
    }
}

private val shortTimeFormatter = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
}

private val shortDateFormatter = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("dd.MM.", Locale.getDefault())
}

private val fullDateAndTimeFormatter = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("dd.MM.yyyy. HH:mm", Locale.getDefault())
}

private fun formatTimestamp(millis: Long): String {
    return fullDateAndTimeFormatter.get()?.format(Date(millis)).orEmpty()
}

private fun formatThreadTimestamp(millis: Long): Pair<String, String?> {
    val now = System.currentTimeMillis()
    val diff = now - millis
    val timeStr = shortTimeFormatter.get()?.format(Date(millis)).orEmpty()
    return if (diff < 86_400_000L) {
        Pair(timeStr, null)
    } else {
        val dateStr = shortDateFormatter.get()?.format(Date(millis)).orEmpty()
        Pair(timeStr, dateStr)
    }
}

/**
 * Displays an expanded, easy-to-browse Backgrounds & Themes selector with a 2-column visual
 * grid for Themes (Colorful, Monochrome, Minimalist) and a 2-column vertical grid for all
 * Txtura background textures (no horizontal swiping required).
 */
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.8.dp)
                .background(palette.glassBorderTop.copy(alpha = 0.28f))
        )

        // 3. Expanded 2-Column Txtura Texture Grid (no left/right swiping needed)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        palette.isAmoled -> Color(0xFF0E131D)
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
fun WallpaperPickerDialog(
    title: String,
    currentConfig: WallpaperConfig,
    showResetToMainButton: Boolean = false,
    onSelectPreset: (WallpaperPreset) -> Unit,
    onSelectTexture: (WallpaperTexture) -> Unit = {},
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
        WallpaperPreset.ARCTIC_FROST -> strings.wpArcticFrost
        WallpaperPreset.OBSIDIAN_GOLD -> strings.wpObsidianGold
        WallpaperPreset.MONO_SLATE -> strings.wpMonoSlate
        WallpaperPreset.MONO_SAPPHIRE -> strings.wpMonoSapphire
        WallpaperPreset.MONO_SAGE -> strings.wpMonoSage
        WallpaperPreset.MONO_CHAMPAGNE -> strings.wpMonoChampagne
        WallpaperPreset.MONO_PLUM -> strings.wpMonoPlum
        WallpaperPreset.MONO_GRAPHITE -> strings.wpMonoGraphite
        WallpaperPreset.MONO_ROSE -> strings.wpMonoRose
        WallpaperPreset.MONO_NAVY -> strings.wpMonoNavy
        WallpaperPreset.MONO_SMOKE -> strings.wpMonoSmoke
        WallpaperPreset.MONO_ESPRESSO -> strings.wpMonoEspresso
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
        WallpaperPreset.ARCTIC_FROST -> strings.wpArcticFrostDesc
        WallpaperPreset.OBSIDIAN_GOLD -> strings.wpObsidianGoldDesc
        WallpaperPreset.MONO_SLATE -> strings.wpMonoSlateDesc
        WallpaperPreset.MONO_SAPPHIRE -> strings.wpMonoSapphireDesc
        WallpaperPreset.MONO_SAGE -> strings.wpMonoSageDesc
        WallpaperPreset.MONO_CHAMPAGNE -> strings.wpMonoChampagneDesc
        WallpaperPreset.MONO_PLUM -> strings.wpMonoPlumDesc
        WallpaperPreset.MONO_GRAPHITE -> strings.wpMonoGraphiteDesc
        WallpaperPreset.MONO_ROSE -> strings.wpMonoRoseDesc
        WallpaperPreset.MONO_NAVY -> strings.wpMonoNavyDesc
        WallpaperPreset.MONO_SMOKE -> strings.wpMonoSmokeDesc
        WallpaperPreset.MONO_ESPRESSO -> strings.wpMonoEspressoDesc
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
        WallpaperTexture.NONE -> strings.texNone
    }
}
