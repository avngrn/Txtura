package com.txtura

import android.Manifest
import android.app.role.RoleManager
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.ContactsContract
import android.provider.Settings
import android.provider.Telephony
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.txtura.data.AppSettingsSnapshot
import com.txtura.data.BackupManager
import com.txtura.data.BackupRestorePayload
import com.txtura.data.ConversationThread
import com.txtura.data.DeletedGroupingMode
import com.txtura.data.MainListLayoutMode
import com.txtura.data.LauncherIconShape
import com.txtura.data.SmsMmsRepository
import com.txtura.data.SwipeAction
import com.txtura.data.UiColorMode
import com.txtura.data.WallpaperConfig
import com.txtura.data.WallpaperPreset
import com.txtura.security.MessageSecurityAnalyzer
import com.txtura.ui.i18n.TxturaStrings
import com.txtura.ui.screens.ConfirmMoveToTrashDialog
import com.txtura.ui.screens.ConfirmRestoreDialog
import com.txtura.ui.screens.ConversationDetailScreen
import com.txtura.ui.screens.ConversationsListScreen
import com.txtura.ui.screens.DefaultSmsRolePromptDialog
import com.txtura.ui.screens.DeletedMessagesScreen
import com.txtura.ui.screens.NewMessageDialog
import com.txtura.ui.screens.SettingsScreen
import com.txtura.ui.screens.WallpaperPickerDialog
import com.txtura.ui.theme.TxturaTheme
import com.txtura.ui.theme.TxturaThemeMode
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {

    companion object {
        private const val APP_PREFS = "txtura_app_prefs"
        private const val KEY_FIRST_STARTUP_DONE = "first_startup_permissions_requested"
        private const val KEY_THEME_MODE = "selected_theme_mode"
        private const val KEY_SHOW_CONTACT_PHOTOS = "show_contact_photos"
        private const val KEY_MAIN_LIST_LAYOUT_MODE = "main_list_layout_mode"
        private const val KEY_BUBBLE_ZOOM_SCALE = "bubble_zoom_scale"
        private const val KEY_SWIPE_RIGHT_ACTION = "swipe_right_action"
        private const val KEY_SWIPE_LEFT_ACTION = "swipe_left_action"
        private const val KEY_AUTO_DELETE_DAYS = "auto_delete_days"
        private const val KEY_DELETED_GROUPING_MODE = "deleted_grouping_mode"
        private const val KEY_MAIN_WALLPAPER = "main_window_wallpaper"
        private const val KEY_CONTACT_WALLPAPERS = "contact_chat_wallpapers_v1"
        private const val KEY_UI_COLOR_MODE = "ui_color_mode"
        private const val KEY_LAUNCHER_ICON_SHAPE = "launcher_icon_shape"
        private const val GALLERY_TARGET_MAIN = "__MAIN__"
    }

    private var themeMode by mutableStateOf(TxturaThemeMode.AMOLED)
    private var showContactPhotos by mutableStateOf(value = true)
    private var listLayoutMode by mutableStateOf(MainListLayoutMode.DEFAULT)
    private var bubbleZoomScale by mutableFloatStateOf(1.0f)
    private var mainWallpaper by mutableStateOf(WallpaperConfig())
    private var contactWallpapers by mutableStateOf<Map<String, WallpaperConfig>>(emptyMap())
    private var uiColorMode by mutableStateOf(UiColorMode.COLORFUL)
    private var launcherIconShape by mutableStateOf(LauncherIconShape.ROUNDED_SQUARE)
    private var swipeRightAction by mutableStateOf(SwipeAction.PIN)
    private var swipeLeftAction by mutableStateOf(SwipeAction.DELETE)
    private var autoDeleteDays by mutableIntStateOf(30)
    private var deletedGroupingMode by mutableStateOf(DeletedGroupingMode.BY_DATE)

    private var lastBackupTimestamp by mutableLongStateOf(0L)
    private var pendingRestorePayload by mutableStateOf<BackupRestorePayload?>(null)
    private var connectedGoogleAccount by mutableStateOf<String?>(null)
    private var isGoogleDriveInstalled by mutableStateOf(value = false)

    private var activeThreadId by mutableStateOf<Long?>(null)
    private var showSettingsScreen by mutableStateOf(value = false)
    private var showDeletedMessagesScreen by mutableStateOf(value = false)
    private var showChatWallpaperDialogForThread by mutableStateOf<ConversationThread?>(null)
    private var wallpaperGalleryTargetKey by mutableStateOf<String?>(null)
    private var threadPendingDeleteConfirmation by mutableStateOf<ConversationThread?>(null)
    private var pendingMmsUri by mutableStateOf<Uri?>(null)
    private var showNewMessageDialog by mutableStateOf(value = false)
    private var showDefaultSmsRolePrompt by mutableStateOf(value = false)
    private var prefilledRecipient by mutableStateOf("")
    private var prefilledBody by mutableStateOf("")
    private var rationaleBannerText by mutableStateOf<String?>(null)
    private var permissionRefreshTick by mutableIntStateOf(0)

    private val requiredSmsPermissions: Array<String>
        get() = buildList {
            add(Manifest.permission.READ_SMS)
            add(Manifest.permission.SEND_SMS)
            add(Manifest.permission.RECEIVE_SMS)
            add(Manifest.permission.RECEIVE_MMS)
            add(Manifest.permission.READ_CONTACTS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()

    // 3-State Runtime Permission Launcher (Check -> Rationale -> Launch -> Settings on permanent denial)
    private val smsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            permissionRefreshTick++
            val strings = TxturaStrings.forLocale()
            val readGranted = results[Manifest.permission.READ_SMS] == true
            val sendGranted = results[Manifest.permission.SEND_SMS] == true

            if (readGranted || sendGranted) {
                rationaleBannerText = null
                lifecycleScope.launch {
                    SmsMmsRepository.syncDeviceMessages(this@MainActivity)
                }
            } else {
                val permanentlyDenied = !shouldShowRequestPermissionRationale(Manifest.permission.READ_SMS) &&
                    !shouldShowRequestPermissionRationale(Manifest.permission.SEND_SMS)

                rationaleBannerText = if (permanentlyDenied) {
                    strings.rationalePermanentDenial
                } else {
                    strings.rationaleRequired
                }
            }

            // After initial permissions dialog finishes, check if Default SMS Role prompt should appear
            if (!SmsMmsRepository.isDefaultSmsApp(this@MainActivity)) {
                showDefaultSmsRolePrompt = true
            }
        }

    // Zero-Permission Photo Picker for MMS attachments (avoids READ_MEDIA_IMAGES / READ_EXTERNAL_STORAGE)
    private val photoPickerLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { pendingMmsUri = it }
        }

    // Zero-Permission Photo Picker for Custom Wallpapers (Main Window or Per-Contact Chat)
    private val wallpaperPhotoPickerLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { pickedUri ->
            val targetKey = wallpaperGalleryTargetKey
            if ((pickedUri != null) && (targetKey != null)) {
                lifecycleScope.launch {
                    val savedFileUri = copyPickedWallpaperToInternalStorage(pickedUri, targetKey)
                    if (savedFileUri != null) {
                        val customConfig = WallpaperConfig(
                            preset = WallpaperPreset.DEFAULT,
                            customImageUri = savedFileUri.toString(),
                        )
                        if (targetKey == GALLERY_TARGET_MAIN) {
                            mainWallpaper = customConfig
                            saveMainWallpaper(customConfig)
                        } else {
                            val updated = contactWallpapers.toMutableMap().apply {
                                put(targetKey, customConfig)
                            }
                            contactWallpapers = updated
                            saveContactWallpapers(updated)
                        }
                    }
                }
            }
        }

    // Default SMS Role Request Launcher
    private val defaultSmsRoleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            permissionRefreshTick++
            showDefaultSmsRolePrompt = false
            // If user granted default SMS role, also ensure runtime SMS/Contacts permissions are synced
            val allGranted = requiredSmsPermissions.all { perm ->
                ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
            }
            if (!allGranted) {
                smsPermissionLauncher.launch(requiredSmsPermissions)
            } else {
                lifecycleScope.launch {
                    SmsMmsRepository.syncDeviceMessages(this@MainActivity)
                }
            }
        }

    // System Contacts App Phone Picker Launcher (picks a phone number from the default Contacts app)
    private val contactPhonePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val contactUri = result.data?.data ?: return@registerForActivityResult
            try {
                contentResolver.query(
                    contactUri,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val rawNumber = cursor.getString(0).orEmpty()
                        prefilledRecipient = MessageSecurityAnalyzer.sanitizePhoneNumber(rawNumber)
                    }
                }
            } catch (_: Exception) {
                // Ignore malformed contact URI
            }
        }

    // Local / Google Drive Backup Document Creation Launcher
    private val createBackupLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) {
                lifecycleScope.launch {
                    val settings = buildAppSettingsSnapshot()
                    val (pinnedAddrs, pinnedMsgs) = SmsMmsRepository.getPinnedDataSnapshot()
                    val deletedMeta = SmsMmsRepository.getDeletedThreadsMetaSnapshot()
                    val currentThreads = SmsMmsRepository.threads.value
                    val currentDeleted = SmsMmsRepository.deletedThreads.value

                    val json = BackupManager.generateBackupJson(
                        settings = settings,
                        threads = currentThreads,
                        deletedThreads = currentDeleted,
                        pinnedAddresses = pinnedAddrs,
                        pinnedMessageIds = pinnedMsgs,
                        deletedThreadsMeta = deletedMeta
                    )
                    val success = BackupManager.saveBackupToUri(this@MainActivity, uri, json)
                    if (success) {
                        BackupManager.createInternalBackupFile(this@MainActivity, json)
                        lastBackupTimestamp = System.currentTimeMillis()
                        val locStrings = TxturaStrings.forLocale()
                        val totalMsgs = currentThreads.sumOf { it.messages.size } + currentDeleted.sumOf { it.thread.messages.size }
                        Toast.makeText(
                            this@MainActivity,
                            locStrings.backupCreatedSuccess(currentThreads.size + currentDeleted.size, totalMsgs),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

    // Local / Google Drive Backup File Open / Restore Launcher
    private val restoreBackupLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                lifecycleScope.launch {
                    try {
                        val json = BackupManager.readBackupFromUri(this@MainActivity, uri)
                        val payload = BackupManager.parseBackupJson(json)
                        pendingRestorePayload = payload
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Failed to read backup from $uri", e)
                        val locStrings = TxturaStrings.forLocale()
                        Toast.makeText(
                            this@MainActivity,
                            locStrings.backupErrorInvalid,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        loadSavedAppPreferences()
        lastBackupTimestamp = BackupManager.getLastBackupTimestamp(this)
        connectedGoogleAccount = BackupManager.getConnectedGoogleAccount(this)
        isGoogleDriveInstalled = BackupManager.isGoogleDriveInstalled(this)
        handleIncomingSmsIntentSafely(intent)
        checkFirstStartupPermissionsAndRole()

        setContent {
            val threads by SmsMmsRepository.threads.collectAsState()
            val deletedThreads by SmsMmsRepository.deletedThreads.collectAsState()

            // Query permissions dynamically (never cache permission state in fields)
            val hasSmsPerm = rememberDynamicSmsPermissionState(permissionRefreshTick)
            val isDefaultSms = rememberDynamicDefaultSmsState(permissionRefreshTick)

            LaunchedEffect(hasSmsPerm, isDefaultSms, autoDeleteDays) {
                SmsMmsRepository.purgeExpiredDeletedThreads(this@MainActivity, autoDeleteDays)
                if (hasSmsPerm || isDefaultSms) {
                    SmsMmsRepository.syncDeviceMessages(this@MainActivity, autoDeleteDays)
                }
            }

            val selectedThread = threads.find { it.id == activeThreadId }
            val activeWallpaper = if ((selectedThread != null) && !showSettingsScreen && !showDeletedMessagesScreen) {
                contactWallpapers[contactWallpaperKey(selectedThread.address)] ?: mainWallpaper
            } else {
                mainWallpaper
            }

            TxturaTheme(
                themeMode = themeMode,
                wallpaperConfig = activeWallpaper
            ) {
                val strings = TxturaStrings.current()

                when {
                    showDeletedMessagesScreen -> {
                        BackHandler {
                            showDeletedMessagesScreen = false
                        }
                        DeletedMessagesScreen(
                            deletedEntries = deletedThreads,
                            retentionDays = autoDeleteDays,
                            groupingMode = deletedGroupingMode,
                            showContactPhotos = showContactPhotos,
                            mainWallpaper = mainWallpaper,
                            onBack = { showDeletedMessagesScreen = false },
                            onSelectGroupingMode = { mode ->
                                deletedGroupingMode = mode
                                saveDeletedGroupingMode(mode)
                            },
                            onRestoreThread = { threadId ->
                                val restoredName = SmsMmsRepository.restoreDeletedThread(
                                    this@MainActivity,
                                    threadId
                                )
                                if (restoredName != null) {
                                    val locStrings = TxturaStrings.forLocale()
                                    Toast.makeText(
                                        this@MainActivity,
                                        locStrings.restoredToast(restoredName),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onPermanentlyDeleteThread = { threadId ->
                                SmsMmsRepository.permanentlyDeleteThread(this@MainActivity, threadId)
                            },
                            onEmptyTrash = {
                                SmsMmsRepository.emptyTrash(this@MainActivity)
                            }
                        )
                    }

                    showSettingsScreen -> {
                        BackHandler {
                            showSettingsScreen = false
                        }
                        SettingsScreen(
                            themeMode = themeMode,
                            showContactPhotos = showContactPhotos,
                            listLayoutMode = listLayoutMode,
                            mainWallpaper = mainWallpaper,
                            uiColorMode = uiColorMode,
                            launcherIconShape = launcherIconShape,
                            swipeRightAction = swipeRightAction,
                            swipeLeftAction = swipeLeftAction,
                            autoDeleteDays = autoDeleteDays,
                            deletedGroupingMode = deletedGroupingMode,
                            deletedMessagesCount = deletedThreads.size,
                            hasSmsPermission = hasSmsPerm,
                            isDefaultSmsApp = isDefaultSms,
                            rationaleMessage = rationaleBannerText,
                            onBack = { showSettingsScreen = false },
                            onSelectTheme = { selectedMode ->
                                themeMode = selectedMode
                                saveSelectedTheme(selectedMode)
                            },
                            onToggleShowContactPhotos = { enabled ->
                                showContactPhotos = enabled
                                saveShowContactPhotos(enabled)
                            },
                            onSelectListLayoutMode = { mode ->
                                listLayoutMode = mode
                                saveListLayoutMode(mode)
                            },
                            onSelectUiColorMode = { mode ->
                                uiColorMode = mode
                                saveUiColorMode(mode)
                                // If user switches mode, select the corresponding default/first preset if currently mismatched
                                if (mode == UiColorMode.MONOCHROME && !mainWallpaper.preset.isMonochrome) {
                                    val cfg = WallpaperConfig(preset = WallpaperPreset.MONO_SLATE, customImageUri = null)
                                    mainWallpaper = cfg
                                    saveMainWallpaper(cfg)
                                } else if (mode == UiColorMode.COLORFUL && mainWallpaper.preset.isMonochrome) {
                                    val cfg = WallpaperConfig(preset = WallpaperPreset.DEFAULT, customImageUri = null)
                                    mainWallpaper = cfg
                                    saveMainWallpaper(cfg)
                                }
                            },
                            onSelectLauncherIconShape = { shape ->
                                launcherIconShape = shape
                                saveLauncherIconShape(shape)
                                val locStrings = TxturaStrings.forLocale()
                                val shapeName = when (shape) {
                                    LauncherIconShape.CIRCLE -> locStrings.launcherIconShapeCircle
                                    LauncherIconShape.SQUIRCLE -> locStrings.launcherIconShapeSquircle
                                    LauncherIconShape.ROUNDED_SQUARE -> locStrings.launcherIconShapeRoundedSquare
                                    LauncherIconShape.TEARDROP -> locStrings.launcherIconShapeTeardrop
                                    LauncherIconShape.PEBBLE -> locStrings.launcherIconShapePebble
                                }
                                Toast.makeText(
                                    this@MainActivity,
                                    locStrings.launcherIconShapeSavedToast(shapeName),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onSelectMainWallpaperPreset = { preset ->
                                val cfg = WallpaperConfig(preset = preset, customImageUri = null)
                                mainWallpaper = cfg
                                saveMainWallpaper(cfg)
                            },
                            onPickMainWallpaperFromGallery = {
                                wallpaperGalleryTargetKey = GALLERY_TARGET_MAIN
                                wallpaperPhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onSelectSwipeRightAction = { action ->
                                swipeRightAction = action
                                saveSwipeRightAction(action)
                            },
                            onSelectSwipeLeftAction = { action ->
                                swipeLeftAction = action
                                saveSwipeLeftAction(action)
                            },
                            onSelectAutoDeleteDays = { days ->
                                autoDeleteDays = days
                                saveAutoDeleteDays(days)
                                SmsMmsRepository.purgeExpiredDeletedThreads(this@MainActivity, days)
                            },
                            onSelectDeletedGroupingMode = { mode ->
                                deletedGroupingMode = mode
                                saveDeletedGroupingMode(mode)
                            },
                            onOpenDeletedMessages = {
                                showDeletedMessagesScreen = true
                            },
                            lastBackupTimestamp = lastBackupTimestamp,
                            connectedGoogleAccount = connectedGoogleAccount,
                            isGoogleDriveInstalled = isGoogleDriveInstalled,
                            onCreateLocalBackup = ::handleCreateLocalBackup,
                            onRestoreLocalBackup = ::handleRestoreLocalBackup,
                            onShareBackupFile = ::handleShareBackupFile,
                            onSaveToGoogleDrive = ::handleSaveToGoogleDrive,
                            onRestoreFromGoogleDrive = ::handleRestoreFromGoogleDrive,
                            onOpenSystemBackupSettings = ::handleOpenSystemBackupSettings,
                            onRequestSmsPermissions = ::requestSmsPermissionsSafely,
                            onRequestDefaultSmsRole = ::requestDefaultSmsRoleSafely,
                            onOpenSystemAppSettings = ::openAppDetailsSettings
                        )
                    }

                    selectedThread != null -> {
                        BackHandler {
                            activeThreadId = null
                            pendingMmsUri = null
                        }
                        val contactKey = contactWallpaperKey(selectedThread.address)
                        val activeChatWallpaper = contactWallpapers[contactKey] ?: mainWallpaper

                        ConversationDetailScreen(
                            thread = selectedThread,
                            showContactPhotos = showContactPhotos,
                            bubbleZoomScale = bubbleZoomScale,
                            onUpdateBubbleZoomScale = { newScale ->
                                bubbleZoomScale = newScale
                                saveBubbleZoomScale(newScale)
                            },
                            wallpaperConfig = activeChatWallpaper,
                            pendingAttachmentUri = pendingMmsUri,
                            onBack = {
                                activeThreadId = null
                                pendingMmsUri = null
                            },
                            onTogglePinThread = {
                                SmsMmsRepository.togglePinThread(this@MainActivity, selectedThread.id)
                            },
                            onTogglePinMessage = { msgId ->
                                SmsMmsRepository.togglePinMessage(this@MainActivity, selectedThread.id, msgId)
                            },
                            onOpenChatWallpaperPicker = {
                                showChatWallpaperDialogForThread = selectedThread
                            },
                            onCallSender = {
                                dialPhoneNumberSafely(selectedThread.address)
                            },
                            onOpenContact = {
                                openOrInsertContactSafely(
                                    phoneNumber = selectedThread.address,
                                    displayName = selectedThread.contactName
                                )
                            },
                            onPickMmsPhoto = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onClearAttachment = { pendingMmsUri = null },
                            onSendMessage = { text, attachment ->
                                SmsMmsRepository.sendMessage(
                                    context = this@MainActivity,
                                    recipientAddress = selectedThread.address,
                                    rawBody = text,
                                    attachmentUri = attachment
                                )
                                pendingMmsUri = null
                            },
                            onCopyOtp = ::copyOtpConfidentially
                        )
                    }

                    else -> {
                        ConversationsListScreen(
                            threads = threads,
                            deletedCount = deletedThreads.size,
                            showContactPhotos = showContactPhotos,
                            listLayoutMode = listLayoutMode,
                            mainWallpaper = mainWallpaper,
                            swipeRightAction = swipeRightAction,
                            swipeLeftAction = swipeLeftAction,
                            onOpenThread = { id ->
                                SmsMmsRepository.markThreadRead(this@MainActivity, id)
                                activeThreadId = id
                            },
                            onTogglePin = { id ->
                                SmsMmsRepository.togglePinThread(this@MainActivity, id)
                            },
                            onRequestDeleteThread = { thread ->
                                threadPendingDeleteConfirmation = thread
                            },
                            onMarkThreadRead = { id ->
                                SmsMmsRepository.markThreadRead(this@MainActivity, id)
                            },
                            onCallSender = ::dialPhoneNumberSafely,
                            onOpenContact = ::openOrInsertContactSafely,
                            onCopyOtp = ::copyOtpConfidentially,
                            onOpenNewMessage = {
                                prefilledRecipient = ""
                                prefilledBody = ""
                                showNewMessageDialog = true
                            },
                            onOpenDeletedMessages = { showDeletedMessagesScreen = true },
                            onOpenSettings = { showSettingsScreen = true }
                        )
                    }
                }

                // Per-Contact Chat Wallpaper Picker Dialog
                val wallpaperDialogThread = showChatWallpaperDialogForThread
                if (wallpaperDialogThread != null) {
                    val contactKey = contactWallpaperKey(wallpaperDialogThread.address)
                    val hasCustomContactWp = contactWallpapers.containsKey(contactKey)
                    val currentChatWp = contactWallpapers[contactKey] ?: mainWallpaper

                    WallpaperPickerDialog(
                        title = strings.chatWallpaperDialogTitle(wallpaperDialogThread.contactName),
                        currentConfig = currentChatWp,
                        showResetToMainButton = hasCustomContactWp,
                        onSelectPreset = { preset ->
                            val cfg = WallpaperConfig(preset = preset, customImageUri = null)
                            val updated = contactWallpapers.toMutableMap().apply {
                                put(contactKey, cfg)
                            }
                            contactWallpapers = updated
                            saveContactWallpapers(updated)
                        },
                        onPickFromGallery = {
                            wallpaperGalleryTargetKey = contactKey
                            wallpaperPhotoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onResetToMainWallpaper = {
                            val updated = contactWallpapers.toMutableMap().apply {
                                remove(contactKey)
                            }
                            contactWallpapers = updated
                            saveContactWallpapers(updated)
                            showChatWallpaperDialogForThread = null
                        },
                        onDismiss = {
                            showChatWallpaperDialogForThread = null
                        }
                    )
                }

                // Confirmation dialog before moving a message/conversation to Deleted Messages (Trash)
                val pendingDelete = threadPendingDeleteConfirmation
                if (pendingDelete != null) {
                    ConfirmMoveToTrashDialog(
                        contactName = pendingDelete.contactName,
                        retentionDays = autoDeleteDays,
                        onConfirm = {
                            threadPendingDeleteConfirmation = null
                            SmsMmsRepository.moveThreadToTrash(
                                context = this@MainActivity,
                                threadId = pendingDelete.id,
                                retentionDays = autoDeleteDays
                            )
                            val locStrings = TxturaStrings.forLocale()
                            Toast.makeText(
                                this@MainActivity,
                                locStrings.movedToTrashToast(pendingDelete.contactName),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onDismiss = {
                            threadPendingDeleteConfirmation = null
                        }
                    )
                }

                if (showNewMessageDialog) {
                    NewMessageDialog(
                        initialRecipient = prefilledRecipient,
                        initialBody = prefilledBody,
                        onPickContact = ::launchSystemContactPicker,
                        onDismiss = { showNewMessageDialog = false },
                        onSend = { recipient, body ->
                            val createdThreadId = SmsMmsRepository.sendMessage(
                                context = this@MainActivity,
                                recipientAddress = recipient,
                                rawBody = body
                            )
                            showNewMessageDialog = false
                            activeThreadId = createdThreadId
                        }
                    )
                }

                // Prompt user to set Txtura as the Default SMS & MMS app whenever it is not currently default
                if (showDefaultSmsRolePrompt && !isDefaultSms) {
                    DefaultSmsRolePromptDialog(
                        onConfirm = {
                            showDefaultSmsRolePrompt = false
                            requestDefaultSmsRoleSafely()
                        },
                        onDismiss = {
                            showDefaultSmsRolePrompt = false
                        }
                    )
                }

                // Confirmation dialog before restoring conversations and settings from a backup
                val restoreTarget = pendingRestorePayload
                if (restoreTarget != null) {
                    ConfirmRestoreDialog(
                        payload = restoreTarget,
                        onConfirm = {
                            pendingRestorePayload = null
                            applyRestorePayload(restoreTarget)
                        },
                        onDismiss = {
                            pendingRestorePayload = null
                        }
                    )
                }
            }
        }
    }

    /**
     * Every time the user enters Txtura from the launcher or switches back to it,
     * check if Txtura is still the Default SMS & MMS app. If the user changed their
     * default SMS app to another app, prompt them again to set Txtura as default.
     */
    override fun onStart() {
        super.onStart()
        permissionRefreshTick++
        SmsMmsRepository.purgeExpiredDeletedThreads(this, autoDeleteDays)
        val prefs = getSharedPreferences(APP_PREFS, MODE_PRIVATE)
        val firstStartupDone = prefs.getBoolean(KEY_FIRST_STARTUP_DONE, false)
        if (firstStartupDone && !SmsMmsRepository.isDefaultSmsApp(this)) {
            showDefaultSmsRolePrompt = true
        }
    }

    private fun loadSavedAppPreferences() {
        val prefs = getSharedPreferences(APP_PREFS, MODE_PRIVATE)
        val savedThemeName = prefs.getString(KEY_THEME_MODE, TxturaThemeMode.AMOLED.name)
        themeMode = TxturaThemeMode.entries.find { it.name == savedThemeName }
            ?: TxturaThemeMode.AMOLED
        showContactPhotos = prefs.getBoolean(KEY_SHOW_CONTACT_PHOTOS, true)
        val savedLayoutMode = prefs.getString(KEY_MAIN_LIST_LAYOUT_MODE, MainListLayoutMode.DEFAULT.name)
        listLayoutMode = MainListLayoutMode.entries.find { it.name == savedLayoutMode }
            ?: MainListLayoutMode.DEFAULT
        bubbleZoomScale = prefs.getFloat(KEY_BUBBLE_ZOOM_SCALE, 1.0f).coerceIn(0.75f, 1.65f)
        mainWallpaper = WallpaperConfig.deserialize(prefs.getString(KEY_MAIN_WALLPAPER, null))

        val savedContactWpSet = prefs.getStringSet(KEY_CONTACT_WALLPAPERS, emptySet()).orEmpty()
        val parsedMap = mutableMapOf<String, WallpaperConfig>()
        savedContactWpSet.forEach { entry ->
            val sepIdx = entry.indexOf('|')
            if (sepIdx > 0) {
                val key = entry.substring(0, sepIdx)
                val rawVal = entry.substring(sepIdx + 1)
                parsedMap[key] = WallpaperConfig.deserialize(rawVal)
            }
        }
        contactWallpapers = parsedMap

        val savedRight = prefs.getString(KEY_SWIPE_RIGHT_ACTION, SwipeAction.PIN.name)
        swipeRightAction = SwipeAction.entries.find { it.name == savedRight } ?: SwipeAction.PIN

        val savedLeft = prefs.getString(KEY_SWIPE_LEFT_ACTION, SwipeAction.DELETE.name)
        swipeLeftAction = SwipeAction.entries.find { it.name == savedLeft } ?: SwipeAction.DELETE

        autoDeleteDays = prefs.getInt(KEY_AUTO_DELETE_DAYS, 30).coerceIn(1, 365)

        val savedGrouping = prefs.getString(KEY_DELETED_GROUPING_MODE, DeletedGroupingMode.BY_DATE.name)
        deletedGroupingMode = DeletedGroupingMode.entries.find { it.name == savedGrouping }
            ?: DeletedGroupingMode.BY_DATE

        val savedColorMode = prefs.getString(KEY_UI_COLOR_MODE, UiColorMode.COLORFUL.name)
        uiColorMode = UiColorMode.entries.find { it.name == savedColorMode } ?: UiColorMode.COLORFUL

        val savedIconShape = prefs.getString(KEY_LAUNCHER_ICON_SHAPE, LauncherIconShape.ROUNDED_SQUARE.name)
        launcherIconShape = LauncherIconShape.entries.find { it.name == savedIconShape } ?: LauncherIconShape.ROUNDED_SQUARE
    }

    private fun contactWallpaperKey(address: String): String {
        val digits = address.filter { it.isDigit() }
        return if (digits.length >= 6) {
            digits.takeLast(8)
        } else {
            address.trim().lowercase().replace("|", "_")
        }
    }

    private fun saveMainWallpaper(config: WallpaperConfig) {
        getSharedPreferences(APP_PREFS, MODE_PRIVATE).edit {
            putString(KEY_MAIN_WALLPAPER, config.serialize())
        }
    }

    private fun saveContactWallpapers(map: Map<String, WallpaperConfig>) {
        val encoded = map.entries.asSequence().map { (k, v) -> "$k|${v.serialize()}" }.toSet()
        getSharedPreferences(APP_PREFS, MODE_PRIVATE).edit {
            putStringSet(KEY_CONTACT_WALLPAPERS, encoded)
        }
    }

    /**
     * Copies a user-selected Gallery photo to app-private internal storage so that the custom
     * wallpaper persists permanently across app restarts without requiring READ_MEDIA_IMAGES.
     */
    private suspend fun copyPickedWallpaperToInternalStorage(
        sourceUri: Uri,
        targetKey: String
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val safeName = targetKey.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val destFile = File(filesDir, "wallpaper_$safeName.jpg")
            contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            // Append timestamp query parameter so Compose produceState reloads when overwritten
            Uri.fromFile(destFile).buildUpon()
                .appendQueryParameter("t", System.currentTimeMillis().toString())
                .build()
        } catch (_: Exception) {
            sourceUri
        }
    }

    private fun saveSelectedTheme(mode: TxturaThemeMode) {
        getSharedPreferences(APP_PREFS, MODE_PRIVATE).edit {
            putString(KEY_THEME_MODE, mode.name)
        }
    }

    private fun saveShowContactPhotos(enabled: Boolean) {
        getSharedPreferences(APP_PREFS, MODE_PRIVATE).edit {
            putBoolean(KEY_SHOW_CONTACT_PHOTOS, enabled)
        }
    }

    private fun saveListLayoutMode(mode: MainListLayoutMode) {
        getSharedPreferences(APP_PREFS, MODE_PRIVATE).edit {
            putString(KEY_MAIN_LIST_LAYOUT_MODE, mode.name)
        }
    }

    private fun saveBubbleZoomScale(scale: Float) {
        getSharedPreferences(APP_PREFS, MODE_PRIVATE).edit {
            putFloat(KEY_BUBBLE_ZOOM_SCALE, scale.coerceIn(0.75f, 1.65f))
        }
    }

    private fun saveSwipeRightAction(action: SwipeAction) {
        getSharedPreferences(APP_PREFS, MODE_PRIVATE).edit {
            putString(KEY_SWIPE_RIGHT_ACTION, action.name)
        }
    }

    private fun saveSwipeLeftAction(action: SwipeAction) {
        getSharedPreferences(APP_PREFS, MODE_PRIVATE).edit {
            putString(KEY_SWIPE_LEFT_ACTION, action.name)
        }
    }

    private fun saveAutoDeleteDays(days: Int) {
        getSharedPreferences(APP_PREFS, MODE_PRIVATE).edit {
            putInt(KEY_AUTO_DELETE_DAYS, days)
        }
    }

    private fun saveDeletedGroupingMode(mode: DeletedGroupingMode) {
        getSharedPreferences(APP_PREFS, MODE_PRIVATE).edit {
            putString(KEY_DELETED_GROUPING_MODE, mode.name)
        }
    }

    private fun saveUiColorMode(mode: UiColorMode) {
        getSharedPreferences(APP_PREFS, MODE_PRIVATE).edit {
            putString(KEY_UI_COLOR_MODE, mode.name)
        }
    }

    private fun saveLauncherIconShape(shape: LauncherIconShape) {
        getSharedPreferences(APP_PREFS, MODE_PRIVATE).edit {
            putString(KEY_LAUNCHER_ICON_SHAPE, shape.name)
        }
    }

    private fun buildAppSettingsSnapshot(): AppSettingsSnapshot {
        return AppSettingsSnapshot(
            themeMode = themeMode,
            showContactPhotos = showContactPhotos,
            listLayoutMode = listLayoutMode,
            mainWallpaper = mainWallpaper,
            contactWallpapers = contactWallpapers,
            swipeRightAction = swipeRightAction,
            swipeLeftAction = swipeLeftAction,
            autoDeleteDays = autoDeleteDays,
            deletedGroupingMode = deletedGroupingMode,
            bubbleZoomScale = bubbleZoomScale,
            uiColorMode = uiColorMode,
            launcherIconShape = launcherIconShape
        )
    }

    private fun handleCreateLocalBackup() {
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        createBackupLauncher.launch("txtura_backup_$dateStr.json")
    }

    private fun handleRestoreLocalBackup() {
        restoreBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
    }

    private fun handleSaveToGoogleDrive() {
        lifecycleScope.launch {
            val settings = buildAppSettingsSnapshot()
            val (pinnedAddrs, pinnedMsgs) = SmsMmsRepository.getPinnedDataSnapshot()
            val deletedMeta = SmsMmsRepository.getDeletedThreadsMetaSnapshot()
            val currentThreads = SmsMmsRepository.threads.value
            val currentDeleted = SmsMmsRepository.deletedThreads.value

            val json = BackupManager.generateBackupJson(
                settings = settings,
                threads = currentThreads,
                deletedThreads = currentDeleted,
                pinnedAddresses = pinnedAddrs,
                pinnedMessageIds = pinnedMsgs,
                deletedThreadsMeta = deletedMeta
            )
            val file = BackupManager.createInternalBackupFile(this@MainActivity, json)
            lastBackupTimestamp = System.currentTimeMillis()

            if (BackupManager.isGoogleDriveInstalled(this@MainActivity)) {
                try {
                    val intent = BackupManager.createShareOrDriveIntent(
                        this@MainActivity,
                        file,
                        targetGoogleDriveOnly = true
                    )
                    startActivity(intent)
                } catch (_: Exception) {
                    val chooser = Intent.createChooser(
                        BackupManager.createShareOrDriveIntent(
                            this@MainActivity,
                            file,
                            targetGoogleDriveOnly = false
                        ),
                        TxturaStrings.forLocale().saveToGoogleDriveButton
                    )
                    startActivity(chooser)
                }
            } else {
                val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                createBackupLauncher.launch("txtura_backup_$dateStr.json")
            }
        }
    }

    private fun handleRestoreFromGoogleDrive() {
        restoreBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
    }

    private fun handleShareBackupFile() {
        lifecycleScope.launch {
            val settings = buildAppSettingsSnapshot()
            val (pinnedAddrs, pinnedMsgs) = SmsMmsRepository.getPinnedDataSnapshot()
            val deletedMeta = SmsMmsRepository.getDeletedThreadsMetaSnapshot()
            val currentThreads = SmsMmsRepository.threads.value
            val currentDeleted = SmsMmsRepository.deletedThreads.value

            val json = BackupManager.generateBackupJson(
                settings = settings,
                threads = currentThreads,
                deletedThreads = currentDeleted,
                pinnedAddresses = pinnedAddrs,
                pinnedMessageIds = pinnedMsgs,
                deletedThreadsMeta = deletedMeta
            )
            val file = BackupManager.createInternalBackupFile(this@MainActivity, json)
            lastBackupTimestamp = System.currentTimeMillis()

            val chooser = Intent.createChooser(
                BackupManager.createShareOrDriveIntent(
                    this@MainActivity,
                    file,
                    targetGoogleDriveOnly = false
                ),
                TxturaStrings.forLocale().shareBackupButton
            )
            startActivity(chooser)
        }
    }

    private fun handleOpenSystemBackupSettings() {
        try {
            startActivity(BackupManager.createSystemBackupSettingsIntent())
        } catch (_: Exception) {
            openAppDetailsSettings()
        }
    }

    private fun applyRestorePayload(payload: BackupRestorePayload) {
        themeMode = payload.settings.themeMode
        saveSelectedTheme(themeMode)

        showContactPhotos = payload.settings.showContactPhotos
        saveShowContactPhotos(showContactPhotos)

        listLayoutMode = payload.settings.listLayoutMode
        saveListLayoutMode(listLayoutMode)

        mainWallpaper = payload.settings.mainWallpaper
        saveMainWallpaper(mainWallpaper)

        contactWallpapers = payload.settings.contactWallpapers
        saveContactWallpapers(contactWallpapers)

        swipeRightAction = payload.settings.swipeRightAction
        saveSwipeRightAction(swipeRightAction)

        swipeLeftAction = payload.settings.swipeLeftAction
        saveSwipeLeftAction(swipeLeftAction)

        autoDeleteDays = payload.settings.autoDeleteDays
        saveAutoDeleteDays(autoDeleteDays)

        deletedGroupingMode = payload.settings.deletedGroupingMode
        saveDeletedGroupingMode(deletedGroupingMode)

        bubbleZoomScale = payload.settings.bubbleZoomScale
        saveBubbleZoomScale(bubbleZoomScale)

        uiColorMode = payload.settings.uiColorMode
        saveUiColorMode(uiColorMode)

        launcherIconShape = payload.settings.launcherIconShape
        saveLauncherIconShape(launcherIconShape)

        lifecycleScope.launch {
            val (threadCount, msgCount) = SmsMmsRepository.restoreDataFromBackup(this@MainActivity, payload)
            val locStrings = TxturaStrings.forLocale()
            Toast.makeText(
                this@MainActivity,
                locStrings.backupRestoreSuccess(threadCount, msgCount),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * On first startup of the application, automatically requests SMS/MMS/Contacts permissions
     * and prompts to set Txtura as the Default SMS & MMS application.
     */
    private fun checkFirstStartupPermissionsAndRole() {
        val prefs = getSharedPreferences(APP_PREFS, MODE_PRIVATE)
        val alreadyRequested = prefs.getBoolean(KEY_FIRST_STARTUP_DONE, false)
        if (!alreadyRequested) {
            prefs.edit { putBoolean(KEY_FIRST_STARTUP_DONE, true) }
            val allGranted = requiredSmsPermissions.all { perm ->
                ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
            }
            if (!allGranted) {
                smsPermissionLauncher.launch(requiredSmsPermissions)
            } else if (!SmsMmsRepository.isDefaultSmsApp(this)) {
                showDefaultSmsRolePrompt = true
            }
        }
    }

    /**
     * Warm boot protection: update active intent reference and run strict IntentSanitizer validation.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
        handleIncomingSmsIntentSafely(intent)
    }

    private fun handleIncomingSmsIntentSafely(incoming: Intent?) {
        val validated = MessageSecurityAnalyzer.validateIncomingSmsIntent(this, incoming) ?: return
        prefilledRecipient = validated.recipientAddress
        prefilledBody = validated.prefilledBody.orEmpty()
        showNewMessageDialog = true
    }

    /**
     * Opens the default Phone/Dialer app to call the message sender directly.
     */
    private fun dialPhoneNumberSafely(rawAddress: String) {
        val strings = TxturaStrings.forLocale()
        val cleanPhone = MessageSecurityAnalyzer.sanitizePhoneNumber(rawAddress)
            .ifBlank { rawAddress.trim() }
        try {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.fromParts("tel", cleanPhone, null)
            }
            startActivity(dialIntent)
        } catch (_: Exception) {
            Toast.makeText(this, strings.dialerErrorToast, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Opens the sender's contact profile in the default Contacts app, or opens the
     * Add-to-Contacts screen if the sender is not yet saved in Contacts.
     */
    private fun openOrInsertContactSafely(phoneNumber: String, displayName: String) {
        val strings = TxturaStrings.forLocale()
        val cleanPhone = MessageSecurityAnalyzer.sanitizePhoneNumber(phoneNumber)
            .ifBlank { phoneNumber.trim() }
        try {
            val lookupUri = SmsMmsRepository.resolveContactLookupUriSafely(this, cleanPhone)
            if (lookupUri != null) {
                val viewContactIntent = Intent(Intent.ACTION_VIEW, lookupUri)
                startActivity(viewContactIntent)
            } else {
                val insertOrEditIntent = Intent(Intent.ACTION_INSERT_OR_EDIT).apply {
                    type = ContactsContract.Contacts.CONTENT_ITEM_TYPE
                    putExtra(ContactsContract.Intents.Insert.PHONE, cleanPhone)
                    if (!displayName.equals(cleanPhone, ignoreCase = true)) {
                        putExtra(ContactsContract.Intents.Insert.NAME, displayName)
                    }
                }
                startActivity(insertOrEditIntent)
            }
        } catch (_: Exception) {
            Toast.makeText(this, strings.contactsErrorToast, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Launches the system Contacts picker so the user can pick a recipient phone number.
     */
    private fun launchSystemContactPicker() {
        val strings = TxturaStrings.forLocale()
        try {
            val pickIntent = Intent(
                Intent.ACTION_PICK,
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            )
            contactPhonePickerLauncher.launch(pickIntent)
        } catch (_: Exception) {
            Toast.makeText(this, strings.contactsErrorToast, Toast.LENGTH_SHORT).show()
        }
    }

    @Composable
    private fun rememberDynamicSmsPermissionState(tick: Int): Boolean {
        val read = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        val send = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
        return (tick >= 0) && (read && send)
    }

    @Composable
    private fun rememberDynamicDefaultSmsState(tick: Int): Boolean {
        return (tick >= 0) && SmsMmsRepository.isDefaultSmsApp(this)
    }

    /**
     * 3-State Runtime Permission Flow:
     * 1. Check dynamic grant state via ContextCompat.checkSelfPermission
     * 2. Check shouldShowRequestPermissionRationale to show educational explanation
     * 3. Launch ActivityResultContracts.RequestMultiplePermissions
     */
    private fun requestSmsPermissionsSafely() {
        val strings = TxturaStrings.forLocale()
        val allGranted = requiredSmsPermissions.all { perm ->
            ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
        }
        when {
            allGranted -> {
                rationaleBannerText = null
                permissionRefreshTick++
                lifecycleScope.launch {
                    SmsMmsRepository.syncDeviceMessages(this@MainActivity)
                }
            }
            requiredSmsPermissions.any { shouldShowRequestPermissionRationale(it) } -> {
                rationaleBannerText = strings.rationaleLocalOnly
                smsPermissionLauncher.launch(requiredSmsPermissions)
            }
            else -> {
                smsPermissionLauncher.launch(requiredSmsPermissions)
            }
        }
    }

    private fun requestDefaultSmsRoleSafely() {
        val strings = TxturaStrings.forLocale()
        try {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                if (!roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                    val roleIntent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                    defaultSmsRoleLauncher.launch(roleIntent)
                }
            } else {
                val fallbackIntent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                    putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
                }
                defaultSmsRoleLauncher.launch(fallbackIntent)
            }
        } catch (_: Exception) {
            Toast.makeText(
                this,
                strings.setDefaultSmsInSystemToast,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun openAppDetailsSettings() {
        val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(settingsIntent)
    }

    /**
     * Copies 2FA/OTP verification code with EXTRA_IS_SENSITIVE flag so Android 13+ hides it
     * from clipboard overlay previews.
     */
    private fun copyOtpConfidentially(otpCode: String) {
        val strings = TxturaStrings.forLocale()
        val clipboard = getSystemService(ClipboardManager::class.java) ?: return
        val clip = ClipData.newPlainText("OTP", otpCode)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
        @Suppress("UsePropertyAccessSyntax")
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, strings.codeCopiedToast(otpCode), Toast.LENGTH_SHORT).show()
    }
}

@Preview(showBackground = true, name = "Txtura AMOLED Inbox (English Default)")
@Composable
fun TxturaAmoledPreview() {
    val threads by SmsMmsRepository.threads.collectAsState()
    TxturaTheme(themeMode = TxturaThemeMode.AMOLED) {
        ConversationsListScreen(
            threads = threads,
            onOpenThread = {},
            onTogglePin = {},
            onRequestDeleteThread = {},
            onCallSender = {},
            onOpenContact = { _, _ -> },
            onCopyOtp = {},
            onOpenNewMessage = {},
            onOpenSettings = {}
        )
    }
}

@Preview(showBackground = true, locale = "sr", name = "Txtura AMOLED Inbox (Serbian Locale)")
@Composable
fun TxturaSerbianPreview() {
    val threads by SmsMmsRepository.threads.collectAsState()
    TxturaTheme(themeMode = TxturaThemeMode.AMOLED) {
        ConversationsListScreen(
            threads = threads,
            onOpenThread = {},
            onTogglePin = {},
            onRequestDeleteThread = {},
            onCallSender = {},
            onOpenContact = { _, _ -> },
            onCopyOtp = {},
            onOpenNewMessage = {},
            onOpenSettings = {}
        )
    }
}
