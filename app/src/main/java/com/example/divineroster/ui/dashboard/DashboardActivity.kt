package com.example.divineroster.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.divineroster.R
import com.example.divineroster.data.auth.AuthManager
import com.example.divineroster.data.auth.AuthState
import com.example.divineroster.data.repository.AppointmentWithMembers
import com.example.divineroster.databinding.ActivityDashboardBinding
import com.example.divineroster.ui.auth.LoginActivity
import com.example.divineroster.ui.auth.PendingApprovalActivity
import com.example.divineroster.ui.schedule.RosterSchedulerActivity
import com.example.divineroster.ui.settings.SettingsActivity
import com.example.divineroster.ui.user.ManageUsersActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class DashboardActivity : AppCompatActivity() {

    @Inject
    lateinit var authManager: AuthManager

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var binding: ActivityDashboardBinding

    private lateinit var rosterAdapterHigh: AppointmentAdapter
    private lateinit var rosterAdapterUrgent: AppointmentAdapter
    private lateinit var rosterAdapterNormal: AppointmentAdapter
    private lateinit var approvalsAdapter: ApprovalAdapter
    private lateinit var tasksAdapter: TaskAdapter
    private lateinit var calendarAdapter: CalendarDayAdapter

    private var isFabMenuOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Month Display
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        binding.tvCurrentMonth.text = monthFormat.format(Date()).uppercase()

        setupRecyclerViews(isAdmin = false)
        setupCalendar()
        setupDrawerNavigation()
        setupSpeedDialFAB()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    authManager.authState.collectLatest { state ->
                        Log.d("DashboardActivity", "authState -> $state")
                        when (state) {
                            is AuthState.LoggedOut -> {
                                startActivity(Intent(this@DashboardActivity, LoginActivity::class.java))
                                finish()
                            }
                            is AuthState.PendingApproval -> {
                                startActivity(Intent(this@DashboardActivity, PendingApprovalActivity::class.java))
                                finish()
                            }
                            is AuthState.Approved -> {
                                binding.userWelcomeSub.text = "Welcome, ${state.name} (${state.role})"
                            }
                            is AuthState.Loading -> Unit
                        }
                    }
                }
                launch {
                    viewModel.userRole.collectLatest { currentRole ->
                        val isAdmin = currentRole == "ADMIN"
                        binding.adminApprovalSection.visibility = if (isAdmin) View.VISIBLE else View.GONE
                        setupRecyclerViews(isAdmin)
                    }
                }
                launch {
                    viewModel.appointments.collectLatest { list ->
                        val approved = list.filter { it.appointment.status == "APPROVED" }
                        val pending = list.filter { it.appointment.status == "PENDING_APPROVAL" }

                        val high = approved.filter { it.appointment.urgencyLevel == "High Importance" }
                        val urgent = approved.filter { it.appointment.urgencyLevel == "Urgent" }
                        val normal = approved.filter { it.appointment.urgencyLevel !in listOf("High Importance", "Urgent") }

                        rosterAdapterHigh.submitList(high)
                        rosterAdapterUrgent.submitList(urgent)
                        rosterAdapterNormal.submitList(normal)
                        binding.sectionHighImportance.visibility = if (high.isEmpty()) View.GONE else View.VISIBLE
                        binding.sectionUrgent.visibility = if (urgent.isEmpty()) View.GONE else View.VISIBLE
                        binding.sectionNormal.visibility = if (normal.isEmpty()) View.GONE else View.VISIBLE

                        approvalsAdapter.submitList(pending)

                        calendarAdapter.submitDays(buildCalendarDays(list))
                    }
                }
                launch {
                    viewModel.tasks.collectLatest { list ->
                        tasksAdapter.submitList(list)
                    }
                }
            }
        }
    }

    private fun setupRecyclerViews(isAdmin: Boolean) {
        rosterAdapterHigh = AppointmentAdapter(isAdmin) { appointment ->
            viewModel.deleteAppointment(appointment)
        }
        binding.rosterHighImportanceRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.rosterHighImportanceRecyclerView.adapter = rosterAdapterHigh

        rosterAdapterUrgent = AppointmentAdapter(isAdmin) { appointment ->
            viewModel.deleteAppointment(appointment)
        }
        binding.rosterUrgentRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.rosterUrgentRecyclerView.adapter = rosterAdapterUrgent

        rosterAdapterNormal = AppointmentAdapter(isAdmin) { appointment ->
            viewModel.deleteAppointment(appointment)
        }
        binding.rosterNormalRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.rosterNormalRecyclerView.adapter = rosterAdapterNormal

        approvalsAdapter = ApprovalAdapter(
            onAcceptClicked = { id -> viewModel.approveAppointment(id) },
            onRejectClicked = { id -> viewModel.rejectAppointment(id) }
        )
        binding.approvalsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.approvalsRecyclerView.adapter = approvalsAdapter

        tasksAdapter = TaskAdapter(
            onCompleteToggled = { id, done -> viewModel.updateTaskCompletion(id, done) },
            onDeleteClicked = { task -> viewModel.deleteTask(task) }
        )
        binding.tasksRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.tasksRecyclerView.adapter = tasksAdapter
    }

    private fun setupDrawerNavigation() {
        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            when (menuItem.itemId) {
                R.id.nav_register_operator -> {
                    startActivity(Intent(this, ManageUsersActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.nav_logout -> {
                    authManager.logout()
                    Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupSpeedDialFAB() {
        binding.fabAdd.setOnClickListener {
            toggleSpeedDialMenu()
        }

        val openAnnouncement = {
            toggleSpeedDialMenu()
            val intent = Intent(this, RosterSchedulerActivity::class.java).apply {
                putExtra("EXTRA_PRE_SELECTED_SERVICE", "Ministers meeting")
                putExtra("EXTRA_PRE_SELECTED_URGENCY", "Normal")
            }
            startActivity(intent)
        }
        binding.btnFabAnnouncement.setOnClickListener { openAnnouncement() }
        binding.fabIconAnnouncement.setOnClickListener { openAnnouncement() }

        val openService = {
            toggleSpeedDialMenu()
            val intent = Intent(this, RosterSchedulerActivity::class.java).apply {
                putExtra("EXTRA_PRE_SELECTED_SERVICE", "Sunday service")
                putExtra("EXTRA_PRE_SELECTED_URGENCY", "High Importance")
            }
            startActivity(intent)
        }
        binding.btnFabService.setOnClickListener { openService() }
        binding.fabIconService.setOnClickListener { openService() }

        val openMeeting = {
            toggleSpeedDialMenu()
            val intent = Intent(this, RosterSchedulerActivity::class.java).apply {
                putExtra("EXTRA_PRE_SELECTED_SERVICE", "Choir meeting")
                putExtra("EXTRA_PRE_SELECTED_URGENCY", "Urgent")
            }
            startActivity(intent)
        }
        binding.btnFabMeeting.setOnClickListener { openMeeting() }
        binding.fabIconMeeting.setOnClickListener { openMeeting() }
    }

    private fun setupCalendar() {
        calendarAdapter = CalendarDayAdapter()
        binding.calendarRecyclerView.layoutManager = GridLayoutManager(this, 7)
        binding.calendarRecyclerView.adapter = calendarAdapter
    }

    private fun buildCalendarDays(appointments: List<AppointmentWithMembers>): List<CalendarDay> {
        val now = Calendar.getInstance()
        val today = now.get(Calendar.DAY_OF_MONTH)
        val currentMonth = now.get(Calendar.MONTH)
        val currentYear = now.get(Calendar.YEAR)

        val firstOfMonth = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
        val firstDayOfWeek = firstOfMonth.get(Calendar.DAY_OF_WEEK) // 1=Sunday..7=Saturday
        val daysInMonth = firstOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH)

        val eventCal = Calendar.getInstance()
        val eventDays = appointments.mapNotNullTo(mutableSetOf()) { item ->
            eventCal.timeInMillis = item.appointment.date
            if (eventCal.get(Calendar.MONTH) == currentMonth && eventCal.get(Calendar.YEAR) == currentYear) {
                eventCal.get(Calendar.DAY_OF_MONTH)
            } else null
        }

        val days = mutableListOf<CalendarDay>()
        repeat(firstDayOfWeek - 1) { days.add(CalendarDay(dayOfMonth = null, hasEvent = false, isToday = false)) }
        for (day in 1..daysInMonth) {
            days.add(CalendarDay(dayOfMonth = day, hasEvent = eventDays.contains(day), isToday = day == today))
        }
        return days
    }

    private fun toggleSpeedDialMenu() {
        isFabMenuOpen = !isFabMenuOpen
        if (isFabMenuOpen) {
            binding.fabSubmenuLayout.visibility = View.VISIBLE
            binding.fabAdd.animate().rotation(45f).setDuration(200).start()
        } else {
            binding.fabSubmenuLayout.visibility = View.GONE
            binding.fabAdd.animate().rotation(0f).setDuration(200).start()
        }
    }

    override fun onResume() {
        super.onResume()
        if (authManager.isDarkModeEnabled()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}
