package com.example.divineroster.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.divineroster.data.entity.UserEntity
import com.example.divineroster.data.repository.UserRepository
import com.example.divineroster.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val TAG = "SignupActivity"

@AndroidEntryPoint
class SignupActivity : AppCompatActivity() {

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    @Inject
    lateinit var userRepository: UserRepository

    private lateinit var binding: ActivitySignupBinding

    private val roles = listOf("Conductor", "Organist", "Choir member", "Minister", "Leader")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        val roleAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, roles)
        binding.actvRole.setAdapter(roleAdapter)
        binding.actvRole.setText(roles[0], false)

        binding.btnSignUp.setOnClickListener { attemptSignUp() }
    }

    private fun attemptSignUp() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val selectedRole = binding.actvRole.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || selectedRole.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }
        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        val requestedRoleToken = when (selectedRole) {
            "Conductor" -> "CONDUCTOR"
            "Organist" -> "ORGANIST"
            "Choir member" -> "CHOIR_MEMBER"
            "Minister" -> "MINISTER"
            "Leader" -> "LEADER"
            else -> "CONDUCTOR"
        }

        Log.d(TAG, "Attempting sign up for $email, requestedRole=$requestedRoleToken")
        setLoading(true)
        lifecycleScope.launch {
            try {
                val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid ?: throw IllegalStateException("Missing user id after sign up")
                Log.d(TAG, "createUserWithEmailAndPassword succeeded, uid=$uid")

                val profile = UserEntity(
                    id = uid,
                    name = name,
                    role = "",
                    email = email,
                    phone = phone,
                    approvalStatus = "PENDING_APPROVAL",
                    hasLoginAccount = true,
                    requestedRole = requestedRoleToken,
                    createdAt = System.currentTimeMillis()
                )
                userRepository.insertUser(profile)
                Log.d(TAG, "Wrote pending profile doc for uid=$uid")

                startActivity(android.content.Intent(this@SignupActivity, PendingApprovalActivity::class.java))
                finish()
            } catch (e: Exception) {
                Log.e(TAG, "Sign up failed for $email", e)
                setLoading(false)
                val message = when (e) {
                    is FirebaseAuthUserCollisionException -> "That email is already registered"
                    is FirebaseAuthWeakPasswordException -> "Please choose a stronger password"
                    else -> e.localizedMessage ?: "Sign up failed"
                }
                Toast.makeText(this@SignupActivity, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.signupProgress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSignUp.isEnabled = !loading
    }
}
