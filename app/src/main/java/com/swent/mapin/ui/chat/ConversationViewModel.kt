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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

  var currentUserProfile: UserProfile = UserProfile()
  /** Get a new unique identifier for a conversation. */
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

  fun getConversationById(conversationId: String) {
    viewModelScope.launch { _gotConversation.value = conversationRepository.getConversationById(conversationId) }
  }
}
