package com.swent.mapin.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

// Data class for messages
data class Message(val text: String, val isMe: Boolean)

/**
 * Assisted by AI Functions representing the UI for conversations between users
 *
 * @param conversationId The ID of the conversation
 * @param conversationName The name of the conversation or group chat
 * @param onNavigateBack Callback invoked when pressing the back button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    conversationId: String,
    conversationName: String,
    onNavigateBack: () -> Unit = {}
) {
  var messageText by remember { mutableStateOf(TextFieldValue("")) }

  // Mock conversation data
  var messages by remember {
    mutableStateOf(
        listOf(
            Message("Hey, how are you?", isMe = false),
            Message("Doing great, thanks!", isMe = true)))
  }

  Scaffold(
      topBar = { ChatTopBar(conversationName, onNavigateBack = onNavigateBack) },
      bottomBar = {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .imePadding() // ensures it moves above the keyboard
                    .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
              TextField(
                  value = messageText,
                  onValueChange = { messageText = it },
                  modifier = Modifier.weight(1f),
                  placeholder = { Text("Type a message...") },
                  singleLine = true,
                  shape = MaterialTheme.shapes.large)
              IconButton(
                  onClick = {
                    if (messageText.text.isNotBlank()) {
                      messages = messages + Message(messageText.text, isMe = true)
                      messageText = TextFieldValue("")
                    }
                  }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                  }
            }
      }) { padding ->
        // Message list
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            reverseLayout = true // new messages appear at bottom
            ) {
              items(messages.reversed()) { message -> MessageBubble(message) }
            }
      }
}

/**
 * Assisted by AI Basic UI for messages as bubbles
 *
 * @param message The message sent or was sent
 */
@Composable
fun MessageBubble(message: Message) {
  val horizontalArrangement = if (message.isMe) Arrangement.End else Arrangement.Start

  val bubbleColor =
      if (message.isMe) MaterialTheme.colorScheme.primaryContainer
      else MaterialTheme.colorScheme.surfaceVariant

  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = horizontalArrangement) {
    Surface(color = bubbleColor, shape = MaterialTheme.shapes.medium, tonalElevation = 2.dp) {
      Text(
          text = message.text,
          modifier = Modifier.padding(12.dp),
          style = MaterialTheme.typography.bodyLarge)
    }
  }
}
