package com.txtura.telephony

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.txtura.data.SmsMmsRepository

/**
 * Background cache retention service that keeps decrypted conversation data
 * warm in memory across activity and screen transitions, preventing expensive re-decryption
 * of encrypted message stores when navigating or reopening the app.
 */
class TxturaMessageCacheService : Service() {

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): TxturaMessageCacheService = this@TxturaMessageCacheService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        SmsMmsRepository.initializePersistedState(applicationContext)
        return START_STICKY
    }

    companion object {
        fun start(context: Context) {
            try {
                val intent = Intent(context, TxturaMessageCacheService::class.java)
                context.startService(intent)
            } catch (_: Exception) {
                // Ignore if background execution limits apply on restricted configurations
            }
        }
    }
}
