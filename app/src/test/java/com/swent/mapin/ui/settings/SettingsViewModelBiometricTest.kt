package com.swent.mapin.ui.settings

import com.swent.mapin.model.PreferencesRepository
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
  private val testDispatcher = UnconfinedTestDispatcher()
  private val biometricUnlockFlow = MutableStateFlow(false)

  @Before
  fun setUp() {
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

    // Trigger stateIn flows to start collecting
    viewModel.biometricUnlockEnabled.value
    testDispatcher.scheduler.advanceUntilIdle()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun `biometricUnlockEnabled initial value is false`() {
    assertEquals(false, viewModel.biometricUnlockEnabled.value)
  }

  @Test
  fun `updateBiometricUnlockEnabled calls repository with true`() = runTest {
    coEvery { mockPreferencesRepository.setBiometricUnlockEnabled(true) } just Runs
    viewModel.updateBiometricUnlockEnabled(true)
    testDispatcher.scheduler.advanceUntilIdle()
    coVerify { mockPreferencesRepository.setBiometricUnlockEnabled(true) }
  }

  @Test
  fun `updateBiometricUnlockEnabled calls repository with false`() = runTest {
    coEvery { mockPreferencesRepository.setBiometricUnlockEnabled(false) } just Runs
    viewModel.updateBiometricUnlockEnabled(false)
    testDispatcher.scheduler.advanceUntilIdle()
    coVerify { mockPreferencesRepository.setBiometricUnlockEnabled(false) }
  }

  @Test
  fun `updateBiometricUnlockEnabled sets error on exception`() = runTest {
    coEvery { mockPreferencesRepository.setBiometricUnlockEnabled(any()) } throws Exception("Error")
    viewModel.updateBiometricUnlockEnabled(true)
    testDispatcher.scheduler.advanceUntilIdle()
    assertTrue(viewModel.errorMessage.value?.contains("biometric", ignoreCase = true) == true)
  }

  @Test
  fun `clearErrorMessage clears error`() = runTest {
    coEvery { mockPreferencesRepository.setBiometricUnlockEnabled(any()) } throws Exception("Error")
    viewModel.updateBiometricUnlockEnabled(true)
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.clearErrorMessage()
    assertNull(viewModel.errorMessage.value)
  }
}
