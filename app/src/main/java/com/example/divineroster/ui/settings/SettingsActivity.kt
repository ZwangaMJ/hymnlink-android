package com.example.divineroster.ui.settings

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.divineroster.data.auth.AuthManager
import com.example.divineroster.data.auth.AuthState
import com.example.divineroster.data.repository.AppointmentRepository
import com.example.divineroster.databinding.ActivitySettingsBinding
import com.example.divineroster.ui.auth.LoginActivity
import com.example.divineroster.util.DocumentExporter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    @Inject
    lateinit var authManager: AuthManager

    @Inject
    lateinit var appointmentRepository: AppointmentRepository

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupListeners()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authManager.authState.collectLatest { state ->
                    if (state is AuthState.Approved) {
                        binding.tvUsername.text = "Name: ${state.name}"
                        binding.tvRole.text = "Role: ${state.role}"
                    }
                }
            }
        }
    }

    private fun setupUI() {
        binding.switchDarkMode.isChecked = authManager.isDarkModeEnabled()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        // Dark Mode Toggle
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            authManager.setDarkModeEnabled(isChecked)
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // Logout
        binding.btnLogout.setOnClickListener {
            authManager.logout()
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }

        // Export CSV
        binding.btnExportCsv.setOnClickListener {
            lifecycleScope.launch {
                val appointments = appointmentRepository.getAllAppointmentsWithMembers().first()
                if (appointments.isEmpty()) {
                    Toast.makeText(this@SettingsActivity, "No schedule data to export", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val file = DocumentExporter.exportToCsv(this@SettingsActivity, appointments)
                if (file != null) {
                    val uri = DocumentExporter.getUriForFile(this@SettingsActivity, file)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(shareIntent, "Share CSV Export"))
                } else {
                    Toast.makeText(this@SettingsActivity, "Failed to export CSV", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Export PDF
        binding.btnExportPdf.setOnClickListener {
            lifecycleScope.launch {
                val appointments = appointmentRepository.getAllAppointmentsWithMembers().first()
                if (appointments.isEmpty()) {
                    Toast.makeText(this@SettingsActivity, "No schedule data to export", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val file = DocumentExporter.exportToPdf(this@SettingsActivity, appointments)
                if (file != null) {
                    val uri = DocumentExporter.getUriForFile(this@SettingsActivity, file)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(shareIntent, "Share PDF Export"))
                } else {
                    Toast.makeText(this@SettingsActivity, "Failed to export PDF", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
