package com.txtura

import android.app.Application
import android.os.StrictMode
import android.util.Log
import com.txtura.i18n.OtaLocalizationManager
import com.txtura.i18n.PendingLocaleWorker

class TxturaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupStrictMode()
        setupOtaLocalization()
    }

    private fun setupOtaLocalization() {
        // Zero-blocking async initialization on Dispatchers.IO for Cold Start protection
        OtaLocalizationManager.initializeAsync(this)
        PendingLocaleWorker.schedulePeriodicSync(this)
    }

    private fun setupStrictMode() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )

            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .detectActivityLeaks()
                    .penaltyLog()
                    .build()
            )
            Log.d("TxturaApplication", "StrictMode initialized for DEBUG build")
        }
    }
}
