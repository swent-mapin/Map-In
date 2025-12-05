package com.swent.mapin.navigationTests

import android.net.Uri
import com.swent.mapin.navigation.DeepLinkHandler
import com.swent.mapin.navigation.Route
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for DeepLinkHandler.parseDeepLink() method.
 *
 * Tests verify correct parsing of all supported URL formats and proper handling of edge cases.
 */
@RunWith(RobolectricTestRunner::class)
class DeepLinkHandlerTest {

  // ==================== Friend Requests Tests ====================

  @Test
  fun `parseDeepLink with friendRequests host returns friends tab REQUESTS route`() {
    val result = DeepLinkHandler.parseDeepLink("mapin://friendRequests")
    assertEquals("friends?tab=REQUESTS", result)
  }

  @Test
  fun `parseDeepLink with friendRequests and path returns friends tab REQUESTS route`() {
    val result = DeepLinkHandler.parseDeepLink("mapin://friendRequests/request123")
    assertEquals("friends?tab=REQUESTS", result)
  }

  // ==================== Friend Accepted Tests ====================

  @Test
  fun `parseDeepLink with friendAccepted host returns friends tab FRIENDS route`() {
    val result = DeepLinkHandler.parseDeepLink("mapin://friendAccepted")
    assertEquals("friends?tab=FRIENDS", result)
  }

  @Test
  fun `parseDeepLink with friendAccepted and path returns friends tab FRIENDS route`() {
    val result = DeepLinkHandler.parseDeepLink("mapin://friendAccepted/user456")
    assertEquals("friends?tab=FRIENDS", result)
  }

  // ==================== Profile Tests ====================

  @Test
  fun `parseDeepLink with profile host returns friends tab FRIENDS route`() {
    val result = DeepLinkHandler.parseDeepLink("mapin://profile")
    assertEquals("friends?tab=FRIENDS", result)
  }

  @Test
  fun `parseDeepLink with profile and userId returns friends tab FRIENDS route`() {
    val result = DeepLinkHandler.parseDeepLink("mapin://profile/user123")
    assertEquals("friends?tab=FRIENDS", result)
  }

  // ==================== Events Tests ====================

  @Test
  fun `parseDeepLink with events host returns map route`() {
    val result = DeepLinkHandler.parseDeepLink("mapin://events")
    assertEquals(Route.Map.route, result)
  }

  @Test
  fun `parseDeepLink with events and eventId returns map route`() {
    val result = DeepLinkHandler.parseDeepLink("mapin://events/event456")
    assertEquals(Route.Map.route, result)
  }

  // ==================== Messages Tests ====================

  @Test
  fun `parseDeepLink with messages host without conversationId returns chat route`() {
    val result = DeepLinkHandler.parseDeepLink("mapin://messages")
    assertEquals(Route.Chat.route, result)
  }

  @Test
  fun `parseDeepLink with messages and conversationId returns conversation route`() {
    val result = DeepLinkHandler.parseDeepLink("mapin://messages/conv123")
    assertEquals("conversation/conv123/${Uri.encode("Unknown")}", result)
  }

  @Test
  fun `parseDeepLink with messages and different conversationId returns correct conversation route`() {
    val result = DeepLinkHandler.parseDeepLink("mapin://messages/abc-def-ghi")
    assertEquals("conversation/abc-def-ghi/${Uri.encode("Unknown")}", result)
  }

  @Test
  fun `parseDeepLink with messages and numeric conversationId returns correct conversation route`() {
    val result = DeepLinkHandler.parseDeepLink("mapin://messages/12345")
    assertEquals("conversation/12345/${Uri.encode("Unknown")}", result)
  }

  // ==================== Map Tests ====================

  @Test
  fun `parseDeepLink with map host returns map route`() {
    val result = DeepLinkHandler.parseDeepLink("mapin://map")
    assertEquals(Route.Map.route, result)
  }

  @Test
  fun `parseDeepLink with map and path returns map route`() {
    val result = DeepLinkHandler.parseDeepLink("mapin://map/some/path")
    assertEquals(Route.Map.route, result)
  }

  // ==================== Invalid Scheme Tests ====================

  @Test
  fun `parseDeepLink with https scheme returns null`() {
    val result = DeepLinkHandler.parseDeepLink("https://example.com/path")
    assertNull(result)
  }

  @Test
  fun `parseDeepLink with http scheme returns null`() {
    val result = DeepLinkHandler.parseDeepLink("http://example.com/path")
    assertNull(result)
  }

  @Test
  fun `parseDeepLink with custom non-mapin scheme returns null`() {
    val result = DeepLinkHandler.parseDeepLink("otherscheme://friendRequests")
    assertNull(result)
  }

  // ==================== Unknown Host Tests ====================

  @Test
  fun `parseDeepLink with unknown host returns null`() {
    val result = DeepLinkHandler.parseDeepLink("mapin://unknown")
    assertNull(result)
  }

  @Test
  fun `parseDeepLink with unknown host and path returns null`() {
    val result = DeepLinkHandler.parseDeepLink("mapin://unknown/path/to/resource")
    assertNull(result)
  }

  @Test
  fun `parseDeepLink with empty host returns null`() {
    val result = DeepLinkHandler.parseDeepLink("mapin://")
    assertNull(result)
  }

  // ==================== Edge Cases ====================

  @Test
  fun `parseDeepLink with malformed URL returns null`() {
    val result = DeepLinkHandler.parseDeepLink("not a valid url")
    // Uri.parse handles malformed URLs gracefully, so this may not throw
    // The result depends on how Uri.parse handles this input
    assertNull(result)
  }

  @Test
  fun `parseDeepLink with empty string returns null`() {
    val result = DeepLinkHandler.parseDeepLink("")
    assertNull(result)
  }

  @Test
  fun `parseDeepLink with special characters in path handles correctly`() {
    val result = DeepLinkHandler.parseDeepLink("mapin://messages/conv%20with%20spaces")
    // Uri.parse decodes the path segments, so spaces are decoded
    assertEquals("conversation/conv with spaces/${Uri.encode("Unknown")}", result)
  }

  @Test
  fun `parseDeepLink with query parameters ignores them for friendRequests`() {
    val result = DeepLinkHandler.parseDeepLink("mapin://friendRequests?extra=param")
    assertEquals("friends?tab=REQUESTS", result)
  }

  @Test
  fun `parseDeepLink with query parameters ignores them for events`() {
    val result = DeepLinkHandler.parseDeepLink("mapin://events?eventId=123")
    assertEquals(Route.Map.route, result)
  }

  // ==================== Case Sensitivity Tests ====================

  @Test
  fun `parseDeepLink is case sensitive for host - FRIENDREQUESTS returns null`() {
    val result = DeepLinkHandler.parseDeepLink("mapin://FRIENDREQUESTS")
    assertNull(result)
  }

  @Test
  fun `parseDeepLink is case sensitive for host - FriendRequests returns null`() {
    val result = DeepLinkHandler.parseDeepLink("mapin://FriendRequests")
    assertNull(result)
  }

  @Test
  fun `parseDeepLink with uppercase scheme MAPIN returns null`() {
    // Android Uri.parse does NOT normalize scheme to lowercase
    val result = DeepLinkHandler.parseDeepLink("MAPIN://friendRequests")
    assertNull(result)
  }
}
