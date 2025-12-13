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
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MemoriesScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val sampleMemories =
      listOf(
          Memory(
              uid = "mem1",
              title = "Amazing Beach Day",
              description = "Had an incredible time playing volleyball with friends!",
              eventId = "2",
              ownerId = "user1",
              public = true,
              createdAt = Timestamp.now(),
              mediaUrls = listOf("https://picsum.photos/id/69/200"),
              taggedUserIds = listOf("user2", "user3")))

  @Test
  fun memoriesScreen_showsTopBarAndMemories() {
    val memoryViewModelMock = mockk<MemoriesViewModel>(relaxed = true)

    every { memoryViewModelMock.memories } returns MutableStateFlow(sampleMemories)
    every { memoryViewModelMock.error } returns MutableStateFlow(null)
    every { memoryViewModelMock.selectedMemory } returns MutableStateFlow(null)
    every { memoryViewModelMock.ownerName } returns MutableStateFlow("user1")
    every { memoryViewModelMock.taggedNames } returns MutableStateFlow(listOf("user2", "user3"))

    // Compose a small test UI that mirrors the important parts of MemoriesScreen
    composeTestRule.setContent { MemoriesScreen(viewModel = memoryViewModelMock) }

    // Top app bar title
    composeTestRule.onNodeWithTag("memoriesScreenTitle").assertIsDisplayed()
    // Section title
    composeTestRule.onNodeWithTag("yourMemoriesMessage").assertIsDisplayed()

    val memory = sampleMemories.first()
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
    val memoryViewModelMock = mockk<MemoriesViewModel>(relaxed = true)

    every { memoryViewModelMock.memories } returns MutableStateFlow(emptyList())
    every { memoryViewModelMock.error } returns MutableStateFlow(null)
    every { memoryViewModelMock.selectedMemory } returns MutableStateFlow(null)
    every { memoryViewModelMock.ownerName } returns MutableStateFlow("")
    every { memoryViewModelMock.taggedNames } returns MutableStateFlow(emptyList())

    composeTestRule.setContent { MemoriesScreen(viewModel = memoryViewModelMock) }
    // Top app bar title
    composeTestRule.onNodeWithTag("memoriesScreenTitle").assertIsDisplayed()
    // Section title
    composeTestRule.onNodeWithTag("yourMemoriesMessage").assertIsDisplayed()
    // Check that no memories message is shown
    composeTestRule.onNodeWithTag("noMemoriesMessage").assertIsDisplayed()
  }

  @Test
  fun navigateBack_buttonExistsAndClickable() {
    val memoryViewModelMock = mockk<MemoriesViewModel>(relaxed = true)

    every { memoryViewModelMock.memories } returns MutableStateFlow(sampleMemories)
    every { memoryViewModelMock.error } returns MutableStateFlow(null)
    every { memoryViewModelMock.selectedMemory } returns MutableStateFlow(null)
    every { memoryViewModelMock.ownerName } returns MutableStateFlow("user1")
    every { memoryViewModelMock.taggedNames } returns MutableStateFlow(listOf("user2", "user3"))

    var clicked = false
    composeTestRule.setContent {
      MemoriesScreen(viewModel = memoryViewModelMock, onNavigateBack = { clicked = true })
    }

    // The back button uses contentDescription "Back" on the Icon in the TopAppBar
    val backNode = composeTestRule.onNodeWithContentDescription("Back")
    backNode.assertIsDisplayed()
    backNode.performClick()

    // Small assertion that click triggered
    assert(clicked)
  }
}
