package com.swent.mapin.model.message

import com.google.firebase.firestore.DocumentSnapshot
import com.swent.mapin.ui.chat.Conversation
import com.swent.mapin.ui.chat.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    /**
     * Retrieves messages from a specific conversation from the repository with live updates
     * @param conversationId The ID of the conversation
     */
    fun observeMessagesFlow(conversationId: String): Flow<Pair<List<Message>, DocumentSnapshot?>>

    /**
     * Sends a message in the conversation and stores it in the repository
     * @param conversationId The ID of the conversation
     * @param text The message that will be sent
     */
    suspend fun sendMessage(conversationId: String, text: String)

    /**
     * Retrieves all conversations of the current logged in user
     */
    suspend fun getConversationsForCurrentUser(): List<Conversation>

    /**
     * Load more messages when the user Scrolls up
     * @param conversationId The ID of the conversation
     * @param lastVisible The last visible snapshot in the conversation
     */
    suspend fun loadMoreMessages(conversationId: String, lastVisible: DocumentSnapshot): Pair<List<Message>, DocumentSnapshot?>

}