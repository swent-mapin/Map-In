package com.swent.mapin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.swent.mapin.ui.map.MapScreen
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
    enableEdgeToEdge()
    setContent { MapInTheme { MapScreen() } }
  }
}
