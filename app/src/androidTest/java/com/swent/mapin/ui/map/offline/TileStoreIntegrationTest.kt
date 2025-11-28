package com.swent.mapin.ui.map.offline

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

/** Integration tests for TileStoreManager with actual Mapbox TileStore. */
@RunWith(AndroidJUnit4::class)
class TileStoreIntegrationTest {

  @After
  fun tearDown() {
    TileStoreManagerProvider.clearInstance()
  }

  @Test
  fun providerGetInstanceCreatesRealTileStoreManager() {
    val manager = TileStoreManagerProvider.getInstance()

    assertNotNull(manager)
    assertEquals(4096L, manager.getDiskQuotaMB())
  }

  @Test
  fun providerGetInstanceReturnsSameTileStoreManager() {
    val manager1 = TileStoreManagerProvider.getInstance()
    val manager2 = TileStoreManagerProvider.getInstance()

    assertSame(manager1, manager2)
  }

  @Test
  fun tileStoreManagerCreatesRealTileStore() {
    val manager = TileStoreManager()

    val tileStore = manager.getTileStore()

    assertNotNull(tileStore)
  }

  @Test
  fun tileStoreManagerInitializeDoesNotThrow() {
    val manager = TileStoreManager()

    // Should not throw exception
    manager.initialize()
  }

  @Test
  fun tileStoreManagerWithCustomQuotaCreatesCorrectly() {
    val customQuota = 100L
    val manager = TileStoreManager(diskQuotaMB = customQuota)

    assertEquals(customQuota, manager.getDiskQuotaMB())
    assertEquals(customQuota * 1024L * 1024L, manager.getDiskQuotaBytes())
  }

  @Test
  fun providerClearInstanceAllowsNewInstance() {
    val manager1 = TileStoreManagerProvider.getInstance()

    TileStoreManagerProvider.clearInstance()
    val manager2 = TileStoreManagerProvider.getInstance()

    assert(manager1 !== manager2)
  }
}
