package com.swent.mapin.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.swent.mapin.ui.event.uploadEventMedia
import com.swent.mapin.ui.memory.uploadMediaFiles
import io.mockk.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Assisted by AI tools like Gemini Unit tests for the uploadEventMedia/ uploadMediaFiles functions
 */
class UploadMediaTest {
  private lateinit var context: Context
  private lateinit var contentResolver: ContentResolver
  private lateinit var storage: FirebaseStorage
  private lateinit var rootRef: StorageReference
  private lateinit var fileRef: StorageReference
  private lateinit var uploadTask: UploadTask
  private lateinit var taskSnapshot: UploadTask.TaskSnapshot
  private lateinit var uri: Uri
  private lateinit var mimeTypeMap: MimeTypeMap

  @Before
  fun setup() {
    context = mockk()
    contentResolver = mockk()
    storage = mockk()
    rootRef = mockk()
    fileRef = mockk()
    uploadTask = mockk()
    taskSnapshot = mockk()
    uri = mockk()
    mimeTypeMap = mockk()

    mockkStatic(FirebaseStorage::class)
    mockkStatic("kotlinx.coroutines.tasks.TasksKt")
    mockkStatic(Log::class)
    mockkStatic(MimeTypeMap::class)

    every { context.contentResolver } returns contentResolver
    every { FirebaseStorage.getInstance() } returns storage
    every { storage.reference } returns rootRef
    every { rootRef.child(any()) } returns fileRef
    every { Log.e(any(), any(), any()) } returns 0

    every { MimeTypeMap.getSingleton() } returns mimeTypeMap
    every { mimeTypeMap.getExtensionFromMimeType("image/jpeg") } returns "jpg"
    every { mimeTypeMap.getExtensionFromMimeType("video/mp4") } returns "mp4"
    every { mimeTypeMap.getExtensionFromMimeType("image/png") } returns "png"
    every { mimeTypeMap.getExtensionFromMimeType(null) } returns null

    every { fileRef.putFile(any(), any()) } returns uploadTask
    coEvery { uploadTask.await() } returns taskSnapshot
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `uploadEventMedia uploads image successfully`() = runTest {
    val userId = "user123"
    val expectedUrl = "https://firebasestorage.googleapis.com/image.jpg"
    every { contentResolver.getType(uri) } returns "image/jpeg"
    val urlTask = mockk<Uri>()
    every { urlTask.toString() } returns expectedUrl
    every { fileRef.downloadUrl } returns mockk { coEvery { await() } returns urlTask }
    val result = uploadEventMedia(context, uri, userId)
    assertTrue(result.isSuccess)
    assertEquals(expectedUrl, result.getOrNull())
    verify { rootRef.child(match { it.contains("events/$userId/images/") && it.endsWith(".jpg") }) }
  }

  @Test
  fun `uploadEventMedia uploads video successfully`() = runTest {
    val userId = "user456"
    val expectedUrl = "https://firebasestorage.googleapis.com/video.mp4"
    every { contentResolver.getType(uri) } returns "video/mp4"
    val urlTask = mockk<Uri>()
    every { urlTask.toString() } returns expectedUrl
    every { fileRef.downloadUrl } returns mockk { coEvery { await() } returns urlTask }
    val result = uploadEventMedia(context, uri, userId)
    assertTrue(result.isSuccess)
    assertEquals(expectedUrl, result.getOrNull())
    verify { rootRef.child(match { it.contains("events/$userId/videos/") && it.endsWith(".mp4") }) }
  }

  @Test
  fun `uploadEventMedia handles upload failure gracefully`() = runTest {
    val userId = "user789"
    every { contentResolver.getType(uri) } returns "image/png"
    every { fileRef.putFile(any(), any()) } throws RuntimeException("Firebase network error")
    val result = uploadEventMedia(context, uri, userId)
    assertTrue(result.isFailure)
    assertEquals("Firebase network error", result.exceptionOrNull()?.message)
  }

  @Test
  fun `uploadEventMedia uses default jpeg when mime type is null`() = runTest {
    val userId = "userDefault"
    val expectedUrl = "https://url.com"
    every { contentResolver.getType(uri) } returns null
    val urlTask = mockk<Uri>()
    every { urlTask.toString() } returns expectedUrl
    every { fileRef.downloadUrl } returns mockk { coEvery { await() } returns urlTask }
    val result = uploadEventMedia(context, uri, userId)
    assertTrue(result.isSuccess)
    verify { rootRef.child(match { it.contains("events/$userId/images/") && it.endsWith(".jpg") }) }
  }

  @Test
  fun `uploadMediaFiles returns empty list when uris list is empty`() = runTest {
    val userId = "user123"
    val uris = emptyList<Uri>()
    val result = uploadMediaFiles(context, uris, userId)
    assertTrue(result.isEmpty())
    verify(exactly = 0) { rootRef.child(any()) }
  }

  @Test
  fun `uploadMediaFiles uploads multiple files successfully`() = runTest {
    val userId = "userMultiple"
    val uri1 = mockk<Uri>()
    val uri2 = mockk<Uri>()
    val uris = listOf(uri1, uri2)

    // Mock behaviors for both files
    every { contentResolver.getType(uri1) } returns "image/jpeg"
    every { contentResolver.getType(uri2) } returns "video/mp4"

    // Mock success URLs
    val url1 = "https://url.com/1.jpg"
    val url2 = "https://url.com/2.mp4"

    val urlTask1 = mockk<Uri>()
    every { urlTask1.toString() } returns url1
    val urlTask2 = mockk<Uri>()
    every { urlTask2.toString() } returns url2

    every { fileRef.downloadUrl } returns
        mockk { coEvery { await() } returnsMany listOf(urlTask1, urlTask2) }

    val result = uploadMediaFiles(context, uris, userId)

    assertEquals(2, result.size)
    assertEquals(url1, result[0])
    assertEquals(url2, result[1])

    // Verify both were uploaded
    verify(exactly = 2) { fileRef.putFile(any(), any()) }
  }

  @Test
  fun `uploadMediaFiles skips failed uploads but continues with others`() = runTest {
    val userId = "userPartial"
    val uriSuccess = mockk<Uri>()
    val uriFail = mockk<Uri>()
    val uris = listOf(uriFail, uriSuccess)

    every { contentResolver.getType(uriFail) } returns "image/png"
    every { contentResolver.getType(uriSuccess) } returns "image/jpeg"

    val urlSuccess = "https://url.com/success.jpg"
    val urlTask = mockk<Uri>()
    every { urlTask.toString() } returns urlSuccess
    every { fileRef.putFile(uriFail, any()) } throws RuntimeException("Upload failed")
    every { fileRef.putFile(uriSuccess, any()) } returns uploadTask
    every { fileRef.downloadUrl } returns mockk { coEvery { await() } returns urlTask }
    val result = uploadMediaFiles(context, uris, userId)
    assertEquals(1, result.size)
    assertEquals(urlSuccess, result[0])
    verify { Log.e(any(), "Failed to upload media file", any()) }
  }
}
