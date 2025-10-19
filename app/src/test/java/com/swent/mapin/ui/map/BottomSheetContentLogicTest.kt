package com.swent.mapin.ui.map

import org.junit.Assert.assertEquals
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
  fun buildSearchHeading_whenQueryBlank_returnsAllEvents() {
    assertEquals("All events", buildSearchHeading(""))
  }

  @Test
  fun buildSearchHeading_whenQueryProvided_quotesQuery() {
    assertEquals("Results for \"concert\"", buildSearchHeading("concert"))
  }
}
