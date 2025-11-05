package com.swent.mapin.model.event

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Data Access Object (DAO) for performing database operations on saved events. Provides methods to
 * query, insert, and delete saved events associated with users. Implemented with the help of AI.
 */
@Dao
interface SavedEventDao {
  @Query("SELECT * FROM saved_events WHERE user_id = :userId ORDER BY saved_at DESC")
  suspend fun getSavedForUser(userId: String): List<SavedEventEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(events: List<SavedEventEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(event: SavedEventEntity)

  @Query("DELETE FROM saved_events WHERE user_id = :userId")
  suspend fun clearForUser(userId: String)

  @Query("DELETE FROM saved_events WHERE id = :eventId AND user_id = :userId")
  suspend fun delete(eventId: String, userId: String)
}
