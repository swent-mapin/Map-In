package com.swent.mapin.model.chat

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.mockito.Mockito.*

@OptIn(ExperimentalCoroutinesApi::class)
class MessageRepositoryFirestoreTest {

  private lateinit var db: FirebaseFirestore
  private lateinit var auth: FirebaseAuth
  private lateinit var repo: MessageRepositoryFirestore
  private lateinit var user: FirebaseUser

  @Before
  fun setUp() {
    db = mock(FirebaseFirestore::class.java)
    auth = mock(FirebaseAuth::class.java)
    user = mock(FirebaseUser::class.java)
    `when`(auth.currentUser).thenReturn(user)
    `when`(user.uid).thenReturn("testUserId")
    repo = MessageRepositoryFirestore(db, auth)
  }

  @Test
  fun `sendMessage does nothing for blank text`() = runTest {
    repo.sendMessage("conversation1", "   ")
    verify(db, never()).collection(anyString())
  }

  @Test
  fun `sendMessage does nothing when user is null`() = runTest {
    `when`(auth.currentUser).thenReturn(null)
    repo.sendMessage("conversation1", "Hello!")
    verify(db, never()).collection(anyString())
  }

  @Test
  fun `sendMessage runs batch when valid`() = runTest {
    val batch = mock(WriteBatch::class.java)
    val docRef = mock(DocumentReference::class.java)
    val messagesCol = mock(CollectionReference::class.java)
    val convDoc = mock(DocumentReference::class.java)

    `when`(db.collection("conversations")).thenReturn(mock(CollectionReference::class.java))
    val convCol = db.collection("conversations")
    `when`(convCol.document("conversation1")).thenReturn(convDoc)
    `when`(convDoc.collection("messages")).thenReturn(messagesCol)
    `when`(db.runBatch(any())).thenAnswer { invocation ->
      val lambda = invocation.arguments[0]
      val batch = mock(WriteBatch::class.java)

      // Safely invoke the Kotlin lambda
      val invokeMethod = lambda::class.java.methods.firstOrNull { it.name == "invoke" }
      invokeMethod?.invoke(lambda, batch)

      Tasks.forResult(null)
    }

    repo.sendMessage("conversation1", "Hello World")

    verify(db).runBatch(any())
  }

  @Test
  fun `loadMoreMessages retrieves paginated messages correctly`() = runTest {
    val mockQuery = mock(Query::class.java)
    val mockConvCol = mock(CollectionReference::class.java)
    val mockConvDoc = mock(DocumentReference::class.java)
    val mockMessagesCol = mock(CollectionReference::class.java)
    val mockQuerySnapshot = mock(QuerySnapshot::class.java)
    val mockLastVisible = mock(DocumentSnapshot::class.java)
    val mockMessageDoc1 = mock(DocumentSnapshot::class.java)
    val mockMessageDoc2 = mock(DocumentSnapshot::class.java)

    `when`(db.collection("conversations")).thenReturn(mockConvCol)
    `when`(mockConvCol.document("conversation1")).thenReturn(mockConvDoc)
    `when`(mockConvDoc.collection("messages")).thenReturn(mockMessagesCol)
    `when`(mockMessagesCol.orderBy("timestamp", Query.Direction.DESCENDING)).thenReturn(mockQuery)
    `when`(mockQuery.startAfter(mockLastVisible)).thenReturn(mockQuery)
    `when`(mockQuery.limit(50L)).thenReturn(mockQuery)
    `when`(mockQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))

    // Mock message documents
    `when`(mockMessageDoc1.getString("senderId")).thenReturn("user1")
    `when`(mockMessageDoc1.getLong("timestamp")).thenReturn(1000L)
    `when`(mockMessageDoc1.getString("text")).thenReturn("Hello")

    `when`(mockMessageDoc2.getString("senderId")).thenReturn("testUserId")
    `when`(mockMessageDoc2.getLong("timestamp")).thenReturn(2000L)
    `when`(mockMessageDoc2.getString("text")).thenReturn("Hi there")

    `when`(mockQuerySnapshot.documents).thenReturn(listOf(mockMessageDoc1, mockMessageDoc2))

    val (messages, lastDoc) = repo.loadMoreMessages("conversation1", mockLastVisible)

    assert(messages.size == 2)
    assert(messages[0].text == "Hi there") // Reversed order
    assert(messages[0].isMe) // User's own message
    assert(messages[1].text == "Hello")
    assert(!messages[1].isMe) // Other user's message
    assert(lastDoc == mockMessageDoc2)
  }

  @Test
  fun `loadMoreMessages filters invalid messages with missing senderId`() = runTest {
    val mockQuery = mock(Query::class.java)
    val mockConvCol = mock(CollectionReference::class.java)
    val mockConvDoc = mock(DocumentReference::class.java)
    val mockMessagesCol = mock(CollectionReference::class.java)
    val mockQuerySnapshot = mock(QuerySnapshot::class.java)
    val mockLastVisible = mock(DocumentSnapshot::class.java)
    val mockMessageDoc1 = mock(DocumentSnapshot::class.java)
    val mockMessageDoc2 = mock(DocumentSnapshot::class.java)

    `when`(db.collection("conversations")).thenReturn(mockConvCol)
    `when`(mockConvCol.document("conversation1")).thenReturn(mockConvDoc)
    `when`(mockConvDoc.collection("messages")).thenReturn(mockMessagesCol)
    `when`(mockMessagesCol.orderBy("timestamp", Query.Direction.DESCENDING)).thenReturn(mockQuery)
    `when`(mockQuery.startAfter(mockLastVisible)).thenReturn(mockQuery)
    `when`(mockQuery.limit(50L)).thenReturn(mockQuery)
    `when`(mockQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))

    // Mock one valid and one invalid message
    `when`(mockMessageDoc1.getString("senderId")).thenReturn(null) // Invalid
    `when`(mockMessageDoc1.getLong("timestamp")).thenReturn(1000L)

    `when`(mockMessageDoc2.getString("senderId")).thenReturn("user1")
    `when`(mockMessageDoc2.getLong("timestamp")).thenReturn(2000L)
    `when`(mockMessageDoc2.getString("text")).thenReturn("Valid message")

    `when`(mockQuerySnapshot.documents).thenReturn(listOf(mockMessageDoc1, mockMessageDoc2))

    val (messages, _) = repo.loadMoreMessages("conversation1", mockLastVisible)

    // Only the valid message should be returned
    assert(messages.size == 1)
    assert(messages[0].text == "Valid message")
  }

  @Test
  fun `loadMoreMessages filters invalid messages with missing timestamp`() = runTest {
    val mockQuery = mock(Query::class.java)
    val mockConvCol = mock(CollectionReference::class.java)
    val mockConvDoc = mock(DocumentReference::class.java)
    val mockMessagesCol = mock(CollectionReference::class.java)
    val mockQuerySnapshot = mock(QuerySnapshot::class.java)
    val mockLastVisible = mock(DocumentSnapshot::class.java)
    val mockMessageDoc1 = mock(DocumentSnapshot::class.java)
    val mockMessageDoc2 = mock(DocumentSnapshot::class.java)

    `when`(db.collection("conversations")).thenReturn(mockConvCol)
    `when`(mockConvCol.document("conversation1")).thenReturn(mockConvDoc)
    `when`(mockConvDoc.collection("messages")).thenReturn(mockMessagesCol)
    `when`(mockMessagesCol.orderBy("timestamp", Query.Direction.DESCENDING)).thenReturn(mockQuery)
    `when`(mockQuery.startAfter(mockLastVisible)).thenReturn(mockQuery)
    `when`(mockQuery.limit(50L)).thenReturn(mockQuery)
    `when`(mockQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))

    // Mock one valid and one invalid message
    `when`(mockMessageDoc1.getString("senderId")).thenReturn("user1")
    `when`(mockMessageDoc1.getLong("timestamp")).thenReturn(null) // Invalid

    `when`(mockMessageDoc2.getString("senderId")).thenReturn("user2")
    `when`(mockMessageDoc2.getLong("timestamp")).thenReturn(2000L)
    `when`(mockMessageDoc2.getString("text")).thenReturn("Valid message")

    `when`(mockQuerySnapshot.documents).thenReturn(listOf(mockMessageDoc1, mockMessageDoc2))

    val (messages, _) = repo.loadMoreMessages("conversation1", mockLastVisible)

    // Only the valid message should be returned
    assert(messages.size == 1)
    assert(messages[0].text == "Valid message")
  }

  @Test
  fun `loadMoreMessages returns empty list when no more messages`() = runTest {
    val mockQuery = mock(Query::class.java)
    val mockConvCol = mock(CollectionReference::class.java)
    val mockConvDoc = mock(DocumentReference::class.java)
    val mockMessagesCol = mock(CollectionReference::class.java)
    val mockQuerySnapshot = mock(QuerySnapshot::class.java)
    val mockLastVisible = mock(DocumentSnapshot::class.java)

    `when`(db.collection("conversations")).thenReturn(mockConvCol)
    `when`(mockConvCol.document("conversation1")).thenReturn(mockConvDoc)
    `when`(mockConvDoc.collection("messages")).thenReturn(mockMessagesCol)
    `when`(mockMessagesCol.orderBy("timestamp", Query.Direction.DESCENDING)).thenReturn(mockQuery)
    `when`(mockQuery.startAfter(mockLastVisible)).thenReturn(mockQuery)
    `when`(mockQuery.limit(50L)).thenReturn(mockQuery)
    `when`(mockQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))

    `when`(mockQuerySnapshot.documents).thenReturn(emptyList())

    val (messages, lastDoc) = repo.loadMoreMessages("conversation1", mockLastVisible)

    assert(messages.isEmpty())
    assert(lastDoc == null)
  }

  @Test
  fun `sendMessage handles empty text after trim`() = runTest {
    repo.sendMessage("conversation1", "   \n\t  ")
    verify(db, never()).collection(anyString())
  }

  @Test
  fun `loadMoreMessages handles text field null`() = runTest {
    val mockQuery = mock(Query::class.java)
    val mockConvCol = mock(CollectionReference::class.java)
    val mockConvDoc = mock(DocumentReference::class.java)
    val mockMessagesCol = mock(CollectionReference::class.java)
    val mockQuerySnapshot = mock(QuerySnapshot::class.java)
    val mockLastVisible = mock(DocumentSnapshot::class.java)
    val mockMessageDoc = mock(DocumentSnapshot::class.java)

    `when`(db.collection("conversations")).thenReturn(mockConvCol)
    `when`(mockConvCol.document("conversation1")).thenReturn(mockConvDoc)
    `when`(mockConvDoc.collection("messages")).thenReturn(mockMessagesCol)
    `when`(mockMessagesCol.orderBy("timestamp", Query.Direction.DESCENDING)).thenReturn(mockQuery)
    `when`(mockQuery.startAfter(mockLastVisible)).thenReturn(mockQuery)
    `when`(mockQuery.limit(50L)).thenReturn(mockQuery)
    `when`(mockQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot))

    `when`(mockMessageDoc.getString("senderId")).thenReturn("user1")
    `when`(mockMessageDoc.getLong("timestamp")).thenReturn(1000L)
    `when`(mockMessageDoc.getString("text")).thenReturn(null)

    `when`(mockQuerySnapshot.documents).thenReturn(listOf(mockMessageDoc))

    val (messages, _) = repo.loadMoreMessages("conversation1", mockLastVisible)

    assert(messages.size == 1)
    assert(messages[0].text == "") // Should default to empty string
  }
}
