package com.swent.mapin.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.swent.mapin.authentication.AuthScreen
import com.swent.mapin.map.MapScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    isLoggedIn: Boolean,
) {
    val startDest = if (isLoggedIn) Route.Map.route else Route.Auth.route

    NavHost(
        navController = navController,
        startDestination = startDest
    ) {
        composable(Route.Auth.route) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(Route.Map.route) {
                        popUpTo(0) // clear back stack
                    }
                }
            )
        }

        composable(Route.Map.route) {
            MapScreen(
            )
        }
    }
}
