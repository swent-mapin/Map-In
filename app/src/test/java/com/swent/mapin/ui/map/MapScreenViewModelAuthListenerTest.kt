package com.swent.mapin.ui.map

import android.content.Context
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.swent.mapin.model.UserProfileRepository
import com.swent.mapin.model.memory.MemoryRepository
import com.swent.mapin.ui.components.BottomSheetConfig
import com.swent.mapin.ui.event.EventViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.whenever

/**
 * Unit tests for [MapScreenViewModel] focusing on FirebaseAuth.AuthStateListener behavior. Verifies
 * that saved/joined events are cleared on sign-out and reloaded on sign-in.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapScreenViewModelAuthListenerTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  @Mock lateinit var mockAuth: FirebaseAuth
  @Mock lateinit var mockUser: FirebaseUser
  @Mock lateinit var mockEventViewModel: EventViewModel
  @Mock lateinit var mockMemoryRepo: MemoryRepository
  @Mock lateinit var mockUserProfileRepo: UserProfileRepository
  @Mock lateinit var mockFilterViewModel: FiltersSectionViewModel
  @Mock lateinit var mockContext: Context

  private lateinit var vm: MapScreenViewModel
  private lateinit var authListener: FirebaseAuth.AuthStateListener

  @Before
  fun setup() {
    MockitoAnnotations.openMocks(this)
    Dispatchers.setMain(mainDispatcherRule.dispatcher)

    whenever(mockAuth.currentUser).thenReturn(mockUser)
    whenever(mockUser.uid).thenReturn("testUserId")
    whenever(mockContext.applicationContext).thenReturn(mockContext)

    whenever(mockEventViewModel.events).thenReturn(MutableStateFlow(emptyList()))
    whenever(mockEventViewModel.savedEvents).thenReturn(MutableStateFlow(emptyList()))
    whenever(mockEventViewModel.joinedEvents).thenReturn(MutableStateFlow(emptyList()))
    whenever(mockEventViewModel.savedEventIds).thenReturn(MutableStateFlow(emptySet()))
    whenever(mockEventViewModel.availableEvents).thenReturn(MutableStateFlow(emptyList()))
    whenever(mockEventViewModel.searchResults).thenReturn(MutableStateFlow(emptyList()))
    whenever(mockEventViewModel.error).thenReturn(MutableStateFlow(null))

    whenever(mockFilterViewModel.filters).thenReturn(MutableStateFlow(Filters()))

    vm =
        MapScreenViewModel(
            initialSheetState = BottomSheetState.COLLAPSED,
            sheetConfig =
                BottomSheetConfig(
                    collapsedHeight = 100.dp, mediumHeight = 300.dp, fullHeight = 600.dp),
            onClearFocus = {},
            applicationContext = mockContext,
            eventViewModel = mockEventViewModel,
            memoryRepository = mockMemoryRepo,
            filterViewModel = mockFilterViewModel,
            auth = mockAuth,
            userProfileRepository = mockUserProfileRepo)

    val captor = argumentCaptor<FirebaseAuth.AuthStateListener>()
    verify(mockAuth).addAuthStateListener(captor.capture())
    authListener = captor.firstValue
  }

  @Test
  fun `on sign-out clears saved and joined events`() = runTest {
    // Given a signed-out state
    whenever(mockAuth.currentUser).thenReturn(null)

    // When the auth listener triggers
    authListener.onAuthStateChanged(mockAuth)

    // Then saved/joined events and avatar are cleared
    assertTrue(vm.savedEvents.isEmpty())
    assertTrue(vm.joinedEvents.isEmpty())
    assertNull(vm.avatarUrl)
  }

  @Test
  fun `on sign-in loads saved and joined events`() = runTest {
    // Given a signed-in user
    whenever(mockAuth.currentUser).thenReturn(mockUser)
    whenever(mockUser.uid).thenReturn("testUserId")

    // When the auth listener triggers
    authListener.onAuthStateChanged(mockAuth)

    // Then EventViewModel fetch methods should be called
    verify(mockEventViewModel).getSavedEventIds("testUserId")
    verify(mockEventViewModel).getSavedEvents("testUserId")
  }

  @Test
  fun `onCleared removes auth state listener`() {
    // When VM is cleared
    vm.onCleared()

    // Then the listener should be removed from FirebaseAuth
    verify(mockAuth).removeAuthStateListener(any())
  }
}
