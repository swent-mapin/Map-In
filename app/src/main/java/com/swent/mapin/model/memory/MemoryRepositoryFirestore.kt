package com.swent.mapin.model.memory

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Assisted by AI
const val MEMORIES_COLLECTION_PATH = "memories"

/** Firestore implementation of MemoryRepository. Stores Memory items in a "memories" collection. */
class MemoryRepositoryFirestore(private val db: FirebaseFirestore) : MemoryRepository {

  override fun getNewUid(): String = db.collection(MEMORIES_COLLECTION_PATH).document().id

  override suspend fun getAllMemories(): List<Memory> {
    val snap = db.collection(MEMORIES_COLLECTION_PATH).orderBy("createdAt").get().await()
    return snap.documents.mapNotNull { documentToMemory(it) }
  }

  override suspend fun getMemory(memoryId: String): Memory {
    val doc = db.collection(MEMORIES_COLLECTION_PATH).document(memoryId).get().await()
    return documentToMemory(doc)
        ?: throw NoSuchElementException(
            "MemoryRepositoryFirestore: Memory not found (id=$memoryId)")
  }

  override suspend fun getMemoriesByEvent(eventId: String): List<Memory> {
    val snap =
        db.collection(MEMORIES_COLLECTION_PATH)
            .whereEqualTo("eventId", eventId)
            .orderBy("createdAt")
            .get()
            .await()
    return snap.documents.mapNotNull { documentToMemory(it) }
  }

  override suspend fun getPublicMemoriesByEvent(eventId: String): List<Memory> {
    val snap =
        db.collection(MEMORIES_COLLECTION_PATH)
            .whereEqualTo("eventId", eventId)
            .whereEqualTo("isPublic", true)
            .orderBy("createdAt")
            .get()
            .await()
    return snap.documents.mapNotNull { documentToMemory(it) }
  }

  override suspend fun getMemoriesByOwner(ownerId: String): List<Memory> {
    val snap =
        db.collection(MEMORIES_COLLECTION_PATH).whereEqualTo("ownerId", ownerId).get().await()
    return snap.documents.mapNotNull { documentToMemory(it) }
  }

  override suspend fun getMemoriesByTimeRange(
      startTime: Timestamp,
      endTime: Timestamp
  ): List<Memory> {
    val snap =
        db.collection(MEMORIES_COLLECTION_PATH)
            .whereGreaterThanOrEqualTo("createdAt", startTime)
            .whereLessThan("createdAt", endTime)
            .orderBy("createdAt")
            .get()
            .await()
    return snap.documents.mapNotNull { documentToMemory(it) }
  }

  override suspend fun getMemoriesByTaggedUser(userId: String): List<Memory> {
    val snap =
        db.collection(MEMORIES_COLLECTION_PATH)
            .whereArrayContains("taggedUserIds", userId)
            .orderBy("createdAt")
            .get()
            .await()
    return snap.documents.mapNotNull { documentToMemory(it) }
  }

  override suspend fun addMemory(memory: Memory) {
    val id = memory.uid.ifBlank { getNewUid() }
    db.collection(MEMORIES_COLLECTION_PATH).document(id).set(memory.copy(uid = id)).await()
  }

  override suspend fun editMemory(memoryId: String, newValue: Memory) {
    db.collection(MEMORIES_COLLECTION_PATH)
        .document(memoryId)
        .set(newValue.copy(uid = memoryId))
        .await()
  }

  override suspend fun deleteMemory(memoryId: String) {
    db.collection(MEMORIES_COLLECTION_PATH).document(memoryId).delete().await()
  }

  private fun documentToMemory(document: DocumentSnapshot): Memory? =
      try {
        document.toObject(Memory::class.java)?.copy(uid = document.id)
      } catch (e: Exception) {
        Log.e(
            "MemoryRepositoryFirestore",
            "Error converting document to Memory (id=${document.id})",
            e)
        null
      }
}
