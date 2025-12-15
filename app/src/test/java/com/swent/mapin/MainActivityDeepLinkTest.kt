package com.swent.mapin

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for getDeepLinkUrlFromIntent() function.
 *
 * Tests verify correct extraction of deep link URLs from intents, including dual-key checking
 * (actionUrl from Firebase and action_url from PendingIntent).
 */
@RunWith(RobolectricTestRunner::class)
class MainActivityDeepLinkTest {

  // ==================== action_url Key Tests (PendingIntent) ====================

  @Test
  fun `getDeepLinkUrlFromIntent returns action_url when present`() {
    val intent =
        Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
          putExtra("action_url", "mapin://events/abc")
        }
    assertEquals("mapin://events/abc", getDeepLinkUrlFromIntent(intent))
  }

  @Test
  fun `getDeepLinkUrlFromIntent returns action_url for friendRequests`() {
    val intent =
        Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
          putExtra("action_url", "mapin://friendRequests/request123")
        }
    assertEquals("mapin://friendRequests/request123", getDeepLinkUrlFromIntent(intent))
  }

  @Test
  fun `getDeepLinkUrlFromIntent returns action_url for messages with conversationId`() {
    val intent =
        Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
          putExtra("action_url", "mapin://messages/conv123")
        }
    assertEquals("mapin://messages/conv123", getDeepLinkUrlFromIntent(intent))
  }

  // ==================== actionUrl Key Tests (Firebase) ====================

  @Test
  fun `getDeepLinkUrlFromIntent returns actionUrl when present`() {
    val intent =
        Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
          putExtra("actionUrl", "mapin://profile/user456")
        }
    assertEquals("mapin://profile/user456", getDeepLinkUrlFromIntent(intent))
  }

  @Test
  fun `getDeepLinkUrlFromIntent returns actionUrl for friendAccepted`() {
    val intent =
        Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
          putExtra("actionUrl", "mapin://friendAccepted")
        }
    assertEquals("mapin://friendAccepted", getDeepLinkUrlFromIntent(intent))
  }

  @Test
  fun `getDeepLinkUrlFromIntent returns actionUrl for messages`() {
    val intent =
        Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
          putExtra("actionUrl", "mapin://messages")
        }
    assertEquals("mapin://messages", getDeepLinkUrlFromIntent(intent))
  }

  // ==================== Dual-Key Priority Tests ====================

  @Test
  fun `getDeepLinkUrlFromIntent prefers actionUrl over action_url when both present`() {
    val intent =
        Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
          putExtra("actionUrl", "mapin://events/from-firebase")
          putExtra("action_url", "mapin://events/from-pending-intent")
        }
    // actionUrl takes priority (checked first)
    assertEquals("mapin://events/from-firebase", getDeepLinkUrlFromIntent(intent))
  }

  @Test
  fun `getDeepLinkUrlFromIntent falls back to action_url when actionUrl absent`() {
    val intent =
        Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
          putExtra("action_url", "mapin://map")
        }
    assertEquals("mapin://map", getDeepLinkUrlFromIntent(intent))
  }

  // ==================== Null and Empty Tests ====================

  @Test
  fun `getDeepLinkUrlFromIntent returns null when absent`() {
    val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
    assertNull(getDeepLinkUrlFromIntent(intent))
  }

  @Test
  fun `getDeepLinkUrlFromIntent returns null for null intent`() {
    assertNull(getDeepLinkUrlFromIntent(null))
  }

  @Test
  fun `getDeepLinkUrlFromIntent returns null when other extras present but not deep link keys`() {
    val intent =
        Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
          putExtra("some_other_key", "some_value")
          putExtra("title", "Notification Title")
        }
    assertNull(getDeepLinkUrlFromIntent(intent))
  }

  // ==================== Edge Cases ====================

  @Test
  fun `getDeepLinkUrlFromIntent returns empty string if actionUrl is empty`() {
    val intent =
        Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
          putExtra("actionUrl", "")
        }
    assertEquals("", getDeepLinkUrlFromIntent(intent))
  }

  @Test
  fun `getDeepLinkUrlFromIntent returns empty string if action_url is empty`() {
    val intent =
        Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
          putExtra("action_url", "")
        }
    assertEquals("", getDeepLinkUrlFromIntent(intent))
  }

  @Test
  fun `getDeepLinkUrlFromIntent handles URL with special characters`() {
    val intent =
        Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
          putExtra("action_url", "mapin://messages/conv%20with%20spaces")
        }
    assertEquals("mapin://messages/conv%20with%20spaces", getDeepLinkUrlFromIntent(intent))
  }

  @Test
  fun `getDeepLinkUrlFromIntent handles long conversation ID`() {
    val longId = "a".repeat(100)
    val intent =
        Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
          putExtra("action_url", "mapin://messages/$longId")
        }
    assertEquals("mapin://messages/$longId", getDeepLinkUrlFromIntent(intent))
  }

  // ==================== Additional Edge Cases ====================

  @Test
  fun `getDeepLinkUrlFromIntent handles query parameters`() {
    val intent =
        Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
          putExtra("action_url", "mapin://events?filter=upcoming&sort=date")
        }
    assertEquals("mapin://events?filter=upcoming&sort=date", getDeepLinkUrlFromIntent(intent))
  }

  @Test
  fun `getDeepLinkUrlFromIntent returns null when intent has wrong type for actionUrl`() {
    val intent =
        Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
          putExtra("actionUrl", 12345) // wrong type - int instead of string
        }
    assertNull(getDeepLinkUrlFromIntent(intent))
  }
}
