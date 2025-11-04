package com.swent.mapin.ui.settings

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.swent.mapin.model.PreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

  private lateinit var viewModel: SettingsViewModel
  private lateinit var mockAuth: FirebaseAuth
  private lateinit var mockFirestore: FirebaseFirestore
  private lateinit var mockPreferencesRepository: PreferencesRepository
  private val testDispatcher = UnconfinedTestDispatcher()

  // Use MutableStateFlow for flows to allow updating during tests
  private val themeModeFlow = MutableStateFlow("system")
  private val showPOIsFlow = MutableStateFlow(true)
  private val showRoadNumbersFlow = MutableStateFlow(true)
  private val showStreetNamesFlow = MutableStateFlow(true)
  private val enable3DViewFlow = MutableStateFlow(false)

  @Before
  fun setup() {
    // Set the main dispatcher for coroutines
    Dispatchers.setMain(testDispatcher)

    // Reset flows to initial values
    themeModeFlow.value = "system"
    showPOIsFlow.value = true
    showRoadNumbersFlow.value = true
    showStreetNamesFlow.value = true
    enable3DViewFlow.value = false

    // Mock dependencies
    mockAuth = mockk(relaxed = true)
    mockFirestore = mockk(relaxed = true)
    mockPreferencesRepository = mockk(relaxed = true)

    // Mock auth.currentUser to return null
    every { mockAuth.currentUser } returns null

    // Mock PreferencesRepository flows with MutableStateFlow
    every { mockPreferencesRepository.themeModeFlow } returns themeModeFlow
    every { mockPreferencesRepository.showPOIsFlow } returns showPOIsFlow
    every { mockPreferencesRepository.showRoadNumbersFlow } returns showRoadNumbersFlow
    every { mockPreferencesRepository.showStreetNamesFlow } returns showStreetNamesFlow
    every { mockPreferencesRepository.enable3DViewFlow } returns enable3DViewFlow

    // Mock suspend functions to update the flows
    coEvery { mockPreferencesRepository.setThemeMode(any()) } coAnswers
        {
          themeModeFlow.value = firstArg()
        }
    coEvery { mockPreferencesRepository.setShowPOIs(any()) } coAnswers
        {
          showPOIsFlow.value = firstArg()
        }
    coEvery { mockPreferencesRepository.setShowRoadNumbers(any()) } coAnswers
        {
          showRoadNumbersFlow.value = firstArg()
        }
    coEvery { mockPreferencesRepository.setShowStreetNames(any()) } coAnswers
        {
          showStreetNamesFlow.value = firstArg()
        }
    coEvery { mockPreferencesRepository.setEnable3DView(any()) } coAnswers
        {
          enable3DViewFlow.value = firstArg()
        }

    // Create ViewModel with mocked dependencies
    viewModel = SettingsViewModel(mockPreferencesRepository, mockAuth, mockFirestore)

    // Trigger stateIn flows to start collecting by accessing them
    viewModel.themeMode.value
    viewModel.mapPreferences.value

    // Advance dispatcher to let stateIn() finish initial collection
    testDispatcher.scheduler.advanceUntilIdle()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun mapPreferences_initialValuesAreCorrect() {
    val preferences = viewModel.mapPreferences.value
    assertEquals(true, preferences.showPOIs)
    assertEquals(true, preferences.showRoadNumbers)
    assertEquals(true, preferences.showStreetNames)
    assertEquals(false, preferences.enable3DView)
  }

  // Note: State update tests have been moved to SettingsViewModelIntegrationTest
  // as they require real coroutine dispatchers for proper stateIn() flow propagation

  @Test
  fun mapPreferencesDataClass_defaultsAreCorrect() {
    val prefs = MapPreferences()
    assertEquals(true, prefs.showPOIs)
    assertEquals(true, prefs.showRoadNumbers)
    assertEquals(true, prefs.showStreetNames)
    assertEquals(false, prefs.enable3DView)
  }

  @Test
  fun mapPreferencesDataClass_customValuesAreCorrect() {
    val prefs =
        MapPreferences(
            showPOIs = false, showRoadNumbers = true, showStreetNames = false, enable3DView = true)
    assertEquals(false, prefs.showPOIs)
    assertEquals(true, prefs.showRoadNumbers)
    assertEquals(false, prefs.showStreetNames)
    assertEquals(true, prefs.enable3DView)
  }

  @Test
  fun isLoading_initialValueIsFalse() {
    val isLoading = viewModel.isLoading.value
    assertEquals(false, isLoading)
  }

  @Test
  fun errorMessage_initialValueIsNull() {
    val errorMessage = viewModel.errorMessage.value
    assertEquals(null, errorMessage)
  }

  @Test
  fun signOut_setsLoadingState() {
    viewModel.signOut()
    // After signOut completes, loading should be false
    assertEquals(false, viewModel.isLoading.value)
  }

  @Test
  fun signOut_callsAuthSignOut() {
    viewModel.signOut()
    verify { mockAuth.signOut() }
  }

  @Test
  fun deleteAccount_whenUserIsNull_setsLoadingToFalse() {
    every { mockAuth.currentUser } returns null
    viewModel.deleteAccount()
    assertEquals(false, viewModel.isLoading.value)
  }

  // Note: Toggle state tests moved to SettingsViewModelIntegrationTest

  @Test
  fun mapPreferences_copyWithShowPOIs_preservesOtherValues() {
    val original =
        MapPreferences(
            showPOIs = true, showRoadNumbers = false, showStreetNames = true, enable3DView = true)
    val copied = original.copy(showPOIs = false)
    assertEquals(false, copied.showPOIs)
    assertEquals(false, copied.showRoadNumbers)
    assertEquals(true, copied.showStreetNames)
    assertEquals(true, copied.enable3DView)
  }

  @Test
  fun mapPreferences_copyWithShowRoadNumbers_preservesOtherValues() {
    val original =
        MapPreferences(
            showPOIs = true, showRoadNumbers = false, showStreetNames = true, enable3DView = true)
    val copied = original.copy(showRoadNumbers = true)
    assertEquals(true, copied.showPOIs)
    assertEquals(true, copied.showRoadNumbers)
    assertEquals(true, copied.showStreetNames)
    assertEquals(true, copied.enable3DView)
  }

  @Test
  fun mapPreferences_copyWithShowStreetNames_preservesOtherValues() {
    val original =
        MapPreferences(
            showPOIs = true, showRoadNumbers = false, showStreetNames = true, enable3DView = true)
    val copied = original.copy(showStreetNames = false)
    assertEquals(true, copied.showPOIs)
    assertEquals(false, copied.showRoadNumbers)
    assertEquals(false, copied.showStreetNames)
    assertEquals(true, copied.enable3DView)
  }

  @Test
  fun mapPreferences_copyWithEnable3DView_preservesOtherValues() {
    val original =
        MapPreferences(
            showPOIs = true, showRoadNumbers = false, showStreetNames = true, enable3DView = false)
    val copied = original.copy(enable3DView = true)
    assertEquals(true, copied.showPOIs)
    assertEquals(false, copied.showRoadNumbers)
    assertEquals(true, copied.showStreetNames)
    assertEquals(true, copied.enable3DView)
  }

  // Note: All toggle tests moved to SettingsViewModelIntegrationTest

  @Test
  fun mapPreferences_equality_sameValues() {
    val prefs1 =
        MapPreferences(
            showPOIs = true, showRoadNumbers = false, showStreetNames = true, enable3DView = false)
    val prefs2 =
        MapPreferences(
            showPOIs = true, showRoadNumbers = false, showStreetNames = true, enable3DView = false)
    assertEquals(prefs1, prefs2)
  }

  @Test
  fun mapPreferences_toString_containsAllFields() {
    val prefs =
        MapPreferences(
            showPOIs = true, showRoadNumbers = false, showStreetNames = true, enable3DView = false)
    val string = prefs.toString()
    assert(string.contains("showPOIs"))
    assert(string.contains("showRoadNumbers"))
    assert(string.contains("showStreetNames"))
    assert(string.contains("enable3DView"))
  }

  @Test
  fun viewModelCreation_withMockedAuth_succeeds() {
    val newViewModel = SettingsViewModel(mockPreferencesRepository, mockAuth, mockFirestore)
    assertEquals(MapPreferences(), newViewModel.mapPreferences.value)
  }

  // ===== Theme Mode Tests =====

  @Test
  fun themeMode_initialValueIsSystem() {
    assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)
  }

  // Note: Theme mode state change tests moved to SettingsViewModelIntegrationTest

  @Test
  fun updateThemeMode_callsPreferencesRepository() {
    viewModel.updateThemeMode(ThemeMode.DARK)
    coVerify { mockPreferencesRepository.setThemeMode("dark") }
  }

  @Test
  fun updateThemeMode_multiple_eachCallsSave() {
    viewModel.updateThemeMode(ThemeMode.LIGHT)
    viewModel.updateThemeMode(ThemeMode.DARK)
    viewModel.updateThemeMode(ThemeMode.SYSTEM)

    coVerify { mockPreferencesRepository.setThemeMode("light") }
    coVerify { mockPreferencesRepository.setThemeMode("dark") }
    coVerify { mockPreferencesRepository.setThemeMode("system") }
  }

  // ===== ThemeMode Enum Tests =====

  @Test
  fun themeMode_fromString_light() {
    assertEquals(ThemeMode.LIGHT, ThemeMode.fromString("light"))
    assertEquals(ThemeMode.LIGHT, ThemeMode.fromString("Light"))
    assertEquals(ThemeMode.LIGHT, ThemeMode.fromString("LIGHT"))
  }

  @Test
  fun themeMode_fromString_dark() {
    assertEquals(ThemeMode.DARK, ThemeMode.fromString("dark"))
    assertEquals(ThemeMode.DARK, ThemeMode.fromString("Dark"))
    assertEquals(ThemeMode.DARK, ThemeMode.fromString("DARK"))
  }

  @Test
  fun themeMode_fromString_system() {
    assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("system"))
    assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("System"))
    assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("anything"))
  }

  @Test
  fun themeMode_toDisplayString() {
    assertEquals("Light", ThemeMode.LIGHT.toDisplayString())
    assertEquals("Dark", ThemeMode.DARK.toDisplayString())
    assertEquals("System", ThemeMode.SYSTEM.toDisplayString())
  }

  @Test
  fun themeMode_toStorageString() {
    assertEquals("light", ThemeMode.LIGHT.toStorageString())
    assertEquals("dark", ThemeMode.DARK.toStorageString())
    assertEquals("system", ThemeMode.SYSTEM.toStorageString())
  }

  // ===== PreferencesRepository Integration Tests =====

  @Test
  fun updateShowPOIs_callsPreferencesRepository() {
    viewModel.updateShowPOIs(false)
    coVerify { mockPreferencesRepository.setShowPOIs(false) }
  }

  @Test
  fun updateShowRoadNumbers_callsPreferencesRepository() {
    viewModel.updateShowRoadNumbers(false)
    coVerify { mockPreferencesRepository.setShowRoadNumbers(false) }
  }

  @Test
  fun updateShowStreetNames_callsPreferencesRepository() {
    viewModel.updateShowStreetNames(false)
    coVerify { mockPreferencesRepository.setShowStreetNames(false) }
  }

  @Test
  fun updateEnable3DView_callsPreferencesRepository() {
    viewModel.updateEnable3DView(true)
    coVerify { mockPreferencesRepository.setEnable3DView(true) }
  }

  // ===== Error Handling Tests =====

  @Test
  fun updateThemeMode_whenRepositoryThrows_handlesError() {
    coEvery { mockPreferencesRepository.setThemeMode(any()) } throws Exception("Test error")

    viewModel.updateThemeMode(ThemeMode.DARK)
    testDispatcher.scheduler.advanceUntilIdle()

    // State should remain unchanged when error occurs (flow not updated)
    assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)
    // Error message should be set
    assertEquals("Failed to update theme: Test error", viewModel.errorMessage.value)
  }

  @Test
  fun updateShowPOIs_whenRepositoryThrows_handlesError() {
    coEvery { mockPreferencesRepository.setShowPOIs(any()) } throws Exception("Test error")

    viewModel.updateShowPOIs(false)
    testDispatcher.scheduler.advanceUntilIdle()

    // State should remain unchanged when error occurs (flow not updated)
    assertEquals(true, viewModel.mapPreferences.value.showPOIs)
    assertEquals("Failed to update POI setting: Test error", viewModel.errorMessage.value)
  }

  @Test
  fun updateShowRoadNumbers_whenRepositoryThrows_handlesError() {
    coEvery { mockPreferencesRepository.setShowRoadNumbers(any()) } throws Exception("Test error")

    viewModel.updateShowRoadNumbers(false)
    testDispatcher.scheduler.advanceUntilIdle()

    // State should remain unchanged when error occurs (flow not updated)
    assertEquals(true, viewModel.mapPreferences.value.showRoadNumbers)
    assertEquals("Failed to update road numbers setting: Test error", viewModel.errorMessage.value)
  }

  @Test
  fun updateShowStreetNames_whenRepositoryThrows_handlesError() {
    coEvery { mockPreferencesRepository.setShowStreetNames(any()) } throws Exception("Test error")

    viewModel.updateShowStreetNames(false)
    testDispatcher.scheduler.advanceUntilIdle()

    // State should remain unchanged when error occurs (flow not updated)
    assertEquals(true, viewModel.mapPreferences.value.showStreetNames)
    assertEquals("Failed to update street names setting: Test error", viewModel.errorMessage.value)
  }

  @Test
  fun updateEnable3DView_whenRepositoryThrows_handlesError() {
    coEvery { mockPreferencesRepository.setEnable3DView(any()) } throws Exception("Test error")

    viewModel.updateEnable3DView(true)
    testDispatcher.scheduler.advanceUntilIdle()

    // State should remain unchanged when error occurs (flow not updated)
    assertEquals(false, viewModel.mapPreferences.value.enable3DView)
    assertEquals("Failed to update 3D view setting: Test error", viewModel.errorMessage.value)
  }

  // ===== Loading State from Flows Tests =====
  // Note: These tests are in SettingsViewModelIntegrationTest

  // ===== Combined State Tests =====
  // Note: These tests are in SettingsViewModelIntegrationTest

  @Test
  fun mapPreferences_hashCode_consistentWithEquals() {
    val prefs1 =
        MapPreferences(
            showPOIs = true, showRoadNumbers = false, showStreetNames = true, enable3DView = false)
    val prefs2 =
        MapPreferences(
            showPOIs = true, showRoadNumbers = false, showStreetNames = true, enable3DView = false)
    assertEquals(prefs1.hashCode(), prefs2.hashCode())
  }

  // ===== Error Message Management Tests =====

  @Test
  fun clearErrorMessage_resetsErrorToNull() {
    // Trigger an error
    coEvery { mockPreferencesRepository.setShowPOIs(any()) } throws Exception("Test error")
    viewModel.updateShowPOIs(false)

    // Verify error is set
    assertEquals("Failed to update POI setting: Test error", viewModel.errorMessage.value)

    // Clear error
    viewModel.clearErrorMessage()

    // Verify error is cleared
    assertEquals(null, viewModel.errorMessage.value)
  }

  @Test
  fun clearErrorMessage_whenNoError_remainsNull() {
    assertEquals(null, viewModel.errorMessage.value)
    viewModel.clearErrorMessage()
    assertEquals(null, viewModel.errorMessage.value)
  }
}
