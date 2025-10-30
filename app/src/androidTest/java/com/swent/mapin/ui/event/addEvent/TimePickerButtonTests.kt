package com.swent.mapin.ui.event.addEvent

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.swent.mapin.ui.event.AddEventScreenTestTags
import com.swent.mapin.ui.event.TimePickerButton
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
          selectedTime = time, onTimeClick = { time.value = "1215" } // simulate selection
          )
    }
  }

  @Test
  fun timePickerUpdatesDisplayedTime() {
    composeTestRule.onNodeWithTag(AddEventScreenTestTags.PICK_EVENT_TIME).performClick()

    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.PICK_EVENT_TIME)
        .assertTextContains("12h15", substring = true, ignoreCase = true)
  }
}
