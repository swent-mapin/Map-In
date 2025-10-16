package com.swent.mapin.ui.map

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

/**
 * Unified BottomSheet content:
 * - Search (SearchViewModel, live results, clear X, focus handling)
 * - Default content (Quick actions, Recent, Discover)
 * - Memory form screen (animated swap)
 */
@Composable
fun BottomSheetContent(
    state: BottomSheetState,
    fullEntryKey: Int,
    searchViewModel: SearchViewModel,
    showMemoryForm: Boolean = false,
    availableEvents: List<Event> = emptyList(),
    onCreateMemoryClick: () -> Unit = {},
    onMemorySave: (MemoryFormData) -> Unit = {},
    onMemoryCancel: () -> Unit = {},
    // Called when user clears search to let parent collapse to MEDIUM, etc.
    onExitSearch: () -> Unit = {}
) {
  val ui = searchViewModel.ui.collectAsState().value
  val isFull = state == BottomSheetState.FULL || ui.searchMode

  val scrollState = remember(fullEntryKey) { ScrollState(0) }
  val focusRequester = remember { FocusRequester() }
  val focusManager = LocalFocusManager.current

  LaunchedEffect(ui.shouldRequestFocus) {
    if (ui.shouldRequestFocus) {
      focusRequester.requestFocus()
      searchViewModel.onFocusHandled()
    }
  }

  AnimatedContent(
      targetState = showMemoryForm,
      transitionSpec = {
        (fadeIn(tween(300)) + slideInVertically(tween(300), initialOffsetY = { it / 4 }))
            .togetherWith(
                fadeOut(tween(200)) + slideOutVertically(tween(200), targetOffsetY = { -it / 4 }))
      },
      label = "bottomSheetMemoryFormTransition") { showForm ->
        if (showForm) {
          // Memory form screen
          val memoryFormScroll = remember { ScrollState(0) }
          MemoryFormScreen(
              scrollState = memoryFormScroll,
              availableEvents = availableEvents,
              onSave = onMemorySave,
              onCancel = onMemoryCancel)
        } else {
          // Regular sheet (search + default content or search results)
          Column(modifier = Modifier.fillMaxWidth()) {
            SearchBar(
                value = ui.query,
                onValueChange = searchViewModel::onQueryChange,
                isFull = isFull,
                onTap = { if (!isFull) searchViewModel.onSearchTapped() },
                focusRequester = focusRequester,
                onSearchAction = { focusManager.clearFocus() },
                onClear = {
                  focusManager.clearFocus()
                  searchViewModel.onClearSearch()
                  onExitSearch()
                })

            Spacer(Modifier.height(24.dp))

            val contentModifier =
                if (isFull) Modifier.fillMaxWidth().verticalScroll(scrollState)
                else Modifier.fillMaxWidth()

            Column(modifier = contentModifier) {
              if (ui.searchMode) {
                SearchResultsList(
                    items = ui.results,
                    showEmpty = ui.showNoResults,
                    onClear = {
                      focusManager.clearFocus()
                      searchViewModel.onClearSearch()
                      onExitSearch()
                    })
              } else {
                QuickActionsSection(onCreateMemoryClick = onCreateMemoryClick)

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f))
                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Recent Activities",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp))

                repeat(4) { i ->
                  ActivityItem(
                      title = "Activity ${i + 1}",
                      description = "Example description for activity ${i + 1}.")
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f))
                Spacer(Modifier.height(16.dp))

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

                Spacer(Modifier.height(24.dp))
              }
            }
          }
        }
      }
}

/** Search bar with clear (X); taps expand to FULL/search mode. */
@Composable
private fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    isFull: Boolean,
    onTap: () -> Unit,
    focusRequester: FocusRequester,
    onSearchAction: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
  OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      placeholder = { Text("Search activities") },
      trailingIcon = {
        if (value.isNotEmpty()) {
          IconButton(onClick = onClear) {
            Icon(Icons.Filled.Close, contentDescription = "Clear search")
          }
        }
      },
      modifier =
          modifier.fillMaxWidth().focusRequester(focusRequester).onFocusChanged { fs ->
            if (!isFull && fs.isFocused) onTap()
          },
      singleLine = true,
      textStyle = MaterialTheme.typography.bodyLarge,
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
      keyboardActions = KeyboardActions(onSearch = { onSearchAction() }),
  )
}

/** Results list + empty state. */
@Composable
private fun SearchResultsList(items: List<Event>, showEmpty: Boolean, onClear: () -> Unit) {
  Column(Modifier.fillMaxWidth()) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text("Results", style = MaterialTheme.typography.titleMedium)
      TextButton(onClick = onClear) { Text("Clear") }
    }

    if (showEmpty) {
      Text(
          text = "No results found",
          style = MaterialTheme.typography.bodyMedium,
          color = Color.Gray,
          modifier = Modifier.padding(top = 8.dp))
    } else {
      items.forEach { e ->
        Text(e.title, style = MaterialTheme.typography.bodyLarge)
        if (e.location.name.isNotBlank()) {
          Text(e.location.name, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f))
        Spacer(Modifier.height(8.dp))
      }
    }
  }
}

/** Quick actions; “Create Memory” triggers memory form mode in parent. */
@Composable
private fun QuickActionsSection(modifier: Modifier = Modifier, onCreateMemoryClick: () -> Unit) {
  val focusManager = LocalFocusManager.current
  val showDialog = remember { mutableStateOf(false) }
  Column(modifier = modifier.fillMaxWidth()) {
    Text(
        text = "Quick Actions",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 16.dp))

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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

@Composable
private fun ActivityItem(title: String, description: String, modifier: Modifier = Modifier) {
  Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
    Text(text = title, style = MaterialTheme.typography.titleSmall)
    Spacer(modifier = Modifier.height(4.dp))
    Text(text = description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
  }
}
