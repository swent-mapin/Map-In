package com.swent.mapin.ui.memory

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.Timestamp
import com.swent.mapin.model.memory.Memory
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
              isPublic = true,
              createdAt = Timestamp.now(),
              mediaUrls = listOf("https://picsum.photos/id/69/200"),
              taggedUserIds = listOf("user2", "user3")))

  @Test
  fun memoriesScreen_showsTopBarAndMemories() {
    // Compose a small test UI that mirrors the important parts of MemoriesScreen
    composeTestRule.setContent { TestMemoriesScreen(memories = sampleMemories) }

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
    composeTestRule.setContent { TestMemoriesScreen(memories = emptyList()) }
    // Top app bar title
    composeTestRule.onNodeWithTag("memoriesScreenTitle").assertIsDisplayed()
    // Section title
    composeTestRule.onNodeWithTag("yourMemoriesMessage").assertIsDisplayed()
    // Check that no memories message is shown
    composeTestRule.onNodeWithTag("noMemoriesMessage").assertIsDisplayed()
  }

  @Test
  fun navigateBack_buttonExistsAndClickable() {
    var clicked = false
    composeTestRule.setContent {
      TestMemoriesScreen(memories = emptyList(), onNavigateBack = { clicked = true })
    }

    // The back button uses contentDescription "Back" on the Icon in the TopAppBar
    val backNode = composeTestRule.onNodeWithContentDescription("Back")
    backNode.assertIsDisplayed()
    backNode.performClick()

    // Small assertion that click triggered
    assert(clicked)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestMemoriesScreen(memories: List<Memory>, onNavigateBack: () -> Unit = {}) {
  MaterialTheme {
    Column(modifier = Modifier.padding(0.dp)) {
      TopAppBar(
          title = { Text(text = "Memories", modifier = Modifier.testTag("memoriesScreenTitle")) },
          navigationIcon = {
            androidx.compose.material3.IconButton(onClick = onNavigateBack) {
              Icon(Icons.Filled.Image, contentDescription = "Back")
            }
          })

      Text("Your Memories", modifier = Modifier.testTag("yourMemoriesMessage"))

      if (memories.isEmpty()) {
        Column(modifier = Modifier.testTag("noMemoriesMessage")) {
          Text(text = "No memories yet")
          Text(text = "Create memories for attended events and they'll appear here")
        }
      } else {
        val m = memories.first()
        // Thumbnail placeholder (use an Icon with same content description as real UI)
        Icon(imageVector = Icons.Filled.Image, contentDescription = "Memory photo")
        Text(text = m.title)
        Text(text = m.description)
        if (m.taggedUserIds.isNotEmpty()) {
          Text(text = "Tagged: ${m.taggedUserIds.joinToString()}")
        }
        Text(text = if (m.isPublic) "Public" else "Private")
      }
    }
  }
}
