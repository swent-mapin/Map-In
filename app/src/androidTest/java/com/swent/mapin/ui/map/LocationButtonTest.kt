package com.swent.mapin.ui.map

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LocationButtonTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun locationButton_isDisplayed() {
    composeTestRule.setContent { MaterialTheme { LocationButton(onClick = {}) } }

    composeTestRule.onNodeWithTag("locationButton").assertIsDisplayed()
  }

  @Test
  fun locationButton_hasClickAction() {
    composeTestRule.setContent { MaterialTheme { LocationButton(onClick = {}) } }

    composeTestRule.onNodeWithTag("locationButton").assertHasClickAction()
  }

  @Test
  fun locationButton_onClick_isCalled() {
    var clicked = false
    composeTestRule.setContent { MaterialTheme { LocationButton(onClick = { clicked = true }) } }

    composeTestRule.onNodeWithTag("locationButton").performClick()

    assertTrue(clicked)
  }

  @Test
  fun locationButton_displaysCorrectIcon() {
    composeTestRule.setContent { MaterialTheme { LocationButton(onClick = {}) } }

    composeTestRule.onNodeWithContentDescription("Center on my location").assertIsDisplayed()
  }

  @Test
  fun locationButton_canBeClickedMultipleTimes() {
    var clickCount = 0
    composeTestRule.setContent {
      MaterialTheme { LocationButton(onClick = { clickCount++ }) }
    }

    composeTestRule.onNodeWithTag("locationButton").performClick()
    composeTestRule.onNodeWithTag("locationButton").performClick()
    composeTestRule.onNodeWithTag("locationButton").performClick()

    assertTrue(clickCount == 3)
  }
}

