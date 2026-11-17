package com.example.divineroster.util

import com.example.divineroster.data.entity.hymnLines
import com.example.divineroster.data.entity.isService
import com.example.divineroster.data.repository.AppointmentWithMembers

/** The two "who" fields of an event: conductor/organist for services, coordinator/attendees for meetings. */
data class Assignments(
    val primaryLabel: String,
    val primaryValue: String,
    val secondaryLabel: String,
    val secondaryValue: String
)

fun AppointmentWithMembers.assignments(): Assignments =
    if (appointment.isService()) {
        Assignments(
            primaryLabel = "Conductor",
            primaryValue = conductor?.name ?: "TBD",
            secondaryLabel = "Organist",
            secondaryValue = organist?.name ?: "TBD"
        )
    } else {
        Assignments(
            primaryLabel = "Event coordinator",
            primaryValue = appointment.coordinatorName.ifBlank { "TBD" },
            secondaryLabel = "Must attend",
            secondaryValue = appointment.mustAttend.ifBlank { "TBD" }
        )
    }

/** "Opening Hymn (OH): 123" per line, or null when no hymn numbers were entered. */
fun AppointmentWithMembers.hymnsText(): String? =
    appointment.hymnLines()
        .takeIf { it.isNotEmpty() }
        ?.joinToString("\n") { (label, number) -> "$label: $number" }
