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
 */
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent { MapInTheme { MapScreen() } }
  }
}
