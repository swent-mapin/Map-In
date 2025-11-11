package com.swent.mapin.ui

import com.swent.mapin.model.Location
import com.swent.mapin.ui.event.extractTags
import com.swent.mapin.ui.event.isValidLocation
import com.swent.mapin.ui.event.isValidTagInput
import junit.framework.TestCase.assertFalse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AddEventUtilsTest {
  @Test
  fun `returns true for blank input`() {
    assertTrue(isValidTagInput(""))
    assertTrue(isValidTagInput("   ")) // whitespace only
  }

  @Test
  fun `returns true for single valid tag`() {
    assertTrue(isValidTagInput("#food"))
    assertTrue(isValidTagInput("#fun_2025"))
  }

  @Test
  fun `returns true for multiple valid tags separated by commas`() {
    assertTrue(isValidTagInput("#food,#travel"))
    assertTrue(isValidTagInput("#food ,#travel"))
    assertTrue(isValidTagInput("#food , #travel , #music"))
  }

  @Test
  fun `returns false for tags missing hash or invalid format`() {
    assertFalse(isValidTagInput("food")) // missing #
    assertFalse(isValidTagInput("#food travel")) // second missing #
    assertFalse(isValidTagInput("#food,travel")) // invalid second
    assertFalse(isValidTagInput("#")) // incomplete tag
    assertFalse(isValidTagInput("#food,,")) // trailing commas
    assertFalse(isValidTagInput("#food, #")) // dangling #
  }

  // ---------- Tests for extractTags ----------

  @Test
  fun `extracts single tag correctly`() {
    val result = extractTags("#food")
    assertEquals(listOf("#food"), result)
  }

  @Test
  fun `extracts multiple tags with mixed separators`() {
    val result = extractTags("#food , #travel,#music")
    assertEquals(listOf("#food", "#travel", "#music"), result)
  }

  @Test
  fun `returns empty list when no tags present`() {
    val result = extractTags("no tags here")
    assertTrue(result.isEmpty())
  }

  // ---------- Tests for isValidLocation ----------

  @Test
  fun `returns true when location name matches ignoring case`() {
    val locations =
        listOf(
            Location("Paris", 0.0, 0.0),
            Location("London", 0.0, 0.0),
            Location("New York", 0.0, 0.0))
    assertTrue(isValidLocation("paris", locations))
    assertTrue(isValidLocation("LONDON", locations))
  }

  @Test
  fun `returns false when location not in list`() {
    val locations = listOf(Location("Berlin", 0.0, 0.0), Location("Rome", 0.0, 0.0))
    assertFalse(isValidLocation("Madrid", locations))
  }

  @Test
  fun `returns false for empty location list`() {
    val locations = emptyList<Location>()
    assertFalse(isValidLocation("Anything", locations))
  }
}
