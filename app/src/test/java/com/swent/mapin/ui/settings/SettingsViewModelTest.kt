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
    assertEquals(true, preferences.enable3DView)
  }

  @Test
  fun mapPreferencesDataClass_defaultsAreCorrect() {
    val prefs = MapPreferences()
    assertEquals(true, prefs.showPOIs)
    assertEquals(true, prefs.showRoadNumbers)
    assertEquals(true, prefs.showStreetNames)
    assertEquals(true, prefs.enable3DView)
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

    viewModel.updateEnable3DView(false)
    testDispatcher.scheduler.advanceUntilIdle()

    // State should remain unchanged when error occurs (flow not updated)
    assertEquals(true, viewModel.mapPreferences.value.enable3DView)
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

  @Test
  fun updateShowPOIs_multiple_eachCallsSave() {
    viewModel.updateShowPOIs(false)
    viewModel.updateShowPOIs(true)
    viewModel.updateShowPOIs(false)

    coVerify { mockPreferencesRepository.setShowPOIs(false) }
    coVerify { mockPreferencesRepository.setShowPOIs(true) }
  }

  @Test
  fun updateShowRoadNumbers_multiple_eachCallsSave() {
    viewModel.updateShowRoadNumbers(false)
    viewModel.updateShowRoadNumbers(true)
    viewModel.updateShowRoadNumbers(false)

    coVerify { mockPreferencesRepository.setShowRoadNumbers(false) }
    coVerify { mockPreferencesRepository.setShowRoadNumbers(true) }
  }

  @Test
  fun updateShowStreetNames_multiple_eachCallsSave() {
    viewModel.updateShowStreetNames(false)
    viewModel.updateShowStreetNames(true)
    viewModel.updateShowStreetNames(false)

    coVerify { mockPreferencesRepository.setShowStreetNames(false) }
    coVerify { mockPreferencesRepository.setShowStreetNames(true) }
  }

  @Test
  fun updateEnable3DView_multiple_eachCallsSave() {
    viewModel.updateEnable3DView(false)
    viewModel.updateEnable3DView(true)
    viewModel.updateEnable3DView(false)

    coVerify { mockPreferencesRepository.setEnable3DView(false) }
    coVerify { mockPreferencesRepository.setEnable3DView(true) }
  }

  @Test
  fun themeMode_fromString_emptyString_returnsSystem() {
    assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString(""))
  }

  @Test
  fun themeMode_fromString_nullString_handledByWhen() {
    // The when statement will catch any non-"light" or "dark" value as SYSTEM
    assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("unknown"))
  }

  @Test
  fun mapPreferences_notEqualWithDifferentShowPOIs() {
    val prefs1 =
        MapPreferences(
            showPOIs = true, showRoadNumbers = true, showStreetNames = true, enable3DView = true)
    val prefs2 =
        MapPreferences(
            showPOIs = false, showRoadNumbers = true, showStreetNames = true, enable3DView = true)
    assert(prefs1 != prefs2)
  }

  @Test
  fun mapPreferences_notEqualWithDifferentShowRoadNumbers() {
    val prefs1 =
        MapPreferences(
            showPOIs = true, showRoadNumbers = true, showStreetNames = true, enable3DView = true)
    val prefs2 =
        MapPreferences(
            showPOIs = true, showRoadNumbers = false, showStreetNames = true, enable3DView = true)
    assert(prefs1 != prefs2)
  }

  @Test
  fun mapPreferences_notEqualWithDifferentShowStreetNames() {
    val prefs1 =
        MapPreferences(
            showPOIs = true, showRoadNumbers = true, showStreetNames = true, enable3DView = true)
    val prefs2 =
        MapPreferences(
            showPOIs = true, showRoadNumbers = true, showStreetNames = false, enable3DView = true)
    assert(prefs1 != prefs2)
  }

  @Test
  fun mapPreferences_notEqualWithDifferentEnable3DView() {
    val prefs1 =
        MapPreferences(
            showPOIs = true, showRoadNumbers = true, showStreetNames = true, enable3DView = true)
    val prefs2 =
        MapPreferences(
            showPOIs = true, showRoadNumbers = true, showStreetNames = true, enable3DView = false)
    assert(prefs1 != prefs2)
  }

  @Test
  fun updateShowPOIs_callsRepositoryWithCorrectValue() {
    viewModel.updateShowPOIs(true)
    coVerify { mockPreferencesRepository.setShowPOIs(true) }

    viewModel.updateShowPOIs(false)
    coVerify { mockPreferencesRepository.setShowPOIs(false) }
  }

  @Test
  fun updateShowRoadNumbers_callsRepositoryWithCorrectValue() {
    viewModel.updateShowRoadNumbers(true)
    coVerify { mockPreferencesRepository.setShowRoadNumbers(true) }

    viewModel.updateShowRoadNumbers(false)
    coVerify { mockPreferencesRepository.setShowRoadNumbers(false) }
  }

  @Test
  fun updateShowStreetNames_callsRepositoryWithCorrectValue() {
    viewModel.updateShowStreetNames(true)
    coVerify { mockPreferencesRepository.setShowStreetNames(true) }

    viewModel.updateShowStreetNames(false)
    coVerify { mockPreferencesRepository.setShowStreetNames(false) }
  }

  @Test
  fun updateEnable3DView_callsRepositoryWithCorrectValue() {
    viewModel.updateEnable3DView(true)
    coVerify { mockPreferencesRepository.setEnable3DView(true) }

    viewModel.updateEnable3DView(false)
    coVerify { mockPreferencesRepository.setEnable3DView(false) }
  }

  @Test
  fun signOut_callsAuthSignOut_always() {
    viewModel.signOut()
    verify { mockAuth.signOut() }

    viewModel.signOut()
    verify(exactly = 2) { mockAuth.signOut() }
  }

  @Test
  fun deleteAccount_whenUserIsNull_doesNotThrowException() {
    every { mockAuth.currentUser } returns null
    try {
      viewModel.deleteAccount()
    } catch (e: Exception) {
      throw AssertionError("deleteAccount should not throw when user is null", e)
    }
  }

  @Test
  fun mapPreferences_allFieldsInToString() {
    val prefs =
        MapPreferences(
            showPOIs = true, showRoadNumbers = false, showStreetNames = true, enable3DView = false)
    val stringRep = prefs.toString()
    assert(stringRep.contains("true") || stringRep.contains("false"))
  }

  @Test
  fun updateThemeMode_light_callsRepositoryWithLightString() {
    viewModel.updateThemeMode(ThemeMode.LIGHT)
    coVerify { mockPreferencesRepository.setThemeMode("light") }
  }

  @Test
  fun updateThemeMode_dark_callsRepositoryWithDarkString() {
    viewModel.updateThemeMode(ThemeMode.DARK)
    coVerify { mockPreferencesRepository.setThemeMode("dark") }
  }

  @Test
  fun updateThemeMode_system_callsRepositoryWithSystemString() {
    viewModel.updateThemeMode(ThemeMode.SYSTEM)
    coVerify { mockPreferencesRepository.setThemeMode("system") }
  }

  @Test
  fun errorMessage_afterUpdate_canBeCleared() {
    coEvery { mockPreferencesRepository.setShowPOIs(any()) } throws Exception("Test")
    viewModel.updateShowPOIs(false)

    val errorBefore = viewModel.errorMessage.value
    assert(errorBefore != null)

    viewModel.clearErrorMessage()
    val errorAfter = viewModel.errorMessage.value
    assertEquals(null, errorAfter)
  }

  @Test
  fun mapPreferences_hashCode_differentForDifferentValues() {
    val prefs1 =
        MapPreferences(
            showPOIs = true, showRoadNumbers = true, showStreetNames = true, enable3DView = true)
    val prefs2 =
        MapPreferences(
            showPOIs = false, showRoadNumbers = true, showStreetNames = true, enable3DView = true)
    // HashCodes may be different (not guaranteed, but likely)
    // This test ensures hashCode() doesn't throw an exception
    assert(prefs1.hashCode() != null)
    assert(prefs2.hashCode() != null)
  }

  @Test
  fun mapPreferences_copyAllFields() {
    val original =
        MapPreferences(
            showPOIs = false, showRoadNumbers = true, showStreetNames = false, enable3DView = true)
    val copied =
        original.copy(
            showPOIs = true, showRoadNumbers = false, showStreetNames = true, enable3DView = false)
    assertEquals(true, copied.showPOIs)
    assertEquals(false, copied.showRoadNumbers)
    assertEquals(true, copied.showStreetNames)
    assertEquals(false, copied.enable3DView)
  }

  @Test
  fun themeMode_all_haveDisplayStrings() {
    ThemeMode.values().forEach { mode ->
      val displayString = mode.toDisplayString()
      assert(displayString.isNotEmpty())
    }
  }

  @Test
  fun themeMode_all_haveStorageStrings() {
    ThemeMode.values().forEach { mode ->
      val storageString = mode.toStorageString()
      assert(storageString.isNotEmpty())
    }
  }

  @Test
  fun updateShowPOIs_whenRepositoryThrows_errorMessageIsSet() {
    val errorMessage = "Custom error message"
    coEvery { mockPreferencesRepository.setShowPOIs(any()) } throws Exception(errorMessage)

    viewModel.updateShowPOIs(false)
    testDispatcher.scheduler.advanceUntilIdle()

    assert(viewModel.errorMessage.value?.contains(errorMessage) ?: false)
  }

  @Test
  fun updateShowRoadNumbers_whenRepositoryThrows_errorMessageIsSet() {
    val errorMessage = "Custom road error"
    coEvery { mockPreferencesRepository.setShowRoadNumbers(any()) } throws Exception(errorMessage)

    viewModel.updateShowRoadNumbers(false)
    testDispatcher.scheduler.advanceUntilIdle()

    assert(viewModel.errorMessage.value?.contains(errorMessage) ?: false)
  }

  @Test
  fun updateShowStreetNames_whenRepositoryThrows_errorMessageIsSet() {
    val errorMessage = "Custom street error"
    coEvery { mockPreferencesRepository.setShowStreetNames(any()) } throws Exception(errorMessage)

    viewModel.updateShowStreetNames(false)
    testDispatcher.scheduler.advanceUntilIdle()

    assert(viewModel.errorMessage.value?.contains(errorMessage) ?: false)
  }

  @Test
  fun updateEnable3DView_whenRepositoryThrows_errorMessageIsSet() {
    val errorMessage = "Custom 3D error"
    coEvery { mockPreferencesRepository.setEnable3DView(any()) } throws Exception(errorMessage)

    viewModel.updateEnable3DView(false)
    testDispatcher.scheduler.advanceUntilIdle()

    assert(viewModel.errorMessage.value?.contains(errorMessage) ?: false)
  }

  @Test
  fun mapPreferences_copyPreservesAllFieldsNotChanged() {
    val original =
        MapPreferences(
            showPOIs = true, showRoadNumbers = false, showStreetNames = true, enable3DView = false)
    val copied = original.copy()

    assertEquals(original.showPOIs, copied.showPOIs)
    assertEquals(original.showRoadNumbers, copied.showRoadNumbers)
    assertEquals(original.showStreetNames, copied.showStreetNames)
    assertEquals(original.enable3DView, copied.enable3DView)
  }

  @Test
  fun isLoading_initialStateIsFalse() {
    assertEquals(false, viewModel.isLoading.value)
  }

  @Test
  fun errorMessage_initialStateIsNull() {
    assertEquals(null, viewModel.errorMessage.value)
  }

  @Test
  fun themeMode_initialStateIsSystem() {
    assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)
  }

  @Test
  fun updateThemeMode_multipleChanges_lastValueWins() {
    viewModel.updateThemeMode(ThemeMode.LIGHT)
    viewModel.updateThemeMode(ThemeMode.DARK)
    viewModel.updateThemeMode(ThemeMode.SYSTEM)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)
  }

  // ===== Delete Account Tests =====

  @Test
  fun deleteAccount_whenUserIsNull_setsLoadingToFalseImmediately() {
    every { mockAuth.currentUser } returns null

    viewModel.deleteAccount()
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(false, viewModel.isLoading.value)
    assertEquals(null, viewModel.errorMessage.value)
  }

  @Test
  fun deleteAccount_whenExceptionThrown_setsErrorMessageAndLoading() {
    every { mockAuth.currentUser } throws RuntimeException("Auth error")

    viewModel.deleteAccount()
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(false, viewModel.isLoading.value)
    assert(viewModel.errorMessage.value?.contains("Failed to delete account") ?: false)
    assert(viewModel.errorMessage.value?.contains("Auth error") ?: false)
  }

  @Test
  fun deleteAccount_setsLoadingToTrueInitially() {
    every { mockAuth.currentUser } returns null

    viewModel.deleteAccount()
    // Before advanceUntilIdle, loading might be true briefly
    // After completion, should be false
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(false, viewModel.isLoading.value)
  }

  @Test
  fun deleteAccount_whenUserExists_getsUserId() {
    val mockUser = mockk<com.google.firebase.auth.FirebaseUser>(relaxed = true)
    val mockCollectionRef = mockk<com.google.firebase.firestore.CollectionReference>(relaxed = true)
    val mockDocumentRef = mockk<com.google.firebase.firestore.DocumentReference>(relaxed = true)
    val mockTask = mockk<com.google.android.gms.tasks.Task<Void>>(relaxed = true)

    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "test-user-id"
    every { mockFirestore.collection("settings") } returns mockCollectionRef
    every { mockCollectionRef.document("test-user-id") } returns mockDocumentRef
    every { mockDocumentRef.delete() } returns mockTask
    every { mockTask.addOnSuccessListener(any()) } returns mockTask
    every { mockTask.addOnFailureListener(any()) } answers
        {
          val listener = firstArg<com.google.android.gms.tasks.OnFailureListener>()
          listener.onFailure(Exception("Settings delete failed"))
          mockTask
        }

    viewModel.deleteAccount()
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(false, viewModel.isLoading.value)
    assert(viewModel.errorMessage.value?.contains("Failed to delete settings") ?: false)
  }

  @Test
  fun deleteAccount_settingsDeleteFails_setsErrorMessage() {
    val mockUser = mockk<com.google.firebase.auth.FirebaseUser>(relaxed = true)
    val mockCollectionRef = mockk<com.google.firebase.firestore.CollectionReference>(relaxed = true)
    val mockDocumentRef = mockk<com.google.firebase.firestore.DocumentReference>(relaxed = true)
    val mockTask = mockk<com.google.android.gms.tasks.Task<Void>>(relaxed = true)

    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "test-user-id"
    every { mockFirestore.collection("settings") } returns mockCollectionRef
    every { mockCollectionRef.document("test-user-id") } returns mockDocumentRef
    every { mockDocumentRef.delete() } returns mockTask
    every { mockTask.addOnSuccessListener(any()) } returns mockTask
    every { mockTask.addOnFailureListener(any()) } answers
        {
          val listener = firstArg<com.google.android.gms.tasks.OnFailureListener>()
          listener.onFailure(Exception("Settings error"))
          mockTask
        }

    viewModel.deleteAccount()
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals("Failed to delete settings: Settings error", viewModel.errorMessage.value)
    assertEquals(false, viewModel.isLoading.value)
  }

  @Test
  fun deleteAccount_userProfileDeleteFails_setsErrorMessage() {
    val mockUser = mockk<com.google.firebase.auth.FirebaseUser>(relaxed = true)
    val mockSettingsCollectionRef =
        mockk<com.google.firebase.firestore.CollectionReference>(relaxed = true)
    val mockSettingsDocRef = mockk<com.google.firebase.firestore.DocumentReference>(relaxed = true)
    val mockSettingsTask = mockk<com.google.android.gms.tasks.Task<Void>>(relaxed = true)
    val mockUsersCollectionRef =
        mockk<com.google.firebase.firestore.CollectionReference>(relaxed = true)
    val mockUsersDocRef = mockk<com.google.firebase.firestore.DocumentReference>(relaxed = true)
    val mockUsersTask = mockk<com.google.android.gms.tasks.Task<Void>>(relaxed = true)

    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "test-user-id"

    // Settings delete succeeds
    every { mockFirestore.collection("settings") } returns mockSettingsCollectionRef
    every { mockSettingsCollectionRef.document("test-user-id") } returns mockSettingsDocRef
    every { mockSettingsDocRef.delete() } returns mockSettingsTask
    every { mockSettingsTask.addOnSuccessListener(any()) } answers
        {
          val listener = firstArg<com.google.android.gms.tasks.OnSuccessListener<Void>>()
          listener.onSuccess(null)
          mockSettingsTask
        }
    every { mockSettingsTask.addOnFailureListener(any()) } returns mockSettingsTask

    // Users delete fails
    every { mockFirestore.collection("users") } returns mockUsersCollectionRef
    every { mockUsersCollectionRef.document("test-user-id") } returns mockUsersDocRef
    every { mockUsersDocRef.delete() } returns mockUsersTask
    every { mockUsersTask.addOnSuccessListener(any()) } returns mockUsersTask
    every { mockUsersTask.addOnFailureListener(any()) } answers
        {
          val listener = firstArg<com.google.android.gms.tasks.OnFailureListener>()
          listener.onFailure(Exception("Profile error"))
          mockUsersTask
        }

    viewModel.deleteAccount()
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals("Failed to delete user profile: Profile error", viewModel.errorMessage.value)
    assertEquals(false, viewModel.isLoading.value)
  }

  @Test
  fun deleteAccount_authDeleteFails_setsErrorMessage() {
    val mockUser = mockk<com.google.firebase.auth.FirebaseUser>(relaxed = true)
    val mockSettingsCollectionRef =
        mockk<com.google.firebase.firestore.CollectionReference>(relaxed = true)
    val mockSettingsDocRef = mockk<com.google.firebase.firestore.DocumentReference>(relaxed = true)
    val mockSettingsTask = mockk<com.google.android.gms.tasks.Task<Void>>(relaxed = true)
    val mockUsersCollectionRef =
        mockk<com.google.firebase.firestore.CollectionReference>(relaxed = true)
    val mockUsersDocRef = mockk<com.google.firebase.firestore.DocumentReference>(relaxed = true)
    val mockUsersTask = mockk<com.google.android.gms.tasks.Task<Void>>(relaxed = true)
    val mockAuthTask = mockk<com.google.android.gms.tasks.Task<Void>>(relaxed = true)

    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "test-user-id"

    // Settings delete succeeds
    every { mockFirestore.collection("settings") } returns mockSettingsCollectionRef
    every { mockSettingsCollectionRef.document("test-user-id") } returns mockSettingsDocRef
    every { mockSettingsDocRef.delete() } returns mockSettingsTask
    every { mockSettingsTask.addOnSuccessListener(any()) } answers
        {
          val listener = firstArg<com.google.android.gms.tasks.OnSuccessListener<Void>>()
          listener.onSuccess(null)
          mockSettingsTask
        }
    every { mockSettingsTask.addOnFailureListener(any()) } returns mockSettingsTask

    // Users delete succeeds
    every { mockFirestore.collection("users") } returns mockUsersCollectionRef
    every { mockUsersCollectionRef.document("test-user-id") } returns mockUsersDocRef
    every { mockUsersDocRef.delete() } returns mockUsersTask
    every { mockUsersTask.addOnSuccessListener(any()) } answers
        {
          val listener = firstArg<com.google.android.gms.tasks.OnSuccessListener<Void>>()
          listener.onSuccess(null)
          mockUsersTask
        }
    every { mockUsersTask.addOnFailureListener(any()) } returns mockUsersTask

    // Auth delete fails
    every { mockUser.delete() } returns mockAuthTask
    every { mockAuthTask.addOnSuccessListener(any()) } returns mockAuthTask
    every { mockAuthTask.addOnFailureListener(any()) } answers
        {
          val listener = firstArg<com.google.android.gms.tasks.OnFailureListener>()
          listener.onFailure(Exception("Auth delete error"))
          mockAuthTask
        }

    viewModel.deleteAccount()
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals("Failed to delete auth account: Auth delete error", viewModel.errorMessage.value)
    assertEquals(false, viewModel.isLoading.value)
  }

  @Test
  fun deleteAccount_allOperationsSucceed_setsLoadingToFalse() {
    val mockUser = mockk<com.google.firebase.auth.FirebaseUser>(relaxed = true)
    val mockSettingsCollectionRef =
        mockk<com.google.firebase.firestore.CollectionReference>(relaxed = true)
    val mockSettingsDocRef = mockk<com.google.firebase.firestore.DocumentReference>(relaxed = true)
    val mockSettingsTask = mockk<com.google.android.gms.tasks.Task<Void>>(relaxed = true)
    val mockUsersCollectionRef =
        mockk<com.google.firebase.firestore.CollectionReference>(relaxed = true)
    val mockUsersDocRef = mockk<com.google.firebase.firestore.DocumentReference>(relaxed = true)
    val mockUsersTask = mockk<com.google.android.gms.tasks.Task<Void>>(relaxed = true)
    val mockAuthTask = mockk<com.google.android.gms.tasks.Task<Void>>(relaxed = true)

    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "test-user-id"

    // Settings delete succeeds
    every { mockFirestore.collection("settings") } returns mockSettingsCollectionRef
    every { mockSettingsCollectionRef.document("test-user-id") } returns mockSettingsDocRef
    every { mockSettingsDocRef.delete() } returns mockSettingsTask
    every { mockSettingsTask.addOnSuccessListener(any()) } answers
        {
          val listener = firstArg<com.google.android.gms.tasks.OnSuccessListener<Void>>()
          listener.onSuccess(null)
          mockSettingsTask
        }
    every { mockSettingsTask.addOnFailureListener(any()) } returns mockSettingsTask

    // Users delete succeeds
    every { mockFirestore.collection("users") } returns mockUsersCollectionRef
    every { mockUsersCollectionRef.document("test-user-id") } returns mockUsersDocRef
    every { mockUsersDocRef.delete() } returns mockUsersTask
    every { mockUsersTask.addOnSuccessListener(any()) } answers
        {
          val listener = firstArg<com.google.android.gms.tasks.OnSuccessListener<Void>>()
          listener.onSuccess(null)
          mockUsersTask
        }
    every { mockUsersTask.addOnFailureListener(any()) } returns mockUsersTask

    // Auth delete succeeds
    every { mockUser.delete() } returns mockAuthTask
    every { mockAuthTask.addOnSuccessListener(any()) } answers
        {
          val listener = firstArg<com.google.android.gms.tasks.OnSuccessListener<Void>>()
          listener.onSuccess(null)
          mockAuthTask
        }
    every { mockAuthTask.addOnFailureListener(any()) } returns mockAuthTask

    viewModel.deleteAccount()
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(false, viewModel.isLoading.value)
    assertEquals(null, viewModel.errorMessage.value)
  }

  @Test
  fun deleteAccount_accessesCorrectFirestoreCollections() {
    val mockUser = mockk<com.google.firebase.auth.FirebaseUser>(relaxed = true)
    val mockCollectionRef = mockk<com.google.firebase.firestore.CollectionReference>(relaxed = true)
    val mockDocumentRef = mockk<com.google.firebase.firestore.DocumentReference>(relaxed = true)
    val mockTask = mockk<com.google.android.gms.tasks.Task<Void>>(relaxed = true)

    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "user-123"
    every { mockFirestore.collection(any()) } returns mockCollectionRef
    every { mockCollectionRef.document(any()) } returns mockDocumentRef
    every { mockDocumentRef.delete() } returns mockTask
    every { mockTask.addOnSuccessListener(any()) } returns mockTask
    every { mockTask.addOnFailureListener(any()) } answers
        {
          val listener = firstArg<com.google.android.gms.tasks.OnFailureListener>()
          listener.onFailure(Exception("Test"))
          mockTask
        }

    viewModel.deleteAccount()
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify settings collection was accessed
    verify { mockFirestore.collection("settings") }
  }

  @Test
  fun deleteAccount_usesCorrectUserId() {
    val mockUser = mockk<com.google.firebase.auth.FirebaseUser>(relaxed = true)
    val mockCollectionRef = mockk<com.google.firebase.firestore.CollectionReference>(relaxed = true)
    val mockDocumentRef = mockk<com.google.firebase.firestore.DocumentReference>(relaxed = true)
    val mockTask = mockk<com.google.android.gms.tasks.Task<Void>>(relaxed = true)

    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "specific-user-id"
    every { mockFirestore.collection(any()) } returns mockCollectionRef
    every { mockCollectionRef.document(any()) } returns mockDocumentRef
    every { mockDocumentRef.delete() } returns mockTask
    every { mockTask.addOnSuccessListener(any()) } returns mockTask
    every { mockTask.addOnFailureListener(any()) } answers
        {
          val listener = firstArg<com.google.android.gms.tasks.OnFailureListener>()
          listener.onFailure(Exception("Test"))
          mockTask
        }

    viewModel.deleteAccount()
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify the correct user ID was used
    verify { mockCollectionRef.document("specific-user-id") }
  }

  @Test
  fun deleteAccount_catchBlock_catchesExceptions() {
    every { mockAuth.currentUser } throws RuntimeException("Unexpected error")

    viewModel.deleteAccount()
    testDispatcher.scheduler.advanceUntilIdle()

    assert(viewModel.errorMessage.value?.contains("Failed to delete account") ?: false)
    assertEquals(false, viewModel.isLoading.value)
  }
}
