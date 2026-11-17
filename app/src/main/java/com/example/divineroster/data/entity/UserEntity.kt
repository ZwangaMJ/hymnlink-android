package com.example.divineroster.data.entity

import com.google.firebase.firestore.DocumentId

data class UserEntity(
    @DocumentId val id: String = "",
    val name: String = "",
    val role: String = "", // "ADMIN", "CONDUCTOR", "ORGANIST", "CHOIR_MEMBER", "MINISTER", or "LEADER"
    val email: String = "",
    val phone: String = "",
    val approvalStatus: String = "APPROVED", // "PENDING_APPROVAL" or "APPROVED"
    val hasLoginAccount: Boolean = false,
    val requestedRole: String = "",
    val createdAt: Long = 0L
)
