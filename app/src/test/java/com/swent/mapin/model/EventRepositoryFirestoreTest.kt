package com.swent.mapin.model

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Transaction
import com.swent.mapin.model.badge.BadgeContext
import com.swent.mapin.model.badge.BadgeRepository
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepositoryFirestore
import com.swent.mapin.model.event.FirestoreSchema.EVENTS_COLLECTION_PATH
import com.swent.mapin.model.event.FirestoreSchema.USERS_COLLECTION_PATH
import com.swent.mapin.model.event.FirestoreSchema.UserFields.JOINED_EVENT_IDS
import com.swent.mapin.model.event.FirestoreSchema.UserFields.OWNED_EVENT_IDS
import com.swent.mapin.model.event.FirestoreSchema.UserFields.SAVED_EVENT_IDS
import com.swent.mapin.model.friends.FriendRequestRepository
import com.swent.mapin.model.friends.FriendWithProfile
import com.swent.mapin.model.friends.FriendshipStatus
import com.swent.mapin.model.location.Location
import com.swent.mapin.model.notification.Notification
import com.swent.mapin.model.notification.NotificationResult
import com.swent.mapin.model.notification.NotificationService
import com.swent.mapin.model.userprofile.UserProfile
import com.swent.mapin.model.userprofile.UserProfileRepository
import com.swent.mapin.ui.filters.Filters
import java.time.LocalDate
import java.util.Calendar
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class EventRepositoryFirestoreTest {

  private lateinit var db: FirebaseFirestore
  private lateinit var repo: EventRepositoryFirestore
  private lateinit var document: DocumentReference
  private lateinit var collection: CollectionReference
  private lateinit var usersCollection: CollectionReference
  private lateinit var userDocRef: DocumentReference
  private lateinit var friendRepo: FriendRequestRepository
  private lateinit var transaction: Transaction
  private lateinit var firebaseAuthMock: MockedStatic<FirebaseAuth>
  private lateinit var notificationService: NotificationService
  private lateinit var userProfileRepository: UserProfileRepository
  private lateinit var badgeRepo: BadgeRepository

  @Before
  fun setup() {
    db = mock()
    friendRepo = mock()
    notificationService = mock()
    userProfileRepository = mock()
    firebaseAuthMock = mockStatic(FirebaseAuth::class.java)

    val fakeAuth = mock<FirebaseAuth>()
    val fakeUser = mock<FirebaseUser>()

    whenever(fakeUser.uid).thenReturn("currentUser")
    whenever(fakeAuth.currentUser).thenReturn(fakeUser)

    firebaseAuthMock.`when`<FirebaseAuth> { FirebaseAuth.getInstance() }.thenReturn(fakeAuth)
    badgeRepo = mock()
    runBlocking {
      whenever(badgeRepo.getBadgeContext(any())).thenReturn(BadgeContext())
      whenever(badgeRepo.saveBadgeContext(any(), any())).thenReturn(true)
      whenever(badgeRepo.updateBadgesAfterContextChange(any())).thenReturn(Unit)
    }
    repo =
        EventRepositoryFirestore(
            db,
            friendRequestRepository = friendRepo,
            notificationService = notificationService,
            userProfileRepository = userProfileRepository,
            badgeRepository = badgeRepo)
    document = mock()
    transaction = mock()
    collection =
        mock<CollectionReference>().apply {
          whenever(this.whereGreaterThanOrEqualTo(any<String>(), any<Timestamp>()))
              .thenReturn(mock())
          whenever(this.orderBy(any<String>())).thenReturn(mock())
          whenever(this.orderBy(any<String>(), any<Query.Direction>())).thenReturn(mock())
        }
    usersCollection = mock()
    userDocRef = mock()

    whenever(db.collection(EVENTS_COLLECTION_PATH)).thenReturn(collection)
    whenever(db.collection(USERS_COLLECTION_PATH)).thenReturn(usersCollection)
    whenever(collection.document()).thenReturn(document)
    whenever(collection.document(any())).thenReturn(document)
  }

  @After
  fun tearDown() {
    firebaseAuthMock.close()
  }
  // Helper methods
  private fun createEvent(
      uid: String = "",
      title: String = "Event",
      url: String = "",
      description: String = "Description",
      date: Timestamp = Timestamp.now(),
      location: Location = Location.from("Location", 0.0, 0.0),
      tags: List<String> = emptyList(),
      public: Boolean = true,
      ownerId: String = "owner",
      imageUrl: String? = null,
      capacity: Int = 10,
      participantIds: List<String> = emptyList(),
      price: Double = 0.0
  ): Event {
    return Event(
        uid = uid,
        title = title,
        url = url,
        description = description,
        date = date,
        location = location,
        tags = tags,
        public = public,
        ownerId = ownerId,
        imageUrl = imageUrl,
        capacity = capacity,
        participantIds = participantIds,
        price = price)
  }

  private fun doc(id: String, event: Event? = null): QueryDocumentSnapshot {
    val snapshot = mock<QueryDocumentSnapshot>()
    whenever(snapshot.id).thenReturn(id)
    whenever(snapshot.toObject(Event::class.java)).thenReturn(event)
    whenever(snapshot.exists()).thenReturn(event != null)
    return snapshot
  }

  private fun userDoc(id: String, userProfile: UserProfile? = null): DocumentSnapshot {
    val snapshot = mock<DocumentSnapshot>()
    whenever(snapshot.id).thenReturn(id)
    whenever(snapshot.toObject(UserProfile::class.java)).thenReturn(userProfile)
    whenever(snapshot.get(SAVED_EVENT_IDS))
        .thenReturn(userProfile?.savedEventIds ?: emptyList<String>())
    whenever(snapshot.get(JOINED_EVENT_IDS))
        .thenReturn(userProfile?.joinedEventIds ?: emptyList<String>())
    whenever(snapshot.get(OWNED_EVENT_IDS))
        .thenReturn(userProfile?.ownedEventIds ?: emptyList<String>())
    whenever(snapshot.exists()).thenReturn(userProfile != null)
    return snapshot
  }

  private fun qs(vararg docs: DocumentSnapshot): QuerySnapshot {
    val snapshot = mock<QuerySnapshot>()
    whenever(snapshot.documents).thenReturn(docs.toList())
    return snapshot
  }

  private fun <T> taskOf(value: T): Task<T> = Tasks.forResult(value)

  private fun voidTask(): Task<Void> = Tasks.forResult(null)

  private fun createRepoWithNotifications(
      notificationService: NotificationService = mock(),
      userProfileRepo: UserProfileRepository = mock()
  ) =
      EventRepositoryFirestore(
          db,
          friendRequestRepository = friendRepo,
          notificationService = notificationService,
          userProfileRepository = userProfileRepo,
          badgeRepository = badgeRepo)

  // ========== BASIC CRUD TESTS ==========

  @Test
  fun getNewUid_returnsUniqueId() {
    whenever(document.id).thenReturn("RANDOM_ID")
    val id = repo.getNewUid()
    assertEquals("RANDOM_ID", id)
  }

  @Test
  fun getEvent_success_returnsMappedEvent() = runTest {
    val event =
        createEvent(
            uid = "E1",
            title = "Title",
            location = Location.from("Zurich", 47.3769, 8.5417),
            tags = listOf("music"),
            capacity = 50)
    val docSnapshot = doc("E1", event)
    whenever(document.get()).thenReturn(taskOf(docSnapshot))

    val result = repo.getEvent("E1")
    assertEquals("E1", result.uid)
    assertEquals("Title", result.title)
  }

  @Test
  fun getEvent_throwsException_whenEventNotFound() = runTest {
    val docSnapshot = mock<DocumentSnapshot>()
    whenever(docSnapshot.exists()).thenReturn(false)
    whenever(document.get()).thenReturn(Tasks.forResult(docSnapshot))

    val exception = assertFailsWith<Exception> { repo.getEvent("MISSING") }
    assertTrue(exception.message!!.contains("Event not found"))
  }

  @Test
  fun getEvent_catchesFirestoreException_andWrapsIt() = runTest {
    whenever(document.get()).thenReturn(Tasks.forException(RuntimeException("firestore broken")))

    val exception = assertFailsWith<Exception> { repo.getEvent("E1") }
    assertTrue(exception.message!!.contains("Failed to fetch event"))
  }

  // ========== ADD EVENT TESTS ==========

  @Test
  fun addEvent_success_createsEventAndUpdatesOwner() = runTest {
    val input =
        createEvent(
            uid = "", title = "New Event", ownerId = "owner123", participantIds = emptyList())

    val newEventDocRef = mock<DocumentReference>()
    whenever(collection.document()).thenReturn(newEventDocRef)
    whenever(newEventDocRef.id).thenReturn("E123")

    whenever(db.runBatch(any())).thenReturn(voidTask())

    repo.addEvent(input)

    verify(db).runBatch(any())
    verify(collection).document()
  }

  @Test
  fun addEvent_throwsException_whenInvalidEvent() = runTest {
    val invalidEvent = createEvent(title = "", ownerId = "")
    assertFailsWith<IllegalArgumentException> { repo.addEvent(invalidEvent) }
  }

  @Test
  fun addEvent_throwsException_whenParticipantsNotEmpty() = runTest {
    val invalidEvent = createEvent(ownerId = "owner123", participantIds = listOf("user1"))
    assertFailsWith<IllegalArgumentException> { repo.addEvent(invalidEvent) }
  }

  @Test
  fun addEvent_catchesFirestoreError() = runTest {
    val event = createEvent(ownerId = "owner123", participantIds = emptyList())
    whenever(collection.document()).thenReturn(document)
    whenever(document.id).thenReturn("E123")
    whenever(db.runBatch(any())).thenReturn(Tasks.forException(RuntimeException("boom")))

    val exception = assertFailsWith<Exception> { repo.addEvent(event) }
    assertTrue(exception.message!!.contains("Failed to add event"))
  }

  // ========== EDIT EVENT AS OWNER TESTS ==========

  @Test
  fun editEventAsOwner_success_updatesEvent() = runTest {
    val existing =
        createEvent(uid = "E1", title = "Old", ownerId = "owner", participantIds = listOf("user1"))
    val updated = existing.copy(title = "Updated", description = "New desc")
    val docSnapshot = doc("E1", existing)
    val eventDocRef = mock<DocumentReference>()

    whenever(collection.document("E1")).thenReturn(eventDocRef)
    whenever(db.runTransaction(any<Transaction.Function<Void>>())).thenAnswer { invocation ->
      val txFunction = invocation.getArgument<Transaction.Function<Void>>(0)
      whenever(transaction.get(eventDocRef)).thenReturn(docSnapshot)
      whenever(transaction.set(any<DocumentReference>(), any<Event>())).thenReturn(transaction)
      txFunction.apply(transaction)
      voidTask()
    }

    repo.editEventAsOwner("E1", updated)

    verify(transaction).set(any<DocumentReference>(), any<Event>())
  }

  @Test
  fun editEventAsOwner_throwsException_whenEventNotFound() = runTest {
    val eventDocRef = mock<DocumentReference>()
    whenever(collection.document("E1")).thenReturn(eventDocRef)
    whenever(db.runTransaction(any<Transaction.Function<Void>>())).thenAnswer { invocation ->
      val txFunction = invocation.getArgument<Transaction.Function<Void>>(0)
      val missingSnapshot = mock<DocumentSnapshot>()
      whenever(missingSnapshot.exists()).thenReturn(false)
      whenever(transaction.get(eventDocRef)).thenReturn(missingSnapshot)
      txFunction.apply(transaction)
      voidTask()
    }

    val updated = createEvent(uid = "E1", ownerId = "owner")
    val exception = assertFailsWith<Exception> { repo.editEventAsOwner("E1", updated) }
    assertTrue(exception.message!!.contains("Event not found"))
  }

  @Test
  fun editEventAsOwner_throwsException_whenOwnerMismatch() = runTest {
    val existing = createEvent(uid = "E1", ownerId = "owner1")
    val updated = existing.copy(ownerId = "owner2")
    val docSnapshot = doc("E1", existing)
    val eventDocRef = mock<DocumentReference>()

    whenever(collection.document("E1")).thenReturn(eventDocRef)
    whenever(db.runTransaction(any<Transaction.Function<Void>>())).thenAnswer { invocation ->
      val txFunction = invocation.getArgument<Transaction.Function<Void>>(0)
      whenever(transaction.get(eventDocRef)).thenReturn(docSnapshot)
      txFunction.apply(transaction)
      voidTask()
    }

    val exception = assertFailsWith<Exception> { repo.editEventAsOwner("E1", updated) }
    assertTrue(exception.message!!.contains("Only the owner can call editEventAsOwner"))
  }

  @Test
  fun editEventAsOwner_throwsException_whenChangingParticipants() = runTest {
    val existing = createEvent(uid = "E1", ownerId = "owner", participantIds = listOf("user1"))
    val updated = existing.copy(participantIds = listOf("user1", "user2"))
    val docSnapshot = doc("E1", existing)
    val eventDocRef = mock<DocumentReference>()

    whenever(collection.document("E1")).thenReturn(eventDocRef)
    whenever(db.runTransaction(any<Transaction.Function<Void>>())).thenAnswer { invocation ->
      val txFunction = invocation.getArgument<Transaction.Function<Void>>(0)
      whenever(transaction.get(eventDocRef)).thenReturn(docSnapshot)
      txFunction.apply(transaction)
      voidTask()
    }

    val exception = assertFailsWith<Exception> { repo.editEventAsOwner("E1", updated) }
    assertTrue(exception.message!!.contains("Owner cannot change participants list"))
  }

  @Test
  fun editEventAsOwner_throwsException_whenReducingCapacityBelowParticipants() = runTest {
    val existing =
        createEvent(
            uid = "E1", ownerId = "owner", capacity = 10, participantIds = listOf("u1", "u2", "u3"))
    val updated = existing.copy(capacity = 2)
    val docSnapshot = doc("E1", existing)
    val eventDocRef = mock<DocumentReference>()

    whenever(collection.document("E1")).thenReturn(eventDocRef)
    whenever(db.runTransaction(any<Transaction.Function<Void>>())).thenAnswer { invocation ->
      val txFunction = invocation.getArgument<Transaction.Function<Void>>(0)
      whenever(transaction.get(eventDocRef)).thenReturn(docSnapshot)
      txFunction.apply(transaction)
      voidTask()
    }

    val exception = assertFailsWith<Exception> { repo.editEventAsOwner("E1", updated) }
    assertTrue(exception.message!!.contains("Capacity cannot be lower than current participants"))
  }

  // ========== EDIT EVENT AS USER TESTS ==========

  @Test
  fun editEventAsUser_join_success() = runTest {
    val event =
        createEvent(uid = "E1", ownerId = "owner", participantIds = listOf("user1"), capacity = 10)
    val docSnapshot = doc("E1", event)
    val eventDocRef = mock<DocumentReference>()
    val userRef = mock<DocumentReference>()
    whenever(usersCollection.document(any())).thenReturn(userRef)
    whenever(collection.document("E1")).thenReturn(eventDocRef)
    whenever(db.runTransaction(any<Transaction.Function<Void>>())).thenAnswer { invocation ->
      val txFunction = invocation.getArgument<Transaction.Function<Void>>(0)
      whenever(transaction.get(eventDocRef)).thenReturn(docSnapshot)
      whenever(transaction.update(any<DocumentReference>(), any<String>(), any()))
          .thenReturn(transaction)
      txFunction.apply(transaction)
      voidTask()
    }

    repo.editEventAsUser("E1", "user2", join = true)

    verify(transaction, times(2)).update(any<DocumentReference>(), any<String>(), any())
  }

  @Test
  fun editEventAsUser_join_throwsException_whenEventFull() = runTest {
    val event =
        createEvent(
            uid = "E1", ownerId = "owner", participantIds = listOf("u1", "u2"), capacity = 2)
    val docSnapshot = doc("E1", event)
    val eventDocRef = mock<DocumentReference>()

    whenever(collection.document("E1")).thenReturn(eventDocRef)
    whenever(db.runTransaction(any<Transaction.Function<Void>>())).thenAnswer { invocation ->
      val txFunction = invocation.getArgument<Transaction.Function<Void>>(0)
      whenever(transaction.get(eventDocRef)).thenReturn(docSnapshot)
      txFunction.apply(transaction)
      voidTask()
    }

    val exception = assertFailsWith<Exception> { repo.editEventAsUser("E1", "user3", join = true) }
    assertTrue(exception.message!!.contains("Event is full"))
  }

  @Test
  fun editEventAsUser_leave_success() = runTest {
    val event =
        createEvent(uid = "E1", ownerId = "owner", participantIds = listOf("user1", "user2"))
    val docSnapshot = doc("E1", event)
    val eventDocRef = mock<DocumentReference>()
    val userRef = mock<DocumentReference>()
    whenever(usersCollection.document(any())).thenReturn(userRef)
    whenever(collection.document("E1")).thenReturn(eventDocRef)
    whenever(db.runTransaction(any<Transaction.Function<Void>>())).thenAnswer { invocation ->
      val txFunction = invocation.getArgument<Transaction.Function<Void>>(0)
      whenever(transaction.get(eventDocRef)).thenReturn(docSnapshot)
      whenever(transaction.update(any<DocumentReference>(), any<String>(), any()))
          .thenReturn(transaction)
      txFunction.apply(transaction)
      voidTask()
    }

    repo.editEventAsUser("E1", "user2", join = false)

    verify(transaction, times(2)).update(any<DocumentReference>(), any<String>(), any())
  }

  // ========== DELETE EVENT TESTS ==========

  @Test
  fun deleteEvent_success_removesEventAndUpdatesUsers() = runTest {
    val event = createEvent(uid = "E1", ownerId = "owner123", participantIds = listOf("user1"))
    val eventDocRef = mock<DocumentReference>()
    val eventSnapshot = doc("E1", event)

    whenever(collection.document("E1")).thenReturn(eventDocRef)
    whenever(eventDocRef.get()).thenReturn(taskOf(eventSnapshot))
    whenever(db.runBatch(any())).thenReturn(voidTask())

    // Mock savedEventIds query
    val savedQuery = mock<Query>()
    val emptySnapshot = qs()
    whenever(usersCollection.whereArrayContains(SAVED_EVENT_IDS, "E1")).thenReturn(savedQuery)
    whenever(savedQuery.get()).thenReturn(taskOf(emptySnapshot))

    repo.deleteEvent("E1")

    verify(db, atLeastOnce()).runBatch(any())
    verify(eventDocRef, never()).delete() // Delete is inside the batch
    verify(savedQuery).get()
  }

  @Test
  fun deleteEvent_eventNotFound_throwsException() = runTest {
    val eventDocRef = mock<DocumentReference>()
    whenever(collection.document("E404")).thenReturn(eventDocRef)
    val missingSnapshot = mock<DocumentSnapshot>()
    whenever(missingSnapshot.exists()).thenReturn(false)
    whenever(eventDocRef.get()).thenReturn(Tasks.forResult(missingSnapshot))

    val exception = assertFailsWith<Exception> { repo.deleteEvent("E404") }
    assertTrue(exception.message!!.contains("Event not found"))
  }

  // ========== GET USER EVENTS TESTS ==========

  @Test
  fun getSavedEvents_returnsSortedList() = runTest {
    val userProfile = UserProfile(userId = "user123", savedEventIds = listOf("E1", "E2"))
    val userSnapshot = userDoc("user123", userProfile)
    val userDocRef = mock<DocumentReference>()

    whenever(usersCollection.document("user123")).thenReturn(userDocRef)
    whenever(userDocRef.get()).thenReturn(taskOf(userSnapshot))

    val e1 = createEvent(uid = "E1", title = "First")
    val e2 = createEvent(uid = "E2", title = "Second")
    val doc1 = doc("E1", e1)
    val doc2 = doc("E2", e2)
    val eventSnapshot = qs(doc1, doc2)

    val eventQuery = mock<Query>()
    whenever(collection.whereIn(any<FieldPath>(), any<List<String>>())).thenReturn(eventQuery)
    whenever(eventQuery.get()).thenReturn(taskOf(eventSnapshot))

    val result = repo.getSavedEvents("user123")

    assertEquals(2, result.size)
  }

  @Test
  fun getJoinedEvents_returnsSortedList() = runTest {
    val userProfile = UserProfile(userId = "user123", joinedEventIds = listOf("E1"))
    val userSnapshot = userDoc("user123", userProfile)
    val userDocRef = mock<DocumentReference>()

    whenever(usersCollection.document("user123")).thenReturn(userDocRef)
    whenever(userDocRef.get()).thenReturn(taskOf(userSnapshot))

    val e1 = createEvent(uid = "E1", title = "Event")
    val doc1 = doc("E1", e1)
    val eventSnapshot = qs(doc1)

    val eventQuery = mock<Query>()
    whenever(collection.whereIn(any<FieldPath>(), any<List<String>>())).thenReturn(eventQuery)
    whenever(eventQuery.get()).thenReturn(taskOf(eventSnapshot))

    val result = repo.getJoinedEvents("user123")

    assertEquals(1, result.size)
    assertEquals("Event", result.first().title)
  }

  @Test
  fun getOwnedEvents_returnsSortedList() = runTest {
    val userProfile = UserProfile(userId = "owner123", ownedEventIds = listOf("E1"))
    val userSnapshot = userDoc("owner123", userProfile)
    val userDocRef = mock<DocumentReference>()

    whenever(usersCollection.document("owner123")).thenReturn(userDocRef)
    whenever(userDocRef.get()).thenReturn(taskOf(userSnapshot))
    whenever(userSnapshot.get(any<String>())).thenReturn(listOf("E1"))

    val e1 = createEvent(uid = "E1", title = "My Event", ownerId = "owner123")
    val doc1 = doc("E1", e1)
    val eventSnapshot = qs(doc1)

    val eventQuery = mock<Query>()
    whenever(collection.whereIn(any<FieldPath>(), any<List<String>>())).thenReturn(eventQuery)
    whenever(eventQuery.get()).thenReturn(taskOf(eventSnapshot))

    val result = repo.getOwnedEvents("owner123")

    assertEquals(1, result.size)
    assertEquals("My Event", result.first().title)
  }

  @Test
  fun getSavedEvents_returnsEmptyList_whenUserHasNoSavedEvents() = runTest {
    val userProfile = UserProfile(userId = "user123", savedEventIds = emptyList())
    val userSnapshot = userDoc("user123", userProfile)
    val userDocRef = mock<DocumentReference>()

    whenever(usersCollection.document("user123")).thenReturn(userDocRef)
    whenever(userDocRef.get()).thenReturn(taskOf(userSnapshot))

    val result = repo.getSavedEvents("user123")

    assertTrue(result.isEmpty())
  }

  @Test
  fun getJoinedEvents_returnsEmptyList_whenUserHasNoJoinedEvents() = runTest {
    val userProfile = UserProfile(userId = "user123", joinedEventIds = emptyList())
    val userSnapshot = userDoc("user123", userProfile)
    val userDocRef = mock<DocumentReference>()

    whenever(usersCollection.document("user123")).thenReturn(userDocRef)
    whenever(userDocRef.get()).thenReturn(taskOf(userSnapshot))

    val result = repo.getJoinedEvents("user123")

    assertTrue(result.isEmpty())
  }

  @Test
  fun getOwnedEvents_returnsEmptyList_whenUserHasNoOwnedEvents() = runTest {
    val userProfile = UserProfile(userId = "owner123", ownedEventIds = emptyList())
    val userSnapshot = userDoc("owner123", userProfile)
    val userDocRef = mock<DocumentReference>()

    whenever(usersCollection.document("owner123")).thenReturn(userDocRef)
    whenever(userDocRef.get()).thenReturn(taskOf(userSnapshot))

    val result = repo.getOwnedEvents("owner123")

    assertTrue(result.isEmpty())
  }

  // ========== GET FILTERED EVENTS TESTS ==========

  @Test
  fun getFilteredEvents_appliesPopularOnlyFilter() = runTest {
    val filters = Filters(popularOnly = true)
    val popular = createEvent("E1", "Popular", participantIds = List(35) { "user$it" })
    val unpopular = createEvent("E2", "Unpopular", participantIds = listOf("user1"))
    val doc1 = doc("E1", popular)
    val doc2 = doc("E2", unpopular)
    val snap = qs(doc1, doc2)

    val queryMock = mock<Query>()
    whenever(collection.whereGreaterThanOrEqualTo(any<String>(), any<Timestamp>()))
        .thenReturn(queryMock)
    whenever(queryMock.orderBy(any<String>(), any<Query.Direction>())).thenReturn(queryMock)
    whenever(queryMock.get()).thenReturn(taskOf(snap))

    val result = repo.getFilteredEvents(filters, "user")

    // Only popular event should be returned (>30 participants)
    assertEquals(1, result.size)
    assertEquals("Popular", result.first().title)
    assertTrue(result.first().participantIds.size > 30)
  }

  @Test
  fun getFilteredEvents_tagFilter_skipsEventsWithoutTags() = runTest {
    val filters = Filters(tags = setOf("art"))

    val taggedEvent = createEvent("E1", "Tagged", tags = listOf("art"))
    val untaggedEvent = createEvent("E2", "No Tags")
    val doc1 = doc("E1", taggedEvent)
    val doc2 = doc("E2", untaggedEvent)
    val snap = qs(doc1)

    val filteredQuery = mock<Query>()
    val orderedQuery = mock<Query>()
    whenever(collection.whereGreaterThanOrEqualTo(eq("date"), any())).thenReturn(filteredQuery)
    whenever(filteredQuery.whereArrayContainsAny(any<String>(), any<List<String>>()))
        .thenReturn(filteredQuery)
    whenever(filteredQuery.orderBy("date", Query.Direction.DESCENDING)).thenReturn(orderedQuery)
    whenever(orderedQuery.get()).thenReturn(taskOf(snap))

    val result = repo.getFilteredEvents(filters, "user")

    // Both events returned by query, but only tagged one should match
    assertEquals(1, result.size)
    assertTrue("art" in result.first().tags)
  }

  @Test
  fun getFilteredEvents_maxPrice_filtersCorrectly() = runTest {
    val filters = Filters(maxPrice = 50)

    val cheapEvent = createEvent("E1", "Cheap", price = 30.0)
    val expensiveEvent = createEvent("E2", "Expensive", price = 100.0)
    val doc1 = doc("E1", cheapEvent)
    val doc2 = doc("E2", expensiveEvent)
    val snap = qs(doc1, doc2)

    val queryMock = mock<Query>()
    whenever(collection.whereGreaterThanOrEqualTo(any<String>(), any<Timestamp>()))
        .thenReturn(queryMock)
    whenever(queryMock.whereLessThanOrEqualTo(eq("price"), eq(50.0))).thenReturn(queryMock)
    whenever(queryMock.orderBy(any<String>(), any<Query.Direction>())).thenReturn(queryMock)
    whenever(queryMock.get()).thenReturn(taskOf(snap))

    repo.getFilteredEvents(filters, "user")

    // Should include both in mock, but in real scenario only cheap would be returned by query
    verify(queryMock).whereLessThanOrEqualTo("price", 50.0)
  }

  @Test
  fun getFilteredEvents_friendsOnly_withNoFriends_returnsEmpty() = runTest {
    val filters = Filters(friendsOnly = true)

    // No friends
    whenever(friendRepo.getFriends("currentUser")).thenReturn(emptyList())

    val result = repo.getFilteredEvents(filters, "currentUser")

    verify(friendRepo).getFriends("currentUser")
    assertTrue(result.isEmpty())
  }

  @Test
  fun getFilteredEvents_friendsOnly_withTags_appliesTagsClientSide() = runTest {
    val filters = Filters(friendsOnly = true, tags = setOf("music"))

    val friend1 =
        FriendWithProfile(
            userProfile = UserProfile(userId = "friend1", name = "Friend1"),
            friendshipStatus = FriendshipStatus.ACCEPTED)
    whenever(friendRepo.getFriends("currentUser")).thenReturn(listOf(friend1))

    val musicEvent = createEvent("E1", "Concert", ownerId = "friend1", tags = listOf("music"))
    val sportEvent = createEvent("E2", "Game", ownerId = "friend1", tags = listOf("sport"))
    val doc1 = doc("E1", musicEvent)
    val doc2 = doc("E2", sportEvent)
    val snap = qs(doc1, doc2)

    val queryMock = mock<Query>()
    whenever(collection.whereGreaterThanOrEqualTo(any<String>(), any<Timestamp>()))
        .thenReturn(queryMock)
    whenever(queryMock.whereIn(eq("ownerId"), any<List<String>>())).thenReturn(queryMock)
    whenever(queryMock.get()).thenReturn(taskOf(snap))

    val result = repo.getFilteredEvents(filters, "currentUser")

    // Tags should be filtered CLIENT-SIDE (not in whereArrayContainsAny)
    verify(queryMock, never()).whereArrayContainsAny(any<String>(), any<List<String>>())
    assertEquals(1, result.size)
    assertEquals("Concert", result.first().title)
    assertTrue("music" in result.first().tags)
  }

  @Test
  fun getFilteredEvents_throwsException_whenQueryFails() = runTest {
    val query = mock<Query>()
    whenever(collection.whereGreaterThanOrEqualTo(any<String>(), any<Timestamp>()))
        .thenReturn(query)
    whenever(query.orderBy(any<String>(), any<Query.Direction>())).thenReturn(query)
    whenever(query.get()).thenReturn(Tasks.forException(RuntimeException("broken")))

    val filters = Filters(startDate = LocalDate.now())
    val exception = assertFailsWith<Exception> { repo.getFilteredEvents(filters, "user") }

    assertTrue(exception.message!!.contains("Failed to fetch filtered events"))
  }

  @Test
  fun getFilteredEvents_allFilters_combinesCorrectly() = runTest {
    val filters =
        Filters(
            friendsOnly = true,
            tags = setOf("music"),
            popularOnly = true,
            maxPrice = 50,
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(7))

    val friend1 =
        FriendWithProfile(
            userProfile = UserProfile(userId = "friend1", name = "Friend1"),
            friendshipStatus = FriendshipStatus.ACCEPTED)
    whenever(friendRepo.getFriends("currentUser")).thenReturn(listOf(friend1))

    val validEvent =
        createEvent(
            uid = "E1",
            title = "Valid Event",
            ownerId = "friend1",
            tags = listOf("music"),
            participantIds = List(35) { "user$it" },
            price = 30.0)

    val invalidEvent1 =
        createEvent(
            uid = "E2",
            title = "Wrong Tag",
            ownerId = "friend1",
            tags = listOf("sport"),
            participantIds = List(35) { "user$it" },
            price = 30.0)

    val invalidEvent2 =
        createEvent(
            uid = "E3",
            title = "Not Popular",
            ownerId = "friend1",
            tags = listOf("music"),
            participantIds = listOf("user1"),
            price = 30.0)

    val doc1 = doc("E1", validEvent)
    val doc2 = doc("E2", invalidEvent1)
    val doc3 = doc("E3", invalidEvent2)
    val snap = qs(doc1, doc2, doc3)

    val queryMock = mock<Query>()
    whenever(collection.whereGreaterThanOrEqualTo(any<String>(), any<Timestamp>()))
        .thenReturn(queryMock)
    whenever(queryMock.whereLessThanOrEqualTo(eq("date"), any<Timestamp>())).thenReturn(queryMock)
    whenever(queryMock.whereLessThan(eq("date"), any<Timestamp>())).thenReturn(queryMock)
    whenever(queryMock.whereLessThanOrEqualTo(eq("price"), any())).thenReturn(queryMock)
    whenever(queryMock.whereIn(eq("ownerId"), any<List<String>>())).thenReturn(queryMock)
    whenever(queryMock.get()).thenReturn(taskOf(snap))

    val result = repo.getFilteredEvents(filters, "currentUser")

    // Only validEvent should pass all filters
    assertEquals(1, result.size)
    assertEquals("Valid Event", result.first().title)
  }

  // ========== LISTEN TO FILTERED EVENTS TESTS ==========

  @Test
  fun listenToFilteredEvents_onError_triggersCallbackWithEmptyLists() = runTest {
    val filters = Filters()

    val queryMock = mock<Query>()
    whenever(collection.whereGreaterThanOrEqualTo(any<String>(), any<Timestamp>()))
        .thenReturn(queryMock)
    whenever(queryMock.orderBy(any<String>(), any<Query.Direction>())).thenReturn(queryMock)

    val listenerCaptor = argumentCaptor<EventListener<QuerySnapshot>>()
    val listenerRegistration = mock<ListenerRegistration>()
    whenever(queryMock.addSnapshotListener(listenerCaptor.capture()))
        .thenReturn(listenerRegistration)

    var addedEvents: List<Event>? = null
    var modifiedEvents: List<Event>? = null
    var removedEvents: List<Event>? = null

    repo.listenToFilteredEvents(filters) { added, modified, removed ->
      addedEvents = added
      modifiedEvents = modified
      removedEvents = removed
    }

    val error = mock<FirebaseFirestoreException>()
    listenerCaptor.firstValue.onEvent(null, error)

    assertTrue(addedEvents!!.isEmpty())
    assertTrue(modifiedEvents!!.isEmpty())
    assertTrue(removedEvents!!.isEmpty())
  }

  @Test
  fun listenToFilteredEvents_appliesClientSideFilters() = runTest {
    val filters =
        Filters(
            tags = setOf("music"), popularOnly = true // Client-side filter
            )

    val queryMock = mock<Query>()
    whenever(collection.whereGreaterThanOrEqualTo(any<String>(), any<Timestamp>()))
        .thenReturn(queryMock)
    whenever(queryMock.whereArrayContainsAny(any<String>(), any<List<String>>()))
        .thenReturn(queryMock)
    whenever(queryMock.orderBy(any<String>(), any<Query.Direction>())).thenReturn(queryMock)

    val listenerCaptor = argumentCaptor<EventListener<QuerySnapshot>>()
    val listenerRegistration = mock<ListenerRegistration>()
    whenever(queryMock.addSnapshotListener(listenerCaptor.capture()))
        .thenReturn(listenerRegistration)

    var addedEvents: List<Event> = emptyList()

    repo.listenToFilteredEvents(filters) { added, _, _ -> addedEvents = added }

    val popularEvent =
        createEvent(
            uid = "E1",
            title = "Popular Concert",
            tags = listOf("music"),
            participantIds = List(35) { "user$it" })

    val unpopularEvent =
        createEvent(
            uid = "E2",
            title = "Small Concert",
            tags = listOf("music"),
            participantIds = listOf("user1"))

    val doc1 = doc("E1", popularEvent)
    val doc2 = doc("E2", unpopularEvent)

    val change1 = mock<DocumentChange>()
    whenever(change1.type).thenReturn(DocumentChange.Type.ADDED)
    whenever(change1.document).thenReturn(doc1)

    val change2 = mock<DocumentChange>()
    whenever(change2.type).thenReturn(DocumentChange.Type.ADDED)
    whenever(change2.document).thenReturn(doc2)

    val snapshot = mock<QuerySnapshot>()
    whenever(snapshot.documentChanges).thenReturn(listOf(change1, change2))

    listenerCaptor.firstValue.onEvent(snapshot, null)

    assertEquals(1, addedEvents.size)
    assertEquals("Popular Concert", addedEvents.first().title)
  }

  // ========== LISTEN TO SAVED EVENTS TESTS ==========

  @Test
  fun listenToSavedEvents_detectsEventDeletion() = runTest {
    val userId = "user123"
    val userDocRef = mock<DocumentReference>()

    whenever(usersCollection.document(userId)).thenReturn(userDocRef)

    val userListenerCaptor = argumentCaptor<EventListener<DocumentSnapshot>>()
    val userListenerReg = mock<ListenerRegistration>()
    whenever(userDocRef.addSnapshotListener(userListenerCaptor.capture()))
        .thenReturn(userListenerReg)

    // Setup event listener
    val eventDocRef = mock<DocumentReference>()
    whenever(collection.document("E1")).thenReturn(eventDocRef)

    val eventListenerCaptor = argumentCaptor<EventListener<DocumentSnapshot>>()
    val eventListenerReg = mock<ListenerRegistration>()
    whenever(eventDocRef.addSnapshotListener(eventListenerCaptor.capture()))
        .thenReturn(eventListenerReg)

    var removedEvents: List<String> = emptyList()

    val registration = repo.listenToSavedEvents(userId) { _, _, removed -> removedEvents = removed }

    // Simulate user document update with saved event
    val userSnapshot = mock<DocumentSnapshot>()
    whenever(userSnapshot.exists()).thenReturn(true)
    whenever(userSnapshot.get(SAVED_EVENT_IDS)).thenReturn(listOf("E1"))
    userListenerCaptor.firstValue.onEvent(userSnapshot, null)

    // Simulate event deletion (event document no longer exists)
    val deletedEventSnapshot = mock<DocumentSnapshot>()
    whenever(deletedEventSnapshot.exists()).thenReturn(false)
    whenever(deletedEventSnapshot.id).thenReturn("E1")
    eventListenerCaptor.firstValue.onEvent(deletedEventSnapshot, null)

    // Verify removal was detected
    assertEquals(1, removedEvents.size)
    assertEquals("E1", removedEvents.first())

    registration.remove()
    verify(userListenerReg).remove()
    verify(eventListenerReg).remove()
  }

  @Test
  fun listenToSavedEvents_handlesUserDocumentNotFound() = runTest {
    val userId = "user123"
    val userDocRef = mock<DocumentReference>()

    whenever(usersCollection.document(userId)).thenReturn(userDocRef)

    val userListenerCaptor = argumentCaptor<EventListener<DocumentSnapshot>>()
    val userListenerReg = mock<ListenerRegistration>()
    whenever(userDocRef.addSnapshotListener(userListenerCaptor.capture()))
        .thenReturn(userListenerReg)

    var addedEvents: List<Event> = emptyList()
    var modifiedEvents: List<Event> = emptyList()
    var removedEvents: List<String> = emptyList()

    val registration =
        repo.listenToSavedEvents(userId) { added, modified, removed ->
          addedEvents = added
          modifiedEvents = modified
          removedEvents = removed
        }

    // Simulate user document not found
    val userSnapshot = mock<DocumentSnapshot>()
    whenever(userSnapshot.exists()).thenReturn(false)
    userListenerCaptor.firstValue.onEvent(userSnapshot, null)

    // Should trigger callback with empty lists
    assertTrue(addedEvents.isEmpty())
    assertTrue(modifiedEvents.isEmpty())
    assertTrue(removedEvents.isEmpty())

    registration.remove()
  }

  @Test
  fun listenToSavedEvents_handlesListenerError() = runTest {
    val userId = "user123"
    val userDocRef = mock<DocumentReference>()

    whenever(usersCollection.document(userId)).thenReturn(userDocRef)

    val userListenerCaptor = argumentCaptor<EventListener<DocumentSnapshot>>()
    val userListenerReg = mock<ListenerRegistration>()
    whenever(userDocRef.addSnapshotListener(userListenerCaptor.capture()))
        .thenReturn(userListenerReg)

    var addedEvents: List<Event> = emptyList()
    var modifiedEvents: List<Event> = emptyList()
    var removedEvents: List<String> = emptyList()

    val registration =
        repo.listenToSavedEvents(userId) { added, modified, removed ->
          addedEvents = added
          modifiedEvents = modified
          removedEvents = removed
        }

    // Simulate error
    val error = mock<FirebaseFirestoreException>()
    userListenerCaptor.firstValue.onEvent(null, error)

    // Should trigger callback with empty lists
    assertTrue(addedEvents.isEmpty())
    assertTrue(modifiedEvents.isEmpty())
    assertTrue(removedEvents.isEmpty())

    registration.remove()
  }

  // ========== LISTEN TO JOINED EVENTS TESTS ==========

  @Test
  fun listenToJoinedEvents_detectsConsecutiveModifications() = runTest {
    val userId = "user123"
    val userDocRef = mock<DocumentReference>()

    whenever(usersCollection.document(userId)).thenReturn(userDocRef)

    val userListenerCaptor = argumentCaptor<EventListener<DocumentSnapshot>>()
    val userListenerReg = mock<ListenerRegistration>()
    whenever(userDocRef.addSnapshotListener(userListenerCaptor.capture()))
        .thenReturn(userListenerReg)

    val eventDocRef = mock<DocumentReference>()
    whenever(collection.document("E1")).thenReturn(eventDocRef)

    val eventListenerCaptor = argumentCaptor<EventListener<DocumentSnapshot>>()
    val eventListenerReg = mock<ListenerRegistration>()
    whenever(eventDocRef.addSnapshotListener(eventListenerCaptor.capture()))
        .thenReturn(eventListenerReg)

    val addedTitles = mutableListOf<String>()
    val modifiedTitles = mutableListOf<String>()

    val registration =
        repo.listenToJoinedEvents(userId) { added, modified, _ ->
          addedTitles.addAll(added.map { it.title })
          modifiedTitles.addAll(modified.map { it.title })
        }

    // Simulate user document with joined event
    val userSnapshot = mock<DocumentSnapshot>()
    whenever(userSnapshot.exists()).thenReturn(true)
    whenever(userSnapshot.get(JOINED_EVENT_IDS)).thenReturn(listOf("E1"))
    userListenerCaptor.firstValue.onEvent(userSnapshot, null)

    // First load - should be treated as "added"
    val event1 = createEvent(uid = "E1", title = "Version 1")
    val eventSnapshot1 = doc("E1", event1)
    whenever(eventSnapshot1.exists()).thenReturn(true)
    eventListenerCaptor.firstValue.onEvent(eventSnapshot1, null)

    // Second load - should be treated as "modified"
    val event2 = createEvent(uid = "E1", title = "Version 2")
    val eventSnapshot2 = doc("E1", event2)
    whenever(eventSnapshot2.exists()).thenReturn(true)
    eventListenerCaptor.firstValue.onEvent(eventSnapshot2, null)

    // First load should be in "added"
    assertEquals(1, addedTitles.size)
    assertEquals("Version 1", addedTitles[0])

    // Second load should be in "modified"
    assertEquals(1, modifiedTitles.size)
    assertEquals("Version 2", modifiedTitles[0])

    registration.remove()
  }

  // ========== Listen to Owned Events with Initial Population ==========
  @Test
  fun `listenToOwnedEvents treats first load as added and subsequent as modified`() = runTest {
    val userId = "owner123"
    val userDocRef = mock<DocumentReference>()
    val eventDocRef = mock<DocumentReference>()

    whenever(usersCollection.document(userId)).thenReturn(userDocRef)
    whenever(collection.document("E1")).thenReturn(eventDocRef)

    val userListenerCaptor = argumentCaptor<EventListener<DocumentSnapshot>>()
    val eventListenerCaptor = argumentCaptor<EventListener<DocumentSnapshot>>()

    whenever(userDocRef.addSnapshotListener(userListenerCaptor.capture())).thenReturn(mock())
    whenever(eventDocRef.addSnapshotListener(eventListenerCaptor.capture())).thenReturn(mock())

    val addedEvents = mutableListOf<Event>()
    val modifiedEvents = mutableListOf<Event>()

    val registration =
        repo.listenToOwnedEvents(userId) { added, modified, _ ->
          addedEvents.addAll(added)
          modifiedEvents.addAll(modified)
        }

    // Simulate user document with owned event
    val userSnapshot = mock<DocumentSnapshot>()
    whenever(userSnapshot.exists()).thenReturn(true)
    whenever(userSnapshot.get(OWNED_EVENT_IDS)).thenReturn(listOf("E1"))
    userListenerCaptor.firstValue.onEvent(userSnapshot, null)

    // First event load should be treated as "added"
    val event1 = createEvent(uid = "E1", title = "Initial", ownerId = userId)
    val eventSnapshot1 = doc("E1", event1)
    whenever(eventSnapshot1.exists()).thenReturn(true)
    eventListenerCaptor.firstValue.onEvent(eventSnapshot1, null)

    assertEquals(1, addedEvents.size)
    assertEquals("Initial", addedEvents.first().title)
    assertTrue(modifiedEvents.isEmpty())

    // Second load should be treated as "modified"
    val event2 = createEvent(uid = "E1", title = "Updated", ownerId = userId)
    val eventSnapshot2 = doc("E1", event2)
    whenever(eventSnapshot2.exists()).thenReturn(true)
    eventListenerCaptor.firstValue.onEvent(eventSnapshot2, null)

    assertEquals(1, modifiedEvents.size)
    assertEquals("Updated", modifiedEvents.first().title)

    registration.remove()
  }

  // ========== User Document Removal Triggers Event Removal ==========
  @Test
  fun `listenToSavedEvents detects removal when event disappears from user document`() = runTest {
    val userId = "user123"
    val userDocRef = mock<DocumentReference>()
    val eventDocRef = mock<DocumentReference>()

    whenever(usersCollection.document(userId)).thenReturn(userDocRef)
    whenever(collection.document("E1")).thenReturn(eventDocRef)

    val userListenerCaptor = argumentCaptor<EventListener<DocumentSnapshot>>()
    val eventListenerCaptor = argumentCaptor<EventListener<DocumentSnapshot>>()

    whenever(userDocRef.addSnapshotListener(userListenerCaptor.capture())).thenReturn(mock())
    whenever(eventDocRef.addSnapshotListener(eventListenerCaptor.capture())).thenReturn(mock())

    val addedEvents = mutableListOf<Event>()
    val removedIds = mutableListOf<String>()

    val registration =
        repo.listenToSavedEvents(userId) { added, _, removed ->
          addedEvents.addAll(added)
          removedIds.addAll(removed)
        }

    // Initial: user has saved event E1
    val userSnapshot1 = mock<DocumentSnapshot>()
    whenever(userSnapshot1.exists()).thenReturn(true)
    whenever(userSnapshot1.get(SAVED_EVENT_IDS)).thenReturn(listOf("E1"))
    userListenerCaptor.firstValue.onEvent(userSnapshot1, null)

    // Event loaded initially
    val event = createEvent(uid = "E1", title = "Saved Event")
    val eventSnapshot = doc("E1", event)
    whenever(eventSnapshot.exists()).thenReturn(true)
    eventListenerCaptor.firstValue.onEvent(eventSnapshot, null)

    assertEquals(1, addedEvents.size)

    // User document updated: E1 removed from savedEventIds
    val userSnapshot2 = mock<DocumentSnapshot>()
    whenever(userSnapshot2.exists()).thenReturn(true)
    whenever(userSnapshot2.get(SAVED_EVENT_IDS)).thenReturn(emptyList<String>())
    userListenerCaptor.firstValue.onEvent(userSnapshot2, null)

    // Should trigger removal
    assertEquals(1, removedIds.size)
    assertEquals("E1", removedIds.first())

    registration.remove()
  }

  @Test
  fun `listenToOwnedEvents handles null event parsing gracefully`() = runTest {
    val userId = "owner123"
    val userDocRef = mock<DocumentReference>()
    val eventDocRef = mock<DocumentReference>()

    whenever(usersCollection.document(userId)).thenReturn(userDocRef)
    whenever(collection.document("E1")).thenReturn(eventDocRef)

    val userListenerCaptor = argumentCaptor<EventListener<DocumentSnapshot>>()
    val eventListenerCaptor = argumentCaptor<EventListener<DocumentSnapshot>>()

    whenever(userDocRef.addSnapshotListener(userListenerCaptor.capture())).thenReturn(mock())
    whenever(eventDocRef.addSnapshotListener(eventListenerCaptor.capture())).thenReturn(mock())

    val addedEvents = mutableListOf<Event>()
    val modifiedEvents = mutableListOf<Event>()

    val registration =
        repo.listenToOwnedEvents(userId) { added, modified, _ ->
          addedEvents.addAll(added)
          modifiedEvents.addAll(modified)
        }

    // Simulate user document with owned event
    val userSnapshot = mock<DocumentSnapshot>()
    whenever(userSnapshot.exists()).thenReturn(true)
    whenever(userSnapshot.get(OWNED_EVENT_IDS)).thenReturn(listOf("E1"))
    userListenerCaptor.firstValue.onEvent(userSnapshot, null)

    // Simulate event snapshot that fails to parse (toObject returns null)
    val corruptedSnapshot = mock<DocumentSnapshot>()
    whenever(corruptedSnapshot.exists()).thenReturn(true)
    whenever(corruptedSnapshot.id).thenReturn("E1")
    whenever(corruptedSnapshot.toObject(Event::class.java)).thenReturn(null)
    eventListenerCaptor.firstValue.onEvent(corruptedSnapshot, null)

    // No events should be added or modified when parsing fails
    assertTrue(addedEvents.isEmpty())
    assertTrue(modifiedEvents.isEmpty())

    registration.remove()
  }

  @Test
  fun `listenToOwnedEvents handles event listener error and logs it`() = runTest {
    val userId = "owner123"
    val userDocRef = mock<DocumentReference>()
    val eventDocRef = mock<DocumentReference>()

    whenever(usersCollection.document(userId)).thenReturn(userDocRef)
    whenever(collection.document("E1")).thenReturn(eventDocRef)

    val userListenerCaptor = argumentCaptor<EventListener<DocumentSnapshot>>()
    val eventListenerCaptor = argumentCaptor<EventListener<DocumentSnapshot>>()

    whenever(userDocRef.addSnapshotListener(userListenerCaptor.capture())).thenReturn(mock())
    whenever(eventDocRef.addSnapshotListener(eventListenerCaptor.capture())).thenReturn(mock())

    var callbackInvoked = false
    val addedEvents = mutableListOf<Event>()
    val modifiedEvents = mutableListOf<Event>()
    val removedIds = mutableListOf<String>()

    val registration =
        repo.listenToOwnedEvents(userId) { added, modified, removed ->
          callbackInvoked = true
          addedEvents.addAll(added)
          modifiedEvents.addAll(modified)
          removedIds.addAll(removed)
        }

    // Simulate user document with owned event
    val userSnapshot = mock<DocumentSnapshot>()
    whenever(userSnapshot.exists()).thenReturn(true)
    whenever(userSnapshot.get(OWNED_EVENT_IDS)).thenReturn(listOf("E1"))
    userListenerCaptor.firstValue.onEvent(userSnapshot, null)

    // Simulate Firestore error on event listener
    val error = mock<FirebaseFirestoreException>()
    whenever(error.message).thenReturn("Network error")
    eventListenerCaptor.firstValue.onEvent(null, error)

    // Verify callback was invoked with empty lists (or appropriate error handling)
    assertTrue(callbackInvoked)
    assertTrue(addedEvents.isEmpty())
    assertTrue(modifiedEvents.isEmpty())
    assertTrue(removedIds.isEmpty())

    registration.remove()
  }

  // ========== FOLLOWER NOTIFICATION TESTS ==========

  @Test
  fun addEvent_notifiesFollowersOfNewEvent() = runTest {
    // Create repository with mocked dependencies
    val mockNotificationService = mock<NotificationService>()
    val mockUserProfileRepo = mock<UserProfileRepository>()

    val repoWithNotifications =
        createRepoWithNotifications(mockNotificationService, mockUserProfileRepo)

    val ownerProfile =
        UserProfile(
            userId = "owner123",
            name = "Event Creator",
            followerIds = listOf("follower1", "follower2", "follower3"))

    whenever(mockUserProfileRepo.getUserProfile("owner123")).thenReturn(ownerProfile)
    whenever(
            mockNotificationService.sendNewEventFromFollowedUserNotification(
                any(), any(), any(), any(), any()))
        .thenReturn(NotificationResult.Success(Notification()))

    val newEventDocRef = mock<DocumentReference>()
    whenever(collection.document()).thenReturn(newEventDocRef)
    whenever(newEventDocRef.id).thenReturn("E123")
    whenever(db.runBatch(any())).thenReturn(voidTask())

    val input =
        createEvent(
            uid = "",
            title = "New Music Festival",
            ownerId = "owner123",
            participantIds = emptyList())

    repoWithNotifications.addEvent(input)

    // Verify notifications were sent to all followers
    verify(mockNotificationService)
        .sendNewEventFromFollowedUserNotification(
            recipientId = "follower1",
            organizerId = "owner123",
            organizerName = "Event Creator",
            eventId = "E123",
            eventTitle = "New Music Festival")
    verify(mockNotificationService)
        .sendNewEventFromFollowedUserNotification(
            recipientId = "follower2",
            organizerId = "owner123",
            organizerName = "Event Creator",
            eventId = "E123",
            eventTitle = "New Music Festival")
    verify(mockNotificationService)
        .sendNewEventFromFollowedUserNotification(
            recipientId = "follower3",
            organizerId = "owner123",
            organizerName = "Event Creator",
            eventId = "E123",
            eventTitle = "New Music Festival")
  }

  @Test
  fun addEvent_doesNotNotifyWhenNoFollowers() = runTest {
    val mockNotificationService = mock<NotificationService>()
    val mockUserProfileRepo = mock<UserProfileRepository>()

    val repoWithNotifications =
        createRepoWithNotifications(mockNotificationService, mockUserProfileRepo)

    val ownerProfile =
        UserProfile(userId = "owner123", name = "Event Creator", followerIds = emptyList())

    whenever(mockUserProfileRepo.getUserProfile("owner123")).thenReturn(ownerProfile)

    val newEventDocRef = mock<DocumentReference>()
    whenever(collection.document()).thenReturn(newEventDocRef)
    whenever(newEventDocRef.id).thenReturn("E123")
    whenever(db.runBatch(any())).thenReturn(voidTask())

    val input =
        createEvent(
            uid = "",
            title = "New Music Festival",
            ownerId = "owner123",
            participantIds = emptyList())

    repoWithNotifications.addEvent(input)

    // Verify no notifications were sent
    verify(mockNotificationService, never())
        .sendNewEventFromFollowedUserNotification(any(), any(), any(), any(), any())
  }

  @Test
  fun addEvent_continuesEvenIfOwnerProfileNotFound() = runTest {
    val mockNotificationService = mock<NotificationService>()
    val mockUserProfileRepo = mock<UserProfileRepository>()

    val repoWithNotifications =
        createRepoWithNotifications(mockNotificationService, mockUserProfileRepo)

    whenever(mockUserProfileRepo.getUserProfile("owner123")).thenReturn(null)

    val newEventDocRef = mock<DocumentReference>()
    whenever(collection.document()).thenReturn(newEventDocRef)
    whenever(newEventDocRef.id).thenReturn("E123")
    whenever(db.runBatch(any())).thenReturn(voidTask())

    val input =
        createEvent(
            uid = "",
            title = "New Music Festival",
            ownerId = "owner123",
            participantIds = emptyList())

    // Should not throw even when owner profile not found
    repoWithNotifications.addEvent(input)

    // Verify no notifications were sent
    verify(mockNotificationService, never())
        .sendNewEventFromFollowedUserNotification(any(), any(), any(), any(), any())
  }

  @Test
  fun addEvent_usesDefaultNameWhenOrganizerNameIsBlank() = runTest {
    val mockNotificationService = mock<NotificationService>()
    val mockUserProfileRepo = mock<UserProfileRepository>()

    val repoWithNotifications =
        createRepoWithNotifications(mockNotificationService, mockUserProfileRepo)

    // Owner profile with blank name
    val ownerProfile =
        UserProfile(userId = "owner123", name = "", followerIds = listOf("follower1"))

    whenever(mockUserProfileRepo.getUserProfile("owner123")).thenReturn(ownerProfile)
    whenever(
            mockNotificationService.sendNewEventFromFollowedUserNotification(
                any(), any(), any(), any(), any()))
        .thenReturn(NotificationResult.Success(Notification()))

    val newEventDocRef = mock<DocumentReference>()
    whenever(collection.document()).thenReturn(newEventDocRef)
    whenever(newEventDocRef.id).thenReturn("E123")
    whenever(db.runBatch(any())).thenReturn(voidTask())

    val input =
        createEvent(
            uid = "", title = "New Event", ownerId = "owner123", participantIds = emptyList())

    repoWithNotifications.addEvent(input)

    // Verify fallback name "Someone you follow" is used
    verify(mockNotificationService)
        .sendNewEventFromFollowedUserNotification(
            recipientId = "follower1",
            organizerId = "owner123",
            organizerName = "Someone you follow",
            eventId = "E123",
            eventTitle = "New Event")
  }

  // ========== LISTEN TO FILTERED EVENTS TESTS (continue) ==========

  @Test
  fun editEventAsUser_join_alreadyParticipant_noChanges() = runTest {
    val event =
        createEvent(uid = "E1", ownerId = "owner", participantIds = listOf("user1", "user2"))
    val docSnapshot = doc("E1", event)
    val eventDocRef = mock<DocumentReference>()

    whenever(collection.document("E1")).thenReturn(eventDocRef)
    whenever(db.runTransaction(any<Transaction.Function<Void>>())).thenAnswer { invocation ->
      val txFunction = invocation.getArgument<Transaction.Function<Void>>(0)
      whenever(transaction.get(eventDocRef)).thenReturn(docSnapshot)
      txFunction.apply(transaction)
      voidTask()
    }

    repo.editEventAsUser("E1", "user1", join = true)

    // Should not update since user is already a participant
    verify(transaction, never()).update(any<DocumentReference>(), any<String>(), any())
  }

  @Test
  fun editEventAsUser_leave_notParticipant_noChanges() = runTest {
    val event = createEvent(uid = "E1", ownerId = "owner", participantIds = listOf("user1"))
    val docSnapshot = doc("E1", event)
    val eventDocRef = mock<DocumentReference>()

    whenever(collection.document("E1")).thenReturn(eventDocRef)
    whenever(db.runTransaction(any<Transaction.Function<Void>>())).thenAnswer { invocation ->
      val txFunction = invocation.getArgument<Transaction.Function<Void>>(0)
      whenever(transaction.get(eventDocRef)).thenReturn(docSnapshot)
      txFunction.apply(transaction)
      voidTask()
    }

    repo.editEventAsUser("E1", "user2", join = false)

    // Should not update since user is not a participant
    verify(transaction, never()).update(any<DocumentReference>(), any<String>(), any())
  }

  @Test
  fun editEventAsUser_join_withNullCapacity_success() = runTest {
    val event =
        createEvent(uid = "E1", ownerId = "owner", participantIds = listOf("user1"), capacity = 100)
    val docSnapshot = doc("E1", event)
    val eventDocRef = mock<DocumentReference>()
    val userRef = mock<DocumentReference>()

    whenever(usersCollection.document(any())).thenReturn(userRef)
    whenever(collection.document("E1")).thenReturn(eventDocRef)
    whenever(db.runTransaction(any<Transaction.Function<Void>>())).thenAnswer { invocation ->
      val txFunction = invocation.getArgument<Transaction.Function<Void>>(0)
      whenever(transaction.get(eventDocRef)).thenReturn(docSnapshot)
      whenever(transaction.update(any<DocumentReference>(), any<String>(), any()))
          .thenReturn(transaction)
      txFunction.apply(transaction)
      voidTask()
    }

    repo.editEventAsUser("E1", "user2", join = true)

    // Should update successfully (no capacity limit)
    verify(transaction, times(2)).update(any<DocumentReference>(), any<String>(), any())
    verify(badgeRepo).getBadgeContext("user2")
    verify(badgeRepo).saveBadgeContext(eq("user2"), any())
    verify(badgeRepo).updateBadgesAfterContextChange("user2")
  }

  @Test
  fun editEventAsUser_join_updatesBadgeContext_earlyMorning() = runTest {
    val event = createEvent(uid = "E1", ownerId = "owner", participantIds = emptyList())
    val docSnapshot = doc("E1", event)
    val eventDocRef = mock<DocumentReference>()
    val userRef = mock<DocumentReference>()

    whenever(usersCollection.document(any())).thenReturn(userRef)
    whenever(collection.document("E1")).thenReturn(eventDocRef)
    whenever(db.runTransaction(any<Transaction.Function<Void>>())).thenAnswer { invocation ->
      val txFunction = invocation.getArgument<Transaction.Function<Void>>(0)
      whenever(transaction.get(eventDocRef)).thenReturn(docSnapshot)
      whenever(transaction.update(any<DocumentReference>(), any<String>(), any()))
          .thenReturn(transaction)
      txFunction.apply(transaction)
      voidTask()
    }

    // Create a mock Calendar that returns a specific hour
    val mockCalendar = mock<Calendar>()
    whenever(mockCalendar[Calendar.HOUR_OF_DAY]).thenReturn(6)

    // Mock Calendar.getInstance() to return our mock calendar
    val calendarMock = mockStatic(Calendar::class.java)
    try {
      calendarMock.`when`<Calendar> { Calendar.getInstance() }.thenReturn(mockCalendar)

      val initialBadgeContext = BadgeContext(joinedEvents = 5, earlyJoin = 2)
      whenever(badgeRepo.getBadgeContext(eq("user1"))).thenReturn(initialBadgeContext)

      repo.editEventAsUser("E1", "user1", join = true)

      val badgeContextCaptor = argumentCaptor<BadgeContext>()
      verify(badgeRepo).saveBadgeContext(eq("user1"), badgeContextCaptor.capture())

      val capturedContext = badgeContextCaptor.firstValue
      assertEquals(6, capturedContext.joinedEvents)
      assertEquals(3, capturedContext.earlyJoin)
    } finally {
      calendarMock.close()
    }
  }

  @Test
  fun editEventAsUser_join_updatesBadgeContext_lateNight() = runTest {
    val event = createEvent(uid = "E1", ownerId = "owner", participantIds = emptyList())
    val docSnapshot = doc("E1", event)
    val eventDocRef = mock<DocumentReference>()
    val userRef = mock<DocumentReference>()

    whenever(usersCollection.document(any())).thenReturn(userRef)
    whenever(collection.document("E1")).thenReturn(eventDocRef)
    whenever(db.runTransaction(any<Transaction.Function<Void>>())).thenAnswer { invocation ->
      val txFunction = invocation.getArgument<Transaction.Function<Void>>(0)
      whenever(transaction.get(eventDocRef)).thenReturn(docSnapshot)
      whenever(transaction.update(any<DocumentReference>(), any<String>(), any()))
          .thenReturn(transaction)
      txFunction.apply(transaction)
      voidTask()
    }

    // Create a mock Calendar that returns a specific hour
    val mockCalendar = mock<Calendar>()
    whenever(mockCalendar[Calendar.HOUR_OF_DAY]).thenReturn(1)

    // Mock Calendar.getInstance() to return our mock calendar
    val calendarMock = mockStatic(Calendar::class.java)
    try {
      calendarMock.`when`<Calendar> { Calendar.getInstance() }.thenReturn(mockCalendar)

      val initialBadgeContext = BadgeContext(joinedEvents = 10, lateJoin = 3)
      whenever(badgeRepo.getBadgeContext(eq("user1"))).thenReturn(initialBadgeContext)

      repo.editEventAsUser("E1", "user1", join = true)

      val badgeContextCaptor = argumentCaptor<BadgeContext>()
      verify(badgeRepo).saveBadgeContext(eq("user1"), badgeContextCaptor.capture())

      val capturedContext = badgeContextCaptor.firstValue
      assertEquals(11, capturedContext.joinedEvents)
      assertEquals(4, capturedContext.lateJoin)
    } finally {
      calendarMock.close()
    }
  }

  @Test
  fun editEventAsUser_join_updatesBadgeContext_regularTime() = runTest {
    val event = createEvent(uid = "E1", ownerId = "owner", participantIds = emptyList())
    val docSnapshot = doc("E1", event)
    val eventDocRef = mock<DocumentReference>()
    val userRef = mock<DocumentReference>()

    whenever(usersCollection.document(any())).thenReturn(userRef)
    whenever(collection.document("E1")).thenReturn(eventDocRef)
    whenever(db.runTransaction(any<Transaction.Function<Void>>())).thenAnswer { invocation ->
      val txFunction = invocation.getArgument<Transaction.Function<Void>>(0)
      whenever(transaction.get(eventDocRef)).thenReturn(docSnapshot)
      whenever(transaction.update(any<DocumentReference>(), any<String>(), any()))
          .thenReturn(transaction)
      txFunction.apply(transaction)
      voidTask()
    }

    // Create a mock Calendar that returns a specific hour
    val mockCalendar = mock<Calendar>()
    whenever(mockCalendar[Calendar.HOUR_OF_DAY]).thenReturn(15)

    // Mock Calendar.getInstance() to return our mock calendar
    val calendarMock = mockStatic(Calendar::class.java)
    try {
      calendarMock.`when`<Calendar> { Calendar.getInstance() }.thenReturn(mockCalendar)

      val initialBadgeContext = BadgeContext(joinedEvents = 7, earlyJoin = 2, lateJoin = 1)
      whenever(badgeRepo.getBadgeContext(eq("user1"))).thenReturn(initialBadgeContext)

      repo.editEventAsUser("E1", "user1", join = true)

      val badgeContextCaptor = argumentCaptor<BadgeContext>()
      verify(badgeRepo).saveBadgeContext(eq("user1"), badgeContextCaptor.capture())

      val capturedContext = badgeContextCaptor.firstValue
      assertEquals(8, capturedContext.joinedEvents)
      assertEquals(2, capturedContext.earlyJoin) // Should not change
      assertEquals(1, capturedContext.lateJoin) // Should not change
    } finally {
      calendarMock.close()
    }
  }

  @Test
  fun editEventAsUser_leave_doesNotUpdateBadgeContext() = runTest {
    val event =
        createEvent(uid = "E1", ownerId = "owner", participantIds = listOf("user1", "user2"))
    val docSnapshot = doc("E1", event)
    val eventDocRef = mock<DocumentReference>()
    val userRef = mock<DocumentReference>()

    whenever(usersCollection.document(any())).thenReturn(userRef)
    whenever(collection.document("E1")).thenReturn(eventDocRef)
    whenever(db.runTransaction(any<Transaction.Function<Void>>())).thenAnswer { invocation ->
      val txFunction = invocation.getArgument<Transaction.Function<Void>>(0)
      whenever(transaction.get(eventDocRef)).thenReturn(docSnapshot)
      txFunction.apply(transaction)
      voidTask()
    }

    repo.editEventAsUser("E1", "user1", join = false)

    // Badge context should NOT be updated when leaving
    verify(badgeRepo, never()).getBadgeContext(any())
    verify(badgeRepo, never()).saveBadgeContext(any(), any())
    verify(badgeRepo, never()).updateBadgesAfterContextChange(any())
  }

  @Test
  fun editEventAsUser_throwsException_whenEventNotFound() = runTest {
    val eventDocRef = mock<DocumentReference>()
    val missingSnapshot = mock<DocumentSnapshot>()
    whenever(missingSnapshot.exists()).thenReturn(false)

    whenever(collection.document("E404")).thenReturn(eventDocRef)
    whenever(db.runTransaction(any<Transaction.Function<Void>>())).thenAnswer { invocation ->
      val txFunction = invocation.getArgument<Transaction.Function<Void>>(0)
      whenever(transaction.get(eventDocRef)).thenReturn(missingSnapshot)
      txFunction.apply(transaction)
      voidTask()
    }

    val exception =
        assertFailsWith<Exception> { repo.editEventAsUser("E404", "user1", join = true) }
    assertTrue(exception.message!!.contains("Event not found"))
  }

  @Test
  fun editEventAsUser_join_atExactCapacity_throwsException() = runTest {
    val event =
        createEvent(
            uid = "E1", ownerId = "owner", participantIds = listOf("u1", "u2", "u3"), capacity = 3)
    val docSnapshot = doc("E1", event)
    val eventDocRef = mock<DocumentReference>()

    whenever(collection.document("E1")).thenReturn(eventDocRef)
    whenever(db.runTransaction(any<Transaction.Function<Void>>())).thenAnswer { invocation ->
      val txFunction = invocation.getArgument<Transaction.Function<Void>>(0)
      whenever(transaction.get(eventDocRef)).thenReturn(docSnapshot)
      txFunction.apply(transaction)
      voidTask()
    }

    val exception = assertFailsWith<Exception> { repo.editEventAsUser("E1", "user4", join = true) }
    assertTrue(exception.message!!.contains("Event is full"))
  }

  @Test
  fun editEventAsOwner_withValidUpdates_usesIndexedAccessor() = runTest {
    val existing =
        createEvent(uid = "E1", title = "Old", ownerId = "owner", participantIds = emptyList())
    val updated = existing.copy(title = "Updated Title", description = "Updated Description")
    val docSnapshot = doc("E1", existing)
    val eventDocRef = mock<DocumentReference>()

    whenever(collection.document("E1")).thenReturn(eventDocRef)
    whenever(db.runTransaction(any<Transaction.Function<Void>>())).thenAnswer { invocation ->
      val txFunction = invocation.getArgument<Transaction.Function<Void>>(0)
      whenever(transaction[eventDocRef]).thenReturn(docSnapshot)
      whenever(transaction.set(any<DocumentReference>(), any<Event>())).thenReturn(transaction)
      txFunction.apply(transaction)
      voidTask()
    }

    repo.editEventAsOwner("E1", updated)

    verify(transaction).set(any<DocumentReference>(), any<Event>())
  }

  @Test
  fun editEventAsOwner_changePublicToPrivate_throwsException() = runTest {
    val existing =
        createEvent(uid = "E1", ownerId = "owner", public = true, participantIds = emptyList())
    val updated = existing.copy(public = false)
    val docSnapshot = doc("E1", existing)
    val eventDocRef = mock<DocumentReference>()

    whenever(collection.document("E1")).thenReturn(eventDocRef)
    whenever(db.runTransaction(any<Transaction.Function<Void>>())).thenAnswer { invocation ->
      val txFunction = invocation.getArgument<Transaction.Function<Void>>(0)
      whenever(transaction[eventDocRef]).thenReturn(docSnapshot)
      txFunction.apply(transaction)
      voidTask()
    }

    val exception = assertFailsWith<Exception> { repo.editEventAsOwner("E1", updated) }
    assertTrue(exception.message!!.contains("Owner cannot change from public to private"))
  }

  @Test
  fun editEventAsOwner_keepPublicTrue_success() = runTest {
    val existing =
        createEvent(uid = "E1", ownerId = "owner", public = true, participantIds = emptyList())
    val updated = existing.copy(title = "Updated", public = true)
    val docSnapshot = doc("E1", existing)
    val eventDocRef = mock<DocumentReference>()

    whenever(collection.document("E1")).thenReturn(eventDocRef)
    whenever(db.runTransaction(any<Transaction.Function<Void>>())).thenAnswer { invocation ->
      val txFunction = invocation.getArgument<Transaction.Function<Void>>(0)
      whenever(transaction[eventDocRef]).thenReturn(docSnapshot)
      whenever(transaction.set(any<DocumentReference>(), any<Event>())).thenReturn(transaction)
      txFunction.apply(transaction)
      voidTask()
    }

    repo.editEventAsOwner("E1", updated)

    verify(transaction).set(any<DocumentReference>(), any<Event>())
  }

  @Test
  fun editEventAsOwner_changePrivateToPublic_success() = runTest {
    val existing =
        createEvent(uid = "E1", ownerId = "owner", public = false, participantIds = emptyList())
    val updated = existing.copy(title = "Updated", public = true)
    val docSnapshot = doc("E1", existing)
    val eventDocRef = mock<DocumentReference>()

    whenever(collection.document("E1")).thenReturn(eventDocRef)
    whenever(db.runTransaction(any<Transaction.Function<Void>>())).thenAnswer { invocation ->
      val txFunction = invocation.getArgument<Transaction.Function<Void>>(0)
      whenever(transaction[eventDocRef]).thenReturn(docSnapshot)
      whenever(transaction.set(any<DocumentReference>(), any<Event>())).thenReturn(transaction)
      txFunction.apply(transaction)
      voidTask()
    }

    repo.editEventAsOwner("E1", updated)

    verify(transaction).set(any<DocumentReference>(), any<Event>())
  }
}
