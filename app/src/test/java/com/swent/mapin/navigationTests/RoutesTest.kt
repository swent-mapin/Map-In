package com.swent.mapin.navigationTests

import com.swent.mapin.navigation.Route
import junit.framework.TestCase.assertEquals
import org.junit.Test

class RoutesTest {
  @Test
  fun routes_valuesAreStable() {
    assertEquals("auth", Route.Auth.route)
    assertEquals("map", Route.Map.route)
    assertEquals("profile", Route.Profile.route)
    assertEquals("settings", Route.Settings.route)
  }
}
