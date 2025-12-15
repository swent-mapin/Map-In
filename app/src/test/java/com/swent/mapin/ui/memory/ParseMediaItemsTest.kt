package com.swent.mapin.ui.memory

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class ParseMediaItemsTest {

  @Test
  fun `test images and videos are parsed correctly`() {
    val urls =
        listOf(
            "https://example.com/photo1.jpg",
            "https://example.com/movie1.mp4",
            "https://example.com/image.png",
            "https://example.com/video.MOV",
            "https://example.com/other_video.mkv?alt=media")

    val items = parseMediaItems(urls)

    // Check size
    assertEquals(urls.size, items.size)

    // Check first is image
    assertTrue(items[0] is MediaItem.Image)
    assertEquals("https://example.com/photo1.jpg", (items[0] as MediaItem.Image).url)

    // Check second is video
    assertTrue(items[1] is MediaItem.Video)
    assertEquals("https://example.com/movie1.mp4", (items[1] as MediaItem.Video).url)

    // Check third is image
    assertTrue(items[2] is MediaItem.Image)
    assertEquals("https://example.com/image.png", (items[2] as MediaItem.Image).url)

    // Fourth video with uppercase extension
    assertTrue(items[3] is MediaItem.Video)
    assertEquals("https://example.com/video.MOV", (items[3] as MediaItem.Video).url)

    // Fifth video with query parameters
    assertTrue(items[4] is MediaItem.Video)
    assertEquals("https://example.com/other_video.mkv?alt=media", (items[4] as MediaItem.Video).url)
  }

  @Test
  fun `empty list returns empty`() {
    val items = parseMediaItems(emptyList())
    assertTrue(items.isEmpty())
  }
}
