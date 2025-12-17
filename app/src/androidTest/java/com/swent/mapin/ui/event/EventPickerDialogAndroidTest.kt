package com.swent.mapin.ui.event

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.google.firebase.Timestamp
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.location.Location
import java.util.Calendar
import org.junit.Rule
import org.junit.Test

class EventPickerDialogAndroidTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val sampleEvents =
      listOf(
          testEvent(id = "1", title = "Alpha Event", locationName = "Zurich"),
          testEvent(id = "2", title = "Beta Event", locationName = "Geneva"))

  @Test
  fun eventPicker_showsEvents_andSelectsItem() {
    var selectedId: String? = null
    composeTestRule.setContent {
      EventPickerDialog(events = sampleEvents, onEventSelected = { selectedId = it.uid }) {}
    }

    composeTestRule.onNodeWithTag("eventPickerSearch").assertIsDisplayed()
    composeTestRule.onNodeWithTag("eventPickerList").assertIsDisplayed()
    composeTestRule.onNodeWithTag("eventPickerItem_1").assertIsDisplayed()

    composeTestRule.onNodeWithTag("eventPickerItem_1").performClick()
    composeTestRule.waitForIdle()

    assert(selectedId == "1")
  }

  @Test
  fun eventPicker_filtersByQuery() {
    composeTestRule.setContent {
      EventPickerDialog(events = sampleEvents, onEventSelected = {}, onDismiss = {})
    }

    composeTestRule.onNodeWithTag("eventPickerSearch").performTextInput("Beta")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("eventPickerItem_2").assertIsDisplayed()
    composeTestRule.onNodeWithText("Alpha Event").assertDoesNotExist()
  }

  @Test
  fun eventPicker_showsEmptyState_whenNoEvents() {
    composeTestRule.setContent {
      EventPickerDialog(events = emptyList(), onEventSelected = {}, onDismiss = {})
    }

    composeTestRule.onNodeWithTag("eventPickerEmpty").assertIsDisplayed()
    composeTestRule.onNodeWithText("No events available").assertIsDisplayed()
  }

  private fun testEvent(
      id: String,
      title: String,
      locationName: String,
      year: Int = 2025,
      month: Int = Calendar.JANUARY,
      day: Int = 1
  ): Event {
    val timestamp =
        Timestamp(
            Calendar.getInstance().apply { set(year, month, day, 10, 0, 0) }.time.time / 1000, 0)
    return Event(
        uid = id,
        title = title,
        description = "Description for $title",
        date = timestamp,
        location = Location.from(locationName, 0.0, 0.0))
  }
}
