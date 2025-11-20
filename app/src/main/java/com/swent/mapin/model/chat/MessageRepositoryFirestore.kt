package com.swent.mapin.model.chat

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.swent.mapin.ui.chat.Message
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

// Assisted by AI

/**
 * Firestore implementation of [MessageRepository].
 *
 * @param db Firestore instance to use.
 * @param auth The Firestore authorization.
 */
class MessageRepositoryFirestore(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : MessageRepository {

  private val PAGE_SIZE = 50

  /**
   * Retrieves the last PAGE_SIZE messages from a specific conversation from the repository with
   * live updates
   *
   * @param conversationId The ID of the conversation
   */
  override fun observeMessagesFlow(
      conversationId: String,
  ): Flow<Pair<List<Message>, DocumentSnapshot?>> = callbackFlow {
    val query =
        db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE.toLong())

    val listener =
        query.addSnapshotListener { snapshot, error ->
          if (error != null) {
            close(error)
            return@addSnapshotListener
          }

          val documents = snapshot?.documents ?: emptyList()

          val messages =
              documents
                  .mapNotNull { doc ->
                    val senderId = doc.getString("senderId")
                    val timestamp = doc.getLong("timestamp")

                    // Validate required fields
                    if (senderId == null || timestamp == null) {
                      Log.w("Firestore", "Invalid message doc: ${doc.id}")
                      return@mapNotNull null
                    }

                    Message(
                        text = doc.getString("text") ?: "",
                        senderId = senderId,
                        isMe = senderId == auth.currentUser?.uid,
                        timestamp = timestamp)
                  }
                  .reversed()

          val lastVisible = documents.lastOrNull()
          trySend(messages to lastVisible)
        }

    awaitClose { listener.remove() }
  }

  /**
   * Sends a message in the conversation and stores it in the repository
   *
   * @param conversationId The ID of the conversation
   * @param text The message that will be sent
   */
  override suspend fun sendMessage(conversationId: String, text: String) {
    if (text.isBlank()) return

    val currentUser = auth.currentUser ?: return
    val dbRef = db.collection("conversations").document(conversationId)
    val messagesRef = dbRef.collection("messages")

    val messageData =
        mapOf(
            "senderId" to currentUser.uid,
            "text" to text,
            "timestamp" to System.currentTimeMillis())

    db.runBatch { batch ->
          val newMessageRef = messagesRef.document()
          batch.set(newMessageRef, messageData)
          batch.update(
              dbRef,
              mapOf("lastMessage" to text, "lastMessageTimestamp" to System.currentTimeMillis()))
        }
        .await()
  }
  /**
   * Load more messages when the user Scrolls up
   *
   * @param conversationId The ID of the conversation
   * @param lastVisible The last visible snapshot in the conversation
   */
  override suspend fun loadMoreMessages(
      conversationId: String,
      lastVisible: DocumentSnapshot
  ): Pair<List<Message>, DocumentSnapshot?> {
    val snapshot =
        db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .startAfter(lastVisible)
            .limit(PAGE_SIZE.toLong())
            .get()
            .await()

    val messages =
        snapshot.documents
            .mapNotNull { doc ->
              val senderId = doc.getString("senderId")
              val timestamp = doc.getLong("timestamp")

              if (senderId == null || timestamp == null) {
                Log.w("Firestore", "Invalid message doc: ${doc.id}")
                return@mapNotNull null
              }

              Message(
                  text = doc.getString("text") ?: "",
                  senderId = senderId,
                  isMe = senderId == auth.currentUser?.uid,
                  timestamp = timestamp)
            }
            .reversed()

    // Get the last document in this page to use for the next pagination
    val newLastVisible = snapshot.documents.lastOrNull()

    return messages to newLastVisible
  }
}
