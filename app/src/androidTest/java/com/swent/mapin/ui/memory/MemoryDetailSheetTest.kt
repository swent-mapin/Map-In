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

  @Test
  fun full_gallery_mixed_images_and_videos_composes_images_on_image_pages() {
    val urls =
        listOf(
            "https://example.com/a.jpg",
            "https://example.com/b.mp4",
            "https://example.com/c.MKV",
            "https://example.com/d.PNG")

    val memory =
        Memory(
            uid = "mix1",
            title = "Mix",
            description = "",
            ownerId = "u1",
            mediaUrls = urls,
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
            onDelete = {},
            onOpenLinkedEvent = {})
      }
    }

    // Verify indicators present
    composeTestRule.onNodeWithTag("mediaIndicatorsRow").assertExists()
    urls.indices.forEach { i -> composeTestRule.onNodeWithTag("mediaIndicator_$i").assertExists() }

    val pager = composeTestRule.onNodeWithTag("mediaPager")
    pager.assertExists()

    // For each page, swipe to it (for i>0) and check if mediaItem_i exists only when URL is an
    // image
    for (i in urls.indices) {
      if (i > 0) {
        // swipe once to go to next page
        pager.performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
      }

      val url = urls[i]
      val isVideo =
          listOf(".mp4", ".mov", ".avi", ".mkv", ".webm").any { url.lowercase().endsWith(it) }

      if (!isVideo) {
        // Wait until the image node for this page exists
        composeTestRule.waitUntil(timeoutMillis = 3_000) {
          try {
            composeTestRule.onAllNodesWithTag("mediaItem_$i").fetchSemanticsNodes().isNotEmpty()
          } catch (_: Throwable) {
            false
          }
        }
        composeTestRule.onNodeWithTag("mediaItem_$i").assertExists()
      } else {
        // Video page: there should be no mediaItem_$i tag
        composeTestRule.onNodeWithTag("mediaItem_$i").assertDoesNotExist()
      }
    }
  }

  @Test
  fun full_gallery_all_videos_has_no_image_tags_but_indicators_match_size() {
    val urls = listOf("https://ex/1.mp4", "https://ex/2.MOV", "https://ex/3.webm")
    val memory =
        Memory(
            uid = "vids",
            title = "VideosOnly",
            description = "",
            ownerId = "u1",
            mediaUrls = urls,
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
            onDelete = {},
            onOpenLinkedEvent = {})
      }
    }

    // Indicators should be exactly the number of urls
    composeTestRule.onNodeWithTag("mediaIndicatorsRow").assertExists()
    urls.indices.forEach { i -> composeTestRule.onNodeWithTag("mediaIndicator_$i").assertExists() }

    // No mediaItem_X tags should exist since all are videos
    urls.indices.forEach { i -> composeTestRule.onNodeWithTag("mediaItem_$i").assertDoesNotExist() }
  }

  @Test
  fun video_item_single_composes_and_disposes_without_crash() {
    val urls = listOf("https://example.com/video.mp4")
    val memory =
        Memory(
            uid = "v1",
            title = "VideoOnly",
            description = "",
            ownerId = "u1",
            mediaUrls = urls,
            createdAt = Timestamp(Date()))

    // Use mutable state so we can unmount the FULL sheet to trigger DisposableEffect
    // (player.release())
    lateinit var sheetState: MutableState<BottomSheetState>
    composeTestRule.runOnUiThread { sheetState = mutableStateOf(BottomSheetState.FULL) }

    composeTestRule.setContent {
      MapInTheme {
        MemoryDetailSheet(
            memory = memory,
            sheetState = sheetState.value,
            ownerName = "Alice",
            isOwner = false,
            taggedUserNames = emptyList(),
            onShare = {},
            onClose = {},
            onEdit = {},
            onDelete = {},
            onOpenLinkedEvent = {})
      }
    }

    // mediaItem_0 is not present for videos (only images have that testTag)
    composeTestRule.onNodeWithTag("mediaItem_0").assertDoesNotExist()
    // Indicators should still exist
    composeTestRule.onNodeWithTag("mediaIndicatorsRow").assertExists()

    // Now unmount full by switching to MEDIUM which will dispose the player
    composeTestRule.runOnUiThread { sheetState.value = BottomSheetState.MEDIUM }
    composeTestRule.waitForIdle()

    // Ensure medium content exists after unmount (no crash)
    composeTestRule.onNodeWithTag("memoryTitleMedium").assertExists()
  }

  @Test
  fun invalid_video_url_does_not_crash_and_shows_indicator() {
    val urls = listOf("not-a-valid-url.mp4")
    val memory =
        Memory(
            uid = "v2",
            title = "BadVideo",
            description = "",
            ownerId = "u1",
            mediaUrls = urls,
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
            onDelete = {},
            onOpenLinkedEvent = {})
      }
    }

    // The gallery should render indicators even if the URL is malformed
    composeTestRule.onNodeWithTag("mediaIndicatorsRow").assertExists()
    composeTestRule.onNodeWithTag("mediaIndicator_0").assertExists()

    // And there should be no image test tag for the video
    composeTestRule.onNodeWithTag("mediaItem_0").assertDoesNotExist()
  }
}
