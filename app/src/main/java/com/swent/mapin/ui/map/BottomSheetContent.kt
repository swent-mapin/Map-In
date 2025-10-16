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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.swent.mapin.model.event.Event
import com.swent.mapin.ui.components.AddEventPopUp
import com.swent.mapin.ui.components.AddEventPopUpTestTags

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
 * - (Temporary) Recent activities
 * - (Temporary) Discover section
 * - Memory creation form (when showMemoryForm is true)
 *
 * @param state Current bottom sheet state
 * @param fullEntryKey Increments each time we enter full mode - triggers scroll reset
 * @param searchBarState search bar state and callbacks
 * @param showMemoryForm Whether to show memory creation form
 * @param availableEvents List of events that can be linked to memories
 * @param topTags List of top tags to display in the discover section
 * @param selectedTags Set of currently selected tags
 * @param onTagClick Callback when a tag is clicked
 * @param onCreateMemoryClick Callback when "Create Memory" button is clicked
 * @param onMemorySave Callback when memory is saved
 * @param onMemoryCancel Callback when memory creation is cancelled
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BottomSheetContent(
    state: BottomSheetState,
    fullEntryKey: Int,
    searchBarState: SearchBarState,
    showMemoryForm: Boolean = false,
    availableEvents: List<Event> = emptyList(),
    topTags: List<String> = emptyList(),
    selectedTags: Set<String> = emptySet(),
    onTagClick: (String) -> Unit = {},
    onCreateMemoryClick: () -> Unit = {},
    onMemorySave: (MemoryFormData) -> Unit = {},
    onMemoryCancel: () -> Unit = {},
    searchResults: List<Event> = emptyList(),
    isSearchMode: Boolean = false,
    recentActivities: List<Event> = emptyList(),
    onEventClick: (Event) -> Unit = {}
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
            Column(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                SearchBar(
                    value = searchBarState.query,
                    onValueChange = searchBarState.onQueryChange,
                    onTap = searchBarState.onTap,
                    focusRequester = focusRequester,
                    onSearchAction = { focusManager.clearFocus() },
                    onClear = searchBarState.onClear)

                Spacer(modifier = Modifier.height(24.dp))

                if (isSearchMode) {
                    SearchResultsSection(
                        results = searchResults,
                        query = searchBarState.query,
                        modifier = Modifier.weight(1f, fill = true),
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

                        Text(
                            text = "Recent Activities",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp))

                        if (recentActivities.isNotEmpty()) {
                            recentActivities.forEach { event ->
                                ActivityItem(
                                    title = event.title,
                                    description = event.location.name,
                                    modifier = Modifier.clickable { onEventClick(event) })
                            }
                        } else {
                            NoActivitiesMessage(modifier = Modifier.fillMaxWidth())
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f))

                        Spacer(modifier = Modifier.height(16.dp))

              // Dynamic tag selection
              if (topTags.isNotEmpty()) {
                TagsSection(topTags = topTags, selectedTags = selectedTags, onTagClick = onTagClick)
              }

                        Spacer(modifier = Modifier.height(24.dp))
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

    val heading = if (query.isBlank()) "All events" else "Results for \"$query\""

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

@Composable
private fun NoResultsMessage(query: String, modifier: Modifier = Modifier) {
    val message =
        if (query.isBlank()) {
            "No events available yet."
        } else {
            "No results found"
        }

    val subtitle =
        if (query.isBlank()) {
            "Try again once events are added."
        } else {
            "Try a different keyword or check the spelling."
        }

    Box(modifier = modifier.fillMaxWidth().fillMaxHeight(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = message, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
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
            Text(text = "No recent activities", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your recent activity will appear here once you interact with events or create memories.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SearchResultItem(event: Event, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
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

            event.attendeeCount?.let { attendees ->
                Text(
                    text = "$attendees attending",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Search bar that triggers full mode when tapped. */
@Composable
private fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onTap: () -> Unit,
    focusRequester: FocusRequester,
    onSearchAction: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("Search activities") },
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) {
                    onTap()
                }
            },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        trailingIcon = {
            if (isFocused) {
                IconButton(onClick = onClear) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear search")
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearchAction() }))
}

/** Row of quick action buttons (Create Memory, Create Event, Filters). */
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
      QuickActionButton(
          text = "Filters", modifier = Modifier.weight(1f), onClick = { focusManager.clearFocus() })
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

/** Tag item for discover section - displays tag text and handles selection. */
@Composable
private fun TagItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
  val contentColor =
      if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary

  OutlinedButton(
      onClick = onClick,
      modifier = modifier.padding(4.dp).defaultMinSize(minHeight = 36.dp),
      shape = RoundedCornerShape(16.dp),
      colors =
          ButtonDefaults.buttonColors(
              containerColor = backgroundColor, contentColor = contentColor)) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis)
      }
}

/** Section displaying dynamic tags - replaces the hardcoded discover section. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsSection(
    topTags: List<String>,
    selectedTags: Set<String>,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Text(
        text = "Discover",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 8.dp))

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
          topTags.forEach { tag ->
            val isSelected = selectedTags.contains(tag)
            TagItem(text = tag, isSelected = isSelected, onClick = { onTagClick(tag) })
          }
        }
  }
}
