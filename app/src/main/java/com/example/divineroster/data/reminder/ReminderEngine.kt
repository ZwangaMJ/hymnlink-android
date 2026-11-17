package com.example.divineroster.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.divineroster.data.entity.AppointmentEntity
import com.example.divineroster.data.entity.isService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules the three reminders (6 days before, 3 days before, the day before) that the appointed
 * conductor of an upcoming divine service receives. Alarms are local to the device, so every
 * device schedules only its own user's reminders (see [ReminderSyncManager]).
 */
@Singleton
class ReminderEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private data class Milestone(val daysBefore: Int, val label: String)

    private val milestones = listOf(
        Milestone(6, "in 6 days"),
        Milestone(3, "in 3 days"),
        Milestone(1, "tomorrow")
    )

    private val prefs = context.getSharedPreferences("reminder_prefs", Context.MODE_PRIVATE)

    /**
     * Makes this device's alarms match [appointments] for the signed-in user: reminders exist for
     * every approved, upcoming service where [uid] is the conductor, and for nothing else
     * (reassigned, deleted or rejected appointments lose theirs).
     */
    fun syncForConductor(uid: String, appointments: List<AppointmentEntity>) {
        val now = System.currentTimeMillis()
        val eligible = appointments.filter {
            it.status == "APPROVED" && it.isService() && it.conductorId == uid && it.date > now
        }

        val previouslyScheduled = prefs.getStringSet(KEY_SCHEDULED_IDS, emptySet()).orEmpty()
        val eligibleIds = eligible.map { it.id }.toSet()

        (previouslyScheduled - eligibleIds).forEach { cancelReminders(it) }
        eligible.forEach { scheduleReminders(it) }

        prefs.edit().putStringSet(KEY_SCHEDULED_IDS, eligibleIds).apply()
    }

    /** Removes every reminder this device has scheduled (used when the user signs out). */
    fun cancelAll() {
        prefs.getStringSet(KEY_SCHEDULED_IDS, emptySet()).orEmpty().forEach { cancelReminders(it) }
        prefs.edit().remove(KEY_SCHEDULED_IDS).apply()
    }

    private fun scheduleReminders(appointment: AppointmentEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()

        milestones.forEachIndexed { index, milestone ->
            val triggerTime = reminderTime(appointment.date, milestone.daysBefore)
            if (triggerTime <= now) return@forEachIndexed // already past, e.g. the service was scheduled late

            val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
                action = ReminderBroadcastReceiver.ACTION_REMINDER
                putExtra(ReminderBroadcastReceiver.EXTRA_APPOINTMENT_ID, appointment.id)
                putExtra(ReminderBroadcastReceiver.EXTRA_SERVICE_TYPE, appointment.serviceType)
                putExtra(ReminderBroadcastReceiver.EXTRA_SERVICE_DATE, appointment.date)
                putExtra(ReminderBroadcastReceiver.EXTRA_TIME_LABEL, milestone.label)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCodeFor(appointment.id, index),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (canUseExactAlarms(alarmManager)) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
                Log.d(TAG, "Scheduled '${milestone.label}' reminder for appointment ${appointment.id} at $triggerTime")
            } catch (e: SecurityException) {
                Log.e(TAG, "Exact alarm denied, falling back to an inexact alarm", e)
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        }
    }

    private fun cancelReminders(appointmentId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        milestones.indices.forEach { index ->
            val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
                action = ReminderBroadcastReceiver.ACTION_REMINDER
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCodeFor(appointmentId, index),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    /** 08:00 local time, [daysBefore] days before the day of the service. */
    private fun reminderTime(serviceTime: Long, daysBefore: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = serviceTime
            add(Calendar.DAY_OF_YEAR, -daysBefore)
            set(Calendar.HOUR_OF_DAY, REMINDER_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun canUseExactAlarms(alarmManager: AlarmManager): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun requestCodeFor(appointmentId: String, index: Int): Int =
        appointmentId.hashCode() * 10 + index

    private companion object {
        const val TAG = "ReminderEngine"
        const val KEY_SCHEDULED_IDS = "scheduled_appointment_ids"
        const val REMINDER_HOUR = 8
    }
}
