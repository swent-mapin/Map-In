package com.swent.mapin.ui

import com.google.firebase.Timestamp
import com.swent.mapin.model.Location
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepository
import com.swent.mapin.ui.event.EventViewModel
import com.swent.mapin.ui.map.Filters
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

@ExperimentalCoroutinesApi
class EventViewModelTests {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var repository: EventRepository
  private lateinit var viewModel: EventViewModel

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    repository = Mockito.mock(EventRepository::class.java)
    viewModel = EventViewModel(repository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // 1. getNewUid
  @Test
  fun `getNewUid returns repository value`() {
    Mockito.`when`(repository.getNewUid()).thenReturn("123")
    val uid = viewModel.getNewUid()
    assertEquals("123", uid)
  }

  // 2. getAllEvents
  @Test
  fun `getAllEvents updates events StateFlow`() = runTest {
    val events = listOf(Event("1"), Event("2"))
    Mockito.`when`(repository.getAllEvents()).thenReturn(events)
    viewModel.getAllEvents()
    advanceUntilIdle()
    assertEquals(events, viewModel.events.value)
  }

  // 3. getEvent
  @Test
  fun `getEvent updates gotEvent StateFlow`() = runTest {
    val event = Event("1")
    Mockito.`when`(repository.getEvent("1")).thenReturn(event)
    viewModel.getEvent("1")
    advanceUntilIdle()
    assertEquals(event, viewModel.gotEvent.value)
  }

  // 4. getFilteredEvents
  @Test
  fun getFilteredEvents_emitsEventsFromRepository() =
      runTest(testDispatcher) {
        // Arrange
        val filters = Filters(tags = setOf("tag1"))
        val event =
            Event(
                uid = "E1",
                title = "Test Event",
                description = "Desc",
                date = Timestamp.now(),
                location = Location("Loc", 1.0, 2.0),
                tags = listOf("tag1"),
                public = true,
                ownerId = "owner1",
                imageUrl = null,
                capacity = 10,
                participantIds = emptyList(),
                price = 0.0)
        Mockito.`when`(repository.getFilteredEvents(filters)).thenReturn(listOf(event))

        // Act
        viewModel.getFilteredEvents(filters)
        advanceUntilIdle()

        // Assert
        val result = viewModel.events.value
        assertEquals(1, result.size)
        assertEquals("E1", result[0].uid)
      }

  // 5. getSearchedEvents
  @Test
  fun getSearchedEvents_emitsMatchingEvents() =
      runTest(testDispatcher) {
        val filters = Filters()
        // Arrange
        val searchTerm = "test"
        val event =
            Event(
                uid = "E1",
                title = "Test Event",
                description = "Desc",
                date = Timestamp.now(),
                location = Location("Loc", 1.0, 2.0),
                tags = emptyList(),
                public = true,
                ownerId = "owner1",
                imageUrl = null,
                capacity = 10,
                participantIds = emptyList(),
                price = 0.0)
        Mockito.`when`(repository.getSearchedEvents(searchTerm, filters)).thenReturn(listOf(event))

        // Act
        viewModel.getSearchedEvents(searchTerm, filters)
        advanceUntilIdle()

        // Assert
        val result = viewModel.events.value
        assertEquals(1, result.size)
        assertEquals("E1", result[0].uid)
      }

  // 6. addEvent
  @Test
  fun `addEvent calls repository`() = runTest {
    val event = Event("1")
    viewModel.addEvent(event)
    advanceUntilIdle()
    Mockito.verify(repository).addEvent(event)
  }

  // 7. editEvent
  @Test
  fun `editEvent calls repository`() = runTest {
    val event = Event("1")
    viewModel.editEvent("1", event)
    advanceUntilIdle()
    Mockito.verify(repository).editEvent("1", event)
  }

  // 8. deleteEvent
  @Test
  fun `deleteEvent calls repository`() = runTest {
    viewModel.deleteEvent("1")
    advanceUntilIdle()
    Mockito.verify(repository).deleteEvent("1")
  }

  // 9. getAllEvents handles exception and updates error StateFlow
  @Test
  fun `getAllEvents sets error when repository throws`() = runTest {
    Mockito.`when`(repository.getAllEvents()).thenThrow(RuntimeException("Network error"))
    viewModel.getAllEvents()
    advanceUntilIdle()
    assertEquals("Network error", viewModel.error.value)
  }

  // 10. addEvent handles exception and updates error StateFlow
  @Test
  fun `addEvent sets error when repository throws`() = runTest {
    val event = Event("1")
    Mockito.doThrow(RuntimeException("Write failed")).`when`(repository).addEvent(event)
    viewModel.addEvent(event)
    advanceUntilIdle()
    assertEquals("Write failed", viewModel.error.value)
  }

  // 11. editEvent handles exception and updates error StateFlow
  @Test
  fun `editEvent sets error when repository throws`() = runTest {
    val event = Event("1")
    Mockito.doThrow(RuntimeException("Edit failed")).`when`(repository).editEvent("1", event)
    viewModel.editEvent("1", event)
    advanceUntilIdle()
    assertEquals("Edit failed", viewModel.error.value)
  }

  // 12. deleteEvent handles exception and updates error StateFlow
  @Test
  fun `deleteEvent sets error when repository throws`() = runTest {
    Mockito.doThrow(RuntimeException("Delete failed")).`when`(repository).deleteEvent("1")
    viewModel.deleteEvent("1")
    advanceUntilIdle()
    assertEquals("Delete failed", viewModel.error.value)
  }

  // 13. clearError resets error StateFlow
  @Test
  fun `clearError resets error StateFlow`() = runTest {
    viewModel.getAllEvents() // no error yet
    advanceUntilIdle()
    viewModel.clearError()
    assertEquals(null, viewModel.error.value)
  }
}
