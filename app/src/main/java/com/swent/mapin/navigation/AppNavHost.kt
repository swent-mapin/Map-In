package com.swent.mapin.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.swent.mapin.ui.auth.SignInScreen
import com.swent.mapin.ui.chat.ChatScreen
import com.swent.mapin.ui.chat.ConversationScreen
import com.swent.mapin.ui.chat.NewConversationScreen
import com.swent.mapin.ui.friends.FriendsScreen
import com.swent.mapin.ui.map.MapScreen
import com.swent.mapin.ui.memory.MemoriesScreen
import com.swent.mapin.ui.profile.ProfileScreen
import com.swent.mapin.ui.settings.ChangePasswordScreen
import com.swent.mapin.ui.settings.SettingsScreen

/**
 * Extracts an event ID from a deep link URL using proper URI parsing.
 *
 * Supports the format: mapin://events/{eventId} Extensible for future deep link types (e.g.,
 * mapin://profile/{userId})
 *
 * @return event ID when URL matches mapin://events/{id}, otherwise null.
 */
internal fun parseDeepLinkEventId(deepLinkUrl: String?): String? {
  if (deepLinkUrl == null) return null

  return try {
    val uri = Uri.parse(deepLinkUrl)
    // Check scheme and host
    if (uri.scheme == "mapin" && uri.host == "events") {
      // Extract event ID from path segments (works in instrumented tests)
      val eventId = uri.pathSegments?.firstOrNull()
      // Fallback for unit tests where pathSegments might be empty
      eventId ?: uri.path?.removePrefix("/")?.takeIf { it.isNotEmpty() }
    } else {
      null
    }
  } catch (e: Exception) {
    // Fallback to string parsing if Uri.parse fails or isn't available
    if (deepLinkUrl.startsWith("mapin://events/")) {
      deepLinkUrl.substringAfter("mapin://events/").takeIf { it.isNotEmpty() }
    } else {
      null
    }
  }
}

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    isLoggedIn: Boolean,
    renderMap: Boolean = true, // Set to false in instrumented tests to skip Mapbox rendering
    deepLinkQueue: SnapshotStateList<String> = remember {
      androidx.compose.runtime.mutableStateListOf()
    }, // Queue of deep links to process
    autoRequestPermissions: Boolean = true // Set to false in tests to skip permission dialogs
) {
  val startDest = if (isLoggedIn) Route.Map.route else Route.Auth.route

  // Track current deep link being processed
  var currentDeepLinkEventId by remember { mutableStateOf<String?>(null) }

  // Process deep links from queue with LaunchedEffect
  LaunchedEffect(deepLinkQueue.size) {
    if (deepLinkQueue.isNotEmpty()) {
      val deepLinkUrl = deepLinkQueue.removeAt(0)
      currentDeepLinkEventId = parseDeepLinkEventId(deepLinkUrl)
    }
  }

  // Debounce navigation to prevent double-click issues
  var lastNavigationTime by remember { mutableLongStateOf(0L) }
  val navigationDebounceMs = 500L

  fun safePopBackStack() {
    val currentTime = System.currentTimeMillis()
    if (currentTime - lastNavigationTime > navigationDebounceMs) {
      lastNavigationTime = currentTime
      navController.popBackStack()
    }
  }

  NavHost(navController = navController, startDestination = startDest) {
    composable(Route.Auth.route) {
      SignInScreen(
          onSignInSuccess = {
            navController.navigate(Route.Map.route) {
              popUpTo(Route.Auth.route) { inclusive = true }
              launchSingleTop = true
            }
          })
    }

    composable(Route.Map.route) {
      MapScreen(
          onNavigateToProfile = { navController.navigate(Route.Profile.route) },
          onNavigateToSettings = { navController.navigate(Route.Settings.route) },
          onNavigateToFriends = { navController.navigate(Route.Friends.route) },
          onNavigateToMemories = { navController.navigate(Route.Memories.route) },
          onNavigateToChat = { navController.navigate(Route.Chat.route) },
          renderMap = renderMap,
          deepLinkEventId = currentDeepLinkEventId,
          onDeepLinkConsumed = { currentDeepLinkEventId = null },
          autoRequestPermissions = autoRequestPermissions)
    }

    composable(Route.Profile.route) {
      ProfileScreen(
          onNavigateBack = { safePopBackStack() },
          onNavigateToSettings = { navController.navigate(Route.Settings.route) },
          onNavigateToSignIn = {
            navController.navigate(Route.Auth.route) {
              // Clear the whole back stack by popping up to the nav graph's start destination
              popUpTo(navController.graph.startDestinationId) { inclusive = true }
              launchSingleTop = true
            }
          },
          onNavigateToFriends = { navController.navigate(Route.Friends.route) })
    }

    composable(Route.Settings.route) {
      // Check if returning from password change with success result
      val passwordChangeResult =
          navController.currentBackStackEntry?.savedStateHandle?.get<Boolean>("password_changed")

      SettingsScreen(
          onNavigateBack = { safePopBackStack() },
          onNavigateToSignIn = {
            navController.navigate(Route.Auth.route) {
              // Clear the whole back stack by popping up to the nav graph's start destination
              popUpTo(navController.graph.startDestinationId) { inclusive = true }
              launchSingleTop = true
            }
          },
          onNavigateToChangePassword = { navController.navigate(Route.ChangePassword.route) },
          passwordChangeSuccess = passwordChangeResult)

      // Clear the result after reading it
      if (passwordChangeResult != null) {
        navController.currentBackStackEntry?.savedStateHandle?.remove<Boolean>("password_changed")
      }
    }

    composable(Route.ChangePassword.route) {
      ChangePasswordScreen(
          onNavigateBack = { safePopBackStack() },
          onPasswordChanged = {
            // Set result to communicate success back to settings
            navController.previousBackStackEntry?.savedStateHandle?.set("password_changed", true)
            // Navigate back to settings
            safePopBackStack()
          })
    }
    composable(Route.Memories.route) { MemoriesScreen(onNavigateBack = { safePopBackStack() }) }

    composable(Route.Friends.route) { FriendsScreen(onNavigateBack = { safePopBackStack() }) }

    composable(Route.Chat.route) {
      ChatScreen(
          onNavigateBack = { safePopBackStack() },
          onNewConversation = { navController.navigate(Route.NewConversation.route) },
          onOpenConversation = { conversation ->
            val encodedName = Uri.encode(conversation.name)
            navController.navigate("conversation/${conversation.id}/${encodedName}")
          },
          onTabSelected = { chatTab -> navController.navigate(chatTab.destination) })
    }

    composable(Route.NewConversation.route) {
      NewConversationScreen(
          onNavigateBack = { safePopBackStack() },
          onConfirm = {
            navController.navigate(Route.Chat.route) {
              popUpTo(Route.Chat.route) { inclusive = true }
              launchSingleTop = true
            }
          })
    }
    composable("conversation/{conversationId}/{name}") { backStackEntry ->
      val conversationId =
          backStackEntry.arguments?.getString("conversationId") ?: return@composable
      val encodedName = backStackEntry.arguments?.getString("name") ?: ""
      val name = Uri.decode(encodedName)

      ConversationScreen(
          conversationId = conversationId,
          conversationName = name,
          onNavigateBack = { safePopBackStack() })
    }
  }
}
