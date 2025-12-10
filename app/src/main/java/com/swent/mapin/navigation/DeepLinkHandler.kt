package com.swent.mapin.navigation

import android.net.Uri
import android.util.Log

/**
 * Handler for parsing and processing deep links in the application.
 *
 * This object handles incoming deep links with the "mapin://" scheme and converts them into
 * navigation routes that can be used by the app's navigation system. Deep links are used for push
 * notifications and external links to navigate users to specific screens within the app.
 *
 * Supported deep link formats:
 * - mapin://friendRequests -> Friends screen (Requests tab)
 * - mapin://friendAccepted -> Friends screen (Friends tab)
 * - mapin://profile -> Friends screen (Friends tab)
 * - mapin://events -> Map screen
 * - mapin://messages -> Chat screen
 * - mapin://messages/{conversationId} -> Specific conversation screen
 * - mapin://map -> Map screen
 */
object DeepLinkHandler {

  /**
   * Parses a deep link string and returns the corresponding navigation route.
   *
   * @param deepLink The deep link URL to parse (e.g., "mapin://friendRequests")
   * @return The navigation route string if the deep link is valid, null otherwise
   */
  fun parseDeepLink(deepLink: String): String? {
    return try {
      val uri = Uri.parse(deepLink)

      if (uri.scheme != "mapin") {
        Log.w("DeepLinkHandler", "Invalid scheme: ${uri.scheme}")
        return null
      }

      when (uri.host) {
        "friendRequests" -> "friends?tab=REQUESTS"
        "friendAccepted",
        "profile" -> "friends?tab=FRIENDS"
        "events" -> Route.Map.route
        "messages" -> {
          val conversationId = uri.pathSegments.firstOrNull()
          if (conversationId != null) {
            "conversation/$conversationId/${Uri.encode("Unknown")}"
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
    } catch (e: Exception) {
      Log.e("DeepLinkHandler", "Parse error: $deepLink", e)
      null
    }
  }
}
