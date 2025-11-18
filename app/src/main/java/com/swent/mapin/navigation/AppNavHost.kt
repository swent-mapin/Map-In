package com.swent.mapin.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
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
    renderMap: Boolean =
        true // Set to false in instrumented tests to skip Mapbox rendering entirely (all styles)
) {
  val startDest = if (isLoggedIn) Route.Map.route else Route.Auth.route

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
          renderMap = renderMap)
    }

    composable(Route.Profile.route) {
      ProfileScreen(
          onNavigateBack = { navController.popBackStack() },
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
          onNavigateBack = { navController.popBackStack() },
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
          onNavigateBack = { navController.popBackStack() },
          onPasswordChanged = {
            // Set result to communicate success back to settings
            navController.previousBackStackEntry?.savedStateHandle?.set("password_changed", true)
            // Navigate back to settings
            navController.popBackStack()
          })
    }

    composable(Route.Friends.route) {
      FriendsScreen(onNavigateBack = { navController.popBackStack() })
    }

    composable(Route.Chat.route) {
      ChatScreen(
          onNavigateBack = { navController.popBackStack() },
          onNewConversation = { navController.navigate(Route.NewConversation.route) },
          onOpenConversation = { conversation ->
            val encodedName = Uri.encode(conversation.name)
            navController.navigate("conversation/${conversation.id}/${encodedName}")
          },
          onTabSelected = { chatTab -> navController.navigate(chatTab.destination) })
    }

    composable(Route.NewConversation.route) {
      NewConversationScreen(
          onNavigateBack = { navController.popBackStack() },
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
          onNavigateBack = { navController.popBackStack() })
    }
  }
}
