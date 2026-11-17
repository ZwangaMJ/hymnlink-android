package com.example.divineroster.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.divineroster.data.entity.UserEntity
import com.example.divineroster.data.firestore.documentFlow
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AuthManager"

sealed class AuthState {
    object Loading : AuthState()
    object LoggedOut : AuthState()
    data class PendingApproval(val name: String) : AuthState()
    data class Approved(val uid: String, val name: String, val role: String) : AuthState()
}

@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext context: Context,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("divine_auth_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _darkModeFlow = MutableStateFlow(isDarkModeEnabled())
    val darkModeFlow: StateFlow<Boolean> = _darkModeFlow

    private fun firebaseUidFlow() = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser?.uid) }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val authState: StateFlow<AuthState> = firebaseUidFlow()
        .onEach { uid -> Log.d(TAG, "Firebase auth state changed, uid=$uid") }
        .flatMapLatest { uid ->
            if (uid == null) {
                flowOf(AuthState.LoggedOut)
            } else {
                firestore.collection("users").document(uid).documentFlow().map { snapshot ->
                    Log.d(TAG, "users/$uid snapshot: exists=${snapshot.exists()} fromCache=${snapshot.metadata.isFromCache}")

                    if (!snapshot.exists()) {
                        // Auth account exists (e.g. added directly in the Firebase console) but no
                        // Firestore profile was ever written for it (SignupActivity normally writes
                        // one). Auto-provision a pending profile so the user isn't stuck loading
                        // forever, and an admin can approve them from the console or the app.
                        Log.w(TAG, "No users/$uid profile doc found, auto-provisioning a pending one")
                        provisionMissingProfile(uid)
                    } else {
                        val user = snapshot.toObject(UserEntity::class.java)
                        when {
                            user == null -> {
                                Log.w(TAG, "users/$uid doc exists but failed to deserialize to UserEntity")
                                AuthState.Loading
                            }
                            user.approvalStatus == "APPROVED" -> {
                                Log.d(TAG, "users/$uid is APPROVED, role=${user.role}")
                                AuthState.Approved(uid, user.name, user.role)
                            }
                            else -> {
                                Log.d(TAG, "users/$uid is ${user.approvalStatus}, awaiting admin approval")
                                AuthState.PendingApproval(user.name)
                            }
                        }
                    }
                }
            }
        }
        .onEach { state -> Log.d(TAG, "authState -> $state") }
        .stateIn(scope, SharingStarted.Eagerly, AuthState.Loading)

    private suspend fun provisionMissingProfile(uid: String): AuthState {
        val email = firebaseAuth.currentUser?.email ?: ""
        val fallbackName = email.substringBefore("@").ifBlank { "New Member" }
        val profile = UserEntity(
            id = uid,
            name = fallbackName,
            role = "",
            email = email,
            phone = "",
            approvalStatus = "PENDING_APPROVAL",
            hasLoginAccount = true,
            requestedRole = "",
            createdAt = System.currentTimeMillis()
        )
        try {
            firestore.collection("users").document(uid).set(profile).await()
            Log.d(TAG, "Auto-provisioned pending profile for uid=$uid")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to auto-provision profile for uid=$uid", e)
        }
        return AuthState.PendingApproval(fallbackName)
    }

    fun logout() {
        Log.d(TAG, "logout() called")
        firebaseAuth.signOut()
    }

    fun currentUid(): String? = firebaseAuth.currentUser?.uid

    fun setDarkModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("KEY_DARK_MODE", enabled).apply()
        _darkModeFlow.value = enabled
    }

    fun isDarkModeEnabled(): Boolean {
        return prefs.getBoolean("KEY_DARK_MODE", false)
    }
}
