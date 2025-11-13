package com.swent.mapin.ui.map

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.swent.mapin.model.LocationViewModel
import com.swent.mapin.model.event.Event
import com.swent.mapin.ui.event.AddEventScreen
import com.swent.mapin.ui.event.AddEventScreenTestTags
import com.swent.mapin.ui.event.EventViewModel
import com.swent.mapin.ui.filters.FiltersSection
import com.swent.mapin.ui.filters.FiltersSectionViewModel
import com.swent.mapin.ui.map.bottomsheet.SearchBarState
import com.swent.mapin.ui.map.bottomsheet.components.AllRecentItemsPage
import com.swent.mapin.ui.map.bottomsheet.components.AttendedEventsSection
import com.swent.mapin.ui.map.bottomsheet.components.EventsSection
import com.swent.mapin.ui.map.bottomsheet.components.QuickActionsSection
import com.swent.mapin.ui.map.bottomsheet.components.SearchBar
import com.swent.mapin.ui.map.bottomsheet.components.SearchResultsSection
import com.swent.mapin.ui.map.search.RecentItem
import com.swent.mapin.ui.memory.MemoryFormData
import com.swent.mapin.ui.memory.MemoryFormScreen
import com.swent.mapin.ui.profile.ProfileViewModel

enum class BottomSheetScreen {
  MAIN_CONTENT,
  MEMORY_FORM,
  ADD_EVENT
}

// Animation constants for consistent transitions
private const val TRANSITION_FADE_IN_DURATION_MS = 250
private const val TRANSITION_FADE_OUT_DURATION_MS = 200
private const val TRANSITION_SLIDE_OFFSET_DIVISOR = 6

/**
 * Unified BottomSheetContent combining features from both originals:
 * - Search results mode vs main content
 * - Quick actions (Create Memory / Event)
 * - Tabs for Saved vs Joined events
 * - Filters section (time/place/price/tags...) using Filter view models
 * - Profile shortcut with optional avatarUrl or preset icon
 *
 * @param state Current state of the bottom sheet (COLLAPSED, MEDIUM, FULL).
 * @param fullEntryKey Key to reset scroll state when entering FULL state.
 * @param searchBarState State and callbacks for search bar interactions.
 * @param searchResults List of events filtered by search query.
 * @param isSearchMode Whether the bottom sheet is in search results mode.
 * @param currentScreen Current screen displayed in the bottom sheet (MAIN_CONTENT, MEMORY_FORM,
 *   ADD_EVENT).
 * @param availableEvents List of events available for memory creation.
 * @param joinedEvents List of events the user has joined.
 * @param savedEvents List of events the user has saved.
 * @param selectedTab Currently selected tab (SAVED_EVENTS or JOINED_EVENTS).
 * @param onEventClick Callback invoked when an event is clicked in search results.
 * @param onCreateMemoryClick Callback to show the memory creation form.
 * @param onCreateEventClick Callback to show the event creation form.
 * @param onMemorySave Callback to save a new memory.
 * @param onMemoryCancel Callback to cancel memory creation.
 * @param onCreateEventDone Callback to handle completion or cancellation of event creation.
 * @param onTabChange Callback to switch between Saved and Joined tabs.
 * @param onTabEventClick Callback when an event is clicked in the Saved or Joined tabs.
 * @param avatarUrl URL or ID of the user's avatar for the profile button.
 * @param onProfileClick Callback to navigate to the profile screen.
 * @param filterViewModel ViewModel for managing filter state, such as maximum price, tags, and
 *   location, used to filter events displayed on the map and in search results.
 * @param locationViewModel ViewModel for managing location-related data.
 * @param profileViewModel ViewModel for accessing user profile data, such as avatar URL.
 */
@SuppressLint("VisibleForTests")
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetContent(
    state: BottomSheetState,
    fullEntryKey: Int,
    searchBarState: SearchBarState,
    onModalShown: (Boolean) -> Unit = {},
    // Search results and mode
    searchResults: List<Event> = emptyList(),
    isSearchMode: Boolean = false,
    recentItems: List<RecentItem> = emptyList(),
    onRecentSearchClick: (String) -> Unit = {},
    onRecentEventClick: (String) -> Unit = {},
    onClearRecentSearches: () -> Unit = {},
    topCategories: List<String> = emptyList(),
    onCategoryClick: (String) -> Unit = {},
    // Memory form and events
    currentScreen: BottomSheetScreen = BottomSheetScreen.MAIN_CONTENT,
    availableEvents: List<Event> = emptyList(),
    // Optional initial event to prefill memory form when opening it
    initialMemoryEvent: Event? = null,
    // Joined/Saved events
    joinedEvents: List<Event> = emptyList(),
    savedEvents: List<Event> = emptyList(),
    // Tabs & tags
    selectedTab: MapScreenViewModel.BottomSheetTab = MapScreenViewModel.BottomSheetTab.SAVED_EVENTS,
    // Callbacks
    onEventClick: (Event) -> Unit = {},
    // now accepts an optional Event to prefill the memory form (null = new memory without event)
    onCreateMemoryClick: (Event?) -> Unit = {},
    onCreateEventClick: () -> Unit = {},
    onNavigateToFriends: () -> Unit = {},
    onMemorySave: (MemoryFormData) -> Unit = {},
    onMemoryCancel: () -> Unit = {},
    onCreateEventDone: () -> Unit = {},
    onTabChange: (MapScreenViewModel.BottomSheetTab) -> Unit = {},
    onTabEventClick: (Event) -> Unit = {},
    // Profile/Filters support
    avatarUrl: String? = null,
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    filterViewModel: FiltersSectionViewModel,
    locationViewModel: LocationViewModel,
    profileViewModel: ProfileViewModel,
    eventViewModel: EventViewModel
) {
  val isFull = state == BottomSheetState.FULL
  val scrollState = remember(fullEntryKey) { ScrollState(0) }
  val focusRequester = remember { FocusRequester() }
  val focusManager = LocalFocusManager.current
  val filterSection = remember { FiltersSection() }
  val userProfile by profileViewModel.userProfile.collectAsStateWithLifecycle()

  LaunchedEffect(isFull, searchBarState.shouldRequestFocus) {
    if (isFull && searchBarState.shouldRequestFocus) {
      focusRequester.requestFocus()
      searchBarState.onFocusHandled()
    }
  }

  AnimatedContent(
      targetState = currentScreen,
      transitionSpec = {
        (fadeIn(animationSpec = tween(300)) +
                slideInVertically(animationSpec = tween(300), initialOffsetY = { it / 4 }))
            .togetherWith(
                fadeOut(animationSpec = tween(200)) +
                    slideOutVertically(animationSpec = tween(200), targetOffsetY = { -it / 4 }))
      },
      label = "memoryFormTransition") { screen ->
        when (screen) {
          BottomSheetScreen.MEMORY_FORM -> {
            val memoryFormScrollState = remember { ScrollState(0) }
            MemoryFormScreen(
                scrollState = memoryFormScrollState,
                availableEvents = availableEvents,
                onSave = onMemorySave,
                onCancel = onMemoryCancel,
                initialSelectedEvent = initialMemoryEvent)
          }
          BottomSheetScreen.ADD_EVENT -> {
            AddEventScreen(
                modifier = Modifier.testTag(AddEventScreenTestTags.SCREEN),
                eventViewModel = eventViewModel,
                onCancel = onCreateEventDone,
                onDone = onCreateEventDone)
          }
          BottomSheetScreen.MAIN_CONTENT -> {
            var showAllRecents by remember { mutableStateOf(false) }
            var showProfileMenu by remember { mutableStateOf(false) }

            // Notify host when modal profile menu opens/closes so the anchored bottom sheet can
            // hide
            LaunchedEffect(showProfileMenu) { onModalShown(showProfileMenu) }

            AnimatedContent(
                targetState = showAllRecents,
                transitionSpec = {
                  (fadeIn(animationSpec = tween(TRANSITION_FADE_IN_DURATION_MS)) +
                          slideInVertically(
                              animationSpec = tween(TRANSITION_FADE_IN_DURATION_MS),
                              initialOffsetY = { it / TRANSITION_SLIDE_OFFSET_DIVISOR }))
                      .togetherWith(
                          fadeOut(animationSpec = tween(TRANSITION_FADE_OUT_DURATION_MS)) +
                              slideOutVertically(
                                  animationSpec = tween(TRANSITION_FADE_OUT_DURATION_MS),
                                  targetOffsetY = { -it / TRANSITION_SLIDE_OFFSET_DIVISOR }))
                },
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                label = "allRecentsPageTransition") { showAll ->
                  if (showAll) {
                    AllRecentItemsPage(
                        recentItems = recentItems,
                        onRecentSearchClick = { query ->
                          showAllRecents = false
                          onRecentSearchClick(query)
                        },
                        onRecentEventClick = { eventId ->
                          showAllRecents = false
                          onRecentEventClick(eventId)
                        },
                        onClearAll = {
                          onClearRecentSearches()
                          showAllRecents = false
                        },
                        onBack = { showAllRecents = false })
                  } else {
                    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                      SearchBar(
                          value = searchBarState.query,
                          onValueChange = searchBarState.onQueryChange,
                          isSearchMode = isSearchMode,
                          onTap = searchBarState.onTap,
                          focusRequester = focusRequester,
                          onSearchAction = {
                            searchBarState.onSubmit()
                            focusManager.clearFocus()
                          },
                          onClear = searchBarState.onClear,
                          avatarUrl = avatarUrl ?: userProfile.avatarUrl,
                          onProfileClick = { showProfileMenu = true })

                      Spacer(modifier = Modifier.height(24.dp))

                      AnimatedContent(
                          targetState = isSearchMode,
                          transitionSpec = {
                            (fadeIn(animationSpec = tween(TRANSITION_FADE_IN_DURATION_MS)) +
                                    slideInVertically(
                                        animationSpec = tween(TRANSITION_FADE_IN_DURATION_MS),
                                        initialOffsetY = { it / TRANSITION_SLIDE_OFFSET_DIVISOR }))
                                .togetherWith(
                                    fadeOut(
                                        animationSpec = tween(TRANSITION_FADE_OUT_DURATION_MS)) +
                                        slideOutVertically(
                                            animationSpec = tween(TRANSITION_FADE_OUT_DURATION_MS),
                                            targetOffsetY = {
                                              it / TRANSITION_SLIDE_OFFSET_DIVISOR
                                            }))
                          },
                          modifier = Modifier.fillMaxWidth().weight(1f, fill = true),
                          label = "searchModeTransition") { searchActive ->
                            if (searchActive) {
                              SearchResultsSection(
                                  modifier = Modifier.fillMaxSize(),
                                  results = searchResults,
                                  query = searchBarState.query,
                                  recentItems = recentItems,
                                  onRecentSearchClick = onRecentSearchClick,
                                  onRecentEventClick = onRecentEventClick,
                                  onShowAllRecents = { showAllRecents = true },
                                  topCategories = topCategories,
                                  onCategoryClick = onCategoryClick,
                                  onEventClick = onEventClick)
                            } else {
                              val contentModifier =
                                  if (isFull) Modifier.fillMaxWidth().verticalScroll(scrollState)
                                  else Modifier.fillMaxWidth()

                              Column(modifier = contentModifier) {
                                QuickActionsSection(
                                    onCreateMemoryClick = onCreateMemoryClick,
                                    onCreateEventClick = onCreateEventClick)

                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f))
                                Spacer(modifier = Modifier.height(16.dp))

                                // Tabs
                                TabRow(
                                    selectedTabIndex =
                                        if (selectedTab ==
                                            MapScreenViewModel.BottomSheetTab.SAVED_EVENTS)
                                            0
                                        else 1,
                                    modifier = Modifier.fillMaxWidth()) {
                                      Tab(
                                          selected =
                                              selectedTab ==
                                                  MapScreenViewModel.BottomSheetTab.SAVED_EVENTS,
                                          onClick = {
                                            onTabChange(
                                                MapScreenViewModel.BottomSheetTab.SAVED_EVENTS)
                                          },
                                          text = { Text("Saved Events") })
                                      Tab(
                                          selected =
                                              selectedTab ==
                                                  MapScreenViewModel.BottomSheetTab.JOINED_EVENTS,
                                          onClick = {
                                            onTabChange(
                                                MapScreenViewModel.BottomSheetTab.JOINED_EVENTS)
                                          },
                                          text = { Text("Joined Events") })
                                    }

                                Spacer(modifier = Modifier.height(16.dp))

                                when (selectedTab) {
                                  MapScreenViewModel.BottomSheetTab.SAVED_EVENTS -> {
                                    EventsSection(
                                        events = savedEvents, onEventClick = onTabEventClick)
                                  }
                                  MapScreenViewModel.BottomSheetTab.JOINED_EVENTS -> {
                                    EventsSection(
                                        events = joinedEvents, onEventClick = onTabEventClick)
                                  }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f))
                                Spacer(modifier = Modifier.height(16.dp))

                                // Attended events section
                                AttendedEventsSection(
                                    availableEvents, userProfile, onEventClick, onCreateMemoryClick)

                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f))
                                Spacer(modifier = Modifier.height(16.dp))

                                // Filters section visible unless collapsed
                                if (state != BottomSheetState.COLLAPSED) {
                                  filterSection.Render(
                                      Modifier.fillMaxWidth(),
                                      filterViewModel,
                                      locationViewModel,
                                      userProfile)

                                  Spacer(modifier = Modifier.height(16.dp))
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                              }
                            }
                          }
                    }
                  }
                }

            // Modal bottom sheet for profile menu (larger & scrollable)
            if (showProfileMenu) {
              ModalBottomSheet(onDismissRequest = { showProfileMenu = false }) {
                val sheetScroll = rememberScrollState()
                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                            .fillMaxHeight(0.75f)
                            .verticalScroll(sheetScroll)
                            .padding(16.dp)) {
                      // En-tête : photo de profil + message de bienvenue
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = userProfile.avatarUrl ?: avatarUrl,
                            contentDescription = "Profile picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(56.dp).clip(CircleShape))

                        Spacer(modifier = Modifier.width(12.dp))

                        Text("Hello ${userProfile.name}!")
                      }

                      Spacer(modifier = Modifier.height(8.dp))

                      Button(
                          onClick = {
                            showProfileMenu = false
                            onProfileClick()
                          },
                          modifier = Modifier.fillMaxWidth()) {
                            Text("Profile")
                          }

                      Spacer(modifier = Modifier.height(8.dp))

                      Button(
                          onClick = {
                            showProfileMenu = false
                            onNavigateToFriends()
                          },
                          modifier = Modifier.fillMaxWidth()) {
                            Text("Friends")
                          }

                      Spacer(modifier = Modifier.height(8.dp))

                      Button(
                          onClick = {
                            showProfileMenu = false
                            onSettingsClick()
                          },
                          modifier = Modifier.fillMaxWidth()) {
                            Text("Settings")
                          }
                    }
              }
            }
          }
        }
      }
}
