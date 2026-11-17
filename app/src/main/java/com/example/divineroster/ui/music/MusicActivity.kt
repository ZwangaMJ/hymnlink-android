package com.example.divineroster.ui.music

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.divineroster.R
import com.example.divineroster.data.auth.AuthManager
import com.example.divineroster.data.auth.AuthState
import com.example.divineroster.data.entity.MusicItemEntity
import com.example.divineroster.data.repository.MusicRepository
import com.example.divineroster.databinding.ActivityMusicBinding
import com.example.divineroster.databinding.DialogMusicUploadBinding
import com.example.divineroster.ui.common.BottomNav
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MusicActivity : AppCompatActivity() {

    private companion object {
        const val TAG = "MusicActivity"
        const val MAX_UPLOAD_BYTES = 50L * 1024 * 1024
    }

    @Inject
    lateinit var musicRepository: MusicRepository

    @Inject
    lateinit var authManager: AuthManager

    private lateinit var binding: ActivityMusicBinding
    private lateinit var adapter: MusicAdapter

    private var allItems = listOf<MusicItemEntity>()
    private var isUploading = false
    private var pendingDownload: MusicItemEntity? = null

    private val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) promptForTitleAndUpload(uri)
    }

    // Only needed on Android 9 and older, where saving into the public Downloads folder needs this permission.
    private val storagePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val item = pendingDownload
        pendingDownload = null
        if (granted && item != null) {
            enqueueDownload(item)
        } else {
            Toast.makeText(this, "Storage permission is needed to save the file", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMusicBinding.inflate(layoutInflater)
        setContentView(binding.root)

        BottomNav.setup(this, binding.bottomNavigation, R.id.nav_music)

        adapter = MusicAdapter(
            isAdmin = false,
            onDownloadClicked = { item -> download(item) },
            onDeleteClicked = { item -> confirmDelete(item) }
        )
        binding.musicRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.musicRecyclerView.adapter = adapter

        binding.rgMusicCategory.setOnCheckedChangeListener { _, _ -> showSelectedCategory() }
        binding.fabUpload.setOnClickListener {
            if (isUploading) {
                Toast.makeText(this, "An upload is already in progress", Toast.LENGTH_SHORT).show()
            } else {
                pickFile.launch(arrayOf("application/pdf", "image/*", "audio/*"))
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    authManager.authState.collect { state ->
                        val isAdmin = (state as? AuthState.Approved)?.role == "ADMIN"
                        adapter.isAdmin = isAdmin
                        binding.fabUpload.visibility = if (isAdmin) View.VISIBLE else View.GONE
                        binding.tvMusicSubtitle.text =
                            if (isAdmin) "Upload sheet music and audio for members to download."
                            else "Sheet music and audio to download."
                    }
                }
                launch {
                    musicRepository.getAllMusic()
                        .catch { error ->
                            Log.e(TAG, "Music listener failed", error)
                            Toast.makeText(this@MusicActivity, "Could not load music: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                            emit(emptyList())
                        }
                        .collect { items ->
                        allItems = items
                        showSelectedCategory()
                    }
                }
            }
        }
    }

    private fun showSelectedCategory() {
        val category =
            if (binding.rgMusicCategory.checkedRadioButtonId == R.id.rbAudio) MusicItemEntity.CATEGORY_AUDIO
            else MusicItemEntity.CATEGORY_SHEET
        val visible = allItems.filter { it.category == category }
        adapter.submitList(visible)

        binding.tvMusicEmpty.visibility = if (visible.isEmpty()) View.VISIBLE else View.GONE
        binding.tvMusicEmpty.text =
            if (category == MusicItemEntity.CATEGORY_AUDIO) "No audio files have been uploaded yet."
            else "No sheet music has been uploaded yet."
    }

    // ---- Download ---------------------------------------------------------------------------

    private fun download(item: MusicItemEntity) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingDownload = item
            storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }
        enqueueDownload(item)
    }

    private fun enqueueDownload(item: MusicItemEntity) {
        if (item.downloadUrl.isBlank()) {
            Toast.makeText(this, "This file has no download link", Toast.LENGTH_SHORT).show()
            return
        }
        val safeName = item.fileName.ifBlank { item.title }.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val request = DownloadManager.Request(Uri.parse(item.downloadUrl))
            .setTitle(item.title)
            .setDescription("HymnLink")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "HymnLink/$safeName")
        if (item.mimeType.isNotBlank()) request.setMimeType(item.mimeType)

        try {
            (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
            Toast.makeText(this, "Downloading to Downloads/HymnLink", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Download failed to start", e)
            Toast.makeText(this, "Could not start the download", Toast.LENGTH_SHORT).show()
        }
    }

    // ---- Upload (admins) --------------------------------------------------------------------

    private class PickedFile(val name: String, val size: Long, val mimeType: String)

    private fun describe(uri: Uri): PickedFile {
        var name = "file"
        var size = 0L
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
            }
        }
        return PickedFile(name, size, contentResolver.getType(uri) ?: "application/octet-stream")
    }

    private fun promptForTitleAndUpload(uri: Uri) {
        val file = describe(uri)
        if (file.size > MAX_UPLOAD_BYTES) {
            Toast.makeText(this, "That file is larger than 50 MB", Toast.LENGTH_LONG).show()
            return
        }

        val dialogBinding = DialogMusicUploadBinding.inflate(LayoutInflater.from(this))
        dialogBinding.tvUploadFileInfo.text = file.name
        dialogBinding.etMusicTitle.setText(file.name.substringBeforeLast('.'))

        val dialog = AlertDialog.Builder(this)
            .setTitle("Upload")
            .setView(dialogBinding.root)
            .setPositiveButton("Upload", null)
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val title = dialogBinding.etMusicTitle.text.toString().trim()
            if (title.isEmpty()) {
                dialogBinding.etMusicTitle.error = "Give it a title"
                return@setOnClickListener
            }
            dialog.dismiss()
            upload(uri, file, title)
        }
    }

    private fun upload(uri: Uri, file: PickedFile, title: String) {
        val author = authManager.authState.value as? AuthState.Approved ?: return
        val category =
            if (file.mimeType.startsWith("audio/")) MusicItemEntity.CATEGORY_AUDIO else MusicItemEntity.CATEGORY_SHEET

        setUploading(true)
        lifecycleScope.launch {
            try {
                musicRepository.upload(
                    uri = uri,
                    fileName = file.name.replace(Regex("[\\\\/:*?\"<>|]"), "_"),
                    mimeType = file.mimeType,
                    sizeBytes = file.size,
                    title = title,
                    category = category,
                    uploadedByUid = author.uid,
                    uploadedByName = author.name,
                    onProgress = { percent -> binding.uploadProgress.progress = percent }
                )
                // Show the tab the new file landed in.
                binding.rgMusicCategory.check(if (category == MusicItemEntity.CATEGORY_AUDIO) R.id.rbAudio else R.id.rbSheet)
                Toast.makeText(this@MusicActivity, "Uploaded", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Upload failed", e)
                Toast.makeText(this@MusicActivity, "Upload failed: ${e.localizedMessage ?: "unknown error"}", Toast.LENGTH_LONG).show()
            } finally {
                setUploading(false)
            }
        }
    }

    private fun setUploading(uploading: Boolean) {
        isUploading = uploading
        binding.uploadProgress.progress = 0
        binding.uploadProgress.visibility = if (uploading) View.VISIBLE else View.GONE
        binding.fabUpload.isEnabled = !uploading
    }

    private fun confirmDelete(item: MusicItemEntity) {
        AlertDialog.Builder(this)
            .setTitle("Delete \"${item.title}\"?")
            .setMessage("Members will no longer be able to download it.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        musicRepository.delete(item)
                    } catch (e: Exception) {
                        Log.e(TAG, "Delete failed", e)
                        Toast.makeText(this@MusicActivity, "Could not delete: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        BottomNav.markSelected(binding.bottomNavigation, R.id.nav_music)
    }
}
