package com.example.divineroster.ui.user

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.divineroster.R
import com.example.divineroster.data.entity.UserEntity
import com.example.divineroster.databinding.ItemUserBinding

class UserAdapter : ListAdapter<UserEntity, UserAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: UserEntity) {
            binding.tvUserName.text = item.name
            binding.tvUserContact.text = "${item.email} • ${item.phone}"
            val context = itemView.context
            when (item.role) {
                "CONDUCTOR" -> {
                    binding.tvUserRole.text = "Conductor"
                    binding.tvUserRole.setBackgroundResource(R.drawable.bg_badge_conductor)
                    binding.tvUserRole.setTextColor(ContextCompat.getColor(context, R.color.badge_conductor_txt))
                }
                "CHOIR_MEMBER" -> {
                    binding.tvUserRole.text = "Choir Member"
                    binding.tvUserRole.setBackgroundResource(R.drawable.bg_badge_organist)
                    binding.tvUserRole.setTextColor(ContextCompat.getColor(context, R.color.badge_organist_txt))
                }
                "MINISTER" -> {
                    binding.tvUserRole.text = "Minister"
                    binding.tvUserRole.setBackgroundResource(R.drawable.bg_badge_approved)
                    binding.tvUserRole.setTextColor(ContextCompat.getColor(context, R.color.badge_approved_txt))
                }
                "LEADER" -> {
                    binding.tvUserRole.text = "Leader"
                    binding.tvUserRole.setBackgroundResource(R.drawable.bg_badge_pending)
                    binding.tvUserRole.setTextColor(ContextCompat.getColor(context, R.color.badge_pending_txt))
                }
                else -> {
                    binding.tvUserRole.text = item.role
                    binding.tvUserRole.setBackgroundResource(R.drawable.bg_badge_conductor)
                    binding.tvUserRole.setTextColor(ContextCompat.getColor(context, R.color.badge_conductor_txt))
                }
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<UserEntity>() {
        override fun areItemsTheSame(oldItem: UserEntity, newItem: UserEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: UserEntity, newItem: UserEntity): Boolean {
            return oldItem == newItem
        }
    }
}
