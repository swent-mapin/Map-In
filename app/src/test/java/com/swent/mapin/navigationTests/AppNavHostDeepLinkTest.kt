package com.swent.mapin.navigationTests

import com.swent.mapin.navigation.isFriendAcceptedDeepLink
import com.swent.mapin.navigation.isFriendRequestDeepLink
import com.swent.mapin.navigation.isMapDeepLink
import com.swent.mapin.navigation.isMessagesDeepLink
import com.swent.mapin.navigation.parseDeepLinkEventId
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Test

class AppNavHostDeepLinkTest {
  @Test
  fun `parseDeepLinkEventId extracts event id when URL matches`() {
    assertEquals("abc123", parseDeepLinkEventId("mapin://events/abc123"))
  }

  @Test
  fun `parseDeepLinkEventId returns null for non matching URL`() {
    assertNull(parseDeepLinkEventId("https://example.com"))
  }

  @Test
  fun `parseDeepLinkEventId returns null for null input`() {
    assertNull(parseDeepLinkEventId(null))
  }

  @Test
  fun `isFriendRequestDeepLink returns true for friend request URLs`() {
    assertTrue(isFriendRequestDeepLink("mapin://friendRequests/request123"))
    assertTrue(isFriendRequestDeepLink("mapin://friendRequests"))
  }

  @Test
  fun `isFriendRequestDeepLink returns false for non-friend request URLs`() {
    assertFalse(isFriendRequestDeepLink("mapin://events/event123"))
    assertFalse(isFriendRequestDeepLink("https://example.com"))
    assertFalse(isFriendRequestDeepLink(null))
  }

  @Test
  fun `isFriendAcceptedDeepLink returns true for friend accepted URLs`() {
    assertTrue(isFriendAcceptedDeepLink("mapin://friendAccepted"))
    assertTrue(isFriendAcceptedDeepLink("mapin://friendAccepted/extra"))
  }

  @Test
  fun `isFriendAcceptedDeepLink returns true for profile URLs`() {
    assertTrue(isFriendAcceptedDeepLink("mapin://profile/user123"))
    assertTrue(isFriendAcceptedDeepLink("mapin://profile/"))
  }

  @Test
  fun `isFriendAcceptedDeepLink returns false for non-friend accepted URLs`() {
    assertFalse(isFriendAcceptedDeepLink("mapin://events/event123"))
    assertFalse(isFriendAcceptedDeepLink("mapin://friendRequests/req123"))
    assertFalse(isFriendAcceptedDeepLink("https://example.com"))
    assertFalse(isFriendAcceptedDeepLink(null))
  }

  @Test
  fun `isMessagesDeepLink returns true for messages URLs`() {
    assertTrue(isMessagesDeepLink("mapin://messages"))
    assertTrue(isMessagesDeepLink("mapin://messages/conversation123"))
  }

  @Test
  fun `isMessagesDeepLink returns false for non-messages URLs`() {
    assertFalse(isMessagesDeepLink("mapin://events/event123"))
    assertFalse(isMessagesDeepLink("mapin://map"))
    assertFalse(isMessagesDeepLink("https://example.com"))
    assertFalse(isMessagesDeepLink(null))
  }

  @Test
  fun `isMapDeepLink returns true for map URLs`() {
    assertTrue(isMapDeepLink("mapin://map"))
    assertTrue(isMapDeepLink("mapin://map/extra"))
  }

  @Test
  fun `isMapDeepLink returns false for non-map URLs`() {
    assertFalse(isMapDeepLink("mapin://events/event123"))
    assertFalse(isMapDeepLink("mapin://messages"))
    assertFalse(isMapDeepLink("https://example.com"))
    assertFalse(isMapDeepLink(null))
  }
}
