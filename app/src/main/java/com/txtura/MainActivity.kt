@file:Suppress("SpellCheckingInspection", "DestructuringDeclaration", "TrailingComma")

package com.txtura

import android.Manifest
import android.app.KeyguardManager
import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricPrompt
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.PersistableBundle
import android.provider.ContactsContract
import android.provider.Settings
import android.provider.Telephony
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.txtura.data.AppSettingsSnapshot
import com.txtura.data.AppUpdateManager
import com.txtura.data.BackupManager
import com.txtura.data.BackupRestorePayload
import com.txtura.data.ContactPickerItem
import com.txtura.data.ConversationFilter
import com.txtura.data.ConversationThread
import com.txtura.data.DeletedGroupingMode
import com.txtura.data.DockBarItem
import com.txtura.data.MainListLayoutMode
import com.txtura.data.SmsMmsRepository
import com.txtura.data.SmsNotificationSound
import com.txtura.data.SwipeAction
import com.txtura.telephony.EmergencyAlertManager
import com.txtura.data.UiColorMode
import com.txtura.data.WallpaperConfig
import com.txtura.data.WallpaperPreset
import com.txtura.data.WallpaperTexture
import com.txtura.security.AppSecurityManager
import com.txtura.security.DeviceKeyMismatchException
import com.txtura.security.MessageSecurityAnalyzer
import com.txtura.security.SecurityCryptoManager
import com.txtura.telephony.FlashNotificationManager
import com.txtura.telephony.MmsImageCompressor
import com.txtura.ui.components.GlassBackground
import com.txtura.ui.i18n.TxturaStrings
import com.txtura.ui.screens.AppLockScreen
import com.txtura.ui.screens.ConfirmMoveToTrashDialog
import com.txtura.ui.screens.ConfirmRestoreDialog
import com.txtura.ui.screens.ConversationDetailScreen
import com.txtura.ui.screens.ConversationsListScreen
import com.txtura.ui.screens.CreateEncryptedBackupDialog
import com.txtura.ui.screens.DefaultSmsRolePromptDialog
import com.txtura.ui.screens.DeletedMessagesScreen
import com.txtura.ui.screens.HighlightedMessagesScreen
import com.txtura.ui.screens.NewMessageDialog
import com.txtura.ui.screens.RestoreEncryptedBackupPasswordDialog
import com.txtura.ui.screens.SettingsScreen
import com.txtura.ui.screens.WallpaperPickerDialog
import com.txtura.ui.theme.TxturaHaptics
import com.txtura.ui.theme.TxturaMotion
import com.txtura.ui.theme.TxturaTheme
import com.txtura.ui.theme.TxturaThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.minutes

private enum class MainAppScreen {
    DELETED_MESSAGES,
    HIGHLIGHTED_MESSAGES,
    SETTINGS,
    CONVERSATION_DETAIL,
    CONVERSATIONS_LIST
}

private fun getScreenOrder(screen: MainAppScreen, dockOrder: List<DockBarItem>): Int {
    return when (screen) {
        MainAppScreen.CONVERSATIONS_LIST -> 0
        MainAppScreen.HIGHLIGHTED_MESSAGES -> {
            val idx = dockOrder.indexOf(DockBarItem.HIGHLIGHTED)
            if (idx >= 0) idx + 1 else 2
        }
        MainAppScreen.DELETED_MESSAGES -> {
            val idx = dockOrder.indexOf(DockBarItem.DELETED)
            if (idx >= 0) idx + 1 else 4
        }
        MainAppScreen.SETTINGS -> {
            val idx = dockOrder.indexOf(DockBarItem.SETTINGS)
            if (idx >= 0) idx + 1 else 5
        }
        MainAppScreen.CONVERSATION_DETAIL -> 100
    }
}

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
        private const val KEY_AUTO_DELETE_DAYS = "auto_delete_days_v2"
        private const val KEY_DELETED_GROUPING_MODE = "deleted_grouping_mode"
        private const val KEY_MAIN_WALLPAPER = "main_window_wallpaper"
        private const val KEY_CONTACT_WALLPAPERS = "contact_chat_wallpapers_v1"
        private const val KEY_UI_COLOR_MODE = "ui_color_mode"
        private const val KEY_UI_OPACITY = "ui_opacity_v3"
        private const val KEY_CATEGORY_ORDER = "category_order_v1"
        private const val KEY_DOCK_BAR_ORDER = "dock_bar_order_v1"
        private const val KEY_RECALL_MESSAGES_ENABLED = "recall_messages_enabled"
        private const val KEY_IDENTIFY_MALICIOUS_WEBSITES = "identify_malicious_websites"
        private const val KEY_IN_APP_SOUNDS_ENABLED = "in_app_sounds_enabled"
        private const val KEY_PINCH_TO_ZOOM_ENABLED = "pinch_to_zoom_enabled"
        private const val KEY_EXPRESSIVE_ANIMATIONS_ENABLED = "expressive_animations_enabled"
        private const val KEY_FLASH_ALERT_ENABLED = "flash_alert_enabled"
        private const val KEY_HAPTICS_ENABLED = "haptics_enabled"
        private const val KEY_BUBBLES_ENABLED = "bubbles_enabled"
        private const val KEY_AUTO_PREVIEWS_SHOW_ALL = "auto_previews_show_all"
        private const val KEY_AUTO_PREVIEWS_WEB_LINKS_ONLY = "auto_previews_web_links_only"
        private const val KEY_AUTO_PREVIEWS_WIFI_ONLY = "auto_previews_wifi_only"
        private const val GALLERY_TARGET_MAIN = "__MAIN__"
    }

    private var themeMode by mutableStateOf(TxturaThemeMode.SYSTEM)
    private var showContactPhotos by mutableStateOf(value = false)
    private var listLayoutMode by mutableStateOf(MainListLayoutMode.DEFAULT)
    private var bubbleZoomScale by mutableFloatStateOf(1.0f)
    private var mainWallpaper by mutableStateOf(WallpaperConfig(preset = WallpaperPreset.STARLIGHT_GALAXY))
    private var contactWallpapers by mutableStateOf<Map<String, WallpaperConfig>>(emptyMap())
    private var uiColorMode by mutableStateOf(UiColorMode.COLORFUL)
    private var uiOpacity by mutableFloatStateOf(0.50f)
    private var categoryOrder by mutableStateOf(ConversationFilter.defaultOrder)
    private var dockBarOrder by mutableStateOf(DockBarItem.defaultOrder)
    private var swipeRightAction by mutableStateOf(SwipeAction.MARK_READ)
    private var swipeLeftAction by mutableStateOf(SwipeAction.DELETE)
    private var autoDeleteDays by mutableIntStateOf(30)
    private var deletedGroupingMode by mutableStateOf(DeletedGroupingMode.BY_DATE)
    private var recallMessagesEnabled by mutableStateOf(value = true)
    private var identifyMaliciousWebsites by mutableStateOf(value = true)
    private var inAppSoundsEnabled by mutableStateOf(value = true)
    private var smsNotificationSound by mutableStateOf(SmsNotificationSound.DUPLI_PULS)
    private var pinchToZoomEnabled by mutableStateOf(value = true)
    private var expressiveAnimationsEnabled by mutableStateOf(value = true)
    private var flashAlertEnabled by mutableStateOf(value = true)
    private var hapticsEnabled by mutableStateOf(value = true)
    private var bubblesEnabled by mutableStateOf(value = false)
    private var autoPreviewsShowAll by mutableStateOf(value = true)
    private var autoPreviewsWebLinksOnly by mutableStateOf(value = false)
    private var autoPreviewsWifiOnly by mutableStateOf(value = false)

    private var lastBackupTimestamp by mutableLongStateOf(0L)
    private var pendingRestorePayload by mutableStateOf<BackupRestorePayload?>(null)
    private var pendingBackupPassword by mutableStateOf<String?>(null)
    private var showCreateBackupDialog by mutableStateOf(false)
    private var pendingEncryptedBackupContent by mutableStateOf<String?>(null)
    private var showRestorePasswordPrompt by mutableStateOf(false)

    private var activeThreadId by mutableStateOf<Long?>(null)
    private var activeThreadAddress by mutableStateOf<String?>(null)
    private var showSettingsScreen by mutableStateOf(value = false)
    private var focusUpdateSectionOnSettings by mutableStateOf(value = false)
    private var showDeletedMessagesScreen by mutableStateOf(value = false)
    private var showHighlightedMessagesScreen by mutableStateOf(value = false)
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
    private var isAppLocked by mutableStateOf(false)
    private var startupPreferencesReady by mutableStateOf(false)
    private var initialMessageLoadFinished by mutableStateOf(false)
    private var startupInitializationFinished = false
    private var pendingStartupIntent: Intent? = null
    private var firstStartupDone = false
    private var biometricLockEnabled = false
    private var deviceLockConfigured = false
    private var biometricLockTimeoutMinutes = 0
    private var isAuthenticating by mutableStateOf(false)
    private var lastPauseTimestamp by mutableLongStateOf(0L)
    private var lastUnlockTimestamp by mutableLongStateOf(0L)
    private var biometricCancellationSignal: CancellationSignal? = null

    private fun onAuthenticationSuccess() {
        isAuthenticating = false
        isAppLocked = false
        lastUnlockTimestamp = System.currentTimeMillis()
        lastPauseTimestamp = System.currentTimeMillis()
    }

    private val requiredSmsPermissions: Array<String>
        get() = buildList {
            add(Manifest.permission.READ_SMS)
            add(Manifest.permission.SEND_SMS)
            add(Manifest.permission.RECEIVE_SMS)
            add(Manifest.permission.RECEIVE_MMS)
            add(Manifest.permission.READ_CONTACTS)
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.READ_CALENDAR)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()

    // Keyguard / Biometric / Device PIN Authentication Launcher
    private val confirmCredentialsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                onAuthenticationSuccess()
            } else {
                isAuthenticating = false
            }
        }

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

    private val microphonePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            permissionRefreshTick++
            val strings = TxturaStrings.forLocale()
            if (granted) {
                Toast.makeText(
                    this,
                    if (strings.isSerbian) "Dozvola za mikrofon je odobrena." else "Microphone permission granted.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val permanentlyDenied = !shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)
                if (permanentlyDenied) {
                    Toast.makeText(this, strings.voiceMessagePermissionRequired, Toast.LENGTH_LONG).show()
                    openAppDetailsSettings()
                } else {
                    Toast.makeText(this, strings.voiceMessagePermissionRequired, Toast.LENGTH_LONG).show()
                }
            }
        }

    private val contactsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            permissionRefreshTick++
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            permissionRefreshTick++
        }

    // Zero-Permission Photo Picker for MMS attachments (avoids READ_MEDIA_IMAGES / READ_EXTERNAL_STORAGE)
    // Strips EXIF GPS coordinates and camera metadata when Zero Metadata Minimization is enabled,
    // and automatically compresses images > 299.9 kB to fit MMS constraints while preserving true photo colors
    private val photoPickerLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val safeUri = AppSecurityManager.stripExifMetadata(this@MainActivity, uri)
                    val compressedUri = MmsImageCompressor.compressImageIfNeeded(this@MainActivity, safeUri)
                    withContext(Dispatchers.Main) {
                        pendingMmsUri = compressedUri
                    }
                }
            }
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
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ),
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val rawNumber = cursor.getString(0).orEmpty()
                        val displayName = cursor.getString(1)
                        val cleanNumber = MessageSecurityAnalyzer.sanitizePhoneNumber(rawNumber).ifBlank { rawNumber.trim() }
                        if (cleanNumber.isNotBlank()) {
                            prefilledRecipient = cleanNumber
                            val threadId = SmsMmsRepository.getOrCreateConversationWithRecipient(
                                context = this@MainActivity,
                                recipientAddress = cleanNumber,
                                preferredName = displayName,
                            )
                            showNewMessageDialog = false
                            activeThreadAddress = cleanNumber
                            activeThreadId = threadId
                        }
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
                    val settings = withContext(Dispatchers.IO) { buildAppSettingsSnapshot() }
                    val (pinnedAddrs, pinnedMsgs) = SmsMmsRepository.getPinnedDataSnapshot()
                    val highlightedMsgs = SmsMmsRepository.getHighlightedMessageIdsSnapshot()
                    val blockedAddrs = SmsMmsRepository.getBlockedAddressesSnapshot()
                    val deletedMeta = SmsMmsRepository.getDeletedThreadsMetaSnapshot()
                    SmsMmsRepository.awaitInitialization()
                    SmsMmsRepository.loadAllCachedThreadHistories(this@MainActivity)
                    val currentThreads = SmsMmsRepository.threads.value
                    val currentDeleted = SmsMmsRepository.deletedThreads.value

                    val json = withContext(Dispatchers.IO) {
                        BackupManager.generateBackupJson(
                            settings = settings,
                            threads = currentThreads,
                            deletedThreads = currentDeleted,
                            pinnedAddresses = pinnedAddrs,
                            pinnedMessageIds = pinnedMsgs,
                            highlightedMessageIds = highlightedMsgs,
                            blockedAddresses = blockedAddrs,
                            deletedThreadsMeta = deletedMeta,
                        )
                    }
                    val pwd = pendingBackupPassword
                    val success = withContext(Dispatchers.IO) {
                        BackupManager.saveBackupToUri(this@MainActivity, uri, json, pwd)
                    }
                    if (success) {
                        withContext(Dispatchers.IO) {
                            BackupManager.createInternalBackupFile(this@MainActivity, json, pwd)
                        }
                        pendingBackupPassword = null
                        lastBackupTimestamp = System.currentTimeMillis()
                        val locStrings = TxturaStrings.forLocale()
                        val totalMsgs = currentThreads.sumOf { (_, _, _, _, _, _, messages) -> messages.size } +
                            currentDeleted.sumOf { (thread, _) -> thread.messages.size }
                        Toast.makeText(
                            this@MainActivity,
                            locStrings.backupCreatedSuccess(currentThreads.size + currentDeleted.size, totalMsgs),
                            Toast.LENGTH_LONG,
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
                        val (rawContent, needsPassword) = withContext(Dispatchers.IO) {
                            val contents = BackupManager.readBackupFromUri(this@MainActivity, uri)
                            contents to (
                                SecurityCryptoManager.isEncryptedBackup(contents) &&
                                    SecurityCryptoManager.isPasswordProtectedBackup(contents)
                                )
                        }
                        if (needsPassword) {
                            pendingEncryptedBackupContent = rawContent
                            showRestorePasswordPrompt = true
                        } else {
                            val payload = withContext(Dispatchers.IO) {
                                BackupManager.parseBackupJson(rawContent)
                            }
                            pendingRestorePayload = payload
                        }
                    } catch (e: DeviceKeyMismatchException) {
                        android.util.Log.e("MainActivity", "Device key mismatch", e)
                        val locStrings = TxturaStrings.forLocale()
                        Toast.makeText(
                            this@MainActivity,
                            locStrings.backupDeviceKeyMismatch,
                            Toast.LENGTH_LONG,
                        ).show()
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Failed to read backup from $uri", e)
                        val locStrings = TxturaStrings.forLocale()
                        Toast.makeText(
                            this@MainActivity,
                            locStrings.backupErrorInvalid,
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            }
        }

    private data class StartupSettings(
        val appPreferences: Map<String, *>,
        val legacyFlashAlertEnabled: Boolean,
        val notificationSound: SmsNotificationSound,
        val lastBackupTimestamp: Long,
        val biometricLockEnabled: Boolean,
        val deviceLockConfigured: Boolean,
        val biometricLockTimeoutMinutes: Int
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SmsMmsRepository.initializePersistedState(this)
        EmergencyAlertManager.initialize(this)
        enableEdgeToEdge()
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)

        setContent {
            if (!startupPreferencesReady) {
                Box(modifier = Modifier.fillMaxSize())
            } else {
                val threads by SmsMmsRepository.threads.collectAsState()
            val deletedThreads by SmsMmsRepository.deletedThreads.collectAsState()
            val blockedSenders by SmsMmsRepository.blockedSenders.collectAsState()
            val scheduledMessages by SmsMmsRepository.scheduledMessages.collectAsState()

            // Query permissions dynamically (never cache permission state in fields)
            val hasSmsPerm = rememberDynamicSmsPermissionState(permissionRefreshTick)
            val hasMicPerm = rememberDynamicMicrophonePermissionState(permissionRefreshTick)
            val hasContactsPerm = rememberDynamicContactsPermissionState(permissionRefreshTick)
            val hasNotifPerm = rememberDynamicNotificationPermissionState(permissionRefreshTick)
            val isDefaultSms = rememberDynamicDefaultSmsState(permissionRefreshTick)

            var availableContacts by remember { mutableStateOf<List<ContactPickerItem>>(emptyList()) }

            LaunchedEffect(showDeletedMessagesScreen, showHighlightedMessagesScreen) {
                if (showDeletedMessagesScreen || showHighlightedMessagesScreen) {
                    SmsMmsRepository.awaitInitialization()
                    SmsMmsRepository.loadAllCachedThreadHistories(this@MainActivity)
                }
            }

            LaunchedEffect(hasSmsPerm, isDefaultSms, autoDeleteDays) {
                launch {
                    availableContacts = SmsMmsRepository.loadAvailableContactsForPicker(this@MainActivity)
                }
                try {
                    SmsMmsRepository.awaitInitialization()
                    SmsMmsRepository.purgeExpiredDeletedThreads(this@MainActivity, autoDeleteDays)
                    if (hasSmsPerm || isDefaultSms) {
                        SmsMmsRepository.syncDeviceMessagesIfStale(this@MainActivity, autoDeleteDays)
                    }
                } finally {
                    initialMessageLoadFinished = true
                }
            }

            // Real-time GitHub Update Checker: checks every 6 hours when Wi-Fi or mobile data is connected
            val currentAppVersionName = remember {
                runCatching {
                    packageManager.getPackageInfo(packageName, 0).versionName ?: "3.3.1"
                }.getOrDefault("3.3.1")
            }
            LaunchedEffect(Unit) {
                while (true) {
                    AppUpdateManager.checkAndNotifyIfUpdateAvailable(this@MainActivity, currentAppVersionName)
                    delay(30.minutes)
                }
            }
            DisposableEffect(Unit) {
                val cm = getSystemService(ConnectivityManager::class.java)
                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        lifecycleScope.launch {
                            AppUpdateManager.checkAndNotifyIfUpdateAvailable(this@MainActivity, currentAppVersionName)
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
                            lifecycleScope.launch {
                                AppUpdateManager.checkAndNotifyIfUpdateAvailable(this@MainActivity, currentAppVersionName)
                            }
                        }
                    }
                }
                try {
                    cm?.registerDefaultNetworkCallback(callback)
                } catch (_: Exception) {
                    // Ignore on restricted environments
                }
                onDispose {
                    try {
                        cm?.unregisterNetworkCallback(callback)
                    } catch (_: Exception) {
                        // Ignore
                    }
                }
            }

            val selectedThread = threads.find { (id) -> id == activeThreadId }
                ?: activeThreadAddress?.takeIf { it.isNotBlank() }?.let { addr ->
                    SmsMmsRepository.findThreadByAddress(threads, addr)
                }

            LaunchedEffect(selectedThread?.id, selectedThread?.isHistoryLoaded) {
                if (selectedThread != null && !selectedThread.isHistoryLoaded) {
                    SmsMmsRepository.loadCachedThreadHistory(this@MainActivity, selectedThread.id)
                }
            }

            LaunchedEffect(selectedThread?.id, activeThreadAddress) {
                SmsMmsRepository.setActiveForegroundThread(selectedThread?.id)
                if (selectedThread != null) {
                    if (activeThreadId != selectedThread.id) {
                        activeThreadId = selectedThread.id
                    }
                    if (activeThreadAddress.isNullOrBlank()) {
                        activeThreadAddress = selectedThread.address
                    }
                    SmsMmsRepository.markThreadRead(this@MainActivity, selectedThread.id, playSound = false)
                }
            }

            LaunchedEffect(threads, activeThreadAddress) {
                val targetAddr = activeThreadAddress
                if (!targetAddr.isNullOrBlank()) {
                    val realThread = SmsMmsRepository.findThreadByAddress(threads, targetAddr)
                    if (realThread != null && activeThreadId != realThread.id) {
                        activeThreadId = realThread.id
                    }
                }
            }

            var previousMessageCount by remember(selectedThread?.id) {
                mutableIntStateOf(selectedThread?.messages?.size ?: 0)
            }
            LaunchedEffect(selectedThread?.id, selectedThread?.messages?.size) {
                val currentCount = selectedThread?.messages?.size ?: 0
                val isHistoryFullyLoaded = selectedThread?.isHistoryLoaded == true
                if (selectedThread != null && isHistoryFullyLoaded && currentCount > previousMessageCount && previousMessageCount > 1) {
                    val lastMsg = selectedThread.messages.lastOrNull()
                    val isRealTimeIncoming = lastMsg != null && lastMsg.isIncoming &&
                        (System.currentTimeMillis() - lastMsg.timestampMillis < 10_000L)
                    if (isRealTimeIncoming && inAppSoundsEnabled) {
                        SmsMmsRepository.playInAppMessageReceivedSound(this@MainActivity)
                    }
                }
                previousMessageCount = currentCount
            }

            var lastActiveThread by remember { mutableStateOf<ConversationThread?>(null) }
            if (selectedThread != null) {
                lastActiveThread = selectedThread
            }

            val activeWallpaper = if ((selectedThread != null) && !showSettingsScreen && !showDeletedMessagesScreen && !showHighlightedMessagesScreen) {
                contactWallpapers[contactWallpaperKey(selectedThread.address)] ?: mainWallpaper
            } else {
                mainWallpaper
            }

            TxturaTheme(
                themeMode = themeMode,
                wallpaperConfig = activeWallpaper,
                uiOpacity = uiOpacity,
                uiColorMode = uiColorMode,
                expressiveAnimations = expressiveAnimationsEnabled,
            ) {
                val strings = TxturaStrings.current()

                if (isAppLocked) {
                    BackHandler {
                        finishAffinity()
                    }
                    LaunchedEffect(isAppLocked) {
                        promptBiometricUnlock()
                    }
                    AppLockScreen(
                        mainWallpaper = mainWallpaper,
                        onUnlock = ::promptBiometricUnlock
                    )
                } else {
                    val currentScreen = when {
                        showDeletedMessagesScreen -> MainAppScreen.DELETED_MESSAGES
                        showHighlightedMessagesScreen -> MainAppScreen.HIGHLIGHTED_MESSAGES
                        showSettingsScreen -> MainAppScreen.SETTINGS
                        selectedThread != null -> MainAppScreen.CONVERSATION_DETAIL
                        else -> MainAppScreen.CONVERSATIONS_LIST
                    }

                    var isConversationsSelectionMode by remember { mutableStateOf(false) }

                    DisposableEffect(currentScreen == MainAppScreen.CONVERSATION_DETAIL) {
                        val isDetail = currentScreen == MainAppScreen.CONVERSATION_DETAIL
                        if (isDetail) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                        onDispose {
                            if (isDetail) {
                                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            }
                        }
                    }

                    val currentWallpaper = if (currentScreen == MainAppScreen.CONVERSATION_DETAIL) {
                        val activeDetailThread = selectedThread ?: lastActiveThread
                        if (activeDetailThread != null) {
                            val contactKey = contactWallpaperKey(activeDetailThread.address)
                            contactWallpapers[contactKey] ?: mainWallpaper
                        } else mainWallpaper
                    } else {
                        mainWallpaper
                    }

                    GlassBackground(
                        modifier = Modifier.fillMaxSize(),
                        wallpaperConfig = currentWallpaper
                    ) {
                                AnimatedContent(
                                    targetState = currentScreen,
                                    modifier = Modifier.fillMaxSize(),
                                    transitionSpec = {
                                        val forward = getScreenOrder(targetState, dockBarOrder) >= getScreenOrder(initialState, dockBarOrder)
                                        TxturaMotion.screenTransition(
                                            forward = forward,
                                            isExpressive = expressiveAnimationsEnabled
                                        )
                                    },
                                    label = "mainScreenTransition"
                                ) { screen ->
                    when (screen) {
                        MainAppScreen.DELETED_MESSAGES -> {
                        BackHandler {
                            showDeletedMessagesScreen = false
                        }
                        val totalHighlightedCount = remember(threads) {
                            SmsMmsRepository.getHighlightedMessageIdsSnapshot().size
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
                                    threadId,
                                )
                                if (restoredName != null) {
                                    val locStrings = TxturaStrings.forLocale()
                                    Toast.makeText(
                                        this@MainActivity,
                                        locStrings.restoredToast(restoredName),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            },
                            onRestoreMessages = { threadId, messageIds ->
                                val restoredName = SmsMmsRepository.restoreDeletedMessages(
                                    this@MainActivity,
                                    threadId,
                                    messageIds,
                                )
                                if (restoredName != null) {
                                    val locStrings = TxturaStrings.forLocale()
                                    Toast.makeText(
                                        this@MainActivity,
                                        locStrings.restoredToast(restoredName),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            },
                            onPermanentlyDeleteThread = { threadId ->
                                SmsMmsRepository.permanentlyDeleteThread(this@MainActivity, threadId)
                            },
                            onPermanentlyDeleteMessages = { threadId, messageIds ->
                                SmsMmsRepository.permanentlyDeleteMessages(
                                    this@MainActivity,
                                    threadId,
                                    messageIds,
                                )
                            },
                            highlightedCount = totalHighlightedCount,
                            dockBarOrder = dockBarOrder,
                            onUpdateDockBarOrder = { newOrder ->
                                dockBarOrder = newOrder
                                saveDockBarOrder(newOrder)
                            },
                            onOpenHighlightedMessages = {
                                showDeletedMessagesScreen = false
                                showHighlightedMessagesScreen = true
                            },
                            onOpenNewMessage = {
                                showDeletedMessagesScreen = false
                                prefilledRecipient = ""
                                prefilledBody = ""
                                showNewMessageDialog = true
                            },
                            onMarkAllRead = {
                                SmsMmsRepository.markAllThreadsRead(this@MainActivity)
                                val locStrings = TxturaStrings.forLocale()
                                Toast.makeText(
                                    this@MainActivity,
                                    locStrings.allMessagesMarkedReadToast,
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                            onOpenSettings = {
                                showDeletedMessagesScreen = false
                                showSettingsScreen = true
                            },
                            onEmptyTrash = {
                                SmsMmsRepository.emptyTrash(this@MainActivity)
                            }
                        )
                    }

                    MainAppScreen.HIGHLIGHTED_MESSAGES -> {
                        BackHandler {
                            showHighlightedMessagesScreen = false
                        }
                        HighlightedMessagesScreen(
                            threads = threads,
                            mainWallpaper = mainWallpaper,
                            onBack = { showHighlightedMessagesScreen = false },
                            onOpenThread = { threadId ->
                                showHighlightedMessagesScreen = false
                                activeThreadId = threadId
                                activeThreadAddress = threads.find { (tId) -> tId == threadId }?.address
                                SmsMmsRepository.markThreadRead(this@MainActivity, threadId)
                            },
                            onToggleHighlight = { threadId, messageId ->
                                SmsMmsRepository.toggleHighlightMessage(this@MainActivity, threadId, messageId)
                            },
                            deletedCount = deletedThreads.size,
                            dockBarOrder = dockBarOrder,
                            onUpdateDockBarOrder = { newOrder ->
                                dockBarOrder = newOrder
                                saveDockBarOrder(newOrder)
                            },
                            onOpenDeletedMessages = {
                                showHighlightedMessagesScreen = false
                                showDeletedMessagesScreen = true
                            },
                            onOpenNewMessage = {
                                showHighlightedMessagesScreen = false
                                prefilledRecipient = ""
                                prefilledBody = ""
                                showNewMessageDialog = true
                            },
                            onMarkAllRead = {
                                SmsMmsRepository.markAllThreadsRead(this@MainActivity)
                                val locStrings = TxturaStrings.forLocale()
                                Toast.makeText(
                                    this@MainActivity,
                                    locStrings.allMessagesMarkedReadToast,
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                            onOpenSettings = {
                                showHighlightedMessagesScreen = false
                                showSettingsScreen = true
                            }
                        )
                    }

                    MainAppScreen.SETTINGS -> {
                        BackHandler {
                            showSettingsScreen = false
                        }
                        SettingsScreen(
                            focusUpdateSection = focusUpdateSectionOnSettings,
                            onUpdateSectionFocused = { focusUpdateSectionOnSettings = false },
                            themeMode = themeMode,
                            showContactPhotos = showContactPhotos,
                            listLayoutMode = listLayoutMode,
                            mainWallpaper = mainWallpaper,
                            uiColorMode = uiColorMode,
                            uiOpacity = uiOpacity,
                            onUpdateUiOpacity = { newOpacity ->
                                uiOpacity = newOpacity.coerceIn(0.0f, 1.0f)
                                saveUiOpacity(uiOpacity)
                            },
                            blockedSenders = blockedSenders,
                            onUnblockSender = { address ->
                                SmsMmsRepository.unblockSender(this@MainActivity, address)
                                val locStrings = TxturaStrings.forLocale()
                                Toast.makeText(
                                    this@MainActivity,
                                    locStrings.senderUnblockedToast(address),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                            swipeRightAction = swipeRightAction,
                            swipeLeftAction = swipeLeftAction,
                            autoDeleteDays = autoDeleteDays,
                            deletedGroupingMode = deletedGroupingMode,
                            deletedMessagesCount = deletedThreads.size,
                            recallMessagesEnabled = recallMessagesEnabled,
                            identifyMaliciousWebsites = identifyMaliciousWebsites,
                            inAppSoundsEnabled = inAppSoundsEnabled,
                            onToggleInAppSounds = { enabled ->
                                inAppSoundsEnabled = enabled
                                saveInAppSoundsEnabled(enabled)
                            },
                            pinchToZoomEnabled = pinchToZoomEnabled,
                            onTogglePinchToZoom = { enabled ->
                                pinchToZoomEnabled = enabled
                                savePinchToZoomEnabled(enabled)
                            },
                            expressiveAnimationsEnabled = expressiveAnimationsEnabled,
                            onToggleExpressiveAnimations = { enabled ->
                                expressiveAnimationsEnabled = enabled
                                saveExpressiveAnimationsEnabled(enabled)
                            },
                            flashAlertEnabled = flashAlertEnabled,
                            onToggleFlashAlert = { enabled ->
                                flashAlertEnabled = enabled
                                saveFlashAlertEnabled(enabled)
                            },
                            hapticsEnabled = hapticsEnabled,
                            onToggleHaptics = { enabled ->
                                hapticsEnabled = enabled
                                saveHapticsEnabled(enabled)
                            },
                            smsNotificationSound = smsNotificationSound,
                            onSelectSmsNotificationSound = { sound ->
                                smsNotificationSound = sound
                                SmsMmsRepository.setSmsNotificationSound(this@MainActivity, sound)
                                SmsMmsRepository.playSmsNotificationSoundPreview(this@MainActivity, sound)
                            },
                            bubblesEnabled = bubblesEnabled,
                            onToggleBubbles = { enabled ->
                                bubblesEnabled = enabled
                                saveBubblesEnabled(enabled)
                            },
                            onOpenSystemBubbleSettings = {
                                SmsMmsRepository.openSystemBubbleSettings(this@MainActivity)
                            },
                            autoPreviewsShowAll = autoPreviewsShowAll,
                            onToggleAutoPreviewsShowAll = { enabled ->
                                autoPreviewsShowAll = enabled
                                saveAutoPreviewsShowAll(enabled)
                            },
                            autoPreviewsWebLinksOnly = autoPreviewsWebLinksOnly,
                            onToggleAutoPreviewsWebLinksOnly = { enabled ->
                                autoPreviewsWebLinksOnly = enabled
                                saveAutoPreviewsWebLinksOnly(enabled)
                            },
                            autoPreviewsWifiOnly = autoPreviewsWifiOnly,
                            onToggleAutoPreviewsWifiOnly = { enabled ->
                                autoPreviewsWifiOnly = enabled
                                saveAutoPreviewsWifiOnly(enabled)
                            },
                            hasSmsPermission = hasSmsPerm,
                            hasMicrophonePermission = hasMicPerm,
                            hasContactsPermission = hasContactsPerm,
                            hasNotificationPermission = hasNotifPerm,
                            isDefaultSmsApp = isDefaultSms,
                            rationaleMessage = rationaleBannerText,
                            onBack = { showSettingsScreen = false },
                            onSelectTheme = { selectedMode ->
                                themeMode = selectedMode
                                saveSelectedTheme(selectedMode)
                                if (!mainWallpaper.isCustomGalleryImage && mainWallpaper.preset.isMinimalist) {
                                    val sysDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                                        android.content.res.Configuration.UI_MODE_NIGHT_YES
                                    val targetDark = when (selectedMode) {
                                        TxturaThemeMode.LIGHT -> false
                                        TxturaThemeMode.DARK, TxturaThemeMode.AMOLED -> true
                                        TxturaThemeMode.SYSTEM -> sysDark
                                    }
                                    val targetPreset = if (targetDark) WallpaperPreset.ALL_BLACK else WallpaperPreset.ALL_WHITE
                                    if (mainWallpaper.preset != targetPreset) {
                                        val cfg = mainWallpaper.copy(preset = targetPreset)
                                        mainWallpaper = cfg
                                        saveMainWallpaper(cfg)
                                    }
                                }
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
                                when (mode) {
                                    UiColorMode.MONOCHROME -> {
                                        if (!mainWallpaper.preset.isMonochrome || mainWallpaper.preset.isMinimalist) {
                                            val cfg = WallpaperConfig(
                                                preset = WallpaperPreset.MONO_SLATE,
                                                customImageUri = null,
                                                texture = mainWallpaper.texture
                                            )
                                            mainWallpaper = cfg
                                            saveMainWallpaper(cfg)
                                        }
                                    }
                                    UiColorMode.COLORFUL -> {
                                        if (mainWallpaper.preset.isMonochrome || mainWallpaper.preset.isMinimalist) {
                                            val cfg = WallpaperConfig(
                                                preset = WallpaperPreset.DEFAULT,
                                                customImageUri = null,
                                                texture = mainWallpaper.texture,
                                            )
                                            mainWallpaper = cfg
                                            saveMainWallpaper(cfg)
                                        }
                                    }
                                    UiColorMode.MINIMALIST -> {
                                        if (!mainWallpaper.preset.isMinimalist) {
                                            val sysDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                                                android.content.res.Configuration.UI_MODE_NIGHT_YES
                                            val isDark = when (themeMode) {
                                                TxturaThemeMode.LIGHT -> false
                                                TxturaThemeMode.DARK, TxturaThemeMode.AMOLED -> true
                                                TxturaThemeMode.SYSTEM -> sysDark
                                            }
                                            val cfg = WallpaperConfig(
                                                preset = if (isDark) WallpaperPreset.ALL_BLACK else WallpaperPreset.ALL_WHITE,
                                                customImageUri = null,
                                                texture = WallpaperTexture.NONE
                                            )
                                            mainWallpaper = cfg
                                            saveMainWallpaper(cfg)
                                        }
                                    }
                                }
                            },
                            onSelectMainWallpaperPreset = { preset ->
                                val targetMode = when {
                                    preset.isMinimalist -> UiColorMode.MINIMALIST
                                    preset.isMonochrome -> UiColorMode.MONOCHROME
                                    else -> UiColorMode.COLORFUL
                                }
                                if (uiColorMode != targetMode) {
                                    uiColorMode = targetMode
                                    saveUiColorMode(targetMode)
                                }
                                val cfg = WallpaperConfig(
                                    preset = preset,
                                    customImageUri = null,
                                    texture = if (preset.isMinimalist && (mainWallpaper.texture == WallpaperTexture.AUTO_THEME)) {
                                        WallpaperTexture.NONE
                                    } else {
                                        mainWallpaper.texture
                                    },
                                )
                                mainWallpaper = cfg
                                saveMainWallpaper(cfg)
                            },
                            onSelectMainWallpaperTexture = { selectedTex ->
                                val cfg = mainWallpaper.copy(texture = selectedTex)
                                mainWallpaper = cfg
                                saveMainWallpaper(cfg)
                            },
                            onSelectMainWallpaperAccentColor = { hex ->
                                val cfg = mainWallpaper.copy(customAccentColorHex = hex)
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
                            onToggleRecallMessages = { enabled ->
                                recallMessagesEnabled = enabled
                                saveRecallMessagesEnabled(enabled)
                            },
                            onToggleIdentifyMaliciousWebsites = { enabled ->
                                identifyMaliciousWebsites = enabled
                                MessageSecurityAnalyzer.isOnlineMaliciousWebsiteCheckEnabled = enabled
                                saveIdentifyMaliciousWebsites(enabled)
                                if (enabled) {
                                    SmsMmsRepository.scanAllThreadsForMaliciousWebsitesOnline(this@MainActivity)
                                }
                            },
                            onOpenDeletedMessages = {
                                showDeletedMessagesScreen = true
                            },
                            lastBackupTimestamp = lastBackupTimestamp,
                            onCreateLocalBackup = ::handleCreateLocalBackup,
                            onRestoreLocalBackup = ::handleRestoreLocalBackup,
                            onShareBackupFile = ::handleShareBackupFile,
                            onRequestSmsPermissions = ::requestSmsPermissionsSafely,
                            onRequestMicrophonePermission = ::requestMicrophonePermissionSafely,
                            onRequestContactsPermission = ::requestContactsPermissionSafely,
                            onRequestNotificationPermission = ::requestNotificationPermissionSafely,
                            onRequestDefaultSmsRole = ::requestDefaultSmsRoleSafely,
                            onOpenSystemAppSettings = ::openAppDetailsSettings,
                            onLockAppNow = {
                                if (deviceLockConfigured) {
                                    lastPauseTimestamp = 1L
                                    lastUnlockTimestamp = 0L
                                    isAppLocked = true
                                } else {
                                    val isSr = TxturaStrings.isSerbianLocale()
                                    Toast.makeText(
                                        this@MainActivity,
                                        if (isSr) "Uređaj nema podešen PIN, šablon niti biometriju zaključavanja ekrana."
                                        else "Device does not have a screen lock PIN, pattern, or biometrics set up.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        )
                    }

                    MainAppScreen.CONVERSATION_DETAIL -> {
                        val activeDetailThread = selectedThread ?: lastActiveThread
                        if (activeDetailThread != null) {
                            BackHandler {
                                activeThreadId = null
                                activeThreadAddress = null
                                pendingMmsUri = null
                            }
                            val contactKey = contactWallpaperKey(activeDetailThread.address)
                            val activeChatWallpaper = contactWallpapers[contactKey] ?: mainWallpaper
                            val threadScheduledMessages = scheduledMessages.filter { (_, threadId) ->
                                threadId == activeDetailThread.id
                            }

                            ConversationDetailScreen(
                                thread = activeDetailThread,
                                showContactPhotos = showContactPhotos,
                                recallMessagesEnabled = recallMessagesEnabled,
                                bubbleZoomScale = bubbleZoomScale,
                                onUpdateBubbleZoomScale = { newScale ->
                                bubbleZoomScale = newScale
                                saveBubbleZoomScale(newScale)
                            },
                            pinchToZoomEnabled = pinchToZoomEnabled,
                            expressiveAnimationsEnabled = expressiveAnimationsEnabled,
                            autoPreviewsShowAll = autoPreviewsShowAll,
                            autoPreviewsWebLinksOnly = autoPreviewsWebLinksOnly,
                            autoPreviewsWifiOnly = autoPreviewsWifiOnly,
                            wallpaperConfig = activeChatWallpaper,
                            pendingAttachmentUri = pendingMmsUri,
                            availableContacts = availableContacts,
                            scheduledMessages = threadScheduledMessages,
                            onBack = {
                                activeThreadId = null
                                activeThreadAddress = null
                                pendingMmsUri = null
                            },
                            onTogglePinThread = {
                                SmsMmsRepository.togglePinThread(this@MainActivity, activeDetailThread.id)
                            },
                            onTogglePinMessage = { msgId ->
                                SmsMmsRepository.togglePinMessage(this@MainActivity, activeDetailThread.id, msgId)
                            },
                            onToggleHighlightMessage = { msgId ->
                                SmsMmsRepository.toggleHighlightMessage(this@MainActivity, activeDetailThread.id, msgId)
                            },
                            onDeleteMessage = { msgId ->
                                SmsMmsRepository.deleteMessage(this@MainActivity, activeDetailThread.id, msgId, autoDeleteDays)
                                val locStrings = TxturaStrings.forLocale()
                                Toast.makeText(
                                    this@MainActivity,
                                    locStrings.messageDeletedToast,
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                            onDeleteMessages = { msgIds ->
                                SmsMmsRepository.deleteMessages(this@MainActivity, activeDetailThread.id, msgIds, autoDeleteDays)
                                val locStrings = TxturaStrings.forLocale()
                                Toast.makeText(
                                    this@MainActivity,
                                    if (msgIds.size == 1) locStrings.messageDeletedToast else locStrings.threadsDeletedToast(msgIds.size),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                            onCopyMessageText = { text ->
                                copyMessageTextToClipboard(text)
                            },
                            onForwardMessage = { msgToForward, recipientAddress, recipientName ->
                                if (!recipientName.isNullOrBlank()) {
                                    SmsMmsRepository.getOrCreateConversationWithRecipient(
                                        context = this@MainActivity,
                                        recipientAddress = recipientAddress,
                                        preferredName = recipientName,
                                    )
                                }
                                if (inAppSoundsEnabled) {
                                    SmsMmsRepository.playInAppMessageSentSound(this@MainActivity)
                                }
                                val targetThreadId = SmsMmsRepository.sendMessage(
                                    context = this@MainActivity,
                                    recipientAddress = recipientAddress,
                                    rawBody = msgToForward.body,
                                    attachmentUri = msgToForward.attachment?.uri,
                                )
                                val locStrings = TxturaStrings.forLocale()
                                Toast.makeText(
                                    this@MainActivity,
                                    locStrings.messageForwardedToast(recipientName ?: recipientAddress),
                                    Toast.LENGTH_SHORT,
                                ).show()
                                activeThreadAddress = recipientAddress
                                activeThreadId = targetThreadId
                            },
                            onOpenChatWallpaperPicker = {
                                showChatWallpaperDialogForThread = activeDetailThread
                            },
                            onCallSender = {
                                dialPhoneNumberSafely(activeDetailThread.address)
                            },
                            onOpenContact = {
                                openOrInsertContactSafely(
                                    phoneNumber = activeDetailThread.address,
                                    displayName = activeDetailThread.contactName,
                                )
                            },
                            onBlockContact = {
                                val blockedAddr = activeDetailThread.address
                                SmsMmsRepository.blockSender(this@MainActivity, blockedAddr)
                                val locStrings = TxturaStrings.forLocale()
                                Toast.makeText(
                                    this@MainActivity,
                                    locStrings.senderBlockedToast(blockedAddr),
                                    Toast.LENGTH_SHORT,
                                ).show()
                                activeThreadId = null
                                activeThreadAddress = null
                                pendingMmsUri = null
                            },
                            onToggleSenderCategory = {
                                val newIsService = SmsMmsRepository.toggleSenderCategory(this@MainActivity, activeDetailThread.id)
                                val locStrings = TxturaStrings.forLocale()
                                val toastMsg = if (newIsService) {
                                    locStrings.senderMovedToServicesToast
                                } else {
                                    locStrings.senderMovedToPeopleToast
                                }
                                Toast.makeText(this@MainActivity, toastMsg, Toast.LENGTH_SHORT).show()
                            },
                            onPickMmsPhoto = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                            onAttachUri = { uri -> pendingMmsUri = uri },
                            onClearAttachment = { pendingMmsUri = null },
                            onSendMessage = { text, attachment ->
                                if (inAppSoundsEnabled) {
                                    SmsMmsRepository.playInAppMessageSentSound(this@MainActivity)
                                }
                                SmsMmsRepository.sendMessage(
                                    context = this@MainActivity,
                                    recipientAddress = activeDetailThread.address,
                                    rawBody = text,
                                    attachmentUri = attachment,
                                )
                                pendingMmsUri = null
                            },
                            onScheduleMessage = { text, attachment, scheduledAtMillis ->
                                SmsMmsRepository.scheduleMessage(
                                    context = this@MainActivity,
                                    threadId = activeDetailThread.id,
                                    recipientAddress = activeDetailThread.address,
                                    rawBody = text,
                                    attachmentUri = attachment,
                                    scheduledAtMillis = scheduledAtMillis,
                                )
                                pendingMmsUri = null
                            },
                            onCancelScheduledMessage = { scheduledId ->
                                if (inAppSoundsEnabled) {
                                    SmsMmsRepository.playInAppMessageCancelledSound(this@MainActivity)
                                }
                                SmsMmsRepository.cancelScheduledMessage(scheduledId)
                            },
                            inAppSoundsEnabled = inAppSoundsEnabled,
                            onCopyOtp = ::copyOtpConfidentially
                        )
                    }
                    }

                    MainAppScreen.CONVERSATIONS_LIST -> {
                        ConversationsListScreen(
                            threads = threads,
                            isInitialLoading = !initialMessageLoadFinished,
                            deletedCount = deletedThreads.size,
                            autoDeleteDays = autoDeleteDays,
                            showContactPhotos = showContactPhotos,
                            listLayoutMode = listLayoutMode,
                            mainWallpaper = mainWallpaper,
                            swipeRightAction = swipeRightAction,
                            swipeLeftAction = swipeLeftAction,
                            categoryOrder = categoryOrder,
                            onUpdateCategoryOrder = { newOrder ->
                                categoryOrder = newOrder
                                saveCategoryOrder(newOrder)
                            },
                            dockBarOrder = dockBarOrder,
                            onUpdateDockBarOrder = { newOrder ->
                                dockBarOrder = newOrder
                                saveDockBarOrder(newOrder)
                            },
                            onOpenThread = { id ->
                                SmsMmsRepository.markThreadRead(this@MainActivity, id)
                                activeThreadAddress = threads.find { (tId) -> tId == id }?.address
                                activeThreadId = id
                            },
                            onTogglePin = { id ->
                                SmsMmsRepository.togglePinThread(this@MainActivity, id)
                            },
                            onRequestDeleteThread = { thread ->
                                threadPendingDeleteConfirmation = thread
                            },
                            onMarkThreadRead = { id ->
                                SmsMmsRepository.toggleThreadRead(this@MainActivity, id)
                            },
                            onDeleteSelectedThreads = { threadIds ->
                                SmsMmsRepository.moveThreadsToTrash(
                                    context = this@MainActivity,
                                    threadIds = threadIds,
                                    retentionDays = autoDeleteDays
                                )
                                val locStrings = TxturaStrings.forLocale()
                                Toast.makeText(
                                    this@MainActivity,
                                    locStrings.threadsDeletedToast(threadIds.size),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                            onMarkSelectedThreadsRead = { threadIds ->
                                SmsMmsRepository.markThreadsRead(this@MainActivity, threadIds)
                                val locStrings = TxturaStrings.forLocale()
                                Toast.makeText(
                                    this@MainActivity,
                                    locStrings.threadsMarkedReadToast(threadIds.size),
                                    Toast.LENGTH_SHORT,
                                ).show()
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
                            onOpenHighlightedMessages = { showHighlightedMessagesScreen = true },
                            onMarkAllRead = {
                                SmsMmsRepository.markAllThreadsRead(this@MainActivity)
                                val locStrings = TxturaStrings.forLocale()
                                Toast.makeText(
                                    this@MainActivity,
                                    locStrings.allMessagesMarkedReadToast,
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                            onSelectionModeChange = { isConversationsSelectionMode = it },
                            onOpenSettings = { showSettingsScreen = true }
                        )
                    }
                }
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
                            val cfg = WallpaperConfig(
                                preset = preset,
                                customImageUri = null,
                                texture = currentChatWp.texture,
                            )
                            val updated = contactWallpapers.toMutableMap().apply {
                                put(contactKey, cfg)
                            }
                            contactWallpapers = updated
                            saveContactWallpapers(updated)
                        },
                        onSelectTexture = { selectedTex ->
                            val cfg = currentChatWp.copy(texture = selectedTex)
                            val updated = contactWallpapers.toMutableMap().apply {
                                put(contactKey, cfg)
                            }
                            contactWallpapers = updated
                            saveContactWallpapers(updated)
                        },
                        onSelectAccentColor = { hex ->
                            val cfg = currentChatWp.copy(customAccentColorHex = hex)
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
                    ) {
                        showChatWallpaperDialogForThread = null
                    }
                }

                // Confirmation dialog before moving a message/conversation to Deleted Messages (Trash)
                threadPendingDeleteConfirmation?.let { (delId, delName) ->
                    ConfirmMoveToTrashDialog(
                        contactName = delName,
                        retentionDays = autoDeleteDays,
                        onConfirm = {
                            threadPendingDeleteConfirmation = null
                            SmsMmsRepository.moveThreadToTrash(
                                context = this@MainActivity,
                                threadId = delId,
                                retentionDays = autoDeleteDays,
                            )
                            val locStrings = TxturaStrings.forLocale()
                            Toast.makeText(
                                this@MainActivity,
                                locStrings.movedToTrashToast(delName),
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                    ) {
                        threadPendingDeleteConfirmation = null
                    }
                }

                if (showNewMessageDialog) {
                    NewMessageDialog(
                        initialRecipient = prefilledRecipient,
                        availableContacts = availableContacts,
                        inAppSoundsEnabled = inAppSoundsEnabled,
                        onPickContact = ::launchSystemContactPicker,
                        onDismiss = {
                            showNewMessageDialog = false
                            prefilledRecipient = ""
                            prefilledBody = ""
                        },
                        onSelectRecipient = { recipient, preferredName ->
                            val threadId = SmsMmsRepository.getOrCreateConversationWithRecipient(
                                context = this@MainActivity,
                                recipientAddress = recipient,
                                preferredName = preferredName,
                            )
                            if (prefilledBody.isNotBlank()) {
                                if (inAppSoundsEnabled) {
                                    SmsMmsRepository.playInAppMessageSentSound(this@MainActivity)
                                }
                                SmsMmsRepository.sendMessage(
                                    context = this@MainActivity,
                                    recipientAddress = recipient,
                                    rawBody = prefilledBody
                                )
                                prefilledBody = ""
                            }
                            showNewMessageDialog = false
                            prefilledRecipient = ""
                            activeThreadId = threadId
                        },
                        onSendBroadcastMessage = { recipients, messageBody ->
                            if (inAppSoundsEnabled) {
                                SmsMmsRepository.playInAppMessageSentSound(this@MainActivity)
                            }
                            TxturaHaptics.confirm(null, this@MainActivity)
                            lifecycleScope.launch(Dispatchers.IO) {
                                recipients.forEach { rec ->
                                    SmsMmsRepository.sendMessage(
                                        context = this@MainActivity,
                                        recipientAddress = rec.address,
                                        rawBody = messageBody
                                    )
                                }
                            }
                            val locStrings = TxturaStrings.forLocale()
                            Toast.makeText(
                                this@MainActivity,
                                locStrings.broadcastMessagesSentToast(recipients.size),
                                Toast.LENGTH_SHORT
                            ).show()
                            showNewMessageDialog = false
                            prefilledRecipient = ""
                            prefilledBody = ""
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
                    ) {
                        showDefaultSmsRolePrompt = false
                    }
                }

                // Confirmation dialog before restoring conversations and settings from a backup
                pendingRestorePayload?.let { restoreTarget ->
                    ConfirmRestoreDialog(
                        payload = restoreTarget,
                        onConfirm = {
                            pendingRestorePayload = null
                            applyRestorePayload(restoreTarget)
                        },
                    ) {
                        pendingRestorePayload = null
                    }
                }

                if (showCreateBackupDialog) {
                    CreateEncryptedBackupDialog(
                        onConfirm = { pwd ->
                            showCreateBackupDialog = false
                            pendingBackupPassword = pwd
                            val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                            createBackupLauncher.launch("txtura_backup_$dateStr.json")
                        },
                        onDismiss = {
                            showCreateBackupDialog = false
                        }
                    )
                }

                if (showRestorePasswordPrompt) {
                    RestoreEncryptedBackupPasswordDialog(
                        onConfirm = { pwd ->
                            val rawContent = pendingEncryptedBackupContent
                            if (rawContent != null) {
                                try {
                                    lifecycleScope.launch {
                                        try {
                                            val payload = withContext(Dispatchers.IO) {
                                                BackupManager.parseBackupJson(rawContent, pwd)
                                            }
                                            showRestorePasswordPrompt = false
                                            pendingEncryptedBackupContent = null
                                            pendingRestorePayload = payload
                                        } catch (_: Exception) {
                                            val locStrings = TxturaStrings.forLocale()
                                            Toast.makeText(
                                                this@MainActivity,
                                                locStrings.backupPasswordIncorrect,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                } catch (_: Exception) {
                                    val locStrings = TxturaStrings.forLocale()
                                    Toast.makeText(
                                        this@MainActivity,
                                        locStrings.backupPasswordIncorrect,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        onDismiss = {
                            showRestorePasswordPrompt = false
                            pendingEncryptedBackupContent = null
                        }
                    )
                }
                }
            }
        }
        }
        lifecycleScope.launch {
            val startupSettings = withContext(Dispatchers.IO) {
                val appPreferences = getSharedPreferences(APP_PREFS, MODE_PRIVATE).all.mapValues { (_, value) ->
                    if (value is Set<*>) value.toSet() else value
                }
                StartupSettings(
                    appPreferences = appPreferences,
                    legacyFlashAlertEnabled = FlashNotificationManager.isFlashAlertEnabled(this@MainActivity),
                    notificationSound = SmsMmsRepository.getSmsNotificationSound(this@MainActivity),
                    lastBackupTimestamp = BackupManager.getLastBackupTimestamp(this@MainActivity),
                    biometricLockEnabled = AppSecurityManager.isBiometricLockEnabled(this@MainActivity),
                    deviceLockConfigured = AppSecurityManager.isDeviceLockConfigured(this@MainActivity),
                    biometricLockTimeoutMinutes = AppSecurityManager.getBiometricLockTimeout(this@MainActivity)
                )
            }

            applySavedAppPreferences(
                values = startupSettings.appPreferences,
                legacyFlashAlertEnabled = startupSettings.legacyFlashAlertEnabled,
                notificationSound = startupSettings.notificationSound
            )
            firstStartupDone = startupSettings.appPreferences[KEY_FIRST_STARTUP_DONE] as? Boolean ?: false
            lastBackupTimestamp = startupSettings.lastBackupTimestamp
            biometricLockEnabled = startupSettings.biometricLockEnabled
            deviceLockConfigured = startupSettings.deviceLockConfigured
            biometricLockTimeoutMinutes = startupSettings.biometricLockTimeoutMinutes
            if (biometricLockEnabled && !deviceLockConfigured) {
                withContext(Dispatchers.IO) {
                    AppSecurityManager.setBiometricLockEnabled(this@MainActivity, false)
                }
                biometricLockEnabled = false
            }
            isAppLocked = biometricLockEnabled && deviceLockConfigured
            startupPreferencesReady = true

            SmsMmsRepository.awaitInitialization()
            startupInitializationFinished = true
            val startupIntent = pendingStartupIntent ?: intent
            pendingStartupIntent = null
            handleIncomingSmsIntentSafely(startupIntent)
            checkFirstStartupPermissionsAndRole()
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
        if (startupPreferencesReady && firstStartupDone && !SmsMmsRepository.isDefaultSmsApp(this)) {
            showDefaultSmsRolePrompt = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isAuthenticating && startupInitializationFinished) {
            lifecycleScope.launch {
                val lockSettings = withContext(Dispatchers.IO) {
                    Triple(
                        AppSecurityManager.isBiometricLockEnabled(this@MainActivity),
                        AppSecurityManager.isDeviceLockConfigured(this@MainActivity),
                        AppSecurityManager.getBiometricLockTimeout(this@MainActivity)
                    )
                }
                biometricLockEnabled = lockSettings.first
                deviceLockConfigured = lockSettings.second
                biometricLockTimeoutMinutes = lockSettings.third
                if (biometricLockEnabled && !deviceLockConfigured) {
                    withContext(Dispatchers.IO) {
                        AppSecurityManager.setBiometricLockEnabled(this@MainActivity, false)
                    }
                    biometricLockEnabled = false
                } else if (biometricLockEnabled && !isAppLocked) {
                    val elapsed = System.currentTimeMillis() - lastPauseTimestamp
                    val gracePeriod = System.currentTimeMillis() - lastUnlockTimestamp
                    if (gracePeriod > 3000L && lastPauseTimestamp > 0L &&
                        (biometricLockTimeoutMinutes == 0 || elapsed >= biometricLockTimeoutMinutes * 60 * 1000L)
                    ) {
                        isAppLocked = true
                    }
                }
            }
        }
        SmsMmsRepository.setActiveForegroundThread(activeThreadId)
        triggerAppEntryUpdateCheck()
    }

    private fun triggerAppEntryUpdateCheck() {
        lifecycleScope.launch(Dispatchers.IO) {
            val currentAppVersionName = runCatching {
                packageManager.getPackageInfo(packageName, 0).versionName ?: "3.3.1"
            }.getOrDefault("3.3.1")
            AppUpdateManager.checkAndNotifyIfUpdateAvailable(this@MainActivity, currentAppVersionName)
        }
    }

    override fun onPause() {
        super.onPause()
        if (!isAuthenticating) {
            lastPauseTimestamp = System.currentTimeMillis()
        }
        SmsMmsRepository.setActiveForegroundThread(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        biometricCancellationSignal?.cancel()
    }

    @Suppress("DEPRECATION")
    private fun promptBiometricUnlock() {
        if (isAuthenticating) return

        val isSr = TxturaStrings.isSerbianLocale()
        val title = if (isSr) "Txtura Zaštita" else "Txtura Security"
        val desc = if (isSr) "Otključajte Txtura aplikaciju biometrijom ili PIN-om" else "Unlock Txtura with biometric or device PIN"

        if (!deviceLockConfigured) {
            biometricLockEnabled = false
            AppSecurityManager.setBiometricLockEnabled(this, false)
            onAuthenticationSuccess()
            Toast.makeText(
                this,
                if (isSr) "Uređaj nema podešen PIN niti biometriju. Aplikacija je otključana i zaštita je isključena."
                else "Device has no screen lock configured. App unlocked and lock disabled.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        biometricCancellationSignal?.cancel()
        val cancellationSignal = CancellationSignal().also { biometricCancellationSignal = it }
        val executor = ContextCompat.getMainExecutor(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                isAuthenticating = true
                val prompt = BiometricPrompt.Builder(this)
                    .setTitle(title)
                    .setDescription(desc)
                    .setAllowedAuthenticators(
                        android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                            android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                            android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                    .build()

                prompt.authenticate(
                    cancellationSignal,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                            super.onAuthenticationSucceeded(result)
                            onAuthenticationSuccess()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            super.onAuthenticationError(errorCode, errString)
                            isAuthenticating = false
                            val km = getSystemService(KeyguardManager::class.java)
                            val hasKeyguard = km != null && km.isDeviceSecure
                            if (!hasKeyguard || errorCode == 11 || errorCode == 14 || errorCode == 12) {
                                AppSecurityManager.setBiometricLockEnabled(this@MainActivity, false)
                                onAuthenticationSuccess()
                                Toast.makeText(
                                    this@MainActivity,
                                    if (isSr) "Zaključavanje ekrana nije podešeno. Aplikacija je otključana."
                                    else "No device credentials enrolled. App unlocked.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                )
                return
            } catch (_: Exception) {
                isAuthenticating = false
            }
        } else {
            try {
                isAuthenticating = true
                @Suppress("DEPRECATION")
                val prompt = BiometricPrompt.Builder(this)
                    .setTitle(title)
                    .setDescription(desc)
                    .setDeviceCredentialAllowed(true)
                    .build()

                prompt.authenticate(
                    cancellationSignal,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                            super.onAuthenticationSucceeded(result)
                            onAuthenticationSuccess()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            super.onAuthenticationError(errorCode, errString)
                            isAuthenticating = false
                            val km = getSystemService(KeyguardManager::class.java)
                            val hasKeyguard = km != null && km.isDeviceSecure
                            if (!hasKeyguard || errorCode == 11 || errorCode == 14 || errorCode == 12) {
                                AppSecurityManager.setBiometricLockEnabled(this@MainActivity, false)
                                onAuthenticationSuccess()
                                Toast.makeText(
                                    this@MainActivity,
                                    if (isSr) "Zaključavanje ekrana nije podešeno. Aplikacija je otključana."
                                    else "No device credentials enrolled. App unlocked.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                )
                return
            } catch (_: Exception) {
                isAuthenticating = false
            }
        }

        val km = getSystemService(KeyguardManager::class.java)
        if (km != null && km.isDeviceSecure) {
            val intent = km.createConfirmDeviceCredentialIntent(title, desc)
            if (intent != null) {
                isAuthenticating = true
                confirmCredentialsLauncher.launch(intent)
                return
            }
        }
        AppSecurityManager.setBiometricLockEnabled(this, false)
        onAuthenticationSuccess()
    }

    private fun applySavedAppPreferences(
        values: Map<String, *>,
        legacyFlashAlertEnabled: Boolean,
        notificationSound: SmsNotificationSound
    ) {
        fun string(key: String): String? = values[key] as? String
        fun boolean(key: String, default: Boolean): Boolean = values[key] as? Boolean ?: default
        fun float(key: String, default: Float): Float = values[key] as? Float ?: default
        fun int(key: String, default: Int): Int = values[key] as? Int ?: default

        themeMode = TxturaThemeMode.entries.find {
            it.name == (string(KEY_THEME_MODE) ?: TxturaThemeMode.SYSTEM.name)
        } ?: TxturaThemeMode.SYSTEM
        showContactPhotos = boolean(KEY_SHOW_CONTACT_PHOTOS, false)
        listLayoutMode = MainListLayoutMode.entries.find {
            it.name == (string(KEY_MAIN_LIST_LAYOUT_MODE) ?: MainListLayoutMode.DEFAULT.name)
        } ?: MainListLayoutMode.DEFAULT
        bubbleZoomScale = float(KEY_BUBBLE_ZOOM_SCALE, 1.0f).coerceIn(0.75f, 1.65f)
        mainWallpaper = WallpaperConfig.deserialize(string(KEY_MAIN_WALLPAPER))

        val savedContactWpSet = (values[KEY_CONTACT_WALLPAPERS] as? Set<*>)
            .orEmpty()
            .filterIsInstance<String>()
        val parsedMap = mutableMapOf<String, WallpaperConfig>()
        savedContactWpSet.forEach { entry ->
            val sepIdx = entry.indexOf('|')
            if (sepIdx > 0) {
                parsedMap[entry.substring(0, sepIdx)] =
                    WallpaperConfig.deserialize(entry.substring(sepIdx + 1))
            }
        }
        contactWallpapers = parsedMap

        swipeRightAction = SwipeAction.entries.find {
            it.name == (string(KEY_SWIPE_RIGHT_ACTION) ?: SwipeAction.MARK_READ.name)
        } ?: SwipeAction.MARK_READ
        swipeLeftAction = SwipeAction.entries.find {
            it.name == (string(KEY_SWIPE_LEFT_ACTION) ?: SwipeAction.DELETE.name)
        } ?: SwipeAction.DELETE
        autoDeleteDays = int(KEY_AUTO_DELETE_DAYS, 30).coerceIn(1, 365)
        deletedGroupingMode = DeletedGroupingMode.entries.find {
            it.name == (string(KEY_DELETED_GROUPING_MODE) ?: DeletedGroupingMode.BY_DATE.name)
        } ?: DeletedGroupingMode.BY_DATE
        uiColorMode = UiColorMode.entries.find {
            it.name == (string(KEY_UI_COLOR_MODE) ?: UiColorMode.COLORFUL.name)
        } ?: UiColorMode.COLORFUL
        uiOpacity = float(KEY_UI_OPACITY, 0.50f).coerceIn(0.0f, 1.0f)
        categoryOrder = ConversationFilter.deserializeOrder(string(KEY_CATEGORY_ORDER))
        dockBarOrder = DockBarItem.deserializeOrder(string(KEY_DOCK_BAR_ORDER))
        recallMessagesEnabled = boolean(KEY_RECALL_MESSAGES_ENABLED, true)
        identifyMaliciousWebsites = boolean(KEY_IDENTIFY_MALICIOUS_WEBSITES, true)
        inAppSoundsEnabled = boolean(KEY_IN_APP_SOUNDS_ENABLED, true)
        pinchToZoomEnabled = boolean(KEY_PINCH_TO_ZOOM_ENABLED, true)
        expressiveAnimationsEnabled = boolean(KEY_EXPRESSIVE_ANIMATIONS_ENABLED, true)
        flashAlertEnabled = boolean(KEY_FLASH_ALERT_ENABLED, true) || legacyFlashAlertEnabled
        hapticsEnabled = boolean(KEY_HAPTICS_ENABLED, true)
        TxturaHaptics.isEnabled = hapticsEnabled
        bubblesEnabled = boolean(KEY_BUBBLES_ENABLED, false)
        autoPreviewsShowAll = boolean(KEY_AUTO_PREVIEWS_SHOW_ALL, true)
        autoPreviewsWebLinksOnly = boolean(KEY_AUTO_PREVIEWS_WEB_LINKS_ONLY, false)
        autoPreviewsWifiOnly = boolean(KEY_AUTO_PREVIEWS_WIFI_ONLY, false)
        MessageSecurityAnalyzer.isOnlineMaliciousWebsiteCheckEnabled = identifyMaliciousWebsites
        smsNotificationSound = notificationSound
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
        targetKey: String,
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

    private fun saveUiOpacity(opacity: Float) {
        getSharedPreferences(APP_PREFS, MODE_PRIVATE).edit {
            putFloat(KEY_UI_OPACITY, opacity.coerceIn(0.0f, 1.0f))
        }
    }

    private fun saveCategoryOrder(order: List<ConversationFilter>) {
        getSharedPreferences(APP_PREFS, MODE_PRIVATE).edit {
            putString(KEY_CATEGORY_ORDER, ConversationFilter.serializeOrder(order))
        }
    }

    private fun saveDockBarOrder(order: List<DockBarItem>) {
        getSharedPreferences(APP_PREFS, MODE_PRIVATE).edit {
            putString(KEY_DOCK_BAR_ORDER, DockBarItem.serializeOrder(order))
        }
    }

    private fun saveRecallMessagesEnabled(enabled: Boolean) {
        getSharedPreferences(APP_PREFS, MODE_PRIVATE).edit {
            putBoolean(KEY_RECALL_MESSAGES_ENABLED, enabled)
        }
    }

    private fun saveIdentifyMaliciousWebsites(enabled: Boolean) {
        getSharedPreferences(APP_PREFS, MODE_PRIVATE).edit {
            putBoolean(KEY_IDENTIFY_MALICIOUS_WEBSITES, enabled)
        }
    }

    private fun saveInAppSoundsEnabled(enabled: Boolean) {
        getSharedPreferences(APP_PREFS, MODE_PRIVATE).edit { putBoolean(KEY_IN_APP_SOUNDS_ENABLED, enabled) }
    }

    private fun savePinchToZoomEnabled(enabled: Boolean) {
        getSharedPreferences(APP_PREFS, MODE_PRIVATE).edit { putBoolean(KEY_PINCH_TO_ZOOM_ENABLED, enabled) }
    }

    private fun saveExpressiveAnimationsEnabled(enabled: Boolean) {
        getSharedPreferences(APP_PREFS, MODE_PRIVATE).edit { putBoolean(KEY_EXPRESSIVE_ANIMATIONS_ENABLED, enabled) }
    }

    private fun saveFlashAlertEnabled(enabled: Boolean) {
        FlashNotificationManager.setFlashAlertEnabled(this, enabled)
        getSharedPreferences(APP_PREFS, MODE_PRIVATE).edit { putBoolean(KEY_FLASH_ALERT_ENABLED, enabled) }
    }

    private fun saveHapticsEnabled(enabled: Boolean) {
        TxturaHaptics.isEnabled = enabled
        getSharedPreferences(APP_PREFS, MODE_PRIVATE).edit { putBoolean(KEY_HAPTICS_ENABLED, enabled) }
    }

    private fun saveBubblesEnabled(enabled: Boolean) {
        getSharedPreferences(APP_PREFS, MODE_PRIVATE).edit { putBoolean(KEY_BUBBLES_ENABLED, enabled) }
    }

    private fun saveAutoPreviewsShowAll(enabled: Boolean) {
        getSharedPreferences(APP_PREFS, MODE_PRIVATE).edit { putBoolean(KEY_AUTO_PREVIEWS_SHOW_ALL, enabled) }
    }

    private fun saveAutoPreviewsWebLinksOnly(enabled: Boolean) {
        getSharedPreferences(APP_PREFS, MODE_PRIVATE).edit { putBoolean(KEY_AUTO_PREVIEWS_WEB_LINKS_ONLY, enabled) }
    }

    private fun saveAutoPreviewsWifiOnly(enabled: Boolean) {
        getSharedPreferences(APP_PREFS, MODE_PRIVATE).edit { putBoolean(KEY_AUTO_PREVIEWS_WIFI_ONLY, enabled) }
    }

    private fun buildAppSettingsSnapshot(): AppSettingsSnapshot {
        val mmsApn = SmsMmsRepository.getMmsApnSettings(this)
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
            uiOpacity = uiOpacity,
            categoryOrder = categoryOrder,
            dockBarOrder = dockBarOrder,
            recallMessagesEnabled = recallMessagesEnabled,
            identifyMaliciousWebsites = identifyMaliciousWebsites,
            inAppSoundsEnabled = inAppSoundsEnabled,
            pinchToZoomEnabled = pinchToZoomEnabled,
            expressiveAnimationsEnabled = expressiveAnimationsEnabled,
            flashAlertEnabled = flashAlertEnabled,
            hapticsEnabled = hapticsEnabled,
            bubblesEnabled = bubblesEnabled,
            autoPreviewsShowAll = autoPreviewsShowAll,
            autoPreviewsWebLinksOnly = autoPreviewsWebLinksOnly,
            autoPreviewsWifiOnly = autoPreviewsWifiOnly,
            deliveryReportsEnabled = SmsMmsRepository.isDeliveryReportsEnabled(this),
            mmsRequestReadReports = SmsMmsRepository.isMmsRequestReadReports(this),
            mmsSendReadReports = SmsMmsRepository.isMmsSendReadReports(this),
            mmsAutoRetrieve = SmsMmsRepository.getMmsAutoRetrieveMode(this),
            mmsDataDisabledMode = SmsMmsRepository.getMmsDataDisabledMode(this),
            mmsCreationMode = SmsMmsRepository.getMmsCreationMode(this),
            smscNumber = SmsMmsRepository.getSmscAddress(this),
            mmsApnCarrier = mmsApn.carrierName,
            mmsApnMmsc = mmsApn.mmscUrl,
            mmsApnProxy = mmsApn.mmsProxy,
            mmsApnPort = mmsApn.mmsPort,
            mmsApnSource = mmsApn.source
        )
    }

    private fun handleCreateLocalBackup() {
        showCreateBackupDialog = true
    }

    private fun handleRestoreLocalBackup() {
        restoreBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
    }

    private fun handleShareBackupFile() {
        lifecycleScope.launch {
            val settings = withContext(Dispatchers.IO) { buildAppSettingsSnapshot() }
            val (pinnedAddrs, pinnedMsgs) = SmsMmsRepository.getPinnedDataSnapshot()
            val highlightedMsgs = SmsMmsRepository.getHighlightedMessageIdsSnapshot()
            val blockedAddrs = SmsMmsRepository.getBlockedAddressesSnapshot()
            val deletedMeta = SmsMmsRepository.getDeletedThreadsMetaSnapshot()
            SmsMmsRepository.awaitInitialization()
            SmsMmsRepository.loadAllCachedThreadHistories(this@MainActivity)
            val currentThreads = SmsMmsRepository.threads.value
            val currentDeleted = SmsMmsRepository.deletedThreads.value

            val json = withContext(Dispatchers.IO) {
                BackupManager.generateBackupJson(
                    settings = settings,
                    threads = currentThreads,
                    deletedThreads = currentDeleted,
                    pinnedAddresses = pinnedAddrs,
                    pinnedMessageIds = pinnedMsgs,
                    highlightedMessageIds = highlightedMsgs,
                    blockedAddresses = blockedAddrs,
                    deletedThreadsMeta = deletedMeta,
                )
            }
            val file = withContext(Dispatchers.IO) {
                BackupManager.createInternalBackupFile(this@MainActivity, json)
            }
            lastBackupTimestamp = System.currentTimeMillis()

            val chooser = Intent.createChooser(
                BackupManager.createShareOrDriveIntent(
                    this@MainActivity,
                    file,
                    targetGoogleDriveOnly = false
                ),
                TxturaStrings.forLocale().shareBackupButton,
            )
            startActivity(chooser)
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

        uiOpacity = payload.settings.uiOpacity
        saveUiOpacity(uiOpacity)

        categoryOrder = payload.settings.categoryOrder
        saveCategoryOrder(categoryOrder)

        dockBarOrder = payload.settings.dockBarOrder
        saveDockBarOrder(dockBarOrder)

        recallMessagesEnabled = payload.settings.recallMessagesEnabled
        saveRecallMessagesEnabled(recallMessagesEnabled)

        identifyMaliciousWebsites = payload.settings.identifyMaliciousWebsites
        MessageSecurityAnalyzer.isOnlineMaliciousWebsiteCheckEnabled = identifyMaliciousWebsites
        saveIdentifyMaliciousWebsites(identifyMaliciousWebsites)

        inAppSoundsEnabled = payload.settings.inAppSoundsEnabled
        saveInAppSoundsEnabled(inAppSoundsEnabled)

        pinchToZoomEnabled = payload.settings.pinchToZoomEnabled
        savePinchToZoomEnabled(pinchToZoomEnabled)

        expressiveAnimationsEnabled = payload.settings.expressiveAnimationsEnabled
        saveExpressiveAnimationsEnabled(expressiveAnimationsEnabled)

        flashAlertEnabled = payload.settings.flashAlertEnabled
        saveFlashAlertEnabled(flashAlertEnabled)

        hapticsEnabled = payload.settings.hapticsEnabled
        saveHapticsEnabled(hapticsEnabled)

        bubblesEnabled = payload.settings.bubblesEnabled
        saveBubblesEnabled(bubblesEnabled)

        autoPreviewsShowAll = payload.settings.autoPreviewsShowAll
        saveAutoPreviewsShowAll(autoPreviewsShowAll)

        autoPreviewsWebLinksOnly = payload.settings.autoPreviewsWebLinksOnly
        saveAutoPreviewsWebLinksOnly(autoPreviewsWebLinksOnly)

        autoPreviewsWifiOnly = payload.settings.autoPreviewsWifiOnly
        saveAutoPreviewsWifiOnly(autoPreviewsWifiOnly)

        SmsMmsRepository.setDeliveryReportsEnabled(this, payload.settings.deliveryReportsEnabled)
        SmsMmsRepository.setMmsRequestReadReports(this, payload.settings.mmsRequestReadReports)
        SmsMmsRepository.setMmsSendReadReports(this, payload.settings.mmsSendReadReports)
        SmsMmsRepository.setMmsAutoRetrieveMode(this, payload.settings.mmsAutoRetrieve)
        SmsMmsRepository.setMmsDataDisabledMode(this, payload.settings.mmsDataDisabledMode)
        SmsMmsRepository.setMmsCreationMode(this, payload.settings.mmsCreationMode)
        if (payload.settings.smscNumber.isNotBlank()) {
            SmsMmsRepository.setSmscAddress(this, payload.settings.smscNumber)
        }
        if (payload.settings.mmsApnMmsc.isNotBlank()) {
            SmsMmsRepository.setMmsApnSettings(
                this,
                com.txtura.data.MmsApnSettings(
                    carrierName = payload.settings.mmsApnCarrier,
                    mmscUrl = payload.settings.mmsApnMmsc,
                    mmsProxy = payload.settings.mmsApnProxy,
                    mmsPort = payload.settings.mmsApnPort,
                    source = payload.settings.mmsApnSource.ifBlank { "Vraćeno iz rezervne kopije" }
                )
            )
        }

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
     * On every startup of the application, verifies all required runtime permissions
     * (SMS, MMS, Contacts, and Notifications) and prompts for any that are missing,
     * as well as prompting to set Txtura as the Default SMS & MMS application.
     */
    private fun checkFirstStartupPermissionsAndRole() {
        firstStartupDone = true
        val prefs = getSharedPreferences(APP_PREFS, MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_FIRST_STARTUP_DONE, true) }

        val missingPermissions = requiredSmsPermissions.filter { perm ->
            ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            smsPermissionLauncher.launch(missingPermissions.toTypedArray())
        } else if (!SmsMmsRepository.isDefaultSmsApp(this)) {
            showDefaultSmsRolePrompt = true
        }
    }

    /**
     * Warm boot protection: update active intent reference and run strict IntentSanitizer validation.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
        if (startupInitializationFinished) {
            handleIncomingSmsIntentSafely(intent)
        } else {
            pendingStartupIntent = intent
        }
    }

    private fun handleIncomingSmsIntentSafely(incoming: Intent?) {
        if (incoming == null) return

        // 0. Handle tapping an app update notification
        if (incoming.action == AppUpdateManager.ACTION_NAVIGATE_TO_UPDATES ||
            incoming.getBooleanExtra(AppUpdateManager.EXTRA_NAVIGATE_TO_UPDATES, false)
        ) {
            AppUpdateManager.dismissUpdateNotification(this)
            showSettingsScreen = true
            focusUpdateSectionOnSettings = true
            activeThreadId = null
            activeThreadAddress = null
            showDeletedMessagesScreen = false
            showNewMessageDialog = false
            showChatWallpaperDialogForThread = null
            threadPendingDeleteConfirmation = null
            incoming.action = null
            incoming.removeExtra(AppUpdateManager.EXTRA_NAVIGATE_TO_UPDATES)
            return
        }

        // 1. Handle tapping an incoming SMS/MMS notification created by Txtura itself
        if (incoming.action == SmsMmsRepository.ACTION_OPEN_THREAD_NOTIFICATION) {
            val notifAddress = incoming.getStringExtra(SmsMmsRepository.EXTRA_NOTIFICATION_ADDRESS).orEmpty().trim()
            val notifThreadId = incoming.getLongExtra(SmsMmsRepository.EXTRA_NOTIFICATION_THREAD_ID, -1L)
            val notifId = incoming.getIntExtra(SmsMmsRepository.EXTRA_NOTIFICATION_ID, -1)

            // Dismiss the notification from status bar immediately
            val nm = getSystemService(NotificationManager::class.java)
            if (notifId > 0) {
                nm?.cancel(notifId)
            }
            if (notifAddress.isNotBlank()) {
                nm?.cancel((SmsMmsRepository.normalizeAddressKey(notifAddress).hashCode() and 0x7FFFFFFF).coerceAtLeast(1001))
            }

            // Reset all overlay states so we navigate straight into the conversation
            showSettingsScreen = false
            showDeletedMessagesScreen = false
            showHighlightedMessagesScreen = false
            showNewMessageDialog = false
            showChatWallpaperDialogForThread = null
            threadPendingDeleteConfirmation = null

            val currentThreads = SmsMmsRepository.threads.value
            val matchedThread = (if (notifThreadId > 0L) currentThreads.find { it.id == notifThreadId } else null)
                ?: (if (notifAddress.isNotBlank()) SmsMmsRepository.findThreadByAddress(currentThreads, notifAddress) else null)

            if (matchedThread != null) {
                activeThreadId = matchedThread.id
                activeThreadAddress = matchedThread.address
                SmsMmsRepository.markThreadRead(this, matchedThread.id)
            } else if (notifAddress.isNotBlank()) {
                activeThreadAddress = notifAddress
                val resolvedId = SmsMmsRepository.getOrCreateConversationWithRecipient(
                    context = this,
                    recipientAddress = notifAddress,
                )
                activeThreadId = if (notifThreadId > 0L) notifThreadId else resolvedId
                SmsMmsRepository.markThreadRead(this, activeThreadId!!)
            } else if (notifThreadId > 0L) {
                activeThreadId = notifThreadId
                activeThreadAddress = null
                SmsMmsRepository.markThreadRead(this, notifThreadId)
            }

            lifecycleScope.launch(Dispatchers.IO) {
                SmsMmsRepository.syncDeviceMessages(this@MainActivity, autoDeleteDays)
            }
            incoming.action = null
            incoming.removeExtra(SmsMmsRepository.EXTRA_NOTIFICATION_ADDRESS)
            incoming.removeExtra(SmsMmsRepository.EXTRA_NOTIFICATION_THREAD_ID)
            incoming.removeExtra(SmsMmsRepository.EXTRA_NOTIFICATION_ID)
            return
        }

        // 2. Validate external SEND / SENDTO / VIEW SMS intents
        val (validatedRecipient, validatedBody) =
            MessageSecurityAnalyzer.validateIncomingSmsIntent(this, incoming) ?: return
        prefilledRecipient = validatedRecipient
        prefilledBody = validatedBody.orEmpty()
        if (prefilledRecipient.isNotBlank() && prefilledBody.isBlank()) {
            activeThreadAddress = prefilledRecipient
            val threadId = SmsMmsRepository.getOrCreateConversationWithRecipient(
                context = this,
                recipientAddress = prefilledRecipient,
            )
            activeThreadId = threadId
            showSettingsScreen = false
            showDeletedMessagesScreen = false
            showHighlightedMessagesScreen = false
            showNewMessageDialog = false
            showChatWallpaperDialogForThread = null
            threadPendingDeleteConfirmation = null
        } else {
            showNewMessageDialog = true
        }
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
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
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
            Manifest.permission.READ_SMS,
        ) == PackageManager.PERMISSION_GRANTED
        val send = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.SEND_SMS,
        ) == PackageManager.PERMISSION_GRANTED
        return (tick >= 0) && (read && send)
    }

    @Composable
    private fun rememberDynamicMicrophonePermissionState(tick: Int): Boolean {
        val mic = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        return (tick >= 0) && mic
    }

    @Composable
    private fun rememberDynamicContactsPermissionState(tick: Int): Boolean {
        val contacts = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED
        return (tick >= 0) && contacts
    }

    @Composable
    private fun rememberDynamicNotificationPermissionState(tick: Int): Boolean {
        val notif = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        return (tick >= 0) && notif
    }

    @Composable
    private fun rememberDynamicDefaultSmsState(tick: Int): Boolean {
        return (tick >= 0) && SmsMmsRepository.isDefaultSmsApp(this)
    }

    private fun requestMicrophonePermissionSafely() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            openAppDetailsSettings()
            return
        }
        if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
            Toast.makeText(this, TxturaStrings.forLocale().voiceMessagePermissionRequired, Toast.LENGTH_SHORT).show()
        }
        microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun requestContactsPermissionSafely() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            openAppDetailsSettings()
        } else {
            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    private fun requestNotificationPermissionSafely() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                openAppDetailsSettings()
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            openAppDetailsSettings()
        }
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
            if ((roleManager != null) && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
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
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun openAppDetailsSettings() {
        val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(settingsIntent)
    }

    private fun copyMessageTextToClipboard(text: String) {
        val strings = TxturaStrings.forLocale()
        val clipboard = getSystemService(ClipboardManager::class.java) ?: return
        val clip = ClipData.newPlainText("Message", text)
        @Suppress("UsePropertyAccessSyntax")
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, strings.messageCopiedToast, Toast.LENGTH_SHORT).show()
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
        TxturaHaptics.confirm(null, this)
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
        ) {}
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
            onRequestDeleteThread = { _ -> },
            onCallSender = {},
            onOpenContact = { _, _ -> },
            onCopyOtp = {},
            onOpenNewMessage = {},
        ) {}
    }
}
