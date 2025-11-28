package com.swent.mapin.ui.profile

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.swent.mapin.model.FriendRequestRepository
import com.swent.mapin.model.ImageUploadHelper
import com.swent.mapin.model.UserProfile
import com.swent.mapin.model.UserProfileRepository
import com.swent.mapin.model.badge.BadgeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing user profile state and operations.
 *
 * Responsibilities:
 * - Load user profile data from Firestore
 * - Handle profile editing mode
 * - Save profile changes to Firestore
 * - Manage temporary edit state
 * - Validate user inputs
 * - Upload images to Firebase Storage
 * - Calculate and update user badges
 */
class ProfileViewModel(
    private val repository: UserProfileRepository = UserProfileRepository(),
    private val imageUploadHelper: ImageUploadHelper = ImageUploadHelper(),
    private val friendRequestRepository: FriendRequestRepository? = null
) : ViewModel() {

  // Current user profile from Firestore
  private val _userProfile = MutableStateFlow(UserProfile())
  val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

  // Loading state
  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  // Whether the profile is in edit mode
  var isEditMode by mutableStateOf(false)
    private set

  // Whether the avatar selector dialog is shown
  var showAvatarSelector by mutableStateOf(false)
    private set

  // Whether the banner selector dialog is shown
  var showBannerSelector by mutableStateOf(false)
    private set

  // Whether the delete confirmation dialog is shown
  var showDeleteConfirmation by mutableStateOf(false)
    private set

  // Whether uploading an image
  var isUploadingImage by mutableStateOf(false)
    private set

  // Temporary fields for editing (before saving)
  var editName by mutableStateOf("")
    private set

  var editBio by mutableStateOf("")
    private set

  var editLocation by mutableStateOf("")
    private set

  var editHobbies by mutableStateOf("")
    private set

  var selectedAvatar by mutableStateOf("")
    private set

  var selectedBanner by mutableStateOf("")
    private set

  // Add a counter to force recomposition
  private var _avatarUpdateCounter by mutableStateOf(0)
  private var _bannerUpdateCounter by mutableStateOf(0)

  // Visibility toggles
  var hobbiesVisible by mutableStateOf(true)
    private set

  // Validation error messages
  var nameError by mutableStateOf<String?>(null)
    private set

  var bioError by mutableStateOf<String?>(null)
    private set

  var locationError by mutableStateOf<String?>(null)
    private set

  var hobbiesError by mutableStateOf<String?>(null)
    private set

  init {
    loadUserProfile()
  }

  /** Load user profile from Firestore. If profile doesn't exist, creates a default one. */
  fun loadUserProfile() {
    viewModelScope.launch {
      _isLoading.value = true
      val currentUser = FirebaseAuth.getInstance().currentUser

      if (currentUser != null) {
        try {
          // Try to load existing profile from Firestore
          val existingProfile = repository.getUserProfile(currentUser.uid)

          if (existingProfile != null) {
            // Profile exists in Firestore, use it
            // Calculate badges on the client side (not stored in Firestore)
            // Only calculate badges if friendRequestRepository is available (not in tests)
            if (friendRequestRepository != null) {
              val friendsCount = getFriendsCount()
              val updatedBadges = BadgeManager.calculateBadges(existingProfile, friendsCount)
              val profileWithBadges = existingProfile.copy(badges = updatedBadges)
              _userProfile.value = profileWithBadges
            } else {
              // In tests, just use the profile as-is
              _userProfile.value = existingProfile
            }
            println(
                "ProfileViewModel - Profile loaded successfully: name='${existingProfile.name}', bio='${existingProfile.bio}', location='${existingProfile.location}'")
          } else {
            // No profile in Firestore - this should ONLY happen on first launch
            println(
                "ProfileViewModel - WARNING: No profile found in Firestore, creating default one")
            val defaultProfile =
                repository.createDefaultProfile(
                    userId = currentUser.uid,
                    name = currentUser.displayName ?: "Anonymous User",
                    profilePictureUrl = currentUser.photoUrl?.toString())
            _userProfile.value = defaultProfile
          }
        } catch (e: Exception) {
          println("ProfileViewModel - ERROR loading profile: ${e.message}")
          e.printStackTrace()
          // On error, try to create a default profile
          val defaultProfile =
              repository.createDefaultProfile(
                  userId = currentUser.uid,
                  name = currentUser.displayName ?: "Anonymous User",
                  profilePictureUrl = currentUser.photoUrl?.toString())
          _userProfile.value = defaultProfile
        }
      }
      _isLoading.value = false
    }
  }

  /** Enable edit mode and populate edit fields with current values */
  fun startEditing() {
    isEditMode = true
    val profile = _userProfile.value
    editName = profile.name

    // Clear default placeholder values when editing
    editBio = if (profile.bio == "Tell us about yourself...") "" else profile.bio
    editLocation = if (profile.location == "Unknown") "" else profile.location

    editHobbies = profile.hobbies.joinToString(", ")
    selectedAvatar = profile.avatarUrl ?: ""
    selectedBanner = profile.bannerUrl ?: ""
    hobbiesVisible = profile.hobbiesVisible
    clearErrors()
  }

  /** Cancel editing and discard changes */
  fun cancelEditing() {
    isEditMode = false
    editName = ""
    editBio = ""
    editLocation = ""
    editHobbies = ""
    selectedAvatar = ""
    selectedBanner = ""
    clearErrors()
  }

  /** Save profile changes to Firestore and exit edit mode */
  fun saveProfile() {
    // Validate all fields before saving
    if (!validateAll()) {
      println("ProfileViewModel - Validation failed")
      // Don't exit edit mode if validation fails
      return
    }

    viewModelScope.launch {
      _isLoading.value = true

      val hobbiesList = editHobbies.split(",").map { it.trim() }.filter { it.isNotEmpty() }

      val updatedProfile =
          _userProfile.value.copy(
              name = editName,
              bio = editBio,
              location = editLocation,
              hobbies = hobbiesList,
              avatarUrl = selectedAvatar.ifEmpty { null },
              bannerUrl = selectedBanner.ifEmpty { null },
              hobbiesVisible = hobbiesVisible)

      // Log pour debug
      println("ProfileViewModel - Saving profile: $updatedProfile")
      println("ProfileViewModel - Avatar URL: ${updatedProfile.avatarUrl}")
      println("ProfileViewModel - Banner URL: ${updatedProfile.bannerUrl}")

      // Calculate and update badges before saving
      val friendsCount = getFriendsCount()
      val updatedBadges = BadgeManager.calculateBadges(updatedProfile, friendsCount)
      val profileWithBadges = updatedProfile.copy(badges = updatedBadges)

      // Save to Firestore
      val success = repository.saveUserProfile(profileWithBadges)
      println("ProfileViewModel - Save success: $success")

      // Update local state based on success/failure
      if (success) {
        _userProfile.value = profileWithBadges
        println("ProfileViewModel - Profile updated successfully")
      } else {
        println("ProfileViewModel - Failed to save profile")
      }

      isEditMode = false
      clearErrors()

      _isLoading.value = false
    }
  }

  /** Update name field during editing */
  fun updateEditName(newName: String) {
    editName = newName
    validateName()
  }

  /** Update bio field during editing */
  fun updateEditBio(newBio: String) {
    editBio = newBio
    validateBio()
  }

  /** Update location field during editing */
  fun updateEditLocation(newLocation: String) {
    editLocation = newLocation
    validateLocation()
  }

  /** Update hobbies field during editing */
  fun updateEditHobbies(newHobbies: String) {
    editHobbies = newHobbies
    validateHobbies()
  }

  /** Validate name field */
  private fun validateName() {
    nameError =
        when {
          editName.isEmpty() -> "Name cannot be empty"
          editName.length < 2 -> "Name must be at least 2 characters"
          editName.length > 50 -> "Name must be less than 50 characters"
          else -> null
        }
  }

  /** Validate bio field */
  private fun validateBio() {
    bioError =
        when {
          editBio.length > 500 -> "Bio must be less than 500 characters"
          else -> null
        }
  }

  /** Validate location field */
  private fun validateLocation() {
    locationError =
        when {
          editLocation.isEmpty() -> "Location cannot be empty"
          editLocation.length > 100 -> "Location must be less than 100 characters"
          else -> null
        }
  }

  /** Validate hobbies field */
  private fun validateHobbies() {
    hobbiesError =
        when {
          editHobbies.length > 200 -> "Hobbies must be less than 200 characters"
          else -> null
        }
  }

  /** Validate all fields */
  private fun validateAll(): Boolean {
    validateName()
    validateBio()
    validateLocation()
    validateHobbies()

    return nameError == null && bioError == null && locationError == null && hobbiesError == null
  }

  /** Clear all error messages */
  private fun clearErrors() {
    nameError = null
    bioError = null
    locationError = null
    hobbiesError = null
  }

  /** Update avatar selection */
  fun updateAvatarSelection(avatarUrl: String) {
    selectedAvatar = avatarUrl
    _avatarUpdateCounter++
  }

  /** Update banner selection */
  fun updateBannerSelection(bannerUrl: String) {
    selectedBanner = bannerUrl
    _bannerUpdateCounter++
  }

  /** Toggle avatar selector dialog */
  fun toggleAvatarSelector() {
    showAvatarSelector = !showAvatarSelector
  }

  /** Toggle banner selector dialog */
  fun toggleBannerSelector() {
    showBannerSelector = !showBannerSelector
  }

  /** Toggle hobbies visibility */
  fun toggleHobbiesVisibility() {
    hobbiesVisible = !hobbiesVisible
  }

  /**
   * Signs out the current user from Firebase Auth and resets the profile state.
   *
   * This method performs the following actions:
   * - Signs out the user from Firebase Authentication.
   * - Resets the user profile to a default, empty state.
   * - Exits edit mode.
   * - Clears any validation or input errors.
   *
   * Call this method when the user chooses to sign out, to ensure all profile-related state in the
   * ViewModel is cleared and ready for a new session.
   */
  fun signOut() {
    FirebaseAuth.getInstance().signOut()
    // Reset the profile state
    _userProfile.value = UserProfile()
    isEditMode = false
    clearErrors()
  }

  /** Show delete confirmation dialog */
  fun showDeleteDialog() {
    showDeleteConfirmation = true
  }

  /** Hide delete confirmation dialog */
  fun hideDeleteDialog() {
    showDeleteConfirmation = false
  }

  /** Delete/reset profile to default values */
  fun deleteProfile() {
    viewModelScope.launch {
      _isLoading.value = true

      val currentUser = FirebaseAuth.getInstance().currentUser

      if (currentUser != null) {
        // Delete old avatar and banner images from Firebase Storage if they exist
        val currentProfile = _userProfile.value

        // Delete avatar image if it's a Firebase Storage URL
        if (!currentProfile.avatarUrl.isNullOrEmpty() &&
            currentProfile.avatarUrl!!.contains("firebasestorage")) {
          val avatarDeleted = imageUploadHelper.deleteImage(currentProfile.avatarUrl!!)
          if (!avatarDeleted) {
            println("ProfileViewModel - Failed to delete avatar image")
          }
        }

        // Delete banner image if it's a Firebase Storage URL
        if (!currentProfile.bannerUrl.isNullOrEmpty() &&
            currentProfile.bannerUrl!!.contains("firebasestorage")) {
          val bannerDeleted = imageUploadHelper.deleteImage(currentProfile.bannerUrl!!)
          if (!bannerDeleted) {
            println("ProfileViewModel - Failed to delete banner image")
          }
        }

        // Create a new default profile
        val defaultProfile =
            repository.createDefaultProfile(
                userId = currentUser.uid,
                name = currentUser.displayName ?: "Anonymous User",
                profilePictureUrl = currentUser.photoUrl?.toString())

        // Save the default profile to Firestore (this will overwrite the existing profile)
        val success = repository.saveUserProfile(defaultProfile)

        if (success) {
          // Update local state
          _userProfile.value = defaultProfile
          println("ProfileViewModel - Profile reset to default successfully")
          // Ensure we exit edit mode and clear any temporary selections so the UI
          // reflects the reset profile immediately (no need to navigate away).
          cancelEditing()
        } else {
          println("ProfileViewModel - Failed to reset profile")
        }
      }

      showDeleteConfirmation = false
      _isLoading.value = false
    }
  }

  /** Upload avatar image from gallery */
  fun uploadAvatarImage(uri: Uri) {
    viewModelScope.launch {
      isUploadingImage = true
      val currentUser = FirebaseAuth.getInstance().currentUser

      if (currentUser != null) {
        val downloadUrl = imageUploadHelper.uploadImage(uri, currentUser.uid, "avatar")
        if (downloadUrl != null) {
          // Force recomposition by reassigning the entire value
          selectedAvatar = ""
          selectedAvatar = downloadUrl
          println("ProfileViewModel - Avatar uploaded: $downloadUrl")
        } else {
          println("ProfileViewModel - Failed to upload avatar")
        }
      }

      isUploadingImage = false
    }
  }

  /** Upload banner image from gallery */
  fun uploadBannerImage(uri: Uri) {
    viewModelScope.launch {
      isUploadingImage = true
      val currentUser = FirebaseAuth.getInstance().currentUser

      if (currentUser != null) {
        val downloadUrl = imageUploadHelper.uploadImage(uri, currentUser.uid, "banner")
        if (downloadUrl != null) {
          // Force recomposition by reassigning the entire value
          selectedBanner = ""
          selectedBanner = downloadUrl
          println("ProfileViewModel - Banner uploaded: $downloadUrl")
        } else {
          println("ProfileViewModel - Failed to upload banner")
        }
      }

      isUploadingImage = false
    }
  }

  /**
   * Updates the user's badges based on current profile and friends count. This method recalculates
   * all badges and saves them to Firestore immediately. This ensures badges are always up-to-date,
   * even when navigating between screens.
   */
  fun updateBadges() {
    viewModelScope.launch {
      val currentUser = FirebaseAuth.getInstance().currentUser
      if (currentUser != null) {
        val friendsCount = getFriendsCount()
        val currentProfile = _userProfile.value

        // Calculate badges
        val updatedBadges = BadgeManager.calculateBadges(currentProfile, friendsCount)

        // Update profile with new badges
        val updatedProfile = currentProfile.copy(badges = updatedBadges)

        // Save to Firestore immediately to persist badges 24/7
        val success = repository.saveUserProfile(updatedProfile)

        if (success) {
          _userProfile.value = updatedProfile
          println("ProfileViewModel - Badges updated and saved successfully")
        } else {
          println("ProfileViewModel - Failed to save updated badges")
        }
      }
    }
  }

  /**
   * Updates the user's badges locally without saving to Firestore. Used during profile loading to
   * avoid overwriting existing data.
   */
  private fun updateBadgesLocalOnly() {
    viewModelScope.launch {
      val currentUser = FirebaseAuth.getInstance().currentUser
      if (currentUser != null) {
        val friendsCount = getFriendsCount()
        val currentProfile = _userProfile.value

        // Calculate badges
        val updatedBadges = BadgeManager.calculateBadges(currentProfile, friendsCount)

        // Update profile with new badges (local state only, no Firestore save)
        _userProfile.value = currentProfile.copy(badges = updatedBadges)

        println("ProfileViewModel - Badges updated locally (no Firestore save)")
      }
    }
  }

  /**
   * Gets the current user's friends count.
   *
   * @return The number of friends the user has.
   */
  private suspend fun getFriendsCount(): Int {
    val currentUser = FirebaseAuth.getInstance().currentUser
    return if (currentUser != null) {
      try {
        val friends = friendRequestRepository?.getFriends(currentUser.uid)
        friends?.size ?: 0
      } catch (e: Exception) {
        println("ProfileViewModel - Error getting friends count: ${e.message}")
        0
      }
    } else {
      0
    }
  }
}
