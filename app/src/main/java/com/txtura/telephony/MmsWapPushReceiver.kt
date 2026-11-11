package com.txtura.telephony

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.txtura.data.SmsMmsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Required WAP Push receiver for Default SMS/MMS Role (ROLE_SMS).
 * Protected in AndroidManifest.xml by android.permission.BROADCAST_WAP_PUSH.
 */
class MmsWapPushReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if ((action != Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION) &&
            (action != Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION)
        ) {
            return
        }

        // The default SMS app receives both actions; only DELIVER is for processing.
        if ((action == Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION) &&
            (SmsMmsRepository.isDefaultSmsApp(context))
        ) {
            return
        }

        val pushData = intent.getByteArrayExtra("data")
        if (pushData == null || pushData.isEmpty()) {
            Log.w(TAG, "Empty WAP Push MMS payload received")
            return
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        @Suppress("DEPRECATION")
        val subId = (intent.extras?.get("subscription") as? Number)?.toInt() ?: -1

        CoroutineScope(Dispatchers.IO).launch {
            try {
                SmsMmsRepository.handleIncomingMmsWapPush(
                    context = appContext,
                    pushData = pushData,
                    subId = subId,
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error handling MMS WAP push", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "MmsWapPushReceiver"
    }
}
