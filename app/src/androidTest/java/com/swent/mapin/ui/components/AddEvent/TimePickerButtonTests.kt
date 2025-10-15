package com.swent.mapin.ui.components.AddEvent

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.swent.mapin.ui.components.AddEventPopUpTestTags
import com.swent.mapin.ui.components.TimePickerButton
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TimePickerButtonTests {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setup() {
    val time = mutableStateOf("")
    composeTestRule.setContent {
      TimePickerButton(
          selectedTime = time, onTimeClick = { time.value = "12h15" } // simulate selection
          )
    }
  }

  @Test
  fun timePickerUpdatesDisplayedTime() {
    composeTestRule.onNodeWithTag(AddEventPopUpTestTags.PICK_EVENT_TIME).performClick()

    composeTestRule
        .onNodeWithTag(AddEventPopUpTestTags.PICK_EVENT_TIME)
        .assertTextContains("12h15", substring = true, ignoreCase = true)
  }
}
