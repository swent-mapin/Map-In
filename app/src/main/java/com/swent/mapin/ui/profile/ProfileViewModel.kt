package com.swent.mapin.ui.profile

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.swent.mapin.model.FriendRequestRepository
import com.swent.mapin.model.ImageUploadHelper
import com.swent.mapin.model.NotificationService
import com.swent.mapin.model.UserProfile
import com.swent.mapin.model.UserProfileRepository
import com.swent.mapin.model.badge.Badge
import com.swent.mapin.model.badge.BadgeContext
import com.swent.mapin.model.badge.BadgeManager
import com.swent.mapin.model.badge.BadgeRepository
import com.swent.mapin.model.badge.BadgeRepositoryFirestore
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
 * - Calculate and persist user badges
 * - Track badge unlock notifications
 */
class ProfileViewModel(
    private val repository: UserProfileRepository = UserProfileRepository(),
    private val imageUploadHelper: ImageUploadHelper = ImageUploadHelper(),
    badgeRepo: BadgeRepository? = null,
    friendRepo: FriendRequestRepository? = null,
) : ViewModel() {
  private var badgeRepository: BadgeRepository? = badgeRepo
  private var friendRequestRepository: FriendRequestRepository? = friendRepo

  // Current user profile from Firestore
  private val _userProfile = MutableStateFlow(UserProfile())
  val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

  // Loading state
  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  // Badges state
  private val _badges = MutableStateFlow<List<Badge>>(emptyList())
  val badges: StateFlow<List<Badge>> = _badges.asStateFlow()

  private val _badgeContext = MutableStateFlow<BadgeContext>(BadgeContext())
  val badgeContext: StateFlow<BadgeContext> = _badgeContext.asStateFlow()

  // Whether the profile is in edit mode
  var isEditMode by mutableStateOf(false)
    private set

  // Whether the avatar selector dialog is shown
  var showAvatarSelector by mutableStateOf(false)
    private set

  // Whether the banner selector dialog is shown
  var showBannerSelector by mutableStateOf(false)
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
    try {
      // Only initialize repositories if they weren't injected via constructor
      if (badgeRepository == null) {
        badgeRepository = BadgeRepositoryFirestore()
      }
      if (friendRequestRepository == null) {
        friendRequestRepository =
            FriendRequestRepository(notificationService = NotificationService())
      }
    } catch (e: Exception) {
      Log.e(
          "BadgeRepoInitialization",
          "ProfileViewModel - Failed initializing repositories: ${e.message}")
    }

    try {
      loadUserProfile()
      fetchBadgeContext()
    } catch (e: Exception) {
      // Gracefully handle initialization errors (e.g., Firebase not initialized in tests)
      Log.e("BadgeRepoInitialization", "ProfileViewModel - Init error: ${e.message}")
      _isLoading.value = false
    }
  }

  /** Load user profile from Firestore. If profile doesn't exist, creates a default one. */
  fun loadUserProfile() {
    viewModelScope.launch {
      _isLoading.value = true
      val currentUser = FirebaseAuth.getInstance().currentUser

      if (currentUser != null) {
        // Try to load existing profile from Firestore
        val existingProfile = repository.getUserProfile(currentUser.uid)

        if (existingProfile != null) {
          // Profile exists in Firestore, use it
          _userProfile.value = existingProfile
        } else {
          // No profile in Firestore, create default one
          val defaultProfile =
              repository.createDefaultProfile(
                  userId = currentUser.uid,
                  name = currentUser.displayName ?: "Anonymous User",
                  profilePictureUrl = currentUser.photoUrl?.toString())
          _userProfile.value = defaultProfile
        }
      }
      // When currentUser is null, just keep the default empty UserProfile()
      // This allows tests to run without authentication
      _isLoading.value = false
    }
  }

  /** Fetch badge context from Firestore and update StateFlow */
  private fun fetchBadgeContext() {
    viewModelScope.launch {
      val currentUser = FirebaseAuth.getInstance().currentUser ?: return@launch

      // Only proceed if badgeRepository is initialized
      val repo =
          badgeRepository
              ?: run {
                Log.e(
                    "badgeRepoInitialization",
                    "ProfileViewModel - badgeRepository not initialized, skipping badge fetch")
                return@launch
              }

      // Clear cache to ensure we get fresh data from Firestore
      // Badge context may have been modified by other components (e.g., FriendRequestRepository)
      repo.clearCache(currentUser.uid)

      var context =
          try {
            repo.getBadgeContext(currentUser.uid)
          } catch (e: Exception) {
            Log.e(
                "badgeFetchFail",
                "ProfileViewModel - Failed to fetch BadgeContext, using default: ${e.message}")
            BadgeContext()
          }

      // Sync friend count with actual friends list to handle legacy data
      // or cases where the count got out of sync
      try {
        val actualFriends = friendRequestRepository?.getFriends(currentUser.uid) ?: emptyList()
        val actualFriendCount = actualFriends.size
        if (context.friendsCount != actualFriendCount) {
          Log.d(
              "BadgeContextSync",
              "Syncing friendsCount: stored=${context.friendsCount}, actual=$actualFriendCount")
          context = context.copy(friendsCount = actualFriendCount)
          // Save the corrected context to Firestore
          repo.saveBadgeContext(currentUser.uid, context)
        }
      } catch (e: Exception) {
        Log.e("BadgeContextSync", "Failed to sync friend count: ${e.message}")
      }

      // Update safely
      _badgeContext.value = context

      try {
        calculateAndUpdateBadges()
      } catch (e: Exception) {
        Log.e(
            "badgeCalculationFail",
            "ProfileViewModel - Failed recalculating badges after context fetch: ${e.message}")
      }
    }
  }

  /**
   * Calculate badges based on current profile data and persist to Firestore.
   *
   * This method:
   * 1. Fetches the friend count from FriendRequestRepository
   * 2. Calculates all badges using BadgeManager
   * 3. Updates the userProfile with the new badges
   * 4. Saves updated profile (with badges) to Firestore
   */
  private suspend fun calculateAndUpdateBadges() {
    // Skip badge calculation if dependencies are not available (e.g., in tests)
    if (badgeRepository == null || friendRequestRepository == null) {
      Log.e(
          "BadgeCalculationSkip",
          "ProfileViewModel - Skipping badge calculation (dependencies not available)")
      return
    }

    val currentUser = FirebaseAuth.getInstance().currentUser ?: return
    val profile = _userProfile.value

    // Calculate badges
    val calculatedBadges = BadgeManager.calculateBadges(profile, badgeContext.value)

    // Update userProfile with calculated badges
    val updatedProfile = profile.copy(badges = calculatedBadges)
    _userProfile.value = updatedProfile

    // Also update the separate badges StateFlow for backward compatibility
    _badges.value = calculatedBadges

    // Persist to Firestore
    try {
      val success = badgeRepository?.saveBadgeProgress(currentUser.uid, calculatedBadges) ?: false
      if (success) {
        Log.e(
            "BadgeCalculationSuccess",
            "ProfileViewModel - Successfully saved ${calculatedBadges.size} badges")
      } else {
        Log.e("BadgeCalculationFail", "ProfileViewModel - Failed to save badges to Firestore")
      }
    } catch (e: Exception) {
      Log.e("BadgeCalculationException", "ProfileViewModel - Exception saving badges: ${e.message}")
      e.printStackTrace()
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
              bannerUrl = selectedBanner.ifEmpty { null })

      // Log pour debug
      println("ProfileViewModel - Saving profile: $updatedProfile")
      println("ProfileViewModel - Avatar URL: ${updatedProfile.avatarUrl}")
      println("ProfileViewModel - Banner URL: ${updatedProfile.bannerUrl}")

      // Save to Firestore
      val success = repository.saveUserProfile(updatedProfile)

      println("ProfileViewModel - Save success: $success")

      // Exit edit mode regardless of success/failure
      if (success) {
        // Update local state only if Firestore save succeeded
        _userProfile.value = updatedProfile
        println("ProfileViewModel - Profile updated successfully")

        // Recalculate badges after profile update
        calculateAndUpdateBadges()
      } else {
        // Handle error
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

  /**
   * Signs out the current user from Firebase Auth and resets the profile state.
   *
   * This method performs the following actions:
   * - Signs out the user from Firebase Authentication.
   * - Resets the user profile to a default, empty state.
   * - Clears all badges.
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
    _badges.value = emptyList()
    isEditMode = false
    clearErrors()
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
}
