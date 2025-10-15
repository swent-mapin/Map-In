package com.swent.mapin.ui.map

import androidx.compose.foundation.ScrollState
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.swent.mapin.ui.components.AddEventPopUp

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
 * - (Temporary) Recent activities
 * - (Temporary) Discover section
 *
 * @param state Current bottom sheet state
 * @param fullEntryKey Increments each time we enter full mode - triggers scroll reset
 * @param searchBarState search bar state and callbacks
 */
@Composable
fun BottomSheetContent(state: BottomSheetState, fullEntryKey: Int, searchBarState: SearchBarState) {
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
        if (isFull) Modifier.fillMaxWidth().verticalScroll(scrollState) else Modifier.fillMaxWidth()

    Column(modifier = contentModifier) {
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
private fun QuickActionsSection(modifier: Modifier = Modifier) {
  val focusManager = LocalFocusManager.current
  val showDialog = remember { mutableStateOf(false) }
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
        modifier = Modifier,
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
