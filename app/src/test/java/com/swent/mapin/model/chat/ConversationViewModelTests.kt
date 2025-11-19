package com.swent.mapin.model.chat

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.swent.mapin.model.UserProfile
import com.swent.mapin.model.UserProfileRepository
import com.swent.mapin.ui.chat.Conversation
import com.swent.mapin.ui.chat.ConversationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.*
import org.mockito.kotlin.*

// Assisted by AI
@ExperimentalCoroutinesApi
class ConversationViewModelTest {

  @get:Rule private val testDispatcher = StandardTestDispatcher()
  private lateinit var conversationRepository: ConversationRepository
  private lateinit var userProfileRepository: UserProfileRepository
  private lateinit var viewModel: ConversationViewModel

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    conversationRepository = mock()
    userProfileRepository = mock()
    viewModel =
        ConversationViewModel(
            conversationRepository, userProfileRepository, currentUserIdProvider = { "user123" })
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `getNewUID returns repository UID`() {
    whenever(conversationRepository.getNewUid()).thenReturn("new_uid_123")
    val uid = viewModel.getNewUID()
    Assert.assertEquals("new_uid_123", uid)
  }

  @Test
  fun `getCurrentUserProfile sets currentUserProfile correctly`() = runTest {
    val userProfile =
        UserProfile(
            userId = "user123",
            name = "Test User",
            bio = "Bio",
            hobbies = listOf("Reading", "Gaming"),
            location = "Test City")
    whenever(userProfileRepository.getUserProfile("user123")).thenReturn(userProfile)

    // Just call the function; no Firebase mocking needed
    viewModel.getCurrentUserProfile()
    testDispatcher.scheduler.advanceUntilIdle()

    Assert.assertEquals(userProfile, viewModel.currentUserProfile)
  }

  @Test
  fun `observeConversations updates userConversations StateFlow`() = runTest {
    val conversations =
        listOf(
            Conversation(id = "conv1", name = "Chat One"),
            Conversation(id = "conv2", name = "Chat Two"))

    whenever(conversationRepository.observeConversationsForCurrentUser())
        .thenReturn(flow { emit(conversations) })

    viewModel.observeConversations()
    testDispatcher.scheduler.advanceUntilIdle()

    Assert.assertEquals(conversations, viewModel.userConversations.value)
  }

  @Test
  fun `createConversation calls repository addConversation`() = runTest {
    val conversation = Conversation(id = "conv1", name = "Chat One")
    viewModel.createConversation(conversation)
    testDispatcher.scheduler.advanceUntilIdle()

    verify(conversationRepository).addConversation(conversation)
  }

  // Assisted by GPT
  @Test
  fun `observeConversationsForCurrentUser overrides name and profile picture for 1-to-1 chat`() =
      runTest {
        // Mock FirebaseAuth
        val auth = mock<FirebaseAuth>()
        val currentUser = mock<com.google.firebase.auth.FirebaseUser>()
        whenever(currentUser.uid).thenReturn("user123")
        whenever(auth.currentUser).thenReturn(currentUser)

        // Mock Firestore
        val db = mock<FirebaseFirestore>()
        val collectionRef = mock<com.google.firebase.firestore.CollectionReference>()
        val query = mock<com.google.firebase.firestore.Query>()

        whenever(db.collection("conversations")).thenReturn(collectionRef)
        whenever(collectionRef.whereArrayContains("participantIds", "user123")).thenReturn(query)
        whenever(query.orderBy("lastMessageTimestamp", Query.Direction.DESCENDING))
            .thenReturn(query)

        val conversation =
            Conversation(
                id = "conv1",
                name = "Original Name",
                profilePictureUrl = "original_url",
                participants =
                    listOf(
                        UserProfile(
                            userId = "user123",
                            name = "Current User",
                            bio = "",
                            hobbies = emptyList(),
                            location = ""),
                        UserProfile(
                            userId = "user456",
                            name = "Other User",
                            bio = "",
                            hobbies = emptyList(),
                            location = "")),
                participantIds = listOf("user123", "user456"))

        // Stub addSnapshotListener to immediately call the listener
        whenever(query.addSnapshotListener(any())).thenAnswer { invocation ->
          val listener =
              invocation.getArgument<
                  com.google.firebase.firestore.EventListener<
                      com.google.firebase.firestore.QuerySnapshot>>(
                  0)

          val snapshot = mock<com.google.firebase.firestore.QuerySnapshot>()
          val doc = mock<com.google.firebase.firestore.DocumentSnapshot>()
          whenever(snapshot.documents).thenReturn(listOf(doc))
          whenever(doc.toObject(Conversation::class.java)).thenReturn(conversation)

          listener.onEvent(snapshot, null)
          mock<com.google.firebase.firestore.ListenerRegistration>()
        }

        val repo = ConversationRepositoryFirestore(db, auth)

        val results = mutableListOf<List<Conversation>>()
        val job = launch { repo.observeConversationsForCurrentUser().collect { results.add(it) } }

        // Let the flow emit
        advanceUntilIdle()

        job.cancel()

        val observedConversation = results.flatten().firstOrNull()
        Assert.assertNotNull(
            "Flow should have emitted at least one conversation", observedConversation)
        observedConversation?.let {
          Assert.assertEquals("Other User", it.name)
          Assert.assertEquals(conversation.id, it.id)
        }
      }
}
