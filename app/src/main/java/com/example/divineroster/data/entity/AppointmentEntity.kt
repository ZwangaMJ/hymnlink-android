package com.example.divineroster.data.entity

import com.google.firebase.firestore.DocumentId

data class AppointmentEntity(
    @DocumentId val id: String = "",
    val date: Long = 0L, // timestamp in ms
    val serviceType: String = "", // free text: e.g. "Sunday service", "Choir meeting", or anything typed in manually
    val kind: String = "", // KIND_SERVICE or KIND_MEETING ("" on legacy documents, see isService())
    val conductorId: String? = null, // services: the appointed conductor
    val organistId: String? = null, // services: the appointed organist
    val coordinatorName: String = "", // meetings: event coordinator (picked or typed)
    val mustAttend: String = "", // meetings: who must be present (picked and/or typed)
    val hymns: Map<String, String> = emptyMap(), // services: hymn numbers keyed by HymnSegment.key
    val status: String = "PENDING_APPROVAL", // "APPROVED" or "PENDING_APPROVAL"
    val urgencyLevel: String = "Normal", // "Normal", "Urgent", or "High Importance"
    val notes: String = "",
    val createdByUid: String = ""
) {
    companion object {
        const val KIND_SERVICE = "SERVICE"
        const val KIND_MEETING = "MEETING"
    }
}

/** Legacy documents have no `kind`; fall back to the event type text. */
fun AppointmentEntity.isService(): Boolean =
    kind == AppointmentEntity.KIND_SERVICE ||
        (kind.isEmpty() && serviceType.contains("service", ignoreCase = true))

/** Hymn numbers that were actually filled in, in service order. */
fun AppointmentEntity.hymnLines(): List<Pair<String, String>> =
    HymnSegment.entries.mapNotNull { segment ->
        hymns[segment.key]?.trim()?.takeIf { it.isNotEmpty() }?.let { segment.label to it }
    }
