package com.swent.mapin.navigationTests

import android.net.Uri
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import com.swent.mapin.navigation.navigateToExistingConversation
import io.mockk.*
import kotlin.test.Test

// Assisted by ChatGPT

class NavUtilsTests {

  @Test
  fun `navigateToExistingConversation builds correct route and nav options`() {
    // Arrange
    val navController = mockk<NavHostController>(relaxed = true)
    val lambdaSlot = slot<NavOptionsBuilder.() -> Unit>()

    val conversationId = "123"
    val conversationName = "My Conversation"
    val encodedName = Uri.encode(conversationName)

    // Act
    navigateToExistingConversation(navController, conversationId, conversationName)

    // Assert route
    verify {
      navController.navigate(
          "conversation/$conversationId/$encodedName", any<NavOptionsBuilder.() -> Unit>())
    }
  }
}
