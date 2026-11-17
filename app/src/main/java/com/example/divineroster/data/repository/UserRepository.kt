package com.example.divineroster.data.repository

import com.example.divineroster.data.entity.UserEntity
import com.example.divineroster.data.firestore.snapshotFlow
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface UserRepository {
    fun getAllUsers(): Flow<List<UserEntity>>
    fun getUsersByRole(role: String): Flow<List<UserEntity>>
    fun getPendingSignups(): Flow<List<UserEntity>>
    suspend fun getUserById(id: String): UserEntity?
    suspend fun insertUser(user: UserEntity): String
    suspend fun approveUser(userId: String, role: String)
    suspend fun deleteUser(user: UserEntity)
}

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRepository {

    private val usersCollection get() = firestore.collection("users")

    override fun getAllUsers(): Flow<List<UserEntity>> =
        usersCollection
            .whereEqualTo("approvalStatus", "APPROVED")
            .snapshotFlow()
            .map { snapshot -> snapshot.toObjects(UserEntity::class.java).sortedBy { it.name } }

    override fun getUsersByRole(role: String): Flow<List<UserEntity>> =
        usersCollection
            .whereEqualTo("role", role)
            .whereEqualTo("approvalStatus", "APPROVED")
            .snapshotFlow()
            .map { snapshot -> snapshot.toObjects(UserEntity::class.java).sortedBy { it.name } }

    override fun getPendingSignups(): Flow<List<UserEntity>> =
        usersCollection
            .whereEqualTo("approvalStatus", "PENDING_APPROVAL")
            .whereEqualTo("hasLoginAccount", true)
            .snapshotFlow()
            .map { snapshot -> snapshot.toObjects(UserEntity::class.java).sortedBy { it.createdAt } }

    override suspend fun getUserById(id: String): UserEntity? =
        usersCollection.document(id).get().await().toObject(UserEntity::class.java)

    override suspend fun insertUser(user: UserEntity): String {
        val docRef = if (user.id.isNotBlank()) usersCollection.document(user.id) else usersCollection.document()
        docRef.set(user.copy(id = docRef.id)).await()
        return docRef.id
    }

    override suspend fun approveUser(userId: String, role: String) {
        usersCollection.document(userId)
            .update(mapOf("role" to role, "approvalStatus" to "APPROVED"))
            .await()
    }

    override suspend fun deleteUser(user: UserEntity) {
        usersCollection.document(user.id).delete().await()
    }
}
