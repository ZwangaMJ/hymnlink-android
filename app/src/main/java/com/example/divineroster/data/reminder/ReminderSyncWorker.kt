package com.example.divineroster.data.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

/**
 * Background safety net: reminders are scheduled when the app sees a roster change, but a
 * conductor may be assigned while the app is closed, so the roster is re-checked periodically.
 */
class ReminderSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerEntryPoint {
        fun reminderSyncManager(): ReminderSyncManager
    }

    override suspend fun doWork(): Result {
        val manager = EntryPointAccessors
            .fromApplication(applicationContext, WorkerEntryPoint::class.java)
            .reminderSyncManager()
        return if (manager.syncOnce()) Result.success() else Result.retry()
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "reminder_sync_periodic"

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderSyncWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun runOnce(context: Context) {
            WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<ReminderSyncWorker>().build())
        }
    }
}
