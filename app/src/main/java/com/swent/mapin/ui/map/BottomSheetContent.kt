package com.swent.mapin.ui.map

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.swent.mapin.model.event.Event
import java.text.SimpleDateFormat
import java.util.Locale

// Assisted by AI
/** States for search bar interactions. */
@Stable
data class SearchBarState(
    val query: String,
    val shouldRequestFocus: Boolean,
    val onQueryChange: (String) -> Unit,
    val onTap: () -> Unit,
    val onFocusHandled: () -> Unit
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
 * @param onCreateMemoryClick Callback when "Create Memory" button is clicked
 * @param onMemorySave Callback when memory is saved
 * @param onMemoryCancel Callback when memory creation is cancelled
 * @param onTabChange Callback when tab is changed
 * @param onJoinedEventClick Callback when a joined event is clicked
 */
@Composable
fun BottomSheetContent(
    state: BottomSheetState,
    fullEntryKey: Int,
    searchBarState: SearchBarState,
    showMemoryForm: Boolean = false,
    availableEvents: List<Event> = emptyList(),
    joinedEvents: List<Event> = emptyList(),
    selectedTab: MapScreenViewModel.BottomSheetTab =
        MapScreenViewModel.BottomSheetTab.RECENT_ACTIVITIES,
    onCreateMemoryClick: () -> Unit = {},
    onMemorySave: (MemoryFormData) -> Unit = {},
    onMemoryCancel: () -> Unit = {},
    onTabChange: (MapScreenViewModel.BottomSheetTab) -> Unit = {},
    onJoinedEventClick: (Event) -> Unit = {}
) {
  val isFull = state == BottomSheetState.FULL
  val scrollState = remember(fullEntryKey) { ScrollState(0) }
  val focusRequester = remember { FocusRequester() }
  val focusManager = LocalFocusManager.current

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
        (fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) +
                slideInVertically(
                    animationSpec = androidx.compose.animation.core.tween(300),
                    initialOffsetY = { it / 4 }))
            .togetherWith(
                fadeOut(animationSpec = androidx.compose.animation.core.tween(200)) +
                    slideOutVertically(
                        animationSpec = androidx.compose.animation.core.tween(200),
                        targetOffsetY = { -it / 4 }))
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
          Column(modifier = Modifier.fillMaxWidth()) {
            SearchBar(
                value = searchBarState.query,
                onValueChange = searchBarState.onQueryChange,
                isFull = isFull,
                onTap = { if (!isFull) searchBarState.onTap() },
                focusRequester = focusRequester,
                onSearchAction = { focusManager.clearFocus() })

            Spacer(modifier = Modifier.height(24.dp))

            val contentModifier =
                if (isFull) Modifier.fillMaxWidth().verticalScroll(scrollState)
                else Modifier.fillMaxWidth()

            Column(modifier = contentModifier) {
              QuickActionsSection(onCreateMemoryClick = onCreateMemoryClick)

              Spacer(modifier = Modifier.height(16.dp))

              HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f))

              Spacer(modifier = Modifier.height(16.dp))

              // Tab selector
              TabRow(
                  selectedTabIndex =
                      if (selectedTab == MapScreenViewModel.BottomSheetTab.RECENT_ACTIVITIES) 0
                      else 1,
                  modifier = Modifier.fillMaxWidth()) {
                    Tab(
                        selected =
                            selectedTab == MapScreenViewModel.BottomSheetTab.RECENT_ACTIVITIES,
                        onClick = {
                          onTabChange(MapScreenViewModel.BottomSheetTab.RECENT_ACTIVITIES)
                        },
                        text = { Text("Recent Activities") })
                    Tab(
                        selected = selectedTab == MapScreenViewModel.BottomSheetTab.JOINED_EVENTS,
                        onClick = { onTabChange(MapScreenViewModel.BottomSheetTab.JOINED_EVENTS) },
                        text = { Text("Joined Events") })
                  }

              Spacer(modifier = Modifier.height(16.dp))

              // Content based on selected tab
              when (selectedTab) {
                MapScreenViewModel.BottomSheetTab.RECENT_ACTIVITIES -> {
                  repeat(4) { index ->
                    ActivityItem(
                        title = "Activity ${index + 1}",
                        description = "Example description for activity ${index + 1}.")
                  }
                }
                MapScreenViewModel.BottomSheetTab.JOINED_EVENTS -> {
                  JoinedEventsSection(events = joinedEvents, onEventClick = onJoinedEventClick)
                }
              }

              Spacer(modifier = Modifier.height(16.dp))

              HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f))

              Spacer(modifier = Modifier.height(16.dp))

              Text(
                  text = "Discover",
                  style = MaterialTheme.typography.titleMedium,
                  modifier = Modifier.padding(bottom = 8.dp))

              val categories = listOf("Sports", "Music", "Food", "Art", "Outdoors", "Learning")
              categories.forEach { category ->
                OutlinedButton(
                    onClick = {}, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                      Text(category, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
              }

              Spacer(modifier = Modifier.height(24.dp))
            }
          }
        }
      }
}

/** Search bar that triggers full mode when tapped. */
@Composable
private fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    isFull: Boolean,
    onTap: () -> Unit,
    focusRequester: FocusRequester,
    onSearchAction: () -> Unit,
    modifier: Modifier = Modifier
) {
  OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      placeholder = { Text("Search activities") },
      modifier =
          modifier.fillMaxWidth().focusRequester(focusRequester).onFocusChanged { focusState ->
            if (!isFull && focusState.isFocused) {
              onTap()
            }
          },
      singleLine = true,
      textStyle = MaterialTheme.typography.bodyLarge,
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
      keyboardActions = KeyboardActions(onSearch = { onSearchAction() }))
}

/** Row of quick action buttons (Create Memory, Create Event, Filters). */
@Composable
private fun QuickActionsSection(modifier: Modifier = Modifier, onCreateMemoryClick: () -> Unit) {
  val focusManager = LocalFocusManager.current
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
          onClick = { focusManager.clearFocus() })
      QuickActionButton(
          text = "Filters", modifier = Modifier.weight(1f), onClick = { focusManager.clearFocus() })
    }
  }
}

/** button for quick actions */
@Composable
private fun QuickActionButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
  Button(
      onClick = onClick,
      modifier = modifier.defaultMinSize(minHeight = 44.dp).padding(horizontal = 4.dp),
      shape = RoundedCornerShape(20.dp),
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

/** List item displaying an activity title and description. */
@Composable
private fun ActivityItem(title: String, description: String, modifier: Modifier = Modifier) {
  Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
    Text(text = title, style = MaterialTheme.typography.titleSmall)
    Spacer(modifier = Modifier.height(4.dp))
    Text(text = description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
  }
}

/** Section displaying joined events with click handling. */
@Composable
private fun JoinedEventsSection(events: List<Event>, onEventClick: (Event) -> Unit) {
  if (events.isEmpty()) {
    // Empty state
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
              text = "You haven't joined any events yet",
              style = MaterialTheme.typography.bodyMedium,
              color = Color.Gray,
              textAlign = TextAlign.Center)
        }
  } else {
    Column(modifier = Modifier.fillMaxWidth()) {
      events.forEach { event ->
        val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val formattedDate = event.date?.toDate()?.let { dateFormatter.format(it) } ?: "No date"

        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .clickable { onEventClick(event) }
                    .padding(vertical = 12.dp)) {
              Text(text = event.title, style = MaterialTheme.typography.titleSmall)
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                  text = formattedDate,
                  style = MaterialTheme.typography.bodySmall,
                  color = Color.Gray)
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                  text = event.location.name,
                  style = MaterialTheme.typography.bodySmall,
                  color = Color.Gray,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis)
            }

        HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f))
      }
    }
  }
}
