package com.swent.mapin.model.event

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SavedEventEntity::class], version = 1, exportSchema = false)
abstract class EventsDatabase : RoomDatabase() {
  abstract fun savedEventDao(): SavedEventDao

  companion object {
    @Volatile
    private var INSTANCE: EventsDatabase? = null

    fun getInstance(context: Context): EventsDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
            context.applicationContext,
            EventsDatabase::class.java,
            "events_db"
        ).build()
        INSTANCE = instance
        instance
      }
    }
  }
}

