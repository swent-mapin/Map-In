package com.swent.mapin.navigation

/**
 * Centralized navigation routes for the application.
 *
 * This sealed class defines all navigation destinations used throughout the app. Each route object
 * encapsulates its string identifier and provides type-safe navigation.
 *
 * ## Naming Conventions:
 * - **Simple routes**: Use camelCase (e.g., "settings", "profile")
 * - **Multi-word routes**: Use camelCase without separators (e.g., "changePassword",
 *   "newConversation")
 * - **Parameterized routes**: Use format "basePath/{paramName}" (e.g., "conversation/{id}/{name}")
 *
 * ## Adding Parameterized Routes:
 * For routes that require parameters, create a companion object with a builder function:
 * ```
 * object Conversation : Route("conversation/{conversationId}/{name}") {
 *     fun createRoute(conversationId: String, name: String) =
 *         "conversation/$conversationId/$name"
 * }
 * ```
 *
 * ## Usage:
 * ```
 * // Simple navigation
 * navController.navigate(Route.Settings.route)
 *
 * // Parameterized navigation
 * navController.navigate(Route.Conversation.createRoute(id, name))
 * ```
 */
sealed class Route(val route: String) {
  /** Authentication/Sign-in screen - Entry point for unauthenticated users */
  object Auth : Route("auth")

  /** Main map screen - Primary navigation destination showing the interactive map */
  object Map : Route("map")

  /** User profile screen - Displays and allows editing of user information */
  object Profile : Route("profile")

  /** Application settings screen - Configuration and preferences */
  object Settings : Route("settings")

  /**
   * Change password screen - Allows users to update their account password
   *
   * Navigation result: Sets "password_changed" = true in SavedStateHandle on success
   */
  object ChangePassword : Route("changePassword")

  /** Friends list screen - Displays user's friends and friend management */
  object Friends : Route("friends")

  /** Chat list screen - Shows all conversations */
  object Chat : Route("chat")

  /** New conversation screen - Create a new chat conversation */
  object NewConversation : Route("newConversation")

  /** Memories screen - Displays and allows editing of user's memories */
  object Memories : Route("memories")

  /** AI Assistant screen - Voice-based AI event recommendations */
  object AiAssistant : Route("aiassistant")
}
