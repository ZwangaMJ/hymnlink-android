package com.example.divineroster.data.repository

import com.example.divineroster.data.entity.TaskEntity
import com.example.divineroster.data.firestore.snapshotFlow
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface TaskRepository {
    fun getAllTasks(): Flow<List<TaskEntity>>
    suspend fun insertTask(task: TaskEntity): String
    suspend fun updateTaskCompletion(taskId: String, isCompleted: Boolean)
    suspend fun deleteTask(task: TaskEntity)
}

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : TaskRepository {

    private val tasksCollection get() = firestore.collection("tasks")

    override fun getAllTasks(): Flow<List<TaskEntity>> =
        tasksCollection.snapshotFlow()
            .map { snapshot -> snapshot.toObjects(TaskEntity::class.java).sortedBy { it.dueDate } }

    override suspend fun insertTask(task: TaskEntity): String {
        val docRef = tasksCollection.document()
        docRef.set(task.copy(id = docRef.id)).await()
        return docRef.id
    }

    override suspend fun updateTaskCompletion(taskId: String, isCompleted: Boolean) {
        tasksCollection.document(taskId).update("isCompleted", isCompleted).await()
    }

    override suspend fun deleteTask(task: TaskEntity) {
        tasksCollection.document(task.id).delete().await()
    }
}
