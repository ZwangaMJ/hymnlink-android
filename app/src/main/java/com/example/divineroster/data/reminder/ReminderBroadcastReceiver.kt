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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REMINDER = "com.example.divineroster.ACTION_REMINDER"
        const val EXTRA_APPOINTMENT_ID = "EXTRA_APPOINTMENT_ID"
        const val EXTRA_SERVICE_TYPE = "EXTRA_SERVICE_TYPE"
        const val EXTRA_SERVICE_DATE = "EXTRA_SERVICE_DATE"
        const val EXTRA_TIME_LABEL = "EXTRA_TIME_LABEL"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMINDER) return

        val appointmentId = intent.getStringExtra(EXTRA_APPOINTMENT_ID) ?: return
        val serviceType = intent.getStringExtra(EXTRA_SERVICE_TYPE) ?: "divine service"
        val serviceDate = intent.getLongExtra(EXTRA_SERVICE_DATE, 0L)
        val label = intent.getStringExtra(EXTRA_TIME_LABEL) ?: "soon"

        sendLocalNotification(context, appointmentId, serviceType, serviceDate, label)
    }

    private fun sendLocalNotification(
        context: Context,
        appointmentId: String,
        serviceType: String,
        serviceDate: Long,
        label: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "divine_local_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Conductor Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders before a service you are appointed to conduct"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val notificationId = appointmentId.hashCode() * 31 + label.hashCode()
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val whenText = if (serviceDate > 0) {
            SimpleDateFormat("EEEE, d MMMM 'at' HH:mm", Locale.getDefault()).format(Date(serviceDate))
        } else {
            label
        }
        val title = "You conduct $serviceType $label"
        val message = "$whenText. Tap to see the hymns and details."

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
