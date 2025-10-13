package com.swent.mapin.model

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileRepositoryTest {

  private lateinit var repository: UserProfileRepository
  private lateinit var mockFirestore: FirebaseFirestore
  private lateinit var mockCollection: CollectionReference
  private lateinit var mockDocument: DocumentReference
  private lateinit var mockSnapshot: DocumentSnapshot

  private val testUserId = "test-user-123"
  private val testProfile =
      UserProfile(
          userId = testUserId,
          name = "John Doe",
          bio = "Test bio",
          hobbies = listOf("Reading", "Gaming"),
          location = "New York",
          avatarUrl = "https://example.com/avatar.jpg",
          bannerUrl = "https://example.com/banner.jpg",
          hobbiesVisible = true)

  @Before
  fun setup() {
    mockFirestore = mockk(relaxed = true)
    mockCollection = mockk(relaxed = true)
    mockDocument = mockk(relaxed = true)
    mockSnapshot = mockk(relaxed = true)

    every { mockFirestore.collection("users") } returns mockCollection
    every { mockCollection.document(any()) } returns mockDocument

    repository = UserProfileRepository(mockFirestore)
  }

  @Test
  fun `getUserProfile returns profile when document exists`() = runTest {
    // Mock document exists and has data
    every { mockSnapshot.exists() } returns true
    every { mockSnapshot.toObject(UserProfile::class.java) } returns testProfile
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)

    val result = repository.getUserProfile(testUserId)

    assertNotNull(result)
    assertEquals(testProfile, result)
    verify { mockCollection.document(testUserId) }
  }

  @Test
  fun `getUserProfile returns null when document does not exist`() = runTest {
    // Mock document doesn't exist
    every { mockSnapshot.exists() } returns false
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)

    val result = repository.getUserProfile(testUserId)

    assertNull(result)
    verify { mockCollection.document(testUserId) }
  }

  @Test
  fun `getUserProfile returns null on exception`() = runTest {
    // Mock exception during fetch
    every { mockDocument.get() } returns Tasks.forException(Exception("Network error"))

    val result = repository.getUserProfile(testUserId)

    assertNull(result)
    verify { mockCollection.document(testUserId) }
  }

  @Test
  fun `saveUserProfile saves profile successfully`() = runTest {
    // Mock successful save
    every { mockDocument.set(any()) } returns Tasks.forResult(null)

    val result = repository.saveUserProfile(testProfile)

    assertTrue(result)
    verify { mockCollection.document(testUserId) }
    verify { mockDocument.set(testProfile) }
  }

  @Test
  fun `saveUserProfile returns false on exception`() = runTest {
    // Mock exception during save
    every { mockDocument.set(any()) } returns Tasks.forException(Exception("Save error"))

    val result = repository.saveUserProfile(testProfile)

    assertFalse(result)
    verify { mockCollection.document(testUserId) }
    verify { mockDocument.set(testProfile) }
  }

  @Test
  fun `createDefaultProfile creates profile with provided data`() = runTest {
    every { mockDocument.set(any()) } returns Tasks.forResult(null)

    val result =
        repository.createDefaultProfile(
            userId = testUserId,
            name = "John Doe",
            profilePictureUrl = "https://example.com/pic.jpg")

    assertNotNull(result)
    assertEquals(testUserId, result.userId)
    assertEquals("John Doe", result.name)
    assertEquals("Tell us about yourself...", result.bio)
    assertEquals("Unknown", result.location)
    assertEquals("https://example.com/pic.jpg", result.profilePictureUrl)
    assertEquals(emptyList<String>(), result.hobbies)
    verify { mockDocument.set(any()) }
  }

  @Test
  fun `createDefaultProfile creates profile with empty name as Anonymous User`() = runTest {
    every { mockDocument.set(any()) } returns Tasks.forResult(null)

    val result =
        repository.createDefaultProfile(userId = testUserId, name = "", profilePictureUrl = null)

    assertNotNull(result)
    assertEquals("Anonymous User", result.name)
    assertNull(result.profilePictureUrl)
  }

  @Test
  fun `createDefaultProfile creates profile without profile picture`() = runTest {
    every { mockDocument.set(any()) } returns Tasks.forResult(null)

    val result = repository.createDefaultProfile(userId = testUserId, name = "Jane Doe")

    assertNotNull(result)
    assertEquals("Jane Doe", result.name)
    assertNull(result.profilePictureUrl)
  }

  @Test
  fun `saveUserProfile saves all profile fields correctly`() = runTest {
    every { mockDocument.set(any()) } returns Tasks.forResult(null)

    val profileWithAllFields =
        UserProfile(
            userId = "user-456",
            name = "Complete User",
            bio = "Complete bio",
            hobbies = listOf("Hobby1", "Hobby2", "Hobby3"),
            location = "Paris",
            profilePictureUrl = "https://example.com/profile.jpg",
            avatarUrl = "https://example.com/avatar.jpg",
            bannerUrl = "https://example.com/banner.jpg",
            hobbiesVisible = false)

    val result = repository.saveUserProfile(profileWithAllFields)

    assertTrue(result)
    verify { mockDocument.set(profileWithAllFields) }
  }

  @Test
  fun `getUserProfile handles partial profile data`() = runTest {
    val partialProfile =
        UserProfile(
            userId = testUserId,
            name = "Partial User",
            bio = "",
            hobbies = emptyList(),
            location = "")

    every { mockSnapshot.exists() } returns true
    every { mockSnapshot.toObject(UserProfile::class.java) } returns partialProfile
    every { mockDocument.get() } returns Tasks.forResult(mockSnapshot)

    val result = repository.getUserProfile(testUserId)

    assertNotNull(result)
    assertEquals("Partial User", result?.name)
    assertEquals("", result?.bio)
    assertEquals(emptyList<String>(), result?.hobbies)
  }

  @Test
  fun `saveUserProfile handles profile with null optional fields`() = runTest {
    every { mockDocument.set(any()) } returns Tasks.forResult(null)

    val profileWithNulls =
        UserProfile(
            userId = testUserId,
            name = "User Without Images",
            bio = "Bio text",
            hobbies = listOf("Reading"),
            location = "Tokyo",
            profilePictureUrl = null,
            avatarUrl = null,
            bannerUrl = null,
            hobbiesVisible = true)

    val result = repository.saveUserProfile(profileWithNulls)

    assertTrue(result)
    verify { mockDocument.set(profileWithNulls) }
  }
}
