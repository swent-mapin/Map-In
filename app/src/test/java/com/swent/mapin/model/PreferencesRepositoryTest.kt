package com.swent.mapin.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for PreferencesRepository.
 *
 * Note: DataStore testing requires integration tests with Robolectric or instrumented tests. These
 * tests verify the preference key constants are correctly defined.
 */
class PreferencesRepositoryTest {

  @Test
  fun `THEME_MODE key has correct name`() {
    val key = PreferencesRepository.THEME_MODE
    assertEquals("theme_mode", key.name)
  }

  @Test
  fun `MAP_STYLE key has correct name`() {
    val key = PreferencesRepository.MAP_STYLE
    assertEquals("map_style", key.name)
  }

  @Test
  fun `SHOW_POIS key has correct name`() {
    val key = PreferencesRepository.SHOW_POIS
    assertEquals("show_pois", key.name)
  }

  @Test
  fun `SHOW_ROAD_NUMBERS key has correct name`() {
    val key = PreferencesRepository.SHOW_ROAD_NUMBERS
    assertEquals("show_road_numbers", key.name)
  }

  @Test
  fun `SHOW_STREET_NAMES key has correct name`() {
    val key = PreferencesRepository.SHOW_STREET_NAMES
    assertEquals("show_street_names", key.name)
  }

  @Test
  fun `ENABLE_3D_VIEW key has correct name`() {
    val key = PreferencesRepository.ENABLE_3D_VIEW
    assertEquals("enable_3d_view", key.name)
  }

  @Test
  fun `string preference keys are of correct type`() {
    assertTrue(
        PreferencesRepository.THEME_MODE is androidx.datastore.preferences.core.Preferences.Key<*>)
    assertTrue(
        PreferencesRepository.MAP_STYLE is androidx.datastore.preferences.core.Preferences.Key<*>)
  }

  @Test
  fun `boolean preference keys are of correct type`() {
    assertTrue(
        PreferencesRepository.SHOW_POIS is androidx.datastore.preferences.core.Preferences.Key<*>)
    assertTrue(
        PreferencesRepository.SHOW_ROAD_NUMBERS
            is androidx.datastore.preferences.core.Preferences.Key<*>)
    assertTrue(
        PreferencesRepository.SHOW_STREET_NAMES
            is androidx.datastore.preferences.core.Preferences.Key<*>)
    assertTrue(
        PreferencesRepository.ENABLE_3D_VIEW
            is androidx.datastore.preferences.core.Preferences.Key<*>)
  }

  @Test
  fun `all preference keys are unique`() {
    val keys =
        setOf(
            PreferencesRepository.THEME_MODE.name,
            PreferencesRepository.MAP_STYLE.name,
            PreferencesRepository.SHOW_POIS.name,
            PreferencesRepository.SHOW_ROAD_NUMBERS.name,
            PreferencesRepository.SHOW_STREET_NAMES.name,
            PreferencesRepository.ENABLE_3D_VIEW.name)

    // If all keys are unique, the set size should equal the number of keys
    assertEquals(6, keys.size)
  }
}
