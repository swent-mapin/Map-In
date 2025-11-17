package com.swent.mapin.ui.map.offline

import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Unit tests for TileStoreManagerProvider.
 *
 * Note: Tests that verify actual TileStore creation (getInstance without setInstance) are in
 * instrumentation tests due to native library requirements.
 */
class TileStoreManagerProviderTest {

  @After
  fun tearDown() {
    // Clean up singleton state between tests
    TileStoreManagerProvider.clearInstance()
  }

  @Test
  fun `setInstance allows custom TileStoreManager`() {
    val mockManager = mockk<TileStoreManager>(relaxed = true)

    TileStoreManagerProvider.setInstance(mockManager)
    val retrieved = TileStoreManagerProvider.getInstance()

    assertSame(mockManager, retrieved)
  }

  @Test
  fun `getInstance returns non-null TileStoreManager after setInstance`() {
    val mockManager = mockk<TileStoreManager>(relaxed = true)
    TileStoreManagerProvider.setInstance(mockManager)

    val manager = TileStoreManagerProvider.getInstance()

    assertNotNull(manager)
  }

  @Test
  fun `getInstance returns same instance on multiple calls after setInstance`() {
    val mockManager = mockk<TileStoreManager>(relaxed = true)
    TileStoreManagerProvider.setInstance(mockManager)

    val manager1 = TileStoreManagerProvider.getInstance()
    val manager2 = TileStoreManagerProvider.getInstance()

    assertSame(manager1, manager2)
  }

  @Test
  fun `clearInstance resets singleton state`() {
    val mockManager1 = mockk<TileStoreManager>(relaxed = true)
    val mockManager2 = mockk<TileStoreManager>(relaxed = true)

    TileStoreManagerProvider.setInstance(mockManager1)
    val manager1 = TileStoreManagerProvider.getInstance()

    TileStoreManagerProvider.clearInstance()
    TileStoreManagerProvider.setInstance(mockManager2)
    val manager2 = TileStoreManagerProvider.getInstance()

    // Should be different instances after clear
    assert(manager1 !== manager2)
    assertSame(mockManager1, manager1)
    assertSame(mockManager2, manager2)
  }

  @Test
  fun `setInstance replaces existing instance`() {
    val mockManager1 = mockk<TileStoreManager>(relaxed = true)
    val mockManager2 = mockk<TileStoreManager>(relaxed = true)

    TileStoreManagerProvider.setInstance(mockManager1)
    val manager1 = TileStoreManagerProvider.getInstance()

    TileStoreManagerProvider.setInstance(mockManager2)
    val retrieved = TileStoreManagerProvider.getInstance()

    assertSame(mockManager2, retrieved)
    assert(manager1 !== retrieved)
  }

  @Test
  fun `setInstance with custom manager preserves custom properties`() {
    val mockManager = mockk<TileStoreManager>(relaxed = true)
    every { mockManager.getDiskQuotaMB() } returns 100L

    TileStoreManagerProvider.setInstance(mockManager)
    val retrieved = TileStoreManagerProvider.getInstance()

    assertSame(mockManager, retrieved)
    assert(retrieved.getDiskQuotaMB() == 100L)
  }
}
