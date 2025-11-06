package com.swent.mapin.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

// Assisted by AI
/**
 * Configuration for the bottom sheet behavior. Defines the three anchor heights that the sheet can
 * snap to.
 *
 * @property collapsedHeight Height when sheet is collapsed
 * @property mediumHeight Height when sheet is at medium state
 * @property fullHeight Height when sheet is fully expanded
 */
data class BottomSheetConfig(val collapsedHeight: Dp, val mediumHeight: Dp, val fullHeight: Dp)

/**
 * Reusable bottom sheet with continuous content reveal, no content switching. Dragging follows
 * finger movement, then snaps to nearest state/height on release. Doesn't block map interaction
 * unless fully expanded.
 *
 * @param modifier Optional modifier for the root composable
 * @param config Configuration for sheet heights
 * @param currentState Current state of the bottom sheet
 * @param onStateChange Callback when state should change
 * @param calculateTargetState Function to calculate target state based on drag position
 * @param stateToHeight Function to map state to height
 * @param onHeightChange Callback that provides the current animated height value
 * @param content Composable content (rendered once at full size, clipped by sheet height)
 */
@Composable
fun <T> BottomSheet(
    modifier: Modifier = Modifier,
    config: BottomSheetConfig,
    currentState: T,
    onStateChange: (T) -> Unit,
    calculateTargetState: (Float, Float, Float, Float) -> T,
    stateToHeight: (T) -> Dp,
    onHeightChange: (Dp) -> Unit = {},
    content: @Composable () -> Unit
) {
  val density = LocalDensity.current
  val scope = rememberCoroutineScope()

  val targetHeight = stateToHeight(currentState)

  val currentHeight = remember { Animatable(config.collapsedHeight.value) }

  // When state changes externally (here, search tap), animate smoothly to new height
  LaunchedEffect(targetHeight) {
    currentHeight.animateTo(
        targetValue = targetHeight.value.coerceAtMost(config.fullHeight.value),
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f))
  }

  // Report every height change to parent (here, for gradual map scrim )
  LaunchedEffect(currentHeight.value) { onHeightChange(currentHeight.value.dp) }

  // Nested scroll connection to handle scrolling in content
  val nestedScrollConnection =
      remember(density, config, currentHeight) {
        createBottomSheetNestedScrollConnection(
            density = density,
            config = config,
            currentHeight = currentHeight,
            scope = scope,
            calculateTargetState = calculateTargetState,
            stateToHeight = stateToHeight,
            onStateChange = onStateChange)
      }

  Surface(
      modifier =
          modifier
              .fillMaxWidth()
              .height(currentHeight.value.dp)
              .nestedScroll(nestedScrollConnection)
              .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                      scope.launch {
                        val currentHeightPx = currentHeight.value * density.density
                        val collapsedPx = config.collapsedHeight.toPx()
                        val mediumPx = config.mediumHeight.toPx()
                        val fullPx = config.fullHeight.toPx()

                        val targetState =
                            calculateTargetState(currentHeightPx, collapsedPx, mediumPx, fullPx)
                        val targetHeightValue = stateToHeight(targetState).value

                        onStateChange(targetState)

                        currentHeight.animateTo(
                            targetValue = targetHeightValue.coerceAtMost(config.fullHeight.value),
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = 500f))
                      }
                    },
                    onVerticalDrag = { _, dragAmount ->
                      scope.launch {
                        val newHeight = (currentHeight.value - dragAmount / density.density)
                        // Small overscroll allowance for natural feel without scroll state bugs
                        val minHeight = config.collapsedHeight.value - 8f
                        val maxHeight = config.fullHeight.value + 8f
                        currentHeight.snapTo(newHeight.coerceIn(minHeight, maxHeight))
                      }
                    })
              },
      shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
      shadowElevation = 8.dp,
      color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxWidth().clipToBounds().padding(horizontal = 16.dp)) {
          Box(
              modifier =
                  Modifier.padding(vertical = 8.dp)
                      .width(40.dp)
                      .height(4.dp)
                      .background(Color.Gray.copy(alpha = 0.3f), shape = RoundedCornerShape(2.dp))
                      .align(Alignment.CenterHorizontally))

          content()
        }
      }
}

/**
 * Creates a nested scroll connection for the bottom sheet that handles pre-scroll, post-scroll, and
 * fling gestures to prioritize sheet expansion over content scrolling.
 *
 * This function is extracted for testability.
 */
fun <T> createBottomSheetNestedScrollConnection(
    density: Density,
    config: BottomSheetConfig,
    currentHeight: Animatable<Float, *>,
    scope: kotlinx.coroutines.CoroutineScope,
    calculateTargetState: (Float, Float, Float, Float) -> T,
    stateToHeight: (T) -> Dp,
    onStateChange: (T) -> Unit
): NestedScrollConnection {
  return object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
      // Only handle drag gestures (not flings)
      if (source != NestedScrollSource.UserInput) return Offset.Zero

      val delta = available.y
      val currentHeightValue = currentHeight.value

      // If sheet is not at full height, prioritize expanding the sheet
      // over allowing content to scroll
      if (currentHeightValue < config.fullHeight.value) {
        // Always consume scroll to expand sheet when not full
        scope.launch {
          val newHeight = (currentHeightValue - delta / density.density)
          // Small overscroll allowance for natural feel without scroll state bugs
          val minHeight = config.collapsedHeight.value - 8f
          val maxHeight = config.fullHeight.value + 8f
          currentHeight.snapTo(newHeight.coerceIn(minHeight, maxHeight))
        }
        // Consume the scroll so content doesn't scroll
        return Offset(0f, delta)
      }

      return Offset.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
      // Only handle drag gestures (not flings)
      if (source != NestedScrollSource.UserInput) return Offset.Zero

      val delta = available.y
      if (delta == 0f) return Offset.Zero

      val currentHeightValue = currentHeight.value

      // If sheet is at full height and scroll is upward (negative delta),
      // don't consume it - prevents sheet from exceeding full height
      if (currentHeightValue >= config.fullHeight.value && delta < 0f) {
        return Offset.Zero
      }

      // If there's unconsumed scroll, apply it to the sheet
      scope.launch {
        val newHeight = (currentHeightValue - delta / density.density)
        // Small overscroll allowance for natural feel without scroll state bugs
        val minHeight = config.collapsedHeight.value - 8f
        val maxHeight = config.fullHeight.value + 8f
        currentHeight.snapTo(newHeight.coerceIn(minHeight, maxHeight))
      }

      return Offset(0f, delta)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
      // Handle fling gestures
      val velocityY = available.y

      // Only intercept strong downward flings while the sheet isn't already fully expanded
      if (velocityY > 500f &&
          currentHeight.value > config.collapsedHeight.value &&
          currentHeight.value < config.fullHeight.value) {
        val currentHeightPx = currentHeight.value * density.density
        val collapsedPx = config.collapsedHeight.value * density.density
        val mediumPx = config.mediumHeight.value * density.density
        val fullPx = config.fullHeight.value * density.density

        val targetState = calculateTargetState(currentHeightPx, collapsedPx, mediumPx, fullPx)
        onStateChange(targetState)

        return Velocity(0f, velocityY) // Consume the fling
      }

      return Velocity.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
      // After content finishes flinging, snap sheet to nearest state
      val currentHeightPx = currentHeight.value * density.density
      val collapsedPx = config.collapsedHeight.value * density.density
      val mediumPx = config.mediumHeight.value * density.density
      val fullPx = config.fullHeight.value * density.density

      val targetState = calculateTargetState(currentHeightPx, collapsedPx, mediumPx, fullPx)
      val targetHeightValue = stateToHeight(targetState).value

      onStateChange(targetState)

      currentHeight.animateTo(
          targetValue = targetHeightValue.coerceAtMost(config.fullHeight.value),
          animationSpec = spring(dampingRatio = 0.85f, stiffness = 500f))

      return available
    }
  }
}
