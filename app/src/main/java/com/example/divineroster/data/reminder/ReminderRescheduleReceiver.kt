package com.example.divineroster.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Alarms are wiped by a reboot or an app update, so rebuild them as soon as either happens. */
class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            ReminderSyncWorker.runOnce(context)
        }
    }
}
