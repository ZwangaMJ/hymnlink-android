package com.example.divineroster.data.repository

import android.net.Uri
import com.example.divineroster.data.entity.MusicItemEntity
import com.example.divineroster.data.firestore.snapshotFlow
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface MusicRepository {
    fun getAllMusic(): Flow<List<MusicItemEntity>>

    /** Uploads the file to Firebase Storage, then publishes its metadata so members can see and download it. */
    suspend fun upload(
        uri: Uri,
        fileName: String,
        mimeType: String,
        sizeBytes: Long,
        title: String,
        category: String,
        uploadedByUid: String,
        uploadedByName: String,
        onProgress: (percent: Int) -> Unit
    ): String

    suspend fun delete(item: MusicItemEntity)
}

@Singleton
class MusicRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : MusicRepository {

    private val collection get() = firestore.collection("music")

    override fun getAllMusic(): Flow<List<MusicItemEntity>> =
        collection.snapshotFlow().map { snapshot ->
            snapshot.toObjects(MusicItemEntity::class.java).sortedByDescending { it.createdAt }
        }

    override suspend fun upload(
        uri: Uri,
        fileName: String,
        mimeType: String,
        sizeBytes: Long,
        title: String,
        category: String,
        uploadedByUid: String,
        uploadedByName: String,
        onProgress: (percent: Int) -> Unit
    ): String {
        val docRef = collection.document()
        val storagePath = "music/${docRef.id}/$fileName"
        val fileRef = storage.reference.child(storagePath)

        val metadata = StorageMetadata.Builder().setContentType(mimeType).build()
        val uploadTask = fileRef.putFile(uri, metadata)
        uploadTask.addOnProgressListener { snap ->
            if (snap.totalByteCount > 0) {
                onProgress((100 * snap.bytesTransferred / snap.totalByteCount).toInt())
            }
        }
        uploadTask.await()
        val downloadUrl = fileRef.downloadUrl.await().toString()

        val item = MusicItemEntity(
            id = docRef.id,
            title = title,
            category = category,
            fileName = fileName,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            storagePath = storagePath,
            downloadUrl = downloadUrl,
            uploadedByUid = uploadedByUid,
            uploadedByName = uploadedByName,
            createdAt = System.currentTimeMillis()
        )
        try {
            docRef.set(item).await()
        } catch (e: Exception) {
            // Don't leave an orphaned file behind if publishing the metadata failed.
            runCatching { fileRef.delete().await() }
            throw e
        }
        return docRef.id
    }

    override suspend fun delete(item: MusicItemEntity) {
        if (item.storagePath.isNotBlank()) {
            // The file may already be gone; the listing entry must still be removable.
            runCatching { storage.reference.child(item.storagePath).delete().await() }
        }
        collection.document(item.id).delete().await()
    }
}
