package com.txtura

import android.app.Application
import com.txtura.data.SmsMmsRepository
import com.txtura.telephony.EmergencyAlertManager

/**
 * Custom Application class for Txtura.
 * Pre-warms in-memory message and telephony caches on background thread as soon as the
 * app process launches, ensuring instant zero-delay loading upon cold start and smooth transitions.
 */
class TxturaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Start cache and alert initialization asynchronously; Application startup stays non-blocking.
        SmsMmsRepository.initializePersistedState(this)
        EmergencyAlertManager.initialize(this)
    }
}
