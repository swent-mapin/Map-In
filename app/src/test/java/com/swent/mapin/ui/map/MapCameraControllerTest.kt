package com.swent.mapin.ui.map

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapCameraControllerTest {

  @Test
  fun runProgrammatic_marksZoomAsProgrammaticDuringBlock() = runTest {
    val controller = MapCameraController(this)

    var wasProgrammaticInsideBlock = false

    controller.runProgrammatic { wasProgrammaticInsideBlock = controller.isProgrammaticZoom }

    assertTrue(wasProgrammaticInsideBlock)

    advanceTimeBy(1200)
    assertFalse(controller.isProgrammaticZoom)
  }
}
