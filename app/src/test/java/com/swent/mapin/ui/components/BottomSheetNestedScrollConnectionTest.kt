package com.swent.mapin.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.swent.mapin.ui.map.BottomSheetState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BottomSheetNestedScrollConnectionTest {

  private val config =
      BottomSheetConfig(collapsedHeight = 100.dp, mediumHeight = 200.dp, fullHeight = 300.dp)

  @Test
  fun onPreScroll_consumesScrollWhenNotFull() = runTest {
    val animatable = Animatable(config.collapsedHeight.value)
    val connection =
        createBottomSheetNestedScrollConnection(
            density = Density(1f),
            config = config,
            currentHeight = animatable,
            scope = this,
            calculateTargetState = { _, _, _, _ -> BottomSheetState.COLLAPSED },
            stateToHeight = { config.collapsedHeight },
            onStateChange = {})

    val result = connection.onPreScroll(Offset(0f, -20f), NestedScrollSource.UserInput)

    // Should consume the scroll
    assertEquals(-20f, result.y, 0.001f)
  }

  @Test
  fun onPreScroll_atFullHeight_doesNotConsumeScroll() = runTest {
    val animatable = Animatable(config.fullHeight.value)
    val connection =
        createBottomSheetNestedScrollConnection(
            density = Density(1f),
            config = config,
            currentHeight = animatable,
            scope = this,
            calculateTargetState = { _, _, _, _ -> BottomSheetState.FULL },
            stateToHeight = { config.fullHeight },
            onStateChange = {})

    val result = connection.onPreScroll(Offset(0f, -20f), NestedScrollSource.UserInput)

    // Should not consume when at full height
    assertEquals(Offset.Zero, result)
  }

  @Test
  fun onPreScroll_withNonUserInputSource_doesNotConsumeScroll() = runTest {
    val animatable = Animatable(config.collapsedHeight.value)
    val connection =
        createBottomSheetNestedScrollConnection(
            density = Density(1f),
            config = config,
            currentHeight = animatable,
            scope = this,
            calculateTargetState = { _, _, _, _ -> BottomSheetState.COLLAPSED },
            stateToHeight = { config.collapsedHeight },
            onStateChange = {})

    val result = connection.onPreScroll(Offset(0f, -20f), NestedScrollSource.SideEffect)

    // Should not consume when source is not user input
    assertEquals(Offset.Zero, result)
  }

  @Test
  fun onPostScroll_withZeroDelta_doesNothing() = runTest {
    val animatable = Animatable(config.collapsedHeight.value)
    val connection =
        createBottomSheetNestedScrollConnection(
            density = Density(1f),
            config = config,
            currentHeight = animatable,
            scope = this,
            calculateTargetState = { _, _, _, _ -> BottomSheetState.COLLAPSED },
            stateToHeight = { config.collapsedHeight },
            onStateChange = {})

    val result = connection.onPostScroll(Offset.Zero, Offset.Zero, NestedScrollSource.UserInput)

    assertEquals(Offset.Zero, result)
  }

  @Test
  fun onPostScroll_consumesUnconsumedScroll() = runTest {
    val animatable = Animatable(config.collapsedHeight.value)
    val connection =
        createBottomSheetNestedScrollConnection(
            density = Density(1f),
            config = config,
            currentHeight = animatable,
            scope = this,
            calculateTargetState = { _, _, _, _ -> BottomSheetState.COLLAPSED },
            stateToHeight = { config.collapsedHeight },
            onStateChange = {})

    val result =
        connection.onPostScroll(Offset.Zero, Offset(0f, -10f), NestedScrollSource.UserInput)

    // Should consume the unconsumed scroll
    assertEquals(Offset(0f, -10f), result)
  }

  @Test
  fun onPostScroll_atFullHeight_withUpwardScroll_doesNotConsumeScroll() = runTest {
    val animatable = Animatable(config.fullHeight.value)
    val connection =
        createBottomSheetNestedScrollConnection(
            density = Density(1f),
            config = config,
            currentHeight = animatable,
            scope = this,
            calculateTargetState = { _, _, _, _ -> BottomSheetState.FULL },
            stateToHeight = { config.fullHeight },
            onStateChange = {})

    // Upward scroll (negative delta)
    val result =
        connection.onPostScroll(Offset.Zero, Offset(0f, -10f), NestedScrollSource.UserInput)

    // Should not consume upward scroll when at full height
    assertEquals(Offset.Zero, result)
  }

  @Test
  fun onPostScroll_atFullHeight_withDownwardScroll_consumesScroll() = runTest {
    val animatable = Animatable(config.fullHeight.value)
    val connection =
        createBottomSheetNestedScrollConnection(
            density = Density(1f),
            config = config,
            currentHeight = animatable,
            scope = this,
            calculateTargetState = { _, _, _, _ -> BottomSheetState.FULL },
            stateToHeight = { config.fullHeight },
            onStateChange = {})

    // Downward scroll (positive delta)
    val result = connection.onPostScroll(Offset.Zero, Offset(0f, 10f), NestedScrollSource.UserInput)

    // Should consume downward scroll to allow collapsing
    assertEquals(Offset(0f, 10f), result)
  }

  @Test
  fun onPreFling_weakVelocity_doesNotIntercept() = runTest {
    val animatable = Animatable(180f)
    val observedStates = mutableListOf<BottomSheetState>()
    val connection =
        createBottomSheetNestedScrollConnection(
            density = Density(1f),
            config = config,
            currentHeight = animatable,
            scope = this,
            calculateTargetState = { _, _, _, _ -> BottomSheetState.MEDIUM },
            stateToHeight = { config.mediumHeight },
            onStateChange = { observedStates.add(it) })

    val result = connection.onPreFling(Velocity(0f, 400f))

    // Should not intercept weak velocity
    assertEquals(0, observedStates.size)
    assertEquals(Velocity.Zero, result)
  }

  @Test
  fun onPreFling_strongVelocity_interceptsAndNotifiesStateChange() = runTest {
    val animatable = Animatable(180f)
    val observedStates = mutableListOf<BottomSheetState>()
    val connection =
        createBottomSheetNestedScrollConnection(
            density = Density(1f),
            config = config,
            currentHeight = animatable,
            scope = this,
            calculateTargetState = { _, _, _, _ -> BottomSheetState.MEDIUM },
            stateToHeight = { config.mediumHeight },
            onStateChange = { observedStates.add(it) })

    val result = connection.onPreFling(Velocity(0f, 600f))

    // Should intercept and notify state change
    assertEquals(1, observedStates.size)
    assertEquals(BottomSheetState.MEDIUM, observedStates.single())
    assertEquals(600f, result.y, 0.001f)
  }

  @Test
  fun onPreFling_atCollapsed_doesNotIntercept() = runTest {
    val animatable = Animatable(config.collapsedHeight.value)
    val observedStates = mutableListOf<BottomSheetState>()
    val connection =
        createBottomSheetNestedScrollConnection(
            density = Density(1f),
            config = config,
            currentHeight = animatable,
            scope = this,
            calculateTargetState = { _, _, _, _ -> BottomSheetState.COLLAPSED },
            stateToHeight = { config.collapsedHeight },
            onStateChange = { observedStates.add(it) })

    val result = connection.onPreFling(Velocity(0f, 600f))

    // Should not intercept at collapsed
    assertEquals(0, observedStates.size)
    assertEquals(Velocity.Zero, result)
  }

  @Test
  fun onPreFling_atFull_doesNotIntercept() = runTest {
    val animatable = Animatable(config.fullHeight.value)
    val observedStates = mutableListOf<BottomSheetState>()
    val connection =
        createBottomSheetNestedScrollConnection(
            density = Density(1f),
            config = config,
            currentHeight = animatable,
            scope = this,
            calculateTargetState = { _, _, _, _ -> BottomSheetState.FULL },
            stateToHeight = { config.fullHeight },
            onStateChange = { observedStates.add(it) })

    val result = connection.onPreFling(Velocity(0f, 600f))

    // Should not intercept at full
    assertEquals(0, observedStates.size)
    assertEquals(Velocity.Zero, result)
  }
}
