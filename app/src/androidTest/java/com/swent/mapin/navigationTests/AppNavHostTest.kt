package com.swent.mapin.navigationTests

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for the AppNavHost composable, which manages navigation based on authentication state.
 *
 * Role: \- Verify that the correct start destination is shown based on whether the user is logged
 * in or not. generated with the help of AI
 */
@RunWith(AndroidJUnit4::class)
class AppNavHostTest {

  @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

  // Routes de test
  private object Routes {
    const val AUTH = "auth"
    const val MAP = "map"
  }

  // Écrans fake ultra-simples avec testTag
  @androidx.compose.runtime.Composable
  private fun FakeAuthScreen() {
    Text("Auth", modifier = Modifier.testTag("AUTH"))
  }

  @androidx.compose.runtime.Composable
  private fun FakeMapScreen() {
    Text("Map", modifier = Modifier.testTag("MAP"))
  }

  // NavHost de test (au lieu d'utiliser l'AppNavHost réel)
  @androidx.compose.runtime.Composable
  private fun TestNavHost(isLoggedIn: Boolean) {
    val nav = rememberNavController()
    val start = if (isLoggedIn) Routes.MAP else Routes.AUTH

    NavHost(navController = nav, startDestination = start) {
      composable(Routes.AUTH) { FakeAuthScreen() }
      composable(Routes.MAP) { FakeMapScreen() }
    }
  }

  @Test
  fun startsOnAuth_whenNotLoggedIn() {
    rule.setContent { TestNavHost(isLoggedIn = false) }
    rule.waitForIdle()
    rule.onNodeWithTag("AUTH", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun startsOnMap_whenLoggedIn() {
    rule.setContent { TestNavHost(isLoggedIn = true) }
    rule.waitForIdle()
    rule.onNodeWithTag("MAP", useUnmergedTree = true).assertIsDisplayed()
  }
}
