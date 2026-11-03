package com.txtura.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.txtura.data.BackupRestorePayload
import com.txtura.data.ChatMessage
import com.txtura.data.ConversationFilter
import com.txtura.data.ConversationThread
import com.txtura.data.DeletedConversationEntry
import com.txtura.data.DeletedGroupingMode
import com.txtura.data.MainListLayoutMode
import com.txtura.data.SwipeAction
import com.txtura.data.UiColorMode
import com.txtura.data.LauncherIconShape
import com.txtura.data.WallpaperConfig
import com.txtura.data.WallpaperPreset
import com.txtura.security.SecurityRiskLevel
import com.txtura.ui.components.GlassBackground
import com.txtura.ui.components.GlassCard
import com.txtura.ui.components.GlassIconButton
import com.txtura.ui.components.GlassTooltipIconButton
import com.txtura.ui.components.TxturaIcons
import com.txtura.ui.i18n.AppStrings
import com.txtura.ui.i18n.TxturaStrings
import com.txtura.ui.theme.LocalGlassPalette
import com.txtura.ui.theme.TxturaThemeMode
import com.txtura.ui.theme.WallpaperHarmonyEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ConversationsListScreen(
    threads: List<ConversationThread>,
    deletedCount: Int = 0,
    showContactPhotos: Boolean = true,
    listLayoutMode: MainListLayoutMode = MainListLayoutMode.DEFAULT,
    mainWallpaper: WallpaperConfig = WallpaperConfig(),
    swipeRightAction: SwipeAction = SwipeAction.PIN,
    swipeLeftAction: SwipeAction = SwipeAction.DELETE,
    onOpenThread: (Long) -> Unit,
    onTogglePin: (Long) -> Unit,
    onRequestDeleteThread: (ConversationThread) -> Unit,
    onMarkThreadRead: (Long) -> Unit = {},
    onCallSender: (String) -> Unit,
    onOpenContact: (String, String) -> Unit,
    onCopyOtp: (String) -> Unit,
    onOpenNewMessage: () -> Unit,
    onOpenDeletedMessages: () -> Unit = {},
    onOpenSettings: () -> Unit
) {
    val strings = TxturaStrings.current()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ConversationFilter.ALL) }

    val filteredThreads = remember(threads, searchQuery, selectedFilter) {
        threads.filter { thread ->
            val matchesSearch = searchQuery.isBlank() ||
                thread.contactName.contains(searchQuery, ignoreCase = true) ||
                thread.address.contains(searchQuery, ignoreCase = true) ||
                thread.messages.any { it.body.contains(searchQuery, ignoreCase = true) }

            val matchesFilter = when (selectedFilter) {
                ConversationFilter.ALL -> true
                ConversationFilter.PINNED -> thread.isPinned || thread.pinnedMessages.isNotEmpty()
                ConversationFilter.UNREAD -> thread.unreadCount > 0
                ConversationFilter.SERVICES -> isServiceThread(thread)
            }
            matchesSearch && matchesFilter
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
            Spacer(modifier = Modifier.height(12.dp))

            // Top Header: Clean "Txtura" branding on left; Plus (New Message), Trash & Settings Gear on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Txtura",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.textPrimary
                )

                // Top-right actions: New Message (+), Deleted Messages (Trash) & Settings Gear with matching accent color
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                                    color = if (palette.isAmoled) Color.Black else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    GlassTooltipIconButton(
                        icon = TxturaIcons.SettingsGear,
                        tooltipText = strings.settingsTooltip,
                        tint = palette.accentPrimary,
                        onClick = onOpenSettings
                    )
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

            // Filter Chips (4 static items - Row avoids LazyRow virtualization overhead)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ConversationFilter.entries.forEach { filter ->
                    val selected = filter == selectedFilter
                    val label = filterLabel(filter, strings)
                    GlassCard(
                        shape = RoundedCornerShape(50),
                        elevated = selected,
                        accentHighlight = if (selected) palette.accentPrimary else null,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                        onClick = { selectedFilter = filter }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) palette.accentPrimary else palette.textSecondary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val density = LocalDensity.current
            val swipeThresholdPx = remember(density) { with(density) { 105.dp.toPx() } }
            val swipeMaxDragPx = remember(density) { with(density) { 165.dp.toPx() } }

            // Conversations List (unobstructed at the bottom since New Message '+' is in the top bar)
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
                        SwipeableConversationCard(
                            thread = thread,
                            strings = strings,
                            swipeRightAction = swipeRightAction,
                            swipeLeftAction = swipeLeftAction,
                            thresholdPx = swipeThresholdPx,
                            maxDragPx = swipeMaxDragPx,
                            onExecuteSwipeAction = { action ->
                                when (action) {
                                    SwipeAction.PIN -> onTogglePin(thread.id)
                                    SwipeAction.DELETE -> onRequestDeleteThread(thread)
                                    SwipeAction.CALL -> onCallSender(thread.address)
                                    SwipeAction.MARK_READ -> onMarkThreadRead(thread.id)
                                    SwipeAction.OPEN_CONTACT -> onOpenContact(thread.address, thread.contactName)
                                    SwipeAction.NONE -> Unit
                                }
                            }
                        ) {
                            ConversationThreadCard(
                                thread = thread,
                                showContactPhotos = showContactPhotos,
                                listLayoutMode = listLayoutMode,
                                strings = strings,
                                onClick = { onOpenThread(thread.id) },
                                onTogglePin = { onTogglePin(thread.id) },
                                onDelete = { onRequestDeleteThread(thread) },
                                onCallSender = { onCallSender(thread.address) },
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

private fun isServiceThread(thread: ConversationThread): Boolean {
    val address = thread.address.trim()
    val cleanDigits = address.filter { it.isDigit() }

    // 1. Alphanumeric sender ID (e.g. "BANK", "INFO", "YETTEL", "GOOGLE", "MTS", "A1")
    if (address.any { it.isLetter() }) return true

    // 2. Shortcode (typically 3 to 6 digits, e.g. 1999, 9800, 2222)
    if (cleanDigits.length in 3..6 && !address.startsWith("+") && cleanDigits.length == address.length) {
        return true
    }

    // 3. Extracted OTP / 2FA verification codes
    if (thread.messages.any { it.securityAnalysis.extractedOtpCode != null }) {
        return true
    }

    // 4. Banking / payment / delivery / service notifications keywords
    val serviceKeywords = listOf(
        "otp", "kod", "verifikac", "transakc", "racun", "račun", "uplata", "isplata",
        "saldo", "stanje", "bank", "kartic", "pin", "kod za", "kod je",
        "code", "verify", "verification", "transaction", "balance", "alert",
        "security code", "passcode", "auth", "confirm", "narudzb", "dostav",
        "posiljka", "pošiljka", "paket", "servis", "obavestenje", "obaveštenje"
    )
    val latestBody = thread.lastMessage?.body?.lowercase() ?: ""
    return serviceKeywords.any { latestBody.contains(it) }
}

private fun filterLabel(filter: ConversationFilter, strings: AppStrings): String {
    return when (filter) {
        ConversationFilter.ALL -> strings.filterAll
        ConversationFilter.PINNED -> strings.filterPinned
        ConversationFilter.UNREAD -> strings.filterUnread
        ConversationFilter.SERVICES -> strings.filterServices
    }
}

/**
 * Dedicated Settings Screen ("Settings" / "Podešavanja") organized into expandable accordion sections
 * (no ordinal numbers; clicking any section header expands or collapses its options):
 * - Izgled i Prikaz (Glassmorphism Theme + Show Contact Photos)
 * - Pozadine Glavnog Prozora i Četova (10 built-in wallpapers + Gallery photo picker)
 * - Svajp Radnje u Glavnom Meniju (Swipe Right & Swipe Left Actions)
 * - Obrisane Poruke i Automatsko Brisanje (Trash folder access, Retention Days, Grouping Mode)
 * - Dozvole Aplikacije i Podrazumevani SMS
 * - Aktivne Bezbednosne Mere i Protokoli
 */
@Composable
fun SettingsScreen(
    themeMode: TxturaThemeMode,
    showContactPhotos: Boolean,
    listLayoutMode: MainListLayoutMode = MainListLayoutMode.DEFAULT,
    mainWallpaper: WallpaperConfig = WallpaperConfig(),
    uiColorMode: UiColorMode = UiColorMode.COLORFUL,
    launcherIconShape: LauncherIconShape = LauncherIconShape.ROUNDED_SQUARE,
    swipeRightAction: SwipeAction,
    swipeLeftAction: SwipeAction,
    autoDeleteDays: Int,
    deletedGroupingMode: DeletedGroupingMode,
    deletedMessagesCount: Int,
    lastBackupTimestamp: Long = 0L,
    connectedGoogleAccount: String? = null,
    isGoogleDriveInstalled: Boolean = false,
    hasSmsPermission: Boolean,
    isDefaultSmsApp: Boolean,
    rationaleMessage: String?,
    onBack: () -> Unit,
    onSelectTheme: (TxturaThemeMode) -> Unit,
    onToggleShowContactPhotos: (Boolean) -> Unit,
    onSelectListLayoutMode: (MainListLayoutMode) -> Unit = {},
    onSelectUiColorMode: (UiColorMode) -> Unit = {},
    onSelectLauncherIconShape: (LauncherIconShape) -> Unit = {},
    onSelectMainWallpaperPreset: (WallpaperPreset) -> Unit = {},
    onPickMainWallpaperFromGallery: () -> Unit = {},
    onSelectSwipeRightAction: (SwipeAction) -> Unit,
    onSelectSwipeLeftAction: (SwipeAction) -> Unit,
    onSelectAutoDeleteDays: (Int) -> Unit,
    onSelectDeletedGroupingMode: (DeletedGroupingMode) -> Unit,
    onOpenDeletedMessages: () -> Unit,
    onCreateLocalBackup: () -> Unit = {},
    onRestoreLocalBackup: () -> Unit = {},
    onShareBackupFile: () -> Unit = {},
    onSaveToGoogleDrive: () -> Unit = {},
    onRestoreFromGoogleDrive: () -> Unit = {},
    onOpenSystemBackupSettings: () -> Unit = {},
    onRequestSmsPermissions: () -> Unit,
    onRequestDefaultSmsRole: () -> Unit,
    onOpenSystemAppSettings: () -> Unit
) {
    val strings = TxturaStrings.current()
    val scrollState = rememberScrollState()
    val retentionOptions = listOf(7, 15, 30, 60, 90)

    var expandedAppearance by remember { mutableStateOf(false) }
    var expandedWallpaper by remember { mutableStateOf(false) }
    var expandedSwipe by remember { mutableStateOf(false) }
    var expandedTrash by remember { mutableStateOf(false) }
    var expandedBackup by remember { mutableStateOf(false) }
    var expandedPermissions by remember { mutableStateOf(false) }
    var showSecurityMeasuresDialog by remember { mutableStateOf(false) }

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

            // Settings Top Bar with Security Shield Icon in Top-Right Corner
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
                            Text(
                                text = strings.settingsSubtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Security Shield Icon in Top-Right of Settings Menu
                    GlassTooltipIconButton(
                        icon = TxturaIcons.ShieldCheck,
                        tooltipText = strings.securitySectionTitle,
                        tint = palette.safeColor,
                        onClick = { showSecurityMeasuresDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(bottom = 24.dp)
            ) {
                // SECTION: Appearance & Display (Theme + Main Window Layout Mode + Contact Photos)
                ExpandableSettingsSection(
                    icon = TxturaIcons.SparklesTheme,
                    iconTint = palette.accentPrimary,
                    title = strings.appearanceSectionTitle,
                    subtitle = strings.appearanceSectionDesc,
                    accentHighlight = if (expandedAppearance) palette.accentPrimary else null,
                    expanded = expandedAppearance,
                    onToggleExpand = { expandedAppearance = !expandedAppearance }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeOptionRow(
                            mode = TxturaThemeMode.AMOLED,
                            title = strings.themeAmoledTitle,
                            subtitle = strings.themeAmoledSub,
                            activePrefix = strings.activePrefix,
                            selected = themeMode == TxturaThemeMode.AMOLED,
                            onClick = { onSelectTheme(TxturaThemeMode.AMOLED) }
                        )
                        ThemeOptionRow(
                            mode = TxturaThemeMode.LIGHT,
                            title = strings.themeLightTitle,
                            subtitle = strings.themeLightSub,
                            activePrefix = strings.activePrefix,
                            selected = themeMode == TxturaThemeMode.LIGHT,
                            onClick = { onSelectTheme(TxturaThemeMode.LIGHT) }
                        )
                        ThemeOptionRow(
                            mode = TxturaThemeMode.DARK,
                            title = strings.themeDarkTitle,
                            subtitle = strings.themeDarkSub,
                            activePrefix = strings.activePrefix,
                            selected = themeMode == TxturaThemeMode.DARK,
                            onClick = { onSelectTheme(TxturaThemeMode.DARK) }
                        )
                        ThemeOptionRow(
                            mode = TxturaThemeMode.SYSTEM,
                            title = strings.themeAutoTitle,
                            subtitle = strings.themeAutoSub,
                            activePrefix = strings.activePrefix,
                            selected = themeMode == TxturaThemeMode.SYSTEM,
                            onClick = { onSelectTheme(TxturaThemeMode.SYSTEM) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // UI Palette Mode (Višebojni UI vs Jednobojni UI)
                    Text(
                        text = strings.uiColorModeSectionTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = strings.uiColorModeSectionDesc,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        UiColorMode.entries.forEach { mode ->
                            val selected = mode == uiColorMode
                            val label = when (mode) {
                                UiColorMode.COLORFUL -> strings.uiColorModeColorfulTitle
                                UiColorMode.MONOCHROME -> strings.uiColorModeMonochromeTitle
                            }
                            GlassCard(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                elevated = selected,
                                accentHighlight = if (selected) palette.accentPrimary else null,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                onClick = { onSelectUiColorMode(mode) }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) palette.accentPrimary else palette.textSecondary,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Launcher Icon Style Selector
                    Text(
                        text = strings.launcherIconShapeSectionTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = strings.launcherIconShapeSectionDesc,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LauncherIconShape.entries.forEach { shape ->
                            val selected = shape == launcherIconShape
                            val shapeObj = when (shape) {
                                LauncherIconShape.CIRCLE -> CircleShape
                                LauncherIconShape.SQUIRCLE -> RoundedCornerShape(percent = 38)
                                LauncherIconShape.ROUNDED_SQUARE -> RoundedCornerShape(percent = 22)
                                LauncherIconShape.TEARDROP -> RoundedCornerShape(topStartPercent = 50, topEndPercent = 50, bottomEndPercent = 50, bottomStartPercent = 10)
                                LauncherIconShape.PEBBLE -> RoundedCornerShape(topStartPercent = 45, topEndPercent = 18, bottomEndPercent = 45, bottomStartPercent = 18)
                            }
                            val shapeLabel = when (shape) {
                                LauncherIconShape.CIRCLE -> strings.launcherIconShapeCircle
                                LauncherIconShape.SQUIRCLE -> strings.launcherIconShapeSquircle
                                LauncherIconShape.ROUNDED_SQUARE -> strings.launcherIconShapeRoundedSquare
                                LauncherIconShape.TEARDROP -> strings.launcherIconShapeTeardrop
                                LauncherIconShape.PEBBLE -> strings.launcherIconShapePebble
                            }
                            GlassCard(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                elevated = selected,
                                accentHighlight = if (selected) palette.accentPrimary else null,
                                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
                                onClick = { onSelectLauncherIconShape(shape) }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(shapeObj)
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(
                                                        Color(0xFF0F172A),
                                                        if (selected) palette.accentPrimary else Color(0xFF334155)
                                                    )
                                                )
                                            )
                                            .border(
                                                1.dp,
                                                if (selected) palette.accentPrimary else Color.White.copy(alpha = 0.25f),
                                                shapeObj
                                            )
                                    ) {
                                        Text(
                                            text = "T",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) palette.accentPrimary else Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = shapeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = if (selected) palette.accentPrimary else palette.textSecondary,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Main Window Display Mode Selector (Default vs Compact)
                    Text(
                        text = strings.mainLayoutModeTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = strings.mainLayoutModeSub,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MainListLayoutMode.entries.forEach { mode ->
                            val selected = mode == listLayoutMode
                            val label = when (mode) {
                                MainListLayoutMode.DEFAULT -> strings.layoutModeDefault
                                MainListLayoutMode.COMPACT -> strings.layoutModeCompact
                            }
                            GlassCard(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                elevated = selected,
                                accentHighlight = if (selected) palette.accentPrimary else null,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                onClick = { onSelectListLayoutMode(mode) }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) palette.accentPrimary else palette.textSecondary,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Contact Photos Display Toggle
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevated = showContactPhotos,
                        accentHighlight = if (showContactPhotos) palette.accentPrimary else null,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        onClick = { onToggleShowContactPhotos(!showContactPhotos) }
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
                                    imageVector = TxturaIcons.ContactPerson,
                                    contentDescription = null,
                                    tint = if (showContactPhotos) palette.accentPrimary else palette.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = strings.showContactPhotosTitle,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = strings.showContactPhotosSub,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Switch(
                                checked = showContactPhotos,
                                onCheckedChange = onToggleShowContactPhotos,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = if (palette.isAmoled) Color.Black else Color.White,
                                    checkedTrackColor = palette.accentPrimary,
                                    uncheckedThumbColor = palette.textSecondary,
                                    uncheckedTrackColor = palette.glassSurfaceElevated
                                )
                            )
                        }
                    }
                }

                // SECTION: Main Window & Chat Wallpapers (10 Curated Presets + Gallery Photo Picker)
                ExpandableSettingsSection(
                    icon = TxturaIcons.AttachImage,
                    iconTint = palette.accentPrimary,
                    title = strings.wallpaperSectionTitle,
                    subtitle = strings.wallpaperSectionDesc,
                    accentHighlight = if (expandedWallpaper) palette.accentPrimary else null,
                    expanded = expandedWallpaper,
                    onToggleExpand = { expandedWallpaper = !expandedWallpaper }
                ) {
                    Text(
                        text = strings.mainWallpaperButton,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.accentPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Pick Personal Photo from Gallery Button
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevated = mainWallpaper.isCustomGalleryImage,
                        accentHighlight = if (mainWallpaper.isCustomGalleryImage) palette.accentPrimary else palette.accentSecondary,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        onClick = onPickMainWallpaperFromGallery
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
                                    imageVector = TxturaIcons.AttachImage,
                                    contentDescription = null,
                                    tint = palette.accentPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = strings.pickWallpaperFromGallery,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (mainWallpaper.isCustomGalleryImage) {
                                        Text(
                                            text = "✓ ${strings.customGalleryWallpaperActive}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.safeColor,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "+",
                                style = MaterialTheme.typography.titleLarge,
                                color = palette.accentPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    WallpaperPresetSelectorGrid(
                        currentConfig = mainWallpaper,
                        strings = strings,
                        onSelectPreset = onSelectMainWallpaperPreset
                    )
                }

                // SECTION: Swipe Gestures in Main Menu (Swipe Left & Swipe Right)
                ExpandableSettingsSection(
                    icon = TxturaIcons.SwipeHoriz,
                    iconTint = palette.accentPrimary,
                    title = strings.swipeSectionTitle,
                    subtitle = strings.swipeSectionDesc,
                    accentHighlight = if (expandedSwipe) palette.accentSecondary else null,
                    expanded = expandedSwipe,
                    onToggleExpand = { expandedSwipe = !expandedSwipe }
                ) {
                    Text(
                        text = strings.swipeRightLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.accentPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SwipeAction.entries.forEach { action ->
                            val selected = action == swipeRightAction
                            GlassCard(
                                shape = RoundedCornerShape(50),
                                elevated = selected,
                                accentHighlight = if (selected) swipeActionColor(action) else null,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                                onClick = { onSelectSwipeRightAction(action) }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = swipeActionIcon(action),
                                        contentDescription = null,
                                        tint = if (selected) swipeActionColor(action) else palette.textSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = swipeActionLabel(action, strings),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) swipeActionColor(action) else palette.textSecondary,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = strings.swipeLeftLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.accentSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SwipeAction.entries.forEach { action ->
                            val selected = action == swipeLeftAction
                            GlassCard(
                                shape = RoundedCornerShape(50),
                                elevated = selected,
                                accentHighlight = if (selected) swipeActionColor(action) else null,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                                onClick = { onSelectSwipeLeftAction(action) }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = swipeActionIcon(action),
                                        contentDescription = null,
                                        tint = if (selected) swipeActionColor(action) else palette.textSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = swipeActionLabel(action, strings),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) swipeActionColor(action) else palette.textSecondary,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // SECTION: Deleted Messages (Trash), Auto-Delete Days & Grouping Mode
                ExpandableSettingsSection(
                    icon = TxturaIcons.Delete,
                    iconTint = palette.accentPrimary,
                    title = strings.trashSectionTitle,
                    subtitle = strings.trashSectionDesc,
                    accentHighlight = if (expandedTrash) palette.cautionColor else null,
                    expanded = expandedTrash,
                    onToggleExpand = { expandedTrash = !expandedTrash }
                ) {
                    // Open Deleted Messages Folder Button
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
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

                    Spacer(modifier = Modifier.height(14.dp))

                    // Auto-delete retention days selector (7, 15, 30, 60, 90 days)
                    Text(
                        text = strings.autoDeleteDaysTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = strings.autoDeleteDaysSub,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        retentionOptions.forEach { days ->
                            val selected = days == autoDeleteDays
                            GlassCard(
                                shape = RoundedCornerShape(50),
                                elevated = selected,
                                accentHighlight = if (selected) palette.accentPrimary else null,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                                onClick = { onSelectAutoDeleteDays(days) }
                            ) {
                                Text(
                                    text = strings.daysCountLabel(days),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) palette.accentPrimary else palette.textSecondary,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Grouping & Sorting mode for Deleted Messages (By Date vs By Contact)
                    Text(
                        text = strings.groupingModeTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = strings.groupingModeSub,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DeletedGroupingMode.entries.forEach { mode ->
                            val selected = mode == deletedGroupingMode
                            val label = when (mode) {
                                DeletedGroupingMode.BY_DATE -> strings.groupByDateLabel
                                DeletedGroupingMode.BY_CONTACT -> strings.groupByContactLabel
                            }
                            GlassCard(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                elevated = selected,
                                accentHighlight = if (selected) palette.accentPrimary else null,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                onClick = { onSelectDeletedGroupingMode(mode) }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) palette.accentPrimary else palette.textSecondary,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                }

                // SECTION: Backup & Restore (Google Account & Local Backup)
                ExpandableSettingsSection(
                    icon = TxturaIcons.FolderBackup,
                    iconTint = palette.accentPrimary,
                    title = strings.backupSectionTitle,
                    subtitle = strings.backupSectionDesc,
                    accentHighlight = if (expandedBackup) palette.accentPrimary else null,
                    expanded = expandedBackup,
                    onToggleExpand = { expandedBackup = !expandedBackup }
                ) {
                    // Last Backup Status & Google Account info card
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevated = true,
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = TxturaIcons.CloudSync,
                                    contentDescription = null,
                                    tint = palette.accentPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (lastBackupTimestamp > 0L) {
                                        strings.lastBackupLabel(formatTimestamp(lastBackupTimestamp))
                                    } else {
                                        strings.noPreviousBackup
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = TxturaIcons.ContactPerson,
                                    contentDescription = null,
                                    tint = palette.safeColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (!connectedGoogleAccount.isNullOrBlank()) {
                                        strings.googleAccountConnected(connectedGoogleAccount)
                                    } else {
                                        strings.googleAccountDefaultLabel
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Subsection: Google Account & Google Drive
                    Text(
                        text = strings.googleAccountBackupTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = strings.googleAccountBackupSub,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Save to Google Drive
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            elevated = true,
                            accentHighlight = palette.accentPrimary,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            onClick = onSaveToGoogleDrive
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = TxturaIcons.CloudUpload,
                                        contentDescription = null,
                                        tint = palette.accentPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = strings.saveToGoogleDriveButton,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = palette.accentPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (isGoogleDriveInstalled) {
                                    Text(
                                        text = "Drive ✓",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.safeColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Restore from Google Drive
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            onClick = onRestoreFromGoogleDrive
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = TxturaIcons.CloudDownload,
                                    contentDescription = null,
                                    tint = palette.accentSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = strings.restoreFromGoogleDriveButton,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.textPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Open Google Account / System Backup Settings
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            onClick = onOpenSystemBackupSettings
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = TxturaIcons.SettingsGear,
                                    contentDescription = null,
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = strings.googleSystemBackupSettingsButton,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Subsection: Local Device Backup
                    Text(
                        text = strings.localBackupTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = strings.localBackupSub,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Create Local Backup
                        GlassCard(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            elevated = true,
                            accentHighlight = palette.accentPrimary,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            onClick = onCreateLocalBackup
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = TxturaIcons.FolderBackup,
                                    contentDescription = null,
                                    tint = palette.accentPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = strings.createLocalBackupButton,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.accentPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Restore Local Backup
                        GlassCard(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            elevated = true,
                            accentHighlight = palette.accentSecondary,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            onClick = onRestoreLocalBackup
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = TxturaIcons.Restore,
                                    contentDescription = null,
                                    tint = palette.accentSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = strings.restoreLocalBackupButton,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Share / Export Backup File Button
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        onClick = onShareBackupFile
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = TxturaIcons.Send,
                                contentDescription = null,
                                tint = palette.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = strings.shareBackupButton,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                        }
                    }
                }

                // SECTION: Application Permissions & Default SMS Role
                ExpandableSettingsSection(
                    icon = TxturaIcons.ShieldCheck,
                    iconTint = if (hasSmsPermission && isDefaultSmsApp) palette.safeColor else palette.accentPrimary,
                    title = strings.permissionsSectionTitle,
                    subtitle = "${strings.smsMmsContactsPermLabel} • ${strings.defaultSmsRoleLabel}",
                    accentHighlight = if (!hasSmsPermission || !isDefaultSmsApp) palette.accentPrimary else if (expandedPermissions) palette.safeColor else null,
                    expanded = expandedPermissions,
                    onToggleExpand = { expandedPermissions = !expandedPermissions }
                ) {
                    PermissionStatusRow(
                        label = strings.smsMmsContactsPermLabel,
                        granted = hasSmsPermission,
                        grantedText = strings.permGranted,
                        deniedText = strings.permDemoNotGranted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    PermissionStatusRow(
                        label = strings.defaultSmsRoleLabel,
                        granted = isDefaultSmsApp,
                        grantedText = strings.roleActive,
                        deniedText = strings.roleNotSet
                    )

                    if (rationaleMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = rationaleMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.cautionColor
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!hasSmsPermission) {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                elevated = true,
                                accentHighlight = palette.accentPrimary,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                onClick = onRequestSmsPermissions
                            ) {
                                Text(
                                    text = strings.grantSmsMmsPermButton,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.accentPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (!isDefaultSmsApp) {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                elevated = true,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                onClick = onRequestDefaultSmsRole
                            ) {
                                Text(
                                    text = strings.setDefaultSmsRoleButton,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.textPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            onClick = onOpenSystemAppSettings
                        ) {
                            Text(
                                text = strings.openSystemAppSettingsButton,
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }

    // Security Measures Dialog opened from the Shield icon in the top-right corner of Settings
    if (showSecurityMeasuresDialog) {
        SecurityMeasuresDialog(
            onDismiss = { showSecurityMeasuresDialog = false }
        )
    }
}

@Composable
private fun SecurityMeasuresDialog(
    onDismiss: () -> Unit
) {
    val palette = LocalGlassPalette.current
    val strings = TxturaStrings.current()
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (palette.isAmoled) Color(0xFF0A0F18) else MaterialTheme.colorScheme.surface,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textSecondary,
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
                contentPadding = PaddingValues(14.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(scrollState)
                ) {
                    SecurityItemRow(
                        title = strings.secItem1Title,
                        subtitle = strings.secItem1Sub
                    )
                    SecurityItemRow(
                        title = strings.secItem2Title,
                        subtitle = strings.secItem2Sub
                    )
                    SecurityItemRow(
                        title = strings.secItem3Title,
                        subtitle = strings.secItem3Sub
                    )
                    SecurityItemRow(
                        title = strings.secItem4Title,
                        subtitle = strings.secItem4Sub
                    )
                    SecurityItemRow(
                        title = strings.secItem5Title,
                        subtitle = strings.secItem5Sub
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = strings.back,
                    color = palette.safeColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
private fun ExpandableSettingsSection(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    accentHighlight: Color?,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    content: @Composable () -> Unit
) {
    val palette = LocalGlassPalette.current
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevated = true,
        accentHighlight = accentHighlight,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
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
                            .background(iconTint.copy(alpha = 0.14f))
                            .border(1.dp, iconTint.copy(alpha = 0.45f), CircleShape)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
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
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                            maxLines = if (expanded) 3 else 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (expanded) TxturaIcons.ChevronDown else TxturaIcons.ChevronRight,
                    contentDescription = null,
                    tint = palette.accentPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))
                    content()
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(
    mode: TxturaThemeMode,
    title: String,
    subtitle: String,
    activePrefix: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalGlassPalette.current
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevated = selected,
        accentHighlight = if (selected) palette.accentPrimary else null,
        contentPadding = PaddingValues(12.dp),
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
                    color = if (selected) palette.accentPrimary else palette.textPrimary,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (selected) palette.accentPrimary.copy(alpha = 0.2f)
                        else Color.Transparent
                    )
                    .border(
                        1.dp,
                        if (selected) palette.accentPrimary else palette.textSecondary.copy(alpha = 0.4f),
                        RoundedCornerShape(50)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (selected) "$activePrefix${mode.label}" else mode.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) palette.accentPrimary else palette.textSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    label: String,
    granted: Boolean,
    grantedText: String,
    deniedText: String
) {
    val palette = LocalGlassPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (granted) grantedText else deniedText,
            style = MaterialTheme.typography.labelMedium,
            color = if (granted) palette.safeColor else palette.cautionColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ConversationThreadCard(
    thread: ConversationThread,
    showContactPhotos: Boolean,
    listLayoutMode: MainListLayoutMode = MainListLayoutMode.DEFAULT,
    strings: AppStrings,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    onCallSender: () -> Unit,
    onOpenContact: () -> Unit,
    onCopyOtp: (String) -> Unit
) {
    val palette = LocalGlassPalette.current
    val lastMsg = thread.lastMessage
    val risk = thread.highestRiskLevel
    val otpCode = lastMsg?.securityAnalysis?.extractedOtpCode
    val isCompact = listLayoutMode == MainListLayoutMode.COMPACT

    val borderAccent = when {
        risk == SecurityRiskLevel.SUSPICIOUS -> palette.dangerColor
        thread.isPinned -> palette.accentPrimary
        thread.unreadCount > 0 -> palette.accentPrimary
        else -> null
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (isCompact) 14.dp else 20.dp),
        elevated = thread.unreadCount > 0 || thread.isPinned,
        accentHighlight = borderAccent,
        contentPadding = PaddingValues(
            horizontal = if (isCompact) 10.dp else 14.dp,
            vertical = if (isCompact) 5.dp else 11.dp
        ),
        onClick = onClick
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Optional Contact Avatar / Photo (compact 34.dp or default 46.dp)
                if (showContactPhotos) {
                    ContactAvatarView(
                        thread = thread,
                        size = if (isCompact) 34.dp else 46.dp,
                        onClick = onOpenContact
                    )
                    Spacer(modifier = Modifier.width(if (isCompact) 8.dp else 12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
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
                                text = thread.contactName,
                                style = if (isCompact) {
                                    MaterialTheme.typography.bodyLarge
                                } else {
                                    MaterialTheme.typography.titleMedium
                                },
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (thread.isPinned) {
                                Spacer(modifier = Modifier.width(5.dp))
                                Icon(
                                    imageVector = TxturaIcons.Pin,
                                    contentDescription = strings.pinContactThread,
                                    tint = palette.accentPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        Text(
                            text = lastMsg?.timestampMillis?.let { formatTimestamp(it) } ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (thread.unreadCount > 0) palette.accentPrimary else palette.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(if (isCompact) 1.dp else 4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = lastMsg?.body ?: "",
                            style = if (isCompact) {
                                MaterialTheme.typography.labelMedium
                            } else {
                                MaterialTheme.typography.bodyMedium
                            },
                            color = if (thread.unreadCount > 0) palette.textPrimary else palette.textSecondary,
                            fontWeight = if (thread.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = if (isCompact) 1 else 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (thread.unreadCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
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
                                    color = if (palette.isAmoled) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Inline compact Quick Actions: Pin, Call, Contact, Delete
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = TxturaIcons.Pin,
                                contentDescription = if (thread.isPinned) strings.unpinContactThread else strings.pinContactThread,
                                tint = if (thread.isPinned) palette.accentPrimary else palette.textSecondary,
                                modifier = Modifier
                                    .size(15.dp)
                                    .clickable(onClick = onTogglePin)
                            )
                            Icon(
                                imageVector = TxturaIcons.PhoneCall,
                                contentDescription = strings.callSender,
                                tint = palette.safeColor,
                                modifier = Modifier
                                    .size(15.dp)
                                    .clickable(onClick = onCallSender)
                            )
                            Icon(
                                imageVector = TxturaIcons.ContactPerson,
                                contentDescription = strings.viewContactDetails,
                                tint = palette.textSecondary,
                                modifier = Modifier
                                    .size(15.dp)
                                    .clickable(onClick = onOpenContact)
                            )
                            Icon(
                                imageVector = TxturaIcons.Delete,
                                contentDescription = strings.deleteThread,
                                tint = palette.textSecondary,
                                modifier = Modifier
                                    .size(15.dp)
                                    .clickable(onClick = onDelete)
                            )
                        }
                    }
                }
            }

            // Show OTP copy pill or Suspicious Link warning row only when applicable
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

@Composable
fun ConversationDetailScreen(
    thread: ConversationThread,
    showContactPhotos: Boolean = true,
    bubbleZoomScale: Float = 1.0f,
    onUpdateBubbleZoomScale: (Float) -> Unit = {},
    wallpaperConfig: WallpaperConfig = WallpaperConfig(),
    pendingAttachmentUri: Uri?,
    onBack: () -> Unit,
    onTogglePinThread: () -> Unit,
    onTogglePinMessage: (Long) -> Unit,
    onOpenChatWallpaperPicker: () -> Unit = {},
    onCallSender: () -> Unit,
    onOpenContact: () -> Unit,
    onPickMmsPhoto: () -> Unit,
    onClearAttachment: () -> Unit,
    onSendMessage: (String, Uri?) -> Unit,
    onCopyOtp: (String) -> Unit
) {
    val strings = TxturaStrings.current()
    var draftText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val pinnedMessages = thread.pinnedMessages
    val currentZoom by rememberUpdatedState(bubbleZoomScale)

    LaunchedEffect(thread.messages.size) {
        if (thread.messages.isNotEmpty()) {
            listState.animateScrollToItem(thread.messages.size - 1)
        }
    }

    // Only categorize as MMS when a media/file attachment is attached (never for emojis or long text)
    val isMmsMode = pendingAttachmentUri != null
    val hasNonAsciiOrEmoji = draftText.any { it.code > 127 }
    val charsPerSegment = if (hasNonAsciiOrEmoji) 70 else 160
    val smsSegments = if (draftText.isEmpty()) 1 else ((draftText.length - 1) / charsPerSegment) + 1

    GlassBackground(wallpaperConfig = wallpaperConfig) {
        val palette = LocalGlassPalette.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Sticky Glass Top Bar with Wallpaper Picker, Pin Contact, Contact Card & Direct Call actions
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
                            Text(
                                text = thread.contactName,
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                            Text(
                                text = "${thread.address} • ${strings.protectedChannelSuffix}",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Change Chat Wallpaper for this Specific Contact
                    GlassIconButton(
                        icon = TxturaIcons.SparklesTheme,
                        contentDescription = strings.chatWallpaperTooltip,
                        tint = palette.accentPrimary,
                        onClick = onOpenChatWallpaperPicker,
                        size = 36.dp
                    )

                    Spacer(modifier = Modifier.width(5.dp))

                    // Pin / Unpin Contact Conversation Button
                    GlassIconButton(
                        icon = TxturaIcons.Pin,
                        contentDescription = if (thread.isPinned) strings.unpinContactThread else strings.pinContactThread,
                        tint = if (thread.isPinned) palette.accentPrimary else palette.textSecondary,
                        accentHighlight = if (thread.isPinned) palette.accentPrimary else null,
                        onClick = onTogglePinThread,
                        size = 36.dp
                    )

                    Spacer(modifier = Modifier.width(5.dp))

                    // Open Contact Details in Default Contacts App
                    GlassIconButton(
                        icon = TxturaIcons.ContactPerson,
                        contentDescription = strings.viewContactDetails,
                        tint = palette.accentPrimary,
                        onClick = onOpenContact,
                        size = 36.dp
                    )

                    Spacer(modifier = Modifier.width(5.dp))

                    // Call Sender Directly via Default Phone/Dialer App
                    GlassIconButton(
                        icon = TxturaIcons.PhoneCall,
                        contentDescription = strings.callSender,
                        tint = palette.safeColor,
                        accentHighlight = palette.safeColor,
                        onClick = onCallSender,
                        size = 36.dp
                    )
                }
            }

            // Pinned Messages Banner (shows messages pinned from this contact)
            if (pinnedMessages.isNotEmpty()) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(18.dp),
                    accentHighlight = palette.accentPrimary,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = TxturaIcons.Pin,
                                contentDescription = null,
                                tint = palette.accentPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${strings.pinnedMessagesTitle} (${pinnedMessages.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.accentPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        pinnedMessages.takeLast(2).forEach { pinnedMsg ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = pinnedMsg.body,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = TxturaIcons.Close,
                                    contentDescription = strings.unpinMessage,
                                    tint = palette.textSecondary,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { onTogglePinMessage(pinnedMsg.id) }
                                )
                            }
                        }
                    }
                }
            }

            // Messages Thread with two-finger Pinch-to-Zoom gesture that persists default bubble size
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
                    MessageBubbleItem(
                        message = msg,
                        zoomScale = bubbleZoomScale,
                        strings = strings,
                        onTogglePin = { onTogglePinMessage(msg.id) },
                        onCopyOtp = onCopyOtp
                    )
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
                    // Pending MMS Attachment Preview
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
                        // Attach MMS Image via Zero-Permission Photo Picker
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
                        GlassIconButton(
                            icon = TxturaIcons.Send,
                            contentDescription = strings.send,
                            tint = if (canSend) palette.accentPrimary else palette.textSecondary,
                            onClick = {
                                if (canSend) {
                                    onSendMessage(draftText, pendingAttachmentUri)
                                    draftText = ""
                                }
                            },
                            size = 42.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubbleItem(
    message: ChatMessage,
    zoomScale: Float = 1.0f,
    strings: AppStrings,
    onTogglePin: () -> Unit,
    onCopyOtp: (String) -> Unit
) {
    val palette = LocalGlassPalette.current
    val isOutgoing = !message.isIncoming
    val analysis = message.securityAnalysis
    val s = zoomScale.coerceIn(0.75f, 1.65f)
    val cornerRadius = (18f * s).coerceIn(12f, 26f).dp
    val tailRadius = (4f * s).coerceIn(3f, 8f).dp

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
        palette.outgoingBubbleStart,
        palette.outgoingBubbleEnd,
        palette.incomingBubbleSurface
    ) {
        if (isOutgoing) {
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
        analysis.riskLevel == SecurityRiskLevel.SUSPICIOUS -> palette.dangerColor
        message.isPinned -> palette.accentPrimary
        isOutgoing -> palette.accentPrimary.copy(alpha = 0.55f)
        else -> palette.glassBorderTop
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
                .border(1.dp, borderColor, bubbleShape)
                .padding(horizontal = (12f * s).dp, vertical = (8f * s).dp)
        ) {
            Column {
                // MMS Attachment Display
                if (message.attachment != null) {
                    MmsAttachmentCard(
                        uri = message.attachment.uri,
                        caption = message.attachment.caption,
                        hdrLabel = strings.hdrMmsPhoto
                    )
                    Spacer(modifier = Modifier.height((6f * s).dp))
                }

                // Message Text (scaled with pinch-to-zoom factor)
                Text(
                    text = message.body,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = (14f * s).sp,
                        lineHeight = (19f * s).sp
                    ),
                    color = if (isOutgoing) Color.White else palette.textPrimary
                )

                // Anti-Phishing Security Warning Box
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

                // One-tap OTP Extractor
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

                // Timestamp + Pin Message Action + SMS/MMS badge + Delivery status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.align(Alignment.End)
                ) {
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

/**
 * Displays a contact's actual photo from Contacts (if available) or a glassmorphic initials avatar.
 */
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

    val contactBitmap by produceState<android.graphics.Bitmap?>(initialValue = null, key1 = photoUri) {
        value = if (photoUri == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(photoUri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                } catch (_: Exception) {
                    null
                }
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

@Composable
fun NewMessageDialog(
    initialRecipient: String = "",
    initialBody: String = "",
    onPickContact: () -> Unit,
    onDismiss: () -> Unit,
    onSend: (recipient: String, body: String) -> Unit
) {
    val palette = LocalGlassPalette.current
    val strings = TxturaStrings.current()
    var recipient by remember(initialRecipient) { mutableStateOf(initialRecipient) }
    var body by remember(initialBody) { mutableStateOf(initialBody) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (palette.isAmoled) Color(0xFF0D1117) else MaterialTheme.colorScheme.surface,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textSecondary,
        title = {
            Text(
                text = strings.newSmsMmsDialogTitle,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            BasicTextField(
                                value = recipient,
                                onValueChange = { recipient = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.textPrimary),
                                cursorBrush = SolidColor(palette.accentPrimary),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        // Pick contact directly from the system Default Contacts App
                        GlassIconButton(
                            icon = TxturaIcons.ContactPerson,
                            contentDescription = strings.pickFromContacts,
                            tint = palette.accentPrimary,
                            onClick = onPickContact,
                            size = 38.dp
                        )
                    }
                }

                GlassCard(
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Column {
                        Text(
                            text = strings.messageBodyLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.accentPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        BasicTextField(
                            value = body,
                            onValueChange = { body = it },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.textPrimary),
                            cursorBrush = SolidColor(palette.accentPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (recipient.isNotBlank() && body.isNotBlank()) {
                        onSend(recipient, body)
                    }
                }
            ) {
                Text(strings.send, color = palette.accentPrimary, fontWeight = FontWeight.Bold)
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

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (palette.isAmoled) Color(0xFF0A0F18) else MaterialTheme.colorScheme.surface,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textSecondary,
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
    Column {
        Text(
            text = "✓ $title",
            style = MaterialTheme.typography.labelMedium,
            color = palette.safeColor,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
            fontSize = 13.sp
        )
    }
}

/**
 * Interactive horizontal swipe wrapper for conversation cards in the main menu.
 * Reveals a glowing glassmorphic action pill underneath as the user drags left or right,
 * and triggers the configured [swipeRightAction] or [swipeLeftAction] when released past threshold.
 */
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
        // Revealed background action pill during swipe
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
                    Text(
                        text = swipeActionLabel(activeAction, strings),
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
        SwipeAction.OPEN_CONTACT -> strings.swipeActionContact
        SwipeAction.NONE -> strings.swipeActionNone
    }
}

private fun swipeActionIcon(action: SwipeAction): ImageVector {
    return when (action) {
        SwipeAction.PIN -> TxturaIcons.Pin
        SwipeAction.DELETE -> TxturaIcons.Delete
        SwipeAction.CALL -> TxturaIcons.PhoneCall
        SwipeAction.MARK_READ -> TxturaIcons.CheckDouble
        SwipeAction.OPEN_CONTACT -> TxturaIcons.ContactPerson
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
        SwipeAction.OPEN_CONTACT -> palette.cautionColor
        SwipeAction.NONE -> palette.textSecondary
    }
}

/**
 * Confirmation dialog shown before moving a conversation to Deleted Messages (Trash).
 */
@Composable
fun ConfirmMoveToTrashDialog(
    contactName: String,
    retentionDays: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalGlassPalette.current
    val strings = TxturaStrings.current()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (palette.isAmoled) Color(0xFF0A0F18) else MaterialTheme.colorScheme.surface,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textSecondary,
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

/**
 * Confirmation dialog shown before restoring data from a backup file (Local or Google Drive).
 */
@Composable
fun ConfirmRestoreDialog(
    payload: BackupRestorePayload,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalGlassPalette.current
    val strings = TxturaStrings.current()
    val dateStr = formatTimestamp(payload.createdAtMillis)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (palette.isAmoled) Color(0xFF0A0F18) else MaterialTheme.colorScheme.surface,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textSecondary,
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

/**
 * Dedicated screen for Deleted Messages (Trash / Obrisane poruke) with:
 * - Interactive switching between grouping/sorting By Deletion Date vs By Contact
 * - Restoring messages back to the inbox within the retention window (default 30 days)
 * - Permanent deletion with confirmation
 */
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
    val palette = LocalGlassPalette.current
    val strings = TxturaStrings.current()
    var pendingPermanentDeleteEntry by remember { mutableStateOf<DeletedConversationEntry?>(null) }

    // Group entries either by Deletion Date (sorted newest first) or by Contact Name (sorted alphabetically)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Top Bar
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
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = strings.deletedMessagesTitle,
                                style = MaterialTheme.typography.titleLarge,
                                color = palette.textPrimary
                            )
                            Text(
                                text = strings.deletedMessagesSubtitle(deletedEntries.size, retentionDays),
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                        }
                    }

                    if (deletedEntries.isNotEmpty()) {
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
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Grouping / Sorting Selector Bar (By Deletion Date vs By Contact)
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
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        elevated = selected,
                        accentHighlight = if (selected) palette.accentPrimary else null,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        onClick = { onSelectGroupingMode(mode) }
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
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
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
                                            Text(
                                                text = entry.thread.contactName,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = palette.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${entry.thread.address} • ${strings.messagesCountLabel(entry.thread.messages.size)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.textSecondary
                                            )
                                        }
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
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Preview of deleted messages in this contact's thread
                                    entry.thread.messages.takeLast(2).forEach { msg ->
                                        Text(
                                            text = "• ${msg.body}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = palette.textSecondary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "${strings.deletedOnPrefix}${formatTimestamp(entry.deletedAtMillis)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.textSecondary
                                        )

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            // Restore button
                                            GlassCard(
                                                shape = RoundedCornerShape(50),
                                                elevated = true,
                                                accentHighlight = palette.safeColor,
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                onClick = { onRestoreThread(entry.thread.id) }
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = TxturaIcons.Restore,
                                                        contentDescription = null,
                                                        tint = palette.safeColor,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(5.dp))
                                                    Text(
                                                        text = strings.restoreButton,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = palette.safeColor,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            // Permanent Delete button
                                            GlassCard(
                                                shape = RoundedCornerShape(50),
                                                accentHighlight = palette.dangerColor,
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                onClick = { pendingPermanentDeleteEntry = entry }
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = TxturaIcons.Delete,
                                                        contentDescription = null,
                                                        tint = palette.dangerColor,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(5.dp))
                                                    Text(
                                                        text = strings.deletePermanentlyButton,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = palette.dangerColor,
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
            }
        }
    }

    val entryToConfirm = pendingPermanentDeleteEntry
    if (entryToConfirm != null) {
        AlertDialog(
            onDismissRequest = { pendingPermanentDeleteEntry = null },
            containerColor = if (palette.isAmoled) Color(0xFF0A0F18) else MaterialTheme.colorScheme.surface,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
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

private val shortTimeFormatter = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
}

private val dateAndTimeFormatter = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("dd.MM. HH:mm", Locale.getDefault())
}

private fun formatTimestamp(millis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - millis
    val formatter = if (diff < 86_400_000L) {
        shortTimeFormatter.get()
    } else {
        dateAndTimeFormatter.get()
    }
    return formatter?.format(Date(millis)).orEmpty()
}

/**
 * Displays the 10 curated built-in wallpapers + Default theme option with miniature color-gradient
 * preview cards so the user can pick a background for the main window or for a specific contact's chat.
 */
@Composable
fun WallpaperPresetSelectorGrid(
    currentConfig: WallpaperConfig,
    strings: AppStrings,
    onSelectPreset: (WallpaperPreset) -> Unit
) {
    val palette = LocalGlassPalette.current
    var selectedCategory by remember(currentConfig.preset) {
        mutableStateOf(
            if (currentConfig.preset.isMonochrome) UiColorMode.MONOCHROME else UiColorMode.COLORFUL
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Segmented tabs for Colorful (10) vs Monochrome (10)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val colorfulActive = selectedCategory == UiColorMode.COLORFUL
            GlassCard(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                elevated = colorfulActive,
                accentHighlight = if (colorfulActive) palette.accentPrimary else null,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp),
                onClick = { selectedCategory = UiColorMode.COLORFUL }
            ) {
                Text(
                    text = strings.wallpaperTabColorful,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (colorfulActive) palette.accentPrimary else palette.textSecondary,
                    fontWeight = if (colorfulActive) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            val monoActive = selectedCategory == UiColorMode.MONOCHROME
            GlassCard(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                elevated = monoActive,
                accentHighlight = if (monoActive) palette.accentPrimary else null,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp),
                onClick = { selectedCategory = UiColorMode.MONOCHROME }
            ) {
                Text(
                    text = strings.wallpaperTabMonochrome,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (monoActive) palette.accentPrimary else palette.textSecondary,
                    fontWeight = if (monoActive) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        val presets = if (selectedCategory == UiColorMode.COLORFUL) {
            WallpaperPreset.colorfulPresets
        } else {
            WallpaperPreset.monochromePresets
        }

        presets.forEach { preset ->
            val selected = !currentConfig.isCustomGalleryImage && currentConfig.preset == preset
            val label = wallpaperPresetTitle(preset, strings)
            val compAccent = WallpaperHarmonyEngine.presetComplementaryAccent(preset, palette.isDark)
            val previewColors = listOf(
                Color(preset.primaryOrbHex),
                Color(preset.secondaryOrbHex),
                Color(preset.accentOrbHex)
            )
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                elevated = selected,
                accentHighlight = if (selected) compAccent else null,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
                onClick = { onSelectPreset(preset) }
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
                        // Miniature wallpaper gradient swatch + complementary UI accent dot
                        Box(
                            contentAlignment = Alignment.BottomEnd,
                            modifier = Modifier
                                .size(width = 44.dp, height = 26.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.linearGradient(colors = previewColors))
                                .border(
                                    1.dp,
                                    if (selected) compAccent else Color.White.copy(alpha = 0.35f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(compAccent)
                                    .border(1.dp, Color.Black.copy(alpha = 0.55f), CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selected) compAccent else palette.textPrimary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                    if (selected) {
                        Icon(
                            imageVector = TxturaIcons.CheckDouble,
                            contentDescription = null,
                            tint = compAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dialog for selecting a custom wallpaper for a specific contact's chat (or the main window),
 * offering 10 built-in presets + Default + picking a personal photo from the Gallery + resetting to main window wallpaper.
 */
@Composable
fun WallpaperPickerDialog(
    title: String,
    currentConfig: WallpaperConfig,
    showResetToMainButton: Boolean = false,
    onSelectPreset: (WallpaperPreset) -> Unit,
    onPickFromGallery: () -> Unit,
    onResetToMainWallpaper: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val palette = LocalGlassPalette.current
    val strings = TxturaStrings.current()
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (palette.isAmoled) Color(0xFF0A0F18) else MaterialTheme.colorScheme.surface,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textSecondary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = TxturaIcons.SparklesTheme,
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
                    .height(400.dp)
                    .verticalScroll(scrollState)
            ) {
                // Pick Personal Image from Gallery Button
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevated = true,
                    accentHighlight = palette.accentPrimary,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    onClick = onPickFromGallery
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
                                imageVector = TxturaIcons.AttachImage,
                                contentDescription = null,
                                tint = palette.accentPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = strings.pickWallpaperFromGallery,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = palette.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                if (currentConfig.isCustomGalleryImage) {
                                    Text(
                                        text = "✓ ${strings.customGalleryWallpaperActive}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.safeColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        Text(
                            text = "+",
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.accentPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

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
                    onSelectPreset = onSelectPreset
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
    }
}
