package com.swent.mapin.ui.AddEvent

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.swent.mapin.ui.AddEventPopUpTestTags
import com.swent.mapin.ui.FutureDatePickerButton
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DatePickerButtonTests {
  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setup() {
    val date = mutableStateOf("")
    composeTestRule.setContent {
      FutureDatePickerButton(
          selectedDate = date, onDateClick = { date.value = "10/10/2025" } // simulate selection
          )
    }
  }

  @Test
  fun datePickerUpdatesDisplayedDate() {

    composeTestRule.onNodeWithTag(AddEventPopUpTestTags.PICK_EVENT_DATE).performClick()

    composeTestRule
        .onNodeWithTag(AddEventPopUpTestTags.PICK_EVENT_DATE)
        .assertTextContains("10/10/2025", substring = true, ignoreCase = true)
  }
}
