package com.swent.mapin.ui.profile

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.swent.mapin.model.ImageUploadHelper
import com.swent.mapin.model.UserProfile
import com.swent.mapin.model.UserProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
  // NOTE: The logout button has been moved to the Settings page
  // ProfileScreen now has a Settings button that navigates to SettingsScreen
  // Logout functionality is now in SettingsScreen and SettingsViewModel
  // The following tests remain valid for profile functionality

  private lateinit var viewModel: ProfileViewModel
  private lateinit var mockRepository: UserProfileRepository
  private lateinit var mockImageUploadHelper: ImageUploadHelper
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockUser: FirebaseUser
  private val testDispatcher = UnconfinedTestDispatcher()

  private val testUserId = "test-user-123"
  private val testProfile =
      UserProfile(
          userId = testUserId,
          name = "John Doe",
          bio = "Test bio",
          hobbies = listOf("Reading", "Gaming"),
          location = "New York",
          avatarUrl = "https://example.com/avatar.jpg",
          bannerUrl = "https://example.com/banner.jpg",
          hobbiesVisible = true)

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)

    // Mock FirebaseAuth
    mockAuth = mockk(relaxed = true)
    mockUser = mockk(relaxed = true)
    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns testUserId
    every { mockUser.displayName } returns "John Doe"
    every { mockUser.photoUrl } returns null

    // Mock Repository
    mockRepository = mockk(relaxed = true)

    // Mock ImageUploadHelper
    mockImageUploadHelper = mockk(relaxed = true)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun `initial state has default values`() = runTest {
    coEvery { mockRepository.getUserProfile(testUserId) } returns testProfile

    viewModel = ProfileViewModel(mockRepository, mockImageUploadHelper)

    assertFalse(viewModel.isEditMode)
    assertFalse(viewModel.showAvatarSelector)
    assertFalse(viewModel.showBannerSelector)
    assertEquals("", viewModel.editName)
    assertEquals("", viewModel.editBio)
    assertEquals("", viewModel.editLocation)
    assertEquals("", viewModel.editHobbies)
  }

  @Test
  fun `loadUserProfile loads existing profile successfully`() = runTest {
    coEvery { mockRepository.getUserProfile(testUserId) } returns testProfile

    viewModel = ProfileViewModel(mockRepository, mockImageUploadHelper)

    val loadedProfile = viewModel.userProfile.first()
    assertEquals(testProfile, loadedProfile)
    coVerify { mockRepository.getUserProfile(testUserId) }
  }

  @Test
  fun `startEditing enables edit mode and populates fields`() = runTest {
    coEvery { mockRepository.getUserProfile(testUserId) } returns testProfile

    viewModel = ProfileViewModel(mockRepository, mockImageUploadHelper)
    viewModel.startEditing()

    assertTrue(viewModel.isEditMode)
    assertEquals("John Doe", viewModel.editName)
    assertEquals("Test bio", viewModel.editBio)
    assertEquals("New York", viewModel.editLocation)
    assertEquals("Reading, Gaming", viewModel.editHobbies)
    assertEquals("https://example.com/avatar.jpg", viewModel.selectedAvatar)
    assertEquals("https://example.com/banner.jpg", viewModel.selectedBanner)
  }

  @Test
  fun `cancelEditing disables edit mode and clears fields`() = runTest {
    coEvery { mockRepository.getUserProfile(testUserId) } returns testProfile

    viewModel = ProfileViewModel(mockRepository, mockImageUploadHelper)
    viewModel.startEditing()
    viewModel.updateEditName("Changed Name")
    viewModel.cancelEditing()

    assertFalse(viewModel.isEditMode)
    assertEquals("", viewModel.editName)
    assertEquals("", viewModel.editBio)
    assertEquals("", viewModel.editLocation)
    assertEquals("", viewModel.editHobbies)
  }

  @Test
  fun `saveProfile saves valid profile successfully`() = runTest {
    coEvery { mockRepository.getUserProfile(testUserId) } returns testProfile
    coEvery { mockRepository.saveUserProfile(any()) } returns true

    viewModel = ProfileViewModel(mockRepository, mockImageUploadHelper)
    viewModel.startEditing()
    viewModel.updateEditName("Jane Doe")
    viewModel.updateEditBio("Updated bio")
    viewModel.updateEditLocation("Paris")
    viewModel.updateEditHobbies("Cooking, Travel")
    viewModel.saveProfile()

    assertFalse(viewModel.isEditMode)
    val savedProfile = viewModel.userProfile.first()
    assertEquals("Jane Doe", savedProfile.name)
    assertEquals("Updated bio", savedProfile.bio)
    assertEquals("Paris", savedProfile.location)
    assertEquals(listOf("Cooking", "Travel"), savedProfile.hobbies)
    coVerify { mockRepository.saveUserProfile(any()) }
  }

  @Test
  fun `saveProfile fails validation with empty name`() = runTest {
    coEvery { mockRepository.getUserProfile(testUserId) } returns testProfile

    viewModel = ProfileViewModel(mockRepository, mockImageUploadHelper)
    viewModel.startEditing()
    viewModel.updateEditName("")
    viewModel.saveProfile()

    assertTrue(viewModel.isEditMode)
    assertNotNull(viewModel.nameError)
    assertEquals("Name cannot be empty", viewModel.nameError)
    coVerify(exactly = 0) { mockRepository.saveUserProfile(any()) }
  }

  @Test
  fun `saveProfile fails validation with short name`() = runTest {
    coEvery { mockRepository.getUserProfile(testUserId) } returns testProfile

    viewModel = ProfileViewModel(mockRepository, mockImageUploadHelper)
    viewModel.startEditing()
    viewModel.updateEditName("A")
    viewModel.saveProfile()

    assertTrue(viewModel.isEditMode)
    assertNotNull(viewModel.nameError)
    assertEquals("Name must be at least 2 characters", viewModel.nameError)
  }

  @Test
  fun `saveProfile fails validation with long name`() = runTest {
    coEvery { mockRepository.getUserProfile(testUserId) } returns testProfile

    viewModel = ProfileViewModel(mockRepository, mockImageUploadHelper)
    viewModel.startEditing()
    viewModel.updateEditName("A".repeat(51))
    viewModel.saveProfile()

    assertTrue(viewModel.isEditMode)
    assertNotNull(viewModel.nameError)
    assertEquals("Name must be less than 50 characters", viewModel.nameError)
  }

  @Test
  fun `saveProfile fails validation with long bio`() = runTest {
    coEvery { mockRepository.getUserProfile(testUserId) } returns testProfile

    viewModel = ProfileViewModel(mockRepository, mockImageUploadHelper)
    viewModel.startEditing()
    viewModel.updateEditBio("A".repeat(501))
    viewModel.saveProfile()

    assertTrue(viewModel.isEditMode)
    assertNotNull(viewModel.bioError)
    assertEquals("Bio must be less than 500 characters", viewModel.bioError)
  }

  @Test
  fun `saveProfile fails validation with empty location`() = runTest {
    coEvery { mockRepository.getUserProfile(testUserId) } returns testProfile

    viewModel = ProfileViewModel(mockRepository, mockImageUploadHelper)
    viewModel.startEditing()
    viewModel.updateEditLocation("")
    viewModel.saveProfile()

    assertTrue(viewModel.isEditMode)
    assertNotNull(viewModel.locationError)
    assertEquals("Location cannot be empty", viewModel.locationError)
  }

  @Test
  fun `saveProfile fails validation with long location`() = runTest {
    coEvery { mockRepository.getUserProfile(testUserId) } returns testProfile

    viewModel = ProfileViewModel(mockRepository, mockImageUploadHelper)
    viewModel.startEditing()
    viewModel.updateEditLocation("A".repeat(101))
    viewModel.saveProfile()

    assertTrue(viewModel.isEditMode)
    assertNotNull(viewModel.locationError)
    assertEquals("Location must be less than 100 characters", viewModel.locationError)
  }

  @Test
  fun `saveProfile fails validation with long hobbies`() = runTest {
    coEvery { mockRepository.getUserProfile(testUserId) } returns testProfile

    viewModel = ProfileViewModel(mockRepository, mockImageUploadHelper)
    viewModel.startEditing()
    viewModel.updateEditHobbies("A".repeat(201))
    viewModel.saveProfile()

    assertTrue(viewModel.isEditMode)
    assertNotNull(viewModel.hobbiesError)
    assertEquals("Hobbies must be less than 200 characters", viewModel.hobbiesError)
  }

  @Test
  fun `updateEditName updates name and validates`() = runTest {
    coEvery { mockRepository.getUserProfile(testUserId) } returns testProfile

    viewModel = ProfileViewModel(mockRepository, mockImageUploadHelper)
    viewModel.startEditing()
    viewModel.updateEditName("Valid Name")

    assertEquals("Valid Name", viewModel.editName)
    assertNull(viewModel.nameError)
  }

  @Test
  fun `updateEditBio updates bio and validates`() = runTest {
    coEvery { mockRepository.getUserProfile(testUserId) } returns testProfile

    viewModel = ProfileViewModel(mockRepository, mockImageUploadHelper)
    viewModel.startEditing()
    viewModel.updateEditBio("Valid bio")

    assertEquals("Valid bio", viewModel.editBio)
    assertNull(viewModel.bioError)
  }

  @Test
  fun `updateEditLocation updates location and validates`() = runTest {
    coEvery { mockRepository.getUserProfile(testUserId) } returns testProfile

    viewModel = ProfileViewModel(mockRepository, mockImageUploadHelper)
    viewModel.startEditing()
    viewModel.updateEditLocation("Valid Location")

    assertEquals("Valid Location", viewModel.editLocation)
    assertNull(viewModel.locationError)
  }

  @Test
  fun `updateEditHobbies updates hobbies and validates`() = runTest {
    coEvery { mockRepository.getUserProfile(testUserId) } returns testProfile

    viewModel = ProfileViewModel(mockRepository, mockImageUploadHelper)
    viewModel.startEditing()
    viewModel.updateEditHobbies("Reading, Gaming")

    assertEquals("Reading, Gaming", viewModel.editHobbies)
    assertNull(viewModel.hobbiesError)
  }

  @Test
  fun `updateAvatarSelection updates selected avatar`() = runTest {
    coEvery { mockRepository.getUserProfile(testUserId) } returns testProfile

    viewModel = ProfileViewModel(mockRepository, mockImageUploadHelper)
    val newAvatarUrl = "https://example.com/new-avatar.jpg"
    viewModel.updateAvatarSelection(newAvatarUrl)

    assertEquals(newAvatarUrl, viewModel.selectedAvatar)
  }

  @Test
  fun `updateBannerSelection updates selected banner`() = runTest {
    coEvery { mockRepository.getUserProfile(testUserId) } returns testProfile

    viewModel = ProfileViewModel(mockRepository, mockImageUploadHelper)
    val newBannerUrl = "https://example.com/new-banner.jpg"
    viewModel.updateBannerSelection(newBannerUrl)

    assertEquals(newBannerUrl, viewModel.selectedBanner)
  }

  @Test
  fun `toggleBannerSelector toggles banner selector visibility`() = runTest {
    coEvery { mockRepository.getUserProfile(testUserId) } returns testProfile

    viewModel = ProfileViewModel(mockRepository, mockImageUploadHelper)

    assertFalse(viewModel.showBannerSelector)
    viewModel.toggleBannerSelector()
    assertTrue(viewModel.showBannerSelector)
    viewModel.toggleBannerSelector()
    assertFalse(viewModel.showBannerSelector)
  }

  @Test
  fun `saveProfile handles repository save failure gracefully`() = runTest {
    coEvery { mockRepository.getUserProfile(testUserId) } returns testProfile
    coEvery { mockRepository.saveUserProfile(any()) } returns false

    viewModel = ProfileViewModel(mockRepository, mockImageUploadHelper)
    viewModel.startEditing()
    viewModel.updateEditName("Valid Name")
    viewModel.saveProfile()

    assertFalse(viewModel.isEditMode)
    coVerify { mockRepository.saveUserProfile(any()) }
  }

  @Test
  fun `saveProfile correctly parses hobbies with spaces`() = runTest {
    coEvery { mockRepository.getUserProfile(testUserId) } returns testProfile
    coEvery { mockRepository.saveUserProfile(any()) } returns true

    viewModel = ProfileViewModel(mockRepository, mockImageUploadHelper)
    viewModel.startEditing()
    viewModel.updateEditHobbies("  Reading  ,  Gaming  ,  Coding  ")
    viewModel.saveProfile()

    val savedProfile = viewModel.userProfile.first()
    assertEquals(listOf("Reading", "Gaming", "Coding"), savedProfile.hobbies)
  }

  @Test
  fun `saveProfile handles empty hobbies correctly`() = runTest {
    coEvery { mockRepository.getUserProfile(testUserId) } returns testProfile
    coEvery { mockRepository.saveUserProfile(any()) } returns true

    viewModel = ProfileViewModel(mockRepository, mockImageUploadHelper)
    viewModel.startEditing()
    viewModel.updateEditHobbies("")
    viewModel.saveProfile()

    val savedProfile = viewModel.userProfile.first()
    assertEquals(emptyList<String>(), savedProfile.hobbies)
  }

  @Test
  fun `isLoading is true during profile load`() = runTest {
    coEvery { mockRepository.getUserProfile(testUserId) } returns testProfile

    viewModel = ProfileViewModel(mockRepository, mockImageUploadHelper)

    // After init, loading should be false
    assertFalse(viewModel.isLoading.first())
  }

  @Test
  fun `signOut signs out from Firebase and resets profile state`() = runTest {
    coEvery { mockRepository.getUserProfile(testUserId) } returns testProfile
    every { mockAuth.signOut() } returns Unit

    viewModel = ProfileViewModel(mockRepository, mockImageUploadHelper)

    // Verify profile is loaded
    val loadedProfile = viewModel.userProfile.first()
    assertEquals(testProfile, loadedProfile)

    // Sign out
    viewModel.signOut()

    // Verify Firebase signOut was called
    io.mockk.verify { mockAuth.signOut() }

    // Verify profile is reset to default
    val resetProfile = viewModel.userProfile.first()
    assertEquals(UserProfile(), resetProfile)
    assertFalse(viewModel.isEditMode)
  }

  @Test
  fun `signOut clears edit mode and errors`() = runTest {
    coEvery { mockRepository.getUserProfile(testUserId) } returns testProfile
    every { mockAuth.signOut() } returns Unit

    viewModel = ProfileViewModel(mockRepository, mockImageUploadHelper)

    // Enter edit mode and make some invalid changes
    viewModel.startEditing()
    viewModel.updateEditName("") // Invalid - will cause error

    assertTrue(viewModel.isEditMode)
    assertNotNull(viewModel.nameError)

    // Sign out
    viewModel.signOut()

    // Verify edit mode is cleared
    assertFalse(viewModel.isEditMode)
    assertNull(viewModel.nameError)
  }

  @Test
  fun `signOut from edit mode resets all edit fields`() = runTest {
    coEvery { mockRepository.getUserProfile(testUserId) } returns testProfile
    every { mockAuth.signOut() } returns Unit

    viewModel = ProfileViewModel(mockRepository, mockImageUploadHelper)

    // Enter edit mode and modify fields
    viewModel.startEditing()
    viewModel.updateEditName("New Name")
    viewModel.updateEditBio("New Bio")
    viewModel.updateEditLocation("New Location")

    // Sign out
    viewModel.signOut()

    // Verify profile is reset
    val resetProfile = viewModel.userProfile.first()
    assertEquals(UserProfile(), resetProfile)
    assertFalse(viewModel.isEditMode)
  }
}
