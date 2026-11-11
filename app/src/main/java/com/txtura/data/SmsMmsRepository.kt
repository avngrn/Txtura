@file:Suppress("SpellCheckingInspection", "DestructuringDeclaration", "TrailingComma", "ClarifyingParentheses")

package com.txtura.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.role.RoleManager
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteException
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import android.provider.Telephony
import android.telephony.PhoneNumberUtils
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.net.toUri
import com.txtura.MainActivity
import com.txtura.R
import com.txtura.security.AppSecurityManager
import com.txtura.security.MessageSecurityAnalyzer
import com.txtura.security.SecurityAnalysisResult
import com.txtura.security.SecurityCryptoManager
import com.txtura.security.SecurityRiskLevel
import com.txtura.telephony.DocumentAttachmentHelper
import com.txtura.telephony.FlashNotificationManager
import com.txtura.telephony.MmsPduHelper
import com.txtura.telephony.NotificationActionReceiver
import com.txtura.ui.i18n.TxturaStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Collections
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.milliseconds

object SmsMmsRepository {

    private const val TAG = "SmsMmsRepository"
    const val ACTION_SMS_SENT = "com.txtura.action.SMS_SENT"
    const val ACTION_SMS_DELIVERED = "com.txtura.action.SMS_DELIVERED"
    const val ACTION_MMS_SENT = "com.txtura.action.MMS_SENT"
    const val ACTION_MMS_DOWNLOADED = "com.txtura.action.MMS_DOWNLOADED"
    const val ACTION_OPEN_THREAD_NOTIFICATION = "com.txtura.action.OPEN_THREAD_NOTIFICATION"
    const val ACTION_NOTIFICATION_DIRECT_REPLY = "com.txtura.action.NOTIFICATION_DIRECT_REPLY"
    const val ACTION_NOTIFICATION_MARK_READ = "com.txtura.action.NOTIFICATION_MARK_READ"
    const val ACTION_NOTIFICATION_HIGHLIGHT_MSG = "com.txtura.action.NOTIFICATION_HIGHLIGHT_MSG"
    const val ACTION_NOTIFICATION_COPY_OTP = "com.txtura.action.NOTIFICATION_COPY_OTP"
    const val EXTRA_NOTIFICATION_THREAD_ID = "extra_notification_thread_id"
    const val EXTRA_NOTIFICATION_MESSAGE_ID = "extra_notification_message_id"
    const val EXTRA_NOTIFICATION_ADDRESS = "extra_notification_address"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_NOTIFICATION_OTP_CODE = "extra_notification_otp_code"
    const val KEY_NOTIFICATION_REPLY_TEXT = "key_txtura_notification_reply_text"
    private const val CHANNEL_ID_MESSAGES = "txtura_sms_messages_channel"

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val nextId = AtomicLong(100_000L)
    private const val PREFS_NAME = "txtura_pinned_prefs"
    private const val KEY_PINNED_ADDRS = "pinned_addresses"
    private const val KEY_PINNED_MSGS = "pinned_message_ids"
    private const val KEY_HIGHLIGHTED_MSGS = "highlighted_message_ids"
    private const val KEY_SMS_NOTIFICATION_SOUND = "sms_notification_sound_v1"
    private const val KEY_BLOCKED_ADDRS = "blocked_sender_addresses"
    private const val KEY_DELETED_THREADS = "deleted_threads_meta"
    private const val KEY_DELETED_MSGS = "deleted_messages_meta"
    private const val KEY_FORCED_SERVICE_ADDRS = "forced_service_addresses"
    private const val KEY_FORCED_PERSON_ADDRS = "forced_person_addresses"
    const val KEY_DELIVERY_REPORTS_ENABLED = "delivery_reports_enabled"
    const val KEY_MMS_REQUEST_READ_REPORTS = "mms_request_read_reports"
    const val KEY_MMS_SEND_READ_REPORTS = "mms_send_read_reports"
    const val KEY_MMS_AUTO_RETRIEVE = "mms_auto_retrieve"
    const val KEY_MMS_DATA_DISABLED_MODE = "mms_data_disabled_mode"
    const val KEY_MMS_CREATION_MODE = "mms_creation_mode"
    const val KEY_SMSC_NUMBER = "smsc_number"
    private const val KEY_LAST_DEVICE_SYNC_AT = "last_device_message_sync_at"
    private const val DEVICE_SYNC_MAX_AGE_MILLIS = 24L * 60L * 60L * 1000L

    private const val THREADS_CACHE_ENC_FILE = "threads_cache.enc"
    private const val THREADS_CACHE_ENC_TMP_FILE = "threads_cache.enc.tmp"
    private const val LEGACY_THREADS_CACHE_FILE = "threads_cache.json"
    private val diskCacheLock = Any()
    private val pendingHistoryLock = Any()
    private val pendingHistoryThreadIds = mutableSetOf<Long>()
    private var diskCacheSaveJob: Job? = null

    private val DEMO_THREAD_IDS = setOf(901L, 902L, 903L, 904L)
    private val DEMO_ADDRESSES = setOf("+381641122334", "BANK-OTP", "DEXPRESS", "+381690009911")

    // Keeps track of pinned contact addresses, pinned message IDs, highlighted message IDs, and blocked senders
    private val pinnedAddresses: MutableSet<String> =
        Collections.synchronizedSet(mutableSetOf())
    private val pinnedMessageIds: MutableSet<Long> =
        Collections.synchronizedSet(mutableSetOf())
    private val highlightedMessageIds: MutableSet<Long> =
        Collections.synchronizedSet(mutableSetOf())
    private val blockedAddresses: MutableSet<String> =
        Collections.synchronizedSet(mutableSetOf())
    private val forcedServiceAddresses: MutableSet<String> =
        Collections.synchronizedSet(mutableSetOf())
    private val forcedPersonAddresses: MutableSet<String> =
        Collections.synchronizedSet(mutableSetOf())

    // Cached threads that were hidden because their sender was blocked (so unblocking can restore them in memory mode)
    private val blockedThreadsCache: MutableMap<String, ConversationThread> =
        Collections.synchronizedMap(mutableMapOf())

    // Map of soft-deleted threadId -> deletedAtMillis (persisted across app restarts)
    private val deletedThreadTimestamps: MutableMap<Long, Long> =
        Collections.synchronizedMap(mutableMapOf())

    // Map of soft-deleted messageId -> Pair(threadId, deletedAtMillis) (persisted across app restarts)
    private val deletedMessageMeta: MutableMap<Long, Pair<Long, Long>> =
        Collections.synchronizedMap(mutableMapOf())

    private val _threads = MutableStateFlow<List<ConversationThread>>(emptyList())
    val threads: StateFlow<List<ConversationThread>> = _threads.asStateFlow()

    private val _deletedThreads = MutableStateFlow<List<DeletedConversationEntry>>(emptyList())
    val deletedThreads: StateFlow<List<DeletedConversationEntry>> = _deletedThreads.asStateFlow()

    private val _blockedSenders = MutableStateFlow<List<String>>(emptyList())
    val blockedSenders: StateFlow<List<String>> = _blockedSenders.asStateFlow()

    private val _scheduledMessages = MutableStateFlow<List<ScheduledSmsMessage>>(emptyList())
    val scheduledMessages: StateFlow<List<ScheduledSmsMessage>> = _scheduledMessages.asStateFlow()
    private val scheduledJobs: MutableMap<Long, Job> = Collections.synchronizedMap(mutableMapOf())

    /**
     * Dynamically checks if READ_SMS is granted at invocation time (never cached in fields).
     */
    fun hasReadSmsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Dynamically checks if SEND_SMS is granted at invocation time (never cached in fields).
     */
    fun hasSendSmsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if Txtura is currently set as the Default SMS role holder on the device.
     */
    fun isDefaultSmsApp(context: Context): Boolean {
        return try {
            val roleManager = context.getSystemService(RoleManager::class.java)
            (roleManager?.isRoleHeld(RoleManager.ROLE_SMS) == true) ||
                (Telephony.Sms.getDefaultSmsPackage(context) == context.packageName)
        } catch (e: SecurityException) {
            Log.w(TAG, "Unable to query default SMS role", e)
            false
        }
    }

    private var appContext: Context? = null
    private val initializationLock = Any()
    @Volatile
    private var isInitialized = false
    private var isInitializing = false
    @Volatile
    private var initializationJob: Job? = null

    fun initializePersistedState(context: Context) {
        val appCtx = context.applicationContext
        appContext = appCtx
        val shouldInitialize = synchronized(initializationLock) {
            if (isInitialized || isInitializing) {
                false
            } else {
                isInitializing = true
                true
            }
        }
        if (!shouldInitialize) return

        initializationJob = ioScope.launch {
            try {
                ensureNotificationChannel(appCtx)
                loadDiskCache(appCtx)
                isInitialized = true
                ioScope.launch {
                    preloadInAppSounds(appCtx)
                }
                ioScope.launch {
                    scanAllThreadsForMaliciousWebsitesOnline(appCtx)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Background repository initialization failed", e)
            } finally {
                synchronized(initializationLock) {
                    isInitializing = false
                }
            }
        }
    }

    suspend fun awaitInitialization() {
        initializationJob?.join()
    }

    /**
     * Reconcile the local cache with Android's Telephony provider at most once per day.
     * Incoming and sent messages still update the cache through their normal repository paths.
     */
    suspend fun syncDeviceMessagesIfStale(context: Context, retentionDays: Int = 30) {
        val lastSyncAt = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_DEVICE_SYNC_AT, 0L)
        if (System.currentTimeMillis() - lastSyncAt >= DEVICE_SYNC_MAX_AGE_MILLIS) {
            syncDeviceMessages(context, retentionDays)
        }
    }

    private fun recordDeviceMessageSync(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putLong(KEY_LAST_DEVICE_SYNC_AT, System.currentTimeMillis())
        }
    }

    /**
     * Asynchronously inspects URLs in a specific message against online malware & phishing threat intelligence
     * when online website identification is enabled, updating the message's security analysis in-place.
     */
    fun verifyMessageUrlsOnlineAsync(
        context: Context,
        threadId: Long,
        messageId: Long,
        body: String,
    ) {
        if (!MessageSecurityAnalyzer.isOnlineMaliciousWebsiteCheckEnabled) return
        if (!MessageSecurityAnalyzer.hasUrlsToInspect(body)) return
        val appCtx = context.applicationContext
        ioScope.launch {
            val updatedAnalysis = MessageSecurityAnalyzer.inspectMessageUrlsOnline(appCtx, body)
                ?: return@launch
            _threads.update { current ->
                current.map { thread ->
                    if (thread.id == threadId) {
                        thread.copy(
                            messages = thread.messages.map { msg ->
                                if (msg.id == messageId) {
                                    msg.copy(securityAnalysis = updatedAnalysis)
                                } else {
                                    msg
                                }
                            },
                        )
                    } else {
                        thread
                    }
                }
            }
        }
    }

    /**
     * Scans all active conversation messages that contain links using online malicious website verification.
     */
    fun scanAllThreadsForMaliciousWebsitesOnline(context: Context) {
        if (!MessageSecurityAnalyzer.isOnlineMaliciousWebsiteCheckEnabled) return
        val appCtx = context.applicationContext
        ioScope.launch {
            val snapshot = _threads.value
            for ((threadId, _, _, _, _, _, messages) in snapshot) {
                for ((msgId, _, _, body, _, _, _, _, _, _, _, securityAnalysis) in messages) {
                    if (MessageSecurityAnalyzer.hasUrlsToInspect(body)) {
                        val updatedAnalysis = MessageSecurityAnalyzer.inspectMessageUrlsOnline(appCtx, body)
                        if ((updatedAnalysis != null) && (updatedAnalysis != securityAnalysis)) {
                            _threads.update { current ->
                                current.map { t ->
                                    if (t.id == threadId) {
                                        t.copy(
                                            messages = t.messages.map { m ->
                                                if (m.id == msgId) m.copy(securityAnalysis = updatedAnalysis) else m
                                            }
                                        )
                                    } else {
                                        t
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Ensures the high-importance SMS/MMS notification channel exists with default sound and vibration.
     */
    fun getSmsNotificationSound(context: Context): SmsNotificationSound {
        return try {
            val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_SMS_NOTIFICATION_SOUND, null)
            SmsNotificationSound.fromId(saved)
        } catch (_: Exception) {
            SmsNotificationSound.SYSTEM
        }
    }

    fun setSmsNotificationSound(context: Context, sound: SmsNotificationSound) {
        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                putString(KEY_SMS_NOTIFICATION_SOUND, sound.id)
            }
        } catch (_: Exception) { }
        ensureNotificationChannel(context)
    }

    fun getActiveNotificationChannelId(context: Context): String {
        val sound = getSmsNotificationSound(context)
        return if (sound == SmsNotificationSound.SYSTEM) {
            CHANNEL_ID_MESSAGES
        } else {
            "${CHANNEL_ID_MESSAGES}_${sound.id}"
        }
    }

    fun getActiveNotificationSoundUri(context: Context): Uri {
        val sound = getSmsNotificationSound(context)
        return if (sound == SmsNotificationSound.SYSTEM) {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        } else {
            "android.resource://${context.packageName}/raw/${sound.rawResName}".toUri()
        }
    }

    fun playSmsNotificationSoundPreview(context: Context, sound: SmsNotificationSound) {
        try {
            if (sound == SmsNotificationSound.SYSTEM) {
                val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context, defaultSoundUri)
                ringtone?.play()
            } else {
                val resId = sound.rawResId ?: return
                playRawSound(context, resId, ignoreQuietMode = true)
            }
        } catch (_: Exception) { }
    }

    fun ensureNotificationChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val isSr = TxturaStrings.isSerbianLocale()
        val channelName = if (isSr) "SMS i MMS Poruke" else "SMS & MMS Messages"
        val channelDesc = if (isSr) {
            "Obaveštenja za nove primljene SMS i MMS poruke"
        } else {
            "Notifications for incoming SMS and MMS messages"
        }
        val activeChannelId = getActiveNotificationChannelId(context)
        val soundUri = getActiveNotificationSoundUri(context)
        val audioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(
            activeChannelId,
            channelName,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = channelDesc
            enableLights(true)
            enableVibration(true)
            vibrationPattern = longArrayOf(0L, 220L, 120L, 220L)
            setSound(soundUri, audioAttrs)
            setShowBadge(true)
            setAllowBubbles(true)
        }
        nm.createNotificationChannel(channel)
    }

    private fun loadPersistedPins(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.getStringSet(KEY_PINNED_ADDRS, null)?.let { saved ->
            pinnedAddresses.clear()
            pinnedAddresses.addAll(saved)
        }
        prefs.getStringSet(KEY_PINNED_MSGS, null)?.let { saved ->
            pinnedMessageIds.clear()
            saved.mapNotNull { it.toLongOrNull() }.forEach { pinnedMessageIds.add(it) }
        }
        prefs.getStringSet(KEY_HIGHLIGHTED_MSGS, null)?.let { saved ->
            highlightedMessageIds.clear()
            saved.mapNotNull { it.toLongOrNull() }.forEach { highlightedMessageIds.add(it) }
        }
        prefs.getStringSet(KEY_BLOCKED_ADDRS, null)?.let { saved ->
            blockedAddresses.clear()
            blockedAddresses.addAll(saved)
            _blockedSenders.value = blockedAddresses.sorted()
        }
        prefs.getStringSet(KEY_FORCED_SERVICE_ADDRS, null)?.let { saved ->
            forcedServiceAddresses.clear()
            forcedServiceAddresses.addAll(saved)
        }
        prefs.getStringSet(KEY_FORCED_PERSON_ADDRS, null)?.let { saved ->
            forcedPersonAddresses.clear()
            forcedPersonAddresses.addAll(saved)
        }
        prefs.getStringSet(KEY_DELETED_THREADS, null)?.let { saved ->
            deletedThreadTimestamps.clear()
            saved.forEach { entry ->
                val parts = entry.split("|")
                if (parts.size == 2) {
                    val id = parts[0].toLongOrNull()
                    val ts = parts[1].toLongOrNull()
                    if ((id != null) && (ts != null)) {
                        deletedThreadTimestamps[id] = ts
                    }
                }
            }
        }
        prefs.getStringSet(KEY_DELETED_MSGS, null)?.let { saved ->
            deletedMessageMeta.clear()
            saved.forEach { entry ->
                val parts = entry.split("|")
                if (parts.size == 3) {
                    val msgId = parts[0].toLongOrNull()
                    val threadId = parts[1].toLongOrNull()
                    val ts = parts[2].toLongOrNull()
                    if (msgId != null && threadId != null && ts != null) {
                        deletedMessageMeta[msgId] = Pair(threadId, ts)
                    }
                }
            }
        }

        // Permanently purge any legacy demo threads/addresses from memory and persistent sets
        val hadDemoPins = pinnedAddresses.removeAll(DEMO_ADDRESSES)
        val hadDemoMsgs = pinnedMessageIds.removeAll(setOf(1L, 2L, 3L, 4L, 5L, 6L))
        val hadDemoServices = forcedServiceAddresses.removeAll(DEMO_ADDRESSES)
        var hadDemoDeleted = false
        DEMO_THREAD_IDS.forEach { id ->
            if (deletedThreadTimestamps.remove(id) != null) {
                hadDemoDeleted = true
            }
        }
        var hadDemoDeletedMsgs = false
        DEMO_THREAD_IDS.forEach { demoThreadId ->
            val toRemove = synchronized(deletedMessageMeta) {
                deletedMessageMeta.filter { it.value.first == demoThreadId }.keys.toList()
            }
            toRemove.forEach {
                deletedMessageMeta.remove(it)
                hadDemoDeletedMsgs = true
            }
        }
        if (hadDemoPins || hadDemoMsgs || hadDemoServices || hadDemoDeleted || hadDemoDeletedMsgs) {
            savePersistedPins(context)
        }
        _deletedThreads.update { trash ->
            trash.filterNot { (thread) -> (thread.id in DEMO_THREAD_IDS) || (thread.address in DEMO_ADDRESSES) }
        }
        _threads.update { list ->
            list.filterNot { (id, _, address) -> (id in DEMO_THREAD_IDS) || (address in DEMO_ADDRESSES) }
        }
    }

    private fun savePersistedPins(
        context: Context,
        historyThreadIds: Set<Long> = emptySet(),
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val serializedDeleted = synchronized(deletedThreadTimestamps) {
            deletedThreadTimestamps.entries
                .asSequence()
                .map { (key, value) -> "$key|$value" }
                .toSet()
        }
        val serializedDeletedMsgs = synchronized(deletedMessageMeta) {
            deletedMessageMeta.entries
                .asSequence()
                .map { (key, value) -> "$key|${value.first}|${value.second}" }
                .toSet()
        }
        prefs.edit {
            putStringSet(KEY_PINNED_ADDRS, synchronized(pinnedAddresses) { pinnedAddresses.toSet() })
            putStringSet(KEY_PINNED_MSGS, synchronized(pinnedMessageIds) { pinnedMessageIds.asSequence().map { it.toString() }.toSet() })
            putStringSet(KEY_HIGHLIGHTED_MSGS, synchronized(highlightedMessageIds) { highlightedMessageIds.asSequence().map { it.toString() }.toSet() })
            putStringSet(KEY_BLOCKED_ADDRS, synchronized(blockedAddresses) { blockedAddresses.toSet() })
            putStringSet(KEY_FORCED_SERVICE_ADDRS, synchronized(forcedServiceAddresses) { forcedServiceAddresses.toSet() })
            putStringSet(KEY_FORCED_PERSON_ADDRS, synchronized(forcedPersonAddresses) { forcedPersonAddresses.toSet() })
            putStringSet(KEY_DELETED_THREADS, serializedDeleted)
            putStringSet(KEY_DELETED_MSGS, serializedDeletedMsgs)
        }
        saveDiskCacheAsync(context, historyThreadIds = historyThreadIds)
    }

    private fun loadDiskCache(context: Context) {
        if (_threads.value.isNotEmpty()) return
        synchronized(diskCacheLock) {
            if (_threads.value.isNotEmpty()) return
            try {
                loadPersistedPins(context)
                val database = TxturaSecureDatabase.get(context)
                val snapshotDao = database.repositorySnapshotDao()
                val isEncryptedMode = AppSecurityManager.isDbEncryptionEnabled(context)
                val summarySnapshot = snapshotDao.getThreadSummarySnapshot()

                if (summarySnapshot != null) {
                    val needsReencryption =
                        SecurityCryptoManager.isEncryptedCache(summarySnapshot.payload) != isEncryptedMode
                    val root = JSONObject(readCachePayload(context, summarySnapshot.payload))
                    val summaries = ArrayList<ConversationThread>()
                    val threadArray = root.optJSONArray("threads") ?: JSONArray()
                    for (index in 0 until threadArray.length()) {
                        val thread = threadArray.optJSONObject(index)?.let(::parseCachedThread) ?: continue
                        if (!isSenderBlocked(thread.address) && thread.id !in deletedThreadTimestamps) {
                            summaries.add(
                                thread.copy(
                                    messages = listOfNotNull(thread.lastMessage),
                                    isHistoryLoaded = false,
                                )
                            )
                        }
                    }
                    _threads.value = sortThreads(summaries)
                    _deletedThreads.value = parseCachedDeletedThreads(root)
                    if (needsReencryption) {
                        reencryptStoredThreadHistories(context)
                        saveDiskCacheInternal(context)
                    }
                    return
                }

                val storedSnapshot = snapshotDao.getSnapshot()
                val encryptedFile = File(context.filesDir, THREADS_CACHE_ENC_FILE)
                val legacyFile = File(context.filesDir, LEGACY_THREADS_CACHE_FILE)
                val jsonContent = if (storedSnapshot != null) {
                    readCachePayload(context, storedSnapshot.payload)
                } else {
                    val legacyPayload = when {
                        isEncryptedMode && encryptedFile.exists() && encryptedFile.length() > 0L ->
                            encryptedFile.readBytes()
                        legacyFile.exists() && legacyFile.length() > 0L ->
                            legacyFile.readBytes()
                        encryptedFile.exists() && encryptedFile.length() > 0L ->
                            encryptedFile.readBytes()
                        else -> return
                    }
                    readCachePayload(context, legacyPayload)
                }

                if (jsonContent.isBlank()) return
                val root = JSONObject(jsonContent)
                val threadArray = root.optJSONArray("threads") ?: JSONArray()
                val fullThreads = ArrayList<ConversationThread>(threadArray.length())
                for (index in 0 until threadArray.length()) {
                    val thread = threadArray.optJSONObject(index)?.let(::parseCachedThread) ?: continue
                    if (!isSenderBlocked(thread.address) && thread.id !in deletedThreadTimestamps) {
                        fullThreads.add(thread)
                    }
                }
                val deleted = parseCachedDeletedThreads(root)
                val summaries = fullThreads.map(::toThreadSummary)
                _threads.value = sortThreads(summaries)
                _deletedThreads.value = deleted

                // One-time migration from the monolithic snapshot to per-conversation history rows.
                saveDiskCacheInternal(context, fullHistoryThreads = fullThreads)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load threads cache from disk", e)
            }
        }
    }

    private fun parseCachedDeletedThreads(root: JSONObject): List<DeletedConversationEntry> {
        val deleted = ArrayList<DeletedConversationEntry>()
        root.optJSONArray("deletedThreads")?.let { entries ->
            for (index in 0 until entries.length()) {
                val entry = entries.optJSONObject(index) ?: continue
                val deletedAt = entry.optLong("deletedAtMillis", System.currentTimeMillis())
                val thread = entry.optJSONObject("thread")?.let(::parseCachedThread) ?: continue
                deleted.add(DeletedConversationEntry(thread = thread, deletedAtMillis = deletedAt))
            }
        }
        return deleted
    }

    private fun toThreadSummary(thread: ConversationThread): ConversationThread =
        thread.copy(
            messages = listOfNotNull(thread.lastMessage),
            isHistoryLoaded = false,
        )

    private fun readCachePayload(context: Context, payload: ByteArray): String {
        val plaintext = if (SecurityCryptoManager.isEncryptedCache(payload)) {
            SecurityCryptoManager.decryptCache(context, payload)
        } else {
            payload
        }
        return try {
            String(plaintext, Charsets.UTF_8)
        } finally {
            plaintext.fill(0)
            if (plaintext !== payload) payload.fill(0)
        }
    }

    private fun encodeCachePayload(context: Context, plaintext: ByteArray): ByteArray =
        if (AppSecurityManager.isDbEncryptionEnabled(context)) {
            SecurityCryptoManager.encryptCache(context, plaintext)
        } else {
            plaintext.copyOf()
        }

    private fun reencryptStoredThreadHistories(context: Context) {
        val targetEncrypted = AppSecurityManager.isDbEncryptionEnabled(context)
        val dao = TxturaSecureDatabase.get(context).repositorySnapshotDao()
        val replacements = ArrayList<ConversationHistoryEntity>()
        try {
            dao.getAllConversationHistories().forEach { row ->
                try {
                    if (SecurityCryptoManager.isEncryptedCache(row.payload) == targetEncrypted) return@forEach
                    val plaintext = if (SecurityCryptoManager.isEncryptedCache(row.payload)) {
                        SecurityCryptoManager.decryptCache(context, row.payload)
                    } else {
                        row.payload.copyOf()
                    }
                    try {
                        replacements.add(
                            ConversationHistoryEntity(
                                threadId = row.threadId,
                                payload = encodeCachePayload(context, plaintext),
                                messageCount = row.messageCount,
                                isComplete = row.isComplete,
                            )
                        )
                    } finally {
                        plaintext.fill(0)
                    }
                } finally {
                    row.payload.fill(0)
                }
            }
            if (replacements.isNotEmpty()) dao.saveConversationHistories(replacements)
        } finally {
            replacements.forEach { it.payload.fill(0) }
        }
    }

    suspend fun loadCachedThreadHistory(context: Context, threadId: Long) = withContext(Dispatchers.IO) {
        val activeThread = _threads.value.firstOrNull { it.id == threadId }
        val deletedEntry = if (activeThread == null) {
            _deletedThreads.value.firstOrNull { it.thread.id == threadId }
        } else {
            null
        }
        val currentThread = activeThread ?: deletedEntry?.thread ?: return@withContext
        if (currentThread.isHistoryLoaded) return@withContext

        val historyRow = TxturaSecureDatabase.get(context)
            .repositorySnapshotDao()
            .getConversationHistory(threadId) ?: return@withContext
        val historyJson = readCachePayload(context, historyRow.payload)
        val fullThread = runCatching { parseCachedThread(JSONObject(historyJson)) }.getOrNull()
            ?: return@withContext
        val mergedThread = mergeCachedThreadState(
            fullThread = fullThread,
            currentThread = currentThread,
            isHistoryLoaded = historyRow.isComplete &&
                fullThread.messages.size >= historyRow.messageCount,
        )

        if (activeThread != null) {
            _threads.update { threads ->
                threads.map { if (it.id == threadId && !it.isHistoryLoaded) mergedThread else it }
            }
        } else {
            _deletedThreads.update { entries ->
                entries.map { entry ->
                    if (entry.thread.id == threadId && !entry.thread.isHistoryLoaded) {
                        entry.copy(thread = mergedThread)
                    } else {
                        entry
                    }
                }
            }
        }
        saveDiskCacheAsync(context)
    }

    private fun mergeCachedThreadState(
        fullThread: ConversationThread,
        currentThread: ConversationThread,
        isHistoryLoaded: Boolean,
    ): ConversationThread {
        val mergedMessages = LinkedHashMap<Long, ChatMessage>()
        fullThread.messages.forEach { mergedMessages[it.id] = it }
        currentThread.messages.forEach { mergedMessages[it.id] = it }
        return fullThread.copy(
            contactName = currentThread.contactName,
            address = currentThread.address,
            avatarInitials = currentThread.avatarInitials,
            avatarAccentColorHex = currentThread.avatarAccentColorHex,
            contactPhotoUri = currentThread.contactPhotoUri ?: fullThread.contactPhotoUri,
            messages = mergedMessages.values.sortedBy { it.timestampMillis },
            unreadCount = currentThread.unreadCount,
            isPinned = currentThread.isPinned,
            isService = currentThread.isService,
            draftText = currentThread.draftText ?: fullThread.draftText,
            isHistoryLoaded = isHistoryLoaded,
        )
    }

    suspend fun loadAllCachedThreadHistories(context: Context) {
        val activeIds = _threads.value.filterNot { it.isHistoryLoaded }.map { it.id }
        val activeIdSet = _threads.value.mapTo(mutableSetOf()) { it.id }
        val deletedIds = _deletedThreads.value
            .asSequence()
            .filter { !it.thread.isHistoryLoaded && it.thread.id !in activeIdSet }
            .map { it.thread.id }
            .toList()
        (activeIds + deletedIds).distinct().forEach { loadCachedThreadHistory(context, it) }
    }

    private fun saveDiskCacheInternal(
        context: Context,
        fullHistoryThreads: List<ConversationThread>? = null,
    ): Boolean {
        synchronized(diskCacheLock) {
            var claimedHistoryIds = emptySet<Long>()
            try {
                claimedHistoryIds = synchronized(pendingHistoryLock) {
                    pendingHistoryThreadIds.toSet().also { pendingHistoryThreadIds.clear() }
                }
                val currentThreads = _threads.value
                val currentDeleted = _deletedThreads.value
                val dao = TxturaSecureDatabase.get(context).repositorySnapshotDao()
                val existingHistoryIds = dao.getConversationHistoryIds().toSet()
                val fullHistoryById = fullHistoryThreads
                    .orEmpty()
                    .filter { it.isHistoryLoaded }
                    .associateBy { it.id }
                val savedHistoryIds = mutableSetOf<Long>()
                val historyRows = ArrayList<ConversationHistoryEntity>()

                fun addHistoryIfMissing(
                    thread: ConversationThread,
                    source: ConversationThread?,
                    isComplete: Boolean,
                ) {
                    if (!savedHistoryIds.add(thread.id)) return
                    val historyThread = source ?: thread
                    val historyJson = serializeCachedThread(
                        historyThread.copy(isHistoryLoaded = isComplete),
                        includeAllMessages = true,
                    ).toString().toByteArray(Charsets.UTF_8)
                    val payload = try {
                        encodeCachePayload(context, historyJson)
                    } finally {
                        historyJson.fill(0)
                    }
                    historyRows.add(
                        ConversationHistoryEntity(
                            threadId = thread.id,
                            payload = payload,
                            messageCount = historyThread.messages.size,
                            isComplete = isComplete,
                        )
                    )
                }

                currentThreads.forEach { thread ->
                    val fullHistory = fullHistoryById[thread.id]
                    when {
                        fullHistory != null -> addHistoryIfMissing(thread, fullHistory, isComplete = true)
                        thread.id !in existingHistoryIds && thread.messages.isNotEmpty() ->
                            addHistoryIfMissing(thread, thread, isComplete = thread.isHistoryLoaded)
                        thread.id in claimedHistoryIds && thread.isHistoryLoaded ->
                            addHistoryIfMissing(thread, thread, isComplete = true)
                        thread.id in claimedHistoryIds && !thread.isHistoryLoaded -> {
                            val storedHistory = dao.getConversationHistory(thread.id)
                            if (storedHistory != null) {
                                val storedThread = runCatching {
                                    parseCachedThread(
                                        JSONObject(readCachePayload(context, storedHistory.payload))
                                    )
                                }.getOrNull()
                                if (storedThread != null) {
                                    val mergedHistory = mergeCachedThreadState(
                                        fullThread = storedThread,
                                        currentThread = thread,
                                        isHistoryLoaded = storedHistory.isComplete,
                                    )
                                    addHistoryIfMissing(
                                        thread,
                                        mergedHistory,
                                        isComplete = storedHistory.isComplete,
                                    )
                                }
                            }
                        }
                    }
                }
                val activeIds = currentThreads.mapTo(mutableSetOf()) { it.id }
                currentDeleted.forEach { deleted ->
                    val thread = deleted.thread
                    if (thread.id in activeIds) return@forEach
                    val fullHistory = fullHistoryById[thread.id]
                    when {
                        fullHistory != null -> addHistoryIfMissing(thread, fullHistory, isComplete = true)
                        thread.id !in existingHistoryIds && thread.messages.isNotEmpty() ->
                            addHistoryIfMissing(thread, thread, isComplete = thread.isHistoryLoaded)
                        thread.id in claimedHistoryIds && thread.isHistoryLoaded ->
                            addHistoryIfMissing(thread, thread, isComplete = true)
                        thread.id in claimedHistoryIds -> {
                            val storedHistory = dao.getConversationHistory(thread.id)
                            if (storedHistory != null) {
                                val storedThread = runCatching {
                                    parseCachedThread(
                                        JSONObject(readCachePayload(context, storedHistory.payload))
                                    )
                                }.getOrNull()
                                if (storedThread != null) {
                                    val mergedHistory = mergeCachedThreadState(
                                        fullThread = storedThread,
                                        currentThread = thread,
                                        isHistoryLoaded = storedHistory.isComplete,
                                    )
                                    addHistoryIfMissing(
                                        thread,
                                        mergedHistory,
                                        isComplete = storedHistory.isComplete,
                                    )
                                }
                            }
                        }
                    }
                }

                if (historyRows.isNotEmpty()) {
                    try {
                        dao.saveConversationHistories(historyRows)
                    } finally {
                        historyRows.forEach { it.payload.fill(0) }
                    }
                }

                val retainedHistoryIds = (activeIds + currentDeleted.map { it.thread.id }).toSet()
                existingHistoryIds
                    .asSequence()
                    .filterNot { it in retainedHistoryIds }
                    .forEach(dao::deleteConversationHistory)

                val root = JSONObject()
                root.put("version", 2)
                root.put("timestamp", System.currentTimeMillis())

                val summaries = JSONArray()
                currentThreads.forEach { thread ->
                    summaries.put(serializeCachedThread(toThreadSummary(thread)))
                }
                root.put("threads", summaries)

                val deleted = JSONArray()
                currentDeleted.forEach { entry ->
                    val deletedObject = JSONObject()
                    deletedObject.put("deletedAtMillis", entry.deletedAtMillis)
                    deletedObject.put("thread", serializeCachedThread(entry.thread))
                    deleted.put(deletedObject)
                }
                root.put("deletedThreads", deleted)

                val jsonBytes = root.toString().toByteArray(Charsets.UTF_8)
                val payload = try {
                    encodeCachePayload(context, jsonBytes)
                } finally {
                    jsonBytes.fill(0)
                }
                try {
                    dao.saveThreadSummarySnapshot(
                        ThreadSummarySnapshotEntity(payload = payload)
                    )
                    dao.deleteSnapshot()
                } finally {
                    payload.fill(0)
                }

                File(context.filesDir, THREADS_CACHE_ENC_FILE).delete()
                File(context.filesDir, THREADS_CACHE_ENC_TMP_FILE).delete()
                File(context.filesDir, LEGACY_THREADS_CACHE_FILE).delete()
                File(context.filesDir, "$LEGACY_THREADS_CACHE_FILE.tmp").delete()

                if (!AppSecurityManager.isDbEncryptionEnabled(context)) {
                    SecurityCryptoManager.clearCacheSessionKey(context, deleteWrappedKey = true)
                }
            } catch (e: Exception) {
                synchronized(pendingHistoryLock) {
                    pendingHistoryThreadIds.addAll(claimedHistoryIds)
                }
                Log.w(TAG, "Failed to persist threads cache", e)
                return false
            }
        }
        return true
    }

    /**
     * Re-encrypts or decrypts the local cache when user changes the encryption preference in settings.
     */
    fun reencryptCache(context: Context) {
        val appCtx = context.applicationContext
        ioScope.launch {
            initializationJob?.join()
            synchronized(diskCacheLock) {
                reencryptStoredThreadHistories(appCtx)
                saveDiskCacheInternal(appCtx)
            }
        }
    }

    /**
     * Debounced or immediate asynchronous persistence to disk cache.
     */
    fun saveDiskCacheAsync(
        context: Context,
        immediate: Boolean = false,
        historyThreadIds: Set<Long> = emptySet(),
    ) {
        synchronized(pendingHistoryLock) {
            pendingHistoryThreadIds.addAll(historyThreadIds)
        }
        val appCtx = context.applicationContext
        if (immediate) {
            ioScope.launch {
                saveDiskCacheInternal(appCtx)
            }
        } else {
            synchronized(diskCacheLock) {
                diskCacheSaveJob?.cancel()
                diskCacheSaveJob = ioScope.launch {
                    delay(1500.milliseconds)
                    saveDiskCacheInternal(appCtx)
                }
            }
        }
    }

    private fun parseCachedThread(obj: JSONObject): ConversationThread? {
        val id = obj.optLong("id", 0L)
        if (id <= 0L) return null
        val name = obj.optString("contactName", "Unknown")
        val address = obj.optString("address", "")
        val initials = obj.optString("avatarInitials", "U")
        val colorHex = obj.optLong("avatarAccentColorHex", 0xFF6366F1)
        val photoUriStr = obj.optString("contactPhotoUri").takeIf { it.isNotBlank() }
        val photoUri = photoUriStr?.let { runCatching { it.toUri() }.getOrNull() }
        val unreadCount = obj.optInt("unreadCount", 0)
        val isPinned = isAddressPinned(address) || obj.optBoolean("isPinned", false)
        val draftText = obj.optString("draftText").takeIf { it.isNotBlank() }

        val messages = mutableListOf<ChatMessage>()
        val msgsArray = obj.optJSONArray("messages")
        if (msgsArray != null) {
            for (i in 0 until msgsArray.length()) {
                val mObj = msgsArray.optJSONObject(i) ?: continue
                val mId = mObj.optLong("id", 0L)
                val mThreadId = mObj.optLong("threadId", id)
                val mAddr = mObj.optString("address", address)
                val mBody = mObj.optString("body", "")
                val mTime = mObj.optLong("timestampMillis", 0L)
                val isIncoming = mObj.optBoolean("isIncoming", true)
                val typeStr = mObj.optString("type", MessageType.SMS.name)
                val type = MessageType.entries.find { it.name == typeStr } ?: MessageType.SMS
                val statusStr = mObj.optString("status", DeliveryStatus.DELIVERED.name)
                val status = DeliveryStatus.entries.find { it.name == statusStr } ?: DeliveryStatus.DELIVERED
                val isMsgPinned = (mId in pinnedMessageIds) || mObj.optBoolean("isPinned", false)
                val isMsgHighlighted = (mId in highlightedMessageIds) || mObj.optBoolean("isHighlighted", false)

                var attachment: MmsAttachment? = null
                mObj.optJSONObject("attachment")?.let { aObj ->
                    val uStr = aObj.optString("uri")
                    if (uStr.isNotBlank()) {
                        runCatching {
                            attachment = MmsAttachment(
                                uri = Uri.parse(uStr),
                                mimeType = aObj.optString("mimeType", "image/jpeg"),
                                caption = aObj.optString("caption", "MMS Photo")
                            )
                        }
                    }
                }

                val riskStr = mObj.optString("securityRiskLevel", SecurityRiskLevel.SAFE.name)
                val riskLevel = SecurityRiskLevel.entries.find { it.name == riskStr } ?: SecurityRiskLevel.SAFE
                val otpCode = mObj.optString("otpCode").takeIf { it.isNotBlank() }
                val secResult = SecurityAnalysisResult(
                    riskLevel = riskLevel,
                    reasons = emptyList(),
                    extractedOtpCode = otpCode
                )

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
                        isPinned = isMsgPinned,
                        isHighlighted = isMsgHighlighted,
                        attachment = attachment,
                        securityAnalysis = secResult
                    )
                )
            }
        }

        val isService = isAddressService(address, messages, name)

        return ConversationThread(
            id = id,
            contactName = name,
            address = address,
            avatarInitials = initials,
            avatarAccentColorHex = colorHex,
            contactPhotoUri = photoUri,
            messages = messages,
            unreadCount = unreadCount,
            isPinned = isPinned,
            isService = isService,
            draftText = draftText,
            isHistoryLoaded = obj.optBoolean("historyComplete", true)
        )
    }

    private fun serializeCachedThread(
        thread: ConversationThread,
        includeAllMessages: Boolean = false,
    ): JSONObject {
        val obj = JSONObject()
        obj.put("id", thread.id)
        obj.put("contactName", thread.contactName)
        obj.put("address", thread.address)
        obj.put("avatarInitials", thread.avatarInitials)
        obj.put("avatarAccentColorHex", thread.avatarAccentColorHex)
        thread.contactPhotoUri?.let { obj.put("contactPhotoUri", it.toString()) }
        obj.put("unreadCount", thread.unreadCount)
        obj.put("isPinned", thread.isPinned)
        obj.put("isService", thread.isService)
        thread.draftText?.let { obj.put("draftText", it) }
        obj.put("historyComplete", thread.isHistoryLoaded)

        val msgsArr = JSONArray()
        val messagesToCache = if (includeAllMessages || thread.messages.size <= 100) {
            thread.messages
        } else {
            thread.messages.takeLast(100)
        }
        for (m in messagesToCache) {
            val mObj = JSONObject()
            mObj.put("id", m.id)
            mObj.put("threadId", m.threadId)
            mObj.put("address", m.address)
            mObj.put("body", m.body)
            mObj.put("timestampMillis", m.timestampMillis)
            mObj.put("isIncoming", m.isIncoming)
            mObj.put("type", m.type.name)
            mObj.put("status", m.status.name)
            mObj.put("isPinned", m.isPinned)
            mObj.put("isHighlighted", m.isHighlighted)
            m.attachment?.let { att ->
                val aObj = JSONObject()
                aObj.put("uri", att.uri.toString())
                aObj.put("mimeType", att.mimeType)
                aObj.put("caption", att.caption)
                mObj.put("attachment", aObj)
            }
            mObj.put("securityRiskLevel", m.securityAnalysis.riskLevel.name)
            m.securityAnalysis.extractedOtpCode?.let { mObj.put("otpCode", it) }
            msgsArr.put(mObj)
        }
        obj.put("messages", msgsArr)
        return obj
    }

    /**
     * Checks if a sender address is currently blocked.
     */
    fun isSenderBlocked(address: String): Boolean {
        appContext?.let {
            if (!AppSecurityManager.isSpamFirewallEnabled(it)) {
                return false
            }
        }
        val clean = address.trim()
        if (clean.isBlank()) return false
        val normKey = normalizeAddressKey(clean)
        return synchronized(blockedAddresses) {
            blockedAddresses.any { blocked ->
                blocked.equals(clean, ignoreCase = true) ||
                    (normalizeAddressKey(blocked) == normKey) ||
                    arePhoneNumbersEqual(blocked, clean)
            }
        }
    }

    /** Rebuilds the visible inbox after the user changes the blocked-sender firewall. */
    fun refreshSpamFirewallState(context: Context) {
        val appCtx = context.applicationContext
        if (!hasReadSmsPermission(appCtx)) return
        ioScope.launch {
            syncDeviceMessages(appCtx)
        }
    }

    /**
     * Determines whether an address should be categorized as a Service message.
     * Takes manual user overrides into account first, followed by standard heuristics.
     */
    fun isAddressService(
        address: String,
        messages: List<ChatMessage> = emptyList(),
        contactName: String? = null
    ): Boolean {
        val clean = address.trim()
        val norm = normalizeAddressKey(clean)
        val isForcedPerson = synchronized(forcedPersonAddresses) {
            (clean in forcedPersonAddresses) || (norm.isNotEmpty() && (norm in forcedPersonAddresses))
        }
        if (isForcedPerson) return false

        val isForcedService = synchronized(forcedServiceAddresses) {
            (clean in forcedServiceAddresses) || (norm.isNotEmpty() && (norm in forcedServiceAddresses))
        }
        if (isForcedService) return true

        // 1. If this sender has a saved contact in the user's phone contacts (not just the raw number), it is a PERSON!
        val hasKnownContactName = !contactName.isNullOrBlank() &&
            contactName != clean &&
            contactName.any { it.isLetter() }
        if (hasKnownContactName) return false

        // 2. If the user has sent outgoing messages in this thread (bidirectional chat), it is a PERSON!
        val hasOutgoingMessages = messages.any { !it.isIncoming }
        if (hasOutgoingMessages) return false

        // 3. True alphanumeric sender (e.g. "Yettel", "BankaIntesa", "Google", "DHL") without standard phone digits
        val cleanDigits = clean.filter { it.isDigit() }
        val hasLetters = clean.any { it.isLetter() }
        val isAlphaSender = hasLetters && cleanDigits.length <= 2
        if (isAlphaSender) return true

        // 4. Short codes (typically 3 to 6 digits, without international '+' prefix)
        val isNotInternational = !clean.startsWith("+")
        val isShortCode = isNotInternational && cleanDigits.isNotEmpty() && (cleanDigits.length in 3..6)
        return isShortCode

        // 5. Standard phone numbers (7+ digits or with '+'):
        // Full phone numbers are people by default. OTP messages do not turn full phone numbers into services.
    }

    /**
     * Toggles whether the sender of [threadId] is categorized as "Osobe" or "Servisna poruka".
     * Returns true if now Service, false if now Person.
     */
    fun toggleSenderCategory(context: Context, threadId: Long): Boolean {
        var newIsService = false
        _threads.update { currentList ->
            currentList.map { thread ->
                if (thread.id == threadId) {
                    newIsService = !thread.isService
                    val clean = thread.address.trim()
                    val norm = normalizeAddressKey(clean)
                    synchronized(forcedServiceAddresses) {
                        synchronized(forcedPersonAddresses) {
                            if (newIsService) {
                                forcedPersonAddresses.remove(clean)
                                if (norm.isNotEmpty()) forcedPersonAddresses.remove(norm)
                                forcedServiceAddresses.add(clean)
                                if (norm.isNotEmpty()) forcedServiceAddresses.add(norm)
                            } else {
                                forcedServiceAddresses.remove(clean)
                                if (norm.isNotEmpty()) forcedServiceAddresses.remove(norm)
                                forcedPersonAddresses.add(clean)
                                if (norm.isNotEmpty()) forcedPersonAddresses.add(norm)
                            }
                        }
                    }
                    thread.copy(isService = newIsService)
                } else {
                    thread
                }
            }
        }
        savePersistedPins(context)
        return newIsService
    }

    /**
     * Blocks a single sender address and removes their active conversations from the inbox.
     */
    fun blockSender(context: Context, address: String) {
        blockSenders(context, listOf(address))
    }

    /**
     * Blocks multiple sender addresses and removes their active conversations from the inbox.
     */
    fun blockSenders(context: Context, addresses: Collection<String>) {
        val cleaned = addresses.asSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
        if (cleaned.isEmpty()) return

        synchronized(blockedAddresses) {
            cleaned.forEach { addr ->
                val alreadyPresent = blockedAddresses.any {
                    it.equals(addr, ignoreCase = true) || arePhoneNumbersEqual(it, addr)
                }
                if (!alreadyPresent) {
                    blockedAddresses.add(addr)
                }
            }
            _blockedSenders.value = blockedAddresses.sorted()
        }
        savePersistedPins(context)

        _threads.update { current ->
            val remaining = mutableListOf<ConversationThread>()
            for (thread in current) {
                if (isSenderBlocked(thread.address)) {
                    blockedThreadsCache[normalizeAddressKey(thread.address)] = thread
                } else {
                    remaining.add(thread)
                }
            }
            remaining
        }
    }

    /**
     * Unblocks a previously blocked sender address and restores their thread if cached or on device.
     */
    fun unblockSender(context: Context, address: String) {
        val clean = address.trim()
        val normKey = normalizeAddressKey(clean)
        synchronized(blockedAddresses) {
            blockedAddresses.removeAll { blocked ->
                blocked.equals(clean, ignoreCase = true) ||
                    normalizeAddressKey(blocked) == normKey ||
                    arePhoneNumbersEqual(blocked, clean)
            }
            _blockedSenders.value = blockedAddresses.sorted()
        }
        savePersistedPins(context)

        val cached = blockedThreadsCache.remove(normKey)
        if (cached != null) {
            _threads.update { current ->
                if (current.none { it.id == cached.id }) {
                    sortThreads(current + cached)
                } else {
                    current
                }
            }
        }
        ioScope.launch {
            if (hasReadSmsPermission(context)) {
                syncDeviceMessages(context)
            }
        }
    }

    // --- SMS & MMS Advanced System Settings ---

    fun isDeliveryReportsEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DELIVERY_REPORTS_ENABLED, false)
    }

    fun setDeliveryReportsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_DELIVERY_REPORTS_ENABLED, enabled)
        }
    }

    fun isMmsRequestReadReports(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_MMS_REQUEST_READ_REPORTS, false)
    }

    fun setMmsRequestReadReports(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_MMS_REQUEST_READ_REPORTS, enabled)
        }
    }

    fun isMmsSendReadReports(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_MMS_SEND_READ_REPORTS, false)
    }

    fun setMmsSendReadReports(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_MMS_SEND_READ_REPORTS, enabled)
        }
    }

    fun getMmsAutoRetrieveMode(context: Context): MmsAutoRetrieveMode {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_MMS_AUTO_RETRIEVE, MmsAutoRetrieveMode.NOT_ROAMING.name)
        return MmsAutoRetrieveMode.entries.find { it.name == raw } ?: MmsAutoRetrieveMode.NOT_ROAMING
    }

    fun setMmsAutoRetrieveMode(context: Context, mode: MmsAutoRetrieveMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_MMS_AUTO_RETRIEVE, mode.name)
        }
    }

    fun getMmsDataDisabledMode(context: Context): MmsDataDisabledMode {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_MMS_DATA_DISABLED_MODE, MmsDataDisabledMode.NOT_ROAMING.name)
        return MmsDataDisabledMode.entries.find { it.name == raw } ?: MmsDataDisabledMode.NOT_ROAMING
    }

    fun setMmsDataDisabledMode(context: Context, mode: MmsDataDisabledMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_MMS_DATA_DISABLED_MODE, mode.name)
        }
    }

    fun getMmsCreationMode(context: Context): MmsCreationMode {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_MMS_CREATION_MODE, MmsCreationMode.FREE.name)
        return MmsCreationMode.entries.find { it.name == raw } ?: MmsCreationMode.FREE
    }

    fun setMmsCreationMode(context: Context, mode: MmsCreationMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_MMS_CREATION_MODE, mode.name)
        }
    }

    @android.annotation.SuppressLint("MissingPermission", "HardwareIds")
    fun getSmscAddress(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_SMSC_NUMBER, null)
        if (!saved.isNullOrBlank()) return saved

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val sm = context.getSystemService(SmsManager::class.java)
                val smsc = sm?.smscAddress
                if (!smsc.isNullOrBlank()) return smsc
            } catch (_: Exception) { }
        }
        return ""
    }

    fun setSmscAddress(context: Context, address: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_SMSC_NUMBER, address.trim())
        }
    }

    // Carrier MMS is handled natively by Android's telephony service and carrier bundle





    fun onMessageDelivered(messageId: Long, context: Context? = null) {
        _threads.update { currentThreads ->
            currentThreads.map { thread ->
                if (thread.messages.any { it.id == messageId }) {
                    thread.copy(
                        messages = thread.messages.map { msg ->
                            if (msg.id == messageId) msg.copy(status = DeliveryStatus.DELIVERED) else msg
                        }
                    )
                } else {
                    thread
                }
            }
        }
        context?.let { ctx ->
            val threadId = _threads.value.firstOrNull { thread -> thread.messages.any { it.id == messageId } }?.id
            saveDiskCacheAsync(ctx, historyThreadIds = setOfNotNull(threadId))
        }
    }

    fun onMessageFailed(messageId: Long, context: Context? = null) {
        _threads.update { currentThreads ->
            currentThreads.map { thread ->
                if (thread.messages.any { it.id == messageId }) {
                    thread.copy(
                        messages = thread.messages.map { msg ->
                            if (msg.id == messageId) msg.copy(status = DeliveryStatus.FAILED) else msg
                        }
                    )
                } else {
                    thread
                }
            }
        }
        context?.let { ctx ->
            val threadId = _threads.value.firstOrNull { thread -> thread.messages.any { it.id == messageId } }?.id
            saveDiskCacheAsync(ctx, historyThreadIds = setOfNotNull(threadId))
        }
    }

    fun openEmergencyAlerts(context: Context) {
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

    fun openMessageSoundSettings(context: Context) {
        try {
            ensureNotificationChannel(context)
            val channelId = getActiveNotificationChannelId(context)
            val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
                putExtra("android.provider.extra.APP_PACKAGE", context.packageName)
                putExtra("android.provider.extra.CHANNEL_ID", channelId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        } catch (_: Exception) { }
        try {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_SOUND_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) { }
        }
    }

    fun openSystemBubbleSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_BUBBLE_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            } catch (_: Exception) { }
        }
        try {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) { }
    }

    /**
     * Opens an MMS image attachment in the user's default image viewer or gallery app.
     */
    fun openImageInGallery(context: Context, imageUri: Uri) {
        try {
            val shareableUri: Uri = try {
                if (imageUri.scheme == "content" && imageUri.authority == "${context.packageName}.fileprovider") {
                    imageUri
                } else {
                    val cacheFile = File(context.cacheDir, "mms_view_${imageUri.hashCode().toLong() and 0xFFFFFFFFL}.jpg")
                    context.contentResolver.openInputStream(imageUri)?.use { input ->
                        cacheFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (cacheFile.exists() && cacheFile.length() > 0) {
                        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)
                    } else {
                        imageUri
                    }
                }
            } catch (_: Exception) {
                imageUri
            }

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(shareableUri, "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(viewIntent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(imageUri, "image/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {
                val isSr = TxturaStrings.isSerbianLocale()
                Toast.makeText(
                    context,
                    if (isSr) "Nije pronađena aplikacija za pregled slika." else "No app found to open image.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    @Volatile
    private var sentSoundPool: android.media.SoundPool? = null
    @Volatile
    private var sentSoundId: Int = 0
    @Volatile
    private var sentSoundLoaded: Boolean = false
    @Volatile
    private var receivedSoundId: Int = 0
    @Volatile
    private var receivedSoundLoaded: Boolean = false
    @Volatile
    private var cancelSoundId: Int = 0
    @Volatile
    private var cancelSoundLoaded: Boolean = false

    private val actionSoundMap = java.util.concurrent.ConcurrentHashMap<Int, Int>()
    private val loadedSoundIds = Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<Int, Boolean>())

    @Volatile
    private var activeForegroundThreadId: Long? = null

    fun setActiveForegroundThread(threadId: Long?) {
        activeForegroundThreadId = threadId
    }

    fun getActiveForegroundThread(): Long? = activeForegroundThreadId

    fun isInAppSoundsEnabled(context: Context): Boolean {
        return try {
            context.getSharedPreferences("txtura_app_prefs", Context.MODE_PRIVATE)
                .getBoolean("in_app_sounds_enabled", true)
        } catch (_: Exception) {
            true
        }
    }

    fun preloadInAppSounds(context: Context) {
        try {
            FlashNotificationManager.preloadRhythms(context.applicationContext)
            if (sentSoundPool != null) return
            val appContext = context.applicationContext
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .build()

            val pool = android.media.SoundPool.Builder()
                .setMaxStreams(10)
                .setAudioAttributes(audioAttributes)
                .build()

            pool.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0) {
                    loadedSoundIds.add(sampleId)
                    if (sampleId == sentSoundId) sentSoundLoaded = true
                    if (sampleId == receivedSoundId) receivedSoundLoaded = true
                    if (sampleId == cancelSoundId) cancelSoundLoaded = true
                }
            }

            fun load(resId: Int): Int {
                val id = pool.load(appContext, resId, 1)
                actionSoundMap[resId] = id
                return id
            }

            sentSoundId = load(R.raw.odlazna)
            receivedSoundId = load(R.raw.dolazna)
            cancelSoundId = load(R.raw.cancel)
            load(R.raw.highlight_poruke)
            load(R.raw.unhighlight_poruke)
            load(R.raw.kanta_brisanje)
            load(R.raw.kanta_poklopac_pop)
            load(R.raw.pin_poruke)
            load(R.raw.unpin_poruke)
            load(R.raw.neprocitano)
            load(R.raw.procitano)
            load(R.raw.sms_dupli_puls)
            load(R.raw.sms_kristalni_zvoncic)
            load(R.raw.sms_meki_pop)
            load(R.raw.sms_ritam_kod)
            load(R.raw.sms_topla_kalimba)
            load(R.raw.sms_diskretni_oblak)
            load(R.raw.glasovna_pocetak_dupli_klik)
            load(R.raw.glasovna_ponisti_ispumpani_pop)

            sentSoundPool = pool
        } catch (_: Exception) { }
    }

    fun playRawSound(context: Context, soundResId: Int, ignoreQuietMode: Boolean = false) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
            if (!ignoreQuietMode) {
                if (!isInAppSoundsEnabled(context)) return
                if (audioManager != null && audioManager.ringerMode != android.media.AudioManager.RINGER_MODE_NORMAL) {
                    return
                }
            }

            if (sentSoundPool == null) {
                preloadInAppSounds(context)
            }

            val sampleId = actionSoundMap[soundResId]
            if (sampleId != null && sampleId != 0 && loadedSoundIds.contains(sampleId)) {
                val streamId = sentSoundPool?.play(sampleId, 1.0f, 1.0f, 1, 0, 1.0f) ?: 0
                if (streamId != 0) return
            }

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .build()

            val mediaPlayer = android.media.MediaPlayer.create(context.applicationContext, soundResId, audioAttributes, 0)
                ?: android.media.MediaPlayer.create(context.applicationContext, soundResId)

            mediaPlayer?.apply {
                setOnCompletionListener { mp ->
                    try { mp.reset(); mp.release() } catch (_: Exception) { }
                }
                setOnErrorListener { mp, _, _ ->
                    try { mp.reset(); mp.release() } catch (_: Exception) { }
                    true
                }
                start()
            }
        } catch (_: Exception) { }
    }

    fun playHighlightSound(context: Context) = playRawSound(context, R.raw.highlight_poruke)
    fun playUnhighlightSound(context: Context) = playRawSound(context, R.raw.unhighlight_poruke)
    fun playDeleteSound(context: Context) = playRawSound(context, R.raw.kanta_brisanje)
    fun playPermanentDeleteSound(context: Context) = playRawSound(context, R.raw.kanta_poklopac_pop)
    fun playPinSound(context: Context) = playRawSound(context, R.raw.pin_poruke)
    fun playUnpinSound(context: Context) = playRawSound(context, R.raw.unpin_poruke)
    fun playMarkUnreadSound(context: Context) = playRawSound(context, R.raw.neprocitano)
    fun playMarkReadSound(context: Context) = playRawSound(context, R.raw.procitano)
    fun playVoiceRecordStartSound(context: Context) = playRawSound(context, R.raw.glasovna_pocetak_dupli_klik)
    fun playVoiceRecordCancelSound(context: Context) = playRawSound(context, R.raw.glasovna_ponisti_ispumpani_pop)

    fun playInAppMessageSentSound(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
            // Respect device silent/vibration modes
            if (audioManager != null && audioManager.ringerMode != android.media.AudioManager.RINGER_MODE_NORMAL) {
                return
            }

            // Ensure sound pool is initialized
            if (sentSoundPool == null) {
                preloadInAppSounds(context)
            }

            // If loaded in SoundPool, play instantly with zero latency
            if (sentSoundLoaded && sentSoundId != 0) {
                val streamId = sentSoundPool?.play(sentSoundId, 1.0f, 1.0f, 1, 0, 1.0f) ?: 0
                if (streamId != 0) return
            }

            // Fallback: MediaPlayer if SoundPool hasn't finished loading yet or failed
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .build()

            val mediaPlayer = android.media.MediaPlayer.create(context.applicationContext, R.raw.odlazna, audioAttributes, 0)
                ?: android.media.MediaPlayer.create(context.applicationContext, R.raw.odlazna)

            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener { mp ->
                    try {
                        mp.reset()
                        mp.release()
                    } catch (_: Exception) { }
                }
                mediaPlayer.setOnErrorListener { mp, _, _ ->
                    try {
                        mp.reset()
                        mp.release()
                    } catch (_: Exception) { }
                    true
                }
                mediaPlayer.start()
                return
            }

            audioManager?.playSoundEffect(android.media.AudioManager.FX_KEYPRESS_STANDARD, 0.75f)
        } catch (_: Exception) {
            try {
                android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 65)
                    .startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 70)
            } catch (_: Exception) { }
        }
    }

    fun playInAppMessageReceivedSound(context: Context) {
        if (FlashNotificationManager.isFlashAlertEnabled(context)) {
            FlashNotificationManager.triggerFlashNotification(context)
        }
        val sound = getSmsNotificationSound(context)
        val customResId = sound.rawResId
        if (customResId != null) {
            playRawSound(context, customResId)
            return
        }

        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
            // Respect device silent/vibration modes
            if (audioManager != null && audioManager.ringerMode != android.media.AudioManager.RINGER_MODE_NORMAL) {
                return
            }

            // Ensure sound pool is initialized
            if (sentSoundPool == null) {
                preloadInAppSounds(context)
            }

            // If loaded in SoundPool, play instantly with zero latency
            if (receivedSoundLoaded && receivedSoundId != 0) {
                val streamId = sentSoundPool?.play(receivedSoundId, 1.0f, 1.0f, 1, 0, 1.0f) ?: 0
                if (streamId != 0) return
            }

            // Fallback: MediaPlayer if SoundPool hasn't finished loading yet or failed
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .build()

            val mediaPlayer = android.media.MediaPlayer.create(context.applicationContext, R.raw.dolazna, audioAttributes, 0)
                ?: android.media.MediaPlayer.create(context.applicationContext, R.raw.dolazna)

            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener { mp ->
                    try {
                        mp.reset()
                        mp.release()
                    } catch (_: Exception) { }
                }
                mediaPlayer.setOnErrorListener { mp, _, _ ->
                    try {
                        mp.reset()
                        mp.release()
                    } catch (_: Exception) { }
                    true
                }
                mediaPlayer.start()
                return
            }

            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone?.play()
        } catch (_: Exception) {
            try {
                android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 65)
                    .startTone(android.media.ToneGenerator.TONE_PROP_ACK, 80)
            } catch (_: Exception) { }
        }
    }

    fun playInAppMessageCancelledSound(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
            // Respect device silent/vibration modes
            if (audioManager != null && audioManager.ringerMode != android.media.AudioManager.RINGER_MODE_NORMAL) {
                return
            }

            // Ensure sound pool is initialized
            if (sentSoundPool == null) {
                preloadInAppSounds(context)
            }

            // If loaded in SoundPool, play instantly with zero latency
            if (cancelSoundLoaded && cancelSoundId != 0) {
                val streamId = sentSoundPool?.play(cancelSoundId, 1.0f, 1.0f, 1, 0, 1.0f) ?: 0
                if (streamId != 0) return
            }

            // Fallback: MediaPlayer if SoundPool hasn't finished loading yet or failed
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .build()

            val mediaPlayer = android.media.MediaPlayer.create(context.applicationContext, R.raw.cancel, audioAttributes, 0)
                ?: android.media.MediaPlayer.create(context.applicationContext, R.raw.cancel)

            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener { mp ->
                    try {
                        mp.reset()
                        mp.release()
                    } catch (_: Exception) { }
                }
                mediaPlayer.setOnErrorListener { mp, _, _ ->
                    try {
                        mp.reset()
                        mp.release()
                    } catch (_: Exception) { }
                    true
                }
                mediaPlayer.start()
                return
            }

            audioManager?.playSoundEffect(android.media.AudioManager.FX_KEY_CLICK, 0.75f)
        } catch (_: Exception) {
            try {
                android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 65)
                    .startTone(android.media.ToneGenerator.TONE_PROP_NACK, 70)
            } catch (_: Exception) { }
        }
    }

    fun isWifiConnected(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return false
            val activeNet = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNet) ?: return false
            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
        } catch (_: Exception) {
            false
        }
    }

    fun getSimMessages(context: Context): List<SimCardMessage> {
        val result = mutableListOf<SimCardMessage>()
        try {
            val uri = "content://sms/icc".toUri()
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex("_id").takeIf { it >= 0 }
                    ?: cursor.getColumnIndex("index_on_icc").takeIf { it >= 0 }
                    ?: 0
                val addrIdx = cursor.getColumnIndex("address").takeIf { it >= 0 } ?: -1
                val bodyIdx = cursor.getColumnIndex("body").takeIf { it >= 0 } ?: -1
                val dateIdx = cursor.getColumnIndex("date").takeIf { it >= 0 } ?: -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIdx)
                    val addr = if (addrIdx >= 0) cursor.getString(addrIdx).orEmpty() else ""
                    val rawBody = if (bodyIdx >= 0) cursor.getString(bodyIdx).orEmpty() else ""
                    val cleanBody = MessageSecurityAnalyzer.sanitizeMessageText(rawBody)
                    val date = if (dateIdx >= 0) cursor.getLong(dateIdx) else System.currentTimeMillis()
                    result.add(SimCardMessage(id = id, address = addr, body = cleanBody, timestamp = date))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed reading SIM card messages", e)
        }
        return result
    }

    fun deleteSimMessage(context: Context, simMessageId: Long): Boolean {
        return try {
            val uri = "content://sms/icc/$simMessageId".toUri()
            val count = context.contentResolver.delete(uri, null, null)
            count > 0
        } catch (e: Exception) {
            Log.w(TAG, "Failed deleting SIM card message", e)
            false
        }
    }

    fun copySimMessageToInbox(context: Context, simMsg: SimCardMessage): Boolean {
        return try {
            persistMessageToSystemProviderIfDefault(
                context = context,
                address = simMsg.address,
                body = simMsg.body,
                timestampMillis = if (simMsg.timestamp > 0) simMsg.timestamp else System.currentTimeMillis(),
                messageType = Telephony.Sms.MESSAGE_TYPE_INBOX,
                read = 1
            )
            onIncomingMessageReceived(
                context = context.applicationContext,
                senderAddress = simMsg.address,
                rawBody = simMsg.body,
                isMms = false
            )
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed copying SIM card message to inbox", e)
            false
        }
    }

    /**
     * Loads the complete existing SMS and MMS history from Android's system Telephony provider.
     */
    suspend fun syncDeviceMessages(
        context: Context,
        retentionDays: Int = 30
    ) = withContext(Dispatchers.IO) {
        loadPersistedPins(context)
        purgeExpiredDeletedThreadsInternal(context, retentionDays)
        if (!hasReadSmsPermission(context)) {
            _threads.value = emptyList()
            return@withContext
        }

        val loadedMessages = mutableListOf<ChatMessage>()
        val unreadCountByThread = mutableMapOf<Long, Int>()

        // 1. Load existing SMS messages (Inbox, Sent, Outbox, Failed)
        val smsProjection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
            Telephony.Sms.READ
        )
        val smsSelection = "${Telephony.Sms.TYPE} IN (?, ?, ?, ?)"
        val smsSelectionArgs = arrayOf(
            Telephony.Sms.MESSAGE_TYPE_INBOX.toString(),
            Telephony.Sms.MESSAGE_TYPE_SENT.toString(),
            Telephony.Sms.MESSAGE_TYPE_OUTBOX.toString(),
            Telephony.Sms.MESSAGE_TYPE_FAILED.toString()
        )

        try {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                smsProjection,
                smsSelection,
                smsSelectionArgs,
                "${Telephony.Sms.DATE} DESC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
                val threadIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
                val addrIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val typeIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)
                val readIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.READ)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIdx)
                    val threadId = cursor.getLong(threadIdx)
                    val rawAddr = cursor.getString(addrIdx).orEmpty().ifBlank { "Unknown" }
                    val rawBody = cursor.getString(bodyIdx).orEmpty()
                    val cleanBody = MessageSecurityAnalyzer.sanitizeMessageText(rawBody)
                    val date = cursor.getLong(dateIdx)
                    val msgType = cursor.getInt(typeIdx)
                    val isRead = cursor.getInt(readIdx) == 1
                    val isIncoming = msgType == Telephony.Sms.MESSAGE_TYPE_INBOX

                    if (isIncoming && !isRead) {
                        unreadCountByThread[threadId] = (unreadCountByThread[threadId] ?: 0) + 1
                    }

                    val deliveryStatus = when (msgType) {
                        Telephony.Sms.MESSAGE_TYPE_FAILED -> DeliveryStatus.FAILED
                        Telephony.Sms.MESSAGE_TYPE_OUTBOX -> DeliveryStatus.SENDING
                        else -> DeliveryStatus.DELIVERED
                    }

                    loadedMessages.add(
                        ChatMessage(
                            id = id,
                            threadId = threadId,
                            address = rawAddr,
                            body = cleanBody,
                            timestampMillis = date,
                            isIncoming = isIncoming,
                            type = MessageType.SMS,
                            status = deliveryStatus,
                            isPinned = id in pinnedMessageIds,
                            isHighlighted = id in highlightedMessageIds,
                            securityAnalysis = MessageSecurityAnalyzer.analyzeMessage(cleanBody)
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission revoked while querying Telephony.Sms", e)
            return@withContext
        } catch (e: SQLiteException) {
            Log.e(TAG, "Database error while querying Telephony.Sms", e)
            return@withContext
        }

        // 2. Load existing MMS messages (received & sent multimedia messages)
        loadDeviceMmsMessages(context, loadedMessages, unreadCountByThread)

        // 3. Group by system THREAD_ID (or normalized phone number if threadId <= 0)
        val accentColors = listOf(0xFF00E5FF, 0xFF6366F1, 0xFFA855F7, 0xFF10B981, 0xFFF59E0B)
        val contactNameCache = mutableMapOf<String, String>()
        val contactPhotoCache = mutableMapOf<String, Uri?>()

        val deviceThreads = loadedMessages
            .groupBy { msg ->
                val normAddr = normalizeAddressKey(msg.address)
                val resolvedName = contactNameCache.getOrPut(msg.address) {
                    resolveContactNameSafely(context, msg.address) ?: msg.address
                }
                val hasRealName = resolvedName.isNotBlank() && resolvedName != msg.address && resolvedName.any { it.isLetter() }
                when {
                    hasRealName -> "contact_${resolvedName.trim().lowercase(Locale.ROOT)}"
                    normAddr.isNotBlank() -> "addr_$normAddr"
                    msg.threadId > 0L -> "thread_${msg.threadId}"
                    else -> "addr_${msg.address.trim().lowercase(Locale.ROOT)}"
                }
            }
            .entries
            .mapIndexed { index, (_, msgs) ->
                val sortedMsgs = deduplicateMessageList(msgs).sortedBy { it.timestampMillis }
                val primaryAddress = sortedMsgs.lastOrNull { it.address.isNotBlank() && it.address != "Unknown" }
                    ?.address ?: sortedMsgs.first().address
                val resolvedName = contactNameCache.getOrPut(primaryAddress) {
                    resolveContactNameSafely(context, primaryAddress) ?: primaryAddress
                }
                val resolvedPhotoUri = contactPhotoCache.getOrPut(primaryAddress) {
                    resolveContactPhotoUriSafely(context, primaryAddress)
                }
                val initials = computeInitials(resolvedName)
                val threadId = sortedMsgs.lastOrNull { it.threadId > 0L }?.threadId
                    ?: sortedMsgs.first().threadId.takeIf { it > 0L }
                    ?: (primaryAddress.hashCode().toLong() and 0x7FFFFFFFL)
                val isContactPinned = isAddressPinned(primaryAddress)
                val unread = sortedMsgs.map { it.threadId }.distinct().sumOf { unreadCountByThread[it] ?: 0 }

                ConversationThread(
                    id = threadId,
                    contactName = resolvedName,
                    address = primaryAddress,
                    avatarInitials = initials,
                    avatarAccentColorHex = accentColors[index % accentColors.size],
                    contactPhotoUri = resolvedPhotoUri,
                    messages = sortedMsgs,
                    unreadCount = unread,
                    isPinned = isContactPinned,
                    isService = isAddressService(primaryAddress, sortedMsgs, resolvedName)
                )
            }

        // Partition threads into active inbox vs soft-deleted Trash threads, excluding blocked senders
        val activeList = mutableListOf<ConversationThread>()
        val trashedFromDevice = mutableListOf<DeletedConversationEntry>()
        val allDraftsMap = getAllDrafts(context)
        for (thread in deviceThreads) {
            if (isSenderBlocked(thread.address)) {
                blockedThreadsCache[normalizeAddressKey(thread.address)] = thread
                continue
            }
            val deletedAt = deletedThreadTimestamps[thread.id]
            if (deletedAt != null) {
                trashedFromDevice.add(
                    DeletedConversationEntry(
                        thread = thread,
                        deletedAtMillis = deletedAt
                    )
                )
            } else {
                val (trashedMsgs, remainingMsgs) = thread.messages.partition { msg ->
                    msg.id in deletedMessageMeta
                }
                if (trashedMsgs.isNotEmpty()) {
                    val latestDeletedAt = trashedMsgs.mapNotNull { deletedMessageMeta[it.id]?.second }
                        .maxOrNull() ?: System.currentTimeMillis()
                    trashedFromDevice.add(
                        DeletedConversationEntry(
                            thread = thread.copy(messages = trashedMsgs),
                            deletedAtMillis = latestDeletedAt
                        )
                    )
                }
                val hasDraft = !allDraftsMap[thread.id].isNullOrBlank()
                if (remainingMsgs.isNotEmpty() || hasDraft) {
                    activeList.add(thread.copy(messages = remainingMsgs))
                }
            }
        }

        // Preserve in-memory conversations (or recently arrived/sent messages) not yet in Telephony DB
        val previousInMemoryThreads = _threads.value.filterNot { it.id in DEMO_THREAD_IDS || it.address in DEMO_ADDRESSES }
        val mergedActiveList = activeList.asSequence().map { deviceThread ->
            val matchingInMemory = previousInMemoryThreads.find { prev ->
                areThreadsSameContact(prev, deviceThread)
            }
            if (matchingInMemory != null) {
                val missingMessages = matchingInMemory.messages.filter { memMsg ->
                    memMsg.id !in deletedMessageMeta &&
                        deviceThread.messages.none { dbMsg ->
                            dbMsg.body == memMsg.body &&
                                kotlin.math.abs(dbMsg.timestampMillis - memMsg.timestampMillis) < 15_000L
                        }
                }
                if (missingMessages.isNotEmpty()) {
                    val combinedMsgs = deduplicateMessageList(deviceThread.messages + missingMessages.map { it.copy(threadId = deviceThread.id) })
                        .sortedBy { it.timestampMillis }
                    deviceThread.copy(messages = combinedMsgs)
                } else {
                    deviceThread
                }
            } else {
                deviceThread
            }
        }.toMutableList()

        for (inMemoryThread in previousInMemoryThreads) {
            if (isSenderBlocked(inMemoryThread.address) || (inMemoryThread.id in deletedThreadTimestamps)) {
                continue
            }
            val activeInMemoryMsgs = inMemoryThread.messages.filterNot { it.id in deletedMessageMeta }
            val hasDraft = !allDraftsMap[inMemoryThread.id].isNullOrBlank()
            if (activeInMemoryMsgs.isEmpty() && !hasDraft) {
                continue
            }
            val alreadyInDeviceList = mergedActiveList.any { dev ->
                areThreadsSameContact(dev, inMemoryThread)
            }
            val hasRecentMessage = inMemoryThread.messages.any { (System.currentTimeMillis() - it.timestampMillis) < 60_000L }
            if (!alreadyInDeviceList && (hasRecentMessage || hasDraft)) {
                mergedActiveList.add(inMemoryThread.copy(messages = activeInMemoryMsgs))
            }
        }

        val activeWithDrafts = mergedActiveList.map { th ->
            val draft = allDraftsMap[th.id]
            if (!draft.isNullOrBlank()) th.copy(draftText = draft) else th
        }
        _threads.value = sortThreads(activeWithDrafts)
        _deletedThreads.update { existingTrash ->
            val combinedMap = mutableMapOf<Long, DeletedConversationEntry>()
            for (entry in existingTrash + trashedFromDevice) {
                val prev = combinedMap[entry.thread.id]
                if (prev != null) {
                    val mergedMsgs = deduplicateMessageList(prev.thread.messages + entry.thread.messages)
                        .sortedBy { it.timestampMillis }
                    combinedMap[entry.thread.id] = prev.copy(
                        thread = prev.thread.copy(messages = mergedMsgs),
                        deletedAtMillis = maxOf(prev.deletedAtMillis, entry.deletedAtMillis)
                    )
                } else {
                    combinedMap[entry.thread.id] = entry
                }
            }
            combinedMap.values
                .asSequence()
                .filter {
                    (it.thread.id in deletedThreadTimestamps) ||
                        it.thread.messages.any { m -> m.id in deletedMessageMeta }
                }
                .filterNot { it.thread.id in DEMO_THREAD_IDS || it.thread.address in DEMO_ADDRESSES }
                .sortedByDescending { it.deletedAtMillis }
                .toList()
        }
        val appContext = context.applicationContext
        ioScope.launch {
            delay(1500.milliseconds)
            val activeHistories = _threads.value.filter { it.isHistoryLoaded }
            val activeIds = activeHistories.mapTo(mutableSetOf()) { it.id }
            val deletedOnlyHistories = _deletedThreads.value
                .asSequence()
                .map { it.thread }
                .filter { it.isHistoryLoaded && it.id !in activeIds }
                .toList()
            if (saveDiskCacheInternal(appContext, fullHistoryThreads = activeHistories + deletedOnlyHistories)) {
                recordDeviceMessageSync(appContext)
            }
        }
        scanAllThreadsForMaliciousWebsitesOnline(context)
    }

    /**
     * Queries Telephony.Mms.CONTENT_URI for existing MMS messages, extracting text parts,
     * image attachments, and sender/recipient phone numbers.
     */
    private fun loadDeviceMmsMessages(
        context: Context,
        outMessages: MutableList<ChatMessage>,
        unreadCountByThread: MutableMap<Long, Int>
    ) {
        val mmsProjection = arrayOf(
            Telephony.Mms._ID,
            Telephony.Mms.THREAD_ID,
            Telephony.Mms.DATE,
            Telephony.Mms.MESSAGE_BOX,
            Telephony.Mms.READ
        )
        try {
            context.contentResolver.query(
                Telephony.Mms.CONTENT_URI,
                mmsProjection,
                null,
                null,
                "${Telephony.Mms.DATE} DESC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(Telephony.Mms._ID)
                val threadIdx = cursor.getColumnIndexOrThrow(Telephony.Mms.THREAD_ID)
                val dateIdx = cursor.getColumnIndexOrThrow(Telephony.Mms.DATE)
                val boxIdx = cursor.getColumnIndexOrThrow(Telephony.Mms.MESSAGE_BOX)
                val readIdx = cursor.getColumnIndexOrThrow(Telephony.Mms.READ)

                while (cursor.moveToNext()) {
                    val mmsId = cursor.getLong(idIdx)
                    val threadId = cursor.getLong(threadIdx)
                    val rawDate = cursor.getLong(dateIdx)
                    val dateMillis = if (rawDate < 10_000_000_000L) rawDate * 1000L else rawDate
                    val msgBox = cursor.getInt(boxIdx)
                    val isRead = cursor.getInt(readIdx) == 1
                    val isIncoming = msgBox == Telephony.Mms.MESSAGE_BOX_INBOX

                    if (isIncoming && !isRead) {
                        unreadCountByThread[threadId] = (unreadCountByThread[threadId] ?: 0) + 1
                    }

                    val address = queryMmsAddressSafely(context, mmsId) ?: "Unknown"
                    val (mmsText, mmsAttachment) = queryMmsPartsSafely(context, mmsId)
                    val isSr = TxturaStrings.isSerbianLocale()
                    val fallbackBody = if (isSr) "MMS Poruka" else "MMS Message"
                    val cleanBody = MessageSecurityAnalyzer.sanitizeMessageText(
                        mmsText.ifBlank { fallbackBody }
                    )
                    val uniqueMsgId = 1_000_000_000L + mmsId

                    outMessages.add(
                        ChatMessage(
                            id = uniqueMsgId,
                            threadId = threadId,
                            address = address,
                            body = cleanBody,
                            timestampMillis = dateMillis,
                            isIncoming = isIncoming,
                            type = if (mmsAttachment != null) MessageType.MMS else MessageType.SMS,
                            status = DeliveryStatus.DELIVERED,
                            isPinned = uniqueMsgId in pinnedMessageIds,
                            isHighlighted = uniqueMsgId in highlightedMessageIds,
                            attachment = mmsAttachment,
                            securityAnalysis = MessageSecurityAnalyzer.analyzeMessage(cleanBody)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to query Telephony.Mms history", e)
        }
    }

    private fun queryMmsAddressSafely(context: Context, mmsId: Long): String? {
        val addrUri = "content://mms/$mmsId/addr".toUri()
        return try {
            context.contentResolver.query(
                addrUri,
                arrayOf(Telephony.Mms.Addr.ADDRESS, Telephony.Mms.Addr.TYPE),
                null,
                null,
                null
            )?.use { cursor ->
                var fallbackAddr: String? = null
                while (cursor.moveToNext()) {
                    val addr = cursor.getString(0).orEmpty()
                    val type = cursor.getInt(1)
                    if (addr.isNotBlank() && !addr.equals("insert-address-token", ignoreCase = true)) {
                        if (type == 137) return addr
                        if (fallbackAddr == null) fallbackAddr = addr
                    }
                }
                fallbackAddr
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun queryMmsPartsSafely(
        context: Context,
        mmsId: Long,
    ): Pair<String, MmsAttachment?> {
        val partUri = "content://mms/part".toUri()
        var textBody = ""
        var attachment: MmsAttachment? = null

        try {
            context.contentResolver.query(
                partUri,
                arrayOf("_id", "ct", "text", "cl"),
                "mid = ?",
                arrayOf(mmsId.toString()),
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val partId = cursor.getLong(0)
                    val contentType = cursor.getString(1).orEmpty()
                    val inlineText = cursor.getString(2).orEmpty()
                    val contentLocation = cursor.getString(3).orEmpty()

                    when {
                        contentType == "text/plain" -> {
                            if (inlineText.isNotBlank()) {
                                textBody = inlineText
                            }
                        }
                        contentType.startsWith("image/") -> {
                            val specificPartUri = "content://mms/part/$partId".toUri()
                            attachment = MmsAttachment(
                                uri = specificPartUri,
                                mimeType = contentType,
                                caption = contentLocation.ifBlank { "MMS Photo" }
                            )
                        }
                        contentType.startsWith("audio/", ignoreCase = true) ||
                        contentLocation.endsWith(".amr", ignoreCase = true) ||
                        contentLocation.endsWith(".3gp", ignoreCase = true) ||
                        contentLocation.endsWith(".m4a", ignoreCase = true) ||
                        contentLocation.endsWith(".aac", ignoreCase = true) ||
                        contentType.equals("application/ogg", ignoreCase = true) -> {
                            val specificPartUri = "content://mms/part/$partId".toUri()
                            attachment = MmsAttachment(
                                uri = specificPartUri,
                                mimeType = if (contentType.startsWith("audio/", ignoreCase = true)) contentType else "audio/amr",
                                caption = contentLocation.ifBlank { if (TxturaStrings.isSerbianLocale()) "Govorna poruka" else "Voice message" }
                            )
                        }
                        contentType.contains("calendar", ignoreCase = true) ||
                        contentLocation.endsWith(".ics", ignoreCase = true) ||
                        contentLocation.endsWith(".vcs", ignoreCase = true) -> {
                            val specificPartUri = "content://mms/part/$partId".toUri()
                            attachment = MmsAttachment(
                                uri = specificPartUri,
                                mimeType = contentType.ifBlank { "text/calendar" },
                                caption = contentLocation.ifBlank { if (TxturaStrings.isSerbianLocale()) "Kalendarski događaj" else "Calendar event" }
                            )
                        }
                        contentType != "application/smil" && !contentType.startsWith("text/vnd.wap.wml") && contentType.isNotBlank() -> {
                            val specificPartUri = "content://mms/part/$partId".toUri()
                            attachment = MmsAttachment(
                                uri = specificPartUri,
                                mimeType = contentType,
                                caption = contentLocation.ifBlank { if (TxturaStrings.isSerbianLocale()) "Dokument" else "Document" }
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore inaccessible MMS part
        }
        return textBody to attachment
    }

    fun resolveContactNameSafely(context: Context, phoneNumber: String): String? {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else {
                    null
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Contacts permission revoked at runtime", e)
            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Resolves the contact's photo thumbnail URI from the system Contacts provider
     * when READ_CONTACTS permission is granted.
     */
    fun resolveContactPhotoUriSafely(context: Context, phoneNumber: String): Uri? {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            context.contentResolver.query(
                uri,
                arrayOf(
                    ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI,
                    ContactsContract.PhoneLookup.PHOTO_URI
                ),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val thumbStr = cursor.getString(0)
                    val fullStr = cursor.getString(1)
                    val chosen = thumbStr?.takeIf { it.isNotBlank() }
                        ?: fullStr?.takeIf { it.isNotBlank() }
                    chosen?.toUri()
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Resolves the system Contacts lookup URI for a given phone number so the user can open
     * the contact card directly in the default Contacts app.
     */
    fun resolveContactLookupUriSafely(context: Context, phoneNumber: String): Uri? {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        return try {
            val lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            context.contentResolver.query(
                lookupUri,
                arrayOf(ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.LOOKUP_KEY),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(0)
                    val key = cursor.getString(1)
                    ContactsContract.Contacts.getLookupUri(id, key)
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Queries device contacts (when READ_CONTACTS is granted) combined with existing conversation
     * participants so the user can immediately pick a recipient when starting a New Message.
     */
    suspend fun loadAvailableContactsForPicker(context: Context): List<ContactPickerItem> =
        withContext(Dispatchers.IO) {
            val result = LinkedHashMap<String, ContactPickerItem>()
            val activeThreads = _threads.value

            // 1. Add existing conversation threads only if they have an actual contact name (not raw phone number)
            for (thread in activeThreads) {
                val hasRealName = thread.contactName.isNotBlank() &&
                    thread.contactName != thread.address &&
                    thread.contactName.any { it.isLetter() }
                if (hasRealName) {
                    val key = normalizeAddressKey(thread.address)
                    result[key] = ContactPickerItem(
                        name = thread.contactName,
                        address = thread.address,
                        initials = thread.avatarInitials,
                        photoUri = thread.contactPhotoUri,
                        existingThreadId = thread.id
                    )
                }
            }

            // 2. Query system Contacts if permission is granted
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                try {
                    val projection = arrayOf(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
                    )
                    context.contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        projection,
                        null,
                        null,
                        "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
                    )?.use { cursor ->
                        val nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val numIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        val photoIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

                        while (cursor.moveToNext()) {
                            val rawName = cursor.getString(nameIdx).orEmpty().trim()
                            val rawNum = cursor.getString(numIdx).orEmpty().trim()
                            val photoStr = cursor.getString(photoIdx)
                            val cleanNum = MessageSecurityAnalyzer.sanitizePhoneNumber(rawNum)
                            if (cleanNum.isNotBlank()) {
                                val key = normalizeAddressKey(cleanNum)
                                if (!result.containsKey(key)) {
                                    val displayName = rawName.ifBlank { cleanNum }
                                    val matchingThread = activeThreads.find {
                                        arePhoneNumbersEqual(it.address, cleanNum)
                                    }
                                    result[key] = ContactPickerItem(
                                        name = displayName,
                                        address = cleanNum,
                                        initials = computeInitials(displayName),
                                        photoUri = photoStr?.takeIf { it.isNotBlank() }?.toUri(),
                                        existingThreadId = matchingThread?.id
                                    )
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Ignore contact query failure
                }
            }

            result.values.toList()
        }

    fun sanitizeSenderOrRecipientAddress(rawAddress: String): String {
        val trimmed = rawAddress.trim()
        val hasLetters = trimmed.any { it.isLetter() }
        return if (hasLetters) {
            trimmed.filter { ch ->
                ch.isLetterOrDigit() || (ch in "+-_. ")
            }
                .trim()
                .take(28)
                .ifBlank { trimmed.take(24) }
        } else {
            MessageSecurityAnalyzer.sanitizePhoneNumber(trimmed)
                .ifBlank { trimmed.take(24) }
        }
    }

    fun findThreadByAddress(
        threads: List<ConversationThread>,
        rawAddress: String
    ): ConversationThread? {
        val cleanAddress = sanitizeSenderOrRecipientAddress(rawAddress)
        if (cleanAddress.isBlank()) return null
        val normKey = normalizeAddressKey(cleanAddress)
        return threads.find {
            arePhoneNumbersEqual(it.address, cleanAddress) ||
                it.address.equals(cleanAddress, ignoreCase = true) ||
                it.contactName.equals(cleanAddress, ignoreCase = true) ||
                (normKey.isNotBlank() && normalizeAddressKey(it.address) == normKey) ||
                (normKey.isNotBlank() && it.messages.any { m -> normalizeAddressKey(m.address) == normKey })
        }
    }

    /**
     * Finds an existing conversation thread with [recipientAddress] (restoring from Trash if needed)
     * or creates a new conversation thread if no previous chat exists with that person,
     * returning the thread ID so the chat screen can open immediately.
     */
    fun getOrCreateConversationWithRecipient(
        context: Context,
        recipientAddress: String,
        preferredName: String? = null
    ): Long {
        val cleanAddress = sanitizeSenderOrRecipientAddress(recipientAddress)

        // 1. Check active threads by address
        val existingActive = findThreadByAddress(_threads.value, cleanAddress)
        if (existingActive != null) {
            markThreadRead(context, existingActive.id)
            return existingActive.id
        }

        // 2. Check soft-deleted threads (restore if user starts a chat with them)
        val existingDeleted = _deletedThreads.value.find {
            arePhoneNumbersEqual(it.thread.address, cleanAddress) ||
                it.thread.address.equals(cleanAddress, ignoreCase = true) ||
                normalizeAddressKey(it.thread.address) == normalizeAddressKey(cleanAddress)
        }
        if (existingDeleted != null) {
            restoreDeletedThread(context, existingDeleted.thread.id)
            return existingDeleted.thread.id
        }

        val resolvedName = preferredName?.takeIf { it.isNotBlank() }
            ?: resolveContactNameSafely(context, cleanAddress)
            ?: cleanAddress

        // 3. Check active threads by resolved contact name (prevents duplicate contact threads)
        if (resolvedName != cleanAddress && resolvedName.any { it.isLetter() }) {
            val existingByName = _threads.value.find {
                it.contactName.trim().equals(resolvedName.trim(), ignoreCase = true)
            }
            if (existingByName != null) {
                markThreadRead(context, existingByName.id)
                return existingByName.id
            }
        }

        // 4. Create a brand-new conversation thread
        val newThreadId = nextId.incrementAndGet()
        val resolvedPhotoUri = resolveContactPhotoUriSafely(context, cleanAddress)

        val newThread = ConversationThread(
            id = newThreadId,
            contactName = resolvedName,
            address = cleanAddress,
            avatarInitials = computeInitials(resolvedName),
            avatarAccentColorHex = 0xFF00E5FF,
            contactPhotoUri = resolvedPhotoUri,
            messages = emptyList(),
            unreadCount = 0,
            isPinned = isAddressPinned(cleanAddress),
            isService = isAddressService(cleanAddress, emptyList(), resolvedName)
        )

        _threads.update { current ->
            sortThreads(listOf(newThread) + current)
        }
        return newThreadId
    }

    /**
     * Sends an SMS or MMS message securely and writes it to the system Telephony provider
     * when Txtura is the Default SMS App so conversation history persists across apps.
     */
    fun sendMessage(
        context: Context,
        recipientAddress: String,
        rawBody: String,
        attachmentUri: Uri? = null
    ): Long {
        val cleanAddress = MessageSecurityAnalyzer.sanitizePhoneNumber(recipientAddress)
            .ifBlank { recipientAddress.trim().take(24) }
        val cleanBody = MessageSecurityAnalyzer.sanitizeMessageText(rawBody)
        val isMms = attachmentUri != null
        val msgId = nextId.incrementAndGet()
        val now = System.currentTimeMillis()
        val isSr = TxturaStrings.isSerbianLocale()

        val existingThread = _threads.value.find {
            arePhoneNumbersEqual(it.address, cleanAddress) ||
                it.address.equals(cleanAddress, ignoreCase = true) ||
                it.contactName.equals(cleanAddress, ignoreCase = true)
        }
        val threadId = existingThread?.id ?: nextId.incrementAndGet()

        val requestDeliveryReport = isDeliveryReportsEnabled(context)
        val scAddress = getSmscAddress(context).takeIf { it.isNotBlank() }
        var deliveryStatus = if (requestDeliveryReport) DeliveryStatus.SENT else DeliveryStatus.DELIVERED

        if (!isMms && cleanBody.isNotBlank() && hasSendSmsPermission(context)) {
            try {
                val smsManager = context.getSystemService(SmsManager::class.java)
                if (smsManager != null && cleanAddress.any { it.isDigit() }) {
                    val sentIntent = Intent(ACTION_SMS_SENT).apply {
                        setPackage(context.packageName)
                        putExtra("message_id", msgId)
                    }
                    val sentPi = PendingIntent.getBroadcast(
                        context,
                        msgId.toInt(),
                        sentIntent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    val deliveredPi = if (requestDeliveryReport) {
                        val deliveredIntent = Intent(ACTION_SMS_DELIVERED).apply {
                            setPackage(context.packageName)
                            putExtra("message_id", msgId)
                        }
                        PendingIntent.getBroadcast(
                            context,
                            (msgId + 1).toInt(),
                            deliveredIntent,
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    } else null

                    val parts = smsManager.divideMessage(cleanBody)
                    if (parts.size > 1) {
                        val sentIntents = ArrayList(parts.map { sentPi })
                        val deliveredIntents = if (deliveredPi != null) ArrayList(parts.map { deliveredPi }) else null
                        smsManager.sendMultipartTextMessage(
                            cleanAddress,
                            scAddress,
                            parts,
                            sentIntents,
                            deliveredIntents
                        )
                    } else {
                        smsManager.sendTextMessage(
                            cleanAddress,
                            scAddress,
                            cleanBody,
                            sentPi,
                            deliveredPi
                        )
                    }
                    if (!requestDeliveryReport) {
                        deliveryStatus = DeliveryStatus.DELIVERED
                    }

                    persistMessageToSystemProviderIfDefault(
                        context = context,
                        address = cleanAddress,
                        body = cleanBody,
                        timestampMillis = now,
                        messageType = Telephony.Sms.MESSAGE_TYPE_SENT,
                        read = 1
                    )
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException while sending SMS", e)
            } catch (e: Exception) {
                Log.w(TAG, "Hardware SMS send fallback to local thread", e)
            }
        } else if (isMms) {
            try {
                if (!hasSendSmsPermission(context)) {
                    Log.w(TAG, "MMS send blocked: SEND_SMS permission is not granted")
                    deliveryStatus = DeliveryStatus.FAILED
                } else if (!isDataAllowedForMms(context)) {
                    Log.w(TAG, "Cellular MMS send blocked: mobile data disabled for MMS according to user settings")
                    deliveryStatus = DeliveryStatus.FAILED
                } else {
                    val requestReadReport = isMmsRequestReadReports(context)
                    val creationMode = getMmsCreationMode(context)

                    val uriMime = try { context.contentResolver.getType(attachmentUri) } catch (_: Exception) { null }
                    val isAudioAttachment = uriMime?.startsWith("audio/", ignoreCase = true) == true ||
                        attachmentUri.path?.endsWith(".amr", ignoreCase = true) == true ||
                        attachmentUri.path?.endsWith(".3gp", ignoreCase = true) == true ||
                        attachmentUri.toString().contains("voice_msg") ||
                        attachmentUri.toString().contains("mms_audio")

                    val isCalendarAttachment = !isAudioAttachment && (
                        uriMime?.contains("calendar", ignoreCase = true) == true ||
                        attachmentUri.path?.endsWith(".ics", ignoreCase = true) == true ||
                        attachmentUri.path?.endsWith(".vcs", ignoreCase = true) == true ||
                        attachmentUri.toString().contains("mms_cal")
                    )

                    val isDocumentAttachment = !isAudioAttachment && !isCalendarAttachment && (
                        uriMime?.startsWith("image/") != true &&
                        attachmentUri.path?.endsWith(".jpg", ignoreCase = true) != true &&
                        attachmentUri.path?.endsWith(".jpeg", ignoreCase = true) != true &&
                        attachmentUri.path?.endsWith(".png", ignoreCase = true) != true &&
                        attachmentUri.path?.endsWith(".webp", ignoreCase = true) != true &&
                        attachmentUri.path?.endsWith(".gif", ignoreCase = true) != true
                    )

                    val isImageAttachment = !isAudioAttachment && !isCalendarAttachment && !isDocumentAttachment

                    val imageBytes = if (isImageAttachment) {
                        attachmentUri.let { MmsPduHelper.downscaleImageForMms(context, it, creationMode) }
                    } else null

                    val audioBytes = if (isAudioAttachment) {
                        try {
                            context.contentResolver.openInputStream(attachmentUri)?.use { it.readBytes() }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to read audio attachment bytes", e)
                            null
                        }
                    } else null

                    val otherBytes = if (isCalendarAttachment || isDocumentAttachment) {
                        try {
                            context.contentResolver.openInputStream(attachmentUri)?.use { it.readBytes() }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to read attachment bytes", e)
                            null
                        }
                    } else null

                    val otherMimeType = when {
                        isCalendarAttachment -> "text/calendar"
                        isDocumentAttachment -> uriMime ?: "application/octet-stream"
                        else -> null
                    }

                    val otherFileName = when {
                        isCalendarAttachment -> attachmentUri.lastPathSegment?.substringAfterLast('/')?.takeIf {
                            it.endsWith(".ics", true) || it.endsWith(".vcs", true)
                        } ?: "event.ics"
                        isDocumentAttachment -> DocumentAttachmentHelper.getFileName(context, attachmentUri)
                        else -> null
                    }

                    val pduBytes = MmsPduHelper.buildSendReqPdu(
                        context = context,
                        recipient = cleanAddress,
                        bodyText = cleanBody.takeIf { it.isNotBlank() },
                        imageBytes = imageBytes,
                        audioBytes = audioBytes,
                        audioMimeType = "audio/amr",
                        otherBytes = otherBytes,
                        otherMimeType = otherMimeType,
                        otherFileName = otherFileName,
                        requestDeliveryReport = requestDeliveryReport,
                        requestReadReport = requestReadReport,
                    )

                    val pduFile = File(context.cacheDir, "mms_send_$msgId.pdu")
                    pduFile.writeBytes(pduBytes)

                    val pduUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        pduFile
                    )

                    try {
                        context.grantUriPermission(
                            "com.android.mms.service",
                            pduUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: Exception) {}
                    try {
                        context.grantUriPermission(
                            "com.android.phone",
                            pduUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: Exception) {}

                    val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        context.getSystemService(SmsManager::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        SmsManager.getDefault()
                    }

                    val sentIntent = Intent(ACTION_MMS_SENT).apply {
                        setPackage(context.packageName)
                        putExtra("message_id", msgId)
                        putExtra("pdu_path", pduFile.absolutePath)
                    }
                    val sentPi = PendingIntent.getBroadcast(
                        context,
                        msgId.toInt(),
                        sentIntent,
                        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )

                    val maxBytesLimit = when (creationMode) {
                        MmsCreationMode.RESTRICTED -> 100 * 1024
                        MmsCreationMode.WARNING -> 300 * 1024
                        MmsCreationMode.FREE -> 600 * 1024
                    }

                    val configOverrides = android.os.Bundle().apply {
                        putBoolean(SmsManager.MMS_CONFIG_MMS_DELIVERY_REPORT_ENABLED, requestDeliveryReport)
                        putBoolean(SmsManager.MMS_CONFIG_MMS_READ_REPORT_ENABLED, requestReadReport)
                        putInt(SmsManager.MMS_CONFIG_MAX_MESSAGE_SIZE, maxBytesLimit)
                        putBoolean(SmsManager.MMS_CONFIG_SUPPORT_MMS_CONTENT_DISPOSITION, true)
                    }

                    val activeSmsManager = smsManager
                        ?: throw IllegalStateException("SmsManager is unavailable for MMS sending")
                    activeSmsManager.sendMultimediaMessage(
                        context,
                        pduUri,
                        null,
                        configOverrides,
                        sentPi
                    )
                    deliveryStatus = if (requestDeliveryReport) DeliveryStatus.SENT else DeliveryStatus.DELIVERED

                    // Persist outgoing sent MMS to system Telephony.Mms provider if default SMS app
                    persistMmsToSystemProviderIfDefault(
                        context = context,
                        senderAddress = cleanAddress,
                        bodyText = cleanBody,
                        imageBytes = imageBytes,
                        imageMimeType = "image/jpeg",
                        timestampMillis = now,
                        isIncoming = false,
                        audioBytes = audioBytes,
                        audioMimeType = "audio/amr",
                        otherBytes = otherBytes,
                        otherMimeType = otherMimeType,
                        otherFileName = otherFileName
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send MMS message over cellular network", e)
                deliveryStatus = DeliveryStatus.FAILED
            }
        }

        val resolvedContactName = resolveContactNameSafely(context, cleanAddress) ?: cleanAddress
        val resolvedPhotoUri = resolveContactPhotoUriSafely(context, cleanAddress)

        val uriMime = attachmentUri?.let { try { context.contentResolver.getType(it) } catch (_: Exception) { null } }
        val isAudioAttachment = uriMime?.startsWith("audio/", ignoreCase = true) == true ||
            attachmentUri?.path?.endsWith(".amr", ignoreCase = true) == true ||
            attachmentUri?.path?.endsWith(".3gp", ignoreCase = true) == true ||
            attachmentUri?.toString()?.contains("voice_msg") == true ||
            attachmentUri?.toString()?.contains("mms_audio") == true

        val isCalendarAttachment = !isAudioAttachment && (
            uriMime?.contains("calendar", ignoreCase = true) == true ||
            attachmentUri?.path?.endsWith(".ics", ignoreCase = true) == true ||
            attachmentUri?.path?.endsWith(".vcs", ignoreCase = true) == true ||
            attachmentUri?.toString()?.contains("mms_cal") == true
        )

        val isDocumentAttachment = !isAudioAttachment && !isCalendarAttachment && (
            uriMime?.startsWith("image/") != true &&
            attachmentUri?.path?.endsWith(".jpg", ignoreCase = true) != true &&
            attachmentUri?.path?.endsWith(".jpeg", ignoreCase = true) != true &&
            attachmentUri?.path?.endsWith(".png", ignoreCase = true) != true &&
            attachmentUri?.path?.endsWith(".webp", ignoreCase = true) != true &&
            attachmentUri?.path?.endsWith(".gif", ignoreCase = true) != true
        )

        val defaultMmsText = when {
            isAudioAttachment -> if (isSr) "Govorna poruka" else "Voice message"
            isCalendarAttachment -> if (isSr) "Kalendarski događaj" else "Calendar event"
            isDocumentAttachment -> attachmentUri?.let { DocumentAttachmentHelper.getFileName(context, it) } ?: (if (isSr) "Dokument" else "Document")
            else -> if (isSr) "MMS Fotografija" else "MMS Photo"
        }
        val attachmentMimeType = when {
            isAudioAttachment -> "audio/amr"
            isCalendarAttachment -> "text/calendar"
            isDocumentAttachment -> uriMime ?: "application/octet-stream"
            else -> "image/jpeg"
        }

        val newMessage = ChatMessage(
            id = msgId,
            threadId = threadId,
            address = cleanAddress,
            body = cleanBody.ifBlank { if (isMms) defaultMmsText else "" },
            timestampMillis = now,
            isIncoming = false,
            type = if (isMms) MessageType.MMS else MessageType.SMS,
            status = deliveryStatus,
            attachment = attachmentUri?.let {
                MmsAttachment(
                    uri = it,
                    mimeType = attachmentMimeType,
                    caption = defaultMmsText
                )
            },
            securityAnalysis = MessageSecurityAnalyzer.analyzeMessage(cleanBody)
        )

        _threads.update { current ->
            val updated = if (existingThread != null) {
                current.map { thread ->
                    if (thread.id == existingThread.id) {
                        thread.copy(
                            messages = thread.messages + newMessage,
                            unreadCount = 0
                        )
                    } else {
                        thread
                    }
                }
            } else {
                val newThread = ConversationThread(
                    id = threadId,
                    contactName = resolvedContactName,
                    address = cleanAddress,
                    avatarInitials = computeInitials(resolvedContactName),
                    avatarAccentColorHex = 0xFF00E5FF,
                    contactPhotoUri = resolvedPhotoUri,
                    messages = listOf(newMessage),
                    unreadCount = 0,
                    isPinned = isAddressPinned(cleanAddress),
                    isService = isAddressService(cleanAddress, listOf(newMessage), resolvedContactName)
                )
                listOf(newThread) + current
            }
            sortThreads(updated)
        }

        verifyMessageUrlsOnlineAsync(context, threadId, msgId, cleanBody)
        saveDiskCacheAsync(context, immediate = true, historyThreadIds = setOf(threadId))

        return threadId
    }

    /**
     * Schedules an SMS/MMS message to be sent automatically at [scheduledAtMillis].
     */
    fun scheduleMessage(
        context: Context,
        threadId: Long,
        recipientAddress: String,
        rawBody: String,
        attachmentUri: Uri?,
        scheduledAtMillis: Long,
    ): ScheduledSmsMessage {
        val appContext = context.applicationContext
        val scheduledId = nextId.getAndIncrement()
        val entry = ScheduledSmsMessage(
            id = scheduledId,
            threadId = threadId,
            recipientAddress = recipientAddress.trim(),
            body = rawBody.trim(),
            attachmentUri = attachmentUri,
            scheduledAtMillis = scheduledAtMillis,
        )
        _scheduledMessages.update { current ->
            (current + entry).sortedBy { it.scheduledAtMillis }
        }
        val job = ioScope.launch {
            val waitMillis = (scheduledAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
            delay(waitMillis.milliseconds)
            val stillScheduled = _scheduledMessages.value.any { it.id == scheduledId }
            if (stillScheduled) {
                _scheduledMessages.update { list -> list.filterNot { it.id == scheduledId } }
                scheduledJobs.remove(scheduledId)
                sendMessage(
                    context = appContext,
                    recipientAddress = entry.recipientAddress,
                    rawBody = entry.body,
                    attachmentUri = entry.attachmentUri,
                )
            }
        }
        scheduledJobs[scheduledId] = job
        return entry
    }

    /**
     * Cancels a previously scheduled SMS/MMS message before its scheduled time arrives.
     */
    fun cancelScheduledMessage(scheduledId: Long) {
        scheduledJobs.remove(scheduledId)?.cancel()
        _scheduledMessages.update { list -> list.filterNot { it.id == scheduledId } }
    }

    /**
     * Called by SmsDeliverReceiver or MmsWapPushReceiver when a new message arrives.
     * Drops messages from blocked senders, persists to the system Telephony provider when
     * Txtura is the Default SMS App, and posts a high-importance system notification.
     */
    fun onIncomingMessageReceived(
        context: Context? = null,
        senderAddress: String,
        rawBody: String,
        isMms: Boolean = false
    ) {
        val cleanAddress = sanitizeSenderOrRecipientAddress(senderAddress)

        context?.let(::loadPersistedPins)
        context?.let(::loadDiskCache)
        if (isSenderBlocked(cleanAddress)) {
            Log.i(TAG, "Dropped incoming message from blocked sender: $cleanAddress")
            return
        }

        // If the sender's conversation was in Trash, restore it so the new message appears in the inbox
        if (context != null) {
            val deletedMatch = _deletedThreads.value.find {
                arePhoneNumbersEqual(it.thread.address, cleanAddress) ||
                    it.thread.address.equals(cleanAddress, ignoreCase = true) ||
                    normalizeAddressKey(it.thread.address) == normalizeAddressKey(cleanAddress)
            }
            deletedMatch?.let {
                restoreDeletedThread(context, it.thread.id)
            }
        }

        val cleanBody = MessageSecurityAnalyzer.sanitizeMessageText(rawBody)
        val now = System.currentTimeMillis()
        val msgId = nextId.incrementAndGet()

        if ((context != null) && !isMms) {
            persistMessageToSystemProviderIfDefault(
                context = context,
                address = cleanAddress,
                body = cleanBody,
                timestampMillis = now,
                messageType = Telephony.Sms.MESSAGE_TYPE_INBOX,
                read = 0
            )
        }

        val resolvedName = context?.let { resolveContactNameSafely(it, cleanAddress) } ?: cleanAddress
        val resolvedPhotoUri = context?.let { resolveContactPhotoUriSafely(it, cleanAddress) }
        val securityResult = MessageSecurityAnalyzer.analyzeMessage(cleanBody)
        var targetThreadId = 0L

        _threads.update { current ->
            val existingIndex = current.indexOfFirst {
                arePhoneNumbersEqual(it.address, cleanAddress) ||
                    it.address.equals(cleanAddress, ignoreCase = true) ||
                    normalizeAddressKey(it.address) == normalizeAddressKey(cleanAddress)
            }
            val existingId = if (existingIndex >= 0) current[existingIndex].id else null
            val threadId = existingId ?: nextId.incrementAndGet()
            targetThreadId = threadId
            val incomingMessage = ChatMessage(
                id = msgId,
                threadId = threadId,
                address = cleanAddress,
                body = cleanBody,
                timestampMillis = now,
                isIncoming = true,
                type = if (isMms) MessageType.MMS else MessageType.SMS,
                status = DeliveryStatus.DELIVERED,
                securityAnalysis = securityResult
            )

            val updated = if (existingId != null) {
                current.map { thread ->
                    if (thread.id == existingId) {
                        thread.copy(
                            messages = thread.messages + incomingMessage,
                            unreadCount = thread.unreadCount + 1
                        )
                    } else {
                        thread
                    }
                }
            } else {
                val created = ConversationThread(
                    id = threadId,
                    contactName = resolvedName,
                    address = cleanAddress,
                    avatarInitials = computeInitials(resolvedName),
                    avatarAccentColorHex = 0xFF6366F1,
                    contactPhotoUri = resolvedPhotoUri,
                    messages = listOf(incomingMessage),
                    unreadCount = 1,
                    isPinned = isAddressPinned(cleanAddress),
                    isService = isAddressService(cleanAddress, listOf(incomingMessage), resolvedName)
                )
                listOf(created) + current
            }
            sortThreads(updated)
        }

        context?.let { ctx ->
            saveDiskCacheAsync(ctx, immediate = true, historyThreadIds = setOf(targetThreadId))
            val isCurrentActiveThread = (activeForegroundThreadId != null && activeForegroundThreadId == targetThreadId)
            if (!isCurrentActiveThread) {
                postIncomingMessageNotification(
                    context = ctx,
                    threadId = targetThreadId,
                    messageId = msgId,
                    senderName = resolvedName,
                    senderAddress = cleanAddress,
                    messageBody = cleanBody,
                    securityAnalysis = securityResult,
                )
            }
            verifyMessageUrlsOnlineAsync(ctx, targetThreadId, msgId, cleanBody)
        }
    }

    /**
     * Checks if data access is allowed for MMS transactions under the user's [MmsDataDisabledMode].
     */
    fun isDataAllowedForMms(context: Context): Boolean {
        val mode = getMmsDataDisabledMode(context)
        val telephonyManager = context.getSystemService(TelephonyManager::class.java)
        val isRoaming = telephonyManager?.isNetworkRoaming == true
        return when (mode) {
            MmsDataDisabledMode.ALWAYS -> true
            MmsDataDisabledMode.NOT_ROAMING -> !isRoaming
            MmsDataDisabledMode.NEVER -> isWifiConnected(context)
        }
    }

    /**
     * Handles incoming WAP Push MMS Notifications (`m-notification-ind`, 0x82).
     * Enforces [MmsAutoRetrieveMode] and [MmsDataDisabledMode] settings:
     * - If Auto-retrieve is permitted, triggers automatic carrier MMSC download.
     * - If Auto-retrieve is disabled or in roaming, records a pending MMS item with a manual "Download" affordance.
     */
    fun handleIncomingMmsWapPush(
        context: Context,
        pushData: ByteArray,
        subId: Int
    ) {
        initializePersistedState(context)
        val notif = MmsPduHelper.parseNotificationInd(pushData)
        if (notif == null) {
            Log.e(TAG, "Failed to parse MMS NotificationInd from WAP Push payload")
            return
        }

        val rawSender = notif.senderAddress ?: "MMS"
        val senderAddress = sanitizeSenderOrRecipientAddress(rawSender)

        if (isSenderBlocked(senderAddress)) {
            Log.i(TAG, "Dropped MMS notification from blocked sender: $senderAddress")
            return
        }

        val autoRetrieveMode = getMmsAutoRetrieveMode(context)
        val telephonyManager = context.getSystemService(TelephonyManager::class.java)
        val isRoaming = telephonyManager?.isNetworkRoaming == true

        val shouldAutoRetrieve = when (autoRetrieveMode) {
            MmsAutoRetrieveMode.ALWAYS -> true
            MmsAutoRetrieveMode.NOT_ROAMING -> !isRoaming
            MmsAutoRetrieveMode.DISABLED -> false
        }

        val isDataAllowed = isDataAllowedForMms(context)

        if (shouldAutoRetrieve && isDataAllowed) {
            downloadMms(
                context = context,
                contentLocation = notif.contentLocation,
                senderAddress = senderAddress,
                subId = subId,
                existingMessageId = null
            )
        } else {
            val sizeKb = (notif.messageSize / 1024).coerceAtLeast(1)
            val isSr = TxturaStrings.isSerbianLocale()
            val pendingBody = if (isSr) "MMS poruka ($sizeKb KB)" else "MMS message ($sizeKb KB)"
            createPendingMmsMessage(
                context = context,
                senderAddress = senderAddress,
                bodyText = pendingBody,
                contentLocation = notif.contentLocation,
                messageSize = notif.messageSize,
                subId = subId
            )
        }
    }

    /**
     * Downloads an MMS message from the carrier's MMSC via [SmsManager.downloadMultimediaMessage].
     */
    fun downloadMms(
        context: Context,
        contentLocation: String,
        senderAddress: String,
        subId: Int = -1,
        existingMessageId: Long? = null
    ) {
        try {
            val tempFile = File(context.cacheDir, "mms_recv_${System.currentTimeMillis()}_${(1000..9999).random()}.pdu")
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )

            try {
                context.grantUriPermission(
                    "com.android.mms.service",
                    contentUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {}
            try {
                context.grantUriPermission(
                    "com.android.phone",
                    contentUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {}

            val downloadIntent = Intent(ACTION_MMS_DOWNLOADED).apply {
                setPackage(context.packageName)
                data = contentUri
                putExtra("pdu_path", tempFile.absolutePath)
                putExtra("content_location", contentLocation)
                putExtra("sender_address", senderAddress)
                putExtra("sub_id", subId)
                if (existingMessageId != null) {
                    putExtra("existing_message_id", existingMessageId)
                }
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                contentLocation.hashCode(),
                downloadIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val smsManager = if (subId >= 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java).createForSubscriptionId(subId)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getSmsManagerForSubscriptionId(subId)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
            }

            val creationMode = getMmsCreationMode(context)
            val maxBytesLimit = when (creationMode) {
                MmsCreationMode.RESTRICTED -> 100 * 1024
                MmsCreationMode.WARNING -> 300 * 1024
                MmsCreationMode.FREE -> 600 * 1024
            }

            val configOverrides = android.os.Bundle().apply {
                putInt(SmsManager.MMS_CONFIG_MAX_MESSAGE_SIZE, maxBytesLimit)
                putBoolean(SmsManager.MMS_CONFIG_SUPPORT_MMS_CONTENT_DISPOSITION, true)
            }

            smsManager?.downloadMultimediaMessage(
                context,
                contentLocation,
                contentUri,
                configOverrides,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initiate MMS download over telephony service", e)
            if (existingMessageId != null) {
                updateMessageDeliveryStatus(existingMessageId, DeliveryStatus.FAILED)
            }
        }
    }

    /**
     * Invoked when the user manually taps "Download MMS" in conversation view for a pending MMS.
     */
    fun downloadPendingMms(context: Context, messageId: Long) {
        val targetMsg = _threads.value.asSequence()
            .flatMap { it.messages.asSequence() }
            .find { it.id == messageId } ?: return

        val locationUrl = targetMsg.attachment?.uri?.toString()
        if (locationUrl.isNullOrBlank()) return

        updateMessageDeliveryStatus(messageId, DeliveryStatus.DOWNLOADING, context)

        downloadMms(
            context = context,
            contentLocation = locationUrl,
            senderAddress = targetMsg.address,
            subId = -1,
            existingMessageId = messageId
        )
    }

    /**
     * Invoked when [SmsManager.downloadMultimediaMessage] completes.
     * Parses the downloaded PDU, saves persistent media files, updates UI threads,
     * persists to system Telephony.Mms, and posts an incoming notification.
     */
    fun onMmsDownloaded(
        context: Context,
        resultCode: Int,
        pduPath: String?,
        fallbackSender: String,
        existingMessageId: Long?
    ) {
        if (pduPath.isNullOrBlank()) return
        val pduFile = File(pduPath)

        if (resultCode != android.app.Activity.RESULT_OK) {
            Log.w(TAG, "MMS download failed with resultCode: $resultCode")
            if (existingMessageId != null) {
                updateMessageDeliveryStatus(existingMessageId, DeliveryStatus.FAILED, context)
            }
            try { pduFile.delete() } catch (_: Exception) {}
            return
        }

        try {
            val pduBytes = pduFile.readBytes()
            val downloaded = MmsPduHelper.parseRetrieveConf(pduBytes)
            if (downloaded == null) {
                Log.e(TAG, "Failed to parse downloaded MMS RetrieveConf PDU")
                if (existingMessageId != null) {
                    updateMessageDeliveryStatus(existingMessageId, DeliveryStatus.FAILED, context)
                }
                return
            }

            val senderAddress = sanitizeSenderOrRecipientAddress(
                downloaded.senderAddress.ifBlank { fallbackSender }
            )
            val isSr = TxturaStrings.isSerbianLocale()
            val defaultPhotoText = if (isSr) "MMS Fotografija" else "MMS Photo"
            val rawText = downloaded.bodyText.orEmpty().trim()
            val cleanBody = MessageSecurityAnalyzer.sanitizeMessageText(
                rawText.ifBlank { downloaded.subject?.trim().orEmpty().ifBlank { defaultPhotoText } }
            )

            var attachment: MmsAttachment? = null
            if (downloaded.imageBytes != null && downloaded.imageBytes.isNotEmpty()) {
                val mediaDir = File(context.filesDir, "mms_media").apply { mkdirs() }
                val ext = if (downloaded.imageMimeType?.contains("png", true) == true) "png" else "jpg"
                val imageFile = File(mediaDir, "mms_in_${System.currentTimeMillis()}.$ext")
                imageFile.writeBytes(downloaded.imageBytes)
                val imageUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
                attachment = MmsAttachment(
                    uri = imageUri,
                    mimeType = downloaded.imageMimeType ?: "image/jpeg",
                    caption = defaultPhotoText
                )
            } else if (downloaded.audioBytes != null && downloaded.audioBytes.isNotEmpty()) {
                val mediaDir = File(context.filesDir, "mms_media").apply { mkdirs() }
                val audioFile = File(mediaDir, "mms_audio_${System.currentTimeMillis()}.amr")
                audioFile.writeBytes(downloaded.audioBytes)
                val audioUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", audioFile)
                val voiceText = if (isSr) "Govorna poruka" else "Voice message"
                attachment = MmsAttachment(
                    uri = audioUri,
                    mimeType = downloaded.audioMimeType ?: "audio/amr",
                    caption = voiceText
                )
            } else if (downloaded.otherBytes != null && downloaded.otherBytes.isNotEmpty()) {
                val mediaDir = File(context.filesDir, "mms_media").apply { mkdirs() }
                val mime = downloaded.otherMimeType ?: "application/octet-stream"
                val rawName = downloaded.otherFileName ?: "attachment"
                val ext = when {
                    mime.contains("calendar", true) || rawName.endsWith(".ics", true) -> "ics"
                    rawName.endsWith(".vcs", true) -> "vcs"
                    mime.contains("pdf", true) || rawName.endsWith(".pdf", true) -> "pdf"
                    mime.contains("sheet", true) || rawName.endsWith(".xls", true) -> "xls"
                    mime.contains("word", true) || rawName.endsWith(".doc", true) -> "doc"
                    mime.contains("text", true) || rawName.endsWith(".txt", true) -> "txt"
                    else -> rawName.substringAfterLast('.', "dat")
                }
                val base = rawName.substringBeforeLast('.').replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "mms_file" }
                val cleanFileName = if (rawName.contains('.')) rawName else "$base.$ext"
                val file = File(mediaDir, "mms_other_${System.currentTimeMillis()}_$cleanFileName")
                file.writeBytes(downloaded.otherBytes)
                val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val isCal = mime.contains("calendar", true) || cleanFileName.endsWith(".ics", true) || cleanFileName.endsWith(".vcs", true)
                val caption = if (isCal) {
                    if (isSr) "Kalendarski događaj" else "Calendar event"
                } else {
                    cleanFileName
                }
                attachment = MmsAttachment(
                    uri = fileUri,
                    mimeType = mime,
                    caption = caption
                )
            }

            // Persist to system Telephony.Mms if default SMS handler
            persistMmsToSystemProviderIfDefault(
                context = context,
                senderAddress = senderAddress,
                bodyText = cleanBody,
                imageBytes = downloaded.imageBytes,
                imageMimeType = downloaded.imageMimeType ?: "image/jpeg",
                timestampMillis = downloaded.timestampMillis,
                isIncoming = true,
                audioBytes = downloaded.audioBytes,
                audioMimeType = downloaded.audioMimeType ?: "audio/amr",
                otherBytes = downloaded.otherBytes,
                otherMimeType = downloaded.otherMimeType,
                otherFileName = downloaded.otherFileName
            )

            deliverDownloadedMms(
                context = context,
                senderAddress = senderAddress,
                bodyText = cleanBody,
                attachment = attachment,
                timestampMillis = downloaded.timestampMillis,
                existingMessageId = existingMessageId
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error processing downloaded MMS PDU", e)
            if (existingMessageId != null) {
                updateMessageDeliveryStatus(existingMessageId, DeliveryStatus.FAILED, context)
            }
        } finally {
            try { pduFile.delete() } catch (_: Exception) {}
        }
    }

    private fun createPendingMmsMessage(
        context: Context,
        senderAddress: String,
        bodyText: String,
        contentLocation: String,
        messageSize: Long,
        subId: Int
    ) {
        val cleanAddress = sanitizeSenderOrRecipientAddress(senderAddress)
        val now = System.currentTimeMillis()
        val msgId = nextId.incrementAndGet()
        val resolvedName = resolveContactNameSafely(context, cleanAddress) ?: cleanAddress
        val resolvedPhotoUri = resolveContactPhotoUriSafely(context, cleanAddress)
        val securityResult = MessageSecurityAnalyzer.analyzeMessage(bodyText)

        val pendingAttachment = MmsAttachment(
            uri = contentLocation.toUri(),
            mimeType = "application/vnd.wap.mms-message",
            caption = bodyText
        )

        var targetThreadId = 0L

        _threads.update { current ->
            val existingIndex = current.indexOfFirst {
                arePhoneNumbersEqual(it.address, cleanAddress) ||
                    it.address.equals(cleanAddress, ignoreCase = true) ||
                    normalizeAddressKey(it.address) == normalizeAddressKey(cleanAddress)
            }
            val existingId = if (existingIndex >= 0) current[existingIndex].id else null
            val threadId = existingId ?: nextId.incrementAndGet()
            targetThreadId = threadId

            val incomingMsg = ChatMessage(
                id = msgId,
                threadId = threadId,
                address = cleanAddress,
                body = bodyText,
                timestampMillis = now,
                isIncoming = true,
                type = MessageType.MMS,
                status = DeliveryStatus.PENDING,
                attachment = pendingAttachment,
                securityAnalysis = securityResult
            )

            val updated = if (existingId != null) {
                current.map { thread ->
                    if (thread.id == existingId) {
                        thread.copy(
                            messages = thread.messages + incomingMsg,
                            unreadCount = thread.unreadCount + 1
                        )
                    } else {
                        thread
                    }
                }
            } else {
                val created = ConversationThread(
                    id = threadId,
                    contactName = resolvedName,
                    address = cleanAddress,
                    avatarInitials = computeInitials(resolvedName),
                    avatarAccentColorHex = 0xFF6366F1,
                    contactPhotoUri = resolvedPhotoUri,
                    messages = listOf(incomingMsg),
                    unreadCount = 1,
                    isPinned = isAddressPinned(cleanAddress),
                    isService = isAddressService(cleanAddress, listOf(incomingMsg), resolvedName)
                )
                listOf(created) + current
            }
            sortThreads(updated)
        }

        saveDiskCacheAsync(context, immediate = true, historyThreadIds = setOf(targetThreadId))
        val isCurrentActiveThread = (activeForegroundThreadId != null && activeForegroundThreadId == targetThreadId)
        if (!isCurrentActiveThread) {
            postIncomingMessageNotification(
                context = context,
                threadId = targetThreadId,
                messageId = msgId,
                senderName = resolvedName,
                senderAddress = cleanAddress,
                messageBody = bodyText,
                securityAnalysis = securityResult
            )
        }
    }

    private fun deliverDownloadedMms(
        context: Context,
        senderAddress: String,
        bodyText: String,
        attachment: MmsAttachment?,
        timestampMillis: Long,
        existingMessageId: Long?
    ) {
        val cleanAddress = sanitizeSenderOrRecipientAddress(senderAddress)
        val resolvedName = resolveContactNameSafely(context, cleanAddress) ?: cleanAddress
        val resolvedPhotoUri = resolveContactPhotoUriSafely(context, cleanAddress)
        val securityResult = MessageSecurityAnalyzer.analyzeMessage(bodyText)
        var targetThreadId = 0L
        val now = if (timestampMillis > 0) timestampMillis else System.currentTimeMillis()
        val msgId = existingMessageId ?: nextId.incrementAndGet()

        _threads.update { current ->
            val existingIndex = current.indexOfFirst {
                arePhoneNumbersEqual(it.address, cleanAddress) ||
                    it.address.equals(cleanAddress, ignoreCase = true) ||
                    normalizeAddressKey(it.address) == normalizeAddressKey(cleanAddress)
            }
            val existingId = if (existingIndex >= 0) current[existingIndex].id else null
            val threadId = existingId ?: nextId.incrementAndGet()
            targetThreadId = threadId

            val deliveredMsg = ChatMessage(
                id = msgId,
                threadId = threadId,
                address = cleanAddress,
                body = bodyText,
                timestampMillis = now,
                isIncoming = true,
                type = MessageType.MMS,
                status = DeliveryStatus.DELIVERED,
                attachment = attachment,
                securityAnalysis = securityResult
            )

            val updated = if (existingId != null) {
                current.map { thread ->
                    if (thread.id == existingId) {
                        val messages = if (existingMessageId != null) {
                            thread.messages.map { if (it.id == existingMessageId) deliveredMsg else it }
                        } else {
                            thread.messages + deliveredMsg
                        }
                        val unread = if (existingMessageId != null) thread.unreadCount else (thread.unreadCount + 1)
                        thread.copy(messages = messages, unreadCount = unread)
                    } else {
                        thread
                    }
                }
            } else {
                val created = ConversationThread(
                    id = threadId,
                    contactName = resolvedName,
                    address = cleanAddress,
                    avatarInitials = computeInitials(resolvedName),
                    avatarAccentColorHex = 0xFF6366F1,
                    contactPhotoUri = resolvedPhotoUri,
                    messages = listOf(deliveredMsg),
                    unreadCount = 1,
                    isPinned = isAddressPinned(cleanAddress),
                    isService = isAddressService(cleanAddress, listOf(deliveredMsg), resolvedName)
                )
                listOf(created) + current
            }
            sortThreads(updated)
        }

        saveDiskCacheAsync(context, immediate = true, historyThreadIds = setOf(targetThreadId))
        val isCurrentActiveThread = (activeForegroundThreadId != null && activeForegroundThreadId == targetThreadId)
        if (!isCurrentActiveThread && existingMessageId == null) {
            postIncomingMessageNotification(
                context = context,
                threadId = targetThreadId,
                messageId = msgId,
                senderName = resolvedName,
                senderAddress = cleanAddress,
                messageBody = bodyText,
                securityAnalysis = securityResult
            )
        }
    }

    /**
     * Persists a sent or received MMS message and its text/media parts into Android's system [Telephony.Mms] provider.
     */
    fun persistMmsToSystemProviderIfDefault(
        context: Context,
        senderAddress: String,
        bodyText: String,
        imageBytes: ByteArray?,
        imageMimeType: String = "image/jpeg",
        timestampMillis: Long,
        isIncoming: Boolean,
        audioBytes: ByteArray? = null,
        audioMimeType: String = "audio/amr",
        otherBytes: ByteArray? = null,
        otherMimeType: String? = null,
        otherFileName: String? = null
    ): Long? {
        if (!isDefaultSmsApp(context)) return null
        return try {
            val resolver = context.contentResolver
            val threadId = Telephony.Threads.getOrCreateThreadId(context, senderAddress)
            val hasMedia = (imageBytes != null && imageBytes.isNotEmpty()) ||
                (audioBytes != null && audioBytes.isNotEmpty()) ||
                (otherBytes != null && otherBytes.isNotEmpty())
            val mmsValues = ContentValues().apply {
                put(Telephony.Mms.THREAD_ID, threadId)
                put(Telephony.Mms.DATE, timestampMillis / 1000L)
                put(Telephony.Mms.MESSAGE_BOX, if (isIncoming) Telephony.Mms.MESSAGE_BOX_INBOX else Telephony.Mms.MESSAGE_BOX_SENT)
                put(Telephony.Mms.READ, if (isIncoming) 0 else 1)
                put(Telephony.Mms.SEEN, if (isIncoming) 0 else 1)
                put(Telephony.Mms.CONTENT_TYPE, "application/vnd.wap.multipart.related")
                put(Telephony.Mms.MESSAGE_TYPE, if (isIncoming) 132 else 128)
                put(Telephony.Mms.TEXT_ONLY, if (!hasMedia) 1 else 0)
            }
            val insertUri = if (isIncoming) Telephony.Mms.Inbox.CONTENT_URI else Telephony.Mms.Sent.CONTENT_URI
            val mmsUri = resolver.insert(insertUri, mmsValues) ?: return null
            val mmsId = mmsUri.lastPathSegment?.toLongOrNull() ?: return null

            val addrValues = ContentValues().apply {
                put(Telephony.Mms.Addr.ADDRESS, senderAddress)
                put(Telephony.Mms.Addr.TYPE, if (isIncoming) 137 else 151)
                put(Telephony.Mms.Addr.CHARSET, 106)
            }
            resolver.insert("content://mms/$mmsId/addr".toUri(), addrValues)

            if (bodyText.isNotBlank()) {
                val textPartValues = ContentValues().apply {
                    put("mid", mmsId)
                    put("ct", "text/plain")
                    put("text", bodyText)
                    put("chset", 106)
                }
                resolver.insert("content://mms/$mmsId/part".toUri(), textPartValues)
            }

            if (imageBytes != null && imageBytes.isNotEmpty()) {
                val imgPartValues = ContentValues().apply {
                    put("mid", mmsId)
                    put("ct", imageMimeType)
                    put("cl", "image_01.jpg")
                }
                val partUri = resolver.insert("content://mms/$mmsId/part".toUri(), imgPartValues)
                if (partUri != null) {
                    resolver.openOutputStream(partUri)?.use { it.write(imageBytes) }
                }
            }

            if (audioBytes != null && audioBytes.isNotEmpty()) {
                val audioPartValues = ContentValues().apply {
                    put("mid", mmsId)
                    put("ct", audioMimeType)
                    put("cl", "voice_01.amr")
                }
                val partUri = resolver.insert("content://mms/$mmsId/part".toUri(), audioPartValues)
                if (partUri != null) {
                    resolver.openOutputStream(partUri)?.use { it.write(audioBytes) }
                }
            }

            if (otherBytes != null && otherBytes.isNotEmpty()) {
                val otherPartValues = ContentValues().apply {
                    put("mid", mmsId)
                    put("ct", otherMimeType ?: "application/octet-stream")
                    put("cl", otherFileName ?: "attachment.dat")
                }
                val partUri = resolver.insert("content://mms/$mmsId/part".toUri(), otherPartValues)
                if (partUri != null) {
                    resolver.openOutputStream(partUri)?.use { it.write(otherBytes) }
                }
            }
            mmsId
        } catch (e: Exception) {
            Log.w(TAG, "Failed to persist MMS to system Telephony.Mms provider", e)
            null
        }
    }

    fun updateMessageDeliveryStatus(messageId: Long, status: DeliveryStatus, context: Context? = null) {
        _threads.update { currentThreads ->
            currentThreads.map { thread ->
                if (thread.messages.any { it.id == messageId }) {
                    thread.copy(
                        messages = thread.messages.map { msg ->
                            if (msg.id == messageId) msg.copy(status = status) else msg
                        }
                    )
                } else {
                    thread
                }
            }
        }
        context?.let { ctx ->
            val threadId = _threads.value.firstOrNull { thread -> thread.messages.any { it.id == messageId } }?.id
            saveDiskCacheAsync(ctx, historyThreadIds = setOfNotNull(threadId))
        }
    }

    private fun postIncomingMessageNotification(
        context: Context,
        threadId: Long,
        messageId: Long,
        senderName: String,
        senderAddress: String,
        messageBody: String,
        securityAnalysis: SecurityAnalysisResult,
    ) {
        ensureNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotifPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasNotifPerm) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted; cannot display SMS notification")
                return
            }
        }

        val isSr = TxturaStrings.isSerbianLocale()
        val titleText = senderName

        val expandedBody = buildString {
            append(messageBody)
            securityAnalysis.extractedOtpCode?.let { otp ->
                append("\n")
                append(if (isSr) "Kod: $otp" else "Code: $otp")
            }
            if (securityAnalysis.riskLevel == SecurityRiskLevel.SUSPICIOUS) {
                append("\n")
                append(
                    if (isSr) "Upozorenje: Mogući fišing ili rizičan link"
                    else "Warning: Potential phishing or risky link"
                )
            }
        }

        val notificationId = (normalizeAddressKey(senderAddress).hashCode() and 0x7FFFFFFF).coerceAtLeast(1001)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_THREAD_NOTIFICATION
            data = "txtura://notification/$notificationId".toUri()
            putExtra(EXTRA_NOTIFICATION_THREAD_ID, threadId)
            putExtra(EXTRA_NOTIFICATION_ADDRESS, senderAddress)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val locStrings = TxturaStrings.forLocale()

        // 1. Direct Reply action from Notification (without opening the app)
        val remoteInput = RemoteInput.Builder(KEY_NOTIFICATION_REPLY_TEXT)
            .setLabel(locStrings.notificationReplyHint)
            .build()
        val replyIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_NOTIFICATION_DIRECT_REPLY
            putExtra(EXTRA_NOTIFICATION_THREAD_ID, threadId)
            putExtra(EXTRA_NOTIFICATION_ADDRESS, senderAddress)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val replyFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 10_000,
            replyIntent,
            replyFlags,
        )
        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_stat_message,
            locStrings.notificationReplyAction,
            replyPendingIntent,
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .setShowsUserInterface(false)
            .build()

        // 2. Mark as Read action from Notification
        val markReadIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_NOTIFICATION_MARK_READ
            putExtra(EXTRA_NOTIFICATION_THREAD_ID, threadId)
            putExtra(EXTRA_NOTIFICATION_ADDRESS, senderAddress)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val markReadPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 15_000,
            markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val markReadAction = NotificationCompat.Action.Builder(
            R.drawable.ic_stat_message,
            locStrings.notificationMarkReadAction,
            markReadPendingIntent,
        )
            .setShowsUserInterface(false)
            .build()

        val channelId = getActiveNotificationChannelId(context)
        val soundUri = getActiveNotificationSoundUri(context)
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_message)
            .setContentTitle(titleText)
            .setContentText(messageBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedBody))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setSound(soundUri)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(contentPendingIntent)
            .addAction(replyAction)
            .addAction(markReadAction)

        // Metadata Minimization: redact public lock-screen version so sender/content is never leaked
        if (AppSecurityManager.isMetadataMinimizationEnabled(context)) {
            val publicNotification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_stat_message)
                .setContentTitle("Txtura")
                .setContentText(if (isSr) "Nova poruka" else "New message")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()

            builder.setPublicVersion(publicNotification)
        }

        // 3. Copy 2FA / OTP Code action from Notification if a code is detected, otherwise Highlight action
        val extractedOtp = securityAnalysis.extractedOtpCode
        if (!extractedOtp.isNullOrBlank()) {
            val copyOtpIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_NOTIFICATION_COPY_OTP
                putExtra(EXTRA_NOTIFICATION_THREAD_ID, threadId)
                putExtra(EXTRA_NOTIFICATION_ADDRESS, senderAddress)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(EXTRA_NOTIFICATION_OTP_CODE, extractedOtp)
            }
            val copyOtpPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 20_000,
                copyOtpIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val copyOtpAction = NotificationCompat.Action.Builder(
                R.drawable.ic_stat_message,
                locStrings.notificationCopyCodeAction(extractedOtp),
                copyOtpPendingIntent,
            )
                .setShowsUserInterface(false)
                .build()
            builder.addAction(copyOtpAction)
        } else {
            val highlightIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_NOTIFICATION_HIGHLIGHT_MSG
                putExtra(EXTRA_NOTIFICATION_THREAD_ID, threadId)
                putExtra(EXTRA_NOTIFICATION_MESSAGE_ID, messageId)
                putExtra(EXTRA_NOTIFICATION_ADDRESS, senderAddress)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            val highlightPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 25_000,
                highlightIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val highlightAction = NotificationCompat.Action.Builder(
                R.drawable.ic_stat_message,
                locStrings.notificationHighlightAction,
                highlightPendingIntent,
            )
                .setShowsUserInterface(false)
                .build()
            builder.addAction(highlightAction)
        }

        val notification = builder.build()

        if (FlashNotificationManager.isFlashAlertEnabled(context)) {
            FlashNotificationManager.triggerFlashNotification(context)
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Unable to post incoming SMS notification", e)
        }
    }

    /**
     * Toggles read/unread status for a conversation.
     * If unreadCount > 0, marks as read (unreadCount = 0).
     * If unreadCount == 0, marks as unread (unreadCount = 1).
     */
    fun toggleThreadRead(context: Context, threadId: Long) {
        val current = _threads.value.find { it.id == threadId } ?: return
        if (current.unreadCount > 0) {
            markThreadRead(context, threadId, playSound = true)
        } else {
            markThreadUnread(context, threadId)
        }
    }

    /**
     * Marks a conversation as read both in memory and in the Android Telephony database.
     */
    fun markThreadRead(context: Context, threadId: Long, playSound: Boolean = true) {
        markThreadsRead(context, setOf(threadId), playSound)
    }

    /**
     * Marks a conversation as unread both in memory and in the Android Telephony database.
     */
    fun markThreadUnread(context: Context, threadId: Long) {
        _threads.update { list ->
            list.map {
                if (it.id == threadId) {
                    it.copy(unreadCount = (it.unreadCount + 1).coerceAtLeast(1))
                } else {
                    it
                }
            }
        }
        playMarkUnreadSound(context)
        val appCtx = context.applicationContext
        ioScope.launch {
            if (isDefaultSmsApp(appCtx)) {
                try {
                    val values = ContentValues().apply {
                        put(Telephony.Sms.READ, 0)
                        put(Telephony.Sms.SEEN, 0)
                    }
                    appCtx.contentResolver.update(
                        Telephony.Sms.CONTENT_URI,
                        values,
                        "${Telephony.Sms.THREAD_ID} = ?",
                        arrayOf(threadId.toString())
                    )
                } catch (_: Exception) {
                    // Ignore if not default SMS app or read-only provider
                }
            }
            saveDiskCacheAsync(appCtx)
        }
    }

    /**
     * Marks multiple conversations as read.
     */
    fun markThreadsRead(context: Context, threadIds: Set<Long>, playSound: Boolean = true) {
        if (threadIds.isEmpty()) return
        val affectedAddresses = mutableListOf<String>()
        var hadUnread = false
        _threads.update { list ->
            list.map {
                if (it.id in threadIds) {
                    if (it.unreadCount > 0) {
                        hadUnread = true
                    }
                    affectedAddresses.add(it.address)
                    it.copy(unreadCount = 0)
                } else {
                    it
                }
            }
        }
        if (hadUnread && playSound) {
            playMarkReadSound(context)
        }
        val appCtx = context.applicationContext
        try {
            val nm = NotificationManagerCompat.from(appCtx)
            affectedAddresses.forEach { addr ->
                val notifId = (normalizeAddressKey(addr).hashCode() and 0x7FFFFFFF).coerceAtLeast(1001)
                nm.cancel(notifId)
            }
        } catch (_: Exception) {
            // Ignore notification cancel issues
        }
        ioScope.launch {
            if (isDefaultSmsApp(appCtx)) {
                try {
                    val values = ContentValues().apply {
                        put(Telephony.Sms.READ, 1)
                        put(Telephony.Sms.SEEN, 1)
                    }
                    threadIds.forEach { tid ->
                        appCtx.contentResolver.update(
                            Telephony.Sms.CONTENT_URI,
                            values,
                            "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = ?",
                            arrayOf(tid.toString(), "0")
                        )
                    }
                } catch (_: Exception) {
                    // Ignore if not default SMS app or read-only provider
                }
            }
            saveDiskCacheAsync(appCtx)
        }
    }

    /**
     * Marks all conversations as read.
     */
    fun markAllThreadsRead(context: Context) {
        val allIds = _threads.value.asSequence().map { it.id }.toSet()
        markThreadsRead(context, allIds, playSound = true)
    }

    private fun persistMessageToSystemProviderIfDefault(
        context: Context,
        address: String,
        body: String,
        timestampMillis: Long,
        messageType: Int,
        read: Int
    ) {
        val appCtx = context.applicationContext
        ioScope.launch {
            if (!isDefaultSmsApp(appCtx)) return@launch
            try {
                val values = ContentValues().apply {
                    put(Telephony.Sms.ADDRESS, address)
                    put(Telephony.Sms.BODY, body)
                    put(Telephony.Sms.DATE, timestampMillis)
                    put(Telephony.Sms.TYPE, messageType)
                    put(Telephony.Sms.READ, read)
                    put(Telephony.Sms.SEEN, 1)
                }
                appCtx.contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
            } catch (e: Exception) {
                Log.w(TAG, "Unable to persist SMS to Telephony provider", e)
            }
        }
    }

    /**
     * Pins or unpins a contact's entire conversation thread so it stays at the top.
     */
    fun togglePinThread(context: Context, threadId: Long) {
        _threads.update { list ->
            val updated = list.map { thread ->
                if (thread.id == threadId) {
                    val newPinned = !thread.isPinned
                    val normKey = normalizeAddressKey(thread.address)
                    if (newPinned) {
                        pinnedAddresses.add(thread.address)
                        pinnedAddresses.add(normKey)
                        playPinSound(context)
                    } else {
                        pinnedAddresses.remove(thread.address)
                        pinnedAddresses.remove(normKey)
                        playUnpinSound(context)
                    }
                    thread.copy(isPinned = newPinned)
                } else {
                    thread
                }
            }
            sortThreads(updated)
        }
        savePersistedPins(context)
    }

    /**
     * Pins or unpins a specific message inside a contact's conversation.
     */
    fun togglePinMessage(context: Context, threadId: Long, messageId: Long) {
        _threads.update { list ->
            val updated = list.map { thread ->
                if (thread.id == threadId) {
                    val updatedMessages = thread.messages.map { msg ->
                        if (msg.id == messageId) {
                            val newPinned = !msg.isPinned
                            if (newPinned) {
                                pinnedMessageIds.add(msg.id)
                                playPinSound(context)
                            } else {
                                pinnedMessageIds.remove(msg.id)
                                playUnpinSound(context)
                            }
                            msg.copy(isPinned = newPinned)
                        } else {
                            msg
                        }
                    }
                    thread.copy(messages = updatedMessages)
                } else {
                    thread
                }
            }
            updated
        }
        savePersistedPins(context, historyThreadIds = setOf(threadId))
    }

    /**
     * Highlights or unhighlights a specific message inside a contact's conversation.
     */
    fun toggleHighlightMessage(context: Context, threadId: Long, messageId: Long) {
        _threads.update { list ->
            list.map { thread ->
                if (thread.id == threadId) {
                    val updatedMessages = thread.messages.map { msg ->
                        if (msg.id == messageId) {
                            val newHighlighted = !msg.isHighlighted
                            if (newHighlighted) {
                                highlightedMessageIds.add(msg.id)
                                playHighlightSound(context)
                            } else {
                                highlightedMessageIds.remove(msg.id)
                                playUnhighlightSound(context)
                            }
                            msg.copy(isHighlighted = newHighlighted)
                        } else {
                            msg
                        }
                    }
                    thread.copy(messages = updatedMessages)
                } else {
                    thread
                }
            }
        }
        savePersistedPins(context, historyThreadIds = setOf(threadId))
    }

    /**
     * Moves a single message from an active conversation thread to the Deleted Messages (Trash) folder.
     */
    fun deleteMessage(
        context: Context,
        threadId: Long,
        messageId: Long,
        retentionDays: Int = 30
    ) {
        deleteMessages(context, threadId, setOf(messageId), retentionDays)
    }

    /**
     * Moves multiple messages from an active conversation thread to the Deleted Messages (Trash) folder.
     */
    fun deleteMessages(
        context: Context,
        threadId: Long,
        messageIds: Collection<Long>,
        retentionDays: Int = 30
    ) {
        if (messageIds.isEmpty()) return
        val now = System.currentTimeMillis()
        val idsSet = messageIds.toSet()

        idsSet.forEach { id ->
            pinnedMessageIds.remove(id)
            highlightedMessageIds.remove(id)
            deletedMessageMeta[id] = Pair(threadId, now)
        }
        playDeleteSound(context)
        savePersistedPins(context, historyThreadIds = setOf(threadId))

        var trashedMessages = listOf<ChatMessage>()
        var baseThread: ConversationThread? = null

        _threads.update { list ->
            val updated = list.mapNotNull { thread ->
                if (thread.id == threadId) {
                    baseThread = thread
                    val found = thread.messages.filter { it.id in idsSet }
                    if (found.isNotEmpty()) {
                        trashedMessages = found
                    }
                    val remaining = thread.messages.filterNot { it.id in idsSet }
                    if (remaining.isEmpty() && thread.draftText.isNullOrBlank()) {
                        null
                    } else {
                        thread.copy(messages = remaining)
                    }
                } else {
                    thread
                }
            }
            sortThreads(updated)
        }

        val contactInfo = baseThread ?: _deletedThreads.value.find { it.thread.id == threadId }?.thread
        if (trashedMessages.isNotEmpty() || contactInfo != null) {
            _deletedThreads.update { currentTrash ->
                val existingEntry = currentTrash.find { it.thread.id == threadId }
                val newEntry = if (existingEntry != null) {
                    val combinedMsgs = (existingEntry.thread.messages + trashedMessages)
                        .distinctBy { it.id }
                        .sortedBy { it.timestampMillis }
                    existingEntry.copy(
                        thread = existingEntry.thread.copy(messages = combinedMsgs),
                        deletedAtMillis = now
                    )
                } else if (contactInfo != null) {
                    DeletedConversationEntry(
                        thread = contactInfo.copy(messages = trashedMessages),
                        deletedAtMillis = now
                    )
                } else {
                    null
                }

                if (newEntry != null) {
                    (listOf(newEntry) + currentTrash.filterNot { it.thread.id == threadId })
                        .sortedByDescending { it.deletedAtMillis }
                } else {
                    currentTrash
                }
            }
        }
        purgeExpiredDeletedThreads(context, retentionDays)
    }

    /**
     * Moves a conversation thread to the Deleted Messages (Trash) folder.
     */
    fun moveThreadToTrash(
        context: Context,
        threadId: Long,
        retentionDays: Int = 30
    ) {
        moveThreadsToTrash(context, setOf(threadId), retentionDays)
    }

    /**
     * Moves multiple conversation threads to the Deleted Messages (Trash) folder.
     */
    fun moveThreadsToTrash(
        context: Context,
        threadIds: Set<Long>,
        retentionDays: Int = 30
    ) {
        if (threadIds.isEmpty()) return
        val now = System.currentTimeMillis()
        val targets = _threads.value.filter { it.id in threadIds }
        if (targets.isEmpty()) return

        targets.forEach { target ->
            deletedThreadTimestamps[target.id] = now
            target.messages.forEach { msg ->
                deletedMessageMeta[msg.id] = Pair(target.id, now)
            }
        }
        playDeleteSound(context)
        savePersistedPins(context)

        _threads.update { list -> list.filterNot { it.id in threadIds } }
        _deletedThreads.update { currentTrash ->
            val newEntries = targets.map { target ->
                DeletedConversationEntry(
                    thread = target,
                    deletedAtMillis = now
                )
            }
            (newEntries + currentTrash.filterNot { it.thread.id in threadIds })
                .sortedByDescending { it.deletedAtMillis }
        }
        purgeExpiredDeletedThreads(context, retentionDays)
    }

    /**
     * Restores a conversation thread from the Deleted Messages (Trash) folder back to the active inbox.
     */
    fun restoreDeletedThread(context: Context, threadId: Long): String? {
        val entry = _deletedThreads.value.find { it.thread.id == threadId } ?: return null
        deletedThreadTimestamps.remove(threadId)
        entry.thread.messages.forEach { msg ->
            deletedMessageMeta.remove(msg.id)
        }
        savePersistedPins(context)

        _deletedThreads.update { currentTrash ->
            currentTrash.filterNot { it.thread.id == threadId }
        }
        _threads.update { currentActive ->
            val existing = currentActive.find { it.id == threadId }
            val merged = if (existing != null) {
                val combinedMsgs = (existing.messages + entry.thread.messages)
                    .distinctBy { it.id }
                    .sortedBy { it.timestampMillis }
                currentActive.map { if (it.id == threadId) it.copy(messages = combinedMsgs) else it }
            } else {
                currentActive + entry.thread
            }
            sortThreads(merged)
        }
        return entry.thread.contactName
    }

    /**
     * Permanently deletes a single conversation from the Deleted Messages (Trash) folder,
     * removing all messages, thread entries, and cached media attachments from device storage.
     */
    fun permanentlyDeleteThread(context: Context, threadId: Long) {
        val entry = _deletedThreads.value.find { it.thread.id == threadId }
        deletedThreadTimestamps.remove(threadId)
        val msgs = entry?.thread?.messages.orEmpty()
        val msgIdsToDelete = msgs.map { it.id }.toSet()
        msgIdsToDelete.forEach { id -> deletedMessageMeta.remove(id) }
        playPermanentDeleteSound(context)
        savePersistedPins(context)

        // 1. Physically delete any cached media / attachments on device storage
        msgs.forEach { msg ->
            cleanupAttachmentFile(context, msg.attachment?.uri, msg.id)
        }

        _deletedThreads.update { currentTrash ->
            currentTrash.filterNot { it.thread.id == threadId }
        }

        // 2. Check if this contact STILL has active messages or an active thread in the inbox
        val entryAddress = entry?.thread?.address.orEmpty().trim()
        val hasActiveConversation = _threads.value.any { activeThread ->
            activeThread.id == threadId ||
            (entryAddress.isNotBlank() && arePhoneNumbersEqual(activeThread.address, entryAddress))
        }

        // 3. Permanently delete from Android telephony database ONLY the specific trashed messages!
        // We do NOT delete the thread or the contact's address if active messages remain in the inbox!
        deleteFromSystemProviderThoroughly(
            context = context,
            threadIds = if (!hasActiveConversation && threadId > 0L) setOf(threadId) else emptySet(),
            messageIds = msgIdsToDelete,
            address = if (!hasActiveConversation && entryAddress.isNotBlank()) entryAddress else null
        )

        // 4. Immediately flush the updated messages and trash state to the offline encrypted disk cache
        saveDiskCacheAsync(context)
    }

    /**
     * Restores specific messages from a deleted conversation entry in Trash back to the active inbox.
     */
    fun restoreDeletedMessages(context: Context, threadId: Long, messageIds: Set<Long>): String? {
        if (messageIds.isEmpty()) return null
        val entry = _deletedThreads.value.find { it.thread.id == threadId } ?: return null
        val messagesToRestore = entry.thread.messages.filter { it.id in messageIds }
        if (messagesToRestore.isEmpty()) return null

        messagesToRestore.forEach { msg ->
            deletedMessageMeta.remove(msg.id)
        }

        val remainingTrashedMsgs = entry.thread.messages.filterNot { it.id in messageIds }
        if (remainingTrashedMsgs.isEmpty()) {
            deletedThreadTimestamps.remove(threadId)
            _deletedThreads.update { currentTrash ->
                currentTrash.filterNot { it.thread.id == threadId }
            }
        } else {
            _deletedThreads.update { currentTrash ->
                currentTrash.map {
                    if (it.thread.id == threadId) {
                        it.copy(thread = it.thread.copy(messages = remainingTrashedMsgs))
                    } else {
                        it
                    }
                }
            }
        }

        _threads.update { currentActive ->
            val existing = currentActive.find { it.id == threadId }
            val merged = if (existing != null) {
                val combinedMsgs = deduplicateMessageList(existing.messages + messagesToRestore)
                    .sortedBy { it.timestampMillis }
                currentActive.map { if (it.id == threadId) it.copy(messages = combinedMsgs) else it }
            } else {
                currentActive + entry.thread.copy(messages = messagesToRestore)
            }
            sortThreads(merged)
        }

        savePersistedPins(context, historyThreadIds = setOf(threadId))
        return entry.thread.contactName
    }

    /**
     * Permanently deletes specific messages from a deleted conversation entry in Trash,
     * removing their attachments and records from device storage.
     */
    fun permanentlyDeleteMessages(context: Context, threadId: Long, messageIds: Set<Long>) {
        if (messageIds.isEmpty()) return
        val entry = _deletedThreads.value.find { it.thread.id == threadId } ?: return
        val msgsToDelete = entry.thread.messages.filter { it.id in messageIds }
        if (msgsToDelete.isEmpty()) return

        val msgIdsSet = msgsToDelete.map { it.id }.toSet()
        msgIdsSet.forEach { id -> deletedMessageMeta.remove(id) }
        playPermanentDeleteSound(context)

        // 1. Physically delete any cached media / attachments on device storage
        msgsToDelete.forEach { msg ->
            cleanupAttachmentFile(context, msg.attachment?.uri, msg.id)
        }

        val remainingTrashedMsgs = entry.thread.messages.filterNot { it.id in msgIdsSet }
        if (remainingTrashedMsgs.isEmpty()) {
            deletedThreadTimestamps.remove(threadId)
            _deletedThreads.update { currentTrash ->
                currentTrash.filterNot { it.thread.id == threadId }
            }
        } else {
            _deletedThreads.update { currentTrash ->
                currentTrash.map {
                    if (it.thread.id == threadId) {
                        it.copy(thread = it.thread.copy(messages = remainingTrashedMsgs))
                    } else {
                        it
                    }
                }
            }
        }

        // 2. Check if this contact STILL has active messages or an active thread in the inbox
        val entryAddress = entry.thread.address.trim()
        val hasActiveConversation = _threads.value.any { activeThread ->
            activeThread.id == threadId ||
            (entryAddress.isNotBlank() && arePhoneNumbersEqual(activeThread.address, entryAddress))
        }

        // 3. Permanently delete from Android telephony database ONLY the specific trashed messages!
        deleteFromSystemProviderThoroughly(
            context = context,
            threadIds = if (!hasActiveConversation && remainingTrashedMsgs.isEmpty() && threadId > 0L) setOf(threadId) else emptySet(),
            messageIds = msgIdsSet,
            address = if (!hasActiveConversation && remainingTrashedMsgs.isEmpty() && entryAddress.isNotBlank()) entryAddress else null
        )

        savePersistedPins(context)
    }

    /**
     * Permanently empties all conversations in the Deleted Messages (Trash) folder,
     * physically deleting all trashed SMS, MMS, and media attachments from device storage.
     */
    fun emptyTrash(context: Context) {
        val entriesToPurge = _deletedThreads.value
        val threadIdsToPurge = entriesToPurge.map { it.thread.id }.toSet()
        val activeThreads = _threads.value

        threadIdsToPurge.forEach { id ->
            deletedThreadTimestamps.remove(id)
        }

        val allMsgIds = mutableSetOf<Long>()
        val safeSystemThreadIds = mutableSetOf<Long>()
        val addressesToClean = mutableSetOf<String>()

        entriesToPurge.forEach { entry ->
            val entryAddr = entry.thread.address.trim()
            val hasActive = activeThreads.any { act ->
                act.id == entry.thread.id || (entryAddr.isNotBlank() && arePhoneNumbersEqual(act.address, entryAddr))
            }
            if (!hasActive) {
                if (entryAddr.isNotBlank()) addressesToClean.add(entryAddr)
                if (entry.thread.id > 0L) safeSystemThreadIds.add(entry.thread.id)
            }
            entry.thread.messages.forEach { msg ->
                deletedMessageMeta.remove(msg.id)
                allMsgIds.add(msg.id)
                cleanupAttachmentFile(context, msg.attachment?.uri, msg.id)
            }
        }

        savePersistedPins(context)
        _deletedThreads.value = emptyList()
        playPermanentDeleteSound(context)

        deleteFromSystemProviderThoroughly(
            context = context,
            threadIds = safeSystemThreadIds,
            messageIds = allMsgIds,
            addresses = addressesToClean
        )

        saveDiskCacheAsync(context)
    }

    /**
     * Automatically purges any conversation in the Deleted Messages (Trash) folder that has
     * exceeded the configured [retentionDays] period (default 30 days).
     */
    fun purgeExpiredDeletedThreads(context: Context, retentionDays: Int) {
        val appContext = context.applicationContext
        ioScope.launch {
            purgeExpiredDeletedThreadsInternal(appContext, retentionDays)
        }
    }

    private fun purgeExpiredDeletedThreadsInternal(context: Context, retentionDays: Int) {
        val now = System.currentTimeMillis()
        val maxAgeMillis = retentionDays.coerceAtLeast(1) * 86_400_000L
        val activeThreadIds = _threads.value.map { it.id }.toSet()

        val expiredThreadIds = synchronized(deletedThreadTimestamps) {
            deletedThreadTimestamps.entries
                .asSequence()
                .filter { (_, deletedAt) -> (now - deletedAt) >= maxAgeMillis }
                .map { it.key }
                .toList()
        }
        if (expiredThreadIds.isNotEmpty()) {
            expiredThreadIds.forEach { id ->
                deletedThreadTimestamps.remove(id)
            }
        }

        val expiredMessageEntries = synchronized(deletedMessageMeta) {
            deletedMessageMeta.entries
                .asSequence()
                .filter { (_, meta) -> (now - meta.second) >= maxAgeMillis }
                .map { (msgId, meta) -> msgId to meta.first }
                .toList()
        }
        if (expiredMessageEntries.isNotEmpty()) {
            val expiredMsgIds = expiredMessageEntries.map { it.first }.toSet()
            expiredMsgIds.forEach { id -> deletedMessageMeta.remove(id) }
        }

        val expiredTrashEntries = _deletedThreads.value.filter { entry ->
            entry.thread.id in expiredThreadIds || (now - entry.deletedAtMillis) >= maxAgeMillis
        }

        val allExpiredMsgIds = (expiredTrashEntries.flatMap { it.thread.messages }.map { it.id } +
            expiredMessageEntries.map { it.first }).toSet()

        val safeExpiredSysThreadIds = expiredTrashEntries
            .filter { entry ->
                entry.thread.id > 0L &&
                _threads.value.none { act -> act.id == entry.thread.id || arePhoneNumbersEqual(act.address, entry.thread.address) }
            }
            .map { it.thread.id }
            .toSet()

        val expiredAddresses = expiredTrashEntries
            .filter { entry ->
                entry.thread.address.isNotBlank() &&
                _threads.value.none { act -> act.id == entry.thread.id || arePhoneNumbersEqual(act.address, entry.thread.address) }
            }
            .map { it.thread.address }
            .toSet()

        expiredTrashEntries.forEach { entry ->
            entry.thread.messages.forEach { msg ->
                cleanupAttachmentFile(context, msg.attachment?.uri, msg.id)
            }
        }

        if (allExpiredMsgIds.isNotEmpty() || safeExpiredSysThreadIds.isNotEmpty()) {
            deleteFromSystemProviderThoroughly(
                context = context,
                threadIds = safeExpiredSysThreadIds,
                messageIds = allExpiredMsgIds,
                addresses = expiredAddresses
            )
        }

        if (expiredThreadIds.isNotEmpty() || expiredMessageEntries.isNotEmpty()) {
            savePersistedPins(context)
            saveDiskCacheAsync(context)
        }

        _deletedThreads.update { currentTrash ->
            currentTrash.mapNotNull { entry ->
                if (entry.thread.id in expiredThreadIds || (now - entry.deletedAtMillis) >= maxAgeMillis) {
                    null
                } else {
                    val nonExpiredMsgs = entry.thread.messages.filter { msg ->
                        val meta = deletedMessageMeta[msg.id]
                        val deletedAt = meta?.second ?: entry.deletedAtMillis
                        (now - deletedAt) < maxAgeMillis
                    }
                    if (nonExpiredMsgs.isEmpty()) {
                        null
                    } else {
                        entry.copy(
                            thread = entry.thread.copy(messages = nonExpiredMsgs),
                            deletedAtMillis = nonExpiredMsgs.maxOfOrNull { msg ->
                                deletedMessageMeta[msg.id]?.second ?: entry.deletedAtMillis
                            } ?: entry.deletedAtMillis
                        )
                    }
                }
            }
        }
    }

    /**
     * Snapshot of pinned contact addresses and pinned message IDs for backup.
     */
    fun getPinnedDataSnapshot(): Pair<Set<String>, Set<Long>> {
        return Pair(
            synchronized(pinnedAddresses) { pinnedAddresses.toSet() },
            synchronized(pinnedMessageIds) { pinnedMessageIds.toSet() }
        )
    }

    fun getHighlightedMessageIdsSnapshot(): Set<Long> {
        return synchronized(highlightedMessageIds) { highlightedMessageIds.toSet() }
    }

    fun getBlockedAddressesSnapshot(): Set<String> {
        return synchronized(blockedAddresses) { blockedAddresses.toSet() }
    }

    /**
     * Snapshot of deleted thread timestamps for backup.
     */
    fun getDeletedThreadsMetaSnapshot(): Map<Long, Long> {
        return synchronized(deletedThreadTimestamps) { deletedThreadTimestamps.toMap() }
    }

    /**
     * Restores threads, deleted trash entries, pinned contacts, pinned message IDs,
     * highlighted message IDs, and blocked addresses from a backup payload.
     */
    suspend fun restoreDataFromBackup(
        context: Context,
        payload: BackupRestorePayload
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        synchronized(pinnedAddresses) {
            pinnedAddresses.clear()
            pinnedAddresses.addAll(payload.pinnedAddresses)
        }
        synchronized(pinnedMessageIds) {
            pinnedMessageIds.clear()
            pinnedMessageIds.addAll(payload.pinnedMessageIds)
        }
        synchronized(highlightedMessageIds) {
            highlightedMessageIds.clear()
            highlightedMessageIds.addAll(payload.highlightedMessageIds)
        }
        synchronized(blockedAddresses) {
            blockedAddresses.clear()
            blockedAddresses.addAll(payload.blockedAddresses)
            _blockedSenders.value = blockedAddresses.sorted()
        }
        synchronized(deletedThreadTimestamps) {
            deletedThreadTimestamps.clear()
            deletedThreadTimestamps.putAll(payload.deletedThreadsMeta)
        }
        synchronized(deletedMessageMeta) {
            deletedMessageMeta.clear()
            payload.deletedThreads.forEach { entry ->
                entry.thread.messages.forEach { msg ->
                    deletedMessageMeta[msg.id] = Pair(entry.thread.id, entry.deletedAtMillis)
                }
            }
        }
        savePersistedPins(context)

        _threads.value = sortThreads(payload.threads.filterNot { isSenderBlocked(it.address) })
        _deletedThreads.value = payload.deletedThreads
        val restoredHistoryIds = (payload.threads + payload.deletedThreads.map { it.thread })
            .filter { it.isHistoryLoaded }
            .mapTo(mutableSetOf()) { it.id }
        saveDiskCacheAsync(context, historyThreadIds = restoredHistoryIds)

        val totalThreads = payload.threads.size + payload.deletedThreads.size
        val totalMsgs = payload.totalMessagesCount
        Pair(totalThreads, totalMsgs)
    }

    private fun cleanupAttachmentFile(context: Context, uri: Uri?, msgId: Long? = null) {
        try {
            if (uri != null) {
                if (uri.scheme == "file") {
                    val path = uri.path
                    if (!path.isNullOrBlank()) {
                        File(path).delete()
                    }
                } else if (uri.scheme == "content") {
                    if (uri.authority == "${context.packageName}.fileprovider") {
                        try {
                            context.contentResolver.delete(uri, null, null)
                        } catch (_: Exception) {}
                    }
                    val fileName = uri.lastPathSegment
                    if (!fileName.isNullOrBlank()) {
                        File(context.filesDir, "mms_media/$fileName").delete()
                        File(context.cacheDir, "mms_attachments/$fileName").delete()
                        File(context.cacheDir, "voice_messages/$fileName").delete()
                        File(context.cacheDir, "voice_playback_cache/$fileName").delete()
                    }
                }
            }
            if (msgId != null) {
                val rawMmsId = if (msgId >= 1_000_000_000L) msgId - 1_000_000_000L else msgId
                File(context.cacheDir, "mms_send_$msgId.pdu").delete()
                File(context.cacheDir, "mms_send_$rawMmsId.pdu").delete()
            }
        } catch (_: Exception) {
            // Ignore filesystem cleanup exceptions
        }
    }

    private fun deleteFromSystemProviderThoroughly(
        context: Context,
        threadIds: Set<Long> = emptySet(),
        messageIds: Set<Long> = emptySet(),
        address: String? = null,
        addresses: Set<String> = emptySet()
    ) {
        val appCtx = context.applicationContext
        ioScope.launch {
            if (!isDefaultSmsApp(appCtx)) return@launch
            val cr = appCtx.contentResolver

            // 1. Delete individual SMS messages by ID
            val smsIds = messageIds.filter { it < 1_000_000_000L }
            if (smsIds.isNotEmpty()) {
                smsIds.chunked(50).forEach { chunk ->
                    try {
                        val placeholders = chunk.joinToString(",") { "?" }
                        val args = chunk.map { it.toString() }.toTypedArray()
                        cr.delete(
                            Telephony.Sms.CONTENT_URI,
                            "${Telephony.Sms._ID} IN ($placeholders)",
                            args
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Error batch deleting SMS by ID", e)
                    }
                }
                smsIds.forEach { smsId ->
                    try {
                        cr.delete(ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, smsId), null, null)
                    } catch (_: Exception) {}
                }
            }

            // 2. Delete individual MMS messages by ID and direct content://mms URIs
            val mmsIds = messageIds.filter { it >= 1_000_000_000L }.map { it - 1_000_000_000L }
            if (mmsIds.isNotEmpty()) {
                mmsIds.chunked(50).forEach { chunk ->
                    try {
                        val placeholders = chunk.joinToString(",") { "?" }
                        val args = chunk.map { it.toString() }.toTypedArray()
                        cr.delete(
                            Telephony.Mms.CONTENT_URI,
                            "${Telephony.Mms._ID} IN ($placeholders)",
                            args
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Error batch deleting MMS by ID", e)
                    }
                }
                mmsIds.forEach { mmsId ->
                    try {
                        cr.delete(ContentUris.withAppendedId(Telephony.Mms.CONTENT_URI, mmsId), null, null)
                    } catch (_: Exception) {}
                    try {
                        cr.delete("content://mms/$mmsId".toUri(), null, null)
                    } catch (_: Exception) {}
                }
            }

            // 3. Delete conversation threads ONLY IF completely abandoned and not active in inbox
            val activeThreads = _threads.value
            val activeThreadIds = activeThreads.map { it.id }.toSet()
            val safeThreadIds = threadIds.filter { tId ->
                tId > 0L && tId !in activeThreadIds && activeThreads.none { act -> act.messages.any { it.threadId == tId } }
            }
            safeThreadIds.forEach { tId ->
                try {
                    cr.delete("content://mms-sms/conversations/$tId".toUri(), null, null)
                } catch (e: Exception) {
                    Log.w(TAG, "Error deleting conversation uri for thread $tId", e)
                }
            }

            // 4. Delete by phone number address ONLY IF absolutely NO active thread exists with this contact
            val targetAddresses = (addresses + listOfNotNull(address)).filter { it.isNotBlank() && it != "Unknown" }
            targetAddresses.forEach { addr ->
                val hasActive = _threads.value.any { arePhoneNumbersEqual(it.address, addr) }
                if (!hasActive) {
                    try {
                        cr.delete(
                            Telephony.Sms.CONTENT_URI,
                            "${Telephony.Sms.ADDRESS} = ?",
                            arrayOf(addr)
                        )
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private fun deleteFromSystemProviderIfDefault(context: Context, threadId: Long) {
        deleteFromSystemProviderThoroughly(context = context, threadIds = setOf(threadId))
    }

    private fun deleteMessagesFromSystemProviderIfDefault(context: Context, messageIds: Collection<Long>) {
        deleteFromSystemProviderThoroughly(context = context, messageIds = messageIds.toSet())
    }

    fun arePhoneNumbersEqual(a: String, b: String): Boolean {
        return when {
            a.equals(b, ignoreCase = true) -> true
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                try {
                    PhoneNumberUtils.areSamePhoneNumber(a, b, Locale.getDefault().country.ifBlank { "RS" })
                } catch (_: Exception) {
                    normalizeAddressKey(a) == normalizeAddressKey(b)
                }
            }
            else -> {
                @Suppress("DEPRECATION")
                PhoneNumberUtils.compare(a, b)
            }
        }
    }

    private fun isAddressPinned(address: String): Boolean {
        val normKey = normalizeAddressKey(address)
        return address in pinnedAddresses || normKey in pinnedAddresses
    }

    fun normalizeAddressKey(address: String): String {
        val digitsOnly = address.filter { it.isDigit() }
        return if (digitsOnly.length >= 7) {
            digitsOnly.takeLast(8)
        } else {
            address.trim().lowercase(Locale.ROOT)
        }
    }

    /**
     * Determines whether two [ConversationThread] entries represent the same real-world contact,
     * comparing normalized phone numbers, identical device contact names, or service names.
     */
    fun areThreadsSameContact(a: ConversationThread, b: ConversationThread): Boolean {
        if (a.id == b.id) return true

        // 1. Exact string match (case-insensitive)
        if (a.address.isNotBlank() && b.address.isNotBlank() && a.address.equals(b.address, ignoreCase = true)) {
            return true
        }

        // 2. Normalized phone number key match (e.g. +38164... vs 064... share last 8 digits)
        val normA = normalizeAddressKey(a.address)
        val normB = normalizeAddressKey(b.address)
        if (normA.isNotBlank() && normB.isNotBlank() && normA == normB) {
            return true
        }

        // 3. Same named contact from device contacts (must contain letters, not raw numbers)
        val aHasRealName = a.contactName.isNotBlank() && a.contactName != a.address && a.contactName.any { it.isLetter() }
        val bHasRealName = b.contactName.isNotBlank() && b.contactName != b.address && b.contactName.any { it.isLetter() }
        if (aHasRealName && bHasRealName && a.contactName.trim().equals(b.contactName.trim(), ignoreCase = true)) {
            return true
        }

        // 4. Same service name (e.g., both "Yettel", "MTS", "Banka")
        if (a.isService && b.isService && a.contactName.isNotBlank() && a.contactName.trim().equals(b.contactName.trim(), ignoreCase = true)) {
            return true
        }

        // 5. Fallback telephony phone number match (only if both contain digits and weren't caught by normalized key)
        if (normA.any { it.isDigit() } && normB.any { it.isDigit() }) {
            if (arePhoneNumbersEqual(a.address, b.address)) return true
        }

        return false
    }

    /**
     * Deduplicates and merges multiple conversation threads that correspond to the same contact,
     * consolidating their chat history, unread counters, pin status, and draft text in O(N) time.
     */
    fun deduplicateAndMergeThreads(threads: List<ConversationThread>): List<ConversationThread> {
        if (threads.size <= 1) return threads

        val merged = ArrayList<ConversationThread>(threads.size)
        // Fast index: maps lookup keys (normalized phone digits, saved contact name, raw address)
        // to index in `merged` list
        val keyToIndex = HashMap<String, Int>(threads.size * 3)

        for (thread in threads) {
            val keys = computeThreadLookupKeys(thread)
            var targetIndex: Int? = null

            for (k in keys) {
                val idx = keyToIndex[k]
                if (idx != null) {
                    targetIndex = idx
                    break
                }
            }

            // Fallback for tricky phone formats if not found in hash map
            if (targetIndex == null) {
                for ((i, element) in merged.withIndex()) {
                    if (areThreadsSameContact(element, thread)) {
                        targetIndex = i
                        break
                    }
                }
            }

            if (targetIndex != null) {
                val existing = merged[targetIndex]
                val combinedMessages = if (thread.messages.isEmpty()) {
                    existing.messages
                } else if (existing.messages.isEmpty()) {
                    thread.messages
                } else {
                    (existing.messages + thread.messages)
                        .distinctBy { it.id }
                        .sortedBy { it.timestampMillis }
                }

                val mostRecentMsg = combinedMessages.lastOrNull()
                val primaryAddress = mostRecentMsg?.address?.takeIf { it.isNotBlank() && it != "Unknown" }
                    ?: if (thread.messages.isNotEmpty()) thread.address else existing.address

                val bestName = when {
                    existing.contactName.any { it.isLetter() } && existing.contactName != existing.address -> existing.contactName
                    thread.contactName.any { it.isLetter() } && thread.contactName != thread.address -> thread.contactName
                    existing.contactName.isNotBlank() -> existing.contactName
                    else -> thread.contactName
                }

                val primaryId = when {
                    existing.id > 0L && existing.id !in DEMO_THREAD_IDS -> existing.id
                    thread.id > 0L && thread.id !in DEMO_THREAD_IDS -> thread.id
                    else -> existing.id
                }

                val mergedThread = existing.copy(
                    id = primaryId,
                    contactName = bestName,
                    address = primaryAddress,
                    avatarInitials = computeInitials(bestName),
                    avatarAccentColorHex = existing.avatarAccentColorHex,
                    contactPhotoUri = existing.contactPhotoUri ?: thread.contactPhotoUri,
                    messages = combinedMessages,
                    unreadCount = existing.unreadCount + thread.unreadCount,
                    isPinned = existing.isPinned || thread.isPinned,
                    isService = existing.isService && thread.isService,
                    draftText = if (!existing.draftText.isNullOrBlank()) existing.draftText else thread.draftText,
                    isHistoryLoaded = existing.isHistoryLoaded && thread.isHistoryLoaded
                )
                merged[targetIndex] = mergedThread
                for (k in keys) {
                    keyToIndex[k] = targetIndex
                }
            } else {
                val newIndex = merged.size
                merged.add(thread)
                for (k in keys) {
                    keyToIndex[k] = newIndex
                }
            }
        }

        return merged.sortedWith(
            compareByDescending<ConversationThread> { it.isPinned }
                .thenByDescending { it.lastMessage?.timestampMillis ?: 0L }
        )
    }

    private fun computeThreadLookupKeys(thread: ConversationThread): List<String> {
        val keys = mutableListOf<String>()
        val addr = thread.address.trim()
        if (addr.isNotBlank()) {
            keys.add("a:" + addr.lowercase(Locale.ROOT))
            val digits = addr.filter { it.isDigit() }
            if (digits.length >= 7) {
                keys.add("p:" + digits.takeLast(8))
            }
        }

        val name = thread.contactName.trim()
        if (name.isNotBlank() && name != addr && name.any { it.isLetter() }) {
            keys.add("n:" + name.lowercase(Locale.ROOT))
        }

        if (thread.isService && name.isNotBlank()) {
            keys.add("s:" + name.lowercase(Locale.ROOT))
        }

        return keys
    }

    private fun sortThreads(list: List<ConversationThread>): List<ConversationThread> {
        return deduplicateAndMergeThreads(list)
    }

    fun computeInitials(name: String): String {
        val parts = name.trim().split(" ").filter { it.isNotBlank() }
        return when {
            parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
            parts.size == 1 && parts[0].isNotEmpty() -> parts[0].take(2).uppercase()
            else -> "#"
        }
    }

    private const val PREFS_DRAFTS = "txtura_drafts_prefs"

    fun getDraftForThread(context: Context, threadId: Long): String {
        return context.getSharedPreferences(PREFS_DRAFTS, Context.MODE_PRIVATE)
            .getString(threadId.toString(), "").orEmpty()
    }

    fun saveDraftForThread(context: Context, threadId: Long, draftText: String) {
        val clean = draftText.trimEnd()
        context.getSharedPreferences(PREFS_DRAFTS, Context.MODE_PRIVATE).edit().apply {
            if (clean.isBlank()) {
                remove(threadId.toString())
            } else {
                putString(threadId.toString(), clean)
            }
            apply()
        }
        _threads.update { list ->
            list.map { th ->
                if (th.id == threadId) {
                    th.copy(draftText = clean.ifBlank { null })
                } else {
                    th
                }
            }
        }
        saveDiskCacheAsync(context)
    }

    fun clearDraftForThread(context: Context, threadId: Long) {
        saveDraftForThread(context, threadId, "")
    }

    fun getAllDrafts(context: Context): Map<Long, String> {
        val all = context.getSharedPreferences(PREFS_DRAFTS, Context.MODE_PRIVATE).all
        val result = mutableMapOf<Long, String>()
        all.forEach { (key, value) ->
            val id = key.toLongOrNull()
            val str = value as? String
            if (id != null && !str.isNullOrBlank()) {
                result[id] = str
            }
        }
        return result
    }
}
