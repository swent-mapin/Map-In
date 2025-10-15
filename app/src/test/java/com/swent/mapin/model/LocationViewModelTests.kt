package com.swent.mapin.model

import com.github.se.bootcamp.ui.map.LocationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationViewModelTest {
  private val testDispatcher = StandardTestDispatcher()
  private lateinit var fakeRepository: FakeNominatimRepository
  private lateinit var viewModel: LocationViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    fakeRepository = FakeNominatimRepository()
    fakeRepository.results = listOf(Location("X", 0.0, 0.0))
    viewModel = LocationViewModel(fakeRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initial state is empty`() = runTest {
    val locations = viewModel.locations.first()
    assertEquals(emptyList<Location>(), locations)
  }

  @Test
  fun `query with blank string sets locations to empty`() = runTest {
    viewModel.onQueryChanged("")
    val scheduler = this.testScheduler
    scheduler.advanceTimeBy(1000)
    val locations = viewModel.locations.first()
    assertEquals(emptyList<Location>(), locations)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `query with valid string updates locations`() = runTest {
    val fakeRepo = object : LocationRepository {
      override suspend fun forwardGeocode(query: String): List<Location> {
        return listOf(Location("Test", 0.0, 0.0))
      }

      override suspend fun reverseGeocode(lat: Double, lon: Double): Location? {
        throw UnsupportedOperationException("Reverse not used in this test")
      }
    }

    val viewModel = LocationViewModel(fakeRepo)

    viewModel.onQueryChanged("Test")

    advanceTimeBy(1000)
    advanceUntilIdle()

    val locations = viewModel.locations.value
    assertEquals(1, locations.size)
    assertEquals("Test", locations[0].name)
  }

  @Test
  fun `query with repository exception sets locations to empty`() = runTest {
    fakeRepository.shouldThrow = true
    val scheduler = this.testScheduler
    viewModel.onQueryChanged("FailQuery")
    scheduler.advanceTimeBy(1000)

    val locations = viewModel.locations.first()
    assertEquals(emptyList<Location>(), locations)
  }

  @Test
  fun `onQueryChanged updates internal flow`() = runTest {
    viewModel.onQueryChanged("NewQuery")
    testScheduler.advanceTimeBy(1000)
    advanceUntilIdle()

    val locations = viewModel.locations.value
    assertEquals(1, locations.size)
    assertEquals("X", locations.first().name)
  }

  // Fake repository
  class FakeNominatimRepository : LocationRepository {
    var shouldThrow = false
    var results = listOf(Location("MockCity", 0.0, 0.0))

    override suspend fun forwardGeocode(query: String): List<Location> {
      if (shouldThrow) throw LocationSearchException("Fake error")
      return results
    }

    override suspend fun reverseGeocode(lat: Double, lon: Double): Location? {
      throw UnsupportedOperationException()
    }
  }
}
