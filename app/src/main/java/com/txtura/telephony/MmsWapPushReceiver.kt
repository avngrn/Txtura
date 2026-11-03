package com.txtura.telephony

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.txtura.data.SmsMmsRepository

/**
 * Required WAP Push receiver for Default SMS/MMS Role (ROLE_SMS).
 * Protected in AndroidManifest.xml by android.permission.BROADCAST_WAP_PUSH.
 */
class MmsWapPushReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION) {
            return
        }
        try {
            SmsMmsRepository.onIncomingMessageReceived(
                context = context.applicationContext,
                senderAddress = "MMS",
                rawBody = "Received new MMS multimedia message",
                isMms = true
            )
        } catch (e: Exception) {
            Log.e("MmsWapPushReceiver", "Error handling MMS WAP push", e)
        }
    }
}
