package com.swent.mapin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.swent.mapin.navigation.AppNavHost
import com.swent.mapin.ui.theme.MapInTheme

/**
 * Main activity of the app.
 *
 * Role: \- Android entry point that hosts the Jetpack Compose UI. \- Applies the Material 3 theme
 * and shows the map screen.
 */
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MapInTheme {
        // replace with real auth (e.g., FirebaseAuth.getInstance().currentUser != null)
        var isLoggedIn by remember { mutableStateOf(false) }
        AppNavHost(isLoggedIn = isLoggedIn)
      }
    }
  }
}
