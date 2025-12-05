package com.swent.mapin.ui.settings

import org.junit.Assert.*
import org.junit.Test

class ThemeModeTest {

  @Test
  fun `fromString returns correct values`() {
    assertEquals(ThemeMode.LIGHT, ThemeMode.fromString("light"))
    assertEquals(ThemeMode.DARK, ThemeMode.fromString("dark"))
    assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("system"))
    assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("unknown"))
  }

  @Test
  fun `fromString is case insensitive`() {
    assertEquals(ThemeMode.LIGHT, ThemeMode.fromString("LIGHT"))
    assertEquals(ThemeMode.DARK, ThemeMode.fromString("Dark"))
  }

  @Test
  fun `toDisplayString returns correct values`() {
    assertEquals("Light", ThemeMode.LIGHT.toDisplayString())
    assertEquals("Dark", ThemeMode.DARK.toDisplayString())
    assertEquals("System", ThemeMode.SYSTEM.toDisplayString())
  }

  @Test
  fun `toStorageString returns correct values`() {
    assertEquals("light", ThemeMode.LIGHT.toStorageString())
    assertEquals("dark", ThemeMode.DARK.toStorageString())
    assertEquals("system", ThemeMode.SYSTEM.toStorageString())
  }

  @Test
  fun `storage string round trip preserves value`() {
    for (mode in ThemeMode.entries) {
      assertEquals(mode, ThemeMode.fromString(mode.toStorageString()))
    }
  }

  @Test
  fun `entries contains all modes`() {
    assertEquals(3, ThemeMode.entries.size)
  }
}

class MapPreferencesTest {

  @Test
  fun `default constructor has all values true`() {
    val prefs = MapPreferences()
    assertTrue(prefs.showPOIs)
    assertTrue(prefs.showRoadNumbers)
    assertTrue(prefs.showStreetNames)
    assertTrue(prefs.enable3DView)
  }

  @Test
  fun `custom constructor sets values`() {
    val prefs =
        MapPreferences(
            showPOIs = false, showRoadNumbers = true, showStreetNames = false, enable3DView = true)
    assertFalse(prefs.showPOIs)
    assertTrue(prefs.showRoadNumbers)
    assertFalse(prefs.showStreetNames)
    assertTrue(prefs.enable3DView)
  }

  @Test
  fun `copy creates independent copy`() {
    val original = MapPreferences()
    val copy = original.copy(showPOIs = false)
    assertFalse(copy.showPOIs)
    assertTrue(original.showPOIs)
  }

  @Test
  fun `equals works correctly`() {
    assertEquals(MapPreferences(), MapPreferences())
    assertNotEquals(MapPreferences(showPOIs = true), MapPreferences(showPOIs = false))
  }
}
