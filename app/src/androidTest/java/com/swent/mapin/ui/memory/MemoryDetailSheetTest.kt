package com.swent.mapin.ui.memory

import androidx.activity.ComponentActivity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.Timestamp
import com.swent.mapin.model.memory.Memory
import com.swent.mapin.ui.map.BottomSheetState
import com.swent.mapin.ui.theme.MapInTheme
import java.util.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Assisted by AI

@RunWith(AndroidJUnit4::class)
class MemoryDetailSheetTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun collapsed_shows_title_and_buttons() {
    val memory =
        Memory(
            uid = "m1",
            title = "End of Semester Party",
            description = "",
            ownerId = "u1",
            createdAt = Timestamp(Date()))

    composeTestRule.setContent {
      MapInTheme {
        MemoryDetailSheet(
            memory = memory,
            sheetState = BottomSheetState.COLLAPSED,
            ownerName = "Alice",
            taggedUserNames = emptyList(),
            onShare = {},
            onClose = {})
      }
    }

    composeTestRule.onNodeWithTag("memoryDetailSheet").assertExists()
    // Collapsed uses Icons with contentDescription "Share" and "Close"
    composeTestRule.onNode(hasContentDescription("Share")).assertExists()
    composeTestRule.onNode(hasContentDescription("Close")).assertExists()
    composeTestRule.onNodeWithTag("memoryTitleCollapsed").assertIsDisplayed()
    composeTestRule
        .onNodeWithTag("memoryTitleCollapsed")
        .assertTextContains("End of Semester Party")
  }

  @Test
  fun medium_shows_title_description_and_linked_event_chip() {
    val memory =
        Memory(
            uid = "m2",
            title = "Medium Memory",
            description = "This is a medium description for preview",
            ownerId = "u1",
            eventId = "e42",
            createdAt = Timestamp(Date()))

    composeTestRule.setContent {
      MapInTheme {
        MemoryDetailSheet(
            memory = memory,
            sheetState = BottomSheetState.MEDIUM,
            ownerName = "Alice",
            taggedUserNames = listOf("Bob", "Charlie"),
            onShare = {},
            onClose = {},
            onOpenLinkedEvent = {})
      }
    }

    composeTestRule.onNodeWithTag("memoryTitleMedium").assertExists()
    composeTestRule.onNodeWithTag("memoryDescriptionPreview").assertExists()
    composeTestRule.onNodeWithTag("linkedEventChip").assertExists()
  }

  @Test
  fun full_shows_edit_and_delete_when_owner_and_description_full() {
    val memory =
        Memory(
            uid = "m3",
            title = "Full Memory",
            description = "Detailed description for the full view.",
            ownerId = "u1",
            createdAt = Timestamp(Date()))

    composeTestRule.setContent {
      MapInTheme {
        MemoryDetailSheet(
            memory = memory,
            sheetState = BottomSheetState.FULL,
            ownerName = "Alice",
            isOwner = true,
            taggedUserNames = listOf("Bob"),
            onShare = {},
            onClose = {},
            onEdit = {},
            onDelete = {})
      }
    }

    composeTestRule.onNodeWithTag("memoryTitleFull").assertExists()
    composeTestRule.onNodeWithTag("memoryDescriptionFull").assertExists()
    composeTestRule.onNodeWithTag("editMemoryButton").assertExists()
    composeTestRule.onNodeWithTag("deleteMemoryButton").assertExists()
  }

  // New tests start here

  @Test
  fun medium_shows_media_preview_when_images_present() {
    val memory =
        Memory(
            uid = "m4",
            title = "Has Images",
            description = "",
            ownerId = "u1",
            mediaUrls = listOf("https://example.com/1.jpg"),
            createdAt = Timestamp(Date()))

    composeTestRule.setContent {
      MapInTheme {
        MemoryDetailSheet(
            memory = memory,
            sheetState = BottomSheetState.MEDIUM,
            ownerName = "Alice",
            taggedUserNames = emptyList(),
            onShare = {},
            onClose = {},
            onOpenLinkedEvent = {})
      }
    }

    composeTestRule.onNodeWithTag("memoryMediaPreview").assertExists()
  }

  @Test
  fun edit_delete_buttons_hidden_when_not_owner() {
    val memory =
        Memory(
            uid = "m6",
            title = "No owner actions",
            description = "",
            ownerId = "u2",
            mediaUrls = emptyList(),
            createdAt = Timestamp(Date()))

    composeTestRule.setContent {
      MapInTheme {
        MemoryDetailSheet(
            memory = memory,
            sheetState = BottomSheetState.FULL,
            ownerName = "Alice",
            isOwner = false,
            taggedUserNames = emptyList(),
            onShare = {},
            onClose = {},
            onEdit = {},
            onDelete = {})
      }
    }

    composeTestRule.onNodeWithTag("editMemoryButton").assertDoesNotExist()
    composeTestRule.onNodeWithTag("deleteMemoryButton").assertDoesNotExist()
  }

  @Test
  fun tagged_users_section_displays_formatted_list() {
    val memory =
        Memory(
            uid = "m7",
            title = "Tagged",
            description = "",
            ownerId = "u1",
            mediaUrls = emptyList(),
            createdAt = Timestamp(Date()))

    composeTestRule.setContent {
      MapInTheme {
        MemoryDetailSheet(
            memory = memory,
            sheetState = BottomSheetState.MEDIUM,
            ownerName = "Alice",
            taggedUserNames = listOf("Bob", "Charlie"),
            onShare = {},
            onClose = {},
            onOpenLinkedEvent = {})
      }
    }

    composeTestRule
        .onNodeWithTag("taggedUsersText")
        .assertExists()
        .assertTextContains("Bob, Charlie")
  }

  @Test
  fun no_crash_with_empty_media_in_medium_and_full() {
    val memory =
        Memory(
            uid = "m8",
            title = "EmptyMedia",
            description = "",
            ownerId = "u1",
            mediaUrls = emptyList(),
            createdAt = Timestamp(Date()))

    // Use a mutable state to switch sheetState within one composition (setContent only once)
    lateinit var sheetState: MutableState<BottomSheetState>
    composeTestRule.runOnUiThread { sheetState = mutableStateOf(BottomSheetState.MEDIUM) }

    composeTestRule.setContent {
      MapInTheme {
        MemoryDetailSheet(
            memory = memory,
            sheetState = sheetState.value,
            ownerName = "Alice",
            taggedUserNames = emptyList(),
            onShare = {},
            onClose = {},
            onOpenLinkedEvent = {})
      }
    }

    // Medium state: should render without media preview
    composeTestRule.onNodeWithTag("memoryTitleMedium").assertExists()

    // Switch to full state on UI thread
    composeTestRule.runOnUiThread { sheetState.value = BottomSheetState.FULL }
    composeTestRule.waitForIdle()

    // Full state: gallery should show "No media available"
    composeTestRule.onNodeWithTag("noMediaText").assertExists()
  }

  @Test
  fun no_crash_with_null_and_blank_fields_and_defaults_applied() {
    val memory =
        Memory(
            uid = "m9",
            title = "",
            description = "",
            ownerId = "u1",
            createdAt = null,
            mediaUrls = emptyList())

    composeTestRule.setContent {
      MapInTheme {
        MemoryDetailSheet(
            memory = memory,
            sheetState = BottomSheetState.MEDIUM,
            ownerName = "Alice",
            taggedUserNames = emptyList(),
            onShare = {},
            onClose = {},
            onOpenLinkedEvent = {})
      }
    }

    // Title should fallback to "Memory"
    composeTestRule.onNodeWithTag("memoryTitleMedium").assertExists().assertTextContains("Memory")

    // Metadata should show Unknown date (substring match because metadata is "By Alice • Unknown
    // date")
    composeTestRule.onNode(hasText("Unknown date", substring = true)).assertExists()
  }

  @Test
  fun collapsed_shows_memory_label_when_linked_to_event() {
    val memory =
        Memory(
            uid = "m10",
            title = "",
            description = "",
            ownerId = "u1",
            eventId = "e100",
            createdAt = Timestamp(Date()))

    composeTestRule.setContent {
      MapInTheme {
        MemoryDetailSheet(
            memory = memory,
            sheetState = BottomSheetState.COLLAPSED,
            ownerName = "Alice",
            taggedUserNames = emptyList(),
            onShare = {},
            onClose = {})
      }
    }

    composeTestRule
        .onNodeWithTag("collapsedMemoryLabel")
        .assertExists()
        .assertTextContains("Memory")
  }
}
