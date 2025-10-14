package com.swent.mapin.ui.map

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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Content of the bottom sheet, including search bar, quick actions, recent activities, and discover section. */
/**
 * Content of the bottom sheet, including search bar, quick actions, recent activities, and discover section.
 *
 * @param state Current state of the bottom sheet (COLLAPSED, MEDIUM, FULL)
 * @param fullEntryKey Key that changes when entering full state to reset scroll position
 * @param searchViewModel ViewModel managing search state and logic
 * @param onExitSearch Callback when exiting search mode (optional)
 */
@Composable
fun BottomSheetContent(
    state: BottomSheetState,
    fullEntryKey: Int,
    searchViewModel: SearchViewModel,
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
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

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
                    }
                )
            } else {
                QuickActionsSection()

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Recent Activities",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp))

                repeat(4) { index ->
                    ActivityItem(
                        title = "Activity ${index + 1}",
                        description = "Example description for activity ${index + 1}.")
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
                    OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(category, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/** Search bar that triggers full mode when tapped, includes Clear (X). */
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
            modifier.fillMaxWidth().focusRequester(focusRequester).onFocusChanged { focusState ->
                if (!isFull && focusState.isFocused) onTap()
            },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearchAction() }),
    )
}

/** Results list + empty state. */
@Composable
fun SearchResultsList(
    items: List<com.swent.mapin.model.event.Event>,
    showEmpty: Boolean,
    onClear: () -> Unit
) {
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
                if (e.locationName.isNotBlank()) {
                    Text(e.locationName, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f))
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/** Row of quick action buttons (Create Memory, Create Event, Filters). */
@Composable
private fun QuickActionsSection(modifier: Modifier = Modifier) {
  val focusManager = LocalFocusManager.current
  Column(modifier = modifier.fillMaxWidth()) {
    Text(
        text = "Quick Actions",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 16.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      QuickActionButton(
          text = "Create Memory",
          modifier = Modifier.weight(1f),
          onClick = { focusManager.clearFocus() })
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
