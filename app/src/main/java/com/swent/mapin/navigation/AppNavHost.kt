package com.swent.mapin.navigation

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.swent.mapin.ui.map.MapScreen
import com.swent.mapin.ui.profile.ProfileScreen
import com.swent.mapin.ui.settings.ChangePasswordScreen
import com.swent.mapin.ui.settings.SettingsScreen

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    isLoggedIn: Boolean,
    deepLink: String? = null,
    renderMap: Boolean = true, // Set to false in instrumented tests to skip Mapbox rendering
    autoRequestPermissions: Boolean = true // Set to false in tests to skip permission dialogs
) {
  val startDest = if (isLoggedIn) Route.Map.route else Route.Auth.route

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

  // Handle deep link navigation when received
  LaunchedEffect(deepLink) {
    Log.d("DEEPLINK", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    Log.d("DEEPLINK", "🎯 LaunchedEffect triggered")
    Log.d("DEEPLINK", "🔗 deepLink parameter: '$deepLink'")

    if (deepLink == null) {
      Log.d("DEEPLINK", "⚠️ Deep link is null, no navigation")
      Log.d("DEEPLINK", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
      return@LaunchedEffect
    }

    // Wait for NavHost to be fully initialized (critical for cold start)
    kotlinx.coroutines.delay(500)
    Log.d("DEEPLINK", "⏰ Waited 500ms for NavHost initialization")

    Log.d("DEEPLINK", "🚀 Processing deep link...")
    val route = DeepLinkHandler.parseDeepLink(deepLink)

    if (route == null) {
      Log.w("DEEPLINK", "❌ Parser returned null")
      Log.d("DEEPLINK", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
      return@LaunchedEffect
    }

    Log.d("DEEPLINK", "📍 Parsed route: '$route'")

    try {
      navController.navigate(route) { launchSingleTop = true }
      Log.d("DEEPLINK", "✅ NAVIGATION SUCCESS → $route")
      Log.d("DEEPLINK", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    } catch (e: Exception) {
      Log.e("DEEPLINK", "❌ NAVIGATION FAILED", e)
      Log.d("DEEPLINK", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
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

    composable(
        route = "friends?tab={tab}",
        arguments =
            listOf(
                androidx.navigation.navArgument("tab") {
                  type = androidx.navigation.NavType.StringType
                  nullable = true
                  defaultValue = null
                })) { backStackEntry ->
          val tab = backStackEntry.arguments?.getString("tab")
          val viewModel: com.swent.mapin.ui.friends.FriendsViewModel =
              androidx.lifecycle.viewmodel.compose.viewModel()

          // Set the tab when we have a deep link tab parameter
          LaunchedEffect(tab) {
            if (tab == "REQUESTS") {
              viewModel.selectTab(com.swent.mapin.ui.friends.FriendsTab.REQUESTS)
            }
          }

          FriendsScreen(onNavigateBack = { safePopBackStack() }, viewModel = viewModel)
        }

    // Also keep the simple friends route for backward compatibility
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
