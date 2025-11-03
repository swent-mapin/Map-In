package com.swent.mapin.navigation

sealed class Route(val route: String) {
  object Auth : Route("auth")

  object Map : Route("map")

  object Profile : Route("profile")

  object Settings : Route("settings")

  object Friends : Route("friends")

  object Chat: Route("chat")
}
