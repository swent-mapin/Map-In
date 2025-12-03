package com.swent.mapin.model.event

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for EventRepositoryProvider.
 *
 * Tests singleton behavior including:
 * - setRepository allows custom repository injection
 * - resetRepository clears singleton
 *
 * Note: Tests that create real EventRepositoryFirestore instances are skipped as they require full
 * Firebase initialization.
 */
class EventRepositoryProviderTest {

  private lateinit var mockContext: Context
  private lateinit var mockAppContext: Context

  @Before
  fun setup() {
    mockContext = mockk(relaxed = true)
    mockAppContext = mockk(relaxed = true)
    every { mockContext.applicationContext } returns mockAppContext

    // Reset provider before each test
    EventRepositoryProvider.resetRepository()
  }

  @After
  fun tearDown() {
    // Clean up after each test
    EventRepositoryProvider.resetRepository()
  }

  @Test
  fun `setRepository allows custom repository injection`() {
    val customRepo = mockk<EventRepository>()

    EventRepositoryProvider.setRepository(customRepo)
    val retrievedRepo = EventRepositoryProvider.getRepository()

    assertSame(customRepo, retrievedRepo)
  }

  @Test
  fun `resetRepository clears singleton`() {
    val customRepo1 = mockk<EventRepository>()
    EventRepositoryProvider.setRepository(customRepo1)
    val instance1 = EventRepositoryProvider.getRepository()

    EventRepositoryProvider.resetRepository()

    val customRepo2 = mockk<EventRepository>()
    EventRepositoryProvider.setRepository(customRepo2)
    val instance2 = EventRepositoryProvider.getRepository()

    assertNotSame(instance1, instance2)
  }

  @Test
  fun `init stores application context`() {
    EventRepositoryProvider.init(mockContext)

    // Verify that applicationContext was accessed
    verify { mockContext.applicationContext }
  }

  @Test
  fun `setRepository overrides existing instance`() {
    // Set first custom instance
    val customRepo1 = mockk<EventRepository>()
    EventRepositoryProvider.setRepository(customRepo1)

    val instance1 = EventRepositoryProvider.getRepository()

    // Set second custom instance
    val customRepo2 = mockk<EventRepository>()
    EventRepositoryProvider.setRepository(customRepo2)

    // Get instance again
    val instance2 = EventRepositoryProvider.getRepository()

    assertNotSame(instance1, instance2)
    assertSame(customRepo2, instance2)
  }

  @Test
  fun `multiple resetRepository calls are safe`() {
    val customRepo = mockk<EventRepository>()
    EventRepositoryProvider.setRepository(customRepo)

    EventRepositoryProvider.resetRepository()
    EventRepositoryProvider.resetRepository()
    EventRepositoryProvider.resetRepository()

    // Should not throw exception - set a new one to verify it works
    val newRepo = mockk<EventRepository>()
    EventRepositoryProvider.setRepository(newRepo)
    val instance = EventRepositoryProvider.getRepository()
    assertNotNull(instance)
  }

  @Test
  fun `init can be called multiple times safely`() {
    EventRepositoryProvider.init(mockContext)
    EventRepositoryProvider.init(mockContext)
    EventRepositoryProvider.init(mockContext)

    // Should not throw exception
    verify(atLeast = 3) { mockContext.applicationContext }
  }

  @Test
  fun `getRepository with custom instance is thread-safe`() = runTest {
    val customRepo = mockk<EventRepository>()
    EventRepositoryProvider.setRepository(customRepo)

    // Use concurrent set for thread-safe access
    val instances = java.util.concurrent.ConcurrentHashMap.newKeySet<EventRepository>()

    // Launch multiple coroutines concurrently to test thread safety
    val jobs =
        List(100) {
          launch(kotlinx.coroutines.Dispatchers.Default) {
            val instance = EventRepositoryProvider.getRepository()
            instances.add(instance)
          }
        }

    // Wait for all coroutines to complete
    jobs.forEach { it.join() }

    // All coroutines should get the same instance
    assertEquals(1, instances.size)
  }
}
