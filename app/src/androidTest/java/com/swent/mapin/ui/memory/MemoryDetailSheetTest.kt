package com.swent.mapin.ui.memory

import androidx.activity.ComponentActivity
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

@RunWith(AndroidJUnit4::class)
class MemoryDetailSheetTest {

  @get:Rule
  // Use a plain ComponentActivity for tests so setContent in the test rule doesn't conflict
  // with the app's MainActivity.setContent which is called in production code.
  val composeTestRule = createAndroidComposeRule<ComponentActivity>()

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
}
