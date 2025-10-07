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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.swent.mapin.ui.map.MapConstants
import kotlinx.coroutines.launch

/**
 * Configuration for DraggableBottomSheet behavior. Defines the three anchor heights that the sheet
 * can snap to.
 *
 * @property collapsedHeight Height when sheet is collapsed
 * @property mediumHeight Height when sheet is at medium state
 * @property fullHeight Height when sheet is fully expanded
 */
data class BottomSheetConfig(val collapsedHeight: Dp, val mediumHeight: Dp, val fullHeight: Dp)

/**
 * Reusable draggable bottom sheet with continuous content reveal, no content switching. Dragging
 * follows finger movement, then snaps to nearest state/height on release.
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
fun <T> DraggableBottomSheet(
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
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 280f))
  }

  // Report every height change to parent (here, for gradual map scrim )
  LaunchedEffect(currentHeight.value) { onHeightChange(currentHeight.value.dp) }

  Surface(
      modifier =
          modifier.fillMaxWidth().height(currentHeight.value.dp).pointerInput(Unit) {
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
                        animationSpec = spring(dampingRatio = 0.85f, stiffness = 280f))
                  }
                },
                onVerticalDrag = { _, dragAmount ->
                  scope.launch {
                    val newHeight = (currentHeight.value - dragAmount / density.density)
                    val minHeight =
                        config.collapsedHeight.value - MapConstants.OVERSCROLL_ALLOWANCE_DP
                    val maxHeight = config.fullHeight.value + MapConstants.OVERSCROLL_ALLOWANCE_DP
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
