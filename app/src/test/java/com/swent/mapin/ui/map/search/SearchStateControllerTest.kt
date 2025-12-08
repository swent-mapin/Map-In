package com.swent.mapin.ui.map.search

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.Timestamp
import com.swent.mapin.model.Location
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Targeted tests for SearchStateController covering uncovered branches. */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchStateControllerTest {

  private lateinit var mockContext: Context
  private lateinit var mockSharedPrefs: SharedPreferences
  private lateinit var mockEditor: SharedPreferences.Editor
  private lateinit var mockEventRepository: EventRepository
  private lateinit var onClearFocus: () -> Unit
  private lateinit var controller: SearchStateController

  private val sampleEvents =
      listOf(
          Event(
              uid = "event1",
              title = "Test Event 1",
              description = "Description 1",
              date = Timestamp.now(),
              location = Location("Location1", 0.0, 0.0),
              tags = listOf("tag1"),
              ownerId = "owner1"),
          Event(
              uid = "event2",
              title = "Test Event 2",
              description = "Description 2",
              date = Timestamp.now(),
              location = Location("Location2", 0.0, 0.0),
              tags = listOf("tag2"),
              ownerId = "owner2"))

  @Before
  fun setup() {
    mockContext = mock(Context::class.java)
    mockSharedPrefs = mock(SharedPreferences::class.java)
    mockEditor = mock(SharedPreferences.Editor::class.java)
    mockEventRepository = mock(EventRepository::class.java)
    onClearFocus = mock()

    whenever(mockContext.getSharedPreferences(any(), any())).thenReturn(mockSharedPrefs)
    whenever(mockSharedPrefs.edit()).thenReturn(mockEditor)
    whenever(mockEditor.putString(any(), any())).thenReturn(mockEditor)
    whenever(mockEditor.remove(any())).thenReturn(mockEditor)
    whenever(mockSharedPrefs.getString(any(), any())).thenReturn("[]")

    controller = createController()
  }

  private fun createController(testScope: TestScope = TestScope()): SearchStateController {
    return SearchStateController(applicationContext = mockContext, onClearFocus = onClearFocus)
  }

  // Test that saveRecentEvent adds event to recent items list
  @Test
  fun saveRecentEvent_addsNewEventToRecents() {
    val eventId = "event123"
    val eventTitle = "New Event"

    controller.saveRecentEvent(eventId, eventTitle)

    assertEquals("Should have one recent item", 1, controller.recentItems.size)
    assertTrue(
        "Recent item should be ClickedEvent", controller.recentItems[0] is RecentItem.ClickedEvent)
    val clickedEvent = controller.recentItems[0] as RecentItem.ClickedEvent
    assertEquals("Event ID should match", eventId, clickedEvent.eventId)
    assertEquals("Event title should match", eventTitle, clickedEvent.eventTitle)
  }

  // Test that saveRecentEvent persists to SharedPreferences
  @Test
  fun saveRecentEvent_updatesSharedPreferences() {
    val eventId = "event456"
    val eventTitle = "Updated Event"
    val stringCaptor = argumentCaptor<String>()

    controller.saveRecentEvent(eventId, eventTitle)

    verify(mockEditor).putString(eq("recent_items"), stringCaptor.capture())
    verify(mockEditor, times(1)).apply()

    val savedJson = stringCaptor.firstValue
    assertTrue("Saved JSON should contain event type", savedJson.contains("\"type\":\"event\""))
    assertTrue("Saved JSON should contain event ID", savedJson.contains(eventId))
    assertTrue("Saved JSON should contain event title", savedJson.contains(eventTitle))
  }

  // Test that duplicate event IDs are deduplicated and moved to front
  @Test
  fun saveRecentEvent_withDuplicateEventId_movesToFront() {
    val eventId = "event789"
    val originalTitle = "Original Title"
    val updatedTitle = "Updated Title"

    controller.saveRecentEvent(eventId, originalTitle)
    val firstSize = controller.recentItems.size

    controller.saveRecentEvent(eventId, updatedTitle)

    assertEquals("Should still have same number of items", firstSize, controller.recentItems.size)
    val firstItem = controller.recentItems[0] as RecentItem.ClickedEvent
    assertEquals("Event should be at front", eventId, firstItem.eventId)
    assertEquals("Title should be updated", updatedTitle, firstItem.eventTitle)
  }

  // Test that saveRecentEvent preserves existing search items
  @Test
  fun saveRecentEvent_preservesExistingSearchItems() {
    val searchQuery = "test query"
    whenever(mockSharedPrefs.getString("recent_items", "[]"))
        .thenReturn("""[{"type":"search","value":"$searchQuery"}]""")

    controller.loadRecentSearches()

    val eventId = "event999"
    val eventTitle = "New Event"

    controller.saveRecentEvent(eventId, eventTitle)

    assertEquals("Should have both event and search", 2, controller.recentItems.size)
    assertTrue(
        "First item should be ClickedEvent", controller.recentItems[0] is RecentItem.ClickedEvent)
    assertTrue("Second item should be Search", controller.recentItems[1] is RecentItem.Search)
    assertEquals(
        "Search query should be preserved",
        searchQuery,
        (controller.recentItems[1] as RecentItem.Search).query)
  }

  // Test exception handling when saving fails
  @Test
  fun saveRecentEvent_whenExceptionInSaving_doesNotCrash() {
    whenever(mockEditor.putString(any(), any())).doThrow(RuntimeException("Storage error"))

    try {
      controller.saveRecentEvent("event-error", "Error Event")
      assertTrue("Should handle exception gracefully", true)
    } catch (e: Exception) {
      fail("Should not throw exception: ${e.message}")
    }
  }

  // Test deserialization of search type from JSON
  @Test
  fun loadRecentSearches_deserializesSearchTypeCorrectly() {
    val searchJson = """[{"type":"search","value":"test query"}]"""
    whenever(mockSharedPrefs.getString("recent_items", "[]")).thenReturn(searchJson)

    controller.loadRecentSearches()

    assertEquals("Should have one item", 1, controller.recentItems.size)
    assertTrue("Item should be Search type", controller.recentItems[0] is RecentItem.Search)
    assertEquals(
        "Search query should match",
        "test query",
        (controller.recentItems[0] as RecentItem.Search).query)
  }

  // Test deserialization of event type from JSON
  @Test
  fun loadRecentSearches_deserializesEventTypeCorrectly() {
    val eventJson = """[{"type":"event","value":"event123","eventTitle":"My Event"}]"""
    whenever(mockSharedPrefs.getString("recent_items", "[]")).thenReturn(eventJson)

    controller.loadRecentSearches()

    assertEquals("Should have one item", 1, controller.recentItems.size)
    assertTrue(
        "Item should be ClickedEvent type", controller.recentItems[0] is RecentItem.ClickedEvent)
    val clickedEvent = controller.recentItems[0] as RecentItem.ClickedEvent
    assertEquals("Event ID should match", "event123", clickedEvent.eventId)
    assertEquals("Event title should match", "My Event", clickedEvent.eventTitle)
  }

  // Test that events without eventTitle are filtered out
  @Test
  fun loadRecentSearches_whenEventTypeMissingTitle_returnsNull() {
    val eventJson = """[{"type":"event","value":"event456"}]"""
    whenever(mockSharedPrefs.getString("recent_items", "[]")).thenReturn(eventJson)

    controller.loadRecentSearches()

    assertEquals("Should filter out event without title", 0, controller.recentItems.size)
  }

  // Test that unknown types are filtered out during deserialization
  @Test
  fun loadRecentSearches_withUnknownType_filtersItOut() {
    val mixedJson =
        """[
      {"type":"search","value":"query1"},
      {"type":"unknown","value":"data"},
      {"type":"event","value":"event1","eventTitle":"Event 1"}
    ]"""
    whenever(mockSharedPrefs.getString("recent_items", "[]")).thenReturn(mixedJson)

    controller.loadRecentSearches()

    assertEquals("Should have 2 items (unknown filtered out)", 2, controller.recentItems.size)
    assertTrue("First should be search", controller.recentItems[0] is RecentItem.Search)
    assertTrue("Second should be event", controller.recentItems[1] is RecentItem.ClickedEvent)
  }

  // Test exception handling for malformed JSON
  @Test
  fun loadRecentSearches_withMalformedJson_handlesGracefully() {
    whenever(mockSharedPrefs.getString("recent_items", "[]")).thenReturn("{malformed json")

    controller.loadRecentSearches()

    assertNotNull("Recent items should not be null", controller.recentItems)
    assertEquals("Should have empty list on malformed JSON", 0, controller.recentItems.size)
  }

  // Tests for ClickedProfile type and saveRecentProfile method

  @Test
  fun saveRecentProfile_addsNewProfileToRecents() {
    val userId = "user123"
    val userName = "John Doe"

    controller.saveRecentProfile(userId, userName)

    assertEquals("Should have one recent item", 1, controller.recentItems.size)
    assertTrue(
        "Recent item should be ClickedProfile",
        controller.recentItems[0] is RecentItem.ClickedProfile)
    val clickedProfile = controller.recentItems[0] as RecentItem.ClickedProfile
    assertEquals("User ID should match", userId, clickedProfile.userId)
    assertEquals("User name should match", userName, clickedProfile.userName)
  }

  @Test
  fun saveRecentProfile_updatesSharedPreferences() {
    val userId = "user456"
    val userName = "Jane Smith"
    val stringCaptor = argumentCaptor<String>()

    controller.saveRecentProfile(userId, userName)

    verify(mockEditor).putString(eq("recent_items"), stringCaptor.capture())
    verify(mockEditor, times(1)).apply()

    val savedJson = stringCaptor.firstValue
    assertTrue("Saved JSON should contain profile type", savedJson.contains("\"type\":\"profile\""))
    assertTrue("Saved JSON should contain user ID", savedJson.contains(userId))
    assertTrue("Saved JSON should contain user name", savedJson.contains(userName))
  }

  @Test
  fun saveRecentProfile_withDuplicateUserId_movesToFront() {
    val userId = "user789"
    val originalName = "Original Name"
    val updatedName = "Updated Name"

    controller.saveRecentProfile(userId, originalName)
    val firstSize = controller.recentItems.size

    controller.saveRecentProfile(userId, updatedName)

    assertEquals("Should still have same number of items", firstSize, controller.recentItems.size)
    val firstItem = controller.recentItems[0] as RecentItem.ClickedProfile
    assertEquals("Profile should be at front", userId, firstItem.userId)
    assertEquals("Name should be updated", updatedName, firstItem.userName)
  }

  @Test
  fun saveRecentProfile_preservesExistingSearchAndEventItems() {
    val searchQuery = "test query"
    val eventId = "event1"
    val eventTitle = "Test Event"
    whenever(mockSharedPrefs.getString("recent_items", "[]"))
        .thenReturn(
            """[{"type":"search","value":"$searchQuery"},{"type":"event","value":"$eventId","eventTitle":"$eventTitle"}]""")

    controller.loadRecentSearches()

    val userId = "user999"
    val userName = "New User"

    controller.saveRecentProfile(userId, userName)

    assertEquals("Should have three items", 3, controller.recentItems.size)
    assertTrue(
        "First item should be ClickedProfile",
        controller.recentItems[0] is RecentItem.ClickedProfile)
    assertTrue("Second item should be Search", controller.recentItems[1] is RecentItem.Search)
    assertTrue(
        "Third item should be ClickedEvent", controller.recentItems[2] is RecentItem.ClickedEvent)
  }

  @Test
  fun loadRecentSearches_deserializesProfileTypeCorrectly() {
    val profileJson = """[{"type":"profile","value":"user123","eventTitle":"John Doe"}]"""
    whenever(mockSharedPrefs.getString("recent_items", "[]")).thenReturn(profileJson)

    controller.loadRecentSearches()

    assertEquals("Should have one item", 1, controller.recentItems.size)
    assertTrue(
        "Item should be ClickedProfile type",
        controller.recentItems[0] is RecentItem.ClickedProfile)
    val clickedProfile = controller.recentItems[0] as RecentItem.ClickedProfile
    assertEquals("User ID should match", "user123", clickedProfile.userId)
    assertEquals("User name should match", "John Doe", clickedProfile.userName)
  }

  @Test
  fun saveRecentProfile_whenExceptionInSaving_doesNotCrash() {
    whenever(mockEditor.putString(any(), any())).doThrow(RuntimeException("Storage error"))

    try {
      controller.saveRecentProfile("user-error", "Error User")
      assertTrue("Should handle exception gracefully", true)
    } catch (e: Exception) {
      fail("Should not throw exception: ${e.message}")
    }
  }
}
