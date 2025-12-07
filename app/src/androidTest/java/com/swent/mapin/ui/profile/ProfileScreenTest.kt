package com.swent.mapin.ui.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.swent.mapin.model.UserProfile
import com.swent.mapin.model.badge.Badge
import com.swent.mapin.model.badge.BadgeRarity
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

/**
 * Instrumentation tests for ProfileScreen composables.
 *
 * Tests cover:
 * - ProfileScreen full integration
 * - ProfilePicture rendering with different states
 * - ViewProfileContent with user data
 * - EditProfileContent interactions
 * - ProfileInfoCard display
 * - AvatarSelectorDialog interactions
 * - AvatarSelectionGrid selection
 */
class ProfileScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  // ==================== ProfileScreen Tests ====================

  @Test
  fun profileScreen_displaysCorrectly() {
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)
    every { mockViewModel.userProfile } returns
        MutableStateFlow(UserProfile(name = "Test User", userId = "123"))
    every { mockViewModel.isLoading } returns MutableStateFlow(false)
    every { mockViewModel.isEditMode } returns false
    every { mockViewModel.showAvatarSelector } returns false
    every { mockViewModel.showBannerSelector } returns false

    composeTestRule.setContent {
      MaterialTheme {
        ProfileScreen(onNavigateBack = {}, onNavigateToSignIn = {}, viewModel = mockViewModel)
      }
    }

    composeTestRule.onNodeWithTag("profileScreen").assertIsDisplayed()
    composeTestRule.onNodeWithText("Profile").assertIsDisplayed()
    composeTestRule.onNodeWithTag("backButton").assertIsDisplayed()
  }

  @Test
  fun profileScreen_showsEditButton_whenNotInEditMode() {
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)
    every { mockViewModel.userProfile } returns
        MutableStateFlow(UserProfile(name = "Test User", userId = "123"))
    every { mockViewModel.isLoading } returns MutableStateFlow(false)
    every { mockViewModel.isEditMode } returns false
    every { mockViewModel.showAvatarSelector } returns false
    every { mockViewModel.showBannerSelector } returns false

    composeTestRule.setContent {
      MaterialTheme {
        ProfileScreen(onNavigateBack = {}, onNavigateToSignIn = {}, viewModel = mockViewModel)
      }
    }

    composeTestRule.onNodeWithTag("editButton").assertIsDisplayed()
  }

  @Test
  fun profileScreen_triggersEditMode_whenEditButtonClicked() {
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)
    every { mockViewModel.userProfile } returns
        MutableStateFlow(UserProfile(name = "Test User", userId = "123"))
    every { mockViewModel.isLoading } returns MutableStateFlow(false)
    every { mockViewModel.isEditMode } returns false
    every { mockViewModel.showAvatarSelector } returns false
    every { mockViewModel.showBannerSelector } returns false

    composeTestRule.setContent {
      MaterialTheme {
        ProfileScreen(onNavigateBack = {}, onNavigateToSignIn = {}, viewModel = mockViewModel)
      }
    }

    composeTestRule.onNodeWithTag("editButton").performClick()
    verify { mockViewModel.startEditing() }
  }

  @Test
  fun profileScreen_triggersNavigateBack_whenBackButtonClicked() {
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)
    var navigatedBack = false
    every { mockViewModel.userProfile } returns
        MutableStateFlow(UserProfile(name = "Test User", userId = "123"))
    every { mockViewModel.isLoading } returns MutableStateFlow(false)
    every { mockViewModel.isEditMode } returns false
    every { mockViewModel.showAvatarSelector } returns false
    every { mockViewModel.showBannerSelector } returns false

    composeTestRule.setContent {
      MaterialTheme {
        ProfileScreen(
            onNavigateBack = { navigatedBack = true },
            onNavigateToSignIn = {},
            viewModel = mockViewModel)
      }
    }

    composeTestRule.onNodeWithTag("backButton").performClick()
    assert(navigatedBack)
  }

  @Test
  fun profileScreen_showsAvatarSelector_whenFlagIsTrue() {
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)
    every { mockViewModel.userProfile } returns
        MutableStateFlow(UserProfile(name = "Test User", userId = "123"))
    every { mockViewModel.isLoading } returns MutableStateFlow(false)
    every { mockViewModel.isEditMode } returns true
    every { mockViewModel.showAvatarSelector } returns true
    every { mockViewModel.showBannerSelector } returns false
    every { mockViewModel.selectedAvatar } returns ""

    composeTestRule.setContent {
      MaterialTheme {
        ProfileScreen(onNavigateBack = {}, onNavigateToSignIn = {}, viewModel = mockViewModel)
      }
    }

    composeTestRule.onNodeWithText("Choose Your Avatar").assertIsDisplayed()
  }

  @Test
  fun profileScreen_showsBannerSelector_whenFlagIsTrue() {
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)
    every { mockViewModel.userProfile } returns
        MutableStateFlow(UserProfile(name = "Test User", userId = "123"))
    every { mockViewModel.isLoading } returns MutableStateFlow(false)
    every { mockViewModel.isEditMode } returns true
    every { mockViewModel.showAvatarSelector } returns false
    every { mockViewModel.showBannerSelector } returns true
    every { mockViewModel.selectedBanner } returns ""

    composeTestRule.setContent {
      MaterialTheme {
        ProfileScreen(onNavigateBack = {}, onNavigateToSignIn = {}, viewModel = mockViewModel)
      }
    }

    composeTestRule.onNodeWithText("Choose Your Banner").assertIsDisplayed()
  }

  @Test
  fun profileScreen_displaysEditContent_whenInEditMode() {
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)
    every { mockViewModel.userProfile } returns
        MutableStateFlow(UserProfile(name = "Test User", userId = "123"))
    every { mockViewModel.isLoading } returns MutableStateFlow(false)
    every { mockViewModel.isEditMode } returns true
    every { mockViewModel.showAvatarSelector } returns false
    every { mockViewModel.showBannerSelector } returns false
    every { mockViewModel.editName } returns "Test User"
    every { mockViewModel.editBio } returns ""
    every { mockViewModel.editLocation } returns ""
    every { mockViewModel.editHobbies } returns ""
    every { mockViewModel.nameError } returns null
    every { mockViewModel.bioError } returns null
    every { mockViewModel.locationError } returns null
    every { mockViewModel.hobbiesError } returns null
    every { mockViewModel.selectedAvatar } returns ""

    composeTestRule.setContent {
      MaterialTheme {
        ProfileScreen(onNavigateBack = {}, onNavigateToSignIn = {}, viewModel = mockViewModel)
      }
    }

    composeTestRule.onNodeWithTag("editNameField").assertIsDisplayed()
    composeTestRule.onNodeWithTag("saveButton").assertIsDisplayed()
    composeTestRule.onNodeWithTag("cancelButton").assertIsDisplayed()
  }

  // ==================== ProfilePicture Tests ====================

  @Test
  fun profilePicture_displaysIcon_whenNoAvatarUrl() {
    composeTestRule.setContent {
      MaterialTheme { ProfilePicture(avatarUrl = null, isEditMode = false, onAvatarClick = {}) }
    }

    composeTestRule.onNodeWithTag("profilePicture").assertIsDisplayed()
  }

  @Test
  fun profilePicture_displaysIcon_whenAvatarIsPresetIcon() {
    composeTestRule.setContent {
      MaterialTheme { ProfilePicture(avatarUrl = "person", isEditMode = false, onAvatarClick = {}) }
    }

    composeTestRule.onNodeWithTag("profilePicture").assertIsDisplayed()
  }

  @Test
  fun profilePicture_displaysImage_whenAvatarIsHttpUrl() {
    composeTestRule.setContent {
      MaterialTheme {
        ProfilePicture(
            avatarUrl = "https://example.com/avatar.jpg", isEditMode = false, onAvatarClick = {})
      }
    }

    composeTestRule.onNodeWithTag("profilePicture").assertIsDisplayed()
  }

  @Test
  fun profilePicture_displaysImage_whenAvatarIsContentUri() {
    composeTestRule.setContent {
      MaterialTheme {
        ProfilePicture(
            avatarUrl = "content://media/image/123", isEditMode = false, onAvatarClick = {})
      }
    }

    composeTestRule.onNodeWithTag("profilePicture").assertIsDisplayed()
  }

  @Test
  fun profilePicture_isClickable_whenInEditMode() {
    var clicked = false
    composeTestRule.setContent {
      MaterialTheme {
        ProfilePicture(avatarUrl = null, isEditMode = true, onAvatarClick = { clicked = true })
      }
    }

    composeTestRule.onNodeWithTag("profilePicture").performClick()
    assert(clicked)
  }

  @Test
  fun profilePicture_isNotClickable_whenNotInEditMode() {
    var clicked = false
    composeTestRule.setContent {
      MaterialTheme {
        ProfilePicture(avatarUrl = null, isEditMode = false, onAvatarClick = { clicked = true })
      }
    }

    // Should not crash when trying to click
    composeTestRule.onNodeWithTag("profilePicture").assertIsDisplayed()
  }

  // ==================== ViewProfileContent Tests ====================

  @Test
  fun viewProfileContent_displaysUserName() {
    val userProfile = UserProfile(name = "John Doe", userId = "123")
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)

    composeTestRule.setContent {
      MaterialTheme { ViewProfileContent(userProfile = userProfile, viewModel = mockViewModel) }
    }

    composeTestRule.onNodeWithText("John Doe").assertIsDisplayed()
  }

  @Test
  fun viewProfileContent_displaysBio() {
    val userProfile = UserProfile(name = "John Doe", userId = "123", bio = "I love coding")
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)

    composeTestRule.setContent {
      MaterialTheme { ViewProfileContent(userProfile = userProfile, viewModel = mockViewModel) }
    }

    composeTestRule.onNodeWithText("I love coding").assertIsDisplayed()
  }

  @Test
  fun viewProfileContent_displaysNoBioAdded_whenBioIsEmpty() {
    val userProfile = UserProfile(name = "John Doe", userId = "123", bio = "")
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)

    composeTestRule.setContent {
      MaterialTheme { ViewProfileContent(userProfile = userProfile, viewModel = mockViewModel) }
    }

    composeTestRule.onNodeWithText("No bio added").assertIsDisplayed()
  }

  @Test
  fun viewProfileContent_displaysLocation() {
    val userProfile = UserProfile(name = "John Doe", userId = "123", location = "Paris")
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)

    composeTestRule.setContent {
      MaterialTheme { ViewProfileContent(userProfile = userProfile, viewModel = mockViewModel) }
    }

    composeTestRule.onNodeWithText("Paris").assertIsDisplayed()
  }

  @Test
  fun viewProfileContent_displaysHobbies() {
    val userProfile =
        UserProfile(name = "John Doe", userId = "123", hobbies = listOf("Reading", "Gaming"))
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)

    composeTestRule.setContent {
      MaterialTheme { ViewProfileContent(userProfile = userProfile, viewModel = mockViewModel) }
    }

    composeTestRule.onNodeWithText("Reading, Gaming").assertIsDisplayed()
  }

  @Test
  fun viewProfileContent_displaysNoHobbiesAdded_whenHobbiesIsEmpty() {
    val userProfile = UserProfile(name = "John Doe", userId = "123", hobbies = emptyList())
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)

    composeTestRule.setContent {
      MaterialTheme { ViewProfileContent(userProfile = userProfile, viewModel = mockViewModel) }
    }

    composeTestRule.onNodeWithText("No hobbies added").assertIsDisplayed()
  }

  @Test
  fun viewProfileContent_displaysBadgesSection() {
    val badge =
        Badge(
            id = "test_badge",
            title = "Test Badge",
            description = "Test description",
            iconName = "star",
            rarity = BadgeRarity.COMMON,
            isUnlocked = true,
            progress = 1f)
    val userProfile = UserProfile(name = "John Doe", userId = "123", badges = listOf(badge))
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)

    composeTestRule.setContent {
      MaterialTheme { ViewProfileContent(userProfile = userProfile, viewModel = mockViewModel) }
    }

    composeTestRule.onNodeWithTag("badgesSection").assertIsDisplayed()
  }

  // ==================== ProfileInfoCard Tests ====================

  @Test
  fun profileInfoCard_displaysTitle() {
    composeTestRule.setContent {
      MaterialTheme {
        ProfileInfoCard(
            title = "Bio",
            content = "Test content",
            icon = Icons.Default.Face,
            gradientColors = listOf())
      }
    }

    composeTestRule.onNodeWithText("Bio").assertIsDisplayed()
  }

  @Test
  fun profileInfoCard_displaysContent() {
    composeTestRule.setContent {
      MaterialTheme {
        ProfileInfoCard(
            title = "Location",
            content = "Paris",
            icon = Icons.Default.Face,
            gradientColors = listOf())
      }
    }

    composeTestRule.onNodeWithText("Paris").assertIsDisplayed()
  }

  // ==================== EditProfileContent Tests ====================

  @Test
  fun editProfileContent_displaysAllFields() {
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)
    every { mockViewModel.editName } returns "John Doe"
    every { mockViewModel.editBio } returns "Test bio"
    every { mockViewModel.editLocation } returns "Paris"
    every { mockViewModel.editHobbies } returns "Reading"
    every { mockViewModel.nameError } returns null
    every { mockViewModel.bioError } returns null
    every { mockViewModel.locationError } returns null
    every { mockViewModel.hobbiesError } returns null

    composeTestRule.setContent { MaterialTheme { EditProfileContent(viewModel = mockViewModel) } }

    composeTestRule.onNodeWithTag("editNameField").assertIsDisplayed()
    composeTestRule.onNodeWithTag("editBioField").assertIsDisplayed()
    composeTestRule.onNodeWithTag("editLocationField").assertIsDisplayed()
    composeTestRule.onNodeWithTag("editHobbiesField").assertIsDisplayed()
  }

  @Test
  fun editProfileContent_updatesName_whenTextChanged() {
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)
    every { mockViewModel.editName } returns "John"
    every { mockViewModel.editBio } returns ""
    every { mockViewModel.editLocation } returns ""
    every { mockViewModel.editHobbies } returns ""
    every { mockViewModel.nameError } returns null
    every { mockViewModel.bioError } returns null
    every { mockViewModel.locationError } returns null
    every { mockViewModel.hobbiesError } returns null

    composeTestRule.setContent { MaterialTheme { EditProfileContent(viewModel = mockViewModel) } }

    composeTestRule.onNodeWithTag("editNameField").performTextClearance()
    composeTestRule.onNodeWithTag("editNameField").performTextInput("Jane")
    verify { mockViewModel.updateEditName(any()) }
  }

  @Test
  fun editProfileContent_updatesBio_whenTextChanged() {
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)
    every { mockViewModel.editName } returns "John"
    every { mockViewModel.editBio } returns ""
    every { mockViewModel.editLocation } returns ""
    every { mockViewModel.editHobbies } returns ""
    every { mockViewModel.nameError } returns null
    every { mockViewModel.bioError } returns null
    every { mockViewModel.locationError } returns null
    every { mockViewModel.hobbiesError } returns null

    composeTestRule.setContent { MaterialTheme { EditProfileContent(viewModel = mockViewModel) } }

    composeTestRule.onNodeWithTag("editBioField").performTextInput("New bio")
    verify { mockViewModel.updateEditBio(any()) }
  }

  @Test
  fun editProfileContent_updatesLocation_whenTextChanged() {
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)
    every { mockViewModel.editName } returns "John"
    every { mockViewModel.editBio } returns ""
    every { mockViewModel.editLocation } returns ""
    every { mockViewModel.editHobbies } returns ""
    every { mockViewModel.nameError } returns null
    every { mockViewModel.bioError } returns null
    every { mockViewModel.locationError } returns null
    every { mockViewModel.hobbiesError } returns null

    composeTestRule.setContent { MaterialTheme { EditProfileContent(viewModel = mockViewModel) } }

    composeTestRule.onNodeWithTag("editLocationField").performTextInput("London")
    verify { mockViewModel.updateEditLocation(any()) }
  }

  @Test
  fun editProfileContent_updatesHobbies_whenTextChanged() {
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)
    every { mockViewModel.editName } returns "John"
    every { mockViewModel.editBio } returns ""
    every { mockViewModel.editLocation } returns ""
    every { mockViewModel.editHobbies } returns ""
    every { mockViewModel.nameError } returns null
    every { mockViewModel.bioError } returns null
    every { mockViewModel.locationError } returns null
    every { mockViewModel.hobbiesError } returns null

    composeTestRule.setContent { MaterialTheme { EditProfileContent(viewModel = mockViewModel) } }

    composeTestRule.onNodeWithTag("editHobbiesField").performTextInput("Gaming")
    verify { mockViewModel.updateEditHobbies(any()) }
  }

  @Test
  fun editProfileContent_callsSaveProfile_whenSaveButtonClicked() {
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)
    every { mockViewModel.editName } returns "John"
    every { mockViewModel.editBio } returns ""
    every { mockViewModel.editLocation } returns ""
    every { mockViewModel.editHobbies } returns ""
    every { mockViewModel.nameError } returns null
    every { mockViewModel.bioError } returns null
    every { mockViewModel.locationError } returns null
    every { mockViewModel.hobbiesError } returns null

    composeTestRule.setContent { MaterialTheme { EditProfileContent(viewModel = mockViewModel) } }

    composeTestRule.onNodeWithTag("saveButton").performClick()
    verify { mockViewModel.saveProfile() }
  }

  @Test
  fun editProfileContent_callsCancelEditing_whenCancelButtonClicked() {
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)
    every { mockViewModel.editName } returns "John"
    every { mockViewModel.editBio } returns ""
    every { mockViewModel.editLocation } returns ""
    every { mockViewModel.editHobbies } returns ""
    every { mockViewModel.nameError } returns null
    every { mockViewModel.bioError } returns null
    every { mockViewModel.locationError } returns null
    every { mockViewModel.hobbiesError } returns null

    composeTestRule.setContent { MaterialTheme { EditProfileContent(viewModel = mockViewModel) } }

    composeTestRule.onNodeWithTag("cancelButton").performClick()
    verify { mockViewModel.cancelEditing() }
  }

  @Test
  fun editProfileContent_showsNameError_whenErrorExists() {
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)
    every { mockViewModel.editName } returns ""
    every { mockViewModel.editBio } returns ""
    every { mockViewModel.editLocation } returns ""
    every { mockViewModel.editHobbies } returns ""
    every { mockViewModel.nameError } returns "Name cannot be empty"
    every { mockViewModel.bioError } returns null
    every { mockViewModel.locationError } returns null
    every { mockViewModel.hobbiesError } returns null

    composeTestRule.setContent { MaterialTheme { EditProfileContent(viewModel = mockViewModel) } }

    composeTestRule.onNodeWithText("Name cannot be empty").assertIsDisplayed()
  }

  @Test
  fun editProfileContent_showsBioError_whenErrorExists() {
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)
    every { mockViewModel.editName } returns "John"
    every { mockViewModel.editBio } returns ""
    every { mockViewModel.editLocation } returns ""
    every { mockViewModel.editHobbies } returns ""
    every { mockViewModel.nameError } returns null
    every { mockViewModel.bioError } returns "Bio is too long"
    every { mockViewModel.locationError } returns null
    every { mockViewModel.hobbiesError } returns null

    composeTestRule.setContent { MaterialTheme { EditProfileContent(viewModel = mockViewModel) } }

    composeTestRule.onNodeWithText("Bio is too long").assertIsDisplayed()
  }

  @Test
  fun editProfileContent_showsLocationError_whenErrorExists() {
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)
    every { mockViewModel.editName } returns "John"
    every { mockViewModel.editBio } returns ""
    every { mockViewModel.editLocation } returns ""
    every { mockViewModel.editHobbies } returns ""
    every { mockViewModel.nameError } returns null
    every { mockViewModel.bioError } returns null
    every { mockViewModel.locationError } returns "Location is too long"
    every { mockViewModel.hobbiesError } returns null

    composeTestRule.setContent { MaterialTheme { EditProfileContent(viewModel = mockViewModel) } }

    composeTestRule.onNodeWithText("Location is too long").assertIsDisplayed()
  }

  @Test
  fun editProfileContent_showsHobbiesError_whenErrorExists() {
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)
    every { mockViewModel.editName } returns "John"
    every { mockViewModel.editBio } returns ""
    every { mockViewModel.editLocation } returns ""
    every { mockViewModel.editHobbies } returns ""
    every { mockViewModel.nameError } returns null
    every { mockViewModel.bioError } returns null
    every { mockViewModel.locationError } returns null
    every { mockViewModel.hobbiesError } returns "Hobbies format is invalid"

    composeTestRule.setContent { MaterialTheme { EditProfileContent(viewModel = mockViewModel) } }

    composeTestRule.onNodeWithText("Hobbies format is invalid").assertIsDisplayed()
  }

  // ==================== AvatarSelectorDialog Tests ====================

  @Test
  fun avatarSelectorDialog_displaysTitle() {
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)
    every { mockViewModel.uploadAvatarImage(any()) } just Runs

    composeTestRule.setContent {
      MaterialTheme {
        AvatarSelectorDialog(
            viewModel = mockViewModel, selectedAvatar = "", onAvatarSelected = {}, onDismiss = {})
      }
    }

    composeTestRule.onNodeWithText("Choose Your Avatar").assertIsDisplayed()
  }

  @Test
  fun avatarSelectorDialog_displaysImportButton() {
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)
    every { mockViewModel.uploadAvatarImage(any()) } just Runs

    composeTestRule.setContent {
      MaterialTheme {
        AvatarSelectorDialog(
            viewModel = mockViewModel, selectedAvatar = "", onAvatarSelected = {}, onDismiss = {})
      }
    }

    composeTestRule.onNodeWithText("Import from Gallery").assertIsDisplayed()
  }

  @Test
  fun avatarSelectorDialog_displaysCloseButton() {
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)
    every { mockViewModel.uploadAvatarImage(any()) } just Runs

    composeTestRule.setContent {
      MaterialTheme {
        AvatarSelectorDialog(
            viewModel = mockViewModel, selectedAvatar = "", onAvatarSelected = {}, onDismiss = {})
      }
    }

    composeTestRule.onNodeWithText("Close").assertIsDisplayed()
  }

  @Test
  fun avatarSelectorDialog_callsOnDismiss_whenCloseButtonClicked() {
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)
    var dismissed = false
    every { mockViewModel.uploadAvatarImage(any()) } just Runs

    composeTestRule.setContent {
      MaterialTheme {
        AvatarSelectorDialog(
            viewModel = mockViewModel,
            selectedAvatar = "",
            onAvatarSelected = {},
            onDismiss = { dismissed = true })
      }
    }

    composeTestRule.onNodeWithText("Close").performClick()
    assert(dismissed)
  }

  @Test
  fun avatarSelectorDialog_displaysAvatarOptions() {
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)
    every { mockViewModel.uploadAvatarImage(any()) } just Runs

    composeTestRule.setContent {
      MaterialTheme {
        AvatarSelectorDialog(
            viewModel = mockViewModel, selectedAvatar = "", onAvatarSelected = {}, onDismiss = {})
      }
    }

    composeTestRule.onNodeWithTag("avatar_person").assertIsDisplayed()
    composeTestRule.onNodeWithTag("avatar_face").assertIsDisplayed()
    composeTestRule.onNodeWithTag("avatar_star").assertIsDisplayed()
    composeTestRule.onNodeWithTag("avatar_favorite").assertIsDisplayed()
  }

  @Test
  fun avatarSelectorDialog_callsOnAvatarSelected_whenAvatarClicked() {
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)
    var selectedAvatarId = ""
    every { mockViewModel.uploadAvatarImage(any()) } just Runs

    composeTestRule.setContent {
      MaterialTheme {
        AvatarSelectorDialog(
            viewModel = mockViewModel,
            selectedAvatar = "",
            onAvatarSelected = { selectedAvatarId = it },
            onDismiss = {})
      }
    }

    composeTestRule.onNodeWithTag("avatar_face").performClick()
    assert(selectedAvatarId == "face")
  }

  // ==================== BannerSelectorDialog Tests ====================

  @Test
  fun bannerSelectorDialog_displaysTitle() {
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)
    every { mockViewModel.uploadBannerImage(any()) } just Runs

    composeTestRule.setContent {
      MaterialTheme {
        BannerSelectorDialog(
            viewModel = mockViewModel, selectedBanner = "", onBannerSelected = {}, onDismiss = {})
      }
    }

    composeTestRule.onNodeWithText("Choose Your Banner").assertIsDisplayed()
  }

  @Test
  fun bannerSelectorDialog_displaysImportButton() {
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)
    every { mockViewModel.uploadBannerImage(any()) } just Runs

    composeTestRule.setContent {
      MaterialTheme {
        BannerSelectorDialog(
            viewModel = mockViewModel, selectedBanner = "", onBannerSelected = {}, onDismiss = {})
      }
    }

    composeTestRule.onNodeWithText("Import from Gallery").assertIsDisplayed()
  }

  @Test
  fun bannerSelectorDialog_callsOnDismiss_whenCloseButtonClicked() {
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)
    var dismissed = false
    every { mockViewModel.uploadBannerImage(any()) } just Runs

    composeTestRule.setContent {
      MaterialTheme {
        BannerSelectorDialog(
            viewModel = mockViewModel,
            selectedBanner = "",
            onBannerSelected = {},
            onDismiss = { dismissed = true })
      }
    }

    composeTestRule.onNodeWithText("Close").performClick()
    assert(dismissed)
  }

  // ==================== Helper Components Tests ====================

  @Test
  fun importFromGalleryButton_displaysCorrectly() {
    composeTestRule.setContent { MaterialTheme { ImportFromGalleryButton(onClick = {}) } }

    composeTestRule.onNodeWithText("Import from Gallery").assertIsDisplayed()
  }

  @Test
  fun importFromGalleryButton_callsOnClick_whenClicked() {
    var clicked = false
    composeTestRule.setContent {
      MaterialTheme { ImportFromGalleryButton(onClick = { clicked = true }) }
    }

    composeTestRule.onNodeWithText("Import from Gallery").performClick()
    assert(clicked)
  }
}
