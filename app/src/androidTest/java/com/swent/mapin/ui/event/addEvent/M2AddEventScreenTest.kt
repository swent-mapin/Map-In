package com.swent.mapin.ui.event.addEvent

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import com.swent.mapin.model.location.LocationRepository
import com.swent.mapin.model.location.LocationViewModel
import com.swent.mapin.ui.event.AddEventScreen
import com.swent.mapin.ui.event.AddEventScreenTestTags
import com.swent.mapin.ui.event.DatePickerButton
import com.swent.mapin.ui.event.EventViewModel
import com.swent.mapin.ui.event.TimePickerButton
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

/**
 * Tests for:
 * - DatePickerButton / TimePickerButton (via injected callbacks)
 * - validateStartEnd logic (via a small local harness that mirrors production logic)
 * - error / isDateAndTimeValid / shouldShowMissingFields behaviors in AddEventScreen
 */
class M2AddEventScreenTest {

  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
  val eventViewModel = mockk<EventViewModel>(relaxed = true)
  val locationRepo = mockk<LocationRepository>(relaxed = true)
  val locationViewModel = LocationViewModel(locationRepo)

  // -------------------------------------------------------------------------
  // Date & Time picker buttons
  // -------------------------------------------------------------------------

  @Test
  fun datePickerButton_updates_text_and_invokes_callback() {
    val selectedDate = mutableStateOf("")
    var clicked = false

    compose.setContent {
      DatePickerButton(
          selectedDate = selectedDate,
          onDateClick = {
            clicked = true
            selectedDate.value = "01/01/2030"
          })
    }

    compose.onNodeWithTag(AddEventScreenTestTags.PICK_EVENT_DATE).performClick()
    // callback executed and text reflects state
    compose.onNodeWithText("Select Date: 01/01/2030").assertIsDisplayed()
    assert(clicked)
  }

  @Test
  fun timePickerButton_updates_text_and_invokes_callback() {
    val selectedTime = mutableStateOf("")
    var clicked = false

    compose.setContent {
      TimePickerButton(
          selectedTime = selectedTime,
          onTimeClick = {
            clicked = true
            selectedTime.value = "0930" // internal HHmm format
          })
    }

    compose.onNodeWithTag(AddEventScreenTestTags.PICK_EVENT_TIME).performClick()
    // UI shows "09h30" coming from chunked(2).joinToString("h")
    compose.onNodeWithText("Select Time: 09h30").assertIsDisplayed()
    assert(clicked)
  }

  // -------------------------------------------------------------------------
  // validateStartEnd – exercised in a small test harness so we can drive inputs
  // -------------------------------------------------------------------------

  /**
   * Minimal harness that mirrors AddEventScreen's validateStartEnd logic. It exposes booleans so
   * the test can assert results deterministically without having to open real dialogs.
   */
  @Test
  fun validateStartEnd_same_day_end_before_start_sets_endDateError() {
    val date = mutableStateOf("10/10/2030")
    val endDate = mutableStateOf("10/10/2030")
    val time = mutableStateOf("1400")
    val endTime = mutableStateOf("1330")

    val dateError = mutableStateOf(false)
    val endDateError = mutableStateOf(false)
    val timeError = mutableStateOf(false)
    val endTimeError = mutableStateOf(false)

    compose.setContent {
      // Inline copy of the production logic to validate only this behavior.
      fun validateStartEnd() {
        dateError.value = date.value.isBlank()
        timeError.value = time.value.isBlank()
        endDateError.value = endDate.value.isBlank()
        endTimeError.value = endTime.value.isBlank()

        if (date.value.isBlank() || endDate.value.isBlank()) return

        val fmt = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        val sd = runCatching { fmt.parse(date.value) }.getOrNull()
        val ed = runCatching { fmt.parse(endDate.value) }.getOrNull()
        if (sd == null) {
          dateError.value = true
          return
        }
        if (ed == null) {
          endDateError.value = true
          return
        }

        if (ed.time > sd.time) {
          endDateError.value = false
          endTimeError.value = false
          return
        }
        if (ed.time < sd.time) {
          endDateError.value = true
          endTimeError.value = false
          return
        }

        val rawStart = time.value.replace("h", "")
        val rawEnd = endTime.value.replace("h", "")
        val sm =
            runCatching { rawStart.take(2).toInt() * 60 + rawStart.substring(2, 4).toInt() }
                .getOrNull()
        val em =
            runCatching { rawEnd.take(2).toInt() * 60 + rawEnd.substring(2, 4).toInt() }.getOrNull()
        if (sm == null) {
          timeError.value = true
          return
        }
        if (em == null) {
          endTimeError.value = true
          return
        }
        if (em <= sm) {
          endDateError.value = true
          endTimeError.value = false
        } else {
          endDateError.value = false
          endTimeError.value = false
        }
      }

      // Run once with the initialized states above.
      validateStartEnd()
    }

    // With same day and end 13:30 before start 14:00 => endDateError true
    assert(endDateError.value)
    // We do not want an "end time parsing" error in this path
    assert(!endTimeError.value)
  }

  // -------------------------------------------------------------------------
  // AddEventScreen – error / isDateAndTimeValid / missing fields banner
  // -------------------------------------------------------------------------

  @Test
  fun addEventScreen_click_save_with_missing_fields_shows_validation_banner() {
    compose.setContent {
      AddEventScreen(eventViewModel = eventViewModel, locationViewModel = locationViewModel)
    }

    // Tap Save immediately: everything is blank -> shouldShowMissingFields banner appears
    compose.onNodeWithTag(AddEventScreenTestTags.EVENT_SAVE).performClick()
    compose.waitForIdle()
    compose.onNodeWithTag(AddEventScreenTestTags.ERROR_MESSAGE).assertIsDisplayed()
  }

  @Test
  fun addEventScreen_partial_inputs_still_triggers_missing_fields() {
    compose.setContent {
      AddEventScreen(eventViewModel = eventViewModel, locationViewModel = locationViewModel)
    }

    // Fill some required text fields to clear their individual error flags
    compose.onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_TITLE).performTextInput("Title")
    compose.waitForIdle()
    compose.onNodeWithTag(AddEventScreenTestTags.INPUT_EVENT_DESCRIPTION).performTextInput("Desc")
    compose.waitForIdle()

    // Close keyboard to ensure UI is accessible
    Espresso.closeSoftKeyboard()
    compose.waitForIdle()

    // Use performScrollTo to ensure save button is accessible regardless of screen size/keyboard
    compose.onNodeWithTag(AddEventScreenTestTags.EVENT_SAVE).performScrollTo()
    compose.waitForIdle()
    compose.onNodeWithTag(AddEventScreenTestTags.EVENT_SAVE).performClick()
    compose.waitForIdle()

    // Wait for error banner to appear
    compose.waitUntil(timeoutMillis = 10000) {
      runCatching {
            compose.onNodeWithTag(AddEventScreenTestTags.ERROR_MESSAGE).assertExists()
            true
          }
          .getOrDefault(false)
    }

    compose.onNodeWithTag(AddEventScreenTestTags.ERROR_MESSAGE).assertExists()
  }
}
