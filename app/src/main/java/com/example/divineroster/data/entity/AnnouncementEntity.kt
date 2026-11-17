package com.example.divineroster.data.entity

import com.google.firebase.firestore.DocumentId

data class AnnouncementEntity(
    @DocumentId val id: String = "",
    val text: String = "",
    val importance: String = "Normal", // "Normal", "Urgent", or "High Importance"
    val createdAt: Long = 0L,
    val createdByUid: String = "",
    val createdByName: String = ""
)
