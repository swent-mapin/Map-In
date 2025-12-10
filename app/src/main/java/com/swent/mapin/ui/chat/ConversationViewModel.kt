package com.swent.mapin.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.swent.mapin.model.UserProfile
import com.swent.mapin.model.UserProfileRepository
import com.swent.mapin.model.chat.ConversationRepository
import com.swent.mapin.model.chat.ConversationRepositoryFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Sealed class representing the state of leaving a group conversation. */
sealed class LeaveGroupState {
  /** No operation in progress */
  object Idle : LeaveGroupState()

  /** Leave operation is in progress */
  object Loading : LeaveGroupState()

  /** Leave operation completed successfully */
  object Success : LeaveGroupState()

  /**
   * Leave operation failed.
   *
   * @property message Error message describing the failure
   */
  data class Error(val message: String) : LeaveGroupState()
}

/**
 * ViewModel for managing conversations and chat-related operations.
 *
 * Handles:
 * - Observing user's conversations with real-time updates
 * - Creating new conversations
 * - Fetching conversation details
 * - Leaving group conversations
 * - Managing current user profile state
 *
 * @property conversationRepository Repository for conversation operations
 * @property userProfileRepository Repository for user profile operations
 * @property currentUserIdProvider Function to get current user ID
 */
class ConversationViewModel(
    private val conversationRepository: ConversationRepository =
        ConversationRepositoryFirestore(db = Firebase.firestore, auth = Firebase.auth),
    private val userProfileRepository: UserProfileRepository =
        UserProfileRepository(Firebase.firestore),
    private val currentUserIdProvider: () -> String? = { Firebase.auth.currentUser?.uid }
) : ViewModel() {

  init {
    getCurrentUserProfile()
  }

  private val _userConversations = MutableStateFlow<List<Conversation>>(emptyList())
  /** List of conversations the current user is part of */
  val userConversations: StateFlow<List<Conversation>> = _userConversations.asStateFlow()

  private val _gotConversation = MutableStateFlow<Conversation?>(null)
  /** Currently fetched conversation details */
  val gotConversation: StateFlow<Conversation?> = _gotConversation.asStateFlow()

  private val _leaveGroupState = MutableStateFlow<LeaveGroupState>(LeaveGroupState.Idle)
  /** State of the leave group operation */
  val leaveGroupState: StateFlow<LeaveGroupState> = _leaveGroupState.asStateFlow()

  /** Current user's profile information */
  var currentUserProfile: UserProfile = UserProfile()

  /**
   * Generates a new unique identifier for a conversation.
   *
   * @return A unique conversation ID string
   */
  fun getNewUID(): String {
    return conversationRepository.getNewUid()
  }

  /** Fetches the current user's profile from the repository. */
  fun getCurrentUserProfile() {
    viewModelScope.launch {
      val userId = currentUserIdProvider()
      if (userId != null) {
        val profile = userProfileRepository.getUserProfile(userId)
        if (profile != null) {
          currentUserProfile = profile
        }
      }
    }
  }

  /**
   * Observes conversations for the current user with live updates. Subscribes to real-time changes
   * from Firestore.
   */
  fun observeConversations() {
    viewModelScope.launch {
      conversationRepository.observeConversationsForCurrentUser().collect { conversations ->
        _userConversations.value = conversations
      }
    }
  }

  /**
   * Creates a new conversation in the repository.
   *
   * @param conversation The conversation to create
   */
  fun createConversation(conversation: Conversation) {
    viewModelScope.launch { conversationRepository.addConversation(conversation) }
  }

  private var getConversationJob: Job? = null

  /**
   * Fetches a specific conversation by its ID.
   *
   * @param conversationId The conversation's unique identifier
   */
  fun getConversationById(conversationId: String) {
    getConversationJob?.cancel()

    viewModelScope.launch {
      _gotConversation.value = conversationRepository.getConversationById(conversationId)
    }
  }

  /**
   * Removes the current user from a conversation.
   *
   * @param conversationId The conversation's unique identifier
   */
  fun leaveConversation(conversationId: String) {
    viewModelScope.launch {
      try {
        _leaveGroupState.value = LeaveGroupState.Loading
        conversationRepository.leaveConversation(conversationId)
        _leaveGroupState.value = LeaveGroupState.Success
      } catch (e: Exception) {
        _leaveGroupState.value = LeaveGroupState.Error(e.message ?: "Failed to leave conversation")
      }
    }
  }

  /** Resets the leave group state to Idle. */
  fun resetLeaveGroupState() {
    _leaveGroupState.value = LeaveGroupState.Idle
  }
}
