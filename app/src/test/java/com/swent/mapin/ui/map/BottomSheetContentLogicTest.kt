package com.swent.mapin.ui.map

import com.swent.mapin.ui.map.bottomsheet.components.buildNoResultsCopy
import com.swent.mapin.ui.map.bottomsheet.components.buildSearchHeading
import org.junit.Assert.assertEquals
import org.junit.Test

class BottomSheetContentLogicTest {

  @Test
  fun buildNoResultsCopy_whenQueryBlank_returnsEmptyStateMessages() {
    val copy = buildNoResultsCopy("")

    assertEquals("Start typing to search", copy.title)
    assertEquals("Search for events by name or location", copy.subtitle)
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
