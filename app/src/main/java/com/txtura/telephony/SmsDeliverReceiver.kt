package com.txtura.telephony

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.txtura.data.SmsMmsRepository

/**
 * Required broadcast receiver for Default SMS Role (ROLE_SMS).
 * Protected in AndroidManifest.xml by android.permission.BROADCAST_SMS so only the
 * Android system telephony framework can deliver SMS_DELIVER broadcasts.
 */
class SmsDeliverReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if ((action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) &&
            (action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
        ) {
            return
        }

        // When Txtura is the Default SMS App, Android delivers both SMS_DELIVER_ACTION and
        // SMS_RECEIVED_ACTION. Process SMS_DELIVER_ACTION and skip duplicate SMS_RECEIVED_ACTION.
        if ((action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) &&
            SmsMmsRepository.isDefaultSmsApp(context)
        ) {
            return
        }

        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) return

            val groupedBySender = messages.groupBy {
                it.displayOriginatingAddress ?: it.originatingAddress ?: "Unknown"
            }
            for ((sender, parts) in groupedBySender) {
                val fullBody = parts.joinToString(separator = "") {
                    it.displayMessageBody ?: it.messageBody.orEmpty()
                }
                if (fullBody.isNotBlank()) {
                    SmsMmsRepository.onIncomingMessageReceived(
                        context = context.applicationContext,
                        senderAddress = sender,
                        rawBody = fullBody,
                        isMms = false,
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("SmsDeliverReceiver", "Failed to parse incoming SMS PDU safely", e)
        }
    }
}
