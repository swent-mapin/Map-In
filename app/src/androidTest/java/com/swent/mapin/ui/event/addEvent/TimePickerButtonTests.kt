package com.swent.mapin.ui.event.addEvent

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.swent.mapin.ui.event.AddEventScreenTestTags
import com.swent.mapin.ui.event.TimePickerButton
import org.junit.Rule
import org.junit.Test

class TimePickerButtonTests {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun timePickerUpdatesDisplayedTime() {
    val time = mutableStateOf("")
    composeTestRule.setContent {
      TimePickerButton(selectedTime = time, onTimeClick = { time.value = "1215" })
    }

    composeTestRule.onNodeWithTag(AddEventScreenTestTags.PICK_EVENT_TIME).performClick()

    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.PICK_EVENT_TIME)
        .assertTextContains("12h15", substring = true, ignoreCase = true)
  }

  @Test
  fun timePickerDialogBranchRunsWithoutCrash() {
    val time = mutableStateOf("")
    composeTestRule.setContent {
      TimePickerButton(selectedTime = time, onTimeClick = null)
    }

    // Clicking should invoke the branch that creates/shows a TimePickerDialog. We can't
    // interact with the platform dialog reliably here, but we at least ensure clicking the
    // button does not crash and the composable exists.
    composeTestRule.onNodeWithTag(AddEventScreenTestTags.PICK_EVENT_TIME).performClick()

    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.PICK_EVENT_TIME)
        .assertTextContains("Select Time:", substring = true, ignoreCase = true)
  }
}
