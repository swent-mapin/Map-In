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
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    isLoggedIn: Boolean,
    renderMap: Boolean = true
) {
  val startDest = if (isLoggedIn) Route.Map.route else Route.Auth.route

  // Create a shared ProfileViewModel at the NavHost (activity) scope so Map and Profile share state
  val profileViewModel: com.swent.mapin.ui.profile.ProfileViewModel = viewModel()

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
          renderMap = renderMap,
          profileViewModel = profileViewModel)
    }

    composable(Route.Profile.route) {
      ProfileScreen(
          onNavigateBack = { navController.popBackStack() },
          onNavigateToSignIn = {
            navController.navigate(Route.Auth.route) {
              popUpTo(0) { inclusive = true }
              launchSingleTop = true
            }
          },
          onNavigateToFriends = { navController.navigate(Route.Friends.route) },
          viewModel = profileViewModel)
    }

    composable(Route.Friends.route) {
      FriendsScreen(onNavigateBack = { navController.popBackStack() })
    }
  }
}
