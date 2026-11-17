package com.example.divineroster.data.reminder

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.divineroster.data.entity.AppointmentEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun scheduleReminders(context: Context, appointment: AppointmentEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intervals = listOf(
            TimeUnit.DAYS.toMillis(7) to "7-day",
            TimeUnit.DAYS.toMillis(3) to "3-day",
            0L to "Same-day"
        )

        intervals.forEachIndexed { index, (offset, label) ->
            val triggerTime = appointment.date - offset

            // Only schedule if the trigger time is in the future
            if (triggerTime > System.currentTimeMillis()) {
                val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
                    action = "com.example.divineroster.ACTION_REMINDER"
                    putExtra("EXTRA_APPOINTMENT_ID", appointment.id)
                    putExtra("EXTRA_SERVICE_TYPE", appointment.serviceType)
                    putExtra("EXTRA_TIME_LABEL", label)
                }

                // Request codes: unique per appointment and milestone index (Firestore ids are Strings, hash to an Int)
                val requestCode = requestCodeFor(appointment.id, index)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    } else {
                        alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    }
                    Log.d("ReminderEngine", "Scheduled $label alarm for appointment ${appointment.id} at $triggerTime")
                } catch (e: SecurityException) {
                    // Fallback in case SCHEDULE_EXACT_ALARM is denied
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                    Log.e("ReminderEngine", "SecurityException scheduling exact alarm, falling back to non-exact", e)
                }
            }
        }
    }

    fun cancelReminders(context: Context, appointment: AppointmentEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (index in 0..2) {
            val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
                action = "com.example.divineroster.ACTION_REMINDER"
            }
            val requestCode = requestCodeFor(appointment.id, index)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    private fun requestCodeFor(appointmentId: String, index: Int): Int =
        appointmentId.hashCode() * 10 + index
}
