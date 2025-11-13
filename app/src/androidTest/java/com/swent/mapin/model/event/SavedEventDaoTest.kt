package com.swent.mapin.model.event

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Simple instrumentation tests for SavedEventDao using an in-memory Room database. Exercises
 * insert, insertAll, query ordering, delete and clear operations.
 */
@RunWith(AndroidJUnit4::class)
class SavedEventDaoTest {
  // Test-only RoomDatabase that exposes the DAO under test. Keeps the test self-contained.
  @Database(entities = [SavedEventEntity::class], version = 1, exportSchema = false)
  abstract class TestEventsDatabase : RoomDatabase() {
    abstract fun savedEventDao(): SavedEventDao
  }

  private lateinit var db: TestEventsDatabase
  private lateinit var dao: SavedEventDao

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, TestEventsDatabase::class.java).build()
    dao = db.savedEventDao()
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun insertAndGetSavedForUser_ordersBySavedAtDesc() = runBlocking {
    val e1 =
        SavedEventEntity(
            id = "E1",
            userId = "userA",
            title = "First",
            description = "d1",
            dateSeconds = 100L,
            dateNanoseconds = 0,
            endDateSeconds = 100L,
            endDateNanoseconds = 0,
            locationName = "Loc1",
            locationLat = 1.0,
            locationLng = 2.0,
            tagsCsv = "t1",
            isPublic = true,
            ownerId = "owner1",
            imageUrl = null,
            capacity = 10,
            participantIdsCsv = "",
            savedAtSeconds = 1000L)

    val e2 =
        SavedEventEntity(
            id = "E2",
            userId = "userA",
            title = "Second",
            description = "d2",
            dateSeconds = 200L,
            dateNanoseconds = 0,
            endDateSeconds = 100L,
            endDateNanoseconds = 0,
            locationName = "Loc2",
            locationLat = 3.0,
            locationLng = 4.0,
            tagsCsv = "t2",
            isPublic = true,
            ownerId = "owner2",
            imageUrl = null,
            capacity = 5,
            participantIdsCsv = "p1,p2",
            savedAtSeconds = 2000L)

    dao.insert(e1)
    dao.insert(e2)

    val list = dao.getSavedForUser("userA")
    // e2 has later savedAt and should appear first
    assertEquals(listOf("E2", "E1"), list.map { it.id })
  }

  @Test
  fun insertAll_andClearForUser_andDelete() = runBlocking {
    val events =
        (1..3).map { i ->
          SavedEventEntity(
              id = "E$i",
              userId = "userB",
              title = "T$i",
              description = "d$i",
              dateSeconds = 100L + i,
              dateNanoseconds = 0,
              endDateSeconds = 100L,
              endDateNanoseconds = 0,
              locationName = "L$i",
              locationLat = i.toDouble(),
              locationLng = (i + 0.5),
              tagsCsv = "",
              isPublic = true,
              ownerId = "o$i",
              imageUrl = null,
              capacity = null,
              participantIdsCsv = "",
              savedAtSeconds = 1000L + i)
        }

    dao.insertAll(events)
    val afterInsert = dao.getSavedForUser("userB")
    assertEquals(3, afterInsert.size)

    // Delete one
    dao.delete("E2", "userB")
    val afterDelete = dao.getSavedForUser("userB")
    assertEquals(listOf("E3", "E1"), afterDelete.map { it.id })

    // Clear all for user
    dao.clearForUser("userB")
    val afterClear = dao.getSavedForUser("userB")
    assertEquals(0, afterClear.size)
  }
}
