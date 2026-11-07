package com.txtura.telephony

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PersistableBundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.txtura.data.SmsMmsRepository
import com.txtura.ui.i18n.TxturaStrings

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
            SmsMmsRepository.ACTION_NOTIFICATION_DIRECT_REPLY -> {
                val remoteInputBundle = RemoteInput.getResultsFromIntent(intent)
                val replyText = remoteInputBundle
                    ?.getCharSequence(SmsMmsRepository.KEY_NOTIFICATION_REPLY_TEXT)
                    ?.toString()
                    ?.trim()
                    .orEmpty()

                if (replyText.isNotEmpty() && senderAddress.isNotEmpty()) {
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
                        Toast.makeText(
                            appContext,
                            strings.notificationReplySentToast,
                            Toast.LENGTH_SHORT,
                        ).show()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send direct reply from notification", e)
                    }
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
