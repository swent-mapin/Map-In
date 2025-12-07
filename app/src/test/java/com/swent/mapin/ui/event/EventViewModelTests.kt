package com.swent.mapin.ui.event

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepository
import com.swent.mapin.model.location.Location
import com.swent.mapin.ui.map.eventstate.MapEventStateController
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
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
import org.mockito.kotlin.any
import org.mockito.kotlin.eq

@ExperimentalCoroutinesApi
class EventViewModelTests {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var repository: EventRepository
  private lateinit var viewModel: EventViewModel
  private val stateController: MapEventStateController =
      Mockito.mock(MapEventStateController::class.java)

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    repository = Mockito.mock(EventRepository::class.java)

    val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    val mockUser = mockk<FirebaseUser>(relaxed = true)
    every { mockUser.uid } returns "user123"
    every { mockAuth.currentUser } returns mockUser

    viewModel = EventViewModel(repository, stateController, mockAuth)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun `getNewUid returns repository value`() {
    Mockito.`when`(repository.getNewUid()).thenReturn("123")
    val uid = viewModel.getNewUid()
    assertEquals("123", uid)
  }

  @Test
  fun `addEvent calls repository`() = runTest {
    val event = Event("1")
    viewModel.addEvent(event)
    advanceUntilIdle()
    Mockito.verify(repository).addEvent(event)
  }

  @Test
  fun `editEvent calls repository`() = runTest {
    val event = Event("1")
    viewModel.editEvent("1", event)
    advanceUntilIdle()
    Mockito.verify(repository).editEventAsOwner("1", event)
  }

  @Test
  fun `deleteEvent calls repository`() = runTest {
    viewModel.deleteEvent("1")
    advanceUntilIdle()
    Mockito.verify(repository).deleteEvent("1")
  }

  @Test
  fun `addEvent sets error when repository throws`() = runTest {
    val event = Event("1")
    Mockito.doThrow(RuntimeException("Write failed")).`when`(repository).addEvent(event)
    viewModel.addEvent(event)
    advanceUntilIdle()
    assertEquals("Write failed", viewModel.error.value)
  }

  @Test
  fun `editEvent sets error when repository throws`() = runTest {
    val event = Event("1")
    Mockito.doThrow(RuntimeException("Edit failed")).`when`(repository).editEventAsOwner("1", event)
    viewModel.editEvent("1", event)
    advanceUntilIdle()
    assertEquals("Edit failed", viewModel.error.value)
  }

  @Test
  fun `deleteEvent sets error when repository throws`() = runTest {
    Mockito.doThrow(RuntimeException("Delete failed")).`when`(repository).deleteEvent("1")
    viewModel.deleteEvent("1")
    advanceUntilIdle()
    assertEquals("Delete failed", viewModel.error.value)
  }

  @Test
  fun `selectEventToEdit updates eventToEdit`() {
    val event = Event("1")
    viewModel.selectEventToEdit(event)
    assertEquals(event, viewModel.eventToEdit.value)
  }

  @Test
  fun `clearEventToEdit clears eventToEdit`() {
    val event = Event("1")
    viewModel.selectEventToEdit(event)
    viewModel.clearEventToEdit()
    assertEquals(null, viewModel.eventToEdit.value)
  }

  @Test
  fun `clearError clears error`() = runTest {
    Mockito.doThrow(RuntimeException("Error")).`when`(repository).addEvent(Event("1"))
    viewModel.addEvent(Event("1"))
    advanceUntilIdle()
    assertEquals("Error", viewModel.error.value)
    viewModel.clearError()
    assertEquals(null, viewModel.error.value)
  }

  @Test
  fun `saveEditedEvent calls editEvent and onSuccess when user is owner`() = runTest {
    val event = Event(uid = "1", ownerId = "user123")
    val editedTitle = "Updated Title"
    val editedDesc = "Updated Description"
    val location = Location.from("Test", 0.0, 0.0)
    val startTs = Timestamp.now()
    val endTs = Timestamp.now()
    val tagsString = "tag1 tag2"

    var successCalled = false
    viewModel.saveEditedEvent(
        originalEvent = event,
        title = editedTitle,
        description = editedDesc,
        location = location,
        startTs = startTs,
        endTs = endTs,
        tagsString = tagsString,
        onSuccess = { successCalled = true })
    advanceUntilIdle()

    Mockito.verify(repository).editEventAsOwner(eq(event.uid), any())
    assert(successCalled)
  }

  @Test
  fun `saveEditedEvent does not call editEvent when user is not owner`() = runTest {
    val event = Event(uid = "1", ownerId = "owner123")
    val editedTitle = "Updated Title"
    val editedDesc = "Updated Description"
    val location = Location.from("Test", 0.0, 0.0)
    val startTs = Timestamp.now()
    val endTs = Timestamp.now()
    val tagsString = "tag1 tag2"

    var successCalled = false
    viewModel.saveEditedEvent(
        originalEvent = event,
        title = editedTitle,
        description = editedDesc,
        location = location,
        startTs = startTs,
        endTs = endTs,
        tagsString = tagsString,
        onSuccess = { successCalled = true })
    advanceUntilIdle()

    Mockito.verify(repository, Mockito.never()).editEventAsOwner(eq(event.uid), any())
    assert(!successCalled)
  }
}
