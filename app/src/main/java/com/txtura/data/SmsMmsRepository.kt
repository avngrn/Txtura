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
    const val ACTION_NOTIFICATION_COPY_OTP = "com.txtura.action.NOTIFICATION_COPY_OTP"
    const val EXTRA_NOTIFICATION_THREAD_ID = "extra_notification_thread_id"
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

    // Keeps track of pinned contact addresses, pinned message IDs, highlighted message IDs, and blocked senders
    private val pinnedAddresses: MutableSet<String> =
        Collections.synchronizedSet(mutableSetOf("+381641122334", "BANK-OTP"))
    private val pinnedMessageIds: MutableSet<Long> =
        Collections.synchronizedSet(mutableSetOf(1L, 4L))
    private val highlightedMessageIds: MutableSet<Long> =
        Collections.synchronizedSet(mutableSetOf())
    private val blockedAddresses: MutableSet<String> =
        Collections.synchronizedSet(mutableSetOf())

    // Cached threads that were hidden because their sender was blocked (so unblocking can restore them in demo/memory mode)
    private val blockedThreadsCache: MutableMap<String, ConversationThread> =
        Collections.synchronizedMap(mutableMapOf())

    // Map of soft-deleted threadId -> deletedAtMillis (persisted across app restarts)
    private val deletedThreadTimestamps: MutableMap<Long, Long> =
        Collections.synchronizedMap(mutableMapOf())

    private val _threads = MutableStateFlow(createInitialDemoThreads())
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
        prefs.getStringSet(KEY_DELETED_THREADS, null)?.let { saved ->
            deletedThreadTimestamps.clear()
            saved.forEach { entry ->
                val parts = entry.split("|")
                if (parts.size == 2) {
                    val id = parts[0].toLongOrNull()
                    val ts = parts[1].toLongOrNull()
                    if (id != null && ts != null) {
                        deletedThreadTimestamps[id] = ts
                    }
                }
            }
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
        prefs.edit {
            putStringSet(KEY_PINNED_ADDRS, synchronized(pinnedAddresses) { pinnedAddresses.toSet() })
            putStringSet(KEY_PINNED_MSGS, synchronized(pinnedMessageIds) { pinnedMessageIds.asSequence().map { it.toString() }.toSet() })
            putStringSet(KEY_HIGHLIGHTED_MSGS, synchronized(highlightedMessageIds) { highlightedMessageIds.asSequence().map { it.toString() }.toSet() })
            putStringSet(KEY_BLOCKED_ADDRS, synchronized(blockedAddresses) { blockedAddresses.toSet() })
            putStringSet(KEY_DELETED_THREADS, serializedDeleted)
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
                    normalizeAddressKey(blocked) == normKey ||
                    arePhoneNumbersEqual(blocked, clean)
            }
        }
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
            // Even in demo mode, filter out blocked senders
            _threads.update { current ->
                current.filterNot { isSenderBlocked(it.address) }
            }
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
                "${Telephony.Sms.DATE} DESC LIMIT 500"
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
                val isContactPinned = isAddressPinned(primaryAddress) || sortedMsgs.any { it.isPinned }
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
                    isPinned = isContactPinned
                )
            }

        // Partition threads into active inbox vs soft-deleted Trash threads, excluding blocked senders
        val activeList = mutableListOf<ConversationThread>()
        val trashedFromDevice = mutableListOf<DeletedConversationEntry>()
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
                activeList.add(thread)
            }
        }

        _threads.value = sortThreads(activeList)
        _deletedThreads.update { existingTrash ->
            (existingTrash + trashedFromDevice)
                .associateBy { it.thread.id }
                .values
                .asSequence()
                .filter { it.thread.id in deletedThreadTimestamps }
                .sortedByDescending { it.deletedAtMillis }
                .toList()
        }
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
                "${Telephony.Mms.DATE} DESC LIMIT 100"
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
                    val fallbackBody = if (isSr) "📸 MMS Poruka" else "📸 MMS Message"
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
                        "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC LIMIT 250"
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
        val cleanAddress = MessageSecurityAnalyzer.sanitizePhoneNumber(recipientAddress)
            .ifBlank { recipientAddress.trim().take(24) }

        // 1. Check active threads
        val existingActive = _threads.value.find {
            arePhoneNumbersEqual(it.address, cleanAddress) ||
                it.address.equals(cleanAddress, ignoreCase = true) ||
                it.contactName.equals(cleanAddress, ignoreCase = true)
        }
        if (existingActive != null) {
            markThreadRead(context, existingActive.id)
            return existingActive.id
        }

        // 2. Check soft-deleted threads (restore if user starts a chat with them)
        val existingDeleted = _deletedThreads.value.find {
            arePhoneNumbersEqual(it.thread.address, cleanAddress) ||
                it.thread.address.equals(cleanAddress, ignoreCase = true)
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
            isPinned = isAddressPinned(cleanAddress)
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

        var deliveryStatus = DeliveryStatus.SENT

        if (!isMms && cleanBody.isNotBlank() && hasSendSmsPermission(context)) {
            try {
                val smsManager = context.getSystemService(SmsManager::class.java)
                if (smsManager != null && cleanAddress.any { it.isDigit() }) {
                    val sentIntent = Intent(ACTION_SMS_SENT).apply {
                        setPackage(context.packageName)
                        putExtra("message_id", msgId)
                    }
                    val deliveredIntent = Intent(ACTION_SMS_DELIVERED).apply {
                        setPackage(context.packageName)
                        putExtra("message_id", msgId)
                    }
                    val sentPi = PendingIntent.getBroadcast(
                        context,
                        msgId.toInt(),
                        sentIntent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    val deliveredPi = PendingIntent.getBroadcast(
                        context,
                        (msgId + 1).toInt(),
                        deliveredIntent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )

                    val parts = smsManager.divideMessage(cleanBody)
                    if (parts.size > 1) {
                        val sentIntents = ArrayList(parts.map { sentPi })
                        val deliveredIntents = ArrayList(parts.map { deliveredPi })
                        smsManager.sendMultipartTextMessage(
                            cleanAddress,
                            null,
                            parts,
                            sentIntents,
                            deliveredIntents
                        )
                    } else {
                        smsManager.sendTextMessage(
                            cleanAddress,
                            null,
                            cleanBody,
                            sentPi,
                            deliveredPi
                        )
                    }
                    deliveryStatus = DeliveryStatus.DELIVERED

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
        val defaultMmsText = if (isSr) "📸 MMS Fotografija" else "📸 MMS Photo"

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
                    isPinned = isAddressPinned(cleanAddress)
                )
                listOf(newThread) + current
            }
            sortThreads(updated)
        }

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
        val trimmedSender = senderAddress.trim()
        val hasLetters = trimmedSender.any { it.isLetter() }
        val cleanAddress = if (hasLetters) {
            trimmedSender.filter { ch ->
                ch.isLetterOrDigit() || (ch in "+-_. ")
            }
                .trim()
                .take(28)
                .ifBlank { trimmedSender.take(24) }
        } else {
            MessageSecurityAnalyzer.sanitizePhoneNumber(trimmedSender)
                .ifBlank { trimmedSender.take(24) }
        }

        context?.let(::loadPersistedPins)
        if (isSenderBlocked(cleanAddress)) {
            Log.i(TAG, "Dropped incoming message from blocked sender: $cleanAddress")
            return
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
                    it.address.equals(cleanAddress, ignoreCase = true)
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
                    isPinned = isAddressPinned(cleanAddress)
                )
                listOf(created) + current
            }
            sortThreads(updated)
        }

        context?.let { ctx ->
            postIncomingMessageNotification(
                context = ctx,
                threadId = targetThreadId,
                senderName = resolvedName,
                senderAddress = cleanAddress,
                messageBody = cleanBody,
                securityAnalysis = securityResult,
            )
        }
    }

    private fun postIncomingMessageNotification(
        context: Context,
        threadId: Long,
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
        val titleText = when (securityAnalysis.riskLevel) {
            SecurityRiskLevel.SUSPICIOUS -> "⚠️ $senderName"
            SecurityRiskLevel.CAUTION -> "🛡️ $senderName"
            SecurityRiskLevel.SAFE -> senderName
        }

        val expandedBody = buildString {
            append(messageBody)
            securityAnalysis.extractedOtpCode?.let { otp ->
                append("\n")
                append(if (isSr) "🔑 Kod: $otp" else "🔑 Code: $otp")
            }
            if (securityAnalysis.riskLevel == SecurityRiskLevel.SUSPICIOUS) {
                append("\n")
                append(
                    if (isSr) "⚠️ Upozorenje: Mogući fišing ili rizičan link"
                    else "⚠️ Warning: Potential phishing or risky link"
                )
            }
        }

        val notificationId = (normalizeAddressKey(senderAddress).hashCode() and 0x7FFFFFFF).coerceAtLeast(1001)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_THREAD_NOTIFICATION
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

        // 2. Copy 2FA / OTP Code action from Notification if a code is detected
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
            sortThreads(updated)
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
     * Deletes a single message from a conversation thread.
     */
    fun deleteMessage(context: Context, threadId: Long, messageId: Long) {
        pinnedMessageIds.remove(messageId)
        highlightedMessageIds.remove(messageId)
        savePersistedPins(context)

        _threads.update { list ->
            val updated = list.map { thread ->
                if (thread.id == threadId) {
                    thread.copy(messages = thread.messages.filterNot { it.id == messageId })
                } else {
                    thread
                }
            }
            sortThreads(updated)
        }

        val appCtx = context.applicationContext
        ioScope.launch {
            if (!isDefaultSmsApp(appCtx)) return@launch
            try {
                if (messageId < 1_000_000_000L) {
                    appCtx.contentResolver.delete(
                        Telephony.Sms.CONTENT_URI,
                        "${Telephony.Sms._ID} = ?",
                        arrayOf(messageId.toString())
                    )
                }
            } catch (_: Exception) {
                // Ignore if read-only
            }
        }
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
        savePersistedPins(context)

        _deletedThreads.update { currentTrash ->
            currentTrash.filterNot { it.thread.id == threadId }
        }
        _threads.update { currentActive ->
            val merged = (currentActive.filterNot { it.id == threadId } + entry.thread)
            sortThreads(merged)
        }
        return entry.thread.contactName
    }

    /**
     * Permanently deletes a single conversation from the Deleted Messages (Trash) folder.
     */
    fun permanentlyDeleteThread(context: Context, threadId: Long) {
        deletedThreadTimestamps.remove(threadId)
        savePersistedPins(context)
        _deletedThreads.update { currentTrash ->
            currentTrash.filterNot { it.thread.id == threadId }
        }
        deleteFromSystemProviderIfDefault(context, threadId)
    }

    /**
     * Permanently empties all conversations in the Deleted Messages (Trash) folder.
     */
    fun emptyTrash(context: Context) {
        val idsToPurge = _deletedThreads.value.map { it.thread.id }
        idsToPurge.forEach { id ->
            deletedThreadTimestamps.remove(id)
            deleteFromSystemProviderIfDefault(context, id)
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
        val expiredIds = synchronized(deletedThreadTimestamps) {
            deletedThreadTimestamps.entries
                .filter { (_, deletedAt) -> (now - deletedAt) >= maxAgeMillis }
                .map { it.key }
        }
        if (expiredIds.isNotEmpty()) {
            expiredIds.forEach { id ->
                deletedThreadTimestamps.remove(id)
                deleteFromSystemProviderIfDefault(context, id)
            }
            savePersistedPins(context)
        }
        _deletedThreads.update { currentTrash ->
            currentTrash.filter { entry ->
                (now - entry.deletedAtMillis) < maxAgeMillis
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

    private fun createInitialDemoThreads(): List<ConversationThread> {
        val now = System.currentTimeMillis()
        val isSr = TxturaStrings.isSerbianLocale()

        val m1 = if (isSr) {
            "Zdravo! Stižeš li večeras na kafu u 19h? Poslala sam ti i sliku novog studija 📸"
        } else {
            "Hey! Are you coming for coffee at 7 PM tonight? Sent you a photo of the new studio 📸"
        }
        val m2 = if (isSr) {
            "Stižem sigurno! Izgleda odlično na slici, vidimo se u 19h."
        } else {
            "Definitely coming! Looks awesome in the photo, see you at 7 PM."
        }
        val m3 = if (isSr) {
            "Super, čuvam mesto na terasi ✨"
        } else {
            "Great, saving a table on the terrace ✨"
        }

        val bankMsg = if (isSr) {
            "Vaš jednokratni verifikacioni kod je 849204 za prijavu na mobilno bankarstvo. Nikada ne delite ovaj kod."
        } else {
            "Your one-time verification code is 849204 for mobile banking sign-in. Never share this code."
        }
        val courierMsg = if (isSr) {
            "Vaša pošiljka #RS-40912 biće isporučena danas između 14:00 i 16:00h. Kurir: Nikola (+381641234567)."
        } else {
            "Your parcel #TX-40912 will be delivered today between 2:00 PM and 4:00 PM. Courier: Alex (+381641234567)."
        }
        val phishingDemo = if (isSr) {
            "HITNA UPLATA: Vaš nalog je suspendovan. Verifikujte karticu odmah na http://bit.ly/banka-verifikacija.xyz kako biste izbegli blokadu."
        } else {
            "URGENT PAYMENT: Your account is suspended. Verify your card immediately at http://bit.ly/bank-verify-now.xyz to avoid lock."
        }

        return listOf(
            ConversationThread(
                id = 901L,
                contactName = "Milica Jovanović",
                address = "+381641122334",
                avatarInitials = "MJ",
                avatarAccentColorHex = 0xFF00E5FF,
                isPinned = true,
                unreadCount = 1,
                messages = listOf(
                    ChatMessage(
                        id = 1L,
                        threadId = 901L,
                        address = "+381641122334",
                        body = m1,
                        timestampMillis = now - 3_600_000L,
                        isIncoming = true,
                        type = MessageType.MMS,
                        isPinned = true,
                        attachment = MmsAttachment(
                            uri = "content://com.txtura.demo/studio_photo".toUri(),
                            mimeType = "image/jpeg",
                            caption = "studio_interior_hdr.jpg (1.4 MB)"
                        ),
                        securityAnalysis = MessageSecurityAnalyzer.analyzeMessage(m1)
                    ),
                    ChatMessage(
                        id = 2L,
                        threadId = 901L,
                        address = "+381641122334",
                        body = m2,
                        timestampMillis = now - 1_800_000L,
                        isIncoming = false,
                        type = MessageType.SMS,
                        status = DeliveryStatus.DELIVERED,
                        securityAnalysis = MessageSecurityAnalyzer.analyzeMessage(m2)
                    ),
                    ChatMessage(
                        id = 3L,
                        threadId = 901L,
                        address = "+381641122334",
                        body = m3,
                        timestampMillis = now - 300_000L,
                        isIncoming = true,
                        type = MessageType.SMS,
                        securityAnalysis = MessageSecurityAnalyzer.analyzeMessage(m3)
                    )
                )
            ),
            ConversationThread(
                id = 902L,
                contactName = if (isSr) "Intesa / 2FA Kod" else "Bank / 2FA Code",
                address = "BANK-OTP",
                avatarInitials = "2FA",
                avatarAccentColorHex = 0xFF10B981,
                isPinned = true,
                unreadCount = 1,
                messages = listOf(
                    ChatMessage(
                        id = 4L,
                        threadId = 902L,
                        address = "BANK-OTP",
                        body = bankMsg,
                        timestampMillis = now - 900_000L,
                        isIncoming = true,
                        type = MessageType.SMS,
                        isPinned = true,
                        securityAnalysis = MessageSecurityAnalyzer.analyzeMessage(bankMsg)
                    )
                )
            ),
            ConversationThread(
                id = 903L,
                contactName = if (isSr) "DExpress Kurir" else "Express Courier",
                address = "DEXPRESS",
                avatarInitials = "EC",
                avatarAccentColorHex = 0xFF6366F1,
                unreadCount = 0,
                messages = listOf(
                    ChatMessage(
                        id = 5L,
                        threadId = 903L,
                        address = "DEXPRESS",
                        body = courierMsg,
                        timestampMillis = now - 7_200_000L,
                        isIncoming = true,
                        type = MessageType.SMS,
                        securityAnalysis = MessageSecurityAnalyzer.analyzeMessage(courierMsg)
                    )
                )
            ),
            ConversationThread(
                id = 904L,
                contactName = if (isSr) "+381690009911 (Spam Štit)" else "+381690009911 (Spam Shield)",
                address = "+381690009911",
                avatarInitials = "!",
                avatarAccentColorHex = 0xFFF43F5E,
                unreadCount = 0,
                messages = listOf(
                    ChatMessage(
                        id = 6L,
                        threadId = 904L,
                        address = "+381690009911",
                        body = phishingDemo,
                        timestampMillis = now - 14_400_000L,
                        isIncoming = true,
                        type = MessageType.SMS,
                        securityAnalysis = MessageSecurityAnalyzer.analyzeMessage(phishingDemo)
                    )
                )
            )
        )
    }
}
