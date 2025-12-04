package com.swent.mapin.navigationTests

import com.swent.mapin.navigation.parseDeepLinkEventId
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
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
}
