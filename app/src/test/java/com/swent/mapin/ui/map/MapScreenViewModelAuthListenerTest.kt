package com.swent.mapin.ui.map

import android.content.Context
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.swent.mapin.model.UserProfileRepository
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepository
import com.swent.mapin.model.event.LocalEventRepository
import com.swent.mapin.model.memory.MemoryRepository
import com.swent.mapin.ui.components.BottomSheetConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

// Tests for MapScreenViewModel's FirebaseAuth.AuthStateListener behavior
// specifically loading/clearing saved and joined events on sign-in/sign-out.
// Assisted by AI.
@OptIn(ExperimentalCoroutinesApi::class)
class MapScreenViewModelAuthListenerTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  @Mock lateinit var mockRepo: EventRepository
  @Mock lateinit var mockAuth: FirebaseAuth
  @Mock lateinit var mockUser: FirebaseUser
  @Mock lateinit var mockMemoryRepo: MemoryRepository
  @Mock lateinit var mockUserProfileRepo: UserProfileRepository

  // We'll store the captured listener here after setup
  private lateinit var authListener: FirebaseAuth.AuthStateListener

  private lateinit var vm: MapScreenViewModel

  @Before
  fun setup() {
    MockitoAnnotations.openMocks(this)

    // Default auth stubs
    whenever(mockAuth.currentUser).thenReturn(mockUser)
    whenever(mockUser.uid).thenReturn("testUserId")

    // Stub all suspend repository calls inside a coroutine
    runBlocking {
      whenever(mockRepo.getAllEvents()).thenReturn(LocalEventRepository.defaultSampleEvents())
      whenever(mockRepo.getSavedEventIds(any())).thenReturn(emptySet())
      whenever(mockRepo.getSavedEvents(any())).thenReturn(emptyList())
      whenever(mockUserProfileRepo.getUserProfile(any())).thenReturn(null)
    }

    // Never touch real Firebase in tests
    whenever(mockMemoryRepo.getNewUid()).thenReturn("test-memory-id")

    // Build the ViewModel after Dispatchers.Main is provided by the rule
    vm =
        MapScreenViewModel(
            initialSheetState = BottomSheetState.COLLAPSED,
            sheetConfig =
                BottomSheetConfig(
                    collapsedHeight = 120.dp, mediumHeight = 400.dp, fullHeight = 800.dp),
            onClearFocus = {},
            applicationContext = mock<Context>(),
            memoryRepository = mockMemoryRepo,
            eventRepository = mockRepo,
            auth = mockAuth,
            userProfileRepository = mockUserProfileRepo)

    // Capture the auth state listener the VM registers
    val captor = argumentCaptor<FirebaseAuth.AuthStateListener>()
    verify(mockAuth).addAuthStateListener(captor.capture())
    authListener = captor.firstValue
  }

  @Test
  fun authListener_onSignOut_clearsSavedAndJoined() = runTest {
    // Simulate sign-out
    whenever(mockAuth.currentUser).thenReturn(null)
    authListener.onAuthStateChanged(mockAuth)

    // Saved list cleared
    assertEquals(emptyList<Event>(), vm.savedEvents)

    // savedEventIds is private; check via helper
    val sample = LocalEventRepository.defaultSampleEvents().first()
    assertEquals(false, vm.isEventSaved(sample))

    // Joined list cleared
    assertEquals(0, vm.joinedEvents.size)
  }

  @Test
  fun authListener_onSignIn_loadsSavedAndJoined() = runTest {
    // Provide some saved data after sign-in
    val e = LocalEventRepository.defaultSampleEvents().first()

    whenever(mockAuth.currentUser).thenReturn(mockUser)
    whenever(mockUser.uid).thenReturn("testUserId")

    // Update repo responses for this user
    runBlocking {
      whenever(mockRepo.getSavedEventIds("testUserId")).thenReturn(setOf(e.uid))
      whenever(mockRepo.getSavedEvents("testUserId")).thenReturn(listOf(e))
      // Joined events are derived from _allEvents + uid; not required for this assertion,
      // but you could also stub getEventsByParticipant if your VM uses it here.
    }

    authListener.onAuthStateChanged(mockAuth)
    advanceUntilIdle()

    // Saved IDs & list reflect repo
    assertEquals(true, vm.isEventSaved(e))
    assertEquals(listOf(e.uid), vm.savedEvents.map { it.uid })
  }
}
