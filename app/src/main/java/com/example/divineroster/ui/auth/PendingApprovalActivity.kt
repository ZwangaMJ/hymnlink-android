package com.example.divineroster.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.divineroster.data.auth.AuthManager
import com.example.divineroster.data.auth.AuthState
import com.example.divineroster.databinding.ActivityPendingApprovalBinding
import com.example.divineroster.ui.dashboard.DashboardActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PendingApprovalActivity : AppCompatActivity() {

    @Inject
    lateinit var authManager: AuthManager

    private lateinit var binding: ActivityPendingApprovalBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPendingApprovalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogout.setOnClickListener {
            authManager.logout()
            startActivity(
                Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            finish()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authManager.authState.collectLatest { state ->
                    Log.d("PendingApprovalActivity", "authState -> $state")
                    when (state) {
                        is AuthState.Approved -> {
                            startActivity(Intent(this@PendingApprovalActivity, DashboardActivity::class.java))
                            finish()
                        }
                        is AuthState.LoggedOut -> {
                            startActivity(
                                Intent(this@PendingApprovalActivity, LoginActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                            )
                            finish()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}
