package com.swent.mapin.model.message

import com.swent.mapin.ui.chat.Conversation
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {

    /**
     * Generates and returns a new unique identifier for a Conversation item.
     *
     * @return A unique string identifier.
     */
    fun getNewUid(): String

    /**
     * Observes all conversations of the current logged in user
     */
    fun observeConversationsForCurrentUser(): Flow<List<Conversation>>

    /**
     * Adds a conversation to the repository
     */
    suspend fun addConversation(conversation: Conversation)
}