package com.example.divineroster.data.repository

import com.example.divineroster.data.entity.AnnouncementEntity
import com.example.divineroster.data.firestore.snapshotFlow
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface AnnouncementRepository {
    /** Most important first, newest first within the same importance. */
    fun getAllAnnouncements(): Flow<List<AnnouncementEntity>>
    suspend fun postAnnouncement(announcement: AnnouncementEntity): String
    suspend fun deleteAnnouncement(announcement: AnnouncementEntity)
}

@Singleton
class AnnouncementRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : AnnouncementRepository {

    private val collection get() = firestore.collection("announcements")

    override fun getAllAnnouncements(): Flow<List<AnnouncementEntity>> =
        collection.snapshotFlow().map { snapshot ->
            snapshot.toObjects(AnnouncementEntity::class.java)
                .sortedWith(compareBy<AnnouncementEntity> { importanceRank(it.importance) }.thenByDescending { it.createdAt })
        }

    override suspend fun postAnnouncement(announcement: AnnouncementEntity): String {
        val docRef = collection.document()
        docRef.set(announcement.copy(id = docRef.id)).await()
        return docRef.id
    }

    override suspend fun deleteAnnouncement(announcement: AnnouncementEntity) {
        collection.document(announcement.id).delete().await()
    }

    private fun importanceRank(importance: String): Int = when (importance) {
        "High Importance" -> 0
        "Urgent" -> 1
        else -> 2
    }
}
