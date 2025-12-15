package com.swent.mapin.ui.event

import com.swent.mapin.model.event.Event
import com.swent.mapin.model.location.Location
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventDetailSheetLogicTest {

  private fun baseEvent(
      capacity: Int? = 10,
      participants: List<String> = listOf("user1", "user2")
  ): Event {
    return Event(
        uid = "event",
        title = "Title",
        location = Location.from(name = "Paris", lat = 0.0, lng = 0.0),
        capacity = capacity,
        participantIds = participants)
  }

  @Test
  fun buildAttendeeInfoUi_withCapacityAndSpotsLeft_returnsCapacityText() {
    val info = buildAttendeeInfoUi(baseEvent(capacity = 10, participants = listOf("a", "b", "c")))

    assertEquals("3 attending", info.attendeeText)
    assertEquals("7 spots left", info.capacityText)
  }

  @Test
  fun buildAttendeeInfoUi_whenCapacityMissing_hidesCapacityText() {
    val info = buildAttendeeInfoUi(baseEvent(capacity = null))

    assertEquals("2 attending", info.attendeeText)
    assertEquals(null, info.capacityText)
  }

  @Test
  fun buildAttendeeInfoUi_whenEventFull_hidesCapacityText() {
    val info = buildAttendeeInfoUi(baseEvent(capacity = 2, participants = listOf("a", "b")))

    assertEquals("2 attending", info.attendeeText)
    assertEquals(null, info.capacityText)
  }

  @Test
  fun resolveJoinButtonUi_whenParticipating_hidesJoinButton() {
    val ui = resolveJoinButtonUi(baseEvent(), isParticipating = true)

    assertFalse(ui.showJoinButton)
    assertEquals("", ui.label)
    assertFalse(ui.enabled)
  }

  @Test
  fun resolveJoinButtonUi_whenEventAvailable_showsEnabledJoinButton() {
    val ui =
        resolveJoinButtonUi(
            baseEvent(capacity = 5, participants = listOf("a")), isParticipating = false)

    assertTrue(ui.showJoinButton)
    assertEquals("Join Event", ui.label)
    assertTrue(ui.enabled)
  }

  @Test
  fun resolveJoinButtonUi_whenEventFull_showsDisabledJoinButton() {
    val ui =
        resolveJoinButtonUi(
            baseEvent(capacity = 2, participants = listOf("a", "b")), isParticipating = false)

    assertTrue(ui.showJoinButton)
    assertEquals("Event is full", ui.label)
    assertFalse(ui.enabled)
  }

  @Test
  fun resolveJoinButtonUi_whenCapacityUnknown_stillEnablesJoinButton() {
    val ui =
        resolveJoinButtonUi(
            baseEvent(capacity = null, participants = emptyList()), isParticipating = false)

    assertTrue(ui.showJoinButton)
    assertEquals("Join Event", ui.label)
    assertTrue(ui.enabled)
  }

  @Test
  fun resolveSaveButtonUi_whenNotSaved_showsSaveForLater() {
    val ui = resolveSaveButtonUi(isSaved = false)

    assertTrue(ui.showSaveButton)
    assertEquals("Save for later", ui.label)
  }

  @Test
  fun resolveSaveButtonUi_whenAlreadySaved_showsUnsave() {
    val ui = resolveSaveButtonUi(isSaved = true)

    assertFalse(ui.showSaveButton)
    assertEquals("Unsave", ui.label)
  }
}
