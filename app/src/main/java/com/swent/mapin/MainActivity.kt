package com.swent.mapin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.swent.mapin.navigation.AppNavHost

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      Surface(color = MaterialTheme.colorScheme.background) {
        val navController = rememberNavController()

        // replace with real auth (e.g., FirebaseAuth.getInstance().currentUser != null)
        var isLoggedIn by remember { mutableStateOf(false) }

        AppNavHost(
            navController = navController,
            isLoggedIn = isLoggedIn,
        )
      }
    }
  }
}
