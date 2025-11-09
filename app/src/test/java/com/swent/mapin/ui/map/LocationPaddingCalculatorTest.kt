package com.swent.mapin.ui.map

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationPaddingCalculatorTest {

  private val collapsedPx = 120f
  private val mediumPx = 400f
  private val minPaddingPx = 8f
  private val mediumWeight = 0.85f
  private val mediumExtraPx = 16f

  @Test
  fun padding_equalsMinWhenSheetCollapsed() {
    val padding =
        calculateLocationPaddingPx(
            sheetHeightPx = collapsedPx,
            collapsedHeightPx = collapsedPx,
            mediumHeightPx = mediumPx,
            minPaddingPx = minPaddingPx,
            mediumWeight = mediumWeight,
            mediumExtraPx = mediumExtraPx)

    assertEquals(minPaddingPx, padding, 0.0001f)
  }

  @Test
  fun paddingMatchesMediumTarget() {
    val padding =
        calculateLocationPaddingPx(
            sheetHeightPx = mediumPx,
            collapsedHeightPx = collapsedPx,
            mediumHeightPx = mediumPx,
            minPaddingPx = minPaddingPx,
            mediumWeight = mediumWeight,
            mediumExtraPx = mediumExtraPx)

    val expected = mediumPx * mediumWeight + mediumExtraPx
    assertEquals(expected, padding, 0.0001f)
  }

  @Test
  fun paddingUsesMediumTargetOnceThresholdReached() {
    val sheet = mediumPx + 20f // just over medium

    val padding =
        calculateLocationPaddingPx(
            sheetHeightPx = sheet,
            collapsedHeightPx = collapsedPx,
            mediumHeightPx = mediumPx,
            minPaddingPx = minPaddingPx,
            mediumWeight = mediumWeight,
            mediumExtraPx = mediumExtraPx)

    val expected = sheet * mediumWeight + mediumExtraPx
    assertEquals(expected, padding, 0.0001f)
  }
}
