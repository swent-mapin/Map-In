package com.swent.mapin.model

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Transaction
import com.swent.mapin.model.userprofile.UserProfile
import com.swent.mapin.model.userprofile.UserProfileRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.MockitoAnnotations.openMocks
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class UserProfileRepositoryTest {

  @Mock private lateinit var firestore: FirebaseFirestore
  @Mock private lateinit var collection: CollectionReference
  @Mock private lateinit var currentUserRef: DocumentReference
  @Mock private lateinit var targetUserRef: DocumentReference
  @Mock private lateinit var transaction: Transaction
  @Mock private lateinit var currentSnapshot: DocumentSnapshot
  @Mock private lateinit var targetSnapshot: DocumentSnapshot
  @Mock private lateinit var querySnapshot: QuerySnapshot

  private lateinit var repository: UserProfileRepository

  @Before
  fun setUp() {
    openMocks(this)
    repository = UserProfileRepository(firestore)

    whenever(firestore.collection(COLLECTION)).thenReturn(collection)
    whenever(collection.document(eq(CURRENT_ID))).thenReturn(currentUserRef)
    whenever(collection.document(eq(TARGET_ID))).thenReturn(targetUserRef)
    whenever(transaction.get(currentUserRef)).thenReturn(currentSnapshot)
    whenever(transaction.get(targetUserRef)).thenReturn(targetSnapshot)
    doReturn(transaction)
        .whenever(transaction)
        .update(any<DocumentReference>(), any<String>(), any())

    stubRunTransaction()
  }

  @Test
  fun followUser_addsMissingIds() = runTest {
    givenProfiles(currentFollowing = emptyList(), targetFollowers = emptyList())

    val result = repository.followUser(CURRENT_ID, TARGET_ID)

    assertTrue(result)
    verify(transaction).update(currentUserRef, "followingIds", listOf(TARGET_ID))
    verify(transaction).update(targetUserRef, "followerIds", listOf(CURRENT_ID))
  }

  @Test
  fun followUser_noDuplicateUpdatesWhenAlreadyFollowing() = runTest {
    givenProfiles(currentFollowing = listOf(TARGET_ID), targetFollowers = listOf(CURRENT_ID))

    val result = repository.followUser(CURRENT_ID, TARGET_ID)

    assertTrue(result)
    verify(transaction, never()).update(any<DocumentReference>(), any<String>(), any())
  }

  @Test
  fun followUser_handlesNullLists() = runTest {
    givenProfiles(currentFollowing = null, targetFollowers = null)

    val result = repository.followUser(CURRENT_ID, TARGET_ID)

    assertTrue(result)
    verify(transaction).update(currentUserRef, "followingIds", listOf(TARGET_ID))
    verify(transaction).update(targetUserRef, "followerIds", listOf(CURRENT_ID))
  }

  @Test
  fun unfollowUser_removesIds() = runTest {
    givenProfiles(currentFollowing = listOf(TARGET_ID), targetFollowers = listOf(CURRENT_ID))

    val result = repository.unfollowUser(CURRENT_ID, TARGET_ID)

    assertTrue(result)
    verify(transaction).update(currentUserRef, "followingIds", emptyList<String>())
    verify(transaction).update(targetUserRef, "followerIds", emptyList<String>())
  }

  @Test
  fun followUser_rejectsSelfFollow() = runTest {
    val result = repository.followUser(CURRENT_ID, CURRENT_ID)

    assertFalse(result)
    verify(firestore, never()).runTransaction<Any>(any<Transaction.Function<Any?>>())
  }

  @Test
  fun followUser_returnsFalseWhenUserMissing() = runTest {
    givenProfiles(
        currentFollowing = emptyList(),
        targetFollowers = emptyList(),
        currentExists = true,
        targetExists = false)

    val result = repository.followUser(CURRENT_ID, TARGET_ID)

    assertFalse(result)
    verify(transaction, never()).update(any<DocumentReference>(), any<String>(), any())
  }

  @Test
  fun isFollowing_returnsTrueWhenPresent() = runTest {
    val repoSpy = spy(repository)
    whenever(repoSpy.getUserProfile(CURRENT_ID))
        .thenReturn(UserProfile(userId = CURRENT_ID, followingIds = listOf(TARGET_ID)))

    val result = repoSpy.isFollowing(CURRENT_ID, TARGET_ID)

    assertTrue(result)
  }

  @Test
  fun isFollowing_returnsFalseOnAbsentOrErrors() = runTest {
    val repoSpy = spy(repository)
    whenever(repoSpy.getUserProfile(CURRENT_ID))
        .thenReturn(UserProfile(userId = CURRENT_ID, followingIds = emptyList()))

    assertFalse(repoSpy.isFollowing(CURRENT_ID, TARGET_ID))

    whenever(repoSpy.getUserProfile(CURRENT_ID)).thenReturn(null)
    assertFalse(repoSpy.isFollowing(CURRENT_ID, TARGET_ID))

    whenever(repoSpy.getUserProfile(CURRENT_ID)).thenThrow(RuntimeException("boom"))
    assertFalse(repoSpy.isFollowing(CURRENT_ID, TARGET_ID))
  }

  @Test
  fun getUserProfile_returnsProfileWhenExists() = runTest {
    val expectedProfile = UserProfile(userId = CURRENT_ID, name = "Test")
    whenever(currentSnapshot.exists()).thenReturn(true)
    whenever(currentSnapshot.toObject(UserProfile::class.java)).thenReturn(expectedProfile)
    whenever(currentUserRef.get()).thenReturn(Tasks.forResult(currentSnapshot))

    val result = repository.getUserProfile(CURRENT_ID)

    assertEquals(expectedProfile, result)
  }

  @Test
  fun getUserProfile_returnsNullWhenMissing() = runTest {
    whenever(currentSnapshot.exists()).thenReturn(false)
    whenever(currentUserRef.get()).thenReturn(Tasks.forResult(currentSnapshot))

    val result = repository.getUserProfile(CURRENT_ID)

    assertEquals(null, result)
  }

  @Test
  fun saveUserProfile_returnsTrueOnSuccess() = runTest {
    val profile = UserProfile(userId = CURRENT_ID, name = "User")
    whenever(currentUserRef.set(profile)).thenReturn(Tasks.forResult(null))

    val result = repository.saveUserProfile(profile)

    assertTrue(result)
  }

  @Test
  fun saveUserProfile_returnsFalseOnFailure() = runTest {
    val profile = UserProfile(userId = CURRENT_ID, name = "User")
    whenever(currentUserRef.set(profile))
        .thenReturn(Tasks.forException(RuntimeException("write failed")))

    val result = repository.saveUserProfile(profile)

    assertFalse(result)
  }

  @Test
  fun createDefaultProfile_populatesDefaultsAndSaves() = runTest {
    val repoSpy = spy(repository)
    doReturn(true).whenever(repoSpy).saveUserProfile(any())

    val profile = repoSpy.createDefaultProfile(CURRENT_ID, "", null)

    assertEquals("Anonymous User", profile.name)
    assertEquals("Tell us about yourself...", profile.bio)
    assertEquals("person", profile.avatarUrl)
    assertEquals("purple_blue", profile.bannerUrl)
    assertTrue(profile.hobbiesVisible)
    assertEquals(null, profile.profilePictureUrl)
    verify(repoSpy).saveUserProfile(profile)
  }

  private fun givenProfiles(
      currentFollowing: List<String>? = emptyList(),
      targetFollowers: List<String>? = emptyList(),
      currentExists: Boolean = true,
      targetExists: Boolean = true
  ) {
    whenever(currentSnapshot.exists()).thenReturn(currentExists)
    whenever(targetSnapshot.exists()).thenReturn(targetExists)
    whenever(currentSnapshot.get("followingIds")).thenReturn(currentFollowing)
    whenever(targetSnapshot.get("followerIds")).thenReturn(targetFollowers)
  }

  @Suppress("UNCHECKED_CAST")
  private fun stubRunTransaction() {
    doAnswer { invocation ->
          val function = invocation.arguments[0] as Transaction.Function<Any?>
          try {
            function.apply(transaction)
            Tasks.forResult<Void?>(null)
          } catch (e: Exception) {
            Tasks.forException<Void?>(e)
          }
        }
        .`when`(firestore)
        .runTransaction(any<Transaction.Function<Any?>>())
  }

  // searchUsers tests
  @Test
  fun searchUsers_returnsMatchingUsersByName() = runTest {
    val user1 = UserProfile(userId = "user1", name = "Alice Smith")
    val user2 = UserProfile(userId = "user2", name = "Bob Jones")
    val user3 = UserProfile(userId = "user3", name = "Alice Cooper")

    val doc1 = mock(DocumentSnapshot::class.java)
    val doc2 = mock(DocumentSnapshot::class.java)
    val doc3 = mock(DocumentSnapshot::class.java)
    whenever(doc1.toObject(UserProfile::class.java)).thenReturn(user1)
    whenever(doc2.toObject(UserProfile::class.java)).thenReturn(user2)
    whenever(doc3.toObject(UserProfile::class.java)).thenReturn(user3)
    whenever(querySnapshot.documents).thenReturn(listOf(doc1, doc2, doc3))
    whenever(collection.get()).thenReturn(Tasks.forResult(querySnapshot))

    val results = repository.searchUsers("alice").first()

    assertEquals(2, results.size)
    assertTrue(results.any { it.userId == "user1" })
    assertTrue(results.any { it.userId == "user3" })
  }

  @Test
  fun searchUsers_returnsEmptyListForBlankQuery() = runTest {
    val results = repository.searchUsers("").first()
    assertEquals(0, results.size)

    val resultsBlank = repository.searchUsers("   ").first()
    assertEquals(0, resultsBlank.size)
  }

  @Test
  fun searchUsers_isCaseInsensitive() = runTest {
    val user = UserProfile(userId = "user1", name = "John Doe")
    val doc = mock(DocumentSnapshot::class.java)
    whenever(doc.toObject(UserProfile::class.java)).thenReturn(user)
    whenever(querySnapshot.documents).thenReturn(listOf(doc))
    whenever(collection.get()).thenReturn(Tasks.forResult(querySnapshot))

    val results = repository.searchUsers("JOHN").first()

    assertEquals(1, results.size)
    assertEquals("user1", results[0].userId)
  }

  @Test
  fun searchUsers_limitsResultsToFive() = runTest {
    val users = (1..10).map { UserProfile(userId = "user$it", name = "Test User $it") }
    val docs =
        users.map { user ->
          mock(DocumentSnapshot::class.java).also {
            whenever(it.toObject(UserProfile::class.java)).thenReturn(user)
          }
        }
    whenever(querySnapshot.documents).thenReturn(docs)
    whenever(collection.get()).thenReturn(Tasks.forResult(querySnapshot))

    val results = repository.searchUsers("Test").first()

    assertEquals(5, results.size)
  }

  @Test
  fun searchUsers_returnsEmptyListOnError() = runTest {
    whenever(collection.get()).thenReturn(Tasks.forException(RuntimeException("Network error")))

    val results = repository.searchUsers("test").first()

    assertEquals(0, results.size)
  }

  companion object {
    private const val COLLECTION = "users"
    private const val CURRENT_ID = "current"
    private const val TARGET_ID = "target"
  }
}
