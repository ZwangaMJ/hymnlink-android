package com.example.divineroster.data.entity

import com.google.firebase.firestore.DocumentId

data class AppointmentEntity(
    @DocumentId val id: String = "",
    val date: Long = 0L, // timestamp in ms
    val serviceType: String = "",
    val conductorId: String? = null,
    val organistId: String? = null,
    val status: String = "PENDING_APPROVAL", // "APPROVED" or "PENDING_APPROVAL"
    val urgencyLevel: String = "Normal", // "Normal", "Urgent", or "High Importance"
    val notes: String = "",
    val createdByUid: String = ""
)
