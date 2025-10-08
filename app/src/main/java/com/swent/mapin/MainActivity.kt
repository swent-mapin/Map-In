package com.swent.mapin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.swent.mapin.map.MapScreen
import com.swent.mapin.ui.theme.MapInTheme

/**
 * Main activity of the app.
 *
 * Role:
 * \- Android entry point that hosts the Jetpack Compose UI.
 * \- Applies the Material 3 theme and shows the map screen.
 */
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Set the Compose UI tree for this activity.
    setContent {
      // Apply the app theme (colors, typography, etc.).
      MapInTheme {
        // Themed surface that provides background color and elevation.
        Surface(
          modifier = Modifier.fillMaxSize(),              // Occupies the full screen.
          color = MaterialTheme.colorScheme.background    // Uses the themed background color.
        ) {
          // Main composable that displays the Google Map.
          MapScreen()
        }
      }
    }
  }
}
