package com.swent.mapin.model.chat

import com.swent.mapin.ui.chat.Conversation
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {

  /**
   * Generates and returns a new unique identifier for a Conversation item.
   *
   * @param participantIds The list of participant ids
   * @return A unique string identifier.
   */
  fun getNewUid(participantIds: List<String>): String

  /**
   * Checks whether a given conversationId exists in the database
   *
   * @param conversationId The ID of the conversation
   * @return True if the conversation already exists, False if not
   */
  suspend fun conversationExists(conversationId: String): Boolean

  /** Observes all conversations of the current logged in user */
  fun observeConversationsForCurrentUser(): Flow<List<Conversation>>

  /** Adds a conversation to the repository */
  suspend fun addConversation(conversation: Conversation)

  /**
   * Returns the conversation with the given ID, or null if not found.
   *
   * @param conversationId the conversation’s unique identifier.
   */
  suspend fun getConversationById(conversationId: String): Conversation?

  /**
   * Removes the current user from a conversation.
   *
   * @param conversationId the conversation's unique identifier.
   */
  suspend fun leaveConversation(conversationId: String)
}
