package com.swent.mapin.ui.map.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.swent.mapin.ui.map.BottomSheetState
import com.swent.mapin.ui.map.MapConstants

/** Top gradient overlay for better visibility of status bar icons on map */
@Composable
fun TopGradient() {
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .height(100.dp)
              .background(
                  brush =
                      Brush.verticalGradient(
                          colors = listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent))))
}

/** Scrim that fades in once the sheet passes MEDIUM, dimming the map underneath. */
@Composable
fun ScrimOverlay(currentHeightDp: Dp, mediumHeightDp: Dp, fullHeightDp: Dp) {
  val opacity =
      if (currentHeightDp >= mediumHeightDp) {
        val progress =
            ((currentHeightDp - mediumHeightDp) / (fullHeightDp - mediumHeightDp)).coerceIn(0f, 1f)
        progress * MapConstants.MAX_SCRIM_ALPHA
      } else {
        0f
      }

  Box(
      modifier =
          Modifier.fillMaxSize()
              .testTag("scrimOverlay")
              .background(Color.Black.copy(alpha = opacity)))
}

/** Transparent overlay that consumes all map gestures while the sheet is fully expanded. */
@SuppressLint("ReturnFromAwaitPointerEventScope")
@Composable
fun MapInteractionBlocker() {
  Box(
      modifier =
          Modifier.fillMaxSize().testTag("mapInteractionBlocker").pointerInput(Unit) {
            awaitPointerEventScope {
              while (true) {
                val event = awaitPointerEvent()
                event.changes.forEach { pointerInputChange -> pointerInputChange.consume() }
              }
            }
          })
}

/**
 * Conditionally renders a map interaction blocker when the sheet is fully expanded.
 *
 * Prevents map gestures from interfering with sheet content interaction.
 */
@Composable
fun ConditionalMapBlocker(bottomSheetState: BottomSheetState) {
  if (bottomSheetState == BottomSheetState.FULL) {
    MapInteractionBlocker()
  }
}
