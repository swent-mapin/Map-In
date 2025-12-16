package com.swent.mapin.navigation

import android.net.Uri
import androidx.navigation.NavHostController

internal fun navigateToExistingConversation(
    navController: NavHostController,
    conversationId: String,
    conversationName: String
) {
    val encodedName = Uri.encode(conversationName)

    navController.navigate("conversation/$conversationId/$encodedName") {
        popUpTo(Route.Chat.route) { inclusive = true }
        launchSingleTop = true
    }
}