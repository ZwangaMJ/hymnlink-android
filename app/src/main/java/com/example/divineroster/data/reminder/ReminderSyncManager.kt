package com.example.divineroster.data.reminder

import android.util.Log
import com.example.divineroster.data.auth.AuthManager
import com.example.divineroster.data.auth.AuthState
import com.example.divineroster.data.entity.AppointmentEntity
import com.example.divineroster.data.repository.AppointmentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps this device's conductor reminders in step with the roster: whenever the signed-in user's
 * appointments change (live while the app is running, and from [ReminderSyncWorker] in the
 * background) the alarms are rebuilt, and they are cleared on sign-out.
 */
@Singleton
class ReminderSyncManager @Inject constructor(
    private val authManager: AuthManager,
    private val appointmentRepository: AppointmentRepository,
    private val reminderEngine: ReminderEngine
) {
    private class Snapshot(val uid: String?, val appointments: List<AppointmentEntity>)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            authManager.authState
                .flatMapLatest { state ->
                    when (state) {
                        is AuthState.Approved ->
                            appointmentRepository.getAllAppointments().map { Snapshot(state.uid, it) }
                        is AuthState.LoggedOut -> flowOf(Snapshot(null, emptyList()))
                        else -> emptyFlow() // still resolving, or waiting for approval
                    }
                }
                .catch { Log.e(TAG, "Reminder sync stopped", it) }
                .collect { snapshot -> apply(snapshot) }
        }
    }

    /** One-shot sync for the background worker. Returns false if it should be retried later. */
    suspend fun syncOnce(): Boolean {
        val state = withTimeoutOrNull(AUTH_TIMEOUT_MS) {
            authManager.authState.first { it !is AuthState.Loading }
        } ?: return false

        return when (state) {
            is AuthState.Approved -> {
                val appointments = withTimeoutOrNull(FETCH_TIMEOUT_MS) {
                    appointmentRepository.getAllAppointments().first()
                } ?: return false
                apply(Snapshot(state.uid, appointments))
                true
            }
            is AuthState.LoggedOut -> {
                apply(Snapshot(null, emptyList()))
                true
            }
            else -> true
        }
    }

    private fun apply(snapshot: Snapshot) {
        if (snapshot.uid == null) {
            reminderEngine.cancelAll()
        } else {
            reminderEngine.syncForConductor(snapshot.uid, snapshot.appointments)
        }
    }

    private companion object {
        const val TAG = "ReminderSyncManager"
        const val AUTH_TIMEOUT_MS = 15_000L
        const val FETCH_TIMEOUT_MS = 30_000L
    }
}
