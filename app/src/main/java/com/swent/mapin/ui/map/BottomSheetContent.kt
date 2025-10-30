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
import androidx.compose.material.icons.outlined.AccountCircle
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swent.mapin.model.LocationViewModel
import com.swent.mapin.model.event.Event
import com.swent.mapin.ui.components.AddEventPopUp
import com.swent.mapin.ui.components.AddEventPopUpTestTags
import com.swent.mapin.ui.profile.ProfileViewModel

// Assisted by AI
/** States for search bar interactions. */
@Stable
data class SearchBarState(
    val query: String,
    val shouldRequestFocus: Boolean,
    val onQueryChange: (String) -> Unit,
    val onTap: () -> Unit,
    val onFocusHandled: () -> Unit,
    val onClear: () -> Unit
)

/**
 * Content for the bottom sheet
 * - Search bar (always visible)
 * - Quick actions
 * - Toggle between Recent Activities and Joined Events
 * - Memory creation form (when showMemoryForm is true)
 *
 * @param state Current bottom sheet state
 * @param fullEntryKey Increments each time we enter full mode - triggers scroll reset
 * @param searchBarState search bar state and callbacks
 * @param showMemoryForm Whether to show memory creation form
 * @param availableEvents List of events that can be linked to memories
 * @param joinedEvents List of events the user has joined
 * @param selectedTab Currently selected tab (Recent Activities or Joined Events)
 * @param selectedTags Set of currently selected tags
 * @param onTagClick Callback when a tag is clicked
 * @param onCreateMemoryClick Callback when "Create Memory" button is clicked
 * @param onMemorySave Callback when memory is saved
 * @param onMemoryCancel Callback when memory creation is cancelled
 * @param onTabChange Callback when tab is changed
 * @param onProfileClick Callback when the profile icon is tapped
 * @param filterViewModel ViewModel managing filter state (time, place, price, tags, etc.)
 * @param locationViewModel ViewModel for location search and autocomplete
 * @param profileViewModel ViewModel providing current user profile
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
    // Memory form and events
    showMemoryForm: Boolean = false,
    availableEvents: List<Event> = emptyList(),
    // Joined events
    joinedEvents: List<Event> = emptyList(),
    // Saved events
    savedEvents: List<Event> = emptyList(),
    // Tab and tags
    selectedTab: MapScreenViewModel.BottomSheetTab = MapScreenViewModel.BottomSheetTab.SAVED_EVENTS,
    selectedTags: Set<String> = emptySet(),
    onTagClick: (String) -> Unit = {},
    // Callbacks
    onEventClick: (Event) -> Unit = {},
    onCreateMemoryClick: () -> Unit = {},
    onMemorySave: (MemoryFormData) -> Unit = {},
    onMemoryCancel: () -> Unit = {},
    onTabChange: (MapScreenViewModel.BottomSheetTab) -> Unit = {},
    onJoinedEventClick: (Event) -> Unit = {},
    onProfileClick: () -> Unit = {},
    filterViewModel: FiltersSectionViewModel = viewModel(),
    locationViewModel: LocationViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel()
    onTabEventClick: (Event) -> Unit = {}
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

  // Animated transition between regular content and memory form
  AnimatedContent(
      targetState = showMemoryForm,
      transitionSpec = {
        (fadeIn(animationSpec = tween(300)) +
                slideInVertically(animationSpec = tween(300), initialOffsetY = { it / 4 }))
            .togetherWith(
                fadeOut(animationSpec = tween(200)) +
                    slideOutVertically(animationSpec = tween(200), targetOffsetY = { -it / 4 }))
      },
      label = "memoryFormTransition") { showForm ->
        if (showForm) {
          // Memory form content
          val memoryFormScrollState = remember { ScrollState(0) }
          MemoryFormScreen(
              scrollState = memoryFormScrollState,
              availableEvents = availableEvents,
              onSave = onMemorySave,
              onCancel = onMemoryCancel)
        } else {
          // Regular bottom sheet content
          Column(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            SearchBar(
                value = searchBarState.query,
                onValueChange = searchBarState.onQueryChange,
                isFull = isFull,
                isSearchMode = isSearchMode,
                onTap = searchBarState.onTap,
                focusRequester = focusRequester,
                onSearchAction = { focusManager.clearFocus() },
                onClear = searchBarState.onClear,
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
                        modifier = Modifier.fillMaxSize(),
                        onEventClick = onEventClick)
                  } else {
                    val contentModifier =
                        if (isFull) Modifier.fillMaxWidth().verticalScroll(scrollState)
                        else Modifier.fillMaxWidth()

                    Column(modifier = contentModifier) {
                      QuickActionsSection(onCreateMemoryClick = onCreateMemoryClick)

                      Spacer(modifier = Modifier.height(16.dp))

                      HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f))

                      Spacer(modifier = Modifier.height(16.dp))

                      TabRow(
                          
                        
                        
                        
                        
                        
                        
                        
                        Index =
                              if (selectedTab == MapScreenViewModel.BottomSheetTab.SAVED_EVENTS) 0
                              else 1,
                          modifier = Modifier.fillMaxWidth()) {
                            Tab(
                                selected =
                                    selectedTab == MapScreenViewModel.BottomSheetTab.SAVED_EVENTS,
                                onClick = {
                                  onTabChange(MapScreenViewModel.BottomSheetTab.SAVED_EVENTS)
                                },
                                text = { Text("Saved Events") })
                            Tab(
                                selected =
                                    selectedTab == MapScreenViewModel.BottomSheetTab.JOINED_EVENTS,
                                onClick = {
                                  onTabChange(MapScreenViewModel.BottomSheetTab.JOINED_EVENTS)
                                },
                                text = { Text("Joined Events") })
                          }

                      Spacer(modifier = Modifier.height(16.dp))

                      when (selectedTab) {
                        MapScreenViewModel.BottomSheetTab.SAVED_EVENTS -> {
                          EventsSection(events = savedEvents, onEventClick = onTabEventClick)
                        }
                        MapScreenViewModel.BottomSheetTab.JOINED_EVENTS -> {
                          EventsSection(events = joinedEvents, onEventClick = onTabEventClick)
                        }
                      }

                      if (state != BottomSheetState.COLLAPSED) {
                        Spacer(modifier = Modifier.height(16.dp))

                        filterSection.Render(
                            Modifier.fillMaxWidth(),
                            filterViewModel,
                            locationViewModel,
                            userProfile)
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
    modifier: Modifier = Modifier,
    onEventClick: (Event) -> Unit = {}
) {
  if (results.isEmpty()) {
    NoResultsMessage(query = query, modifier = modifier)
    return
  }

  val heading = remember(query) { buildSearchHeading(query) }

  LazyColumn(
      modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
          Text(
              text = heading,
              style = MaterialTheme.typography.titleMedium,
              modifier = Modifier.padding(horizontal = 16.dp))
        }

        items(results) { event ->
          SearchResultItem(
              event = event,
              modifier = Modifier.padding(horizontal = 16.dp),
              onClick = { onEventClick(event) })
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
      }
}

@VisibleForTesting internal data class NoResultsCopy(val title: String, val subtitle: String)

@VisibleForTesting
internal fun buildNoResultsCopy(query: String): NoResultsCopy {
  return if (query.isBlank()) {
    NoResultsCopy(title = "No events available yet.", subtitle = "Try again once events are added.")
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
  Surface(
      shape = RoundedCornerShape(16.dp),
      tonalElevation = 2.dp,
      modifier = modifier.fillMaxWidth().clickable { onClick() }) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                  text = event.title,
                  style = MaterialTheme.typography.titleMedium,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis)

              if (event.location.name.isNotBlank()) {
                Text(
                    text = event.location.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
              }

              val tagsSummary = event.tags.take(3).joinToString(separator = " • ")
              if (tagsSummary.isNotBlank()) {
                Text(
                    text = tagsSummary,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
              }

              if (event.participantIds.isNotEmpty()) {
                Text(
                    text = "${event.participantIds.size} attending",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
      }
}

/** Search bar that triggers full mode when tapped. Modern, minimalist design. */
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
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  var isFocused by remember { mutableStateOf(false) }
  // Keep the profile shortcut hidden whenever the search sheet is active
  val profileVisible = !isFocused && !isSearchMode
  val fieldHeight = TextFieldDefaults.MinHeight

  Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("Search activities", style = MaterialTheme.typography.bodyLarge) },
        modifier =
            Modifier.weight(1f).height(fieldHeight).focusRequester(focusRequester).onFocusChanged {
                focusState ->
              val nowFocused = focusState.isFocused
              if (nowFocused && !isFocused) {
                onTap()
              }
              isFocused = nowFocused
            },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        trailingIcon = {
          AnimatedVisibility(
              visible = isFocused || value.isNotEmpty(),
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
                IconButton(
                    onClick = onProfileClick,
                    enabled = profileVisible,
                    modifier = Modifier.fillMaxSize()) {
                      Icon(
                          imageVector = Icons.Outlined.AccountCircle,
                          contentDescription = "Profile",
                          tint = MaterialTheme.colorScheme.onSurface)
                    }
              }
        }
  }
}

/** Row of quick action buttons (Create Memory, Create Event). */
@Composable
private fun QuickActionsSection(modifier: Modifier = Modifier, onCreateMemoryClick: () -> Unit) {
  val focusManager = LocalFocusManager.current
  val showDialog = remember { mutableStateOf(false) }
  Column(modifier = modifier.fillMaxWidth()) {
    Text(
        text = "Quick Actions",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 16.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      QuickActionButton(
          text = "Create Memory", modifier = Modifier.weight(1f), onClick = onCreateMemoryClick)
      QuickActionButton(
          text = "Create Event",
          modifier = Modifier.weight(1f),
          onClick = {
            focusManager.clearFocus()
            showDialog.value = true
          })
    }
  }
  if (showDialog.value) {
    AddEventPopUp(
        modifier = Modifier.testTag(AddEventPopUpTestTags.POPUP),
        onDone = { showDialog.value = false },
        onBack = { showDialog.value = false },
        onCancel = { showDialog.value = false },
        onDismiss = { showDialog.value = false },
    )
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
            softWrap = true,
            style = MaterialTheme.typography.labelLarge)
      }
}

@Composable
private fun EventsSection(events: List<Event>, onEventClick: (Event) -> Unit) {
  if (events.isEmpty()) {
    NoResultsMessage(query = "", modifier = Modifier)
    return
  }

  var expanded by remember { mutableStateOf(false) }
  val visible = if (expanded) events else events.take(3)

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
