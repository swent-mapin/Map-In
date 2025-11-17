package com.swent.mapin.ui.map.offline

import com.mapbox.common.TileStore
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TileStoreManager.
 *
 * Note: Tests that verify actual TileStore integration (setOption calls) are in instrumentation
 * tests due to native library requirements.
 */
class TileStoreManagerTest {

  private lateinit var mockTileStore: TileStore
  private lateinit var tileStoreManager: TileStoreManager

  @Before
  fun setup() {
    mockTileStore = mockk(relaxed = true)
  }

  @Test
  fun `getTileStore returns the TileStore instance`() {
    tileStoreManager = TileStoreManager(tileStore = mockTileStore)

    val result = tileStoreManager.getTileStore()

    assertNotNull(result)
    assertSame(mockTileStore, result)
  }

  @Test
  fun `getDiskQuotaMB returns default quota`() {
    tileStoreManager = TileStoreManager(tileStore = mockTileStore)

    val quota = tileStoreManager.getDiskQuotaMB()

    assertEquals(50L, quota)
  }

  @Test
  fun `getDiskQuotaMB returns custom quota`() {
    val customQuotaMB = 75L
    tileStoreManager = TileStoreManager(tileStore = mockTileStore, diskQuotaMB = customQuotaMB)

    val quota = tileStoreManager.getDiskQuotaMB()

    assertEquals(customQuotaMB, quota)
  }

  @Test
  fun `getDiskQuotaBytes returns correct conversion from MB to bytes`() {
    val quotaMB = 25L
    tileStoreManager = TileStoreManager(tileStore = mockTileStore, diskQuotaMB = quotaMB)

    val quotaBytes = tileStoreManager.getDiskQuotaBytes()

    assertEquals(25L * 1024L * 1024L, quotaBytes)
  }

  @Test
  fun `getDiskQuotaBytes returns default value in bytes`() {
    tileStoreManager = TileStoreManager(tileStore = mockTileStore)

    val quotaBytes = tileStoreManager.getDiskQuotaBytes()

    assertEquals(50L * 1024L * 1024L, quotaBytes)
  }

  @Test
  fun `default constant is 50 MB`() {
    assertEquals(50L, TileStoreManager.DEFAULT_DISK_QUOTA_MB)
  }

  @Test
  fun `constructor accepts custom disk quota`() {
    val customQuota = 100L
    tileStoreManager = TileStoreManager(tileStore = mockTileStore, diskQuotaMB = customQuota)

    assertEquals(customQuota, tileStoreManager.getDiskQuotaMB())
    assertEquals(customQuota * 1024L * 1024L, tileStoreManager.getDiskQuotaBytes())
  }
}
