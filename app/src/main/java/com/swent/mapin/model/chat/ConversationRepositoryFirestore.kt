package com.swent.mapin.model.chat

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.swent.mapin.ui.chat.Conversation
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
  /** Generates a new unique identifier for a conversation. */
  override fun getNewUid(): String {
    return db.collection("conversations").document().id
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
                    val conversation = doc.toObject(Conversation::class.java) ?: return@mapNotNull null

                    // If it's a 1-to-1 chat, override the name
                    if (conversation.participants.size == 2) {
                        val otherParticipant =
                        conversation.participants.firstOrNull { it.userId != uid }

                    if (otherParticipant != null) {
                        return@mapNotNull conversation.copy(name = otherParticipant.name, profilePictureUrl = otherParticipant.profilePictureUrl)
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

    conversationRef.set(conversationToSave).await()
  }
}
