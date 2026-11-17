package com.example.divineroster.ui.schedule

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.divineroster.R
import com.example.divineroster.data.auth.AuthManager
import com.example.divineroster.data.auth.AuthState
import com.example.divineroster.data.entity.AppointmentEntity
import com.example.divineroster.data.entity.HymnSegment
import com.example.divineroster.data.entity.UserEntity
import com.example.divineroster.data.repository.AppointmentRepository
import com.example.divineroster.data.repository.UserRepository
import com.example.divineroster.databinding.ActivityRosterSchedulerBinding
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class RosterSchedulerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EVENT_KIND = "EXTRA_EVENT_KIND"
        const val EXTRA_PRE_SELECTED_SERVICE = "EXTRA_PRE_SELECTED_SERVICE"
        const val EXTRA_PRE_SELECTED_URGENCY = "EXTRA_PRE_SELECTED_URGENCY"
    }

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var appointmentRepository: AppointmentRepository

    @Inject
    lateinit var authManager: AuthManager

    private lateinit var binding: ActivityRosterSchedulerBinding

    private var calendar = Calendar.getInstance()

    private val serviceTypes = listOf("Sunday service", "Wednesday service")
    private val meetingTypes = listOf(
        "Choir meeting",
        "Sunday school meeting",
        "Ministers meeting",
        "Youth meeting",
        "Choir practice",
        "Orchestra practice"
    )

    private var eventKind = AppointmentEntity.KIND_MEETING
    private val isService get() = eventKind == AppointmentEntity.KIND_SERVICE

    private var conductors = listOf<UserEntity>()
    private var organists = listOf<UserEntity>()
    private var allMembers = listOf<UserEntity>()

    /** One input per hymn segment, keyed by HymnSegment.key. */
    private val hymnInputs = linkedMapOf<String, TextInputEditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRosterSchedulerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        eventKind = intent.getStringExtra(EXTRA_EVENT_KIND) ?: AppointmentEntity.KIND_MEETING

        applyEventKind()
        setupListeners()
        loadMembers()
        handleIntentExtras()
    }

    /** Shows the fields that belong to this kind of event and labels the screen accordingly. */
    private fun applyEventKind() {
        val typeOptions = if (isService) serviceTypes else meetingTypes
        binding.actvServiceType.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, typeOptions)
        )

        if (isService) {
            binding.tvScreenTitle.text = "Schedule service"
            binding.btnSchedule.text = "Schedule Service"
            binding.groupService.visibility = View.VISIBLE
            binding.groupMeeting.visibility = View.GONE
            binding.cardHymns.visibility = View.VISIBLE
            buildHymnFields()
        } else {
            binding.tvScreenTitle.text = "Schedule event"
            binding.btnSchedule.text = "Schedule Meeting"
            binding.groupMeeting.visibility = View.VISIBLE
            binding.groupService.visibility = View.GONE
            binding.cardHymns.visibility = View.GONE
        }
    }

    private fun buildHymnFields() {
        val inflater = LayoutInflater.from(this)
        HymnSegment.entries.forEach { segment ->
            val field = inflater.inflate(
                R.layout.item_hymn_field,
                binding.hymnFieldsContainer,
                false
            ) as com.google.android.material.textfield.TextInputLayout
            field.hint = segment.label
            binding.hymnFieldsContainer.addView(field)
            hymnInputs[segment.key] = field.findViewById(R.id.etHymn)
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.etDate.setOnClickListener { showDatePicker() }
        binding.btnSchedule.setOnClickListener { submitRosterSlot() }
        binding.tilMustAttend.setEndIconOnClickListener { showMemberPicker() }
    }

    private fun loadMembers() {
        lifecycleScope.launch {
            allMembers = userRepository.getAllUsers().first()

            if (isService) {
                conductors = allMembers.filter { it.role == "CONDUCTOR" }
                binding.actvConductor.setAdapter(
                    ArrayAdapter(this@RosterSchedulerActivity, android.R.layout.simple_dropdown_item_1line, conductors.map { it.name })
                )

                organists = allMembers.filter { it.role == "ORGANIST" }
                binding.actvOrganist.setAdapter(
                    ArrayAdapter(this@RosterSchedulerActivity, android.R.layout.simple_dropdown_item_1line, organists.map { it.name })
                )
            } else {
                binding.actvCoordinator.setAdapter(
                    ArrayAdapter(this@RosterSchedulerActivity, android.R.layout.simple_dropdown_item_1line, allMembers.map { it.name })
                )
            }
        }
    }

    /** Multi-select of members whose names are added to the free-text "Must attend" box. */
    private fun showMemberPicker() {
        if (allMembers.isEmpty()) {
            Toast.makeText(this, "No members to pick from yet - type the names instead", Toast.LENGTH_SHORT).show()
            return
        }

        val names = allMembers.map { it.name }.toTypedArray()
        val typedNames = binding.etMustAttend.text.toString()
            .split(",", "\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val checked = BooleanArray(names.size) { names[it] in typedNames }

        AlertDialog.Builder(this)
            .setTitle("Must attend")
            .setMultiChoiceItems(names, checked) { _, which, isChecked -> checked[which] = isChecked }
            .setPositiveButton("Done") { _, _ ->
                // Keep anything typed by hand that isn't a member, then add the picked members.
                val manual = typedNames.filter { it !in names }
                val picked = names.filterIndexed { index, _ -> checked[index] }
                binding.etMustAttend.setText((manual + picked).joinToString(", "))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleIntentExtras() {
        val preSelectedService = intent.getStringExtra(EXTRA_PRE_SELECTED_SERVICE)
        if (!preSelectedService.isNullOrEmpty()) {
            binding.actvServiceType.setText(preSelectedService, false)
        }

        when (intent.getStringExtra(EXTRA_PRE_SELECTED_URGENCY)) {
            "Normal" -> binding.rbNormal.isChecked = true
            "Urgent" -> binding.rbUrgent.isChecked = true
            "High Importance" -> binding.rbHighImportance.isChecked = true
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
        val eventType = binding.actvServiceType.text.toString().trim()
        val notes = binding.etNotes.text.toString().trim()

        if (dateText.isEmpty() || eventType.isEmpty()) {
            Toast.makeText(this, "Please fill in Date and Event Type", Toast.LENGTH_SHORT).show()
            return
        }

        var conductorId: String? = null
        var organistId: String? = null
        var coordinatorName = ""
        var mustAttend = ""
        var hymns = emptyMap<String, String>()

        if (isService) {
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

            hymns = hymnInputs
                .mapValues { (_, input) -> input.text.toString().trim() }
                .filterValues { it.isNotEmpty() }
        } else {
            coordinatorName = binding.actvCoordinator.text.toString().trim()
            mustAttend = binding.etMustAttend.text.toString().trim()
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
            serviceType = eventType,
            kind = eventKind,
            conductorId = conductorId,
            organistId = organistId,
            coordinatorName = coordinatorName,
            mustAttend = mustAttend,
            hymns = hymns,
            status = status,
            urgencyLevel = urgencyLevel,
            notes = notes,
            createdByUid = authManager.currentUid() ?: ""
        )

        binding.btnSchedule.isEnabled = false
        lifecycleScope.launch {
            try {
                appointmentRepository.insertAppointment(appointment)
            } catch (e: Exception) {
                Log.e("RosterScheduler", "Could not save the event", e)
                binding.btnSchedule.isEnabled = true
                Toast.makeText(this@RosterSchedulerActivity, "Could not save: ${e.localizedMessage ?: "unknown error"}", Toast.LENGTH_LONG).show()
                return@launch
            }

            val message = if (status == "APPROVED") {
                if (isService) "Service scheduled" else "Meeting scheduled"
            } else {
                "Submitted (pending admin approval)"
            }
            Toast.makeText(this@RosterSchedulerActivity, message, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
