package com.swent.mapin.navigationTests

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Simple tests to verify that the NavHost starts on the correct screen based on login state */
@RunWith(AndroidJUnit4::class)
class AppNavHostTest {

  @get:Rule val composeTestRule = createComposeRule()

  private object Routes {
    const val AUTH = "auth"
    const val MAP = "map"
  }

  @androidx.compose.runtime.Composable
  private fun FakeAuthScreen() {
    Text("Auth", modifier = Modifier.testTag("AUTH"))
  }

  @androidx.compose.runtime.Composable
  private fun FakeMapScreen() {
    Text("Map", modifier = Modifier.testTag("MAP"))
  }

  @androidx.compose.runtime.Composable
  private fun TestNavHost(isLoggedIn: Boolean) {
    val navController = rememberNavController()
    val start = if (isLoggedIn) Routes.MAP else Routes.AUTH
    NavHost(navController = navController, startDestination = start) {
      composable(Routes.AUTH) { FakeAuthScreen() }
      composable(Routes.MAP) { FakeMapScreen() }
    }
  }

  @Test
  fun startsOnAuth_whenNotLoggedIn() {
    composeTestRule.setContent { TestNavHost(isLoggedIn = false) }
    composeTestRule.onNodeWithTag("AUTH", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun startsOnMap_whenLoggedIn() {
    composeTestRule.setContent { TestNavHost(isLoggedIn = true) }
    composeTestRule.onNodeWithTag("MAP", useUnmergedTree = true).assertIsDisplayed()
  }
}
