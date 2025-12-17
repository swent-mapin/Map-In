package com.swent.mapin.model.chat

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.swent.mapin.model.UserProfile
import com.swent.mapin.ui.chat.Conversation
import com.swent.mapin.util.HashUtils.hashUserIds
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firestore implementation of [ConversationRepository].
 *
 * @param db Firestore instance to use.
 * @param auth The Firestore authorization.
 */
class ConversationRepositoryFirestore(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ConversationRepository {
  /**
   * Generates a new unique identifier for a conversation. Returns a new random ID if the list given
   * is empty, or a Hashed ID if the list isn't empty
   *
   * @param participantIds The list of participant IDs
   */
  override fun getNewUid(participantIds: List<String>): String {
    if (participantIds.isEmpty()) {
      return db.collection("conversations").document().id
    }
    return hashUserIds(participantIds)
  }
  /**
   * Observes conversations for the current user with live updates.
   *
   * @return A Flow emitting lists of [Conversation] objects.
   */
  override fun observeConversationsForCurrentUser(): Flow<List<Conversation>> = callbackFlow {
    val uid =
        auth.currentUser?.uid
            ?: run {
              close()
              return@callbackFlow
            }

    val listener =
        db.collection("conversations")
            .whereArrayContains("participantIds", uid)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
              if (error != null) {
                close(error)
                return@addSnapshotListener
              }

              val conversations =
                  snapshot?.documents?.mapNotNull { doc ->
                    val conversation =
                        doc.toObject(Conversation::class.java) ?: return@mapNotNull null

                    // If it's a 1-to-1 chat, override the name
                    if (conversation.participants.size == 2) {
                      val otherParticipant =
                          conversation.participants.firstOrNull { it.userId != uid }

                      if (otherParticipant != null) {
                        return@mapNotNull conversation.copy(
                            name = otherParticipant.name,
                            profilePictureUrl = otherParticipant.profilePictureUrl)
                      }
                    }
                    conversation
                  } ?: emptyList()
              trySend(conversations)
            }

    awaitClose { listener.remove() }
  }
  /**
   * Adds a new conversation to Firestore.
   *
   * @param conversation The [Conversation] object to add.
   */
  override suspend fun addConversation(conversation: Conversation) {
    val conversationRef = db.collection("conversations").document(conversation.id)

    // Ensure the conversation includes the creator’s UID if not already added
    val currentUid = auth.currentUser?.uid
    val updatedParticipantIds =
        if (currentUid != null && !conversation.participantIds.contains(currentUid)) {
          conversation.participantIds + currentUid
        } else {
          conversation.participantIds
        }

    val conversationToSave =
        conversation.copy(
            participantIds = updatedParticipantIds,
        )

    db.runTransaction { tx ->
        val snapshot = tx.get(conversationRef)
        if (!snapshot.exists()) {
            tx.set(conversationRef, conversationToSave)
        }
    }.await()
  }

  override suspend fun joinConversation(conversationId: String, userId: String, userProfile: UserProfile) {
      db.collection("conversations").document(conversationId).update(
            mapOf(
                "participantIds" to FieldValue.arrayUnion(userId),
                "participants" to FieldValue.arrayUnion(userProfile)
            )
        )
  }

  /**
   * Checks whether a conversation with the given ID exists in Firestore.
   *
   * This function performs a single read on the "conversations" collection and returns whether a
   * document with the specified [conversationId] exists.
   *
   * @param conversationId The ID of the conversation to check.
   * @return `true` if the conversation exists in Firestore, `false` otherwise.
   */
  override suspend fun conversationExists(conversationId: String): Boolean {
    return try {
      val docSnapshot = db.collection("conversations").document(conversationId).get().await()

      docSnapshot.exists()
    } catch (e: Exception) {
      Log.e("ConversationRepo", "Failed to check if conversation exists", e)
      false
    }
  }

  /**
   * Retrieves a single conversation by its ID.
   *
   * @param conversationId The ID of the conversation to fetch.
   * @return The [Conversation] object if found, or null otherwise.
   */
  override suspend fun getConversationById(conversationId: String): Conversation? {
    return try {
      val docSnapshot = db.collection("conversations").document(conversationId).get().await()
      docSnapshot.toObject(Conversation::class.java)
    } catch (e: Exception) {
      Log.e("ConversationRepo", "Failed to get conversation by id", e)
      null
    }
  }
  // This was written with the help of Claude Sonnet 4.5

  /**
   * Removes the current user from a conversation.
   *
   * @param conversationId The ID of the conversation to leave.
   */
  override suspend fun leaveConversation(conversationId: String) {
    val currentUid = auth.currentUser?.uid ?: return

    try {
      val conversationRef = db.collection("conversations").document(conversationId)

      db.runTransaction { transaction ->
            val docSnapshot = transaction.get(conversationRef)
            val conversation =
                docSnapshot.toObject(Conversation::class.java) ?: return@runTransaction

            // Remove the current user from participantIds
            val updatedParticipantIds = conversation.participantIds.filter { it != currentUid }

            // Remove the current user from participants
            val updatedParticipants = conversation.participants.filter { it.userId != currentUid }

            // Update the conversation atomically
            transaction.update(
                conversationRef,
                mapOf(
                    "participantIds" to updatedParticipantIds,
                    "participants" to updatedParticipants))
          }
          .await()
    } catch (e: Exception) {
      Log.e("ConversationRepo", "Failed to leave conversation", e)
      throw e
    }
  }
}
