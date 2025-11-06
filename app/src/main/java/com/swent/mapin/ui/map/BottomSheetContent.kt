package com.swent.mapin.ui.map

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.swent.mapin.model.LocationViewModel
import com.swent.mapin.model.event.Event
import com.swent.mapin.ui.event.AddEventScreen
import com.swent.mapin.ui.event.AddEventScreenTestTags
import com.swent.mapin.ui.profile.ProfileViewModel

// --- Assisted by AI ---
/** States for search bar interactions. */
@Stable
data class SearchBarState(
    val query: String,
    val shouldRequestFocus: Boolean,
    val onQueryChange: (String) -> Unit,
    val onTap: () -> Unit,
    val onFocusHandled: () -> Unit,
    val onClear: () -> Unit,
    val onSubmit: () -> Unit = {}
)

enum class BottomSheetScreen {
  MAIN_CONTENT,
  MEMORY_FORM,
  ADD_EVENT
}

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
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BottomSheetContent(
    state: BottomSheetState,
    fullEntryKey: Int,
    searchBarState: SearchBarState,
    // Search results and mode
    searchResults: List<Event> = emptyList(),
    isSearchMode: Boolean = false,
    recentItems: List<RecentItem> = emptyList(),
    onRecentSearchClick: (String) -> Unit = {},
    onRecentEventClick: (String) -> Unit = {},
    onClearRecentSearches: () -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    // Memory form and events
    currentScreen: BottomSheetScreen = BottomSheetScreen.MAIN_CONTENT,
    availableEvents: List<Event> = emptyList(),
    // Joined/Saved events
    joinedEvents: List<Event> = emptyList(),
    savedEvents: List<Event> = emptyList(),
    // Tabs & tags
    selectedTab: MapScreenViewModel.BottomSheetTab = MapScreenViewModel.BottomSheetTab.SAVED_EVENTS,
    // Callbacks
    onEventClick: (Event) -> Unit = {},
    onCreateMemoryClick: () -> Unit = {},
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
    filterViewModel: FiltersSectionViewModel,
    locationViewModel: LocationViewModel,
    profileViewModel: ProfileViewModel
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
                onCancel = onMemoryCancel)
          }
          BottomSheetScreen.ADD_EVENT -> {
            AddEventScreen(
                modifier = Modifier.testTag(AddEventScreenTestTags.SCREEN),
                onCancel = onCreateEventDone,
                onDone = onCreateEventDone)
          }
          BottomSheetScreen.MAIN_CONTENT -> {
            var showAllRecents by remember { mutableStateOf(false) }

            AnimatedContent(
                targetState = showAllRecents,
                transitionSpec = {
                  (fadeIn(animationSpec = tween(250)) +
                          slideInVertically(
                              animationSpec = tween(250), initialOffsetY = { it / 6 }))
                      .togetherWith(
                          fadeOut(animationSpec = tween(200)) +
                              slideOutVertically(
                                  animationSpec = tween(200), targetOffsetY = { -it / 6 }))
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
                          isFull = isFull,
                          isSearchMode = isSearchMode,
                          onTap = searchBarState.onTap,
                          focusRequester = focusRequester,
                          onSearchAction = {
                            searchBarState.onSubmit()
                            focusManager.clearFocus()
                          },
                          onClear = searchBarState.onClear,
                          avatarUrl = avatarUrl ?: userProfile?.avatarUrl,
                          onProfileClick = onProfileClick)

                      Spacer(modifier = Modifier.height(24.dp))

                      AnimatedContent(
                          targetState = isSearchMode,
                          transitionSpec = {
                            (fadeIn(animationSpec = tween(250)) +
                                    slideInVertically(
                                        animationSpec = tween(250), initialOffsetY = { it / 6 }))
                                .togetherWith(
                                    fadeOut(animationSpec = tween(200)) +
                                        slideOutVertically(
                                            animationSpec = tween(200), targetOffsetY = { it / 6 }))
                          },
                          modifier = Modifier.fillMaxWidth().weight(1f, fill = true),
                          label = "searchModeTransition") { searchActive ->
                            if (searchActive) {
                              SearchResultsSection(
                                  results = searchResults,
                                  query = searchBarState.query,
                                  recentItems = recentItems,
                                  onRecentSearchClick = onRecentSearchClick,
                                  onRecentEventClick = onRecentEventClick,
                                  onShowAllRecents = { showAllRecents = true },
                                  topCategories = topTags,
                                  onCategoryClick = onCategoryClick,
                                  filterViewModel = filterViewModel,
                                  locationViewModel = locationViewModel,
                                  userProfile = userProfile,
                                  modifier = Modifier.fillMaxSize(),
                                  onEventClick = onEventClick)
                            } else {
                              val contentModifier =
                                  if (isFull) Modifier.fillMaxWidth().verticalScroll(scrollState)
                                  else Modifier.fillMaxWidth()

                              Column(modifier = contentModifier) {
                                QuickActionsSection(
                                    onCreateMemoryClick = onCreateMemoryClick,
                                    onCreateEventClick = onCreateEventClick,
                                    onNavigateToFriends = onNavigateToFriends)

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

                                // Filters section visible unless collapsed
                                if (state != BottomSheetState.COLLAPSED) {
                                  filterSection.Render(
                                      Modifier.fillMaxWidth(),
                                      filterViewModel,
                                      locationViewModel,
                                      userProfile)

                                  Spacer(modifier = Modifier.height(16.dp))
                                }

                                // Dynamic tag selection (Discover)
                                if (topTags.isNotEmpty()) {
                                  TagsSection(
                                      topTags = topTags,
                                      selectedTags = selectedTags,
                                      onTagClick = onTagClick)
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                              }
                            }
                          }
                    }
                  }
                }
          }
        }
      }
}

@Composable
private fun SearchResultsSection(
    results: List<Event>,
    query: String,
    recentItems: List<RecentItem> = emptyList(),
    onRecentSearchClick: (String) -> Unit = {},
    onRecentEventClick: (String) -> Unit = {},
    onShowAllRecents: () -> Unit = {},
    topCategories: List<String> = emptyList(),
    onCategoryClick: (String) -> Unit = {},
    filterViewModel: FiltersSectionViewModel = viewModel(),
    locationViewModel: LocationViewModel = viewModel(),
    userProfile: com.swent.mapin.model.UserProfile? = null,
    modifier: Modifier = Modifier,
    onEventClick: (Event) -> Unit = {}
) {
  // When query is empty, show recent items and top categories instead of results
  if (query.isBlank()) {
    val scrollState = remember { ScrollState(0) }

    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
          // Recent items section (searches and events)
          if (recentItems.isNotEmpty()) {
            RecentItemsSection(
                recentItems = recentItems,
                onRecentSearchClick = onRecentSearchClick,
                onRecentEventClick = onRecentEventClick,
                onShowAll = onShowAllRecents)
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f))
          }

          // Top categories section
          if (topCategories.isNotEmpty()) {
            TopCategoriesSection(categories = topCategories, onCategoryClick = onCategoryClick)
          }

          Spacer(modifier = Modifier.height(24.dp))
        }
    return
  }

  // When there's a query but no results
  if (results.isEmpty()) {
    NoResultsMessage(query = query, modifier = modifier)
    return
  }

  // Show search results
  LazyColumn(modifier = modifier.fillMaxWidth()) {
    items(results) { event -> SearchResultItem(event = event, onClick = { onEventClick(event) }) }

    item { Spacer(modifier = Modifier.height(8.dp)) }
  }
}

@VisibleForTesting internal data class NoResultsCopy(val title: String, val subtitle: String)

@VisibleForTesting
internal fun buildNoResultsCopy(query: String): NoResultsCopy {
  return if (query.isBlank()) {
    NoResultsCopy(
        title = "Start typing to search", subtitle = "Search for events by name or location")
  } else {
    NoResultsCopy(
        title = "No results found", subtitle = "Try a different keyword or check the spelling.")
  }
}

@VisibleForTesting
internal fun buildSearchHeading(query: String): String {
  return if (query.isBlank()) "All events" else "Results for \"$query\""
}

@Composable
private fun NoResultsMessage(query: String, modifier: Modifier = Modifier) {
  val copy = remember(query) { buildNoResultsCopy(query) }

  Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(text = copy.title, style = MaterialTheme.typography.titleMedium)
      Spacer(modifier = Modifier.height(8.dp))
      Text(
          text = copy.subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center)
    }
  }
}

@Composable
private fun NoActivitiesMessage(modifier: Modifier = Modifier) {
  Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(text = "No recent events.", style = MaterialTheme.typography.titleMedium)
      Spacer(modifier = Modifier.height(8.dp))
      Text(
          text =
              "Your recent activity will appear here once you interact with events or create memories.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center)
    }
  }
}

@Composable
private fun SearchResultItem(
    event: Event,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
  Column(
      modifier =
          modifier.fillMaxWidth().clickable { onClick() }.testTag("eventItem_${'$'}{event.uid}")) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                  text = event.title,
                  style = MaterialTheme.typography.bodyLarge,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis)

              if (event.location.name.isNotBlank()) {
                Text(
                    text = event.location.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
              }
            }

        HorizontalDivider(
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
      }
}

/** Search bar that triggers full mode when tapped. Supports avatar image or preset icons. */
@Composable
private fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    isFull: Boolean,
    isSearchMode: Boolean,
    onTap: () -> Unit,
    focusRequester: FocusRequester,
    onSearchAction: () -> Unit,
    onClear: () -> Unit,
    avatarUrl: String?,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  var isFocused by remember { mutableStateOf(false) }
  val profileVisible = !isFocused && !isSearchMode
  val fieldHeight = TextFieldDefaults.MinHeight
  var textFieldValueState by remember {
    mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(value))
  }

  // Update text field value when value changes, preserving cursor at end
  LaunchedEffect(value) {
    if (textFieldValueState.text != value) {
      textFieldValueState =
          androidx.compose.ui.text.input.TextFieldValue(
              text = value, selection = androidx.compose.ui.text.TextRange(value.length))
    }
  }

  // When focus is gained and there's text, move cursor to end
  LaunchedEffect(isFocused, value) {
    if (isFocused && value.isNotEmpty() && textFieldValueState.selection.start == 0) {
      textFieldValueState =
          androidx.compose.ui.text.input.TextFieldValue(
              text = value, selection = androidx.compose.ui.text.TextRange(value.length))
    }
  }

  Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    TextField(
        value = textFieldValueState,
        onValueChange = { newValue ->
          textFieldValueState = newValue
          onValueChange(newValue.text)
        },
        placeholder = { Text("Search activities", style = MaterialTheme.typography.bodyLarge) },
        modifier =
            Modifier.weight(1f).height(fieldHeight).focusRequester(focusRequester).onFocusChanged {
                focusState ->
              val nowFocused = focusState.isFocused
              if (nowFocused && !isFocused) onTap()
              isFocused = nowFocused
            },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        trailingIcon = {
          AnimatedVisibility(
              visible = isSearchMode || value.isNotEmpty(),
              enter =
                  fadeIn(animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)),
              exit =
                  fadeOut(
                      animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing))) {
                IconButton(onClick = onClear) {
                  Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear search")
                }
              }
        },
        shape = RoundedCornerShape(16.dp),
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor =
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                unfocusedContainerColor =
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearchAction() }))

    val profileSlotWidth by
        animateDpAsState(
            targetValue = if (profileVisible) fieldHeight else 0.dp,
            animationSpec =
                tween(
                    durationMillis = if (profileVisible) 220 else 180,
                    easing = FastOutSlowInEasing),
            label = "profileWidth")

    val profileAlpha by
        animateFloatAsState(
            targetValue = if (profileVisible) 1f else 0f,
            animationSpec =
                tween(
                    durationMillis = if (profileVisible) 220 else 180,
                    easing = FastOutSlowInEasing),
            label = "profileFade")

    val slotPadding = if (profileSlotWidth > 0.dp) 12.dp else 0.dp

    Box(
        modifier =
            Modifier.padding(start = slotPadding).height(fieldHeight).width(profileSlotWidth),
        contentAlignment = Alignment.Center) {
          Surface(
              modifier = Modifier.fillMaxSize().testTag("profileButton").alpha(profileAlpha),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
              shape = RoundedCornerShape(16.dp)) {
                Box(
                    modifier =
                        Modifier.fillMaxSize().clickable(enabled = profileVisible) {
                          onProfileClick()
                        },
                    contentAlignment = Alignment.Center) {
                      if (!avatarUrl.isNullOrEmpty() && avatarUrl.startsWith("http")) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize())
                      } else {
                        Icon(
                            imageVector = getAvatarIcon(avatarUrl),
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize(),
                            tint = MaterialTheme.colorScheme.onSurface)
                      }
                    }
              }
        }
  }
}

/** Row of quick action buttons (Create Memory, Create Event, Friends). */
@Composable
private fun QuickActionsSection(
    modifier: Modifier = Modifier,
    onCreateMemoryClick: () -> Unit,
    onCreateEventClick: () -> Unit,
    onNavigateToFriends: () -> Unit
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Text(
        text = "Quick Actions",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 16.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      QuickActionButton(
          text = "Create Memory", modifier = Modifier.weight(1f), onClick = onCreateMemoryClick)
      QuickActionButton(
          text = "Create Event", modifier = Modifier.weight(1f), onClick = onCreateEventClick)
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      QuickActionButton(
          text = "Friends", modifier = Modifier.weight(1f), onClick = onNavigateToFriends)
    }
  }
}

/** button for quick actions - modern, minimalist with consistent height */
@Composable
private fun QuickActionButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
  Button(
      onClick = onClick,
      modifier = modifier.height(56.dp),
      shape = RoundedCornerShape(16.dp),
      colors =
          ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary)) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            maxLines = 2,
            style = MaterialTheme.typography.labelLarge)
      }
}

@Composable
private fun EventsSection(events: List<Event>, onEventClick: (Event) -> Unit) {
  if (events.isEmpty()) {
    NoResultsMessage(query = "", modifier = Modifier)
    return
  }
  val invertedEvents = events.reversed()
  var expanded by remember { mutableStateOf(false) }
  val visible = if (expanded) invertedEvents else invertedEvents.take(3)

  Column(modifier = Modifier.fillMaxWidth()) {
    visible.forEach { event ->
      SearchResultItem(
          event = event,
          modifier = Modifier.padding(horizontal = 16.dp),
          onClick = { onEventClick(event) })
      Spacer(modifier = Modifier.height(8.dp))
      HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f))
      Spacer(modifier = Modifier.height(8.dp))
    }

    if (events.size > 3) {
      Spacer(modifier = Modifier.height(4.dp))
      TextButton(
          onClick = { expanded = !expanded },
          modifier =
              Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("eventsShowMoreButton")) {
            Text(if (expanded) "Show less" else "Show more (${events.size - 3} more)")
          }
    }
  }
}

/** Full page showing all recent items with clear all button. */
@Composable
private fun AllRecentItemsPage(
    recentItems: List<RecentItem>,
    onRecentSearchClick: (String) -> Unit,
    onRecentEventClick: (String) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
  val scrollState = remember { ScrollState(0) }

  Column(modifier = modifier.fillMaxSize().fillMaxHeight()) {
    // Header with back button and clear all
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
      // Left: Close button
      IconButton(
          onClick = onBack,
          modifier = Modifier.align(Alignment.CenterStart).testTag("backFromAllRecentsButton")) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface)
          }

      // Center: Title
      Text(
          text = "Recent searches",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.align(Alignment.Center))

      // Right: Clear All button
      TextButton(
          onClick = onClearAll,
          modifier = Modifier.align(Alignment.CenterEnd).testTag("clearAllRecentButton")) {
            Text("Clear All", style = MaterialTheme.typography.bodyMedium)
          }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Scrollable content
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
      recentItems.forEach { item ->
        when (item) {
          is RecentItem.Search -> {
            RecentSearchItem(
                searchQuery = item.query, onClick = { onRecentSearchClick(item.query) })
          }
          is RecentItem.ClickedEvent -> {
            RecentEventItem(
                eventTitle = item.eventTitle, onClick = { onRecentEventClick(item.eventId) })
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

/** Recent items section with list of recent searches and events, plus Show All button. */
@Composable
private fun RecentItemsSection(
    recentItems: List<RecentItem>,
    onRecentSearchClick: (String) -> Unit,
    onRecentEventClick: (String) -> Unit,
    onShowAll: () -> Unit,
    modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
          Text(
              text = "Recents",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface)

          if (recentItems.isNotEmpty()) {
            TextButton(
                onClick = onShowAll, modifier = Modifier.testTag("showAllRecentSearchesButton")) {
                  Text("Show all", style = MaterialTheme.typography.bodyMedium)
                }
          }
        }

    Spacer(modifier = Modifier.height(8.dp))

    recentItems.take(3).forEach { item ->
      when (item) {
        is RecentItem.Search -> {
          RecentSearchItem(searchQuery = item.query, onClick = { onRecentSearchClick(item.query) })
        }
        is RecentItem.ClickedEvent -> {
          RecentEventItem(
              eventTitle = item.eventTitle, onClick = { onRecentEventClick(item.eventId) })
        }
      }
    }
  }
}

/** Individual recent search item with search icon and clickable text. */
@Composable
private fun RecentSearchItem(
    searchQuery: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  Row(
      modifier =
          modifier
              .fillMaxWidth()
              .clickable { onClick() }
              .padding(horizontal = 16.dp, vertical = 12.dp)
              .testTag("recentSearchItem_$searchQuery"),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = "Recent search",
            tint = MaterialTheme.colorScheme.onSurfaceVariant)

        Text(
            text = searchQuery,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f))
      }
}

/** Individual recent event item with location icon and clickable text. */
@Composable
private fun RecentEventItem(
    eventTitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  Row(
      modifier =
          modifier
              .fillMaxWidth()
              .clickable { onClick() }
              .padding(horizontal = 16.dp, vertical = 12.dp)
              .testTag("recentEventItem_$eventTitle"),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = "Recent event",
            tint = MaterialTheme.colorScheme.primary)

        Text(
            text = eventTitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f))
      }
}

/** Top categories section with list of popular event categories for quick search. */
@Composable
private fun TopCategoriesSection(
    categories: List<String>,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Text(
        text = "Top Categories",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 16.dp))

    Spacer(modifier = Modifier.height(8.dp))

    categories.forEach { category ->
      TopCategoryItem(categoryName = category, onClick = { onCategoryClick(category) })
    }
  }
}

/** Individual category item with clickable text. */
@Composable
private fun TopCategoryItem(
    categoryName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  Row(
      modifier =
          modifier
              .fillMaxWidth()
              .clickable { onClick() }
              .padding(horizontal = 16.dp, vertical = 12.dp)
              .testTag("topCategoryItem_$categoryName"),
      verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = categoryName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f))
      }
}

/** Get avatar icon from URL/ID, matching ProfileScreen presets. */
private fun getAvatarIcon(avatarUrl: String?): ImageVector {
  if (avatarUrl.isNullOrEmpty()) return Icons.Default.Person
  return when (avatarUrl) {
    "person" -> Icons.Default.Person
    "face" -> Icons.Default.Face
    "star" -> Icons.Default.Star
    "favorite" -> Icons.Default.Favorite
    else -> Icons.Default.Person
  }
}
