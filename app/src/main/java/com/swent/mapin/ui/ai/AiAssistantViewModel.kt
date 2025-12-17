package com.swent.mapin.ui.ai

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.swent.mapin.model.ai.*
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepository
import com.swent.mapin.model.event.EventRepositoryProvider
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

// Assisted by Claude Sonnet 4.5

/**
 * Represents a recommended event with full details for display.
 *
 * @property event The full event data
 * @property reason The AI-generated reason for recommendation
 */
data class RecommendedEventWithDetails(val event: Event, val reason: String)

/** Represents the current state of the AI assistant. */
sealed class AiAssistantState {
  object Idle : AiAssistantState()

  object Listening : AiAssistantState()

  object Processing : AiAssistantState()

  object Speaking : AiAssistantState()

  data class Error(val message: String) : AiAssistantState()
}

/** Represents a message in the conversation. */
data class ConversationMessage(
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * ViewModel for the AI Assistant screen.
 *
 * Orchestrates the complete voice interaction flow:
 * - Speech-to-text for user input
 * - AI processing for event recommendations
 * - Text-to-speech for AI responses
 * - Event joining functionality
 *
 * @property context Android context for speech services
 * @property eventRepository Repository for event data
 * @property orchestrator AI assistant orchestrator
 * @property speechToTextService Speech recognition service
 * @property textToSpeechService Text-to-speech service
 */
class AiAssistantViewModel(
    private val context: Context,
    private val eventRepository: EventRepository = EventRepositoryProvider.getRepository(),
    private val orchestrator: AiAssistantOrchestrator = createOrchestrator(eventRepository),
    private val speechToTextService: SpeechToTextService =
        AiConfig.createSpeechToTextService(context),
    private val textToSpeechService: TextToSpeechService =
        AiConfig.createTextToSpeechService(context)
) : ViewModel() {

  companion object {
    private const val TAG = "AiAssistantViewModel"

    private fun createOrchestrator(eventRepository: EventRepository): AiAssistantOrchestrator {
      val aiRepository =
          AiConfig.provideRepository(okHttpClient = OkHttpClient(), gson = Gson(), baseUrl = "")
      return AiConfig.createOrchestrator(aiRepository, eventRepository)
    }

    /**
     * Factory for creating AiAssistantViewModel with proper lifecycle management. Uses
     * applicationContext to avoid memory leaks.
     */
    fun provideFactory(context: Context): ViewModelProvider.Factory {
      return object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          if (modelClass.isAssignableFrom(AiAssistantViewModel::class.java)) {
            return AiAssistantViewModel(context.applicationContext) as T
          }
          throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
      }
    }
  }

  private val _state = MutableStateFlow<AiAssistantState>(AiAssistantState.Idle)
  val state: StateFlow<AiAssistantState> = _state.asStateFlow()

  private val _conversationMessages = MutableStateFlow<List<ConversationMessage>>(emptyList())
  val conversationMessages: StateFlow<List<ConversationMessage>> =
      _conversationMessages.asStateFlow()

  private val _recommendedEvents = MutableStateFlow<List<RecommendedEventWithDetails>>(emptyList())
  val recommendedEvents: StateFlow<List<RecommendedEventWithDetails>> =
      _recommendedEvents.asStateFlow()

  private val _followupQuestions = MutableStateFlow<List<String>>(emptyList())
  val followupQuestions: StateFlow<List<String>> = _followupQuestions.asStateFlow()

  private var lastAiResult: AiAssistantResult? = null

  /**
   * Toggles the listening state. If currently idle, starts listening for speech. If currently
   * listening, stops listening.
   */
  fun toggleListening() {
    when (_state.value) {
      is AiAssistantState.Idle,
      is AiAssistantState.Error -> startListening()
      is AiAssistantState.Listening -> stopListening()
      is AiAssistantState.Speaking -> {
        textToSpeechService.stop()
        _state.value = AiAssistantState.Idle
      }
      else -> {
        /* Processing, don't interrupt */
      }
    }
  }

  /** Starts listening for speech input. */
  private fun startListening() {
    _state.value = AiAssistantState.Listening
    Log.d(TAG, "Starting speech recognition")

    speechToTextService.startListening(
        onResult = { transcribedText ->
          Log.d(TAG, "Speech recognized: $transcribedText")
          handleUserQuery(transcribedText)
        },
        onError = { errorMessage ->
          Log.e(TAG, "Speech recognition error: $errorMessage")
          _state.value = AiAssistantState.Error(errorMessage)
        })
  }

  /** Stops listening for speech input. */
  private fun stopListening() {
    speechToTextService.stopListening()
    _state.value = AiAssistantState.Idle
  }

  /**
   * Handles a user query (from speech or text input).
   *
   * This method:
   * 1. Adds the query to conversation
   * 2. Checks if it's a "join event" intent
   * 3. Otherwise processes as a recommendation request
   */
  fun handleUserQuery(query: String) {
    if (query.isBlank()) {
      _state.value = AiAssistantState.Idle
      return
    }

    // Add user message to conversation
    _conversationMessages.value =
        _conversationMessages.value + ConversationMessage(text = query, isFromUser = true)

    _state.value = AiAssistantState.Processing

    viewModelScope.launch {
      try {
        // Check if this is a "join event" intent
        val joinIntent = parseJoinIntent(query)
        if (joinIntent != null && lastAiResult != null) {
          handleJoinEvent(joinIntent)
        } else {
          // Process as recommendation request
          processRecommendationQuery(query)
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error processing query", e)
        val errorMessage = "Sorry, an error occurred. Please try again."
        addAiMessage(errorMessage)
        speakResponse(errorMessage)
        _state.value = AiAssistantState.Error(e.message ?: "Unknown error")
      }
    }
  }

  /**
   * Parses user query to detect "join event" intent. Returns the event index (0-based) if detected,
   * null otherwise.
   */
  private fun parseJoinIntent(query: String): Int? {
    val lowerQuery = query.lowercase()

    // Check for join keywords
    val joinKeywords =
        listOf(
            "join",
            "register",
            "sign up",
            "book",
            "reserve",
            "participate",
            "go to",
            "attend",
            "rejoindre",
            "participer",
            "inscrire")
    val hasJoinIntent = joinKeywords.any { lowerQuery.contains(it) }

    if (!hasJoinIntent) return null

    // Helper function to check for word boundary matches
    fun matchesWord(pattern: String): Boolean {
      return Regex("\\b$pattern\\b", RegexOption.IGNORE_CASE).containsMatchIn(lowerQuery)
    }

    // Check for explicit "join #N" or "event #N" patterns first
    fun matchesEventNumber(number: Int): Boolean {
      val patterns =
          listOf(
              "#$number",
              "event $number",
              "événement $number",
              "number $number",
              "numéro $number",
              "option $number")
      return patterns.any { lowerQuery.contains(it) }
    }

    // Try to find event index
    return when {
      matchesWord("first") ||
          matchesWord("premier") ||
          matchesWord("première") ||
          matchesEventNumber(1) -> 0
      matchesWord("second") || matchesWord("deuxième") || matchesEventNumber(2) -> 1
      matchesWord("third") || matchesWord("troisième") || matchesEventNumber(3) -> 2
      // If only one event, assume they want that one
      _recommendedEvents.value.size == 1 -> 0
      else -> null
    }
  }

  /** Handles the "join event" flow. */
  private suspend fun handleJoinEvent(eventIndex: Int) {
    val events = _recommendedEvents.value

    if (eventIndex >= events.size) {
      val message =
          "I couldn't find that event. Could you please specify which event you'd like to join?"
      addAiMessage(message)
      speakResponse(message)
      _state.value = AiAssistantState.Idle
      return
    }

    val eventToJoin = events[eventIndex]
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    if (userId == null) {
      val message = "You need to be logged in to join an event."
      addAiMessage(message)
      speakResponse(message)
      _state.value = AiAssistantState.Idle
      return
    }

    try {
      orchestrator.joinRecommendedEventByIndex(eventIndex, userId)
      val message = "Great! I've registered you for ${eventToJoin.event.title}. You're all set!"
      addAiMessage(message)
      speakResponse(message)
    } catch (e: Exception) {
      Log.e(TAG, "Error joining event", e)
      val message =
          "Sorry, I couldn't complete the registration. Please try again or register directly from the event page."
      addAiMessage(message)
      speakResponse(message)
    }
  }

  /** Processes a recommendation query through the AI pipeline. */
  private suspend fun processRecommendationQuery(query: String) {
    Log.d(TAG, "Processing recommendation query: $query")

    val userId = FirebaseAuth.getInstance().currentUser?.uid

    // Get the device language (e.g., "en", "fr")
    val deviceLanguage = Locale.getDefault().language
    // Call the orchestrator
    // Call the orchestrator with the device language
    val result =
        orchestrator.processQuery(
            userQuery = query, userId = userId, responseLanguage = deviceLanguage)
    lastAiResult = result
    Log.d(TAG, "Received ${result.recommendedEvents.size} recommendations")

    // Fetch full event details for recommended events
    val eventsWithDetails =
        result.recommendedEvents.mapNotNull { recommendedEvent ->
          try {
            val fullEvent = eventRepository.getEvent(recommendedEvent.id)
            RecommendedEventWithDetails(event = fullEvent, reason = recommendedEvent.reason)
          } catch (e: Exception) {
            Log.w(TAG, "Could not fetch event ${recommendedEvent.id}", e)
            null
          }
        }

    _recommendedEvents.value = eventsWithDetails
    _followupQuestions.value = result.followupQuestions ?: emptyList()

    // Add AI response to conversation and speak it
    addAiMessage(result.assistantMessage)
    speakResponse(result.assistantMessage)
  }

  /** Adds an AI message to the conversation. */
  private fun addAiMessage(text: String) {
    _conversationMessages.value =
        _conversationMessages.value + ConversationMessage(text = text, isFromUser = false)
  }

  /** Speaks the response using text-to-speech. */
  private fun speakResponse(text: String) {
    _state.value = AiAssistantState.Speaking

    textToSpeechService.speak(text) { _state.value = AiAssistantState.Idle }
  }

  /** Handles selection of a follow-up question. */
  fun selectFollowupQuestion(question: String) {
    handleUserQuery(question)
  }

  /** Resets the conversation. */
  fun resetConversation() {
    textToSpeechService.stop()
    _conversationMessages.value = emptyList()
    _recommendedEvents.value = emptyList()
    _followupQuestions.value = emptyList()
    lastAiResult = null
    orchestrator.resetConversation()
    _state.value = AiAssistantState.Idle
  }

  /** Joins an event by its ID. */
  fun joinEvent(eventId: String) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

    viewModelScope.launch {
      try {
        eventRepository.editEventAsUser(eventId, userId, join = true)

        val eventName =
            _recommendedEvents.value.find { it.event.uid == eventId }?.event?.title ?: "the event"

        val message = "You've successfully joined $eventName!"
        addAiMessage(message)
        speakResponse(message)
      } catch (e: Exception) {
        Log.e(TAG, "Error joining event $eventId", e)
        val message = "Sorry, I couldn't complete the registration. Please try again."
        addAiMessage(message)
        speakResponse(message)
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    speechToTextService.destroy()
    textToSpeechService.shutdown()
  }
}
