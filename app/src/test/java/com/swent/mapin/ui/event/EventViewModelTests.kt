package com.swent.mapin.ui.event

import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepository
import com.swent.mapin.ui.map.eventstate.MapEventStateController
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
  val stateController: MapEventStateController = Mockito.mock(MapEventStateController::class.java)

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    repository = Mockito.mock(EventRepository::class.java)
    viewModel = EventViewModel(repository, stateController)
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

  // 2. addEvent
  @Test
  fun `addEvent calls repository`() = runTest {
    val event = Event("1")
    viewModel.addEvent(event)
    advanceUntilIdle()
    Mockito.verify(repository).addEvent(event)
  }

  // 3. editEvent
  @Test
  fun `editEvent calls repository`() = runTest {
    val event = Event("1")
    viewModel.editEvent("1", event)
    advanceUntilIdle()
    Mockito.verify(repository).editEventAsOwner("1", event)
  }

  // 4. deleteEvent
  @Test
  fun `deleteEvent calls repository`() = runTest {
    viewModel.deleteEvent("1")
    advanceUntilIdle()
    Mockito.verify(repository).deleteEvent("1")
  }

  // 5. addEvent handles exception and updates error StateFlow
  @Test
  fun `addEvent sets error when repository throws`() = runTest {
    val event = Event("1")
    Mockito.doThrow(RuntimeException("Write failed")).`when`(repository).addEvent(event)
    viewModel.addEvent(event)
    advanceUntilIdle()
    assertEquals("Write failed", viewModel.error.value)
  }

  // 6. editEvent handles exception and updates error StateFlow
  @Test
  fun `editEvent sets error when repository throws`() = runTest {
    val event = Event("1")
    Mockito.doThrow(RuntimeException("Edit failed")).`when`(repository).editEventAsOwner("1", event)
    viewModel.editEvent("1", event)
    advanceUntilIdle()
    assertEquals("Edit failed", viewModel.error.value)
  }

  // 7. deleteEvent handles exception and updates error StateFlow
  @Test
  fun `deleteEvent sets error when repository throws`() = runTest {
    Mockito.doThrow(RuntimeException("Delete failed")).`when`(repository).deleteEvent("1")
    viewModel.deleteEvent("1")
    advanceUntilIdle()
    assertEquals("Delete failed", viewModel.error.value)
  }
}
