package com.swent.mapin.ui.map

import com.google.firebase.Timestamp
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

/** Assisted by AI */
/** ViewModel tests for [SearchViewModel]. */

/** Simple fake repo that returns an in-memory list. */
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

  override suspend fun addEvent(event: Event) {}

  override suspend fun editEvent(eventID: String, newValue: Event) {}

  override suspend fun deleteEvent(eventID: String) {}
}

@RunWith(RobolectricTestRunner::class)
class SearchViewModelTest {

  @OptIn(ExperimentalCoroutinesApi::class) private lateinit var dispatcher: TestDispatcher

  private lateinit var vm: SearchViewModel

  // Sample data that covers title, description, locationName and tags
  private val sample =
      listOf(
          Event(
              uid = "1",
              title = "Rock Night",
              url = null,
              description = "Live music downtown",
              date = null,
              locationName = "Zurich",
              latitude = 47.3769,
              longitude = 8.5417,
              tags = listOf("music", "nightlife"),
              public = true,
              ownerId = "a",
              imageUrl = null,
              capacity = null,
              attendeeCount = 10),
          Event(
              uid = "2",
              title = "Morning Run",
              url = null,
              description = "Easy pace 5k",
              date = null,
              locationName = "Lake Park",
              latitude = 0.0,
              longitude = 0.0,
              tags = listOf("sports", "outdoors"),
              public = true,
              ownerId = "b",
              imageUrl = null,
              capacity = null,
              attendeeCount = 5),
          Event(
              uid = "3",
              title = "Jazz Jam",
              url = null,
              description = "impro session · bring your instrument",
              date = null,
              locationName = "Basel",
              latitude = 0.0,
              longitude = 0.0,
              tags = listOf("music", "jam"),
              public = true,
              ownerId = "c",
              imageUrl = null,
              capacity = null,
              attendeeCount = 3))

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setUp() {
    dispatcher = StandardTestDispatcher()
    Dispatchers.setMain(dispatcher)
    // Create VM AFTER setting Main dispatcher
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
    // Let the init { repo.getAllEvents() } finish
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
  fun `onQueryChange filters across title, description, location and tags (case-insensitive)`() =
      runTest {
        advanceUntilIdle()

        // Title match: "rock"
        vm.onQueryChange("rOcK")
        advanceTimeBy(300) // pass debounce
        var ui = vm.ui.value
        assertEquals(listOf("1"), ui.results.map { it.uid })

        // Description match: "easy"
        vm.onQueryChange("easy")
        advanceTimeBy(300)
        ui = vm.ui.value
        assertEquals(listOf("2"), ui.results.map { it.uid })

        // Location match: "basel"
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

    vm.onQueryChange("ro") // first partial
    advanceTimeBy(100) // not enough to fire

    vm.onQueryChange("rock") // replace before 250ms
    advanceTimeBy(300) // now debounce window passes

    val ui = vm.ui.value
    assertEquals(listOf("1"), ui.results.map { it.uid })
  }
}
