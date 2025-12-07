package com.swent.mapin.ui.memory

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.Timestamp
import com.swent.mapin.model.memory.Memory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MemoriesScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  val memories =
      listOf(
          Memory(
              uid = "mem1",
              title = "Amazing Beach Day",
              description = "Had an incredible time playing volleyball with friends!",
              eventId = "2",
              ownerId = "user1",
              isPublic = true,
              createdAt = Timestamp.now(),
              mediaUrls = listOf("https://picsum.photos/id/69/200"),
              taggedUserIds = listOf("user2", "user3")))
  val memory = memories.first()

  @Test
  fun memoriesScreen_showsTopBarAndMemories() {
    composeTestRule.setContent { MemoriesScreen(onNavigateBack = {}, memories = memories) }
    // Top app bar title
    composeTestRule.onNodeWithTag("memoriesScreenTitle").assertIsDisplayed()
    // Section title
    composeTestRule.onNodeWithTag("yourMemoriesMessage").assertIsDisplayed()
    // Check that memory titles from sample data are shown
    composeTestRule.onNodeWithText(memory.title).assertIsDisplayed()
    composeTestRule.onNodeWithText(memory.description).assertIsDisplayed()
    composeTestRule
        .onNodeWithText(memory.taggedUserIds.first(), substring = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithText(memory.taggedUserIds.last(), substring = true)
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("Public").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Memory photo").assertIsDisplayed()
  }

  @Test
  fun memoriesScreen_showsNoMemoriesMessage() {
    composeTestRule.setContent { MemoriesScreen(onNavigateBack = {}, memories = emptyList()) }
    // Top app bar title
    composeTestRule.onNodeWithText("Memories").assertIsDisplayed()
    // Section title
    composeTestRule.onNodeWithText("Your Memories").assertIsDisplayed()
    // Check that no memories message is shown
    composeTestRule.onNodeWithTag("noMemoriesMessage").assertIsDisplayed()
  }

  @Test
  fun navigateBack_buttonExistsAndClickable() {
    var clicked = false
    composeTestRule.setContent { MemoriesScreen(onNavigateBack = { clicked = true }) }

    // The back button uses contentDescription "Back" on the Icon in the TopAppBar
    val backNode = composeTestRule.onNodeWithContentDescription("Back")
    backNode.assertIsDisplayed()
    backNode.performClick()

    // Small assertion that click triggered
    assert(clicked)
  }
}
