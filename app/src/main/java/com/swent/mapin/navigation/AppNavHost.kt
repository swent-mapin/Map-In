package com.swent.mapin.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.swent.mapin.authentication.AuthScreen
import com.swent.mapin.map.MapScreen

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    isLoggedIn: Boolean,
) {
  val startDest = if (isLoggedIn) Route.Map.route else Route.Auth.route

  NavHost(navController = navController, startDestination = startDest) {
    composable(Route.Auth.route) {
      AuthScreen(
          onAuthSuccess = {
            navController.navigate(Route.Map.route) {
              popUpTo(Route.Auth.route) { inclusive = true }
              launchSingleTop = true // not necessary here, but good practice
            }
          })
    }

    composable(Route.Map.route) { MapScreen() }
  }
}
