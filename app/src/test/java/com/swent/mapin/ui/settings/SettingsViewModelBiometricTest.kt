package com.swent.mapin.ui.settings

import com.swent.mapin.model.preferences.PreferencesRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelBiometricTest {

  private lateinit var viewModel: SettingsViewModel
  private lateinit var mockPreferencesRepository: PreferencesRepository
  private lateinit var testDispatcher: TestDispatcher
  private val biometricUnlockFlow = MutableStateFlow(false)

  @Before
  fun setUp() {
    testDispatcher = StandardTestDispatcher()
    Dispatchers.setMain(testDispatcher)
    mockPreferencesRepository = mockk(relaxed = true)
    every { mockPreferencesRepository.biometricUnlockFlow } returns biometricUnlockFlow
    every { mockPreferencesRepository.themeModeFlow } returns MutableStateFlow("system")
    every { mockPreferencesRepository.showPOIsFlow } returns MutableStateFlow(true)
    every { mockPreferencesRepository.showRoadNumbersFlow } returns MutableStateFlow(true)
    every { mockPreferencesRepository.showStreetNamesFlow } returns MutableStateFlow(true)
    every { mockPreferencesRepository.enable3DViewFlow } returns MutableStateFlow(true)
    viewModel =
        SettingsViewModel(mockPreferencesRepository, mockk(relaxed = true), mockk(relaxed = true))
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun `biometricUnlockEnabled initial value is false`() =
      runTest(testDispatcher) {
        advanceUntilIdle()
        assertEquals(false, viewModel.biometricUnlockEnabled.value)
      }

  @Test
  fun `updateBiometricUnlockEnabled calls repository with true`() =
      runTest(testDispatcher) {
        coEvery { mockPreferencesRepository.setBiometricUnlockEnabled(true) } just Runs
        viewModel.updateBiometricUnlockEnabled(true)
        advanceUntilIdle()
        coVerify { mockPreferencesRepository.setBiometricUnlockEnabled(true) }
      }

  @Test
  fun `updateBiometricUnlockEnabled calls repository with false`() =
      runTest(testDispatcher) {
        coEvery { mockPreferencesRepository.setBiometricUnlockEnabled(false) } just Runs
        viewModel.updateBiometricUnlockEnabled(false)
        advanceUntilIdle()
        coVerify { mockPreferencesRepository.setBiometricUnlockEnabled(false) }
      }

  @Test
  fun `updateBiometricUnlockEnabled sets error on exception`() =
      runTest(testDispatcher) {
        coEvery { mockPreferencesRepository.setBiometricUnlockEnabled(any()) } throws
            Exception("Error")
        viewModel.updateBiometricUnlockEnabled(true)
        advanceUntilIdle()
        assertTrue(viewModel.errorMessage.value?.contains("biometric", ignoreCase = true) == true)
      }

  @Test
  fun `clearErrorMessage clears error`() =
      runTest(testDispatcher) {
        coEvery { mockPreferencesRepository.setBiometricUnlockEnabled(any()) } throws
            Exception("Error")
        viewModel.updateBiometricUnlockEnabled(true)
        advanceUntilIdle()
        viewModel.clearErrorMessage()
        assertNull(viewModel.errorMessage.value)
      }
}
