package com.swent.mapin.ui.map.bottomsheet.components

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.swent.mapin.model.UserProfile
import com.swent.mapin.model.event.Event
import com.swent.mapin.ui.map.BottomSheetState
import com.swent.mapin.ui.map.search.RecentItem

@Composable
fun SearchResultsSection(
    modifier: Modifier = Modifier,
    results: List<Event>,
    query: String,
    recentItems: List<RecentItem> = emptyList(),
    onRecentSearchClick: (String) -> Unit = {},
    onRecentEventClick: (String) -> Unit = {},
    onShowAllRecents: () -> Unit = {},
    topCategories: List<String> = emptyList(),
    onCategoryClick: (String) -> Unit = {},
    onEventClick: (Event) -> Unit = {},
    sheetState: BottomSheetState = BottomSheetState.FULL,
    userResults: List<UserProfile> = emptyList(),
    onUserClick: (String) -> Unit = {}
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

  // When there's a query but no results (events and users)
  if (results.isEmpty() && userResults.isEmpty()) {
    NoResultsMessage(query = query, modifier = modifier)
    return
  }

  // Show search results
  LazyColumn(modifier = modifier.fillMaxWidth()) {
    // Show people section in FULL state only (while typing)
    if (sheetState == BottomSheetState.FULL && userResults.isNotEmpty()) {
      item {
        PeopleResultsSection(users = userResults, onUserClick = onUserClick)
        Spacer(modifier = Modifier.height(16.dp))
      }
    }

    items(results) { event -> SearchResultItem(event = event, onClick = { onEventClick(event) }) }

    item { Spacer(modifier = Modifier.height(8.dp)) }
  }
}

@Composable
fun SearchResultItem(event: Event, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {

  Column(
      modifier =
          modifier.fillMaxWidth().clickable { onClick() }.testTag("eventItem_${event.uid}")) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(top = 0.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text(
                    text = event.title,
                    modifier = Modifier.height(24.dp),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
              }

              if (event.location.name.isNotBlank()) {
                Text(
                    text = event.location.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
              }
            }
      }
}

/**
 * Section displaying people/user search results.
 *
 * @param users List of user profiles to display (max 3 recommended)
 * @param onUserClick Callback when a user is clicked, passes userId
 * @param modifier Modifier for the section
 */
@Composable
fun PeopleResultsSection(
    users: List<UserProfile>,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
  if (users.isEmpty()) return

  Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 12.dp)) {
          Icon(
              imageVector = Icons.Default.Person,
              contentDescription = null,
              modifier = Modifier.size(20.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
              text = "People (${users.size})",
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      users.forEach { user ->
        UserSearchCard(
            user = user, onClick = { onUserClick(user.userId) }, modifier = Modifier.weight(1f))
      }
      // Fill remaining space if less than 3 users
      repeat(3 - users.size) { Spacer(modifier = Modifier.weight(1f)) }
    }
  }
}

/** Compact user card for search results. */
@Composable
private fun UserSearchCard(user: UserProfile, onClick: () -> Unit, modifier: Modifier = Modifier) {
  Surface(
      modifier = modifier.clickable { onClick() }.testTag("userSearchCard_${user.userId}"),
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp)) {
              // Avatar
              Box(
                  modifier = Modifier.size(48.dp).clip(RoundedCornerShape(24.dp)),
                  contentAlignment = Alignment.Center) {
                    if (user.avatarUrl != null &&
                        (user.avatarUrl.startsWith("http") ||
                            user.avatarUrl.startsWith("content"))) {
                      AsyncImage(
                          model = user.avatarUrl,
                          contentDescription = "Profile picture of ${user.name}",
                          contentScale = ContentScale.Crop,
                          modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)))
                    } else {
                      Surface(
                          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                          shape = RoundedCornerShape(24.dp),
                          modifier = Modifier.fillMaxSize()) {
                            Box(contentAlignment = Alignment.Center) {
                              Icon(
                                  imageVector = getAvatarIcon(user.avatarUrl),
                                  contentDescription = "Avatar",
                                  modifier = Modifier.size(28.dp),
                                  tint = MaterialTheme.colorScheme.primary)
                            }
                          }
                    }
                  }

              Spacer(modifier = Modifier.height(8.dp))

              // Name
              Text(
                  text = user.name.ifBlank { "User" },
                  style = MaterialTheme.typography.bodyMedium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  textAlign = TextAlign.Center)
            }
      }
}

/** Search bar that triggers full mode when tapped. Supports avatar image or preset icons. */
@Composable
fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
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
        placeholder = { Text("Search events, people", style = MaterialTheme.typography.bodyLarge) },
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
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)))
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
fun NoResultsMessage(query: String, modifier: Modifier = Modifier) {
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

/** Get avatar icon from URL/ID, matching ProfileScreen presets. */
fun getAvatarIcon(avatarUrl: String?): ImageVector {
  if (avatarUrl.isNullOrEmpty()) return Icons.Default.Person
  return when (avatarUrl) {
    "person" -> Icons.Default.Person
    "face" -> Icons.Default.Face
    "star" -> Icons.Default.Star
    "favorite" -> Icons.Default.Favorite
    else -> Icons.Default.Person
  }
}
