package com.swent.mapin.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore
import com.swent.mapin.model.message.MessageRepository
import com.swent.mapin.model.message.MessageRepositoryFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
//Assisted by AI

class MessageViewModel(
    private val messageRepository: MessageRepository =
        MessageRepositoryFirestore(db = Firebase.firestore, auth = Firebase.auth)
): ViewModel() {
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var messagesJob: Job? = null

    /**
     * Start observing messages with live updates and pagination support.
     * @param conversationId The ID of the Conversation
     */
    fun observeMessages(conversationId: String) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            try {
                messageRepository.observeMessagesFlow(conversationId)
                    .collect { (messagesList, lastSnapshot) ->
                        _messages.value = messagesList
                        lastVisibleMessage = lastSnapshot
                    }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    /**
     * Send a new message.
     * @param conversationId The ID of the conversation
     * @param text The message to be sent
     */

    fun sendMessage(conversationId: String, text: String){
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
     * Loads older messages from firestore starting from the last message
     * @param conversationId the ID of the conversation
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
        messagesJob?.cancel()
    }
}