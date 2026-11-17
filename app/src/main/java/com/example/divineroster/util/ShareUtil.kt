package com.example.divineroster.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.divineroster.data.repository.AppointmentWithMembers
import java.text.SimpleDateFormat
import java.util.*

object ShareUtil {

    fun shareToWhatsApp(context: Context, appointment: AppointmentWithMembers) {
        val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(appointment.appointment.date))

        val assignments = appointment.assignments()
        val notes = appointment.appointment.notes.ifEmpty { "None" }
        val hymns = appointment.hymnsText()

        val shareMessage = buildString {
            append("⛪ *HymnLink Assignment* ⛪\n\n")
            append("📅 *Date*: $formattedDate\n")
            append("🛠 *Event*: ${appointment.appointment.serviceType}\n")
            append("🎤 *${assignments.primaryLabel}*: ${assignments.primaryValue}\n")
            append("🎹 *${assignments.secondaryLabel}*: ${assignments.secondaryValue}\n")
            if (hymns != null) append("🎶 *Hymns*:\n$hymns\n")
            append("📝 *Notes*: $notes\n\n")
            append("Please confirm your availability as soon as possible. Thank you!")
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareMessage)
        }

        // Try launching directly to WhatsApp
        val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareMessage)
            setPackage("com.whatsapp")
        }

        try {
            context.startActivity(whatsappIntent)
        } catch (e: Exception) {
            // Fallback to standard share chooser
            try {
                val chooser = Intent.createChooser(sendIntent, "Share Roster Assignment")
                context.startActivity(chooser)
            } catch (ex: Exception) {
                Toast.makeText(context, "No app available to share assignment", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
