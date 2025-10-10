package com.swent.mapin.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThemeTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun mapInTheme_lightMode_appliesCorrectly() {
    composeTestRule.setContent {
      MapInTheme(darkTheme = false, dynamicColor = false) { Text("Test Content") }
    }
    composeTestRule.onNodeWithText("Test Content").assertIsDisplayed()
  }

  @Test
  fun mapInTheme_darkMode_appliesCorrectly() {
    composeTestRule.setContent {
      MapInTheme(darkTheme = true, dynamicColor = false) { Text("Test Content") }
    }
    composeTestRule.onNodeWithText("Test Content").assertIsDisplayed()
  }

  @Test
  fun mapInTheme_withDynamicColor_appliesCorrectly() {
    composeTestRule.setContent {
      MapInTheme(darkTheme = false, dynamicColor = true) { Text("Dynamic Color Test") }
    }
    composeTestRule.onNodeWithText("Dynamic Color Test").assertIsDisplayed()
  }

  @Test
  fun typography_appliesCorrectly() {
    composeTestRule.setContent {
      MapInTheme { Text("Typography Test", style = MaterialTheme.typography.bodyLarge) }
    }
    composeTestRule.onNodeWithText("Typography Test").assertIsDisplayed()
  }

  @Test
  fun colorScheme_containsPrimaryColors() {
    var primaryColor: androidx.compose.ui.graphics.Color? = null
    composeTestRule.setContent {
      MapInTheme(darkTheme = false, dynamicColor = false) {
        primaryColor = MaterialTheme.colorScheme.primary
        Text("Color Test")
      }
    }
    assert(primaryColor != null)
  }

  @Test
  fun colorConstants_areValid() {
    assert(Purple80.value != 0UL)
    assert(PurpleGrey80.value != 0UL)
    assert(Pink80.value != 0UL)
    assert(Purple40.value != 0UL)
    assert(PurpleGrey40.value != 0UL)
    assert(Pink40.value != 0UL)
  }

  @Test
  fun typography_bodyLarge_hasCorrectProperties() {
    assert(Typography.bodyLarge.fontSize.value == 16f)
    assert(Typography.bodyLarge.lineHeight.value == 24f)
  }
}
