package com.swent.mapin.ui.chat

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.swent.mapin.navigation.Route

sealed class ChatTab(
    val name: String,
    val icon: ImageVector,
    val destination: Route,
    val testTag: String
) {
  object Chats :
      ChatTab("Chats", Icons.Outlined.ChatBubbleOutline, Route.Map, ChatScreenTestTags.CHAT_SCREEN)
}

private val tabs = listOf(ChatTab.Chats)

@Composable
fun ChatBottomBar(
    selectedTab: ChatTab,
    onTabSelected: (ChatTab) -> Unit,
    modifier: Modifier = Modifier
) {
  NavigationBar(modifier = modifier) {
    tabs.forEach { tab ->
      NavigationBarItem(
          icon = { Icon(tab.icon, contentDescription = null) },
          label = { Text(tab.name) },
          selected = tab == selectedTab,
          onClick = { onTabSelected(tab) },
          modifier = Modifier.clip(RoundedCornerShape(50.dp)).testTag(tab.testTag))
    }
  }
}
