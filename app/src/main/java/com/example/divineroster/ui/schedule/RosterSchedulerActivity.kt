package com.example.divineroster.ui.schedule

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.divineroster.data.auth.AuthManager
import com.example.divineroster.data.auth.AuthState
import com.example.divineroster.data.entity.AppointmentEntity
import com.example.divineroster.data.entity.UserEntity
import com.example.divineroster.data.reminder.ReminderEngine
import com.example.divineroster.data.repository.AppointmentRepository
import com.example.divineroster.data.repository.UserRepository
import com.example.divineroster.databinding.ActivityRosterSchedulerBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class RosterSchedulerActivity : AppCompatActivity() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var appointmentRepository: AppointmentRepository

    @Inject
    lateinit var authManager: AuthManager

    @Inject
    lateinit var reminderEngine: ReminderEngine

    private lateinit var binding: ActivityRosterSchedulerBinding

    private var calendar = Calendar.getInstance()
    private val serviceTypes = listOf(
        "Choir meeting",
        "Sunday school meeting",
        "Ministers meeting",
        "Youth meeting",
        "Sunday service",
        "Wednesday service",
        "Choir practice",
        "Orchestra practice"
    )

    private var conductors = listOf<UserEntity>()
    private var organists = listOf<UserEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRosterSchedulerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        loadOperators()
        setupDropdowns()
        handleIntentExtras()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        // Date Picker Trigger
        binding.etDate.setOnClickListener {
            showDatePicker()
        }

        // Submit Button Click
        binding.btnSchedule.setOnClickListener {
            submitRosterSlot()
        }
    }

    private fun loadOperators() {
        lifecycleScope.launch {
            conductors = userRepository.getUsersByRole("CONDUCTOR").first()
            val conductorNames = conductors.map { it.name }
            val conductorAdapter = ArrayAdapter(this@RosterSchedulerActivity, android.R.layout.simple_dropdown_item_1line, conductorNames)
            binding.actvConductor.setAdapter(conductorAdapter)

            organists = userRepository.getUsersByRole("ORGANIST").first()
            val organistNames = organists.map { it.name }
            val organistAdapter = ArrayAdapter(this@RosterSchedulerActivity, android.R.layout.simple_dropdown_item_1line, organistNames)
            binding.actvOrganist.setAdapter(organistAdapter)
        }
    }

    private fun setupDropdowns() {
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, serviceTypes)
        binding.actvServiceType.setAdapter(typeAdapter)
    }

    private fun handleIntentExtras() {
        val preSelectedService = intent.getStringExtra("EXTRA_PRE_SELECTED_SERVICE")
        if (!preSelectedService.isNullOrEmpty()) {
            binding.actvServiceType.setText(preSelectedService, false)
        }

        val preSelectedUrgency = intent.getStringExtra("EXTRA_PRE_SELECTED_URGENCY")
        if (!preSelectedUrgency.isNullOrEmpty()) {
            when (preSelectedUrgency) {
                "Normal" -> binding.rbNormal.isChecked = true
                "Urgent" -> binding.rbUrgent.isChecked = true
                "High Importance" -> binding.rbHighImportance.isChecked = true
            }
        }
    }

    private fun showDatePicker() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            showTimePicker()
        }

        DatePickerDialog(
            this, dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker() {
        val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            val format = SimpleDateFormat("EEE, MMM dd, yyyy - HH:mm", Locale.getDefault())
            binding.etDate.setText(format.format(calendar.time))
        }

        TimePickerDialog(
            this, timeSetListener,
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun submitRosterSlot() {
        val dateText = binding.etDate.text.toString().trim()
        val serviceType = binding.actvServiceType.text.toString().trim()
        val notes = binding.etNotes.text.toString().trim()

        if (dateText.isEmpty() || serviceType.isEmpty()) {
            Toast.makeText(this, "Please fill in Date and Event Type", Toast.LENGTH_SHORT).show()
            return
        }

        var conductorId: String? = null
        var organistId: String? = null

        val condName = binding.actvConductor.text.toString()
        if (condName.isNotEmpty()) {
            val match = conductors.firstOrNull { it.name == condName }
            if (match == null) {
                Toast.makeText(this, "Selected conductor is invalid", Toast.LENGTH_SHORT).show()
                return
            }
            conductorId = match.id
        }

        val orgName = binding.actvOrganist.text.toString()
        if (orgName.isNotEmpty()) {
            val match = organists.firstOrNull { it.name == orgName }
            if (match == null) {
                Toast.makeText(this, "Selected organist is invalid", Toast.LENGTH_SHORT).show()
                return
            }
            organistId = match.id
        }

        val urgencyLevel = when (binding.rgUrgencyLevel.checkedRadioButtonId) {
            binding.rbNormal.id -> "Normal"
            binding.rbUrgent.id -> "Urgent"
            binding.rbHighImportance.id -> "High Importance"
            else -> "Normal"
        }

        val currentState = authManager.authState.value
        val role = (currentState as? AuthState.Approved)?.role ?: "MEMBER"
        val status = if (role == "ADMIN") "APPROVED" else "PENDING_APPROVAL"

        val appointment = AppointmentEntity(
            date = calendar.timeInMillis,
            serviceType = serviceType,
            conductorId = conductorId,
            organistId = organistId,
            status = status,
            urgencyLevel = urgencyLevel,
            notes = notes,
            createdByUid = authManager.currentUid() ?: ""
        )

        lifecycleScope.launch {
            val newId = appointmentRepository.insertAppointment(appointment)
            val savedAppointment = appointment.copy(id = newId)

            if (status == "APPROVED") {
                reminderEngine.scheduleReminders(this@RosterSchedulerActivity, savedAppointment)
                Toast.makeText(this@RosterSchedulerActivity, "Event scheduled and local alerts activated", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@RosterSchedulerActivity, "Event submitted (pending admin approval)", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }
}
