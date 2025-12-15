package com.swent.mapin.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore
import com.swent.mapin.model.chat.MessageRepository
import com.swent.mapin.model.chat.MessageRepositoryFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Assisted by AI

/**
 * ViewModel for managing chat messages within a conversation.
 *
 * Handles:
 * - Real-time message observation with live updates
 * - Sending new messages
 * - Pagination for loading older messages
 * - Error handling for message operations
 *
 * **Error Propagation**: All operation errors are exposed through the [error] StateFlow for
 * centralized error observation and handling.
 *
 * **Lifecycle**: Active message subscriptions are automatically cancelled when switching
 * conversations ([observeMessages]) or when the ViewModel is cleared ([onCleared]) to prevent
 * memory leaks.
 *
 * @property messageRepository Repository for message data operations
 */
class MessageViewModel(
    private val messageRepository: MessageRepository =
        MessageRepositoryFirestore(db = Firebase.firestore, auth = Firebase.auth)
) : ViewModel() {
  private val _messages = MutableStateFlow<List<Message>>(emptyList())
  /** List of messages in the current conversation, ordered by timestamp */
  val messages: StateFlow<List<Message>> = _messages.asStateFlow()

  private val _error = MutableStateFlow<String?>(null)
  /** Error message from failed operations */
  val error: StateFlow<String?> = _error.asStateFlow()

  private var messagesJob: Job? = null

  /**
   * Start observing messages with live updates and pagination support.
   *
   * Cancels any existing subscription before starting a new one to prevent memory leaks when
   * switching conversations.
   *
   * @param conversationId The ID of the conversation to observe
   */
  fun observeMessages(conversationId: String) {
    messagesJob?.cancel() // Cancel existing subscription when switching conversations
    messagesJob =
        viewModelScope.launch {
          try {
            messageRepository.observeMessagesFlow(conversationId).collect {
                (messagesList, lastSnapshot) ->
              _messages.value = messagesList
              lastVisibleMessage = lastSnapshot
            }
          } catch (e: Exception) {
            _error.value = e.message
          }
        }
  }

  /**
   * Send a new message to the conversation.
   *
   * @param conversationId The ID of the conversation
   * @param text The message text to send
   */
  fun sendMessage(conversationId: String, text: String) {
    viewModelScope.launch {
      try {
        messageRepository.sendMessage(conversationId, text)
      } catch (e: Exception) {
        _error.value = e.message
      }
    }
  }

  private var lastVisibleMessage: DocumentSnapshot? = null
  private var isLoadingMore = false

  /**
   * Loads older messages from Firestore for pagination.
   *
   * Fetches the next batch of messages starting from the last visible message. Prevents concurrent
   * loading operations.
   *
   * @param conversationId The ID of the conversation
   */
  fun loadMoreMessages(conversationId: String) {
    val last = lastVisibleMessage ?: return
    if (isLoadingMore) return
    isLoadingMore = true

    viewModelScope.launch {
      try {
        val (moreMessages, newLastVisible) =
            messageRepository.loadMoreMessages(conversationId, last)
        if (moreMessages.isNotEmpty()) {
          _messages.value = moreMessages + _messages.value
          lastVisibleMessage = newLastVisible
        }
      } catch (e: Exception) {
        _error.value = e.message
      } finally {
        isLoadingMore = false
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    messagesJob?.cancel() // Cleanup: prevent memory leaks when ViewModel is destroyed
  }
}
