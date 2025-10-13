package com.swent.mapin.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserProfileTest {

  @Test
  fun `UserProfile can be created with all parameters`() {
    val profile =
        UserProfile(
            userId = "user-123",
            name = "John Doe",
            bio = "Software developer",
            hobbies = listOf("Reading", "Gaming", "Coding"),
            location = "New York",
            profilePictureUrl = "https://example.com/profile.jpg",
            avatarUrl = "https://example.com/avatar.jpg",
            bannerUrl = "https://example.com/banner.jpg",
            hobbiesVisible = true)

    assertEquals("user-123", profile.userId)
    assertEquals("John Doe", profile.name)
    assertEquals("Software developer", profile.bio)
    assertEquals(listOf("Reading", "Gaming", "Coding"), profile.hobbies)
    assertEquals("New York", profile.location)
    assertEquals("https://example.com/profile.jpg", profile.profilePictureUrl)
    assertEquals("https://example.com/avatar.jpg", profile.avatarUrl)
    assertEquals("https://example.com/banner.jpg", profile.bannerUrl)
    assertTrue(profile.hobbiesVisible)
  }

  @Test
  fun `UserProfile can be created with default values`() {
    val profile = UserProfile()

    assertEquals("", profile.userId)
    assertEquals("", profile.name)
    assertEquals("", profile.bio)
    assertEquals(emptyList<String>(), profile.hobbies)
    assertEquals("", profile.location)
    assertNull(profile.profilePictureUrl)
    assertNull(profile.avatarUrl)
    assertNull(profile.bannerUrl)
    assertTrue(profile.hobbiesVisible)
  }

  @Test
  fun `UserProfile can be created with partial parameters`() {
    val profile = UserProfile(userId = "user-456", name = "Jane Doe", location = "Paris")

    assertEquals("user-456", profile.userId)
    assertEquals("Jane Doe", profile.name)
    assertEquals("", profile.bio)
    assertEquals(emptyList<String>(), profile.hobbies)
    assertEquals("Paris", profile.location)
    assertNull(profile.profilePictureUrl)
    assertNull(profile.avatarUrl)
    assertNull(profile.bannerUrl)
    assertTrue(profile.hobbiesVisible)
  }

  @Test
  fun `UserProfile copy creates new instance with modified fields`() {
    val original =
        UserProfile(
            userId = "user-789", name = "Original Name", bio = "Original bio", location = "London")

    val modified = original.copy(name = "Modified Name", bio = "Modified bio")

    assertEquals("user-789", modified.userId)
    assertEquals("Modified Name", modified.name)
    assertEquals("Modified bio", modified.bio)
    assertEquals("London", modified.location)

    // Original should remain unchanged
    assertEquals("Original Name", original.name)
    assertEquals("Original bio", original.bio)
  }

  @Test
  fun `UserProfile with empty hobbies list`() {
    val profile = UserProfile(userId = "user-101", name = "Test User", hobbies = emptyList())

    assertNotNull(profile.hobbies)
    assertTrue(profile.hobbies.isEmpty())
    assertEquals(0, profile.hobbies.size)
  }

  @Test
  fun `UserProfile with multiple hobbies`() {
    val hobbies = listOf("Reading", "Gaming", "Cooking", "Travel", "Photography")
    val profile = UserProfile(userId = "user-102", name = "Hobby Enthusiast", hobbies = hobbies)

    assertEquals(5, profile.hobbies.size)
    assertTrue(profile.hobbies.contains("Reading"))
    assertTrue(profile.hobbies.contains("Gaming"))
    assertTrue(profile.hobbies.contains("Cooking"))
    assertTrue(profile.hobbies.contains("Travel"))
    assertTrue(profile.hobbies.contains("Photography"))
  }

  @Test
  fun `UserProfile hobbiesVisible can be set to false`() {
    val profile =
        UserProfile(
            userId = "user-103",
            name = "Private User",
            hobbies = listOf("Secret Hobby"),
            hobbiesVisible = false)

    assertFalse(profile.hobbiesVisible)
    assertEquals(listOf("Secret Hobby"), profile.hobbies)
  }

  @Test
  fun `UserProfile with null optional URLs`() {
    val profile =
        UserProfile(
            userId = "user-104",
            name = "User Without Images",
            profilePictureUrl = null,
            avatarUrl = null,
            bannerUrl = null)

    assertNull(profile.profilePictureUrl)
    assertNull(profile.avatarUrl)
    assertNull(profile.bannerUrl)
  }

  @Test
  fun `UserProfile with all image URLs set`() {
    val profile =
        UserProfile(
            userId = "user-105",
            name = "User With Images",
            profilePictureUrl = "https://example.com/profile.jpg",
            avatarUrl = "https://example.com/avatar.jpg",
            bannerUrl = "https://example.com/banner.jpg")

    assertNotNull(profile.profilePictureUrl)
    assertNotNull(profile.avatarUrl)
    assertNotNull(profile.bannerUrl)
    assertEquals("https://example.com/profile.jpg", profile.profilePictureUrl)
    assertEquals("https://example.com/avatar.jpg", profile.avatarUrl)
    assertEquals("https://example.com/banner.jpg", profile.bannerUrl)
  }

  @Test
  fun `UserProfile equality check with same data`() {
    val profile1 =
        UserProfile(
            userId = "user-106", name = "Test User", bio = "Test bio", location = "Test Location")

    val profile2 =
        UserProfile(
            userId = "user-106", name = "Test User", bio = "Test bio", location = "Test Location")

    assertEquals(profile1, profile2)
  }

  @Test
  fun `UserProfile toString contains all fields`() {
    val profile =
        UserProfile(
            userId = "user-107",
            name = "String Test User",
            bio = "String test bio",
            hobbies = listOf("Hobby1"),
            location = "Test City")

    val profileString = profile.toString()

    assertTrue(profileString.contains("user-107"))
    assertTrue(profileString.contains("String Test User"))
    assertTrue(profileString.contains("String test bio"))
    assertTrue(profileString.contains("Test City"))
  }

  @Test
  fun `UserProfile can have very long bio`() {
    val longBio = "A".repeat(500)
    val profile = UserProfile(userId = "user-108", name = "Long Bio User", bio = longBio)

    assertEquals(500, profile.bio.length)
    assertEquals(longBio, profile.bio)
  }

  @Test
  fun `UserProfile can have special characters in fields`() {
    val profile =
        UserProfile(
            userId = "user-109",
            name = "José María O'Brien-Smith",
            bio = "Developer & Designer! @Company #Tech",
            location = "São Paulo, Brazil 🇧🇷",
            hobbies = listOf("Rock & Roll", "Coffee ☕", "Code++"))

    assertEquals("José María O'Brien-Smith", profile.name)
    assertTrue(profile.bio.contains("&"))
    assertTrue(profile.bio.contains("@"))
    assertTrue(profile.bio.contains("#"))
    assertTrue(profile.location.contains("🇧🇷"))
    assertTrue(profile.hobbies.any { it.contains("☕") })
  }

  @Test
  fun `UserProfile copy preserves all unchanged fields`() {
    val original =
        UserProfile(
            userId = "user-110",
            name = "Original",
            bio = "Original bio",
            hobbies = listOf("Hobby1", "Hobby2"),
            location = "Original Location",
            profilePictureUrl = "url1",
            avatarUrl = "url2",
            bannerUrl = "url3",
            hobbiesVisible = false)

    val modified = original.copy(name = "Modified")

    assertEquals("Modified", modified.name)
    assertEquals("Original bio", modified.bio)
    assertEquals(listOf("Hobby1", "Hobby2"), modified.hobbies)
    assertEquals("Original Location", modified.location)
    assertEquals("url1", modified.profilePictureUrl)
    assertEquals("url2", modified.avatarUrl)
    assertEquals("url3", modified.bannerUrl)
    assertFalse(modified.hobbiesVisible)
  }

  @Test
  fun `UserProfile can update hobbies visibility without changing hobbies`() {
    val profile =
        UserProfile(
            userId = "user-111",
            name = "Visibility Test",
            hobbies = listOf("Hobby1", "Hobby2"),
            hobbiesVisible = true)

    val updated = profile.copy(hobbiesVisible = false)

    assertEquals(profile.hobbies, updated.hobbies)
    assertFalse(updated.hobbiesVisible)
    assertTrue(profile.hobbiesVisible)
  }

  @Test
  fun `UserProfile handles empty strings correctly`() {
    val profile =
        UserProfile(userId = "", name = "", bio = "", location = "", hobbies = emptyList())

    assertEquals("", profile.userId)
    assertEquals("", profile.name)
    assertEquals("", profile.bio)
    assertEquals("", profile.location)
    assertTrue(profile.hobbies.isEmpty())
  }
}
