package com.swent.mapin.model.message

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

  override fun getNewUid(): String {
    return db.collection("conversations").document().id
  }

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
                    val conversation = doc.toObject(Conversation::class.java)
                    // Fallback ordering: if lastMessageTimestamp is null,
                    // treat as very old (so it goes to the end)
                    conversation
                  } ?: emptyList()

              trySend(conversations)
            }

    awaitClose { listener.remove() }
  }

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
