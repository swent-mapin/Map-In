package com.swent.mapin.ui.map

import com.google.gson.JsonPrimitive
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.location.Location
import com.swent.mapin.ui.map.components.computeAnnotationVisualParameters
import com.swent.mapin.ui.map.components.createAnnotationStyle
import com.swent.mapin.ui.map.components.createClusterConfig
import com.swent.mapin.ui.map.components.createEventAnnotations
import com.swent.mapin.ui.map.components.findEventForAnnotation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class MapScreenHelpersTest {

  private fun sampleEvent(uid: String, latitude: Double = 1.0, longitude: Double = 2.0): Event {
    return Event(
        uid = uid,
        title = "Event $uid",
        location = Location.from(name = "Loc$uid", lat = latitude, lng = longitude))
  }

  @Test
  fun computeAnnotationVisualParameters_selectedUsesHighlightedValues() {
    val visual = computeAnnotationVisualParameters(true)

    assertEquals(1.5, visual.iconSize, 0.0)
    assertEquals(15.0, visual.textSize, 0.0)
    assertEquals(listOf(0.0, 0.5), visual.textOffset)
    assertEquals(2.0, visual.textHaloWidth, 0.0)
    assertEquals(0.0, visual.sortKey, 0.0)
  }

  @Test
  fun computeAnnotationVisualParameters_defaultUsesBaselineValues() {
    val visual = computeAnnotationVisualParameters(false)

    assertEquals(1.0, visual.iconSize, 0.0)
    assertEquals(12.0, visual.textSize, 0.0)
    assertEquals(listOf(0.0, 0.2), visual.textOffset)
    assertEquals(1.5, visual.textHaloWidth, 0.0)
    assertEquals(100.0, visual.sortKey, 0.0)
  }

  @Test
  fun createAnnotationStyle_switchesPaletteWithTheme() {
    val dark = createAnnotationStyle(isDarkTheme = true, markerBitmap = null)
    val light = createAnnotationStyle(isDarkTheme = false, markerBitmap = null)

    assertTrue(dark.textColorInt != light.textColorInt)
    assertTrue(dark.haloColorInt != light.haloColorInt)
    assertNull(dark.markerBitmap)
    assertNull(light.markerBitmap)
  }

  @Test
  fun createEventAnnotations_assignsUniqueDataAndSortKey() {
    val events = listOf(sampleEvent("1"), sampleEvent("2"))
    val style = createAnnotationStyle(isDarkTheme = false, markerBitmap = null)

    val annotations = createEventAnnotations(events, style, selectedEventId = "2")

    assertEquals(2, annotations.size)

    val first = annotations[0]
    val second = annotations[1]

    assertEquals(JsonPrimitive(0), first.getData())
    assertEquals(JsonPrimitive(1), second.getData())

    assertEquals(100.0, first.javaField<Double>("symbolSortKey"), 0.0)
    assertEquals(1.0, first.javaField<Double>("iconSize"), 0.0)

    assertEquals(0.0, second.javaField<Double>("symbolSortKey"), 0.0)
    assertEquals(1.5, second.javaField<Double>("iconSize"), 0.0)
  }

  @Test
  fun findEventForAnnotation_prefersIndexData() {
    val events = listOf(sampleEvent("1"), sampleEvent("2"))
    val annotation = mock<PointAnnotation>()
    whenever(annotation.getData()).thenReturn(JsonPrimitive(1))

    val result = findEventForAnnotation(annotation, events)

    assertEquals(events[1], result)
  }

  @Test
  fun findEventForAnnotation_fallsBackToCoordinates() {
    val events = listOf(sampleEvent("1", latitude = 5.0, longitude = 6.0))
    val annotation = mock<PointAnnotation>()
    whenever(annotation.getData()).thenReturn(null)
    whenever(annotation.point).thenReturn(Point.fromLngLat(6.0, 5.0))

    val result = findEventForAnnotation(annotation, events)

    assertEquals(events[0], result)
  }

  @Test
  fun createClusterConfig_setsExpectedDefaults() {
    val config = createClusterConfig()
    val options = config.annotationSourceOptions?.clusterOptions

    assertNotNull(options)
    options ?: return
    assertEquals(60L, options.clusterRadius)
    assertEquals(3, options.colorLevels.size)
    assertEquals(android.graphics.Color.WHITE, options.textColor)
    assertEquals(12.0, options.textSize, 0.0)
  }

  // Reflection helper to access generated fields on PointAnnotationOptions
  private inline fun <reified T> PointAnnotationOptions.javaField(name: String): T {
    val field = PointAnnotationOptions::class.java.getDeclaredField(name)
    field.isAccessible = true
    return field.get(this) as T
  }
}
