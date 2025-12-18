package com.swent.mapin.model

import androidx.datastore.preferences.core.Preferences
import com.swent.mapin.model.preferences.PreferencesRepository
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
  @Suppress("USELESS_IS_CHECK") // Intentional: verifies keys are initialized correctly at runtime
  fun `string preference keys are of correct type`() {
    // Note: Due to type erasure, we cannot verify the generic type parameter (String)
    // at runtime. We verify the keys are Preferences.Key instances, which is sufficient
    // to catch initialization errors. The specific type (String vs Boolean) is enforced
    // at compile time by the factory functions (stringPreferencesKey vs booleanPreferencesKey).
    assertTrue(PreferencesRepository.THEME_MODE is Preferences.Key<*>)
    assertTrue(PreferencesRepository.MAP_STYLE is Preferences.Key<*>)

    // Additional verification: keys should have non-null names
    assertNotNull(PreferencesRepository.THEME_MODE.name)
    assertNotNull(PreferencesRepository.MAP_STYLE.name)
  }

  @Test
  @Suppress("USELESS_IS_CHECK") // Intentional: verifies keys are initialized correctly at runtime
  fun `boolean preference keys are of correct type`() {
    // Note: Due to type erasure, we cannot verify the generic type parameter (Boolean)
    // at runtime. We verify the keys are Preferences.Key instances and have valid names.
    assertTrue(PreferencesRepository.SHOW_POIS is Preferences.Key<*>)
    assertTrue(PreferencesRepository.SHOW_ROAD_NUMBERS is Preferences.Key<*>)
    assertTrue(PreferencesRepository.SHOW_STREET_NAMES is Preferences.Key<*>)
    assertTrue(PreferencesRepository.ENABLE_3D_VIEW is Preferences.Key<*>)

    // Additional verification: keys should have non-null names
    assertNotNull(PreferencesRepository.SHOW_POIS.name)
    assertNotNull(PreferencesRepository.SHOW_ROAD_NUMBERS.name)
    assertNotNull(PreferencesRepository.SHOW_STREET_NAMES.name)
    assertNotNull(PreferencesRepository.ENABLE_3D_VIEW.name)
  }

  @Test
  fun `all preference keys are unique`() {
    // Collect all key names in a list to check for duplicates
    val keyNames =
        listOf(
            PreferencesRepository.THEME_MODE.name,
            PreferencesRepository.MAP_STYLE.name,
            PreferencesRepository.SHOW_POIS.name,
            PreferencesRepository.SHOW_ROAD_NUMBERS.name,
            PreferencesRepository.SHOW_STREET_NAMES.name,
            PreferencesRepository.ENABLE_3D_VIEW.name)

    // Convert to set and compare sizes - if all keys are unique, sizes should match
    val uniqueKeyNames = keyNames.toSet()
    assertEquals(
        "Duplicate key names detected. All preference keys must have unique names.",
        keyNames.size,
        uniqueKeyNames.size)
  }
}
