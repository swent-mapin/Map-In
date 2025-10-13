package com.swent.mapin.model

import com.google.firebase.Timestamp
import com.swent.mapin.model.event.Event
import java.util.Date

/**
 * A sample repository that provides hardcoded event data for development and testing. This can be
 * replaced with a real repository implementation that fetches data from a backend.
 */
object SampleEventRepository {

  /** Returns a list of sample events positioned around EPFL in Lausanne, Switzerland. */
  fun getSampleEvents(): List<Event> {
    val now = Timestamp(Date())

    return listOf(
        Event(
            uid = "event1",
            title = "Music Festival",
            description = "Live music and food",
            date = now,
            locationName = "EPFL Campus",
            latitude = 46.5197,
            longitude = 6.5668,
            tags = listOf("Music", "Festival"),
            public = true,
            ownerId = "user1",
            attendeeCount = 200),
        Event(
            uid = "event2",
            title = "Basketball Game",
            description = "Friendly basketball match",
            date = now,
            locationName = "EPFL Sports Center",
            latitude = 46.5217,
            longitude = 6.5688,
            tags = listOf("Sports", "Basketball"),
            public = true,
            ownerId = "user2",
            attendeeCount = 50),
        Event(
            uid = "event3",
            title = "Art Exhibition",
            description = "Contemporary art showcase",
            date = now,
            locationName = "EPFL ArtLab",
            latitude = 46.5177,
            longitude = 6.5653,
            tags = listOf("Art", "Culture"),
            public = true,
            ownerId = "user3",
            attendeeCount = 120),
        Event(
            uid = "event4",
            title = "Food Market",
            description = "Local food and produce",
            date = now,
            locationName = "EPFL Esplanade",
            latitude = 46.5212,
            longitude = 6.5658,
            tags = listOf("Food", "Market"),
            public = true,
            ownerId = "user4",
            attendeeCount = 80),
        Event(
            uid = "event5",
            title = "Yoga Class",
            description = "Outdoor yoga session",
            date = now,
            locationName = "EPFL Green Space",
            latitude = 46.5182,
            longitude = 6.5683,
            tags = listOf("Yoga", "Wellness"),
            public = true,
            ownerId = "user5",
            attendeeCount = 30))
  }
}
