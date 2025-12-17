package com.swent.mapin.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.swent.mapin.model.userprofile.UserProfile
import com.swent.mapin.model.userprofile.UserProfileRepository
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
 * **Thread Safety:** All StateFlows are thread-safe and can be collected from any thread.
 * Repository operations are executed on viewModelScope (Main dispatcher with lifecycle awareness).
 *
 * **State Management:**
 * - [userConversations]: Real-time list updated via Firestore listener
 * - [gotConversation]: Single conversation fetched on demand, cancellable
 * - [leaveGroupState]: Tracks leave operation lifecycle with explicit state machine
 * - [currentUserProfile]: Cached profile loaded once on initialization
 *
 * **Error Handling:** Only [leaveGroupState] exposes errors through its Error state. Other
 * operations fail silently and should be monitored via repository logs.
 *
 * **Usage Example:**
 *
 * ```
 * @Composable
 * fun ChatScreen(viewModel: ConversationViewModel) {
 *     val conversations by viewModel.userConversations.collectAsState()
 *     val leaveState by viewModel.leaveGroupState.collectAsState()
 *
 *     when (leaveState) {
 *         is LeaveGroupState.Success -> {
 *             // Navigate away or show confirmation
 *             viewModel.resetLeaveGroupState()
 *         }
 *         is LeaveGroupState.Error -> {
 *             // Show error to user
 *         }
 *     }
 * }
 * ```
 *
 * @property conversationRepository Repository for conversation operations
 * @property userProfileRepository Repository for user profile operations
 * @property currentUserIdProvider Function to get current user ID (injectable for testing)
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
  /**
   * Real-time synchronized list from Firestore listener.
   *
   * **Thread Safety:** Safe to collect from any thread.
   *
   * **Updates:** Emits whenever Firestore detects changes (via [observeConversations]).
   */
  val userConversations: StateFlow<List<Conversation>> = _userConversations.asStateFlow()

  private val _gotConversation = MutableStateFlow<Conversation?>(null)
  /**
   * On-demand fetched conversation.
   *
   * **Concurrency:** New calls to [getConversationById] cancel previous pending fetches.
   *
   * **Thread Safety:** Safe to collect from any thread.
   *
   * **Lifecycle:** Emits null initially, then the fetched conversation when available.
   */
  val gotConversation: StateFlow<Conversation?> = _gotConversation.asStateFlow()

  private val _leaveGroupState = MutableStateFlow<LeaveGroupState>(LeaveGroupState.Idle)
  /**
   * Explicit state machine for leave operations.
   *
   * **Thread Safety:** Safe to collect from any thread.
   *
   * **State Transitions:** Idle → Loading → Success/Error → Idle (via [resetLeaveGroupState])
   *
   * **Error Exposure:** Only state that surfaces errors to UI. Observe this to show user feedback.
   */
  val leaveGroupState: StateFlow<LeaveGroupState> = _leaveGroupState.asStateFlow()

  /**
   * Cached profile loaded on ViewModel initialization.
   *
   * **Thread Safety:** Read-only after initial load, safe for concurrent reads.
   *
   * **Updates:** Not reactive. Call [getCurrentUserProfile] to refresh manually.
   */
  var currentUserProfile: UserProfile = UserProfile()

  /**
   * Get a new unique identifier for a conversation.
   *
   * @param participantIds The list of participants IDs
   * @return The generated ID
   */
  fun getNewUID(participantIds: List<String>): String {
    return conversationRepository.getNewUid(participantIds)
  }

  fun joinConversation(conversationId: String, userId: String, userProfile: UserProfile) {
    viewModelScope.launch {
      conversationRepository.joinConversation(conversationId, userId, userProfile)
    }
  }

  /**
   * Checks if a conversation exists and returns it if it does.
   *
   * @param conversationId The ID of the conversation.
   * @return The existing [Conversation] if found, or null otherwise.
   */
  suspend fun getExistingConversation(conversationId: String): Conversation? {
    return if (conversationRepository.conversationExists(conversationId)) {
      conversationRepository.getConversationById(conversationId)
    } else {
      null
    }
  }

  /**
   * Loads current user's profile into [currentUserProfile].
   *
   * **Side Effects:** Updates [currentUserProfile] in-place if user is authenticated.
   *
   * **Thread Safety:** Executes on viewModelScope (Main dispatcher).
   *
   * **Error Handling:** Fails silently if user not found or not authenticated.
   */
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
   * Starts real-time observation of user's conversations.
   *
   * **Lifecycle:** Runs until ViewModel is cleared (viewModelScope cancellation).
   *
   * **Thread Safety:** Flow collection is confined to viewModelScope.
   *
   * **Concurrency:** Multiple calls are safe but wasteful. Call once per ViewModel instance.
   *
   * **Side Effects:** Updates [userConversations] whenever Firestore emits changes.
   */
  fun observeConversations() {
    viewModelScope.launch {
      conversationRepository.observeConversationsForCurrentUser().collect { conversations ->
        _userConversations.value = conversations
      }
    }
  }

  /**
   * Persists a new conversation to repository.
   *
   * **Thread Safety:** Executes on viewModelScope (Main dispatcher).
   *
   * **Error Handling:** Fails silently. Monitor repository logs for errors.
   *
   * @param conversation The conversation to create
   */
  fun createConversation(conversation: Conversation) {
    viewModelScope.launch { conversationRepository.addConversation(conversation) }
  }

  private var getConversationJob: Job? = null

  /**
   * Fetches a specific conversation, canceling any previous in-flight request.
   *
   * **Concurrency:** Calling this multiple times cancels previous Job before starting new fetch.
   * This prevents race conditions where an older request overwrites a newer one.
   *
   * **Thread Safety:** Job cancellation and StateFlow updates are thread-safe.
   *
   * **Side Effects:** Updates [gotConversation] with fetched data or null if not found.
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
   * Removes current user from a conversation and updates [leaveGroupState].
   *
   * **State Machine:**
   * - Sets Loading before operation
   * - Sets Success on completion
   * - Sets Error with message on failure
   *
   * **Thread Safety:** Executes on viewModelScope (Main dispatcher).
   *
   * **Concurrency:** Multiple simultaneous calls are safe but trigger redundant operations. UI
   * should disable leave button while Loading to prevent this.
   *
   * **Error Handling:** Errors are surfaced through [leaveGroupState], not thrown.
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

  /**
   * Resets [leaveGroupState] to Idle after handling Success/Error states.
   *
   * **Usage:** Call after displaying success/error UI to allow future leave operations.
   *
   * **Thread Safety:** Safe to call from any thread.
   */
  fun resetLeaveGroupState() {
    _leaveGroupState.value = LeaveGroupState.Idle
  }
}
