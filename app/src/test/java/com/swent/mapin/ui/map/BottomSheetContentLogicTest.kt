package com.swent.mapin.ui.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomSheetContentLogicTest {

  @Test
  fun buildNoResultsCopy_whenQueryBlank_returnsEmptyStateMessages() {
    val copy = buildNoResultsCopy("")

    assertEquals("No events available yet.", copy.title)
    assertEquals("Try again once events are added.", copy.subtitle)
  }

  @Test
  fun buildNoResultsCopy_whenQueryProvided_returnsSearchMessages() {
    val copy = buildNoResultsCopy("concert")

    assertEquals("No results found", copy.title)
    assertEquals("Try a different keyword or check the spelling.", copy.subtitle)
  }

  @Test
  fun buildNoResultsCopy_treatsWhitespaceAsBlank() {
    val copy = buildNoResultsCopy("   ")
    assertEquals("No events available yet.", copy.title)
    assertEquals("Try again once events are added.", copy.subtitle)
  }

  @Test
  fun buildSearchHeading_whenQueryBlank_returnsAllEvents() {
    assertEquals("All events", buildSearchHeading(""))
  }

  @Test
  fun buildSearchHeading_whenQueryProvided_quotesQuery() {
    assertEquals("Results for \"concert\"", buildSearchHeading("concert"))
  }

  @Test
  fun buildSearchHeading_handlesQuotesAndWhitespace() {
    assertEquals("Results for \"a b\"", buildSearchHeading("a b"))
    assertEquals("All events", buildSearchHeading("   "))
  }

  @Test
  fun isProfileVisible_showsOnlyWhenNotFocusedAndNotInSearchMode() {
    assertTrue(isProfileVisible(isFocused = false, isSearchMode = false))
    assertFalse(isProfileVisible(isFocused = true, isSearchMode = false))
    assertFalse(isProfileVisible(isFocused = false, isSearchMode = true))
    assertFalse(isProfileVisible(isFocused = true, isSearchMode = true))
  }

  @Test
  fun buildTagsSummary_handlesDifferentCounts() {
    assertEquals("", buildTagsSummary(emptyList()))
    assertEquals("a", buildTagsSummary(listOf("a")))
    assertEquals("a • b", buildTagsSummary(listOf("a", "b")))
    assertEquals("a • b • c", buildTagsSummary(listOf("a", "b", "c")))
    // more than 3 tags should be truncated to first 3
    assertEquals("a • b • c", buildTagsSummary(listOf("a", "b", "c", "d")))
  }

  @Test
  fun buildTagsSummary_handlesEmptyAndSpecialTags() {
    assertEquals("", buildTagsSummary(listOf("")))
    assertEquals(" •  ", buildTagsSummary(listOf("", " ")))
    // ensure separator is literal and tags are preserved
    assertEquals("a • b • c", buildTagsSummary(listOf("a", "b", "c")))
  }

  @Test
  fun isRemoteAvatarUrl_recognizesHttpAndHttps() {
    assertTrue(isRemoteAvatarUrl("http://example.com/avatar.png"))
    assertTrue(isRemoteAvatarUrl("https://example.com/avatar.png"))
    assertFalse(isRemoteAvatarUrl(null))
    assertFalse(isRemoteAvatarUrl(""))
    assertFalse(isRemoteAvatarUrl("ftp://example.com/avatar.png"))
    assertFalse(isRemoteAvatarUrl("www.example.com/avatar.png"))
  }

  @Test
  fun isRemoteAvatarUrl_edgeCases() {
    // uppercase scheme should be accepted by the implementation (we normalize to lowercase)
    assertTrue(isRemoteAvatarUrl("HTTPS://example.com/avatar.png"))
    // leading/trailing spaces -> normalized, should be recognized
    assertTrue(isRemoteAvatarUrl(" https://example.com/avatar.png"))
    assertTrue(isRemoteAvatarUrl("https://example.com/avatar.png "))
    // valid normal case remains true
    assertTrue(isRemoteAvatarUrl("https://example.com/avatar.png"))
  }
}
