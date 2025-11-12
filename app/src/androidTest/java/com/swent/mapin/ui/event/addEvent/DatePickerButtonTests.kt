package com.swent.mapin.ui.event.addEvent

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.swent.mapin.ui.event.AddEventScreenTestTags
import com.swent.mapin.ui.event.DatePickerButton
import org.junit.Rule
import org.junit.Test

class DatePickerButtonTests {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun datePickerUpdatesDisplayedDate() {
    val date = mutableStateOf("")
    composeTestRule.setContent {
      DatePickerButton(selectedDate = date, onDateClick = { date.value = "10/10/2025" })
    }

    composeTestRule.onNodeWithTag(AddEventScreenTestTags.PICK_EVENT_DATE).performClick()

    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.PICK_EVENT_DATE)
        .assertTextContains("10/10/2025", substring = true, ignoreCase = true)
  }

  @Test
  fun datePickerDialogBranchRunsWithoutCrash() {
    // Compose a new content where onDateClick is null so the DatePickerDialog branch executes
    val date = mutableStateOf("")
    composeTestRule.setContent {
      DatePickerButton(selectedDate = date, onDateClick = null)
    }

    // Clicking should invoke the branch that creates/shows a DatePickerDialog. We can't
    // interact with the platform dialog reliably here, but we at least ensure clicking the
    // button does not crash and the composable exists.
    composeTestRule.onNodeWithTag(AddEventScreenTestTags.PICK_EVENT_DATE).performClick()

    composeTestRule
        .onNodeWithTag(AddEventScreenTestTags.PICK_EVENT_DATE)
        .assertTextContains("Select Date:", substring = true, ignoreCase = true)
  }
}
