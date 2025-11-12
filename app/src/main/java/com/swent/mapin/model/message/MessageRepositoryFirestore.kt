package com.swent.mapin.model.message

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.swent.mapin.ui.chat.Conversation
import com.swent.mapin.ui.chat.Message
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
//Assisted by AI

/**
 * Firestore implementation of [MessageRepository].
 *
 * @param db Firestore instance to use.
 * @param auth The Firestore authorization.
 */
class MessageRepositoryFirestore(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
): MessageRepository {

    private val PAGE_SIZE = 50

    /**
     * Retrieves the last PAGE_SIZE messages from a specific conversation from the repository with live updates
     * @param conversationId The ID of the conversation
     */
    override fun observeMessagesFlow(
        conversationId: String,
    ): Flow<Pair<List<Message>, DocumentSnapshot?>> = callbackFlow {
        val query = db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE.toLong())

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val documents = snapshot?.documents ?: emptyList()

            val messages = documents.map { doc ->
                Message(
                    text = doc.getString("text") ?: "",
                    isMe = doc.getString("senderId") == auth.currentUser?.uid
                )
            }.reversed() // chronological order

            val lastVisible = documents.lastOrNull()
            trySend(messages to lastVisible)
        }

        awaitClose { listener.remove() }
    }

    /**
     * Sends a message in the conversation and stores it in the repository
     * @param conversationId The ID of the conversation
     * @param text The message that will be sent
     */
    override suspend fun sendMessage(conversationId: String, text: String) {
        if (text.isBlank()) return
        val messageData = mapOf(
            "senderId" to auth.currentUser?.uid,
            "text" to text,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .add(messageData)
            .await()

        db.collection("conversations")
            .document(conversationId)
            .update("lastMessage", text)
            .await()
    }

    override suspend fun getConversationsForCurrentUser(): List<Conversation> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        val snapshot = db.collection("conversations")
            .whereArrayContains("participantIds", uid)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.toObject(Conversation::class.java) }
    }

    override suspend fun loadMoreMessages(
        conversationId: String,
        lastVisible: DocumentSnapshot
    ): Pair<List<Message>, DocumentSnapshot?> {
        val snapshot = db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .startAfter(lastVisible)
            .limit(PAGE_SIZE.toLong())
            .get()
            .await()

        val messages = snapshot.documents.map { doc ->
            Message(
                text = doc.getString("text") ?: "",
                isMe = doc.getString("senderId") == auth.currentUser?.uid
            )
        }.reversed()

        // Get the last document in this page to use for the next pagination
        val newLastVisible = snapshot.documents.lastOrNull()

        return messages to newLastVisible
    }

}