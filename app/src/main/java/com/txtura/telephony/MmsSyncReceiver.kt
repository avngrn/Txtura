package com.txtura.telephony

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import com.txtura.data.SmsMmsRepository

/**
 * Periodically wakes up every ~3 minutes in the background to ensure:
 * 1. Stuck or failed outgoing MMS messages are automatically retried and sent.
 * 2. Pending or missed incoming MMS messages are automatically downloaded from carrier MMSC.
 */
class MmsSyncReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.i(TAG, "MmsSyncReceiver triggered with action: $action")

        // Trigger background MMS sync
        SmsMmsRepository.performBackgroundMmsSync(context)

        // Reschedule next alarm tick
        scheduleNextSyncAlarm(context)
    }

    companion object {
        private const val TAG = "MmsSyncReceiver"
        const val ACTION_MMS_SYNC = "com.txtura.action.MMS_SYNC"
        private const val SYNC_INTERVAL_MILLIS = 3 * 60 * 1000L // 3 minutes

        fun scheduleNextSyncAlarm(context: Context) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val intent = Intent(context, MmsSyncReceiver::class.java).apply {
                    action = ACTION_MMS_SYNC
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    1009,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val triggerAtMillis = SystemClock.elapsedRealtime() + SYNC_INTERVAL_MILLIS

                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule periodic MMS sync alarm", e)
            }
        }
    }
}
