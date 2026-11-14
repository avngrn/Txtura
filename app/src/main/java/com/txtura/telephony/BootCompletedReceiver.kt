package com.txtura.telephony

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.txtura.data.AppUpdateManager
import com.txtura.data.SmsMmsRepository

/**
 * Restores persisted scheduled SMS/MMS messages and background update checks
 * automatically when the device finishes booting or when the app is updated.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            try {
                val appContext = context.applicationContext
                SmsMmsRepository.initializePersistedState(appContext)
                AppUpdateManager.startBackgroundUpdateMonitoring(appContext)
            } catch (e: Exception) {
                Log.w("BootCompletedReceiver", "Failed to restore state after $action", e)
            }
        }
    }
}
