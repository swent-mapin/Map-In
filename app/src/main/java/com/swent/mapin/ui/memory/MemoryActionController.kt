package com.swent.mapin.ui.memory

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import com.swent.mapin.model.memory.Memory
import com.swent.mapin.model.memory.MemoryRepository
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Handles the memory creation flow (uploading media, creating the memory entity, error reporting)
 * so MapScreenViewModel only coordinates UI state.
 */
class MemoryActionController(
    private val applicationContext: Context,
    private val memoryRepository: MemoryRepository,
    private val auth: FirebaseAuth,
    private val scope: CoroutineScope,
    private val onHideMemoryForm: () -> Unit,
    private val onRestoreSheetState: () -> Unit,
    private val setErrorMessage: (String) -> Unit,
    private val clearErrorMessage: () -> Unit
) {

  private var _isSavingMemory by mutableStateOf(false)
  val isSavingMemory: Boolean
    get() = _isSavingMemory

  fun saveMemory(formData: MemoryFormData) {
    scope.launch {
      _isSavingMemory = true
      clearErrorMessage()
      try {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
          setErrorMessage("You must be signed in to create a memory")
          Log.e(TAG, "Cannot save memory: User not authenticated")
          return@launch
        }

        val mediaUrls = uploadMediaFiles(applicationContext, formData.mediaUris, currentUserId)
        val memory =
            Memory(
                uid = memoryRepository.getNewUid(),
                title = formData.title,
                description = formData.description,
                eventId = formData.eventId,
                ownerId = currentUserId,
                isPublic = formData.isPublic,
                createdAt = Timestamp.now(),
                mediaUrls = mediaUrls,
                taggedUserIds = formData.taggedUserIds)
        memoryRepository.addMemory(memory)
        Log.i(TAG, "Memory saved successfully")
        onHideMemoryForm()
        onRestoreSheetState()
      } catch (e: Exception) {
        setErrorMessage("Failed to save memory: ${e.message ?: "Unknown error"}")
        Log.e(TAG, "Error saving memory", e)
      } finally {
        _isSavingMemory = false
      }
    }
  }

    private suspend fun uploadMediaFiles(context: Context, uris: List<Uri>, userId: String): List<String> {
        if (uris.isEmpty()) return emptyList()
        val downloadUrls = mutableListOf<String>()
        val storageRef = FirebaseStorage.getInstance().reference

        for (uri in uris) {
            try {
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val isVideo = mimeType.startsWith("video/")
                val folder = if (isVideo) "videos" else "images"
                val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: if (isVideo) "mp4" else "jpg"

                val fileRef = storageRef.child("memories/$userId/$folder/${UUID.randomUUID()}_${System.currentTimeMillis()}.$extension")
                val metadata = storageMetadata { contentType = mimeType }
                fileRef.putFile(uri, metadata).await()
                downloadUrls.add(fileRef.downloadUrl.await().toString())
                Log.i(TAG, "Uploaded media file successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload media file", e)
            }
        }

        return downloadUrls
    }

    companion object {
    private const val TAG = "MemoryActionController"
  }
}
