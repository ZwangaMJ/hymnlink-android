package com.example.divineroster.ui.music

import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.divineroster.R
import com.example.divineroster.data.entity.MusicItemEntity
import com.example.divineroster.databinding.ItemMusicBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MusicAdapter(
    isAdmin: Boolean,
    private val onDownloadClicked: (MusicItemEntity) -> Unit,
    private val onDeleteClicked: (MusicItemEntity) -> Unit
) : ListAdapter<MusicItemEntity, MusicAdapter.ViewHolder>(DiffCallback) {

    /** Admins get the delete button; changing it refreshes the visible rows. */
    var isAdmin: Boolean = isAdmin
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMusicBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemMusicBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MusicItemEntity) {
            binding.tvMusicTitle.text = item.title
            binding.ivMusicType.setImageResource(
                if (item.category == MusicItemEntity.CATEGORY_AUDIO) R.drawable.ic_audio_file else R.drawable.ic_sheet_file
            )

            val size = Formatter.formatShortFileSize(itemView.context, item.sizeBytes)
            val added = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(item.createdAt))
            val extension = item.fileName.substringAfterLast('.', "").uppercase(Locale.getDefault())
            binding.tvMusicMeta.text = listOf(extension, size, added).filter { it.isNotBlank() }.joinToString(" - ")

            binding.btnDownload.setOnClickListener { onDownloadClicked(item) }
            binding.btnDeleteMusic.visibility = if (isAdmin) View.VISIBLE else View.GONE
            binding.btnDeleteMusic.setOnClickListener { onDeleteClicked(item) }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<MusicItemEntity>() {
        override fun areItemsTheSame(oldItem: MusicItemEntity, newItem: MusicItemEntity) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: MusicItemEntity, newItem: MusicItemEntity) = oldItem == newItem
    }
}
