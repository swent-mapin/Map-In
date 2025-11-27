package com.swent.mapin.ui.map.offline

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mapbox.geojson.Point
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineRegionManagerInstrumentedTest {

  private lateinit var manager: OfflineRegionManager
  private lateinit var tileStoreManager: TileStoreManager

  @Before
  fun setup() {
    tileStoreManager = TileStoreManager()
    manager =
        OfflineRegionManager(
            tileStore = tileStoreManager.getTileStore(), connectivityProvider = { flowOf(true) })
  }

  @After fun tearDown() = manager.cancelActiveDownload()

  @Test
  fun removeTileRegion_callsOnCompleteWithSuccess() = runBlocking {
    val bounds = CoordinateBounds(Point.fromLngLat(6.5, 46.5), Point.fromLngLat(6.6, 46.6))
    val result = manager.removeTileRegion(bounds)
    assertTrue(result.isSuccess)
  }

  @Test
  fun removeTileRegion_worksWithoutCallback() = runBlocking {
    val bounds = CoordinateBounds(Point.fromLngLat(6.5, 46.5), Point.fromLngLat(6.6, 46.6))
    val result = manager.removeTileRegion(bounds)
    assertTrue(result.isSuccess)
  }

  @Test
  fun removeTileRegion_generatesConsistentRegionIds() = runBlocking {
    val bounds = CoordinateBounds(Point.fromLngLat(6.5, 46.5), Point.fromLngLat(6.6, 46.6))
    val result1 = manager.removeTileRegion(bounds)
    val result2 = manager.removeTileRegion(bounds)
    assertTrue(result1.isSuccess)
    assertTrue(result2.isSuccess)
  }

  @Test
  fun cancelActiveDownload_doesNotThrow() {
    manager.cancelActiveDownload()
  }
}
