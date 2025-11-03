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
import kotlinx.coroutines.flow.flowOf
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

  @Before
  fun setup() {
    // Set the main dispatcher for coroutines
    Dispatchers.setMain(testDispatcher)

    // Mock dependencies
    mockAuth = mockk(relaxed = true)
    mockFirestore = mockk(relaxed = true)
    mockPreferencesRepository = mockk(relaxed = true)

    // Mock auth.currentUser to return null
    every { mockAuth.currentUser } returns null

    // Mock PreferencesRepository flows with default values
    every { mockPreferencesRepository.themeModeFlow } returns flowOf("system")
    every { mockPreferencesRepository.showPOIsFlow } returns flowOf(true)
    every { mockPreferencesRepository.showRoadNumbersFlow } returns flowOf(true)
    every { mockPreferencesRepository.showStreetNamesFlow } returns flowOf(true)
    every { mockPreferencesRepository.enable3DViewFlow } returns flowOf(false)

    // Mock suspend functions
    coEvery { mockPreferencesRepository.setThemeMode(any()) } returns Unit
    coEvery { mockPreferencesRepository.setShowPOIs(any()) } returns Unit
    coEvery { mockPreferencesRepository.setShowRoadNumbers(any()) } returns Unit
    coEvery { mockPreferencesRepository.setShowStreetNames(any()) } returns Unit
    coEvery { mockPreferencesRepository.setEnable3DView(any()) } returns Unit

    // Create ViewModel with mocked dependencies
    viewModel = SettingsViewModel(mockPreferencesRepository, mockAuth, mockFirestore)
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

  @Test
  fun updateShowPOIs_changesState() {
    viewModel.updateShowPOIs(false)
    val preferences = viewModel.mapPreferences.value
    assertEquals(false, preferences.showPOIs)
    // Other settings should remain unchanged
    assertEquals(true, preferences.showRoadNumbers)
    assertEquals(true, preferences.showStreetNames)
    assertEquals(false, preferences.enable3DView)
  }

  @Test
  fun updateShowRoadNumbers_changesState() {
    viewModel.updateShowRoadNumbers(false)
    val preferences = viewModel.mapPreferences.value
    assertEquals(false, preferences.showRoadNumbers)
    // Other settings should remain unchanged
    assertEquals(true, preferences.showPOIs)
    assertEquals(true, preferences.showStreetNames)
    assertEquals(false, preferences.enable3DView)
  }

  @Test
  fun updateShowStreetNames_changesState() {
    viewModel.updateShowStreetNames(false)
    val preferences = viewModel.mapPreferences.value
    assertEquals(false, preferences.showStreetNames)
    // Other settings should remain unchanged
    assertEquals(true, preferences.showPOIs)
    assertEquals(true, preferences.showRoadNumbers)
    assertEquals(false, preferences.enable3DView)
  }

  @Test
  fun updateEnable3DView_changesState() {
    viewModel.updateEnable3DView(true)
    val preferences = viewModel.mapPreferences.value
    assertEquals(true, preferences.enable3DView)
    // Other settings should remain unchanged
    assertEquals(true, preferences.showPOIs)
    assertEquals(true, preferences.showRoadNumbers)
    assertEquals(true, preferences.showStreetNames)
  }

  @Test
  fun multipleUpdates_worksCorrectly() {
    viewModel.updateShowPOIs(false)
    viewModel.updateEnable3DView(true)
    viewModel.updateShowRoadNumbers(false)

    val preferences = viewModel.mapPreferences.value
    assertEquals(false, preferences.showPOIs)
    assertEquals(false, preferences.showRoadNumbers)
    assertEquals(true, preferences.showStreetNames)
    assertEquals(true, preferences.enable3DView)
  }

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

  @Test
  fun updateShowPOIs_toTrue_changesState() {
    viewModel.updateShowPOIs(false)
    viewModel.updateShowPOIs(true)
    val preferences = viewModel.mapPreferences.value
    assertEquals(true, preferences.showPOIs)
  }

  @Test
  fun updateShowRoadNumbers_toTrue_changesState() {
    viewModel.updateShowRoadNumbers(false)
    viewModel.updateShowRoadNumbers(true)
    val preferences = viewModel.mapPreferences.value
    assertEquals(true, preferences.showRoadNumbers)
  }

  @Test
  fun updateShowStreetNames_toTrue_changesState() {
    viewModel.updateShowStreetNames(false)
    viewModel.updateShowStreetNames(true)
    val preferences = viewModel.mapPreferences.value
    assertEquals(true, preferences.showStreetNames)
  }

  @Test
  fun updateEnable3DView_toFalse_changesState() {
    viewModel.updateEnable3DView(true)
    viewModel.updateEnable3DView(false)
    val preferences = viewModel.mapPreferences.value
    assertEquals(false, preferences.enable3DView)
  }

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

  @Test
  fun allTogglesOff_allValuesAreFalse() {
    viewModel.updateShowPOIs(false)
    viewModel.updateShowRoadNumbers(false)
    viewModel.updateShowStreetNames(false)
    viewModel.updateEnable3DView(false)

    val preferences = viewModel.mapPreferences.value
    assertEquals(false, preferences.showPOIs)
    assertEquals(false, preferences.showRoadNumbers)
    assertEquals(false, preferences.showStreetNames)
    assertEquals(false, preferences.enable3DView)
  }

  @Test
  fun allTogglesOn_allValuesAreTrue() {
    viewModel.updateShowPOIs(true)
    viewModel.updateShowRoadNumbers(true)
    viewModel.updateShowStreetNames(true)
    viewModel.updateEnable3DView(true)

    val preferences = viewModel.mapPreferences.value
    assertEquals(true, preferences.showPOIs)
    assertEquals(true, preferences.showRoadNumbers)
    assertEquals(true, preferences.showStreetNames)
    assertEquals(true, preferences.enable3DView)
  }

  @Test
  fun rapidToggles_finalStateIsCorrect() {
    // Toggle POIs multiple times
    viewModel.updateShowPOIs(false)
    viewModel.updateShowPOIs(true)
    viewModel.updateShowPOIs(false)
    viewModel.updateShowPOIs(true)

    val preferences = viewModel.mapPreferences.value
    assertEquals(true, preferences.showPOIs)
  }

  @Test
  fun alternatingToggles_maintainsIndependentState() {
    viewModel.updateShowPOIs(false)
    viewModel.updateShowRoadNumbers(true)
    viewModel.updateShowPOIs(true)
    viewModel.updateShowStreetNames(false)
    viewModel.updateEnable3DView(true)
    viewModel.updateShowRoadNumbers(false)

    val preferences = viewModel.mapPreferences.value
    assertEquals(true, preferences.showPOIs)
    assertEquals(false, preferences.showRoadNumbers)
    assertEquals(false, preferences.showStreetNames)
    assertEquals(true, preferences.enable3DView)
  }

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

  @Test
  fun multipleViewModels_haveIndependentState() {
    val viewModel2 = SettingsViewModel(mockPreferencesRepository, mockAuth, mockFirestore)

    viewModel.updateShowPOIs(false)
    viewModel2.updateShowPOIs(true)

    assertEquals(false, viewModel.mapPreferences.value.showPOIs)
    assertEquals(true, viewModel2.mapPreferences.value.showPOIs)
  }

  // ===== Theme Mode Tests =====

  @Test
  fun themeMode_initialValueIsSystem() {
    assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)
  }

  @Test
  fun updateThemeMode_toLight_changesState() {
    viewModel.updateThemeMode(ThemeMode.LIGHT)
    assertEquals(ThemeMode.LIGHT, viewModel.themeMode.value)
  }

  @Test
  fun updateThemeMode_toDark_changesState() {
    viewModel.updateThemeMode(ThemeMode.DARK)
    assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
  }

  @Test
  fun updateThemeMode_toSystem_changesState() {
    viewModel.updateThemeMode(ThemeMode.LIGHT)
    viewModel.updateThemeMode(ThemeMode.SYSTEM)
    assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)
  }

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

    // Should still update state despite error
    assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
    // Error message should be set
    assertEquals("Failed to update theme: Test error", viewModel.errorMessage.value)
  }

  @Test
  fun updateShowPOIs_whenRepositoryThrows_handlesError() {
    coEvery { mockPreferencesRepository.setShowPOIs(any()) } throws Exception("Test error")

    viewModel.updateShowPOIs(false)

    assertEquals(false, viewModel.mapPreferences.value.showPOIs)
    assertEquals("Failed to update POI setting: Test error", viewModel.errorMessage.value)
  }

  @Test
  fun updateShowRoadNumbers_whenRepositoryThrows_handlesError() {
    coEvery { mockPreferencesRepository.setShowRoadNumbers(any()) } throws Exception("Test error")

    viewModel.updateShowRoadNumbers(false)

    assertEquals(false, viewModel.mapPreferences.value.showRoadNumbers)
    assertEquals("Failed to update road numbers setting: Test error", viewModel.errorMessage.value)
  }

  @Test
  fun updateShowStreetNames_whenRepositoryThrows_handlesError() {
    coEvery { mockPreferencesRepository.setShowStreetNames(any()) } throws Exception("Test error")

    viewModel.updateShowStreetNames(false)

    assertEquals(false, viewModel.mapPreferences.value.showStreetNames)
    assertEquals("Failed to update street names setting: Test error", viewModel.errorMessage.value)
  }

  @Test
  fun updateEnable3DView_whenRepositoryThrows_handlesError() {
    coEvery { mockPreferencesRepository.setEnable3DView(any()) } throws Exception("Test error")

    viewModel.updateEnable3DView(true)

    assertEquals(true, viewModel.mapPreferences.value.enable3DView)
    assertEquals("Failed to update 3D view setting: Test error", viewModel.errorMessage.value)
  }

  // ===== Loading State from Flows Tests =====

  @Test
  fun initialization_loadsThemeModeFromFlow() {
    every { mockPreferencesRepository.themeModeFlow } returns flowOf("dark")

    val newViewModel = SettingsViewModel(mockPreferencesRepository, mockAuth, mockFirestore)

    assertEquals(ThemeMode.DARK, newViewModel.themeMode.value)
  }

  @Test
  fun initialization_loadsMapPreferencesFromFlows() {
    every { mockPreferencesRepository.showPOIsFlow } returns flowOf(false)
    every { mockPreferencesRepository.showRoadNumbersFlow } returns flowOf(false)
    every { mockPreferencesRepository.showStreetNamesFlow } returns flowOf(false)
    every { mockPreferencesRepository.enable3DViewFlow } returns flowOf(true)

    val newViewModel = SettingsViewModel(mockPreferencesRepository, mockAuth, mockFirestore)

    // Give flows time to emit
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(false, newViewModel.mapPreferences.value.showPOIs)
    assertEquals(false, newViewModel.mapPreferences.value.showRoadNumbers)
    assertEquals(false, newViewModel.mapPreferences.value.showStreetNames)
    assertEquals(true, newViewModel.mapPreferences.value.enable3DView)
  }

  // ===== Combined State Tests =====

  @Test
  fun themeAndMapPreferences_canBeSetIndependently() {
    viewModel.updateThemeMode(ThemeMode.DARK)
    viewModel.updateShowPOIs(false)
    viewModel.updateEnable3DView(true)

    assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
    assertEquals(false, viewModel.mapPreferences.value.showPOIs)
    assertEquals(true, viewModel.mapPreferences.value.enable3DView)
  }

  @Test
  fun rapidThemeModeChanges_finalStateIsCorrect() {
    viewModel.updateThemeMode(ThemeMode.LIGHT)
    viewModel.updateThemeMode(ThemeMode.DARK)
    viewModel.updateThemeMode(ThemeMode.SYSTEM)
    viewModel.updateThemeMode(ThemeMode.LIGHT)

    assertEquals(ThemeMode.LIGHT, viewModel.themeMode.value)
  }

  @Test
  fun updateSameValueTwice_stateRemainsConsistent() {
    viewModel.updateShowPOIs(false)
    val prefs1 = viewModel.mapPreferences.value
    viewModel.updateShowPOIs(false)
    val prefs2 = viewModel.mapPreferences.value

    assertEquals(prefs1.showPOIs, prefs2.showPOIs)
    assertEquals(false, prefs2.showPOIs)
  }

  @Test
  fun sequentialUpdates_eachUpdateReflectsImmediately() {
    viewModel.updateShowPOIs(false)
    assertEquals(false, viewModel.mapPreferences.value.showPOIs)

    viewModel.updateShowRoadNumbers(false)
    assertEquals(false, viewModel.mapPreferences.value.showRoadNumbers)

    viewModel.updateShowStreetNames(false)
    assertEquals(false, viewModel.mapPreferences.value.showStreetNames)

    viewModel.updateEnable3DView(true)
    assertEquals(true, viewModel.mapPreferences.value.enable3DView)
  }

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
}
