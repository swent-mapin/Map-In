package com.swent.mapin.util

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import kotlinx.coroutines.tasks.await

/** Helper class for uploading images to Firebase Storage */
class ImageUploadHelper(private val storage: FirebaseStorage = FirebaseStorage.getInstance()) {

  companion object {
    private const val TAG = "ImageUploadHelper"
  }

  /**
   * Upload an image to Firebase Storage
   *
   * @param imageUri The URI of the image to upload
   * @param userId The user ID (for organizing storage)
   * @param imageType The type of image (avatar or banner)
   * @return The download URL of the uploaded image, or null if upload failed
   */
  suspend fun uploadImage(imageUri: Uri, userId: String, imageType: String): String? {
    return try {
      val fileName = "${imageType}_${UUID.randomUUID()}.jpg"
      val storageRef = storage.reference.child("users").child(userId).child(fileName)

      // Upload the file
      storageRef.putFile(imageUri).await()

      // Get the download URL
      val downloadUrl = storageRef.downloadUrl.await()
      downloadUrl.toString()
    } catch (e: Exception) {
      Log.e(TAG, "Failed to upload image for user $userId", e)
      null
    }
  }

  /**
   * Delete an image from Firebase Storage
   *
   * @param imageUrl The URL of the image to delete
   * @return true if deletion was successful, false otherwise
   */
  suspend fun deleteImage(imageUrl: String): Boolean {
    return try {
      if (imageUrl.startsWith("http")) {
        val storageRef = storage.getReferenceFromUrl(imageUrl)
        storageRef.delete().await()
        true
      } else {
        // Not a valid Firebase Storage URL, consider it a success (nothing to delete)
        true
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to delete image: $imageUrl", e)
      false
    }
  }
}
