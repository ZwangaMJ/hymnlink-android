package com.example.divineroster.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.divineroster.data.entity.AnnouncementEntity
import com.example.divineroster.databinding.ItemAnnouncementBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AnnouncementAdapter(
    isAdmin: Boolean,
    private val onDeleteClicked: (AnnouncementEntity) -> Unit
) : ListAdapter<AnnouncementEntity, AnnouncementAdapter.ViewHolder>(DiffCallback) {

    var isAdmin: Boolean = isAdmin
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAnnouncementBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemAnnouncementBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AnnouncementEntity) {
            binding.tvImportance.bindUrgencyBadge(item.importance)
            binding.tvAnnouncementText.text = item.text

            val posted = SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).format(Date(item.createdAt))
            binding.tvAnnouncementMeta.text =
                if (item.createdByName.isBlank()) posted else "Posted by ${item.createdByName} - $posted"

            binding.btnDeleteAnnouncement.visibility = if (isAdmin) View.VISIBLE else View.GONE
            binding.btnDeleteAnnouncement.setOnClickListener { onDeleteClicked(item) }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<AnnouncementEntity>() {
        override fun areItemsTheSame(oldItem: AnnouncementEntity, newItem: AnnouncementEntity) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: AnnouncementEntity, newItem: AnnouncementEntity) =
            oldItem == newItem
    }
}
