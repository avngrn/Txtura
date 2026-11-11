package com.txtura.telephony

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PersistableBundle
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.txtura.data.SmsMmsRepository
import com.txtura.ui.i18n.TxturaStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles inline notification actions without opening the app:
 * 1. Direct Reply (`ACTION_NOTIFICATION_DIRECT_REPLY`) via [RemoteInput].
 * 2. One-tap 2FA / OTP Code Copy (`ACTION_NOTIFICATION_COPY_OTP`) to the system clipboard.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val appContext = context.applicationContext
        val strings = TxturaStrings.forLocale()

        val threadId = intent.getLongExtra(SmsMmsRepository.EXTRA_NOTIFICATION_THREAD_ID, -1L)
        val messageId = intent.getLongExtra(SmsMmsRepository.EXTRA_NOTIFICATION_MESSAGE_ID, -1L)
        val senderAddress = intent.getStringExtra(SmsMmsRepository.EXTRA_NOTIFICATION_ADDRESS).orEmpty().trim()
        val notificationId = intent.getIntExtra(SmsMmsRepository.EXTRA_NOTIFICATION_ID, -1)

        when (action) {
            SmsMmsRepository.ACTION_SMS_DELIVERED -> {
                val deliveredMsgId = intent.getLongExtra("message_id", -1L)
                if (deliveredMsgId > 0L) {
                    SmsMmsRepository.onMessageDelivered(deliveredMsgId, appContext)
                }
            }
            SmsMmsRepository.ACTION_SMS_SENT -> {
                val sentMsgId = intent.getLongExtra("message_id", -1L)
                if (sentMsgId > 0L) {
                    if (resultCode == android.app.Activity.RESULT_OK) {
                        SmsMmsRepository.onMessageDelivered(sentMsgId, appContext)
                    } else {
                        SmsMmsRepository.onMessageFailed(sentMsgId, appContext)
                    }
                }
            }
            SmsMmsRepository.ACTION_MMS_SENT -> {
                val sentMsgId = intent.getLongExtra("message_id", -1L)
                val pduPath = intent.getStringExtra("pdu_path")
                val httpStatus = intent.getIntExtra(SmsManager.EXTRA_MMS_HTTP_STATUS, -1)
                val mmsError = intent.getIntExtra("android.telephony.extra.MMS_ERROR_CODE", -1)
                if (resultCode != android.app.Activity.RESULT_OK) {
                    Log.w(TAG, "MMS send failed: resultCode=$resultCode, httpStatus=$httpStatus, mmsError=$mmsError, msgId=$sentMsgId")
                } else {
                    Log.d(TAG, "MMS send succeeded: msgId=$sentMsgId")
                }
                if (!pduPath.isNullOrBlank()) {
                    try { java.io.File(pduPath).delete() } catch (_: Exception) {}
                }
                if (sentMsgId > 0L) {
                    if (resultCode == android.app.Activity.RESULT_OK) {
                        SmsMmsRepository.onMessageDelivered(sentMsgId, appContext)
                    } else {
                        SmsMmsRepository.onMessageFailed(sentMsgId, appContext)
                    }
                }
            }
            SmsMmsRepository.ACTION_MMS_DOWNLOADED -> {
                val pduPath = intent.getStringExtra("pdu_path")
                val fallbackSender = intent.getStringExtra("sender_address").orEmpty()
                val existingMsgId = intent.getLongExtra("existing_message_id", -1L)
                val dlResultCode = resultCode
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        SmsMmsRepository.onMmsDownloaded(
                            context = appContext,
                            resultCode = dlResultCode,
                            pduPath = pduPath,
                            fallbackSender = fallbackSender,
                            existingMessageId = if (existingMsgId > 0) existingMsgId else null
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to handle downloaded MMS in receiver", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            SmsMmsRepository.ACTION_NOTIFICATION_DIRECT_REPLY -> {
                val remoteInputBundle = RemoteInput.getResultsFromIntent(intent)
                val replyText = remoteInputBundle
                    ?.getCharSequence(SmsMmsRepository.KEY_NOTIFICATION_REPLY_TEXT)
                    ?.toString()
                    ?.trim()
                    .orEmpty()

                if (replyText.isNotEmpty() && senderAddress.isNotEmpty()) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            SmsMmsRepository.initializePersistedState(appContext)
                            SmsMmsRepository.sendMessage(
                                context = appContext,
                                recipientAddress = senderAddress,
                                rawBody = replyText,
                                attachmentUri = null,
                            )
                            if (threadId > 0L) {
                                SmsMmsRepository.markThreadRead(appContext, threadId)
                            }
                            if (notificationId > 0) {
                                NotificationManagerCompat.from(appContext).cancel(notificationId)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to send direct reply from notification", e)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                    Toast.makeText(
                        appContext,
                        strings.notificationReplySentToast,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }

            SmsMmsRepository.ACTION_NOTIFICATION_MARK_READ -> {
                try {
                    SmsMmsRepository.initializePersistedState(appContext)
                    if (threadId > 0L) {
                        SmsMmsRepository.markThreadRead(appContext, threadId)
                    } else if (senderAddress.isNotEmpty()) {
                        val resolvedThreadId = SmsMmsRepository.getOrCreateConversationWithRecipient(
                            context = appContext,
                            recipientAddress = senderAddress,
                        )
                        SmsMmsRepository.markThreadRead(appContext, resolvedThreadId)
                    }
                    if (notificationId > 0) {
                        NotificationManagerCompat.from(appContext).cancel(notificationId)
                    }
                    Toast.makeText(
                        appContext,
                        strings.notificationMarkedReadToast,
                        Toast.LENGTH_SHORT,
                    ).show()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to mark thread read from notification", e)
                }
            }

            SmsMmsRepository.ACTION_NOTIFICATION_HIGHLIGHT_MSG -> {
                try {
                    SmsMmsRepository.initializePersistedState(appContext)
                    if ((threadId > 0L) && (messageId > 0L)) {
                        SmsMmsRepository.toggleHighlightMessage(appContext, threadId, messageId)
                    }
                    if (threadId > 0L) {
                        SmsMmsRepository.markThreadRead(appContext, threadId)
                    }
                    if (notificationId > 0) {
                        NotificationManagerCompat.from(appContext).cancel(notificationId)
                    }
                    Toast.makeText(
                        appContext,
                        strings.notificationHighlightedToast,
                        Toast.LENGTH_SHORT,
                    ).show()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to highlight message from notification", e)
                }
            }

            SmsMmsRepository.ACTION_NOTIFICATION_COPY_OTP -> {
                val otpCode = intent.getStringExtra(SmsMmsRepository.EXTRA_NOTIFICATION_OTP_CODE)
                    .orEmpty()
                    .trim()
                if (otpCode.isNotEmpty()) {
                    try {
                        val clipboard = appContext.getSystemService(ClipboardManager::class.java)
                        val clip = ClipData.newPlainText("OTP Code", otpCode)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            clip.description.extras = PersistableBundle().apply {
                                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                            }
                        }
                        @Suppress("UsePropertyAccessSyntax")
                        clipboard?.setPrimaryClip(clip)

                        if (threadId > 0L) {
                            SmsMmsRepository.markThreadRead(appContext, threadId)
                        }
                        if (notificationId > 0) {
                            NotificationManagerCompat.from(appContext).cancel(notificationId)
                        }
                        Toast.makeText(
                            appContext,
                            strings.codeCopiedToast(otpCode),
                            Toast.LENGTH_SHORT,
                        ).show()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to copy OTP code from notification", e)
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "NotifActionReceiver"
    }
}
