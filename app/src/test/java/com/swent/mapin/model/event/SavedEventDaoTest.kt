package com.swent.mapin.model.event

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SavedEventDaoTest {
  private lateinit var db: EventsDatabase
  private lateinit var dao: SavedEventDao

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, EventsDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    dao = db.savedEventDao()
  }

  @After
  fun teardown() {
    db.close()
  }

  @Test
  fun insertAndQuerySavedEvents() = runTest {
    val userId = "user1"
    val e1 = SavedEventEntity(
        id = "e1",
        userId = userId,
        title = "T1",
        description = "D1",
        dateSeconds = 1000L,
        dateNanoseconds = 0,
        locationName = "L1",
        locationLat = 1.0,
        locationLng = 2.0,
        tagsCsv = "tag1,tag2",
        isPublic = true,
        ownerId = "owner1",
        imageUrl = null,
        capacity = 10,
        participantIdsCsv = "p1,p2",
        savedAtSeconds = 2000L
    )
    val e2 = e1.copy(id = "e2", title = "T2")

    dao.insertAll(listOf(e1, e2))

    val results = dao.getSavedForUser(userId)
    assertEquals(2, results.size)
    assertTrue(results.any { it.id == "e1" && it.title == "T1" })
    assertTrue(results.any { it.id == "e2" && it.title == "T2" })
  }

  @Test
  fun deleteAndClearForUser() = runTest {
    val userId = "user_del"
    val e1 = SavedEventEntity(
        id = "d1",
        userId = userId,
        title = "TD",
        description = "Desc",
        dateSeconds = null,
        dateNanoseconds = null,
        locationName = "L",
        locationLat = 0.0,
        locationLng = 0.0,
        tagsCsv = "",
        isPublic = false,
        ownerId = "o",
        imageUrl = null,
        capacity = null,
        participantIdsCsv = "",
        savedAtSeconds = null
    )
    dao.insert(e1)
    var r = dao.getSavedForUser(userId)
    assertEquals(1, r.size)

    dao.delete("d1", userId)
    r = dao.getSavedForUser(userId)
    assertEquals(0, r.size)

    // insert and clear
    dao.insert(e1)
    dao.clearForUser(userId)
    r = dao.getSavedForUser(userId)
    assertEquals(0, r.size)
  }
}
