package com.swent.mapin.ui.map.offline

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mapbox.geojson.Point
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.flowOf
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
  fun removeTileRegion_callsOnCompleteWithSuccess() {
    val bounds = CoordinateBounds(Point.fromLngLat(6.5, 46.5), Point.fromLngLat(6.6, 46.6))
    val latch = CountDownLatch(1)
    var result: Result<Unit>? = null
    manager.removeTileRegion(bounds) {
      result = it
      latch.countDown()
    }
    assertTrue(latch.await(1, TimeUnit.SECONDS))
    assertNotNull(result)
    assertEquals(true, result?.isSuccess)
  }

  @Test
  fun removeTileRegion_worksWithoutCallback() {
    val bounds = CoordinateBounds(Point.fromLngLat(6.5, 46.5), Point.fromLngLat(6.6, 46.6))
    manager.removeTileRegion(bounds)
  }

  @Test
  fun removeTileRegion_generatesConsistentRegionIds() {
    val bounds = CoordinateBounds(Point.fromLngLat(6.5, 46.5), Point.fromLngLat(6.6, 46.6))
    val latch = CountDownLatch(2)
    val results = mutableListOf<Result<Unit>>()
    manager.removeTileRegion(bounds) {
      results.add(it)
      latch.countDown()
    }
    manager.removeTileRegion(bounds) {
      results.add(it)
      latch.countDown()
    }
    assertTrue(latch.await(2, TimeUnit.SECONDS))
    assertEquals(2, results.size)
    assertTrue(results[0].isSuccess)
    assertTrue(results[1].isSuccess)
  }

  @Test
  fun cancelActiveDownload_doesNotThrow() {
    manager.cancelActiveDownload()
  }
}
