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

sealed class LeaveGroupState {
  object Idle : LeaveGroupState()

  object Loading : LeaveGroupState()

  object Success : LeaveGroupState()

  data class Error(val message: String) : LeaveGroupState()
}

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
  val userConversations: StateFlow<List<Conversation>> = _userConversations.asStateFlow()

  private val _gotConversation = MutableStateFlow<Conversation?>(null)
  val gotConversation: StateFlow<Conversation?> = _gotConversation.asStateFlow()

  private val _leaveGroupState = MutableStateFlow<LeaveGroupState>(LeaveGroupState.Idle)
  val leaveGroupState: StateFlow<LeaveGroupState> = _leaveGroupState.asStateFlow()

  var currentUserProfile: UserProfile = UserProfile()

  /** Get a new unique identifier for a conversation.
   * @param participantIds The list of participants IDs
   * @return The generated ID
   */
  fun getNewUID(participantIds: List<String>): String {
    return conversationRepository.getNewUid(participantIds)
  }
  private val _convoExists = MutableStateFlow<Boolean>(false)
  val convoExists: StateFlow<Boolean> = _convoExists.asStateFlow()

  fun conversationExists(conversationId: String){
    viewModelScope.launch {
      _convoExists.value = conversationRepository.conversationExists(conversationId)
    }
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

  /** Observes conversations for the current user with live updates. */
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
   * @param conversation The conversation to be created
   */
  fun createConversation(conversation: Conversation) {
    viewModelScope.launch { conversationRepository.addConversation(conversation) }
  }

  private var getConversationJob: Job? = null

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
