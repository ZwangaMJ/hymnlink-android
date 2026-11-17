package com.example.divineroster.ui.user

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.divineroster.data.entity.UserEntity
import com.example.divineroster.databinding.ItemPendingSignupBinding

class PendingSignupAdapter(
    private val onApproveClicked: (UserEntity) -> Unit
) : ListAdapter<UserEntity, PendingSignupAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPendingSignupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemPendingSignupBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: UserEntity) {
            binding.tvSignupName.text = item.name
            binding.tvSignupContact.text = "${item.email} • ${item.phone}"
            binding.tvSignupRequestedRole.text = item.requestedRole.replace("_", " ")

            binding.btnApproveSignup.setOnClickListener {
                onApproveClicked(item)
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
