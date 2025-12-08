package com.swent.mapin.ui.map.bottomsheet.components

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.swent.mapin.ui.map.search.RecentItem

/** Full page showing all recent items with clear all button. */
@VisibleForTesting
@Composable
fun AllRecentItemsPage(
    recentItems: List<RecentItem>,
    onRecentSearchClick: (String) -> Unit,
    onRecentEventClick: (String) -> Unit,
    onRecentProfileClick: (String) -> Unit = {},
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
          is RecentItem.ClickedProfile -> {
            RecentProfileItem(
                userName = item.userName, onClick = { onRecentProfileClick(item.userId) })
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

/** Recent items section with list of recent searches and events, plus Show All button. */
@Composable
fun RecentItemsSection(
    recentItems: List<RecentItem>,
    onRecentSearchClick: (String) -> Unit,
    onRecentEventClick: (String) -> Unit,
    onRecentProfileClick: (String) -> Unit = {},
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
        is RecentItem.ClickedProfile -> {
          RecentProfileItem(
              userName = item.userName, onClick = { onRecentProfileClick(item.userId) })
        }
      }
    }
  }
}

/** Individual recent search item with search icon and clickable text. */
@Composable
fun RecentSearchItem(searchQuery: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
fun RecentEventItem(eventTitle: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
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

/** Individual recent profile item with person icon and clickable text. */
@Composable
fun RecentProfileItem(userName: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
  Row(
      modifier =
          modifier
              .fillMaxWidth()
              .clickable { onClick() }
              .padding(horizontal = 16.dp, vertical = 12.dp)
              .testTag("recentProfileItem_$userName"),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = "Recent profile",
            tint = MaterialTheme.colorScheme.secondary)

        Text(
            text = userName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f))
      }
}

/** Top categories section with list of popular event categories for quick search. */
@Composable
fun TopCategoriesSection(
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
fun TopCategoryItem(categoryName: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
