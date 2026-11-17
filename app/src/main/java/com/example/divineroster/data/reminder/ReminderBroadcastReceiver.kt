package com.example.divineroster.data.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.divineroster.ui.dashboard.DashboardActivity

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.example.divineroster.ACTION_REMINDER") {
            val appointmentId = intent.getStringExtra("EXTRA_APPOINTMENT_ID") ?: return
            val serviceType = intent.getStringExtra("EXTRA_SERVICE_TYPE") ?: "Church Service"
            val label = intent.getStringExtra("EXTRA_TIME_LABEL") ?: "Roster"

            sendLocalNotification(context, appointmentId, serviceType, label)
        }
    }

    private fun sendLocalNotification(context: Context, appointmentId: String, serviceType: String, label: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "divine_local_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Roster Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channels for local schedule alerts"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            appointmentId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "$label Alert - HymnLink"
        val message = "Reminder for upcoming $serviceType. Tap to view schedule details."

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(appointmentId.hashCode(), notification)
    }
}
