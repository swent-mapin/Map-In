package com.swent.mapin.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.swent.mapin.ui.auth.SignInScreen
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
  }
}
