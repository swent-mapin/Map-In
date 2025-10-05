package com.swent.mapin.navigation

sealed class Route(val route: String) {
    object Auth : Route("auth")
    object Map : Route("map")
}