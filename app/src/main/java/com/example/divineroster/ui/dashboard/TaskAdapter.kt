package com.example.divineroster.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.divineroster.data.entity.TaskEntity
import com.example.divineroster.databinding.ItemTaskBinding
import java.text.SimpleDateFormat
import java.util.*

class TaskAdapter(
    private val onCompleteToggled: (String, Boolean) -> Unit,
    private val onDeleteClicked: (TaskEntity) -> Unit
) : ListAdapter<TaskEntity, TaskAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TaskEntity) {
            binding.tvTaskTitle.text = item.title
            binding.tvTaskDesc.text = item.description

            val dateFormat = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault())
            binding.tvTaskDueDate.text = "Due: ${dateFormat.format(Date(item.dueDate))}"

            // Prevent infinite loop by clearing listener first
            binding.cbTask.setOnCheckedChangeListener(null)
            binding.cbTask.isChecked = item.isCompleted

            binding.cbTask.setOnCheckedChangeListener { _, isChecked ->
                onCompleteToggled(item.id, isChecked)
            }

            binding.btnDeleteTask.setOnClickListener {
                onDeleteClicked(item)
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<TaskEntity>() {
        override fun areItemsTheSame(oldItem: TaskEntity, newItem: TaskEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TaskEntity, newItem: TaskEntity): Boolean {
            return oldItem == newItem
        }
    }
}
