package com.swent.mapin.ui.map

import com.google.firebase.Timestamp
import com.swent.mapin.model.Location
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** --- Fake in-memory repository --- */
private class FakeEventRepository(private val data: List<Event>) : EventRepository {
  override fun getNewUid(): String = "fake"

  override suspend fun getAllEvents(): List<Event> = data

  override suspend fun getEvent(eventID: String): Event = data.first { it.uid == eventID }

  override suspend fun getEventsByTags(tags: List<String>): List<Event> =
      data.filter { it.tags.any(tags::contains) }

  override suspend fun getEventsOnDay(dayStart: Timestamp, dayEnd: Timestamp): List<Event> = data

  override suspend fun getEventsByOwner(ownerId: String): List<Event> =
      data.filter { it.ownerId == ownerId }

  override suspend fun getEventsByTitle(title: String): List<Event> =
      data.filter { it.title.equals(title, ignoreCase = true) }

  override suspend fun getEventsByParticipant(userId: String): List<Event> =
      data.filter { userId in it.participantIds }

  override suspend fun addEvent(event: Event) {}

  override suspend fun editEvent(eventID: String, newValue: Event) {}

  override suspend fun deleteEvent(eventID: String) {}
}

@RunWith(RobolectricTestRunner::class)
class SearchViewModelTest {

  @OptIn(ExperimentalCoroutinesApi::class) private lateinit var dispatcher: TestDispatcher

  private lateinit var vm: SearchViewModel

  // Échantillon adapté: Event.location = Location(name, lat, lon)
  private val sample =
      listOf(
          Event(
              uid = "1",
              title = "Rock Night",
              description = "Live music downtown",
              location = Location(name = "Zurich", latitude = 47.3769, longitude = 8.5417),
              tags = listOf("music", "nightlife"),
              ownerId = "a",
              attendeeCount = 10),
          Event(
              uid = "2",
              title = "Morning Run",
              description = "Easy pace 5k",
              location = Location(name = "Lake Park", latitude = 0.0, longitude = 0.0),
              tags = listOf("sports", "outdoors"),
              ownerId = "b",
              attendeeCount = 5),
          Event(
              uid = "3",
              title = "Jazz Jam",
              description = "impro session · bring your instrument",
              location = Location(name = "Basel", latitude = 0.0, longitude = 0.0),
              tags = listOf("music", "jam"),
              ownerId = "c",
              attendeeCount = 3))

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setUp() {
    dispatcher = StandardTestDispatcher()
    Dispatchers.setMain(
        dispatcher) // nécessaire car SearchViewModel lance dans viewModelScope(Main)
    vm = SearchViewModel(FakeEventRepository(sample))
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `init loads events into results`() = runTest {
    advanceUntilIdle()
    val ui = vm.ui.value
    assertFalse(ui.isLoading)
    assertEquals(3, ui.results.size)
    assertFalse(ui.showNoResults)
    assertEquals("", ui.query)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `onSearchTapped sets flags and onFocusHandled clears one-shot`() = runTest {
    advanceUntilIdle()

    vm.onSearchTapped()
    assertTrue(vm.ui.value.searchMode)
    assertTrue(vm.ui.value.shouldRequestFocus)

    vm.onFocusHandled()
    assertFalse(vm.ui.value.shouldRequestFocus)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `onQueryChange filters across title, description, location_name and tags (case-insensitive)`() =
      runTest {
        advanceUntilIdle()

        // Title match: "rock"
        vm.onQueryChange("rOcK")
        advanceTimeBy(300) // debounce > 250ms
        var ui = vm.ui.value
        assertEquals(listOf("1"), ui.results.map { it.uid })

        // Description match: "easy"
        vm.onQueryChange("easy")
        advanceTimeBy(300)
        ui = vm.ui.value
        assertEquals(listOf("2"), ui.results.map { it.uid })

        // Location.name match: "basel"
        vm.onQueryChange("basel")
        advanceTimeBy(300)
        ui = vm.ui.value
        assertEquals(listOf("3"), ui.results.map { it.uid })

        // Tag match: "music"
        vm.onQueryChange("music")
        advanceTimeBy(300)
        ui = vm.ui.value
        assertEquals(listOf("1", "3"), ui.results.map { it.uid })
      }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `showNoResults true when nothing matches`() = runTest {
    advanceUntilIdle()

    vm.onQueryChange("zzz-not-found")
    advanceTimeBy(300)
    val ui = vm.ui.value
    assertTrue(ui.showNoResults)
    assertTrue(ui.results.isEmpty())
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `clearing query resets list and exits search mode`() = runTest {
    advanceUntilIdle()

    vm.onSearchTapped()
    vm.onQueryChange("rock")
    advanceTimeBy(300)
    assertEquals(1, vm.ui.value.results.size)

    vm.onClearSearch()
    val ui = vm.ui.value
    assertEquals("", ui.query)
    assertEquals(3, ui.results.size)
    assertFalse(ui.searchMode)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `typing then replacing query applies latest filter only (debounce)`() = runTest {
    advanceUntilIdle()

    vm.onQueryChange("ro") // premier partiel
    advanceTimeBy(100) // pas assez pour déclencher

    vm.onQueryChange("rock") // remplace avant 250ms
    advanceTimeBy(300) // laisse passer le debounce

    val ui = vm.ui.value
    assertEquals(listOf("1"), ui.results.map { it.uid })
  }
}
