// Assisted by AI
package com.swent.mapin.ui.map.directions

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class RouteInfoCardTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun routeInfoCard_displaysDistanceInMeters() {
    val routeInfo = RouteInfo(distance = 500.0, duration = 300.0)

    composeTestRule.setContent { RouteInfoCard(routeInfo = routeInfo) }

    composeTestRule.onNodeWithText("500 m").assertIsDisplayed()
    composeTestRule.onNodeWithText("distance").assertIsDisplayed()
  }

  @Test
  fun routeInfoCard_displaysDistanceInKilometers() {
    val routeInfo = RouteInfo(distance = 2500.0, duration = 600.0)

    composeTestRule.setContent { RouteInfoCard(routeInfo = routeInfo) }

    composeTestRule.onNodeWithText("2.5 km").assertIsDisplayed()
    composeTestRule.onNodeWithText("distance").assertIsDisplayed()
  }

  @Test
  fun routeInfoCard_displaysDurationInMinutes() {
    val routeInfo = RouteInfo(distance = 1000.0, duration = 600.0) // 10 minutes

    composeTestRule.setContent { RouteInfoCard(routeInfo = routeInfo) }

    composeTestRule.onNodeWithText("10 min").assertIsDisplayed()
    composeTestRule.onNodeWithText("temps").assertIsDisplayed()
  }

  @Test
  fun routeInfoCard_displaysDurationInHoursAndMinutes() {
    val routeInfo = RouteInfo(distance = 10000.0, duration = 5400.0) // 1h 30min

    composeTestRule.setContent { RouteInfoCard(routeInfo = routeInfo) }

    composeTestRule.onNodeWithText("1 h 30 min").assertIsDisplayed()
    composeTestRule.onNodeWithText("temps").assertIsDisplayed()
  }

  @Test
  fun routeInfoCard_displaysZeroValues() {
    val routeInfo = RouteInfo.ZERO

    composeTestRule.setContent { RouteInfoCard(routeInfo = routeInfo) }

    composeTestRule.onNodeWithText("0 m").assertIsDisplayed()
    composeTestRule.onNodeWithText("0 min").assertIsDisplayed()
  }

  @Test
  fun routeInfoCard_displaysLabels() {
    val routeInfo = RouteInfo(distance = 1500.0, duration = 900.0)

    composeTestRule.setContent { RouteInfoCard(routeInfo = routeInfo) }

    composeTestRule.onNodeWithText("distance").assertIsDisplayed()
    composeTestRule.onNodeWithText("temps").assertIsDisplayed()
  }

  @Test
  fun routeInfoCard_displaysLongDistance() {
    val routeInfo = RouteInfo(distance = 42195.0, duration = 15000.0) // Marathon distance

    composeTestRule.setContent { RouteInfoCard(routeInfo = routeInfo) }

    composeTestRule.onNodeWithText("42.2 km").assertIsDisplayed()
  }

  @Test
  fun routeInfoCard_displaysOnlyHoursWhenNoMinutes() {
    val routeInfo = RouteInfo(distance = 20000.0, duration = 7200.0) // Exactly 2 hours

    composeTestRule.setContent { RouteInfoCard(routeInfo = routeInfo) }

    composeTestRule.onNodeWithText("2 h").assertIsDisplayed()
  }
}
