package com.example.divineroster.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.divineroster.data.auth.AuthManager
import com.example.divineroster.data.auth.AuthState
import com.example.divineroster.data.entity.AppointmentEntity
import com.example.divineroster.data.entity.TaskEntity
import com.example.divineroster.data.repository.AppointmentRepository
import com.example.divineroster.data.repository.AppointmentWithMembers
import com.example.divineroster.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val taskRepository: TaskRepository,
    private val authManager: AuthManager
) : ViewModel() {

    val userRole: StateFlow<String> = authManager.authState
        .map { state -> (state as? AuthState.Approved)?.role ?: "GUEST" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "GUEST")

    val appointments: StateFlow<List<AppointmentWithMembers>> =
        appointmentRepository.getAllAppointmentsWithMembers()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> =
        taskRepository.getAllTasks()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun approveAppointment(appointmentId: String) {
        viewModelScope.launch {
            appointmentRepository.updateAppointmentStatus(appointmentId, "APPROVED")
        }
    }

    fun rejectAppointment(appointmentId: String) {
        viewModelScope.launch {
            val app = appointmentRepository.getAppointmentById(appointmentId)
            if (app != null) {
                appointmentRepository.deleteAppointment(app)
            }
        }
    }

    fun deleteAppointment(appointment: AppointmentEntity) {
        viewModelScope.launch {
            appointmentRepository.deleteAppointment(appointment)
        }
    }

    fun updateTaskCompletion(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            taskRepository.updateTaskCompletion(taskId, isCompleted)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
        }
    }
}
