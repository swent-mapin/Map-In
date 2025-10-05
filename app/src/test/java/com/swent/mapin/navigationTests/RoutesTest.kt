package com.swent.mapin.navigationTests

import com.swent.mapin.navigation.Route
import org.junit.Test
import junit.framework.TestCase.assertEquals

class RoutesTest {
    @Test
    fun routes_valuesAreStable() {
        assertEquals("auth", Route.Auth.route)
        assertEquals("map", Route.Map.route)
    }
}