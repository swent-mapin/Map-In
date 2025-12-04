package com.swent.mapin.model

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for managing app preferences using DataStore. Handles theme settings, map style
 * preferences, and map visibility options.
 *
 * WARNING: Do not instantiate this class directly with an Activity context to avoid memory leaks.
 * Always use PreferencesRepositoryProvider.getRepository() which provides an instance backed by
 * applicationContext.
 */
class PreferencesRepository(private val context: Context) {

  // WARNING: Extension property on Context - ensure only Application context is used
  // to prevent memory leaks. Use PreferencesRepositoryProvider for proper instantiation.
  private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

  companion object {
    // Theme preferences
    val THEME_MODE = stringPreferencesKey("theme_mode") // "light", "dark", "system"

    // Mapbox style preferences
    val MAP_STYLE = stringPreferencesKey("map_style") // "standard", "satellite"

    // Map visibility preferences
    val SHOW_POIS = booleanPreferencesKey("show_pois")
    val SHOW_ROAD_NUMBERS = booleanPreferencesKey("show_road_numbers")
    val SHOW_STREET_NAMES = booleanPreferencesKey("show_street_names")
    val ENABLE_3D_VIEW = booleanPreferencesKey("enable_3d_view")

    // Camera position preferences
    val CAMERA_LATITUDE = doublePreferencesKey("camera_latitude")
    val CAMERA_LONGITUDE = doublePreferencesKey("camera_longitude")
    val CAMERA_ZOOM = doublePreferencesKey("camera_zoom")
  }

  /** Flow for theme mode: "light", "dark", or "system" */
  val themeModeFlow: Flow<String> =
      context.dataStore.data.map { preferences -> preferences[THEME_MODE] ?: "system" }

  /** Flow for map style: "standard" or "satellite" */
  val mapStyleFlow: Flow<String> =
      context.dataStore.data.map { preferences -> preferences[MAP_STYLE] ?: "standard" }

  /** Flow for POIs visibility */
  val showPOIsFlow: Flow<Boolean> =
      context.dataStore.data.map { preferences -> preferences[SHOW_POIS] ?: true }

  /** Flow for road numbers visibility */
  val showRoadNumbersFlow: Flow<Boolean> =
      context.dataStore.data.map { preferences -> preferences[SHOW_ROAD_NUMBERS] ?: true }

  /** Flow for street names visibility */
  val showStreetNamesFlow: Flow<Boolean> =
      context.dataStore.data.map { preferences -> preferences[SHOW_STREET_NAMES] ?: true }

  /** Flow for 3D view setting */
  val enable3DViewFlow: Flow<Boolean> =
      context.dataStore.data.map { preferences -> preferences[ENABLE_3D_VIEW] ?: true }

  /** Update theme mode */
  suspend fun setThemeMode(mode: String) {
    context.dataStore.edit { preferences -> preferences[THEME_MODE] = mode }
  }

  /** Update map style */
  suspend fun setMapStyle(style: String) {
    context.dataStore.edit { preferences -> preferences[MAP_STYLE] = style }
  }

  /** Update POIs visibility */
  suspend fun setShowPOIs(show: Boolean) {
    context.dataStore.edit { preferences -> preferences[SHOW_POIS] = show }
  }

  /** Update road numbers visibility */
  suspend fun setShowRoadNumbers(show: Boolean) {
    context.dataStore.edit { preferences -> preferences[SHOW_ROAD_NUMBERS] = show }
  }

  /** Update street names visibility */
  suspend fun setShowStreetNames(show: Boolean) {
    context.dataStore.edit { preferences -> preferences[SHOW_STREET_NAMES] = show }
  }

  /** Update 3D view setting */
  suspend fun setEnable3DView(enable: Boolean) {
    context.dataStore.edit { preferences -> preferences[ENABLE_3D_VIEW] = enable }
  }

  /** Flow for camera latitude (null if never set) */
  val cameraLatitudeFlow: Flow<Double?> =
      context.dataStore.data.map { preferences -> preferences[CAMERA_LATITUDE] }

  /** Flow for camera longitude (null if never set) */
  val cameraLongitudeFlow: Flow<Double?> =
      context.dataStore.data.map { preferences -> preferences[CAMERA_LONGITUDE] }

  /** Flow for camera zoom (null if never set) */
  val cameraZoomFlow: Flow<Double?> =
      context.dataStore.data.map { preferences -> preferences[CAMERA_ZOOM] }

  /** Save camera position */
  suspend fun saveCameraPosition(latitude: Double, longitude: Double, zoom: Double) {
    context.dataStore.edit { preferences ->
      preferences[CAMERA_LATITUDE] = latitude
      preferences[CAMERA_LONGITUDE] = longitude
      preferences[CAMERA_ZOOM] = zoom
    }
  }
}
