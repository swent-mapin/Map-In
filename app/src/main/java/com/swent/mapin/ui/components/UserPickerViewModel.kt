package com.swent.mapin.ui.components

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.swent.mapin.model.FriendRequestRepository
import com.swent.mapin.model.FriendWithProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the user picker dialog.
 *
 * @param friendRepository Repository for friend requests
 */
class UserPickerViewModel(
    private val friendRepository: FriendRequestRepository,
    private val firebaseAuth: FirebaseAuth = Firebase.auth
) : ViewModel() {

  private val _friends = MutableStateFlow<List<FriendWithProfile>>(emptyList())
  val friends = _friends.asStateFlow()

  init {
    viewModelScope.launch {
      try {
        val userId = firebaseAuth.currentUser!!.uid
        _friends.value = friendRepository.getFriends(userId)
      } catch (e: Exception) {
        Log.e("UserPickerViewModel", "Error fetching friends", e)
      }
    }
  }
}

class UserPickerVMFactory(private val repo: FriendRequestRepository) : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    return UserPickerViewModel(repo) as T
  }
}
