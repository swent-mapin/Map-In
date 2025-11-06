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
import com.swent.mapin.ui.settings.SettingsScreen

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    isLoggedIn: Boolean,
    renderMap: Boolean = true
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
          onNavigateToFriends = { navController.navigate(Route.Friends.route) },
          onNavigateToChat = { navController.navigate(Route.Chat.route) },
          renderMap = renderMap)
    }

    composable(Route.Profile.route) {
      ProfileScreen(
          onNavigateBack = {
            if (navController.previousBackStackEntry != null) {
              navController.popBackStack()
            }
          },
          onNavigateToSettings = { navController.navigate(Route.Settings.route) },
          onNavigateToSignIn = {
            navController.navigate(Route.Auth.route) {
              popUpTo(0) { inclusive = true }
              launchSingleTop = true
            }
          },
          onNavigateToFriends = { navController.navigate(Route.Friends.route) })
    }

    composable(Route.Settings.route) {
      SettingsScreen(
          onNavigateBack = {
            if (navController.previousBackStackEntry != null) {
              navController.popBackStack()
            }
          },
          onNavigateToSignIn = {
            navController.navigate(Route.Auth.route) {
              popUpTo(0) { inclusive = true }
              launchSingleTop = true
            }
          })
    }

    composable(Route.Friends.route) {
      FriendsScreen(
          onNavigateBack = {
            if (navController.previousBackStackEntry != null) {
              navController.popBackStack()
            }
          })
    }

    composable(Route.Chat.route) {
      ChatScreen(
          onNavigateBack = { navController.navigate(Route.Map.route) },
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
          onConfirm = { selectedFriends
            -> // TODO Add Logic to navigate to a potential add group name page
            navController.navigate(Route.Chat.route)
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
          onNavigateBack = { navController.navigate(Route.Chat.route) })
    }
  }
}
