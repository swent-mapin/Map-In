package com.swent.mapin.navigation

import android.net.Uri
import android.util.Log

object DeepLinkHandler {

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
