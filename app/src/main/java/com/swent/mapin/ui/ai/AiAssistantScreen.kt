package com.swent.mapin.ui.ai

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Assisted by AI

/**
 * AI Assistant Screen with voice interaction.
 *
 * This screen provides a voice-based interface to:
 * - Listen to user queries (speech-to-text)
 * - Display AI recommendations
 * - Speak responses (text-to-speech)
 * - Show recommended events
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    onEventSelected: (String) -> Unit = {}
) {
  Scaffold(
      modifier = modifier.testTag("aiAssistantScreen"),
      topBar = {
        TopAppBar(
            title = { Text("AI Assistant", fontWeight = FontWeight.Bold) },
            navigationIcon = {
              IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("backButton")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
              }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer))
      }) { paddingValues ->
        AiAssistantContent(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            onEventSelected = onEventSelected)
      }
}

@Composable
private fun AiAssistantContent(modifier: Modifier = Modifier, onEventSelected: (String) -> Unit) {
  // Mock state - replace with ViewModel later
  var isListening by remember { mutableStateOf(false) }
  var isSpeaking by remember { mutableStateOf(false) }
  var transcribedText by remember { mutableStateOf("") }
  var aiResponse by remember { mutableStateOf<String?>(null) }
  var recommendedEvents by remember { mutableStateOf<List<MockEvent>>(emptyList()) }

  val coroutineScope = rememberCoroutineScope()

  Column(
      modifier = modifier.fillMaxSize().padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally) {
        // Status indicator
        StatusBanner(
            isListening = isListening, isSpeaking = isSpeaking, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(24.dp))

        // Main microphone button
        VoiceInputButton(
            isListening = isListening,
            onClick = {
              isListening = !isListening
              if (isListening) {
                // Simulate speech recognition
                transcribedText = "I'm looking for a concert tonight"
                isListening = false
                // Simulate AI response
                coroutineScope.launch {
                  delay(2000)
                  aiResponse =
                      "I found 2 concerts that might interest you tonight. The first is a jazz concert at Sunset Club at 8pm, and the second is a rock concert at Olympia at 9:30pm."
                  recommendedEvents =
                      listOf(
                          MockEvent("event1", "Jazz Concert at Sunset Club", "8:00 PM"),
                          MockEvent("event2", "Rock Concert at Olympia", "9:30 PM"))
                  isSpeaking = true
                  delay(5000)
                  isSpeaking = false
                }
              }
            },
            modifier = Modifier.testTag("microphoneButton"))

        Spacer(modifier = Modifier.height(32.dp))

        // Conversation display
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).testTag("conversationList"),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
              // User query
              if (transcribedText.isNotEmpty()) {
                item {
                  UserMessageBubble(
                      text = transcribedText, modifier = Modifier.testTag("userMessage"))
                }
              }

              // AI response
              if (aiResponse != null) {
                item {
                  AiMessageBubble(
                      text = aiResponse!!,
                      isSpeaking = isSpeaking,
                      modifier = Modifier.testTag("aiMessage"))
                }
              }

              // Recommended events
              if (recommendedEvents.isNotEmpty()) {
                item {
                  Text(
                      text = "Recommended Events:",
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(vertical = 8.dp))
                }

                items(recommendedEvents) { event ->
                  EventRecommendationCard(
                      event = event,
                      onClick = { onEventSelected(event.id) },
                      modifier = Modifier.testTag("eventCard_${event.id}"))
                }
              }

              // Help text when idle
              if (transcribedText.isEmpty()) {
                item { HelpCard(modifier = Modifier.testTag("helpCard")) }
              }
            }
      }
}

@Composable
private fun StatusBanner(isListening: Boolean, isSpeaking: Boolean, modifier: Modifier = Modifier) {
  val statusText =
      when {
        isListening -> "🎤 Listening..."
        isSpeaking -> "🔊 Assistant speaking..."
        else -> "💬 Ready to listen"
      }

  val backgroundColor =
      when {
        isListening -> MaterialTheme.colorScheme.errorContainer
        isSpeaking -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
      }

  Surface(
      modifier = modifier.fillMaxWidth().testTag("statusBanner"),
      color = backgroundColor,
      shape = RoundedCornerShape(12.dp)) {
        Text(
            text = statusText,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center)
      }
}

@Composable
private fun VoiceInputButton(
    isListening: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  // Pulse animation when listening
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val scale by
      infiniteTransition.animateFloat(
          initialValue = 1f,
          targetValue = if (isListening) 1.2f else 1f,
          animationSpec =
              infiniteRepeatable(
                  animation = tween(1000, easing = FastOutSlowInEasing),
                  repeatMode = RepeatMode.Reverse),
          label = "scale")

  Box(modifier = modifier.size(120.dp), contentAlignment = Alignment.Center) {
    // Outer ring animation
    if (isListening) {
      Box(
          modifier =
              Modifier.size(120.dp)
                  .scale(scale)
                  .background(
                      brush =
                          Brush.radialGradient(
                              colors =
                                  listOf(
                                      MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                      Color.Transparent)),
                      shape = CircleShape))
    }

    // Main button
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.size(80.dp).testTag("micButton"),
        containerColor =
            if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        elevation = FloatingActionButtonDefaults.elevation(8.dp)) {
          Icon(
              imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
              contentDescription = if (isListening) "Stop listening" else "Start listening",
              modifier = Modifier.size(40.dp),
              tint = Color.White)
        }
  }
}

@Composable
private fun UserMessageBubble(text: String, modifier: Modifier = Modifier) {
  Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
    Surface(
        modifier = Modifier.widthIn(max = 280.dp).testTag("userBubble"),
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)) {
          Text(
              text = text,
              modifier = Modifier.padding(12.dp),
              color = MaterialTheme.colorScheme.onPrimary,
              style = MaterialTheme.typography.bodyLarge)
        }
  }
}

@Composable
private fun AiMessageBubble(text: String, isSpeaking: Boolean, modifier: Modifier = Modifier) {
  Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
    Surface(
        modifier = Modifier.widthIn(max = 280.dp).testTag("aiBubble"),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
        border = if (isSpeaking) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null) {
          Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isSpeaking) {
              SpeakingIndicator(modifier = Modifier.padding(end = 8.dp))
            }
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.bodyLarge)
          }
        }
  }
}

@Composable
private fun SpeakingIndicator(modifier: Modifier = Modifier) {
  val infiniteTransition = rememberInfiniteTransition(label = "speaking")
  val alpha by
      infiniteTransition.animateFloat(
          initialValue = 0.3f,
          targetValue = 1f,
          animationSpec =
              infiniteRepeatable(
                  animation = tween(600, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
          label = "alpha")

  Icon(
      imageVector = Icons.AutoMirrored.Filled.VolumeUp,
      contentDescription = "Speaking",
      modifier = modifier.size(20.dp).testTag("speakingIndicator"),
      tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha))
}

@Composable
private fun EventRecommendationCard(
    event: MockEvent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  Card(
      modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
              // Event icon
              Surface(
                  modifier = Modifier.size(48.dp),
                  color = MaterialTheme.colorScheme.primaryContainer,
                  shape = CircleShape) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                  }

              Spacer(modifier = Modifier.width(16.dp))

              // Event info
              Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Text(
                    text = "🕐 ${event.time}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
              }

              // Arrow
              Icon(
                  imageVector = Icons.Default.ChevronRight,
                  contentDescription = "View details",
                  tint = MaterialTheme.colorScheme.primary)
            }
      }
}

@Composable
private fun HelpCard(modifier: Modifier = Modifier) {
  Card(
      modifier = modifier.fillMaxWidth(),
      colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                  imageVector = Icons.AutoMirrored.Filled.Help,
                  contentDescription = null,
                  modifier = Modifier.size(48.dp),
                  tint = MaterialTheme.colorScheme.onTertiaryContainer)

              Spacer(modifier = Modifier.height(12.dp))

              Text(
                  text = "How to use the assistant",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onTertiaryContainer)

              Spacer(modifier = Modifier.height(8.dp))

              Text(
                  text =
                      "Press the microphone and say:\n" +
                          "• \"I'm looking for a concert tonight\"\n" +
                          "• \"Find me a sports event this weekend\"\n" +
                          "• \"What festivals are coming up?\"",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onTertiaryContainer,
                  textAlign = TextAlign.Center)
            }
      }
}

// Mock data class for preview
private data class MockEvent(val id: String, val title: String, val time: String)
