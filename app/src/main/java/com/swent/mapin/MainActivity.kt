package com.swent.mapin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.firebase.auth.FirebaseAuth
import com.swent.mapin.navigation.AppNavHost
import com.swent.mapin.ui.theme.MapInTheme
import okhttp3.OkHttpClient


object HttpClientProvider {
  var client: OkHttpClient = OkHttpClient()
}

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
        // Check if user is already authenticated with Firebase
        val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
        AppNavHost(isLoggedIn = isLoggedIn)
      }
    }
  }
}
