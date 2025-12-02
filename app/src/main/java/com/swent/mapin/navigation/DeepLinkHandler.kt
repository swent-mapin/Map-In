package com.swent.mapin.navigation

import android.net.Uri
import android.util.Log

/**
 * Utility object for handling deep link navigation.
 *
 * This object provides functions to parse and convert deep link URLs from notifications into
 * navigation routes that can be used by the app's navigation system.
 */
object DeepLinkHandler {

  /**
   * Parse a deep link URL and convert it to a navigation route.
   *
   * Supported deep link formats:
   * - mapin://friendRequests/{requestId} -> friends screen
   * - mapin://events/{eventId} -> map screen (event details not implemented yet)
   * - mapin://messages/{conversationId} -> conversation screen
   * - mapin://messages -> chat list screen
   * - mapin://map?focus={friendId} -> map screen with focus
   *
   * @param deepLink The deep link URL from the notification
   * @return The corresponding navigation route, or null if the deep link is invalid
   */
  fun parseDeepLink(deepLink: String): String? {
    return try {
      val uri = Uri.parse(deepLink)

      // Check if it's a valid mapin:// scheme
      if (uri.scheme != "mapin") {
        Log.w("DeepLinkHandler", "Invalid scheme: ${uri.scheme}")
        return null
      }

      val route =
          when (uri.host) {
            "friendRequests" -> "friends?tab=REQUESTS"
            "events" -> Route.Map.route
            "messages" -> {
              val conversationId = uri.pathSegments.firstOrNull()
              if (conversationId != null) {
                "conversation/$conversationId/${Uri.encode("")}"
              } else {
                Route.Chat.route
              }
            }
            "map" -> Route.Map.route
            else -> {
              Log.w("DeepLinkHandler", "Unknown host: ${uri.host}")
              null
            }
          }

      Log.d("DeepLinkHandler", "Parsed: $deepLink -> $route")
      route
    } catch (e: Exception) {
      Log.e("DeepLinkHandler", "Parse error: $deepLink", e)
      null
    }
  }

  /**
   * Extract metadata from a deep link URL.
   *
   * @param deepLink The deep link URL
   * @return A map of metadata (e.g., requestId, eventId, conversationId, focus parameter)
   */
  fun extractMetadata(deepLink: String): Map<String, String> {
    return try {
      val uri = Uri.parse(deepLink)
      val metadata = mutableMapOf<String, String>()

      // Extract path segments
      uri.pathSegments.firstOrNull()?.let { id ->
        when (uri.host) {
          "friendRequests" -> metadata["requestId"] = id
          "events" -> metadata["eventId"] = id
          "messages" -> metadata["conversationId"] = id
        }
      }

      // Extract query parameters
      uri.queryParameterNames.forEach { paramName ->
        uri.getQueryParameter(paramName)?.let { paramValue -> metadata[paramName] = paramValue }
      }

      metadata
    } catch (e: Exception) {
      Log.e("DeepLinkHandler", "Failed to extract metadata from deep link: $deepLink", e)
      emptyMap()
    }
  }
}
