package com.swent.mapin.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// Assisted by AI
class BottomSheetTest {

  @get:Rule val rule = createComposeRule()

  private enum class TestState {
    COLLAPSED,
    MEDIUM,
    FULL
  }

  private val config =
      BottomSheetConfig(collapsedHeight = 100.dp, mediumHeight = 300.dp, fullHeight = 500.dp)

  private fun stateToHeight(s: TestState): Dp =
      when (s) {
        TestState.COLLAPSED -> config.collapsedHeight
        TestState.MEDIUM -> config.mediumHeight
        TestState.FULL -> config.fullHeight
      }

  @Test
  fun initialComposition_displaysAtCollapsedHeight() {
    var reportedHeight = 0.dp

    rule.setContent {
      MaterialTheme {
        var current by remember { mutableStateOf(TestState.COLLAPSED) }
        BottomSheet(
            modifier = Modifier.testTag("sheet"),
            config = config,
            currentState = current,
            onStateChange = { current = it },
            calculateTargetState = { _, _, _, _ -> current },
            stateToHeight = ::stateToHeight,
            onHeightChange = { reportedHeight = it }) {}
      }
    }

    rule.onNodeWithTag("sheet").assertIsDisplayed()
    assertEquals(config.collapsedHeight, reportedHeight)
  }

  @Test
  fun externalStateChange_animatesToTargetHeight() {
    var current by mutableStateOf(TestState.COLLAPSED)
    var reportedHeight = 0.dp

    rule.setContent {
      MaterialTheme {
        BottomSheet(
            modifier = Modifier.testTag("sheet"),
            config = config,
            currentState = current,
            onStateChange = { current = it },
            calculateTargetState = { _, _, _, _ -> current },
            stateToHeight = ::stateToHeight,
            onHeightChange = { reportedHeight = it }) {}
      }
    }

    rule.waitForIdle()
    assertEquals(config.collapsedHeight, reportedHeight)

    current = TestState.MEDIUM
    rule.waitForIdle()
    assertEquals(config.mediumHeight, reportedHeight)

    current = TestState.FULL
    rule.waitForIdle()
    assertEquals(config.fullHeight, reportedHeight)
  }

  @Test
  fun dragUpAndRelease_snapsToCalculatedState() {
    var current by mutableStateOf(TestState.COLLAPSED)
    var stateChangeCount = 0

    rule.setContent {
      MaterialTheme {
        BottomSheet(
            modifier = Modifier.testTag("sheet"),
            config = config,
            currentState = current,
            onStateChange = {
              current = it
              stateChangeCount++
            },
            calculateTargetState = { _, _, _, _ -> TestState.FULL },
            stateToHeight = ::stateToHeight,
            onHeightChange = {}) {}
      }
    }

    rule.onNodeWithTag("sheet").performTouchInput { swipeUp() }
    rule.waitForIdle()

    assertEquals(TestState.FULL, current)
    assertTrue(stateChangeCount > 0)
  }

  @Test
  fun dragDown_snapsToCalculatedState() {
    var current by mutableStateOf(TestState.FULL)

    rule.setContent {
      MaterialTheme {
        BottomSheet(
            modifier = Modifier.testTag("sheet"),
            config = config,
            currentState = current,
            onStateChange = { current = it },
            calculateTargetState = { _, _, _, _ -> TestState.COLLAPSED },
            stateToHeight = ::stateToHeight,
            onHeightChange = {}) {}
      }
    }

    rule.onNodeWithTag("sheet").performTouchInput { swipeDown() }
    rule.waitForIdle()

    assertEquals(TestState.COLLAPSED, current)
  }

  @Test
  fun drag_reportsIntermediateHeights() {
    val heights = mutableListOf<Dp>()

    rule.setContent {
      MaterialTheme {
        var current by remember { mutableStateOf(TestState.COLLAPSED) }
        BottomSheet(
            modifier = Modifier.testTag("sheet"),
            config = config,
            currentState = current,
            onStateChange = { current = it },
            calculateTargetState = { _, _, _, _ -> TestState.FULL },
            stateToHeight = ::stateToHeight,
            onHeightChange = { heights.add(it) }) {}
      }
    }

    rule.onNodeWithTag("sheet").performTouchInput {
      down(center)
      repeat(5) {
        moveBy(androidx.compose.ui.geometry.Offset(0f, -30f))
        advanceEventTime(16)
      }
      up()
    }
    rule.waitForIdle()

    assertTrue(heights.distinct().size > 2)
    assertTrue(heights.last() > heights.first())
  }
}
