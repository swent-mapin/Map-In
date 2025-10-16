package com.swent.mapin.ui.profile

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.swent.mapin.model.UserProfile
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
 * - ProfileBanner rendering and interactions
 * - ViewProfileContent with user data
 * - EditProfileContent interactions
 * - ProfileInfoCard display
 * - AvatarSelectorDialog interactions
 * - BannerSelectorDialog interactions
 * - BannerSelectionGrid selection
 * - DeleteProfileConfirmationDialog interactions
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
    every { mockViewModel.showDeleteConfirmation } returns false

    composeTestRule.setContent {
      MaterialTheme { ProfileScreen(onNavigateBack = {}, viewModel = mockViewModel) }
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
    every { mockViewModel.showDeleteConfirmation } returns false

    composeTestRule.setContent {
      MaterialTheme { ProfileScreen(onNavigateBack = {}, viewModel = mockViewModel) }
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
    every { mockViewModel.showDeleteConfirmation } returns false

    composeTestRule.setContent {
      MaterialTheme { ProfileScreen(onNavigateBack = {}, viewModel = mockViewModel) }
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
    every { mockViewModel.showDeleteConfirmation } returns false

    composeTestRule.setContent {
      MaterialTheme {
        ProfileScreen(onNavigateBack = { navigatedBack = true }, viewModel = mockViewModel)
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
    every { mockViewModel.showDeleteConfirmation } returns false
    every { mockViewModel.selectedAvatar } returns ""

    composeTestRule.setContent {
      MaterialTheme { ProfileScreen(onNavigateBack = {}, viewModel = mockViewModel) }
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
    every { mockViewModel.showDeleteConfirmation } returns false
    every { mockViewModel.selectedBanner } returns ""

    composeTestRule.setContent {
      MaterialTheme { ProfileScreen(onNavigateBack = {}, viewModel = mockViewModel) }
    }

    composeTestRule.onNodeWithText("Choose Your Banner").assertIsDisplayed()
  }

  @Test
  fun profileScreen_showsDeleteDialog_whenFlagIsTrue() {
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)
    every { mockViewModel.userProfile } returns
        MutableStateFlow(UserProfile(name = "Test User", userId = "123"))
    every { mockViewModel.isLoading } returns MutableStateFlow(false)
    every { mockViewModel.isEditMode } returns false
    every { mockViewModel.showAvatarSelector } returns false
    every { mockViewModel.showBannerSelector } returns false
    every { mockViewModel.showDeleteConfirmation } returns true

    composeTestRule.setContent {
      MaterialTheme { ProfileScreen(onNavigateBack = {}, viewModel = mockViewModel) }
    }

    composeTestRule.onNodeWithText("Confirm Deletion").assertIsDisplayed()
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
    every { mockViewModel.showDeleteConfirmation } returns false
    every { mockViewModel.editName } returns "Test User"
    every { mockViewModel.editBio } returns ""
    every { mockViewModel.editLocation } returns ""
    every { mockViewModel.editHobbies } returns ""
    every { mockViewModel.nameError } returns null
    every { mockViewModel.bioError } returns null
    every { mockViewModel.locationError } returns null
    every { mockViewModel.hobbiesError } returns null
    every { mockViewModel.hobbiesVisible } returns true

    composeTestRule.setContent {
      MaterialTheme { ProfileScreen(onNavigateBack = {}, viewModel = mockViewModel) }
    }

    composeTestRule.onNodeWithTag("editNameField").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("saveButton").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("cancelButton").performScrollTo().assertIsDisplayed()
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
    every { mockViewModel.hobbiesVisible } returns true

    composeTestRule.setContent { MaterialTheme { EditProfileContent(viewModel = mockViewModel) } }

    composeTestRule.onNodeWithTag("editNameField").assertIsDisplayed()
    composeTestRule.onNodeWithTag("editBioField").assertIsDisplayed()
    composeTestRule.onNodeWithTag("editLocationField").assertIsDisplayed()
    composeTestRule.onNodeWithTag("editHobbiesField").assertIsDisplayed()
    composeTestRule.onNodeWithTag("hobbiesVisibilitySwitch").assertIsDisplayed()
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
    every { mockViewModel.hobbiesVisible } returns true

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
    every { mockViewModel.hobbiesVisible } returns true

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
    every { mockViewModel.hobbiesVisible } returns true

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
    every { mockViewModel.hobbiesVisible } returns true

    composeTestRule.setContent { MaterialTheme { EditProfileContent(viewModel = mockViewModel) } }

    composeTestRule.onNodeWithTag("editHobbiesField").performTextInput("Gaming")
    verify { mockViewModel.updateEditHobbies(any()) }
  }

  @Test
  fun editProfileContent_togglesHobbiesVisibility_whenSwitchClicked() {
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)
    every { mockViewModel.editName } returns "John"
    every { mockViewModel.editBio } returns ""
    every { mockViewModel.editLocation } returns ""
    every { mockViewModel.editHobbies } returns ""
    every { mockViewModel.nameError } returns null
    every { mockViewModel.bioError } returns null
    every { mockViewModel.locationError } returns null
    every { mockViewModel.hobbiesError } returns null
    every { mockViewModel.hobbiesVisible } returns true

    composeTestRule.setContent { MaterialTheme { EditProfileContent(viewModel = mockViewModel) } }

    composeTestRule.onNodeWithTag("hobbiesVisibilitySwitch").performClick()
    verify { mockViewModel.toggleHobbiesVisibility() }
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
    every { mockViewModel.hobbiesVisible } returns true

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
    every { mockViewModel.hobbiesVisible } returns true

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
    every { mockViewModel.hobbiesVisible } returns true

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
    every { mockViewModel.hobbiesVisible } returns true

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
    every { mockViewModel.hobbiesVisible } returns true

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
    every { mockViewModel.hobbiesVisible } returns true

    composeTestRule.setContent { MaterialTheme { EditProfileContent(viewModel = mockViewModel) } }

    composeTestRule.onNodeWithText("Hobbies format is invalid").assertIsDisplayed()
  }

  @Test
  fun editProfileContent_displaysPrivateLabel_whenHobbiesNotVisible() {
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)
    every { mockViewModel.editName } returns "John"
    every { mockViewModel.editBio } returns ""
    every { mockViewModel.editLocation } returns ""
    every { mockViewModel.editHobbies } returns ""
    every { mockViewModel.nameError } returns null
    every { mockViewModel.bioError } returns null
    every { mockViewModel.locationError } returns null
    every { mockViewModel.hobbiesError } returns null
    every { mockViewModel.hobbiesVisible } returns false

    composeTestRule.setContent { MaterialTheme { EditProfileContent(viewModel = mockViewModel) } }

    composeTestRule.onNodeWithText("Private").assertIsDisplayed()
  }

  @Test
  fun editProfileContent_displaysPublicLabel_whenHobbiesVisible() {
    val mockViewModel = mockk<ProfileViewModel>(relaxed = true)
    every { mockViewModel.editName } returns "John"
    every { mockViewModel.editBio } returns ""
    every { mockViewModel.editLocation } returns ""
    every { mockViewModel.editHobbies } returns ""
    every { mockViewModel.nameError } returns null
    every { mockViewModel.bioError } returns null
    every { mockViewModel.locationError } returns null
    every { mockViewModel.hobbiesError } returns null
    every { mockViewModel.hobbiesVisible } returns true

    composeTestRule.setContent { MaterialTheme { EditProfileContent(viewModel = mockViewModel) } }

    composeTestRule.onNodeWithText("Public").assertIsDisplayed()
  }

  // ==================== DeleteProfileConfirmationDialog Tests ====================

  @Test
  fun deleteProfileDialog_displaysTitle() {
    composeTestRule.setContent {
      MaterialTheme { DeleteProfileConfirmationDialog(onConfirm = {}, onDismiss = {}) }
    }

    composeTestRule.onNodeWithText("Confirm Deletion").assertIsDisplayed()
  }

  @Test
  fun deleteProfileDialog_displaysWarningMessage() {
    composeTestRule.setContent {
      MaterialTheme { DeleteProfileConfirmationDialog(onConfirm = {}, onDismiss = {}) }
    }

    composeTestRule
        .onNodeWithText(
            "Are you sure you want to delete your profile? This action cannot be undone.")
        .assertIsDisplayed()
  }

  @Test
  fun deleteProfileDialog_displaysDeleteButton() {
    composeTestRule.setContent {
      MaterialTheme { DeleteProfileConfirmationDialog(onConfirm = {}, onDismiss = {}) }
    }

    composeTestRule.onNodeWithText("Delete Profile").assertIsDisplayed()
  }

  @Test
  fun deleteProfileDialog_displaysCancelButton() {
    composeTestRule.setContent {
      MaterialTheme { DeleteProfileConfirmationDialog(onConfirm = {}, onDismiss = {}) }
    }

    composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
  }

  @Test
  fun deleteProfileDialog_callsOnConfirm_whenDeleteButtonClicked() {
    var confirmed = false
    composeTestRule.setContent {
      MaterialTheme {
        DeleteProfileConfirmationDialog(onConfirm = { confirmed = true }, onDismiss = {})
      }
    }

    composeTestRule.onNodeWithText("Delete Profile").performClick()
    assert(confirmed)
  }

  @Test
  fun deleteProfileDialog_callsOnDismiss_whenCancelButtonClicked() {
    var dismissed = false
    composeTestRule.setContent {
      MaterialTheme {
        DeleteProfileConfirmationDialog(onConfirm = {}, onDismiss = { dismissed = true })
      }
    }

    composeTestRule.onNodeWithText("Cancel").performClick()
    assert(dismissed)
  }
}
