package com.example.divineroster.data.repository

import com.example.divineroster.data.entity.AppointmentEntity
import com.example.divineroster.data.entity.UserEntity
import com.example.divineroster.data.firestore.snapshotFlow
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class AppointmentWithMembers(
    val appointment: AppointmentEntity,
    val conductor: UserEntity?,
    val organist: UserEntity?
)

interface AppointmentRepository {
    fun getAllAppointmentsWithMembers(): Flow<List<AppointmentWithMembers>>
    suspend fun getAppointmentById(id: String): AppointmentEntity?
    suspend fun insertAppointment(appointment: AppointmentEntity): String
    suspend fun updateAppointmentStatus(appointmentId: String, status: String)
    suspend fun deleteAppointment(appointment: AppointmentEntity)
}

@Singleton
class AppointmentRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val userRepository: UserRepository
) : AppointmentRepository {

    private val appointmentsCollection get() = firestore.collection("appointments")

    private fun allAppointmentsFlow(): Flow<List<AppointmentEntity>> =
        appointmentsCollection.snapshotFlow()
            .map { snapshot -> snapshot.toObjects(AppointmentEntity::class.java).sortedBy { it.date } }

    override fun getAllAppointmentsWithMembers(): Flow<List<AppointmentWithMembers>> {
        return combine(
            allAppointmentsFlow(),
            userRepository.getAllUsers()
        ) { appointments, users ->
            val userMap = users.associateBy { it.id }
            appointments.map { appointment ->
                AppointmentWithMembers(
                    appointment = appointment,
                    conductor = appointment.conductorId?.let { userMap[it] },
                    organist = appointment.organistId?.let { userMap[it] }
                )
            }
        }
    }

    override suspend fun getAppointmentById(id: String): AppointmentEntity? =
        appointmentsCollection.document(id).get().await().toObject(AppointmentEntity::class.java)

    override suspend fun insertAppointment(appointment: AppointmentEntity): String {
        val docRef = appointmentsCollection.document()
        docRef.set(appointment.copy(id = docRef.id)).await()
        return docRef.id
    }

    override suspend fun updateAppointmentStatus(appointmentId: String, status: String) {
        appointmentsCollection.document(appointmentId).update("status", status).await()
    }

    override suspend fun deleteAppointment(appointment: AppointmentEntity) {
        appointmentsCollection.document(appointment.id).delete().await()
    }
}
