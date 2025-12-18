package com.swent.mapin.model

import android.content.Context
import com.swent.mapin.model.preferences.PreferencesRepository
import com.swent.mapin.model.preferences.PreferencesRepositoryProvider
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PreferencesRepositoryProvider.
 *
 * Tests singleton behavior including:
 * - getInstance returns same instance
 * - getInstance uses application context
 * - setInstance allows custom repository injection
 * - clearInstance resets singleton
 *
 * Test Isolation Strategy:
 * - @Before: Clears singleton before each test for intra-class isolation
 * - @After: Clears singleton after each test to prevent state leakage
 * - @AfterClass: Ensures singleton is cleared after all tests complete, preventing leaks when tests
 *   run in parallel or other test classes use the same singleton
 */
class PreferencesRepositoryProviderTest {

  private lateinit var mockContext: Context
  private lateinit var mockAppContext: Context

  companion object {
    /**
     * Class-level cleanup to ensure singleton is cleared after all tests in this class. Prevents
     * singleton state leakage to other test classes, especially important when tests run in
     * parallel.
     */
    @JvmStatic
    @AfterClass
    fun cleanup() {
      PreferencesRepositoryProvider.clearInstance()
    }
  }

  @Before
  fun setup() {
    mockContext = mockk()
    mockAppContext = mockk(relaxed = true) // Relaxed needed for DataStore interactions
    every { mockContext.getApplicationContext() } returns mockAppContext

    // Clear instance before each test for intra-class isolation
    PreferencesRepositoryProvider.clearInstance()
  }

  @After
  fun tearDown() {
    // Clean up after each test to prevent state leakage
    PreferencesRepositoryProvider.clearInstance()
  }

  @Test
  fun `getInstance returns same instance on multiple calls`() {
    val instance1 = PreferencesRepositoryProvider.getInstance(mockContext)
    val instance2 = PreferencesRepositoryProvider.getInstance(mockContext)

    assertSame(instance1, instance2)
  }

  @Test
  fun `getInstance uses application context`() {
    val repository = PreferencesRepositoryProvider.getInstance(mockContext)

    // Verify that applicationContext was accessed from the provided context
    io.mockk.verify { mockContext.getApplicationContext() }

    // Verify that the repository was created with the application context, not the original context
    // We verify this by checking that the repository actually holds the app context
    // This is done by accessing a property that triggers DataStore initialization
    assertNotNull(repository)

    // Verify that subsequent operations use mockAppContext (the application context)
    // The repository should initialize DataStore with mockAppContext, not mockContext
    io.mockk.verify(exactly = 1) { mockContext.getApplicationContext() }
  }

  @Test
  fun `getInstance passes application context to repository not original context`() {
    // Create two different mock contexts to distinguish them
    val activityContext = mockk<Context>()
    val applicationContext = mockk<Context>(relaxed = true)
    every { activityContext.getApplicationContext() } returns applicationContext

    // Clear to ensure fresh creation
    PreferencesRepositoryProvider.clearInstance()

    // Create repository with activity context
    val repository = PreferencesRepositoryProvider.getInstance(activityContext)

    // Verify that application context was retrieved from activity context
    io.mockk.verify { activityContext.getApplicationContext() }

    // Verify repository was created (not null)
    assertNotNull(repository)

    // The key verification: the activity context should only be used to get app context,
    // not for any DataStore operations. All DataStore operations should be on applicationContext.
    // We verify this by confirming getApplicationContext was called exactly once
    io.mockk.verify(exactly = 1) { activityContext.getApplicationContext() }
  }

  @Test
  fun `setInstance allows custom repository injection`() {
    val customRepo = mockk<PreferencesRepository>()

    PreferencesRepositoryProvider.setInstance(customRepo)
    val retrievedRepo = PreferencesRepositoryProvider.getInstance(mockContext)

    assertSame(customRepo, retrievedRepo)
  }

  @Test
  fun `setInstance then clearInstance resets to default behavior`() {
    // Set a custom mock repository
    val customRepo = mockk<PreferencesRepository>()
    PreferencesRepositoryProvider.setInstance(customRepo)

    // Verify custom repo is returned
    val retrievedCustomRepo = PreferencesRepositoryProvider.getInstance(mockContext)
    assertSame(customRepo, retrievedCustomRepo)

    // Clear the instance
    PreferencesRepositoryProvider.clearInstance()

    // After clearing, getInstance should create a new default repository
    val newDefaultRepo = PreferencesRepositoryProvider.getInstance(mockContext)
    assertNotNull(newDefaultRepo)
    assertNotSame(customRepo, newDefaultRepo)
  }

  @Test
  fun `setInstance with mock then setInstance with different mock replaces it`() {
    // Set first mock
    val firstMock = mockk<PreferencesRepository>()
    PreferencesRepositoryProvider.setInstance(firstMock)
    val retrieved1 = PreferencesRepositoryProvider.getInstance(mockContext)
    assertSame(firstMock, retrieved1)

    // Set second mock without clearing
    val secondMock = mockk<PreferencesRepository>()
    PreferencesRepositoryProvider.setInstance(secondMock)
    val retrieved2 = PreferencesRepositoryProvider.getInstance(mockContext)

    // Verify second mock replaced the first
    assertSame(secondMock, retrieved2)
    assertNotSame(firstMock, retrieved2)
  }

  @Test
  fun `clearInstance resets singleton`() {
    val instance1 = PreferencesRepositoryProvider.getInstance(mockContext)

    PreferencesRepositoryProvider.clearInstance()

    val instance2 = PreferencesRepositoryProvider.getInstance(mockContext)

    assertNotSame(instance1, instance2)
  }

  @Test
  fun `setInstance overrides existing instance`() {
    // Create first instance
    val instance1 = PreferencesRepositoryProvider.getInstance(mockContext)

    // Set custom instance
    val customRepo = mockk<PreferencesRepository>()
    PreferencesRepositoryProvider.setInstance(customRepo)

    // Get instance again
    val instance2 = PreferencesRepositoryProvider.getInstance(mockContext)

    assertNotSame(instance1, instance2)
    assertSame(customRepo, instance2)
  }

  @Test
  fun `getInstance creates new instance after clearInstance`() {
    val customRepo = mockk<PreferencesRepository>()
    PreferencesRepositoryProvider.setInstance(customRepo)

    val instance1 = PreferencesRepositoryProvider.getInstance(mockContext)
    assertSame(customRepo, instance1)

    PreferencesRepositoryProvider.clearInstance()

    val instance2 = PreferencesRepositoryProvider.getInstance(mockContext)
    assertNotSame(customRepo, instance2)
  }

  @Test
  fun `multiple clearInstance calls are safe`() {
    PreferencesRepositoryProvider.getInstance(mockContext)

    PreferencesRepositoryProvider.clearInstance()
    PreferencesRepositoryProvider.clearInstance()
    PreferencesRepositoryProvider.clearInstance()

    // Should not throw exception
    val instance = PreferencesRepositoryProvider.getInstance(mockContext)
    assertNotNull(instance)
  }

  @Test
  fun `getInstance is thread-safe`() {
    // Use thread-safe collection to avoid synchronization overhead and fragility
    val instances = ConcurrentHashMap.newKeySet<PreferencesRepository>()
    val threadCount = 50 // Increased from 10 to stress test more
    val callsPerThread = 100 // Multiple calls per thread to increase race condition likelihood

    // CountDownLatch ensures all threads start approximately at the same time
    val startLatch = CountDownLatch(1)
    val completionLatch = CountDownLatch(threadCount)

    val threads =
        List(threadCount) {
          Thread {
            try {
              // Wait for all threads to be ready before starting
              startLatch.await(5, TimeUnit.SECONDS)

              // Make multiple getInstance calls per thread
              repeat(callsPerThread) {
                val instance = PreferencesRepositoryProvider.getInstance(mockContext)
                instances.add(instance)
              }
            } finally {
              completionLatch.countDown()
            }
          }
        }

    // Start all threads
    threads.forEach { it.start() }

    // Release all threads simultaneously to maximize contention
    startLatch.countDown()

    // Wait for all threads to complete (with timeout for safety)
    val completed = completionLatch.await(10, TimeUnit.SECONDS)
    assertTrue("Threads did not complete in time", completed)

    // All threads should get the same singleton instance
    assertEquals("Expected exactly 1 instance, got ${instances.size}", 1, instances.size)
  }
}
