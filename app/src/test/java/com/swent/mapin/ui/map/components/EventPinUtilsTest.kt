package com.swent.mapin.ui.map.components

import com.swent.mapin.R
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.location.Location
import org.junit.Assert.assertEquals
import org.junit.Test

class EventPinUtilsTest {

  private fun createEvent(
      tags: List<String> = emptyList(),
      capacity: Int? = null,
      participantCount: Int = 0
  ): Event {
    return Event(
        uid = "test",
        title = "Test Event",
        location = Location("Test", 0.0, 0.0),
        tags = tags,
        capacity = capacity,
        participantIds = (1..participantCount).map { "user$it" })
  }

  // EventPinType.fromEvent - cover each pin type once + DEFAULT cases

  @Test
  fun `fromEvent returns correct pin type for each category`() {
    // Each pin type matched once (singular forms work due to contains)
    assertEquals(EventPinType.MUSIC, EventPinType.fromEvent(createEvent(tags = listOf("Music"))))
    assertEquals(EventPinType.SPORTS, EventPinType.fromEvent(createEvent(tags = listOf("Sport"))))
    assertEquals(EventPinType.SPORTS, EventPinType.fromEvent(createEvent(tags = listOf("Sports"))))
    assertEquals(EventPinType.FOOD, EventPinType.fromEvent(createEvent(tags = listOf("Food"))))
    assertEquals(
        EventPinType.SCIENCE, EventPinType.fromEvent(createEvent(tags = listOf("Science"))))
  }

  @Test
  fun `fromEvent returns DEFAULT for empty tags or unknown tag`() {
    assertEquals(EventPinType.DEFAULT, EventPinType.fromEvent(createEvent(tags = emptyList())))
    assertEquals(
        EventPinType.DEFAULT, EventPinType.fromEvent(createEvent(tags = listOf("Unknown"))))
  }

  @Test
  fun `fromEvent is case insensitive and matches partial tags`() {
    assertEquals(
        EventPinType.MUSIC, EventPinType.fromEvent(createEvent(tags = listOf("LIVEMUSIC"))))
  }

  // calculateCapacityState - cover each branch once

  @Test
  fun `calculateCapacityState returns AVAILABLE for null or invalid capacity`() {
    assertEquals(CapacityState.AVAILABLE, calculateCapacityState(createEvent(capacity = null)))
    assertEquals(CapacityState.AVAILABLE, calculateCapacityState(createEvent(capacity = 0)))
  }

  @Test
  fun `calculateCapacityState returns correct state based on remaining percentage`() {
    // >50% remaining -> AVAILABLE
    assertEquals(
        CapacityState.AVAILABLE,
        calculateCapacityState(createEvent(capacity = 100, participantCount = 49)))
    // 10-50% remaining -> LIMITED
    assertEquals(
        CapacityState.LIMITED,
        calculateCapacityState(createEvent(capacity = 100, participantCount = 50)))
    // <=10% remaining -> FULL
    assertEquals(
        CapacityState.FULL,
        calculateCapacityState(createEvent(capacity = 100, participantCount = 90)))
  }

  // getEventPinDrawableRes - cover each capacity state with one pin type

  @Test
  fun `getEventPinDrawableRes returns correct icon for each capacity state`() {
    // Tests all 3 capacity states with different pin types
    assertEquals(
        R.drawable.ic_music_green,
        getEventPinDrawableRes(
            createEvent(tags = listOf("Music"), capacity = 100, participantCount = 20)))
    assertEquals(
        R.drawable.ic_sports_orange,
        getEventPinDrawableRes(
            createEvent(tags = listOf("Sports"), capacity = 100, participantCount = 70)))
    assertEquals(
        R.drawable.ic_food_red,
        getEventPinDrawableRes(
            createEvent(tags = listOf("Food"), capacity = 100, participantCount = 95)))
  }

  @Test
  fun `getEventPinDrawableRes returns default icons for unknown tags`() {
    assertEquals(
        R.drawable.ic_map_marker_green,
        getEventPinDrawableRes(createEvent(tags = listOf("Unknown"), capacity = null)))
  }

  // getEventPinInfo - verify data class construction

  @Test
  fun `getEventPinInfo returns correct combined info`() {
    val info =
        getEventPinInfo(
            createEvent(tags = listOf("Science"), capacity = 100, participantCount = 70))
    assertEquals(EventPinType.SCIENCE, info.pinType)
    assertEquals(CapacityState.LIMITED, info.capacityState)
    assertEquals(R.drawable.ic_science_orange, info.drawableRes)
  }
}
