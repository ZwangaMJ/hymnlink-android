package com.example.divineroster.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.divineroster.data.repository.AppointmentWithMembers
import com.example.divineroster.databinding.ItemPendingApprovalBinding
import java.text.SimpleDateFormat
import java.util.*

class ApprovalAdapter(
    private val onAcceptClicked: (String) -> Unit,
    private val onRejectClicked: (String) -> Unit
) : ListAdapter<AppointmentWithMembers, ApprovalAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPendingApprovalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemPendingApprovalBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AppointmentWithMembers) {
            binding.tvServiceType.text = item.appointment.serviceType

            val dateFormat = SimpleDateFormat("EEE, MMM dd, yyyy - HH:mm", Locale.getDefault())
            binding.tvDate.text = dateFormat.format(Date(item.appointment.date))
            binding.tvUrgency.bindUrgencyBadge(item.appointment.urgencyLevel)

            val conductor = item.conductor?.name ?: "TBD"
            val organist = item.organist?.name ?: "TBD"
            binding.tvDetails.text = "Conductor: $conductor • Organist: $organist"

            binding.btnAccept.setOnClickListener {
                onAcceptClicked(item.appointment.id)
            }

            binding.btnReject.setOnClickListener {
                onRejectClicked(item.appointment.id)
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
