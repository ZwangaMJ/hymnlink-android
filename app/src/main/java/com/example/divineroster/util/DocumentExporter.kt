package com.example.divineroster.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.divineroster.data.repository.AppointmentWithMembers
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object DocumentExporter {

    fun exportToCsv(context: Context, appointments: List<AppointmentWithMembers>): File? {
        val csvHeader = "ID,Date,Event,Conductor / Event coordinator,Organist / Must attend,Hymns,Urgency,Status,Notes\n"
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        val csvContent = buildString {
            append(csvHeader)
            appointments.forEach { appWithMembers ->
                val app = appWithMembers.appointment
                val dateStr = dateFormat.format(Date(app.date))
                val assignments = appWithMembers.assignments()
                val conductorName = escapeCsv(assignments.primaryValue)
                val organistName = escapeCsv(assignments.secondaryValue)
                val hymns = escapeCsv(appWithMembers.hymnsText()?.replace("\n", "; ") ?: "")
                val urgency = escapeCsv(app.urgencyLevel)
                val serviceType = escapeCsv(app.serviceType)
                val status = escapeCsv(app.status)
                val notes = escapeCsv(app.notes)

                append("${app.id},$dateStr,$serviceType,$conductorName,$organistName,$hymns,$urgency,$status,$notes\n")
            }
        }

        return try {
            val exportDir = File(context.getExternalFilesDir(null), "Exports").apply { mkdirs() }
            val file = File(exportDir, "divine_roster_${System.currentTimeMillis()}.csv")
            FileOutputStream(file).use { out ->
                out.write(csvContent.toByteArray())
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportToPdf(context: Context, appointments: List<AppointmentWithMembers>): File? {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
        }
        val titlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 18f
            isFakeBoldText = true
        }
        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 13f
            isFakeBoldText = true
        }

        // Page info: A4 size is roughly 595 x 842 points
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        var yPosition = 50f

        // Draw title
        canvas.drawText("HymnLink - Roster Assignments", 40f, yPosition, titlePaint)
        yPosition += 20f

        val dateFormat = SimpleDateFormat("EEE, MMM dd, yyyy - HH:mm", Locale.getDefault())
        canvas.drawText("Generated on: ${dateFormat.format(Date())}", 40f, yPosition, textPaint)
        yPosition += 30f

        // Draw horizontal line
        paint.color = Color.LTGRAY
        paint.strokeWidth = 2f
        canvas.drawLine(40f, yPosition, 555f, yPosition, paint)
        yPosition += 30f

        // Draw table headers
        canvas.drawText("Date & Service", 40f, yPosition, headerPaint)
        canvas.drawText("Conductor / Coordinator", 200f, yPosition, headerPaint)
        canvas.drawText("Organist / Attendees", 350f, yPosition, headerPaint)
        canvas.drawText("Status", 480f, yPosition, headerPaint)
        yPosition += 20f

        canvas.drawLine(40f, yPosition, 555f, yPosition, paint)
        yPosition += 20f

        appointments.forEachIndexed { index, appWithMembers ->
            // Check if page overflow is imminent
            if (yPosition > 780f) {
                pdfDocument.finishPage(page)
                val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
                page = pdfDocument.startPage(newPageInfo)
                canvas = page.canvas
                yPosition = 50f
                canvas.drawLine(40f, yPosition, 555f, yPosition, paint)
                yPosition += 20f
            }

            val app = appWithMembers.appointment
            val dateStr = dateFormat.format(Date(app.date))
            val assignments = appWithMembers.assignments()
            val conductorName = assignments.primaryValue.take(22)
            val organistName = assignments.secondaryValue.take(22)

            canvas.drawText(dateStr, 40f, yPosition, textPaint)
            canvas.drawText(app.serviceType, 40f, yPosition + 15f, textPaint)

            canvas.drawText(conductorName, 200f, yPosition, textPaint)
            canvas.drawText(organistName, 350f, yPosition, textPaint)
            canvas.drawText(app.status, 480f, yPosition, textPaint)

            yPosition += 45f

            // Separator between items
            paint.color = Color.parseColor("#EEEEEE")
            canvas.drawLine(40f, yPosition - 10f, 555f, yPosition - 10f, paint)
        }

        pdfDocument.finishPage(page)

        return try {
            val exportDir = File(context.getExternalFilesDir(null), "Exports").apply { mkdirs() }
            val file = File(exportDir, "divine_roster_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            pdfDocument.close()
        }
    }

    private fun escapeCsv(value: String): String {
        var escaped = value.replace("\"", "\"\"")
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            escaped = "\"$escaped\""
        }
        return escaped
    }

    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}
