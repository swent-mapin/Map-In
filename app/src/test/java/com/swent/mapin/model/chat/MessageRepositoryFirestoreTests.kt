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
}
