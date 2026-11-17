package com.example.divineroster.ui.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.divineroster.data.auth.AuthManager
import com.example.divineroster.data.auth.AuthState
import com.example.divineroster.data.entity.AnnouncementEntity
import com.example.divineroster.data.entity.AppointmentEntity
import com.example.divineroster.data.entity.WordOfTheDay
import com.example.divineroster.data.repository.AnnouncementRepository
import com.example.divineroster.data.repository.AppointmentRepository
import com.example.divineroster.data.repository.AppointmentWithMembers
import com.example.divineroster.data.repository.WordOfTheDayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val announcementRepository: AnnouncementRepository,
    private val wordOfTheDayRepository: WordOfTheDayRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private companion object {
        const val TAG = "DashboardViewModel"
    }

    val userRole: StateFlow<String> = authManager.authState
        .map { state -> (state as? AuthState.Approved)?.role ?: "GUEST" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "GUEST")

    val appointments: StateFlow<List<AppointmentWithMembers>> =
        appointmentRepository.getAllAppointmentsWithMembers()
            .catch { error -> Log.e(TAG, "Appointments listener failed", error); emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val announcements: StateFlow<List<AnnouncementEntity>> =
        announcementRepository.getAllAnnouncements()
            // e.g. permission-denied before the new Firestore rules are published: show nothing instead of crashing
            .catch { error -> Log.e(TAG, "Announcements listener failed", error); emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wordOfTheDay: StateFlow<WordOfTheDay?> =
        wordOfTheDayRepository.observeWordOfTheDay()
            .catch { error -> Log.e(TAG, "Word of the day listener failed", error); emit(null) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** One-off messages for the screen to show (e.g. a write that was rejected by the server). */
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private fun launchWrite(failureMessage: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                Log.e(TAG, failureMessage, e)
                _messages.tryEmit("$failureMessage: ${e.localizedMessage ?: "unknown error"}")
            }
        }
    }

    fun approveAppointment(appointmentId: String) = launchWrite("Could not approve the event") {
        appointmentRepository.updateAppointmentStatus(appointmentId, "APPROVED")
    }

    fun rejectAppointment(appointmentId: String) = launchWrite("Could not reject the event") {
        appointmentRepository.getAppointmentById(appointmentId)?.let { appointmentRepository.deleteAppointment(it) }
    }

    fun deleteAppointment(appointment: AppointmentEntity) = launchWrite("Could not delete the event") {
        appointmentRepository.deleteAppointment(appointment)
    }

    fun postAnnouncement(text: String, importance: String) {
        val author = authManager.authState.value as? AuthState.Approved ?: return
        launchWrite("Could not post the announcement") {
            announcementRepository.postAnnouncement(
                AnnouncementEntity(
                    text = text,
                    importance = importance,
                    createdAt = System.currentTimeMillis(),
                    createdByUid = author.uid,
                    createdByName = author.name
                )
            )
        }
    }

    fun deleteAnnouncement(announcement: AnnouncementEntity) = launchWrite("Could not delete the announcement") {
        announcementRepository.deleteAnnouncement(announcement)
    }

    fun publishWordOfTheDay(text: String, reference: String) {
        val author = authManager.authState.value as? AuthState.Approved ?: return
        launchWrite("Could not publish the word of the day") {
            wordOfTheDayRepository.publish(
                WordOfTheDay(
                    text = text,
                    reference = reference,
                    updatedAt = System.currentTimeMillis(),
                    updatedByName = author.name
                )
            )
        }
    }
}
