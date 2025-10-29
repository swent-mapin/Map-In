package com.swent.mapin.ui.settings

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
    // Set the main dispatcher for coroutines
    Dispatchers.setMain(testDispatcher)

    // Mock Firebase dependencies
    mockAuth = mockk(relaxed = true)
    mockFirestore = mockk(relaxed = true)

    // Mock FirebaseAuth.getInstance()
    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockAuth

    // Mock auth.currentUser to return null (so loadMapPreferences doesn't try to access Firestore)
    every { mockAuth.currentUser } returns null

    // Mock FirebaseFirestore.getInstance()
    mockkStatic(FirebaseFirestore::class)
    every { FirebaseFirestore.getInstance() } returns mockFirestore

    // Create ViewModel with mocked dependencies
    viewModel = SettingsViewModel(mockAuth, mockFirestore)
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
}
