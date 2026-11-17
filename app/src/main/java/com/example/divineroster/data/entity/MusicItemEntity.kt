package com.example.divineroster.data.entity

import com.google.firebase.firestore.DocumentId

data class MusicItemEntity(
    @DocumentId val id: String = "",
    val title: String = "",
    val category: String = CATEGORY_SHEET, // CATEGORY_SHEET or CATEGORY_AUDIO
    val fileName: String = "",
    val mimeType: String = "",
    val sizeBytes: Long = 0L,
    val storagePath: String = "",
    val downloadUrl: String = "",
    val uploadedByUid: String = "",
    val uploadedByName: String = "",
    val createdAt: Long = 0L
) {
    companion object {
        const val CATEGORY_SHEET = "SHEET"
        const val CATEGORY_AUDIO = "AUDIO"
    }
}
