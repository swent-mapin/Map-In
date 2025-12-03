package com.swent.mapin

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainActivityDeepLinkTest {
  @Test
  fun `getDeepLinkUrlFromIntent returns action_url when present`() {
    val intent =
        Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
          putExtra("action_url", "mapin://events/abc")
        }
    assertEquals("mapin://events/abc", getDeepLinkUrlFromIntent(intent))
  }

  @Test
  fun `getDeepLinkUrlFromIntent returns null when absent`() {
    val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
    assertNull(getDeepLinkUrlFromIntent(intent))
    assertNull(getDeepLinkUrlFromIntent(null))
  }
}
