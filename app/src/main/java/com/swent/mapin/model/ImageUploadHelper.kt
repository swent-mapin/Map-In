package com.swent.mapin.model

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import kotlinx.coroutines.tasks.await

/** Helper class for uploading images to Firebase Storage */
class ImageUploadHelper(private val storage: FirebaseStorage = FirebaseStorage.getInstance()) {

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
      e.printStackTrace()
      null
    }
  }

  /**
   * Delete an image from Firebase Storage
   *
   * @param imageUrl The URL of the image to delete
   */
  suspend fun deleteImage(imageUrl: String) {
    try {
      if (imageUrl.startsWith("http")) {
        val storageRef = storage.getReferenceFromUrl(imageUrl)
        storageRef.delete().await()
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
}
