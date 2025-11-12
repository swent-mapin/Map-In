package com.swent.mapin.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.auth.User
import com.google.firebase.firestore.firestore
import com.swent.mapin.model.UserProfile
import com.swent.mapin.model.UserProfileRepository
import com.swent.mapin.model.message.ConversationRepository
import com.swent.mapin.model.message.ConversationRepositoryFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConversationViewModel(
    private val conversationRepository: ConversationRepository =
        ConversationRepositoryFirestore(db = Firebase.firestore, auth = Firebase.auth),
    private val userProfileRepository: UserProfileRepository = UserProfileRepository(Firebase.firestore)
): ViewModel() {

    init {
        getCurrentUserProfile()
    }

    private val _userConversations = MutableStateFlow<List<Conversation>>(emptyList())
    val userConversations: StateFlow<List<Conversation>> = _userConversations.asStateFlow()

    var currentUserProfile: UserProfile = UserProfile()
    fun getNewUID(): String {
        return conversationRepository.getNewUid()
    }

    fun getCurrentUserProfile() {
        viewModelScope.launch {
            val userId = Firebase.auth.currentUser?.uid
            if(userId != null) {
                val profile = userProfileRepository.getUserProfile(userId)
                if(profile != null){
                    currentUserProfile = profile
                }
            }
        }
    }

    fun observeConversations() {
        viewModelScope.launch {
            conversationRepository.observeConversationsForCurrentUser()
                .collect { conversations ->
                    _userConversations.value = conversations
                }
        }
    }

    fun createConversation(conversation: Conversation) {
        viewModelScope.launch {
            conversationRepository.addConversation(conversation)
        }
    }

}