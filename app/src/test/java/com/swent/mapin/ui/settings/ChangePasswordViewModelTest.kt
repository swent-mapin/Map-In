// assisted by AI
package com.swent.mapin.ui.settings

import com.swent.mapin.model.changepassword.ChangePasswordRepository
import com.swent.mapin.model.changepassword.PasswordChangeResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChangePasswordViewModelTest {

  private lateinit var viewModel: ChangePasswordViewModel
  private lateinit var mockRepository: ChangePasswordRepository
  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockRepository = mockk(relaxed = true)
    viewModel = ChangePasswordViewModel(mockRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun `initial state is correct`() {
    val state = viewModel.state.value
    assertEquals("", state.currentPassword)
    assertEquals("", state.newPassword)
    assertEquals("", state.confirmPassword)
    assertFalse(state.isLoading)
    assertNull(state.errorMessage)
    assertNull(state.validationErrors.currentPasswordError)
    assertNull(state.validationErrors.newPasswordError)
    assertNull(state.validationErrors.confirmPasswordError)
  }

  @Test
  fun `updateCurrentPassword updates state correctly`() {
    viewModel.updateCurrentPassword("testPassword123")
    assertEquals("testPassword123", viewModel.state.value.currentPassword)
  }

  @Test
  fun `updateNewPassword updates state correctly`() {
    viewModel.updateNewPassword("newPassword123")
    assertEquals("newPassword123", viewModel.state.value.newPassword)
  }

  @Test
  fun `updateConfirmPassword updates state correctly`() {
    viewModel.updateConfirmPassword("confirmPassword123")
    assertEquals("confirmPassword123", viewModel.state.value.confirmPassword)
  }

  @Test
  fun `changePassword fails when current password is empty`() = runTest {
    viewModel.updateCurrentPassword("")
    viewModel.updateNewPassword("ValidPass123!")
    viewModel.updateConfirmPassword("ValidPass123!")

    var successCalled = false
    viewModel.changePassword { successCalled = true }

    assertFalse(successCalled)
    assertEquals(
        "Current password is required", viewModel.state.value.validationErrors.currentPasswordError)
  }

  @Test
  fun `changePassword fails when new password is empty`() = runTest {
    viewModel.updateCurrentPassword("currentPass123!")
    viewModel.updateNewPassword("")
    viewModel.updateConfirmPassword("")

    var successCalled = false
    viewModel.changePassword { successCalled = true }

    assertFalse(successCalled)
    assertEquals(
        "New password is required", viewModel.state.value.validationErrors.newPasswordError)
  }

  @Test
  fun `changePassword fails when new password is too short`() = runTest {
    viewModel.updateCurrentPassword("currentPass123!")
    viewModel.updateNewPassword("Short1!")
    viewModel.updateConfirmPassword("Short1!")

    var successCalled = false
    viewModel.changePassword { successCalled = true }

    assertFalse(successCalled)
    assertEquals(
        "Password must be at least 8 characters long",
        viewModel.state.value.validationErrors.newPasswordError)
  }

  @Test
  fun `changePassword fails when new password lacks uppercase letter`() = runTest {
    viewModel.updateCurrentPassword("currentPass123!")
    viewModel.updateNewPassword("lowercase123!")
    viewModel.updateConfirmPassword("lowercase123!")

    var successCalled = false
    viewModel.changePassword { successCalled = true }

    assertFalse(successCalled)
    assertEquals(
        "Password must contain at least one uppercase letter",
        viewModel.state.value.validationErrors.newPasswordError)
  }

  @Test
  fun `changePassword fails when new password lacks lowercase letter`() = runTest {
    viewModel.updateCurrentPassword("currentPass123!")
    viewModel.updateNewPassword("UPPERCASE123!")
    viewModel.updateConfirmPassword("UPPERCASE123!")

    var successCalled = false
    viewModel.changePassword { successCalled = true }

    assertFalse(successCalled)
    assertEquals(
        "Password must contain at least one lowercase letter",
        viewModel.state.value.validationErrors.newPasswordError)
  }

  @Test
  fun `changePassword fails when new password lacks digit`() = runTest {
    viewModel.updateCurrentPassword("currentPass123!")
    viewModel.updateNewPassword("NoDigitsHere!")
    viewModel.updateConfirmPassword("NoDigitsHere!")

    var successCalled = false
    viewModel.changePassword { successCalled = true }

    assertFalse(successCalled)
    assertEquals(
        "Password must contain at least one number",
        viewModel.state.value.validationErrors.newPasswordError)
  }

  @Test
  fun `changePassword fails when new password lacks special character`() = runTest {
    viewModel.updateCurrentPassword("currentPass123!")
    viewModel.updateNewPassword("NoSpecial123")
    viewModel.updateConfirmPassword("NoSpecial123")

    var successCalled = false
    viewModel.changePassword { successCalled = true }

    assertFalse(successCalled)
    assertEquals(
        "Password must contain at least one special character",
        viewModel.state.value.validationErrors.newPasswordError)
  }

  @Test
  fun `changePassword fails when confirm password is empty`() = runTest {
    viewModel.updateCurrentPassword("currentPass123!")
    viewModel.updateNewPassword("ValidPass123!")
    viewModel.updateConfirmPassword("")

    var successCalled = false
    viewModel.changePassword { successCalled = true }

    assertFalse(successCalled)
    assertEquals(
        "Please confirm your password", viewModel.state.value.validationErrors.confirmPasswordError)
  }

  @Test
  fun `changePassword fails when passwords do not match`() = runTest {
    viewModel.updateCurrentPassword("currentPass123!")
    viewModel.updateNewPassword("ValidPass123!")
    viewModel.updateConfirmPassword("DifferentPass123!")

    var successCalled = false
    viewModel.changePassword { successCalled = true }

    assertFalse(successCalled)
    assertEquals(
        "Passwords do not match", viewModel.state.value.validationErrors.confirmPasswordError)
  }

  @Test
  fun `changePassword fails when new password is same as current`() = runTest {
    viewModel.updateCurrentPassword("SamePass123!")
    viewModel.updateNewPassword("SamePass123!")
    viewModel.updateConfirmPassword("SamePass123!")

    var successCalled = false
    viewModel.changePassword { successCalled = true }

    assertFalse(successCalled)
    assertEquals(
        "New password must be different from current password",
        viewModel.state.value.validationErrors.newPasswordError)
  }

  @Test
  fun `changePassword succeeds with valid inputs`() = runTest {
    coEvery { mockRepository.changePassword(any(), any()) } returns PasswordChangeResult.Success

    viewModel.updateCurrentPassword("currentPass123!")
    viewModel.updateNewPassword("NewValidPass123!")
    viewModel.updateConfirmPassword("NewValidPass123!")

    var successCalled = false
    viewModel.changePassword { successCalled = true }

    assertTrue(successCalled)
    assertFalse(viewModel.state.value.isLoading)
    assertNull(viewModel.state.value.errorMessage)
    coVerify { mockRepository.changePassword("currentPass123!", "NewValidPass123!") }
  }

  @Test
  fun `changePassword handles InvalidCurrentPassword result`() = runTest {
    coEvery { mockRepository.changePassword(any(), any()) } returns
        PasswordChangeResult.InvalidCurrentPassword

    viewModel.updateCurrentPassword("wrongPass123!")
    viewModel.updateNewPassword("NewValidPass123!")
    viewModel.updateConfirmPassword("NewValidPass123!")

    var successCalled = false
    viewModel.changePassword { successCalled = true }

    assertFalse(successCalled)
    assertFalse(viewModel.state.value.isLoading)
    assertEquals(
        "Current password is incorrect",
        viewModel.state.value.validationErrors.currentPasswordError)
  }

  @Test
  fun `changePassword handles UserNotFound result`() = runTest {
    coEvery { mockRepository.changePassword(any(), any()) } returns
        PasswordChangeResult.UserNotFound

    viewModel.updateCurrentPassword("currentPass123!")
    viewModel.updateNewPassword("NewValidPass123!")
    viewModel.updateConfirmPassword("NewValidPass123!")

    var successCalled = false
    viewModel.changePassword { successCalled = true }

    assertFalse(successCalled)
    assertFalse(viewModel.state.value.isLoading)
    assertTrue(viewModel.state.value.errorMessage!!.contains("User account not found"))
  }

  @Test
  fun `changePassword handles UserNotAuthenticated result`() = runTest {
    coEvery { mockRepository.changePassword(any(), any()) } returns
        PasswordChangeResult.UserNotAuthenticated

    viewModel.updateCurrentPassword("currentPass123!")
    viewModel.updateNewPassword("NewValidPass123!")
    viewModel.updateConfirmPassword("NewValidPass123!")

    var successCalled = false
    viewModel.changePassword { successCalled = true }

    assertFalse(successCalled)
    assertFalse(viewModel.state.value.isLoading)
    assertTrue(viewModel.state.value.errorMessage!!.contains("must be signed in"))
  }

  @Test
  fun `changePassword handles generic Error result`() = runTest {
    coEvery { mockRepository.changePassword(any(), any()) } returns
        PasswordChangeResult.Error("Custom error message")

    viewModel.updateCurrentPassword("currentPass123!")
    viewModel.updateNewPassword("NewValidPass123!")
    viewModel.updateConfirmPassword("NewValidPass123!")

    var successCalled = false
    viewModel.changePassword { successCalled = true }

    assertFalse(successCalled)
    assertFalse(viewModel.state.value.isLoading)
    assertEquals("Custom error message", viewModel.state.value.errorMessage)
  }

  @Test
  fun `clearError clears error message`() = runTest {
    coEvery { mockRepository.changePassword(any(), any()) } returns
        PasswordChangeResult.Error("Test error")

    viewModel.updateCurrentPassword("currentPass123!")
    viewModel.updateNewPassword("NewValidPass123!")
    viewModel.updateConfirmPassword("NewValidPass123!")

    viewModel.changePassword {}

    viewModel.clearError()

    assertNull(viewModel.state.value.errorMessage)
  }

  @Test
  fun `resetState resets all fields`() = runTest {
    viewModel.updateCurrentPassword("test123")
    viewModel.updateNewPassword("newTest123")
    viewModel.updateConfirmPassword("newTest123")

    viewModel.resetState()

    val state = viewModel.state.value
    assertEquals("", state.currentPassword)
    assertEquals("", state.newPassword)
    assertEquals("", state.confirmPassword)
    assertFalse(state.isLoading)
    assertNull(state.errorMessage)
  }

  @Test
  fun `updating fields clears their validation errors`() = runTest {
    // Trigger validation errors
    viewModel.updateCurrentPassword("")
    viewModel.updateNewPassword("")
    viewModel.updateConfirmPassword("")
    viewModel.changePassword {}

    // Update each field and verify its error is cleared
    viewModel.updateCurrentPassword("test")
    assertNull(viewModel.state.value.validationErrors.currentPasswordError)

    viewModel.updateNewPassword("test")
    assertNull(viewModel.state.value.validationErrors.newPasswordError)

    viewModel.updateConfirmPassword("test")
    assertNull(viewModel.state.value.validationErrors.confirmPasswordError)
  }

  @Test
  fun `factory creates ViewModel correctly`() {
    val factory = ChangePasswordViewModel.Factory(mockRepository)
    val viewModel = factory.create(ChangePasswordViewModel::class.java)
    assertTrue(viewModel is ChangePasswordViewModel)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `factory throws exception for wrong class`() {
    val factory = ChangePasswordViewModel.Factory(mockRepository)
    factory.create(SettingsViewModel::class.java)
  }
}
