package com.txtura

import android.app.Application
import android.os.StrictMode
import android.util.Log
import com.txtura.data.AppUpdateManager

class TxturaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupStrictMode()
        AppUpdateManager.startBackgroundUpdateMonitoring(this)
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
