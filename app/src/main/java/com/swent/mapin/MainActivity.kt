package com.swent.mapin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.google.firebase.auth.FirebaseAuth
import com.swent.mapin.model.PreferencesRepositoryProvider
import com.swent.mapin.model.event.EventRepositoryProvider
import com.swent.mapin.model.memory.MemoryRepositoryProvider
import com.swent.mapin.navigation.AppNavHost
import com.swent.mapin.ui.settings.ThemeMode
import com.swent.mapin.ui.theme.MapInTheme
import okhttp3.OkHttpClient

object HttpClientProvider {
  var client: OkHttpClient = OkHttpClient()
}

/**
 * Main activity of the app.* Role: - Android entry point that hosts the Jetpack Compose UI. -
 * Applies the Material 3 theme and shows the map screen.
 */
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    MemoryRepositoryProvider.setRepository(MemoryRepositoryProvider.createLocalRepository())

    // Initialize EventRepositoryFirestore (uncomment to use Firestore backend)
    EventRepositoryProvider.init(this)
    EventRepositoryProvider.getRepository()
    
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
        AppNavHost(isLoggedIn = isLoggedIn)
      }
    }
  }
}
