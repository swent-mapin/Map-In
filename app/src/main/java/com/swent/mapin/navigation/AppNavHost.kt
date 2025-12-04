package com.swent.mapin.navigation

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.swent.mapin.ui.auth.SignInScreen
import com.swent.mapin.ui.chat.ChatScreen
import com.swent.mapin.ui.chat.ConversationScreen
import com.swent.mapin.ui.chat.NewConversationScreen
import com.swent.mapin.ui.friends.FriendsScreen
import com.swent.mapin.ui.friends.FriendsTab
import com.swent.mapin.ui.friends.FriendsViewModel
import com.swent.mapin.ui.map.MapScreen
import com.swent.mapin.ui.profile.ProfileScreen
import com.swent.mapin.ui.settings.ChangePasswordScreen
import com.swent.mapin.ui.settings.SettingsScreen

/**
 * Extracts an event ID from a deep link URL using proper URI parsing.
 *
 * Supports the format: mapin://events/{eventId}
 *
 * @return event ID when URL matches mapin://events/{id}, otherwise null.
 */
internal fun parseDeepLinkEventId(deepLinkUrl: String?): String? {
  if (deepLinkUrl == null) return null

  return try {
    val uri = Uri.parse(deepLinkUrl)
    if (uri.scheme == "mapin" && uri.host == "events") {
      val eventId = uri.pathSegments?.firstOrNull()
      eventId ?: uri.path?.removePrefix("/")?.takeIf { it.isNotEmpty() }
    } else {
      null
    }
  } catch (e: Exception) {
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
    renderMap: Boolean = true,
    deepLink: String? = null,
    autoRequestPermissions: Boolean = true
) {
  val startDest = if (isLoggedIn) Route.Map.route else Route.Auth.route

  // Track current deep link being processed
  var currentDeepLinkEventId by remember { mutableStateOf<String?>(null) }

  // Track pending friends tab for deep link navigation
  var pendingFriendsTab by remember { mutableStateOf<String?>(null) }

  // Process deep link with LaunchedEffect
  LaunchedEffect(deepLink) {
    Log.d("DEEPLINK", "=== LaunchedEffect triggered ===")
    Log.d("DEEPLINK", "Deep link: $deepLink")

    if (deepLink != null) {
      // Small delay to ensure NavHost is initialized (critical for cold start)
      kotlinx.coroutines.delay(500)

      Log.d("DEEPLINK", "Processing URL: $deepLink")

      val route = DeepLinkHandler.parseDeepLink(deepLink)
      Log.d("DEEPLINK", "Route parsed: $route")

      if (route != null) {
        try {
          // Handle special cases for event deep links
          val eventId = parseDeepLinkEventId(deepLink)
          if (eventId != null) {
            Log.d("DEEPLINK", "-> Event deep link with ID: $eventId")
            currentDeepLinkEventId = eventId
          } else if (route.startsWith("friends?tab=")) {
            // Extract tab from route
            val tab = route.substringAfter("friends?tab=")
            Log.d("DEEPLINK", "-> Friends tab: $tab")
            pendingFriendsTab = tab
            navController.navigate(Route.Friends.route) { launchSingleTop = true }
          } else {
            Log.d("DEEPLINK", "-> Navigating to: $route")
            navController.navigate(route) { launchSingleTop = true }
          }
          Log.d("DEEPLINK", "-> Navigation SUCCESS")
        } catch (e: Exception) {
          Log.e("DEEPLINK", "-> Navigation FAILED", e)
        }
      } else {
        Log.d("DEEPLINK", "-> Unknown deep link type")
      }
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
              popUpTo(navController.graph.startDestinationId) { inclusive = true }
              launchSingleTop = true
            }
          },
          onNavigateToFriends = { navController.navigate(Route.Friends.route) })
    }

    composable(Route.Settings.route) {
      val passwordChangeResult =
          navController.currentBackStackEntry?.savedStateHandle?.get<Boolean>("password_changed")

      SettingsScreen(
          onNavigateBack = { safePopBackStack() },
          onNavigateToSignIn = {
            navController.navigate(Route.Auth.route) {
              popUpTo(navController.graph.startDestinationId) { inclusive = true }
              launchSingleTop = true
            }
          },
          onNavigateToChangePassword = { navController.navigate(Route.ChangePassword.route) },
          passwordChangeSuccess = passwordChangeResult)

      if (passwordChangeResult != null) {
        navController.currentBackStackEntry?.savedStateHandle?.remove<Boolean>("password_changed")
      }
    }

    composable(Route.ChangePassword.route) {
      ChangePasswordScreen(
          onNavigateBack = { safePopBackStack() },
          onPasswordChanged = {
            navController.previousBackStackEntry?.savedStateHandle?.set("password_changed", true)
            safePopBackStack()
          })
    }

    composable(Route.Friends.route) {
      val viewModel: FriendsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

      // Handle deep link tab selection
      LaunchedEffect(pendingFriendsTab) {
        Log.d("DEEPLINK", "FriendsScreen LaunchedEffect - pendingTab: $pendingFriendsTab")
        when (pendingFriendsTab) {
          "REQUESTS" -> {
            Log.d("DEEPLINK", "-> Selecting REQUESTS tab")
            viewModel.selectTab(FriendsTab.REQUESTS)
          }
          "FRIENDS" -> {
            Log.d("DEEPLINK", "-> Selecting FRIENDS tab")
            viewModel.selectTab(FriendsTab.FRIENDS)
          }
        }
        pendingFriendsTab = null
      }

      FriendsScreen(onNavigateBack = { safePopBackStack() }, viewModel = viewModel)
    }

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
