package com.txtura.telephony

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.txtura.data.SmsMmsRepository

/**
 * JobScheduler service that runs background MMS sync when cellular data/network is connected.
 */
class MmsSyncJobService : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.i(TAG, "MmsSyncJobService started")
        val context = applicationContext
        SmsMmsRepository.performBackgroundMmsSync(context)
        MmsSyncReceiver.scheduleNextSyncAlarm(context)
        jobFinished(params, false)
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return false
    }

    companion object {
        private const val TAG = "MmsSyncJobService"
        const val JOB_ID_MMS_SYNC = 99881

        fun scheduleJob(context: Context) {
            try {
                val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
                val serviceComponent = ComponentName(context, MmsSyncJobService::class.java)
                val builder = JobInfo.Builder(JOB_ID_MMS_SYNC, serviceComponent)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPersisted(true)

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    builder.setPeriodic(15 * 60 * 1000L, 5 * 60 * 1000L)
                } else {
                    builder.setPeriodic(15 * 60 * 1000L)
                }

                scheduler.schedule(builder.build())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule MmsSyncJobService", e)
            }
        }
    }
}
