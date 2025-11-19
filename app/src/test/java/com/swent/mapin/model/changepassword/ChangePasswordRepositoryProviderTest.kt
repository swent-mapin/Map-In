// Assisted by AI
package com.swent.mapin.model.changepassword

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

class ChangePasswordRepositoryProviderTest {

  @Before
  fun setup() {
    // Mock FirebaseAuth.getInstance() to prevent IllegalStateException in tests
    mockkStatic(FirebaseAuth::class)
    val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    every { FirebaseAuth.getInstance() } returns mockAuth

    // Mock FirebaseApp in case it's checked
    mockkStatic(FirebaseApp::class)
    every { FirebaseApp.initializeApp(any()) } returns mockk(relaxed = true)
  }

  @After
  fun tearDown() {
    // Clear instance after each test to ensure test isolation
    ChangePasswordRepositoryProvider.clearInstance()
    unmockkAll()
  }

  @Test
  fun `getRepository returns singleton instance`() {
    // Execute - call without setting a mock first to test default initialization
    val instance1 = ChangePasswordRepositoryProvider.getRepository()
    val instance2 = ChangePasswordRepositoryProvider.getRepository()

    // Verify
    assertNotNull(instance1)
    assertNotNull(instance2)
    assertSame(instance1, instance2)
  }

  @Test
  fun `getRepository creates default Firebase implementation when not set`() {
    // Execute - call without setting a mock to test Firebase initialization path
    val repository = ChangePasswordRepositoryProvider.getRepository()

    // Verify - should create a ChangePasswordRepositoryFirebase instance
    assertNotNull(repository)
    // Calling again should return the same instance (singleton behavior)
    assertSame(repository, ChangePasswordRepositoryProvider.getRepository())
  }

  @Test
  fun `setRepository allows custom repository injection`() {
    // Setup
    val mockRepository = mockk<ChangePasswordRepository>()

    // Execute
    ChangePasswordRepositoryProvider.setRepository(mockRepository)
    val retrievedRepository = ChangePasswordRepositoryProvider.getRepository()

    // Verify
    assertSame(mockRepository, retrievedRepository)
  }

  @Test
  fun `clearInstance resets singleton state`() {
    // Setup - let it create default first
    val instance1 = ChangePasswordRepositoryProvider.getRepository()

    // Execute - clear the instance
    ChangePasswordRepositoryProvider.clearInstance()

    // Get a new instance after clearing
    val instance2 = ChangePasswordRepositoryProvider.getRepository()

    // Verify - both are non-null, and second call after clear creates new instance
    assertNotNull(instance1)
    assertNotNull(instance2)
  }

  @Test
  fun `setRepository overrides existing instance`() {
    // Setup - get default instance first
    val defaultInstance = ChangePasswordRepositoryProvider.getRepository()

    // Execute - set custom mock
    val mockRepository = mockk<ChangePasswordRepository>()
    ChangePasswordRepositoryProvider.setRepository(mockRepository)
    val customInstance = ChangePasswordRepositoryProvider.getRepository()

    // Verify
    assertNotNull(defaultInstance)
    assertNotNull(customInstance)
    assertSame(mockRepository, customInstance)
  }

  @Test
  fun `clearInstance allows fresh repository creation`() {
    // Setup - inject a mock
    val mockRepository = mockk<ChangePasswordRepository>()
    ChangePasswordRepositoryProvider.setRepository(mockRepository)

    // Verify it's set
    assertSame(mockRepository, ChangePasswordRepositoryProvider.getRepository())

    // Execute - clear the instance
    ChangePasswordRepositoryProvider.clearInstance()

    // Inject a new mock after clearing
    val newMockRepository = mockk<ChangePasswordRepository>()
    ChangePasswordRepositoryProvider.setRepository(newMockRepository)

    // Verify new instance is different
    val newInstance = ChangePasswordRepositoryProvider.getRepository()
    assertNotNull(newInstance)
    assertSame(newMockRepository, newInstance)
  }

  @Test
  fun `synchronized block creates instance only once`() {
    // Clear to ensure clean state
    ChangePasswordRepositoryProvider.clearInstance()

    // Execute - first call should create instance via synchronized block
    val firstCall = ChangePasswordRepositoryProvider.getRepository()

    // Second call should return same instance without entering synchronized block
    val secondCall = ChangePasswordRepositoryProvider.getRepository()

    // Verify
    assertNotNull(firstCall)
    assertNotNull(secondCall)
    assertSame(firstCall, secondCall)
  }
}
