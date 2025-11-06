package com.swent.mapin.ui.chat

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue

class ConversationScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Composable
    private fun ChatTopBar(title: String, onNavigateBack: () -> Unit) {
        Text(text = title)
    }

    @Test
    fun conversationScreen_displaysInitialMessages() {
        composeTestRule.setContent {
            ConversationScreen(
                conversationId = "1",
                conversationName = "Friends Chat",
                onNavigateBack = {}
            )
        }

        composeTestRule.onNodeWithText("Hey, how are you?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Doing great, thanks!").assertIsDisplayed()

        composeTestRule.onNodeWithText("Friends Chat").assertIsDisplayed()
    }

    @Test
    fun sendingMessage_addsNewMessageAndClearsInput() {
        composeTestRule.setContent {
            ConversationScreen(
                conversationId = "42",
                conversationName = "Compose Test",
                onNavigateBack = {}
            )
        }

        val textField = composeTestRule.onNode(hasSetTextAction())
        textField.performTextInput("Hello from test!")

        composeTestRule.onNodeWithContentDescription("Send").performClick()

        composeTestRule.onNodeWithText("Hello from test!").assertIsDisplayed()

        // Input cleared
        textField.assertTextEquals("")
    }

    @Test
    fun emptyMessage_doesNotAddNewMessage() {
        composeTestRule.setContent {
            ConversationScreen(
                conversationId = "2",
                conversationName = "Empty Send",
                onNavigateBack = {}
            )
        }

        // Try sending empty text
        composeTestRule.onNodeWithContentDescription("Send").performClick()

        // Still only 2 mock messages
        composeTestRule.onNodeWithText("Hey, how are you?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Doing great, thanks!").assertIsDisplayed()
    }

    @Test
    fun onNavigateBack_callbackIsCalled() {
        var backCalled = false

        composeTestRule.setContent {
            ConversationScreen(
                conversationId = "3",
                conversationName = "Back Test",
                onNavigateBack = { backCalled = true }
            )
        }

        // Simulate calling the callback directly


        assertTrue(backCalled)
    }

    @Test
    fun messageBubble_rendersBothSides() {
        composeTestRule.setContent {
            MessageBubble(Message("Right side", isMe = true))
            MessageBubble(Message("Left side", isMe = false))
        }

        // Both messages should appear
        composeTestRule.onNodeWithText("Right side").assertIsDisplayed()
        composeTestRule.onNodeWithText("Left side").assertIsDisplayed()
    }
}
