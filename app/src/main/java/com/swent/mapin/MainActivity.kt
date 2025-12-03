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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
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
 * Main activity of the app. Role: - Android entry point that hosts the Jetpack Compose UI. -
 * Applies the Material 3 theme and shows the map screen.
 */
class MainActivity : ComponentActivity() {
  // Use a queue to handle multiple deep links instead of overwriting
  private val deepLinkQueue = mutableStateListOf<String>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
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

    // Extract deep link from initial intent and add to queue
    getDeepLinkUrlFromIntent(intent)?.let {
      Log.d("MainActivity", "Deep link from notification: $it")
      deepLinkQueue.add(it)
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
        AppNavHost(isLoggedIn = isLoggedIn, deepLinkQueue = deepLinkQueue)
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    // Handle deep links when app is already running - add to queue
    getDeepLinkUrlFromIntent(intent)?.let {
      Log.d("MainActivity", "Deep link from new intent: $it")
      deepLinkQueue.add(it)
    }
  }
}

/** Extracts deep link URL from intent, checking both Firebase and PendingIntent keys. */
internal fun getDeepLinkUrlFromIntent(intent: Intent?): String? {
  // Check both "actionUrl" (from Firebase) and "action_url" (from PendingIntent)
  return intent?.getStringExtra("actionUrl") ?: intent?.getStringExtra("action_url")
}
