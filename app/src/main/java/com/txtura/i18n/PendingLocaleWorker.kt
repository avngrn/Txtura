package com.txtura.i18n

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker.Result
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Background WorkManager job that periodically checks whether an OTA translation package
 * has become available for the user's active system locale (e.g., if the user's language
 * was not translated at initial install and fell back to English).
 *
 * Constraints:
 * - NetworkType.CONNECTED / UNMETERED (does not waste SIM data quota)
 * - BatteryNotLow (does not drain battery)
 */
class PendingLocaleWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val systemLocale = Locale.getDefault()
            val systemLang = systemLocale.language.lowercase(Locale.ROOT)

            // If system language is English, no OTA download needed
            if (systemLang == "en" || systemLang.isBlank()) {
                return Result.success()
            }

            Log.i(TAG, "Executing PendingLocaleWorker check for system language: $systemLang")

            val updated = OtaLocalizationManager.syncLanguagePackageOnline(
                context = applicationContext,
                targetLangCode = systemLang
            )

            if (updated) {
                Log.i(TAG, "Successfully synced pending OTA language package for: $systemLang")
            }

            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "PendingLocaleWorker execution failed silently", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "PendingLocaleWorker"
        private const val WORK_NAME = "txtura_pending_locale_sync_work"

        fun schedulePeriodicSync(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()

                val syncWorkRequest = PeriodicWorkRequestBuilder<PendingLocaleWorker>(
                    repeatInterval = 12,
                    repeatIntervalTimeUnit = TimeUnit.HOURS
                )
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    syncWorkRequest
                )

                Log.i(TAG, "Enqueued periodic OTA pending locale sync worker")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule PendingLocaleWorker", e)
            }
        }
    }
}
