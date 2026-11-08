@file:Suppress("SpellCheckingInspection", "DestructuringDeclaration", "TrailingComma", "ClarifyingParentheses")

package com.txtura.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.role.RoleManager
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
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import com.txtura.MainActivity
import com.txtura.R
import com.txtura.security.MessageSecurityAnalyzer
import com.txtura.security.SecurityAnalysisResult
import com.txtura.security.SecurityRiskLevel
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
import java.util.Collections
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.milliseconds

object SmsMmsRepository {

    private const val TAG = "SmsMmsRepository"
    const val ACTION_SMS_SENT = "com.txtura.action.SMS_SENT"
    const val ACTION_SMS_DELIVERED = "com.txtura.action.SMS_DELIVERED"
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

    fun initializePersistedState(context: Context) {
        loadPersistedPins(context)
        ensureNotificationChannel(context)
        scanAllThreadsForMaliciousWebsitesOnline(context)
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
    fun ensureNotificationChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val isSr = TxturaStrings.isSerbianLocale()
        val channelName = if (isSr) "SMS i MMS Poruke" else "SMS & MMS Messages"
        val channelDesc = if (isSr) {
            "Obaveštenja za nove primljene SMS i MMS poruke"
        } else {
            "Notifications for incoming SMS and MMS messages"
        }
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(
            CHANNEL_ID_MESSAGES,
            channelName,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = channelDesc
            enableLights(true)
            enableVibration(true)
            vibrationPattern = longArrayOf(0L, 220L, 120L, 220L)
            setSound(defaultSoundUri, audioAttrs)
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

    private fun savePersistedPins(context: Context) {
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
    }

    /**
     * Checks if a sender address is currently blocked.
     */
    fun isSenderBlocked(address: String): Boolean {
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

    /**
     * Determines whether an address should be categorized as a Service message.
     * Takes manual user overrides into account first, followed by standard heuristics.
     */
    fun isAddressService(address: String, messages: List<ChatMessage> = emptyList()): Boolean {
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

        // Default heuristic:
        val cleanDigits = clean.filter { it.isDigit() }
        val isAlphaSender = clean.any { it.isLetter() }
        val inShortCodeLength = (cleanDigits.length in 3..7)
        val isNotInternationalPlus = !clean.startsWith("+")
        val isShortCode = inShortCodeLength && isNotInternationalPlus
        val hasOtpMessage = messages.any { it.securityAnalysis.extractedOtpCode != null }
        return isAlphaSender || isShortCode || hasOtpMessage
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

    fun onMessageDelivered(messageId: Long) {
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
            val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_ID_MESSAGES)
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

    fun playInAppMessageSentSound(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
            audioManager?.playSoundEffect(android.media.AudioManager.FX_KEYPRESS_STANDARD, 0.75f)
        } catch (_: Exception) {
            try {
                android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 65)
                    .startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 70)
            } catch (_: Exception) { }
        }
    }

    fun playInAppMessageReceivedSound(context: Context) {
        try {
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
        purgeExpiredDeletedThreads(context, retentionDays)
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

        if (loadedMessages.isEmpty()) return@withContext

        // 3. Group by system THREAD_ID (or normalized phone number if threadId <= 0)
        val accentColors = listOf(0xFF00E5FF, 0xFF6366F1, 0xFFA855F7, 0xFF10B981, 0xFFF59E0B)
        val contactNameCache = mutableMapOf<String, String>()
        val contactPhotoCache = mutableMapOf<String, Uri?>()

        val deviceThreads = loadedMessages
            .groupBy { msg ->
                if (msg.threadId > 0L) {
                    "thread_${msg.threadId}"
                } else {
                    "addr_${normalizeAddressKey(msg.address)}"
                }
            }
            .entries
            .mapIndexed { index, (_, msgs) ->
                val sortedMsgs = msgs.sortedBy { it.timestampMillis }
                val primaryAddress = sortedMsgs.lastOrNull { it.address.isNotBlank() && it.address != "Unknown" }
                    ?.address ?: sortedMsgs.first().address
                val resolvedName = contactNameCache.getOrPut(primaryAddress) {
                    resolveContactNameSafely(context, primaryAddress) ?: primaryAddress
                }
                val resolvedPhotoUri = contactPhotoCache.getOrPut(primaryAddress) {
                    resolveContactPhotoUriSafely(context, primaryAddress)
                }
                val initials = computeInitials(resolvedName)
                val threadId = sortedMsgs.first().threadId.takeIf { it > 0L }
                    ?: (primaryAddress.hashCode().toLong() and 0x7FFFFFFFL)
                val isContactPinned = isAddressPinned(primaryAddress)
                val unread = unreadCountByThread[threadId] ?: 0

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
                    isService = isAddressService(primaryAddress, sortedMsgs)
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
                prev.id == deviceThread.id ||
                    arePhoneNumbersEqual(prev.address, deviceThread.address) ||
                    prev.address.equals(deviceThread.address, ignoreCase = true) ||
                    normalizeAddressKey(prev.address) == normalizeAddressKey(deviceThread.address)
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
                    val combinedMsgs = (deviceThread.messages + missingMessages.map { it.copy(threadId = deviceThread.id) })
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
                dev.id == inMemoryThread.id ||
                    arePhoneNumbersEqual(dev.address, inMemoryThread.address) ||
                    dev.address.equals(inMemoryThread.address, ignoreCase = true) ||
                    normalizeAddressKey(dev.address) == normalizeAddressKey(inMemoryThread.address)
            }
            if (!alreadyInDeviceList) {
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
                    val mergedMsgs = (prev.thread.messages + entry.thread.messages)
                        .distinctBy { it.id }
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

            // 1. Add existing conversation threads first (with existingThreadId)
            for (thread in activeThreads) {
                val key = normalizeAddressKey(thread.address)
                result[key] = ContactPickerItem(
                    name = thread.contactName,
                    address = thread.address,
                    initials = thread.avatarInitials,
                    photoUri = thread.contactPhotoUri,
                    existingThreadId = thread.id
                )
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
        return threads.find {
            arePhoneNumbersEqual(it.address, cleanAddress) ||
                it.address.equals(cleanAddress, ignoreCase = true) ||
                it.contactName.equals(cleanAddress, ignoreCase = true) ||
                normalizeAddressKey(it.address) == normalizeAddressKey(cleanAddress)
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

        // 1. Check active threads
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

        // 3. Create a brand-new conversation thread
        val newThreadId = nextId.incrementAndGet()
        val resolvedName = preferredName?.takeIf { it.isNotBlank() }
            ?: resolveContactNameSafely(context, cleanAddress)
            ?: cleanAddress
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
            isService = isAddressService(cleanAddress)
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
            deliveryStatus = DeliveryStatus.DELIVERED
        }

        val resolvedContactName = resolveContactNameSafely(context, cleanAddress) ?: cleanAddress
        val resolvedPhotoUri = resolveContactPhotoUriSafely(context, cleanAddress)
        val defaultMmsText = if (isSr) "MMS Fotografija" else "MMS Photo"

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
                    mimeType = "image/*",
                    caption = if (isSr) "MMS Medij" else "MMS Media"
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
                    isService = isAddressService(cleanAddress, listOf(newMessage))
                )
                listOf(newThread) + current
            }
            sortThreads(updated)
        }

        verifyMessageUrlsOnlineAsync(context, threadId, msgId, cleanBody)

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
                    isService = isAddressService(cleanAddress, listOf(incomingMessage))
                )
                listOf(created) + current
            }
            sortThreads(updated)
        }

        context?.let { ctx ->
            postIncomingMessageNotification(
                context = ctx,
                threadId = targetThreadId,
                messageId = msgId,
                senderName = resolvedName,
                senderAddress = cleanAddress,
                messageBody = cleanBody,
                securityAnalysis = securityResult,
            )
            verifyMessageUrlsOnlineAsync(ctx, targetThreadId, msgId, cleanBody)
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

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_MESSAGES)
            .setSmallIcon(R.drawable.ic_stat_message)
            .setContentTitle(titleText)
            .setContentText(messageBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedBody))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(contentPendingIntent)
            .addAction(replyAction)
            .addAction(markReadAction)

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

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Unable to post incoming SMS notification", e)
        }
    }

    /**
     * Marks a conversation as read both in memory and in the Android Telephony database.
     */
    fun markThreadRead(context: Context, threadId: Long) {
        markThreadsRead(context, setOf(threadId))
    }

    /**
     * Marks multiple conversations as read.
     */
    fun markThreadsRead(context: Context, threadIds: Set<Long>) {
        if (threadIds.isEmpty()) return
        val affectedAddresses = mutableListOf<String>()
        _threads.update { list ->
            list.map {
                if (it.id in threadIds) {
                    affectedAddresses.add(it.address)
                    it.copy(unreadCount = 0)
                } else {
                    it
                }
            }
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
        }
    }

    /**
     * Marks all conversations as read.
     */
    fun markAllThreadsRead(context: Context) {
        val allIds = _threads.value.asSequence().map { it.id }.toSet()
        markThreadsRead(context, allIds)
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
                    } else {
                        pinnedAddresses.remove(thread.address)
                        pinnedAddresses.remove(normKey)
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
                            } else {
                                pinnedMessageIds.remove(msg.id)
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
        savePersistedPins(context)
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
                            } else {
                                highlightedMessageIds.remove(msg.id)
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
        savePersistedPins(context)
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
        savePersistedPins(context)

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
     * Permanently deletes a single conversation from the Deleted Messages (Trash) folder.
     */
    fun permanentlyDeleteThread(context: Context, threadId: Long) {
        val entry = _deletedThreads.value.find { it.thread.id == threadId }
        deletedThreadTimestamps.remove(threadId)
        val msgIdsToDelete = entry?.thread?.messages?.map { it.id }?.toSet().orEmpty()
        msgIdsToDelete.forEach { id -> deletedMessageMeta.remove(id) }
        savePersistedPins(context)

        _deletedThreads.update { currentTrash ->
            currentTrash.filterNot { it.thread.id == threadId }
        }

        val isEntireThreadDeleted = _threads.value.none { it.id == threadId }
        if (isEntireThreadDeleted) {
            deleteFromSystemProviderIfDefault(context, threadId)
        } else {
            deleteMessagesFromSystemProviderIfDefault(context, msgIdsToDelete)
        }
    }

    /**
     * Permanently empties all conversations in the Deleted Messages (Trash) folder.
     */
    fun emptyTrash(context: Context) {
        val entriesToPurge = _deletedThreads.value
        val threadIdsToPurge = entriesToPurge.map { it.thread.id }.toSet()
        val activeThreadIds = _threads.value.map { it.id }.toSet()

        threadIdsToPurge.forEach { id ->
            deletedThreadTimestamps.remove(id)
        }
        entriesToPurge.forEach { entry ->
            val msgIds = entry.thread.messages.map { it.id }.toSet()
            msgIds.forEach { deletedMessageMeta.remove(it) }
            if (entry.thread.id in activeThreadIds) {
                deleteMessagesFromSystemProviderIfDefault(context, msgIds)
            } else {
                deleteFromSystemProviderIfDefault(context, entry.thread.id)
            }
        }
        savePersistedPins(context)
        _deletedThreads.value = emptyList()
    }

    /**
     * Automatically purges any conversation in the Deleted Messages (Trash) folder that has
     * exceeded the configured [retentionDays] period (default 30 days).
     */
    fun purgeExpiredDeletedThreads(context: Context, retentionDays: Int) {
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
                if (id !in activeThreadIds) {
                    deleteFromSystemProviderIfDefault(context, id)
                }
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
            deleteMessagesFromSystemProviderIfDefault(context, expiredMsgIds)
        }

        if (expiredThreadIds.isNotEmpty() || expiredMessageEntries.isNotEmpty()) {
            savePersistedPins(context)
        }

        _deletedThreads.update { currentTrash ->
            currentTrash.mapNotNull { entry ->
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

        val totalThreads = payload.threads.size + payload.deletedThreads.size
        val totalMsgs = payload.totalMessagesCount
        Pair(totalThreads, totalMsgs)
    }

    private fun deleteFromSystemProviderIfDefault(context: Context, threadId: Long) {
        val appCtx = context.applicationContext
        ioScope.launch {
            if (!isDefaultSmsApp(appCtx)) return@launch
            try {
                appCtx.contentResolver.delete(
                    Telephony.Sms.CONTENT_URI,
                    "${Telephony.Sms.THREAD_ID} = ?",
                    arrayOf(threadId.toString())
                )
            } catch (_: Exception) {
                // Ignore if read-only or not default SMS app
            }
        }
    }

    private fun deleteMessagesFromSystemProviderIfDefault(context: Context, messageIds: Collection<Long>) {
        if (messageIds.isEmpty()) return
        val appCtx = context.applicationContext
        ioScope.launch {
            if (!isDefaultSmsApp(appCtx)) return@launch
            try {
                val smsIds = messageIds.filter { it < 1_000_000_000L }
                if (smsIds.isNotEmpty()) {
                    smsIds.chunked(50).forEach { chunk ->
                        val placeholders = chunk.joinToString(",") { "?" }
                        val args = chunk.map { it.toString() }.toTypedArray()
                        appCtx.contentResolver.delete(
                            Telephony.Sms.CONTENT_URI,
                            "${Telephony.Sms._ID} IN ($placeholders)",
                            args
                        )
                    }
                }
                val mmsIds = messageIds.filter { it >= 1_000_000_000L }.map { it - 1_000_000_000L }
                if (mmsIds.isNotEmpty()) {
                    mmsIds.chunked(50).forEach { chunk ->
                        val placeholders = chunk.joinToString(",") { "?" }
                        val args = chunk.map { it.toString() }.toTypedArray()
                        appCtx.contentResolver.delete(
                            Telephony.Mms.CONTENT_URI,
                            "${Telephony.Mms._ID} IN ($placeholders)",
                            args
                        )
                    }
                }
            } catch (_: Exception) {
                // Ignore if read-only or not default SMS app
            }
        }
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

    private fun normalizeAddressKey(address: String): String {
        val digitsOnly = address.filter { it.isDigit() }
        return if (digitsOnly.length >= 7) {
            digitsOnly.takeLast(8)
        } else {
            address.trim().lowercase(Locale.ROOT)
        }
    }

    private fun sortThreads(list: List<ConversationThread>): List<ConversationThread> {
        return list.sortedWith(
            compareByDescending<ConversationThread> { it.isPinned }
                .thenByDescending { it.lastMessage?.timestampMillis ?: 0L }
        )
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
