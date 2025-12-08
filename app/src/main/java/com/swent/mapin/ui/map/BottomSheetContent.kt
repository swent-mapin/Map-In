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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.location.LocationViewModel
import com.swent.mapin.ui.event.AddEventScreen
import com.swent.mapin.ui.event.AddEventScreenTestTags
import com.swent.mapin.ui.event.EditEventScreen
import com.swent.mapin.ui.event.EditEventScreenTestTags
import com.swent.mapin.ui.event.EventViewModel
import com.swent.mapin.ui.filters.FiltersSection
import com.swent.mapin.ui.filters.FiltersSectionViewModel
import com.swent.mapin.ui.map.bottomsheet.SearchBarState
import com.swent.mapin.ui.map.bottomsheet.components.AllRecentItemsPage
import com.swent.mapin.ui.map.bottomsheet.components.AttendedEventsSection
import com.swent.mapin.ui.map.bottomsheet.components.CreateEventSection
import com.swent.mapin.ui.map.bottomsheet.components.MenuDivider
import com.swent.mapin.ui.map.bottomsheet.components.MenuListItem
import com.swent.mapin.ui.map.bottomsheet.components.OwnedEventsSection
import com.swent.mapin.ui.map.bottomsheet.components.SavedEventsSection
import com.swent.mapin.ui.map.bottomsheet.components.SearchBar
import com.swent.mapin.ui.map.bottomsheet.components.SearchResultsSection
import com.swent.mapin.ui.map.bottomsheet.components.UpcomingEventsSection
import com.swent.mapin.ui.map.search.RecentItem
import com.swent.mapin.ui.memory.MemoryFormData
import com.swent.mapin.ui.memory.MemoryFormScreen
import com.swent.mapin.ui.profile.ProfileSheet
import com.swent.mapin.ui.profile.ProfileViewModel

enum class BottomSheetScreen {
  MAIN_CONTENT,
  MEMORY_FORM,
  ADD_EVENT,
  EDIT_EVENT,
  PROFILE_SHEET
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
 * @param attendedEvents List of events the user has attended.
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
    topCategories: List<String> = emptyList(),
    onCategoryClick: (String) -> Unit = {},
    onClearRecentSearches: () -> Unit = {},
    // Memory form and events
    currentScreen: BottomSheetScreen = BottomSheetScreen.MAIN_CONTENT,
    availableEvents: List<Event> = emptyList(),
    // Initial event to prefill memory form when opening it
    initialMemoryEvent: Event? = null,
    // Joined/Saved/Attended/Owned events
    joinedEvents: List<Event> = emptyList(),
    attendedEvents: List<Event> = emptyList(),
    savedEvents: List<Event> = emptyList(),
    ownedEvents: List<Event> = emptyList(),
    ownedLoading: Boolean = false,
    ownedError: String? = null,
    onRetryOwnedEvents: () -> Unit = {},
    // Tabs & tags
    selectedTab: MapScreenViewModel.BottomSheetTab = MapScreenViewModel.BottomSheetTab.SAVED,
    // Callbacks
    onEventClick: (Event) -> Unit = {},
    // now accepts an optional Event to prefill the memory form (null = new memory without event)
    onCreateMemoryClick: (Event) -> Unit = {},
    onCreateEventClick: () -> Unit = {},
    onNavigateToFriends: () -> Unit = {},
    onNavigateToMemories: () -> Unit = {},
    onMemorySave: (MemoryFormData) -> Unit = {},
    onMemoryCancel: () -> Unit = {},
    onCreateEventDone: () -> Unit = {},
    onTabChange: (MapScreenViewModel.BottomSheetTab) -> Unit = {},
    onTabEventClick: (Event) -> Unit = {},
    onEditEvent: (Event) -> Unit = {},
    onDeleteEvent: (Event) -> Unit = {},
    onEditEventDone: () -> Unit = {},
    // Profile/Filters support
    avatarUrl: String? = null,
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    filterViewModel: FiltersSectionViewModel,
    locationViewModel: LocationViewModel,
    profileViewModel: ProfileViewModel,
    eventViewModel: EventViewModel,
    // Profile sheet parameters
    profileSheetUserId: String? = null,
    onProfileSheetClose: () -> Unit = {},
    onProfileSheetEventClick: (Event) -> Unit = {}
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
                onEventClick = onTabEventClick,
                initialSelectedEvent = initialMemoryEvent)
          }
          BottomSheetScreen.ADD_EVENT -> {
            AddEventScreen(
                modifier = Modifier.testTag(AddEventScreenTestTags.SCREEN),
                eventViewModel = eventViewModel,
                locationViewModel = locationViewModel,
                onCancel = onCreateEventDone,
                onDone = onCreateEventDone)
          }
          BottomSheetScreen.EDIT_EVENT -> {
            val eventToEditState by eventViewModel.eventToEdit.collectAsState()
            val eventToEdit = eventToEditState
            if (eventToEdit == null) {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.testTag("EditEventCircularIndicator"))
              }
            } else {
              EditEventScreen(
                  modifier = Modifier.testTag(EditEventScreenTestTags.SCREEN),
                  eventViewModel = eventViewModel,
                  locationViewModel = locationViewModel,
                  event = eventToEdit,
                  onCancel = onEditEventDone,
                  onDone = onEditEventDone)
            }
          }
          BottomSheetScreen.PROFILE_SHEET -> {
            if (profileSheetUserId != null) {
              ProfileSheet(
                  userId = profileSheetUserId,
                  onClose = onProfileSheetClose,
                  onEventClick = onProfileSheetEventClick)
            }
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
                    val density = LocalDensity.current
                    val imeVisible = WindowInsets.isImeVisible
                    val imeBottom = if (imeVisible) WindowInsets.ime.getBottom(density) else 0
                    val imeHeightDp = with(density) { imeBottom.toDp() }

                    Column(
                        modifier =
                            Modifier.fillMaxWidth().fillMaxHeight().padding(bottom = imeHeightDp)) {
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
                                            initialOffsetY = {
                                              it / TRANSITION_SLIDE_OFFSET_DIVISOR
                                            }))
                                    .togetherWith(
                                        fadeOut(
                                            animationSpec =
                                                tween(TRANSITION_FADE_OUT_DURATION_MS)) +
                                            slideOutVertically(
                                                animationSpec =
                                                    tween(TRANSITION_FADE_OUT_DURATION_MS),
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
                                      onEventClick = onEventClick)
                                } else {
                                  val density = LocalDensity.current
                                  val imeBottom = WindowInsets.ime.getBottom(density)
                                  val imePaddingModifier =
                                      if (imeBottom > 0) {
                                        Modifier.padding(
                                            bottom = with(density) { imeBottom.toDp() })
                                      } else {
                                        Modifier
                                      }
                                  val contentModifier =
                                      if (isFull)
                                          Modifier.fillMaxWidth()
                                              .then(imePaddingModifier)
                                              .verticalScroll(scrollState)
                                      else Modifier.fillMaxWidth()

                                  Column(modifier = contentModifier) {
                                    Spacer(modifier = Modifier.height(19.dp))

                                    CreateEventSection(onCreateEventClick = onCreateEventClick)

                                    Spacer(modifier = Modifier.height(19.dp))
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "My Events",
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(bottom = 12.dp))
                                    // Tabs
                                    TabRow(
                                        selectedTabIndex = selectedTab.ordinal,
                                        modifier = Modifier.fillMaxWidth()) {
                                          Tab(
                                              selected =
                                                  selectedTab ==
                                                      MapScreenViewModel.BottomSheetTab.SAVED,
                                              onClick = {
                                                onTabChange(MapScreenViewModel.BottomSheetTab.SAVED)
                                              },
                                              text = {
                                                Text(
                                                    text = "Saved",
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    overflow = TextOverflow.Ellipsis)
                                              })
                                          Tab(
                                              selected =
                                                  selectedTab ==
                                                      MapScreenViewModel.BottomSheetTab.UPCOMING,
                                              onClick = {
                                                onTabChange(
                                                    MapScreenViewModel.BottomSheetTab.UPCOMING)
                                              },
                                              text = {
                                                Text(
                                                    text = "Upcoming",
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    overflow = TextOverflow.Ellipsis)
                                              })
                                          Tab(
                                              selected =
                                                  selectedTab ==
                                                      MapScreenViewModel.BottomSheetTab.PAST,
                                              onClick = {
                                                onTabChange(MapScreenViewModel.BottomSheetTab.PAST)
                                              },
                                              text = {
                                                Text(
                                                    text = "Past",
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    overflow = TextOverflow.Ellipsis)
                                              })
                                          Tab(
                                              selected =
                                                  selectedTab ==
                                                      MapScreenViewModel.BottomSheetTab.OWNED,
                                              onClick = {
                                                onTabChange(MapScreenViewModel.BottomSheetTab.OWNED)
                                              },
                                              text = {
                                                Text(
                                                    text = "Owned",
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    overflow = TextOverflow.Ellipsis)
                                              })
                                        }
                                    Spacer(modifier = Modifier.height(16.dp))

                                    when (selectedTab) {
                                      MapScreenViewModel.BottomSheetTab.SAVED -> {
                                        SavedEventsSection(
                                            savedEvents = savedEvents,
                                            onEventClick = onTabEventClick)
                                      }
                                      MapScreenViewModel.BottomSheetTab.UPCOMING -> {
                                        UpcomingEventsSection(
                                            upcomingEvents = joinedEvents,
                                            onEventClick = onTabEventClick)
                                      }
                                      MapScreenViewModel.BottomSheetTab.PAST -> {
                                        AttendedEventsSection(
                                            attendedEvents = attendedEvents,
                                            onEventClick = onEventClick,
                                            onCreateMemoryClick = onCreateMemoryClick)
                                      }
                                      MapScreenViewModel.BottomSheetTab.OWNED -> {
                                        OwnedEventsSection(
                                            events = ownedEvents,
                                            loading = ownedLoading,
                                            error = ownedError,
                                            onEventClick = onTabEventClick,
                                            onEditEvent = onEditEvent,
                                            onDeleteEvent = onDeleteEvent,
                                            onRetry = onRetryOwnedEvents)
                                      }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

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
              ModalBottomSheet(
                  onDismissRequest = { showProfileMenu = false },
                  containerColor = MaterialTheme.colorScheme.surface,
                  dragHandle = {
                    Box(
                        modifier =
                            Modifier.padding(vertical = 8.dp)
                                .width(40.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)))
                  }) {
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

                            Text(
                                text = "Hello ${userProfile.name}!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold)
                          }

                          Spacer(modifier = Modifier.height(24.dp))

                          // ------- Profile -------
                          MenuListItem(
                              icon = Icons.Default.Person,
                              label = "Profile",
                              onClick = {
                                onProfileClick()
                                showProfileMenu = false
                              })
                          MenuDivider()

                          // ------- Friends -------
                          MenuListItem(
                              icon = Icons.Default.Group,
                              label = "Friends",
                              onClick = {
                                onNavigateToFriends()
                                showProfileMenu = false
                              })
                          MenuDivider()

                          // ------- Memories -------
                          MenuListItem(
                              icon = Icons.Default.PhotoAlbum,
                              label = "Memories",
                              onClick = {
                                onNavigateToMemories()
                                showProfileMenu = false
                              })
                          MenuDivider()

                          // ------- Settings -------
                          MenuListItem(
                              icon = Icons.Default.Settings,
                              label = "Settings",
                              onClick = {
                                onSettingsClick()
                                showProfileMenu = false
                              })
                        }
                  }
            }
          }
        }
      }
}
