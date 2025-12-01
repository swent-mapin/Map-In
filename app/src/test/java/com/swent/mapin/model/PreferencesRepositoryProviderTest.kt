package com.swent.mapin.model

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.After
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
 */
class PreferencesRepositoryProviderTest {

  private lateinit var mockContext: Context
  private lateinit var mockAppContext: Context

  @Before
  fun setup() {
    mockContext = mockk(relaxed = true)
    mockAppContext = mockk(relaxed = true)
    every { mockContext.applicationContext } returns mockAppContext

    // Clear instance before each test
    PreferencesRepositoryProvider.clearInstance()
  }

  @After
  fun tearDown() {
    // Clean up after each test
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
    PreferencesRepositoryProvider.getInstance(mockContext)

    // Verify that applicationContext was accessed
    io.mockk.verify { mockContext.applicationContext }
  }

  @Test
  fun `setInstance allows custom repository injection`() {
    val customRepo = mockk<PreferencesRepository>()

    PreferencesRepositoryProvider.setInstance(customRepo)
    val retrievedRepo = PreferencesRepositoryProvider.getInstance(mockContext)

    assertSame(customRepo, retrievedRepo)
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
    val instances = mutableSetOf<PreferencesRepository>()
    val threads =
        List(10) {
          Thread {
            val instance = PreferencesRepositoryProvider.getInstance(mockContext)
            synchronized(instances) { instances.add(instance) }
          }
        }

    threads.forEach { it.start() }
    threads.forEach { it.join() }

    // All threads should get the same instance
    assertEquals(1, instances.size)
  }
}
