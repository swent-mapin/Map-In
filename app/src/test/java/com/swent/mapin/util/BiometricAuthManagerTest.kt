package com.swent.mapin.util

import androidx.biometric.BiometricManager
import androidx.fragment.app.FragmentActivity
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BiometricAuthManagerTest {

  private lateinit var biometricAuthManager: BiometricAuthManager
  private lateinit var mockActivity: FragmentActivity
  private lateinit var mockBiometricManager: BiometricManager

  @Before
  fun setUp() {
    biometricAuthManager = BiometricAuthManager()
    mockActivity = mockk(relaxed = true)
    mockBiometricManager = mockk(relaxed = true)
    mockkStatic(BiometricManager::class)
    every { BiometricManager.from(any()) } returns mockBiometricManager
  }

  @After fun tearDown() = unmockkAll()

  @Test
  fun `canUseBiometric returns true when BIOMETRIC_STRONG available`() {
    every { mockBiometricManager.canAuthenticate(any()) } returns BiometricManager.BIOMETRIC_SUCCESS
    assertTrue(biometricAuthManager.canUseBiometric(mockActivity))
  }

  @Test
  fun `canUseBiometric returns true when only BIOMETRIC_WEAK available`() {
    every {
      mockBiometricManager.canAuthenticate(
          BiometricManager.Authenticators.BIOMETRIC_STRONG or
              BiometricManager.Authenticators.DEVICE_CREDENTIAL)
    } returns BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
    every {
      mockBiometricManager.canAuthenticate(
          BiometricManager.Authenticators.BIOMETRIC_WEAK or
              BiometricManager.Authenticators.DEVICE_CREDENTIAL)
    } returns BiometricManager.BIOMETRIC_SUCCESS
    assertTrue(biometricAuthManager.canUseBiometric(mockActivity))
  }

  @Test
  fun `canUseBiometric returns false when no biometrics available`() {
    every { mockBiometricManager.canAuthenticate(any()) } returns
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
    assertFalse(biometricAuthManager.canUseBiometric(mockActivity))
  }

  @Test
  fun `canUseBiometric returns false when not enrolled`() {
    every { mockBiometricManager.canAuthenticate(any()) } returns
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
    assertFalse(biometricAuthManager.canUseBiometric(mockActivity))
  }

  @Test
  fun `authenticate calls onError when biometric not available`() {
    every { mockBiometricManager.canAuthenticate(any()) } returns
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
    var errorMessage: String? = null
    biometricAuthManager.authenticate(
        activity = mockActivity,
        onSuccess = { fail("Should not succeed") },
        onError = { errorMessage = it },
        onFallback = { fail("Should not fallback") })
    assertEquals("Biometric authentication not available on this device.", errorMessage)
  }
}
