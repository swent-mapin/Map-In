package com.swent.mapin.model.event

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for storing saved events locally. Defines the database configuration and serves as
 * the app's main access point to the persisted data. Implemented with the help of AI.
 */
@Database(entities = [SavedEventEntity::class], version = 1, exportSchema = false)
abstract class EventsDatabase : RoomDatabase() {
  abstract fun savedEventDao(): SavedEventDao

  companion object {
    @Volatile private var INSTANCE: EventsDatabase? = null

    fun getInstance(context: Context): EventsDatabase {
      return INSTANCE
          ?: synchronized(this) {
            val instance =
                Room.databaseBuilder(
                        context.applicationContext, EventsDatabase::class.java, "events_db")
                    .build()
            INSTANCE = instance
            instance
          }
    }
  }
}
