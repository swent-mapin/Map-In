package com.swent.mapin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.swent.mapin.model.PreferencesRepositoryProvider
import com.swent.mapin.model.event.EventRepositoryProvider
import com.swent.mapin.model.memory.MemoryRepositoryProvider
import com.swent.mapin.navigation.AppNavHost
import com.swent.mapin.notifications.FCMTokenManager
import com.swent.mapin.ui.settings.ThemeMode
import com.swent.mapin.ui.theme.MapInTheme
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

object HttpClientProvider {
  var client: OkHttpClient = OkHttpClient()
}

/**
 * Main activity of the app.* Role: - Android entry point that hosts the Jetpack Compose UI. -
 * Applies the Material 3 theme and shows the map screen.
 */
class MainActivity : ComponentActivity() {

  // Store deep link as mutable state so it triggers recomposition
  private var pendingDeepLink by mutableStateOf<String?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Extract deep link from notification intent
    // Check both "actionUrl" (from Firebase) and "action_url" (from PendingIntent)
    val actionUrl = intent?.getStringExtra("actionUrl") ?: intent?.getStringExtra("action_url")
    if (actionUrl != null) {
      Log.d("MainActivity", "Deep link from notification: $actionUrl")
      pendingDeepLink = actionUrl
    }

    enableEdgeToEdge()
    MemoryRepositoryProvider.setRepository(MemoryRepositoryProvider.createLocalRepository())

    // Initialize EventRepositoryFirestore (uncomment to use Firestore backend)
    EventRepositoryProvider.init(this)
    EventRepositoryProvider.getRepository()

    // Initialize FCM for already logged-in users (when app restarts with active session)
    if (FirebaseAuth.getInstance().currentUser != null) {
      lifecycleScope.launch {
        try {
          val fcmManager = FCMTokenManager()
          fcmManager.initializeForCurrentUser()
        } catch (e: Exception) {
          Log.e("MainActivity", "Failed to initialize FCM for logged-in user", e)
        }
      }
    }

    setContent {
      val preferencesRepository = remember { PreferencesRepositoryProvider.getInstance(this) }
      // Cache the theme mode flow collection to prevent repeated DataStore reads
      val themeModeString by
          remember(preferencesRepository) { preferencesRepository.themeModeFlow }
              .collectAsState(initial = "system")
      val themeMode = ThemeMode.fromString(themeModeString)

      val darkTheme =
          when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
          }

      MapInTheme(darkTheme = darkTheme) {
        // Check if user is already authenticated with Firebase
        val isLoggedIn = FirebaseAuth.getInstance().currentUser != null

        AppNavHost(isLoggedIn = isLoggedIn, deepLink = pendingDeepLink)
      }
    }
  }

  /**
   * Called when activity receives a new intent (e.g., from notification while app is already
   * running).
   */
  override fun onNewIntent(newIntent: Intent) {
    super.onNewIntent(newIntent)
    setIntent(newIntent)

    // Check both keys for compatibility
    val actionUrl = newIntent.getStringExtra("actionUrl") ?: newIntent.getStringExtra("action_url")
    actionUrl?.let {
      Log.d("MainActivity", "Deep link from new intent: $it")
      pendingDeepLink = it
      recreate()
    }
  }
}
