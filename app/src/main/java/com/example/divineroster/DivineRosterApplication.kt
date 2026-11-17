package com.example.divineroster

import android.app.Application
import com.example.divineroster.data.reminder.ReminderSyncManager
import com.example.divineroster.data.reminder.ReminderSyncWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DivineRosterApplication : Application() {

    @Inject
    lateinit var reminderSyncManager: ReminderSyncManager

    override fun onCreate() {
        super.onCreate()
        // Conductor reminders follow the roster live while the app runs, and every few hours in the background.
        reminderSyncManager.start()
        ReminderSyncWorker.schedulePeriodic(this)
    }
}
