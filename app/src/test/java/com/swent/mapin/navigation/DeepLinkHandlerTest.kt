package com.swent.mapin.navigation

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for DeepLinkHandler.
 *
 * Tests cover all supported deep link formats and edge cases.
 *
 * Assisted by AI
 */
@RunWith(AndroidJUnit4::class)
class DeepLinkHandlerTest {

  @Test
  fun parseDeepLink_friendRequest_returnsCorrectRoute() {
    val deepLink = "mapin://friendRequests/request123"
    val route = DeepLinkHandler.parseDeepLink(deepLink)

    assertEquals("friends?tab=REQUESTS", route)
  }

  @Test
  fun parseDeepLink_friendRequestWithDifferentId_returnsCorrectRoute() {
    val deepLink = "mapin://friendRequests/abc-xyz-789"
    val route = DeepLinkHandler.parseDeepLink(deepLink)

    assertEquals("friends?tab=REQUESTS", route)
  }

  @Test
  fun parseDeepLink_event_returnsMapRoute() {
    val deepLink = "mapin://events/event456"
    val route = DeepLinkHandler.parseDeepLink(deepLink)

    assertEquals(Route.Map.route, route)
  }

  @Test
  fun parseDeepLink_eventWithDifferentId_returnsMapRoute() {
    val deepLink = "mapin://events/test-event-id"
    val route = DeepLinkHandler.parseDeepLink(deepLink)

    assertEquals(Route.Map.route, route)
  }

  @Test
  fun parseDeepLink_messageWithConversationId_returnsConversationRoute() {
    val deepLink = "mapin://messages/conv789"
    val route = DeepLinkHandler.parseDeepLink(deepLink)

    assertEquals("conversation/conv789/${Uri.encode("")}", route)
  }

  @Test
  fun parseDeepLink_messageWithComplexId_returnsConversationRoute() {
    val deepLink = "mapin://messages/conv-abc-123-xyz"
    val route = DeepLinkHandler.parseDeepLink(deepLink)

    assertEquals("conversation/conv-abc-123-xyz/${Uri.encode("")}", route)
  }

  @Test
  fun parseDeepLink_messageWithoutConversationId_returnsChatRoute() {
    val deepLink = "mapin://messages"
    val route = DeepLinkHandler.parseDeepLink(deepLink)

    assertEquals(Route.Chat.route, route)
  }

  @Test
  fun parseDeepLink_map_returnsMapRoute() {
    val deepLink = "mapin://map"
    val route = DeepLinkHandler.parseDeepLink(deepLink)

    assertEquals(Route.Map.route, route)
  }

  @Test
  fun parseDeepLink_mapWithQueryParameter_returnsMapRoute() {
    val deepLink = "mapin://map?focus=friend123"
    val route = DeepLinkHandler.parseDeepLink(deepLink)

    assertEquals(Route.Map.route, route)
  }

  @Test
  fun parseDeepLink_mapWithMultipleQueryParameters_returnsMapRoute() {
    val deepLink = "mapin://map?focus=friend123&zoom=15"
    val route = DeepLinkHandler.parseDeepLink(deepLink)

    assertEquals(Route.Map.route, route)
  }

  @Test
  fun parseDeepLink_invalidScheme_returnsNull() {
    val deepLink = "https://example.com/path"
    val route = DeepLinkHandler.parseDeepLink(deepLink)

    assertNull(route)
  }

  @Test
  fun parseDeepLink_httpScheme_returnsNull() {
    val deepLink = "http://mapin.com/friendRequests/123"
    val route = DeepLinkHandler.parseDeepLink(deepLink)

    assertNull(route)
  }

  @Test
  fun parseDeepLink_unknownHost_returnsNull() {
    val deepLink = "mapin://unknown/path"
    val route = DeepLinkHandler.parseDeepLink(deepLink)

    assertNull(route)
  }

  @Test
  fun parseDeepLink_randomHost_returnsNull() {
    val deepLink = "mapin://randomHost/some/path"
    val route = DeepLinkHandler.parseDeepLink(deepLink)

    assertNull(route)
  }

  @Test
  fun parseDeepLink_malformedUrl_returnsNull() {
    val deepLink = "not a valid url"
    val route = DeepLinkHandler.parseDeepLink(deepLink)

    assertNull(route)
  }

  @Test
  fun parseDeepLink_emptyString_returnsNull() {
    val deepLink = ""
    val route = DeepLinkHandler.parseDeepLink(deepLink)

    assertNull(route)
  }

  @Test
  fun parseDeepLink_onlyScheme_returnsNull() {
    val deepLink = "mapin://"
    val route = DeepLinkHandler.parseDeepLink(deepLink)

    assertNull(route)
  }

  @Test
  fun parseDeepLink_specialCharactersInPath_handlesCorrectly() {
    val deepLink = "mapin://messages/conv%20with%20spaces"
    val route = DeepLinkHandler.parseDeepLink(deepLink)

    assertNotNull(route)
    assertTrue(route!!.startsWith("conversation/"))
  }

  @Test
  fun extractMetadata_friendRequest_returnsRequestId() {
    val deepLink = "mapin://friendRequests/request123"
    val metadata = DeepLinkHandler.extractMetadata(deepLink)

    assertEquals("request123", metadata["requestId"])
  }

  @Test
  fun extractMetadata_event_returnsEventId() {
    val deepLink = "mapin://events/event456"
    val metadata = DeepLinkHandler.extractMetadata(deepLink)

    assertEquals("event456", metadata["eventId"])
  }

  @Test
  fun extractMetadata_message_returnsConversationId() {
    val deepLink = "mapin://messages/conv789"
    val metadata = DeepLinkHandler.extractMetadata(deepLink)

    assertEquals("conv789", metadata["conversationId"])
  }

  @Test
  fun extractMetadata_mapWithFocus_returnsFocusParameter() {
    val deepLink = "mapin://map?focus=friend123"
    val metadata = DeepLinkHandler.extractMetadata(deepLink)

    assertEquals("friend123", metadata["focus"])
  }

  @Test
  fun extractMetadata_multipleQueryParameters_returnsAllParameters() {
    val deepLink = "mapin://map?focus=friend123&zoom=15&mode=dark"
    val metadata = DeepLinkHandler.extractMetadata(deepLink)

    assertEquals("friend123", metadata["focus"])
    assertEquals("15", metadata["zoom"])
    assertEquals("dark", metadata["mode"])
  }

  @Test
  fun extractMetadata_noParameters_returnsEmptyMap() {
    val deepLink = "mapin://map"
    val metadata = DeepLinkHandler.extractMetadata(deepLink)

    assertTrue(metadata.isEmpty())
  }

  @Test
  fun extractMetadata_malformedUrl_returnsEmptyMap() {
    val deepLink = "not a valid url"
    val metadata = DeepLinkHandler.extractMetadata(deepLink)

    assertTrue(metadata.isEmpty())
  }

  @Test
  fun extractMetadata_emptyString_returnsEmptyMap() {
    val deepLink = ""
    val metadata = DeepLinkHandler.extractMetadata(deepLink)

    assertTrue(metadata.isEmpty())
  }
}
