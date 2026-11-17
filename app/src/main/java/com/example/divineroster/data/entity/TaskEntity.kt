package com.example.divineroster.data.entity

import com.google.firebase.firestore.DocumentId

data class TaskEntity(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val dueDate: Long = 0L,
    val isCompleted: Boolean = false,
    val assignedUserId: String? = null
)
