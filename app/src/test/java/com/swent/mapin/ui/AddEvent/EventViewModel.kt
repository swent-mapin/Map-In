import com.google.firebase.Timestamp
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepository
import com.swent.mapin.ui.components.EventViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExperimentalCoroutinesApi
class EventViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: EventRepository
    private lateinit var viewModel: EventViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mock(EventRepository::class.java)
        viewModel = EventViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // 1. getNewUid
    @Test
    fun `getNewUid returns repository value`() {
        `when`(repository.getNewUid()).thenReturn("123")
        val uid = viewModel.getNewUid()
        assertEquals("123", uid)
    }

    // 2. getAllEvents
    @Test
    fun `getAllEvents updates events StateFlow`() = runTest {
        val events = listOf(Event("1"), Event("2"))
        `when`(repository.getAllEvents()).thenReturn(events)

        viewModel.getAllEvents()
        advanceUntilIdle()

        assertEquals(events, viewModel.events.value)
    }

    // 3. getEvent
    @Test
    fun `getEvent updates gotEvent StateFlow`() = runTest {
        val event = Event("1")
        `when`(repository.getEvent("1")).thenReturn(event)

        viewModel.getEvent("1")
        advanceUntilIdle()

        assertEquals(event, viewModel.gotEvent.value)
    }

    // 4. getEventByTags
    @Test
    fun `getEventByTags updates events StateFlow`() = runTest {
        val events = listOf(Event("1"))
        val tags = listOf("tag1")
        `when`(repository.getEventsByTags(tags)).thenReturn(events)

        viewModel.getEventByTags(tags)
        advanceUntilIdle()

        assertEquals(events, viewModel.events.value)
    }

    // 5. getEventsOnDay
    @Test
    fun `getEventsOnDay updates events StateFlow`() = runTest {
        val events = listOf(Event("1"))
        val start = Timestamp.now()
        val end = Timestamp.now()
        `when`(repository.getEventsOnDay(start, end)).thenReturn(events)

        viewModel.getEventsOnDay(start, end)
        advanceUntilIdle()

        assertEquals(events, viewModel.events.value)
    }

    // 6. getEventsByOwner
    @Test
    fun `getEventsByOwner updates events StateFlow`() = runTest {
        val events = listOf(Event("1"))
        val ownerId = "owner123"
        `when`(repository.getEventsByOwner(ownerId)).thenReturn(events)

        viewModel.getEventsByOwner(ownerId)
        advanceUntilIdle()

        assertEquals(events, viewModel.events.value)
    }

    // 7. addEvent
    @Test
    fun `addEvent calls repository`() = runTest {
        val event = Event("1")

        viewModel.addEvent(event)
        advanceUntilIdle()

        verify(repository).addEvent(event)
    }

    // 8. editEvent
    @Test
    fun `editEvent calls repository`() = runTest {
        val event = Event("1")

        viewModel.editEvent("1", event)
        advanceUntilIdle()

        verify(repository).editEvent("1", event)
    }

    // 9. deleteEvent
    @Test
    fun `deleteEvent calls repository`() = runTest {
        viewModel.deleteEvent("1")
        advanceUntilIdle()

        verify(repository).deleteEvent("1")
    }
}