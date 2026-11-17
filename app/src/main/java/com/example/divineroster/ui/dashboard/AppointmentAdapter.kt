package com.example.divineroster.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.divineroster.data.entity.AppointmentEntity
import com.example.divineroster.data.repository.AppointmentWithMembers
import com.example.divineroster.databinding.ItemAppointmentBinding
import com.example.divineroster.util.ShareUtil
import java.text.SimpleDateFormat
import java.util.*

class AppointmentAdapter(
    private val isAdmin: Boolean,
    private val onDeleteClicked: (AppointmentEntity) -> Unit
) : ListAdapter<AppointmentWithMembers, AppointmentAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppointmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemAppointmentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AppointmentWithMembers) {
            binding.tvServiceType.text = item.appointment.serviceType

            val dateFormat = SimpleDateFormat("EEE, MMM dd, yyyy - HH:mm", Locale.getDefault())
            binding.tvDate.text = dateFormat.format(Date(item.appointment.date))
            binding.tvUrgency.bindUrgencyBadge(item.appointment.urgencyLevel)

            binding.tvConductorName.text = item.conductor?.name ?: "TBD"
            binding.tvOrganistName.text = item.organist?.name ?: "TBD"

            if (item.appointment.notes.isEmpty()) {
                binding.tvNotes.visibility = View.GONE
            } else {
                binding.tvNotes.visibility = View.VISIBLE
                binding.tvNotes.text = "Notes: ${item.appointment.notes}"
            }

            binding.tvStatus.text = item.appointment.status

            // Delete button visibility depending on Admin role
            binding.btnDelete.visibility = if (isAdmin) View.VISIBLE else View.GONE
            binding.btnDelete.setOnClickListener {
                onDeleteClicked(item.appointment)
            }

            binding.btnShareWhatsApp.setOnClickListener {
                ShareUtil.shareToWhatsApp(itemView.context, item)
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<AppointmentWithMembers>() {
        override fun areItemsTheSame(oldItem: AppointmentWithMembers, newItem: AppointmentWithMembers): Boolean {
            return oldItem.appointment.id == newItem.appointment.id
        }

        override fun areContentsTheSame(oldItem: AppointmentWithMembers, newItem: AppointmentWithMembers): Boolean {
            return oldItem == newItem
        }
    }
}
