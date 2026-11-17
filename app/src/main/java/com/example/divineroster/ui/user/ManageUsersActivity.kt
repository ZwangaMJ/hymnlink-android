package com.example.divineroster.ui.user

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.divineroster.data.entity.UserEntity
import com.example.divineroster.data.repository.UserRepository
import com.example.divineroster.databinding.ActivityManageUsersBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ManageUsersActivity : AppCompatActivity() {

    @Inject
    lateinit var userRepository: UserRepository

    private lateinit var binding: ActivityManageUsersBinding
    private lateinit var adapter: UserAdapter
    private lateinit var pendingSignupAdapter: PendingSignupAdapter

    private val roles = listOf("Conductor", "Organist", "Choir member", "Minister", "Leader")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupPendingSignupsRecyclerView()
        setupListeners()
        setupRoleDropdown()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    userRepository.getAllUsers().collectLatest { list ->
                        adapter.submitList(list)
                    }
                }
                launch {
                    userRepository.getPendingSignups().collectLatest { list ->
                        pendingSignupAdapter.submitList(list)
                        binding.tvPendingSignupsHeader.visibility =
                            if (list.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = UserAdapter()
        binding.usersRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.usersRecyclerView.adapter = adapter
    }

    private fun setupPendingSignupsRecyclerView() {
        pendingSignupAdapter = PendingSignupAdapter { user ->
            lifecycleScope.launch {
                userRepository.approveUser(user.id, user.requestedRole)
                Toast.makeText(this@ManageUsersActivity, "${user.name} approved", Toast.LENGTH_SHORT).show()
            }
        }
        binding.pendingSignupsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.pendingSignupsRecyclerView.adapter = pendingSignupAdapter
    }

    private fun setupRoleDropdown() {
        val roleAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, roles)
        binding.actvRole.setAdapter(roleAdapter)
        binding.actvRole.setText(roles[0], false)
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val selectedRole = binding.actvRole.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || selectedRole.isEmpty()) {
                Toast.makeText(this, "Please fill in all registration fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val roleToken = when (selectedRole) {
                "Conductor" -> "CONDUCTOR"
                "Organist" -> "ORGANIST"
                "Choir member" -> "CHOIR_MEMBER"
                "Minister" -> "MINISTER"
                "Leader" -> "LEADER"
                else -> "CONDUCTOR"
            }

            val user = UserEntity(
                name = name,
                role = roleToken,
                email = email,
                phone = phone,
                approvalStatus = "APPROVED",
                hasLoginAccount = false
            )

            lifecycleScope.launch {
                userRepository.insertUser(user)
                Toast.makeText(this@ManageUsersActivity, "$name registered as $selectedRole", Toast.LENGTH_SHORT).show()
                clearFields()
            }
        }
    }

    private fun clearFields() {
        binding.etName.text?.clear()
        binding.etEmail.text?.clear()
        binding.etPhone.text?.clear()
        binding.actvRole.setText(roles[0], false)
    }
}
