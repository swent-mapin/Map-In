package com.swent.mapin.model.event

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.Timestamp
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EventLocalCacheTest {
  private lateinit var db: EventsDatabase
  private lateinit var dao: SavedEventDao
  private lateinit var cache: EventLocalCache

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, EventsDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    dao = db.savedEventDao()
    cache = EventLocalCache(dao)
  }

  @After
  fun teardown() {
    db.close()
  }

  @Test
  fun saveAndRetrieveSavedEvent() = runTest {
    val userId = "u1"
    val event = Event(
        uid = "ev1",
        title = "Title",
        description = "Desc",
        date = Timestamp(1000, 0),
        location = com.swent.mapin.model.Location("L", 1.0, 2.0),
        tags = listOf("t1","t2"),
        public = true,
        ownerId = "owner",
        imageUrl = null,
        capacity = 5,
        participantIds = listOf("p1")
    )

    cache.saveEventLocally(userId, event, Timestamp.now())

    val saved = cache.getSavedEvents(userId)
    assertEquals(1, saved.size)
    assertEquals("ev1", saved[0].uid)
    assertEquals("Title", saved[0].title)
  }

  @Test
  fun cacheSavedEventsAndClear() = runTest {
    val userId = "u2"
    val events = listOf(
        Event(uid = "a", title = "A"),
        Event(uid = "b", title = "B")
    )
    cache.cacheSavedEvents(userId, events)
    var saved = cache.getSavedEvents(userId)
    assertEquals(2, saved.size)

    // unsave one
    cache.unsaveEventLocally(userId, "a")
    saved = cache.getSavedEvents(userId)
    assertEquals(1, saved.size)
    assertEquals("b", saved[0].uid)
  }
}
