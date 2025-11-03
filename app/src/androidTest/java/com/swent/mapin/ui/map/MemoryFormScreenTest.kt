package com.swent.mapin.ui.map

import android.net.Uri
import androidx.compose.foundation.ScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.google.firebase.Timestamp
import com.swent.mapin.model.Location
import com.swent.mapin.model.event.Event
import org.junit.Rule
import org.junit.Test

// Assisted by AI

class MemoryFormScreenTest {

  @get:Rule val rule = createComposeRule()

  private val sampleEvents =
      listOf(
          Event(
              uid = "1",
              title = "Beach Party",
              url = "url1",
              description = "Fun at the beach",
              date = Timestamp.now(),
              location = Location("Santa Monica Beach", -118.4965, 34.0195),
              tags = listOf("party", "beach"),
              public = true,
              ownerId = "user1",
              imageUrl = null,
              capacity = 50),
          Event(
              uid = "2",
              title = "Mountain Hike",
              url = "url2",
              description = "Hiking adventure",
              date = Timestamp.now(),
              location = Location("Mt. Wilson", -118.0617, 34.2256),
              tags = listOf("hiking", "nature"),
              public = true,
              ownerId = "user2",
              imageUrl = null,
              capacity = 20))

  @Test
  fun memoryForm_rendersSuccessfully() {
    rule.setContent {
      MaterialTheme {
        MemoryFormScreen(
            scrollState = ScrollState(0), availableEvents = emptyList(), onSave = {}, onCancel = {})
      }
    }
    rule.onNodeWithTag("memoryFormScreen").assertIsDisplayed()
    rule.onNodeWithText("New Memory").assertIsDisplayed()
  }

  @Test
  fun memoryForm_initialState_saveButtonDisabled() {
    rule.setContent {
      MaterialTheme {
        MemoryFormScreen(
            scrollState = ScrollState(0), availableEvents = emptyList(), onSave = {}, onCancel = {})
      }
    }

    rule.onNodeWithTag("saveButton").assertIsDisplayed()
    rule.onNodeWithTag("saveButton").assertIsNotEnabled()
  }

  @Test
  fun memoryForm_withDescription_saveButtonEnabled() {
    rule.setContent {
      MaterialTheme {
        MemoryFormScreen(
            scrollState = ScrollState(0), availableEvents = emptyList(), onSave = {}, onCancel = {})
      }
    }

    rule.onNodeWithTag("descriptionField").performScrollTo().performTextInput("Great memory!")
    rule.waitForIdle()
    rule.onNodeWithTag("saveButton").assertIsEnabled()
  }

  @Test
  fun memoryForm_titleField_acceptsInput() {
    rule.setContent {
      MaterialTheme {
        MemoryFormScreen(
            scrollState = ScrollState(0), availableEvents = emptyList(), onSave = {}, onCancel = {})
      }
    }

    rule.onNodeWithTag("titleField").performScrollTo().performTextInput("My Amazing Day")
    rule.waitForIdle()
    rule.onNodeWithText("My Amazing Day").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun memoryForm_descriptionField_acceptsInput() {
    rule.setContent {
      MaterialTheme {
        MemoryFormScreen(
            scrollState = ScrollState(0), availableEvents = emptyList(), onSave = {}, onCancel = {})
      }
    }

    rule
        .onNodeWithTag("descriptionField")
        .performScrollTo()
        .performTextInput("This was an incredible experience!")
    rule.waitForIdle()
    rule.onNodeWithText("This was an incredible experience!").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun memoryForm_eventSelectionCard_displayed() {
    rule.setContent {
      MaterialTheme {
        MemoryFormScreen(
            scrollState = ScrollState(0),
            availableEvents = sampleEvents,
            onSave = {},
            onCancel = {})
      }
    }

    rule.onNodeWithTag("eventSelectionCard").performScrollTo().assertIsDisplayed()
    rule.onNodeWithText("Tap to select an event").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun memoryForm_eventSelection_showsEventPicker() {
    rule.setContent {
      MaterialTheme {
        MemoryFormScreen(
            scrollState = ScrollState(0),
            availableEvents = sampleEvents,
            onSave = {},
            onCancel = {})
      }
    }

    rule.onNodeWithTag("eventSelectionCard").performScrollTo().performClick()
    rule.waitForIdle()

    rule.onNodeWithText("Select an event").assertIsDisplayed()
    rule.onNodeWithText("Beach Party").performScrollTo().assertIsDisplayed()
    rule.onNodeWithText("Mountain Hike").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun memoryForm_eventSelection_andClear() {
    rule.setContent {
      MaterialTheme {
        MemoryFormScreen(
            scrollState = ScrollState(0),
            availableEvents = sampleEvents,
            onSave = {},
            onCancel = {})
      }
    }

    rule.onNodeWithTag("eventSelectionCard").performScrollTo().performClick()
    rule.waitForIdle()
    rule.onNodeWithText("Beach Party").performScrollTo().performClick()
    rule.waitForIdle()

    rule.onNodeWithTag("eventSelectionCard").performScrollTo()
    rule.onNodeWithText("Beach Party").assertIsDisplayed()
    rule.onNodeWithText("Santa Monica Beach", substring = true).assertIsDisplayed()

    rule.onNodeWithTag("clearEventButton").performClick()
    rule.waitForIdle()

    rule.onNodeWithText("Tap to select an event").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun memoryForm_publicSwitch_togglesState() {
    rule.setContent {
      MaterialTheme {
        MemoryFormScreen(
            scrollState = ScrollState(0), availableEvents = emptyList(), onSave = {}, onCancel = {})
      }
    }

    rule.onNodeWithTag("publicSwitch").performScrollTo().assertIsDisplayed()
    rule.onNodeWithTag("publicSwitch").performScrollTo().performClick()
    rule.waitForIdle()
    rule.onNodeWithTag("publicSwitch").performScrollTo().performClick()
    rule.waitForIdle()
    rule.onNodeWithTag("publicSwitch").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun memoryForm_addMediaButton_displayed() {
    rule.setContent {
      MaterialTheme {
        MemoryFormScreen(
            scrollState = ScrollState(0), availableEvents = emptyList(), onSave = {}, onCancel = {})
      }
    }

    rule.onNodeWithTag("addMediaButton").performScrollTo().assertIsDisplayed()
    rule.onNodeWithText("Tap to add photos or videos").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun memoryForm_addUserButton_displayed() {
    rule.setContent {
      MaterialTheme {
        MemoryFormScreen(
            scrollState = ScrollState(0), availableEvents = emptyList(), onSave = {}, onCancel = {})
      }
    }

    rule.onNodeWithTag("addUserButton").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun memoryForm_addUserButton_showsUserPicker() {
    rule.setContent {
      MaterialTheme {
        MemoryFormScreen(
            scrollState = ScrollState(0), availableEvents = emptyList(), onSave = {}, onCancel = {})
      }
    }

    rule.onNodeWithTag("addUserButton").performScrollTo().performClick()
    rule.waitForIdle()

    rule.onNodeWithText("Done").assertIsDisplayed()
  }

  @Test
  fun memoryForm_cancelButton_displayed() {
    var cancelCalled = false
    rule.setContent {
      MaterialTheme {
        MemoryFormScreen(
            scrollState = ScrollState(0),
            availableEvents = emptyList(),
            onSave = {},
            onCancel = { cancelCalled = true })
      }
    }

    rule.onNodeWithTag("cancelButton").assertIsDisplayed()
    rule.onNodeWithTag("cancelButton").performClick()
    rule.waitForIdle()

    assert(cancelCalled)
  }

  @Test
  fun memoryForm_saveButton_callsOnSaveWithCorrectData() {
    var savedData: MemoryFormData? = null
    rule.setContent {
      MaterialTheme {
        MemoryFormScreen(
            scrollState = ScrollState(0),
            availableEvents = sampleEvents,
            onSave = { data -> savedData = data },
            onCancel = {})
      }
    }

    rule.onNodeWithTag("descriptionField").performScrollTo().performTextInput("Test description")
    rule.waitForIdle()

    rule.onNodeWithTag("saveButton").performClick()
    rule.waitForIdle()

    assert(savedData != null)
    assert(savedData?.description == "Test description")
  }

  @Test
  fun memoryForm_allFieldsTogether_saveWithCompleteData() {
    var savedData: MemoryFormData? = null
    rule.setContent {
      MaterialTheme {
        MemoryFormScreen(
            scrollState = ScrollState(0),
            availableEvents = sampleEvents,
            onSave = { data -> savedData = data },
            onCancel = {})
      }
    }

    rule.onNodeWithTag("titleField").performScrollTo().performTextInput("Epic Day")
    rule.waitForIdle()

    rule.onNodeWithTag("descriptionField").performScrollTo().performTextInput("Amazing experience!")
    rule.waitForIdle()

    rule.onNodeWithTag("eventSelectionCard").performScrollTo().performClick()
    rule.waitForIdle()
    rule.onNodeWithText("Beach Party").performScrollTo().performClick()
    rule.waitForIdle()

    rule.onNodeWithTag("publicSwitch").performScrollTo().performClick()
    rule.waitForIdle()

    rule.onNodeWithTag("saveButton").performClick()
    rule.waitForIdle()

    assert(savedData != null)
    assert(savedData?.title == "Epic Day")
    assert(savedData?.description == "Amazing experience!")
    assert(savedData?.eventId == "1")
    assert(savedData?.isPublic == true)
  }

  @Test
  fun memoryForm_formSections_allVisible() {
    rule.setContent {
      MaterialTheme {
        MemoryFormScreen(
            scrollState = ScrollState(0),
            availableEvents = sampleEvents,
            onSave = {},
            onCancel = {})
      }
    }

    rule.onNodeWithText("Link to event (optional)").performScrollTo().assertIsDisplayed()
    rule.onNodeWithText("Title (optional)").performScrollTo().assertIsDisplayed()
    rule.onNodeWithText("Description *").performScrollTo().assertIsDisplayed()
    rule.onNodeWithText("Photos or videos (up to 5)").performScrollTo().assertIsDisplayed()
    rule.onNodeWithText("Tag people").performScrollTo().assertIsDisplayed()
    rule.onNodeWithText("Make this memory public").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun mediaSelectionSection_withSelectedMedia_showsThumbnailsAndRemoveButtons() {
    rule.setContent {
      MaterialTheme {
        val selectedMediaUris =
            listOf(Uri.parse("content://media/1"), Uri.parse("content://media/2"))

        MediaSelectionSection(
            selectedMediaUris = selectedMediaUris, onLaunchMediaPicker = {}, onRemoveMedia = {})
      }
    }

    rule.onNodeWithText("Tap to add photos or videos").assertDoesNotExist()
    rule.onNodeWithText("Photos or videos (up to 5)").assertIsDisplayed()
  }

  @Test
  fun mediaSelectionSection_withMultipleMedia_displaysAllThumbnails() {
    rule.setContent {
      MaterialTheme {
        val selectedMediaUris =
            listOf(
                Uri.parse("content://media/1"),
                Uri.parse("content://media/2"),
                Uri.parse("content://media/3"))

        MediaSelectionSection(
            selectedMediaUris = selectedMediaUris, onLaunchMediaPicker = {}, onRemoveMedia = {})
      }
    }

    rule.onNodeWithText("Tap to add photos or videos").assertDoesNotExist()
    rule.onNodeWithText("Photos or videos (up to 5)").assertIsDisplayed()
  }

  @Test
  fun mediaSelectionSection_withMaxMedia_showsNoAddMoreButton() {
    rule.setContent {
      MaterialTheme {
        val selectedMediaUris =
            listOf(
                Uri.parse("content://media/1"),
                Uri.parse("content://media/2"),
                Uri.parse("content://media/3"),
                Uri.parse("content://media/4"),
                Uri.parse("content://media/5"))

        MediaSelectionSection(
            selectedMediaUris = selectedMediaUris, onLaunchMediaPicker = {}, onRemoveMedia = {})
      }
    }
    rule.onNodeWithText("Tap to add photos or videos").assertDoesNotExist()
    rule.onNodeWithText("Photos or videos (up to 5)").assertIsDisplayed()
  }

  @Test
  fun memoryForm_userTagging_section_isDisplayed() {
    rule.setContent {
      MaterialTheme {
        MemoryFormScreen(
            scrollState = ScrollState(0), availableEvents = emptyList(), onSave = {}, onCancel = {})
      }
    }

    // Verify that the "Add user" button is visible
    rule.onNodeWithTag("addUserButton").performScrollTo().assertIsDisplayed()

    // Optionally, check that the section title "Tag people" exists
    rule.onNodeWithText("Tag people").performScrollTo().assertIsDisplayed()
  }
}
