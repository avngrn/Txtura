package com.txtura.telephony

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.telephony.TelephonyManager
import android.util.Log
import com.txtura.data.SmsMmsRepository
import com.txtura.security.MessageSecurityAnalyzer

/**
 * Required service for Default SMS Role (ROLE_SMS) to support "Respond via Message"
 * from incoming phone call rejection.
 * Guarded in AndroidManifest.xml with android.permission.SEND_RESPOND_VIA_MESSAGE.
 */
class HeadlessSmsSendService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == TelephonyManager.ACTION_RESPOND_VIA_MESSAGE) {
            try {
                val uri = intent.data
                val rawRecipient = uri?.schemeSpecificPart?.substringBefore('?').orEmpty()
                val rawMessage = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()

                val cleanRecipient = MessageSecurityAnalyzer.sanitizePhoneNumber(rawRecipient)
                val cleanMessage = MessageSecurityAnalyzer.sanitizeMessageText(rawMessage)

                if (cleanRecipient.isNotBlank() && cleanMessage.isNotBlank()) {
                    SmsMmsRepository.sendMessage(
                        context = applicationContext,
                        recipientAddress = cleanRecipient,
                        rawBody = cleanMessage
                    )
                }
            } catch (e: SecurityException) {
                Log.e("HeadlessSmsSendService", "SecurityException in RespondViaMessage", e)
            } catch (e: Exception) {
                Log.e("HeadlessSmsSendService", "Failed to process RespondViaMessage", e)
            }
        }
        stopSelfResult(startId)
        return START_NOT_STICKY
    }
}
