package com.swent.mapin.model.event

import com.google.firebase.Timestamp
import com.swent.mapin.model.Location
import java.util.Date
import java.util.UUID

/**
 * Local in-memory implementation of [EventRepository] backed by a predefined dataset used for
 * development, previews, and offline testing.
 */
class LocalEventRepository(initialEvents: List<Event> = defaultSampleEvents()) : EventRepository {

  private val events =
      initialEvents
          .map { event ->
            if (event.ownerId.isNotBlank() && !event.participantIds.contains(event.ownerId)) {
              event.copy(participantIds = event.participantIds + event.ownerId)
            } else {
              event
            }
          }
          .associateBy { it.uid }
          .toMutableMap()

  private var nextNumericId: Int =
      events.keys.mapNotNull { key -> key.removePrefix("event").toIntOrNull() }.maxOrNull()?.plus(1)
          ?: 1

  override fun getNewUid(): String {
    val candidate = "event$nextNumericId"
    nextNumericId += 1
    return if (!events.containsKey(candidate)) candidate else "event_${UUID.randomUUID()}"
  }

  override suspend fun getAllEvents(): List<Event> = events.values.sortedBy { it.date }

  override suspend fun getEvent(eventID: String): Event {
    return events[eventID]
        ?: throw NoSuchElementException("LocalEventRepository: Event not found (id=$eventID)")
  }

  override suspend fun getEventsByTags(tags: List<String>): List<Event> {
    if (tags.isEmpty()) return getAllEvents()
    val tagSet = tags.map { it.trim().lowercase() }.toSet()
    return events.values
        .filter { event -> event.tags.any { tag -> tag.trim().lowercase() in tagSet } }
        .sortedBy { it.date }
  }

  override suspend fun getEventsOnDay(dayStart: Timestamp, dayEnd: Timestamp): List<Event> {
    return events.values
        .filter { event ->
          event.date?.let { timestamp -> timestamp >= dayStart && timestamp < dayEnd } ?: false
        }
        .sortedBy { it.date }
  }

  override suspend fun getEventsByOwner(ownerId: String): List<Event> {
    val trimmed = ownerId.trim()
    return events.values.filter { it.ownerId == trimmed }.sortedBy { it.date }
  }

  override suspend fun getEventsByTitle(title: String): List<Event> {
    val normalized = title.trim().lowercase()
    return events.values.filter { it.title.trim().lowercase() == normalized }.sortedBy { it.date }
  }

  override suspend fun getEventsByParticipant(userId: String): List<Event> {
    val trimmed = userId.trim()
    return events.values
        .filter { event -> event.participantIds.contains(trimmed) }
        .sortedBy { it.date }
  }

  override suspend fun addEvent(event: Event) {
    val id = event.uid.ifBlank { getNewUid() }
    val participants =
        if (event.ownerId.isNotBlank() && !event.participantIds.contains(event.ownerId)) {
          event.participantIds + event.ownerId
        } else {
          event.participantIds
        }
    val eventToStore = event.copy(uid = id, participantIds = participants)
    events[id] = eventToStore
  }

  override suspend fun editEvent(eventID: String, newValue: Event) {
    if (!events.containsKey(eventID)) {
      throw NoSuchElementException("LocalEventRepository: Event not found (id=$eventID)")
    }

    val participants =
        if (newValue.ownerId.isNotBlank() && !newValue.participantIds.contains(newValue.ownerId)) {
          newValue.participantIds + newValue.ownerId
        } else {
          newValue.participantIds
        }

    events[eventID] = newValue.copy(uid = eventID, participantIds = participants)
  }

  override suspend fun deleteEvent(eventID: String) {
    if (events.remove(eventID) == null) {
      throw NoSuchElementException("LocalEventRepository: Event not found (id=$eventID)")
    }
  }

  companion object {
    /** Helper function to generate a list of participant IDs with a specific count */
    private fun generateParticipants(ownerId: String, additionalCount: Int): List<String> {
      return listOf(ownerId) +
          (1..additionalCount).map { "user${ownerId.removePrefix("user").toIntOrNull() ?: 0}_$it" }
    }

    /** Returns a deterministic list of sample events positioned around the EPFL campus. */
    fun defaultSampleEvents(): List<Event> {
      val now = Timestamp(Date())

      return listOf(
          // High attendance cluster near Rolex Learning Center
          Event(
              uid = "event1",
              title = "Music Festival",
              description = "Live music and food",
              date = now,
              location = Location("EPFL Campus", 46.5197, 6.5668),
              tags = listOf("Music", "Festival"),
              public = true,
              ownerId = "user1",
              capacity = null,
              participantIds = generateParticipants("user1", 450)), // Huge event, no capacity limit
          Event(
              uid = "event2",
              title = "Tech Conference",
              description = "Latest technology trends",
              date = now,
              location = Location("Rolex Learning Center", 46.5187, 6.5659),
              tags = listOf("Technology", "Conference"),
              public = true,
              ownerId = "user2",
              capacity = 500,
              participantIds = generateParticipants("user2", 385)), // Very popular, near full
          Event(
              uid = "event3",
              title = "Food Festival",
              description = "International cuisine",
              date = now,
              location = Location("EPFL Plaza", 46.5192, 6.5662),
              tags = listOf("Food", "Festival"),
              public = true,
              ownerId = "user3",
              capacity = 400,
              participantIds = generateParticipants("user3", 320)), // Popular hotspot

          // Moderate attendance around Sports Center
          Event(
              uid = "event4",
              title = "Basketball Game",
              description = "Friendly basketball match",
              date = now,
              location = Location("EPFL Sports Center", 46.5217, 6.5688),
              tags = listOf("Sports", "Basketball"),
              public = true,
              ownerId = "user4",
              capacity = 200,
              participantIds = generateParticipants("user4", 145)),
          Event(
              uid = "event5",
              title = "Volleyball Tournament",
              description = "Inter-university volleyball",
              date = now,
              location = Location("Sports Field 1", 46.5220, 6.5685),
              tags = listOf("Sports", "Volleyball"),
              public = true,
              ownerId = "user5",
              capacity = 150,
              participantIds = generateParticipants("user5", 98)),
          Event(
              uid = "event6",
              title = "Running Club",
              description = "Weekly running session",
              date = now,
              location = Location("Sports Field 2", 46.5215, 6.5692),
              tags = listOf("Sports", "Running"),
              public = true,
              ownerId = "user6",
              capacity = 80,
              participantIds = generateParticipants("user6", 52)),

          // Low attendance cultural events near ArtLab
          Event(
              uid = "event7",
              title = "Art Exhibition",
              description = "Contemporary art showcase",
              date = now,
              location = Location("EPFL ArtLab", 46.5177, 6.5653),
              tags = listOf("Art", "Culture"),
              public = true,
              ownerId = "user7",
              capacity = 100,
              participantIds = generateParticipants("user7", 35)),
          Event(
              uid = "event8",
              title = "Photography Workshop",
              description = "Learn photography basics",
              date = now,
              location = Location("ArtLab Studio", 46.5175, 6.5655),
              tags = listOf("Art", "Workshop"),
              public = true,
              ownerId = "user8",
              capacity = 40,
              participantIds = generateParticipants("user8", 18)),

          // Very small attendance study groups scattered around
          Event(
              uid = "event9",
              title = "Study Group - Math",
              description = "Analysis homework help",
              date = now,
              location = Location("BC Building", 46.5202, 6.5645),
              tags = listOf("Study", "Math"),
              public = true,
              ownerId = "user9",
              capacity = 20,
              participantIds = generateParticipants("user9", 7)),
          Event(
              uid = "event10",
              title = "Coding Meetup",
              description = "Python coding session",
              date = now,
              location = Location("CM Building", 46.5210, 6.5642),
              tags = listOf("Coding", "Tech"),
              public = true,
              ownerId = "user10",
              capacity = 30,
              participantIds = generateParticipants("user10", 12)),
          Event(
              uid = "event11",
              title = "Language Exchange",
              description = "Practice French & English",
              date = now,
              location = Location("Library", 46.5195, 6.5670),
              tags = listOf("Languages", "Social"),
              public = true,
              ownerId = "user11",
              capacity = 35,
              participantIds = generateParticipants("user11", 15)),

          // Medium attendance around Esplanade
          Event(
              uid = "event12",
              title = "Food Market",
              description = "Local food and produce",
              date = now,
              location = Location("EPFL Esplanade", 46.5212, 6.5658),
              tags = listOf("Food", "Market"),
              public = true,
              ownerId = "user12",
              capacity = 250,
              participantIds = generateParticipants("user12", 180)),
          Event(
              uid = "event13",
              title = "Farmers Market",
              description = "Organic vegetables and fruits",
              date = now,
              location = Location("Central Square", 46.5208, 6.5655),
              tags = listOf("Food", "Market"),
              public = true,
              ownerId = "user13",
              capacity = 300,
              participantIds = generateParticipants("user13", 225)),

          // Very high attendance hotspot - special events
          Event(
              uid = "event14",
              title = "Science Expo",
              description = "Interactive science exhibits",
              date = now,
              location = Location("Convention Center", 46.5180, 6.5665),
              tags = listOf("Science", "Exhibition"),
              public = true,
              ownerId = "user14",
              capacity = 600,
              participantIds = generateParticipants("user14", 520)),
          Event(
              uid = "event15",
              title = "Startup Fair",
              description = "Meet EPFL startups",
              date = now,
              location = Location("Innovation Square", 46.5190, 6.5675),
              tags = listOf("Business", "Networking"),
              public = true,
              ownerId = "user15",
              capacity = 700,
              participantIds = generateParticipants("user15", 650)), // Nearly full, very hot

          // Workshops and classes
          Event(
              uid = "event16",
              title = "Yoga Class",
              description = "Morning yoga session",
              date = now,
              location = Location("Sports Hall", 46.5205, 6.5648),
              tags = listOf("Wellness", "Yoga"),
              public = true,
              ownerId = "user16",
              capacity = 60,
              participantIds = generateParticipants("user16", 42)),
          Event(
              uid = "event17",
              title = "Cooking Workshop",
              description = "Learn Italian cuisine",
              date = now,
              location = Location("Kitchen Lab", 46.5172, 6.5669),
              tags = listOf("Food", "Workshop"),
              public = true,
              ownerId = "user17",
              capacity = 45,
              participantIds = generateParticipants("user17", 28)),
          Event(
              uid = "event18",
              title = "Photography Class",
              description = "Night photography techniques",
              date = now,
              location = Location("Photo Studio", 46.5182, 6.5672),
              tags = listOf("Art", "Photography"),
              public = true,
              ownerId = "user18",
              capacity = 50,
              participantIds = generateParticipants("user18", 31)),

          // Student clubs
          Event(
              uid = "event19",
              title = "Chess Club",
              description = "Weekly chess tournament",
              date = now,
              location = Location("Student Center", 46.5191, 6.5680),
              tags = listOf("Games", "Club"),
              public = true,
              ownerId = "user19",
              capacity = 30,
              participantIds = generateParticipants("user19", 14)),
          Event(
              uid = "event20",
              title = "Board Game Night",
              description = "Play various board games",
              date = now,
              location = Location("Community Hall", 46.5200, 6.5660),
              tags = listOf("Games", "Social"),
              public = true,
              ownerId = "user20",
              capacity = 100,
              participantIds = generateParticipants("user20", 75)),
          Event(
              uid = "event21",
              title = "Robotics Workshop",
              description = "Build your own robot",
              date = now,
              location = Location("Robotics Lab", 46.5223, 6.5665),
              tags = listOf("Science", "Physics"),
              public = true,
              ownerId = "user21",
              capacity = 80,
              participantIds = generateParticipants("user21", 58)),
          Event(
              uid = "event22",
              title = "Biology Lecture",
              description = "Genetics and CRISPR",
              date = now,
              location = Location("SV Building", 46.5225, 6.5670),
              tags = listOf("Science", "Biology"),
              public = true,
              ownerId = "user22",
              capacity = 120,
              participantIds = generateParticipants("user22", 88)),

          // Music performances - varying attendance
          Event(
              uid = "event23",
              title = "Jazz Concert",
              description = "Live jazz performance",
              date = now,
              location = Location("Amphitheater", 46.5198, 6.5656),
              tags = listOf("Music", "Jazz"),
              public = true,
              ownerId = "user23",
              capacity = 300,
              participantIds = generateParticipants("user23", 245)),
          Event(
              uid = "event24",
              title = "Open Mic Night",
              description = "Sing or perform",
              date = now,
              location = Location("Satellite Bar", 46.5204, 6.5665),
              tags = listOf("Music", "Performance"),
              public = true,
              ownerId = "user24",
              capacity = 150,
              participantIds = generateParticipants("user24", 112)),
          Event(
              uid = "event25",
              title = "Classical Concert",
              description = "Orchestra performance",
              date = now,
              location = Location("Concert Hall", 46.5196, 6.5660),
              tags = listOf("Music", "Classical"),
              public = true,
              ownerId = "user25",
              capacity = 200,
              participantIds = generateParticipants("user25", 165)),

          // Extended area - North of EPFL
          Event(
              uid = "event26",
              title = "Startup Pitch Night",
              description = "Entrepreneurs present their ideas",
              date = now,
              location = Location("Innovation Park", 46.5240, 6.5700),
              tags = listOf("Business", "Startup"),
              public = true,
              ownerId = "user26",
              capacity = 250,
              participantIds = generateParticipants("user26", 195)),
          Event(
              uid = "event27",
              title = "Hackathon",
              description = "24-hour coding challenge",
              date = now,
              location = Location("SwissTech Convention", 46.5235, 6.5660),
              tags = listOf("Tech", "Coding"),
              public = true,
              ownerId = "user27",
              capacity = 400,
              participantIds = generateParticipants("user27", 368)),
          Event(
              uid = "event28",
              title = "Robotics Demo",
              description = "Latest robotics projects",
              date = now,
              location = Location("Robotics Lab", 46.5228, 6.5675),
              tags = listOf("Robotics", "Tech"),
              public = true,
              ownerId = "user28",
              capacity = 180,
              participantIds = generateParticipants("user28", 134)),

          // Extended area - South of EPFL
          Event(
              uid = "event29",
              title = "Lake Picnic",
              description = "Social gathering by the lake",
              date = now,
              location = Location("Lake Geneva Shore", 46.5155, 6.5650),
              tags = listOf("Social", "Outdoor"),
              public = true,
              ownerId = "user29",
              capacity = 200,
              participantIds = generateParticipants("user29", 156)),
          Event(
              uid = "event30",
              title = "Beach Volleyball",
              description = "Tournament by the lake",
              date = now,
              location = Location("Beach Court", 46.5148, 6.5665),
              tags = listOf("Sports", "Beach"),
              public = true,
              ownerId = "user30",
              capacity = 100,
              participantIds = generateParticipants("user30", 78)),
          Event(
              uid = "event31",
              title = "Sailing Club",
              description = "Learn to sail",
              date = now,
              location = Location("Yacht Club", 46.5160, 6.5680),
              tags = listOf("Sports", "Sailing"),
              public = true,
              ownerId = "user31",
              capacity = 50,
              participantIds = generateParticipants("user31", 34)),

          // Extended area - East of EPFL
          Event(
              uid = "event32",
              title = "Trail Running",
              description = "Forest trail running",
              date = now,
              location = Location("Forest Trail", 46.5205, 6.5720),
              tags = listOf("Sports", "Running"),
              public = true,
              ownerId = "user32",
              capacity = 70,
              participantIds = generateParticipants("user32", 46)),
          Event(
              uid = "event33",
              title = "Outdoor Cinema",
              description = "Movie under the stars",
              date = now,
              location = Location("Park Amphitheater", 46.5190, 6.5710),
              tags = listOf("Entertainment", "Film"),
              public = true,
              ownerId = "user33",
              capacity = 500,
              participantIds = generateParticipants("user33", 425)), // Very popular
          Event(
              uid = "event34",
              title = "Cycling Club",
              description = "Weekend bike ride",
              date = now,
              location = Location("Bike Path Start", 46.5215, 6.5705),
              tags = listOf("Sports", "Cycling"),
              public = true,
              ownerId = "user34",
              capacity = 90,
              participantIds = generateParticipants("user34", 62)),

          // Extended area - West of EPFL
          Event(
              uid = "event35",
              title = "Wine Tasting",
              description = "Swiss wines exploration",
              date = now,
              location = Location("Wine Bar", 46.5185, 6.5620),
              tags = listOf("Food", "Social"),
              public = true,
              ownerId = "user35",
              capacity = 60,
              participantIds = generateParticipants("user35", 44)),
          Event(
              uid = "event36",
              title = "Cooking Class",
              description = "Learn Swiss cuisine",
              date = now,
              location = Location("Culinary School", 46.5170, 6.5625),
              tags = listOf("Food", "Workshop"),
              public = true,
              ownerId = "user36",
              capacity = 50,
              participantIds = generateParticipants("user36", 32)),
          Event(
              uid = "event37",
              title = "Comedy Night",
              description = "Stand-up comedy show",
              date = now,
              location = Location("Comedy Club", 46.5195, 6.5630),
              tags = listOf("Entertainment", "Comedy"),
              public = true,
              ownerId = "user37",
              capacity = 220,
              participantIds = generateParticipants("user37", 185)),

          // Scattered small events across wider area
          Event(
              uid = "event38",
              title = "Photography Walk",
              description = "Capture campus beauty",
              date = now,
              location = Location("East Campus", 46.5230, 6.5690),
              tags = listOf("Art", "Photography"),
              public = true,
              ownerId = "user38",
              capacity = 25,
              participantIds = generateParticipants("user38", 11)),
          Event(
              uid = "event39",
              title = "Book Club",
              description = "Discuss latest bestsellers",
              date = now,
              location = Location("Coffee Shop", 46.5165, 6.5640),
              tags = listOf("Reading", "Social"),
              public = true,
              ownerId = "user39",
              capacity = 25,
              participantIds = generateParticipants("user39", 9)),
          Event(
              uid = "event40",
              title = "Chess Tournament",
              description = "Strategic board game competition",
              date = now,
              location = Location("Student Center", 46.5178, 6.5695),
              tags = listOf("Games", "Competition"),
              public = true,
              ownerId = "user40",
              capacity = 50,
              participantIds = generateParticipants("user40", 28)),

          // More scattered medium events
          Event(
              uid = "event41",
              title = "Sustainability Fair",
              description = "Eco-friendly initiatives",
              date = now,
              location = Location("Green Building", 46.5250, 6.5655),
              tags = listOf("Environment", "Fair"),
              public = true,
              ownerId = "user41",
              capacity = 350,
              participantIds = generateParticipants("user41", 275)),
          Event(
              uid = "event42",
              title = "3D Printing Workshop",
              description = "Learn 3D design and printing",
              date = now,
              location = Location("Maker Space", 46.5165, 6.5710),
              tags = listOf("Tech", "Workshop"),
              public = true,
              ownerId = "user42",
              capacity = 70,
              participantIds = generateParticipants("user42", 48)),
          Event(
              uid = "event43",
              title = "Dance Class",
              description = "Learn salsa dancing",
              date = now,
              location = Location("Dance Studio", 46.5142, 6.5655),
              tags = listOf("Dance", "Social"),
              public = true,
              ownerId = "user43",
              capacity = 110,
              participantIds = generateParticipants("user43", 82)),
          Event(
              uid = "event44",
              title = "Debate Club",
              description = "Sharpen your argumentation skills",
              date = now,
              location = Location("Debate Hall", 46.5238, 6.5645),
              tags = listOf("Debate", "Education"),
              public = true,
              ownerId = "user44",
              capacity = 60,
              participantIds = generateParticipants("user44", 38)),
          Event(
              uid = "event45",
              title = "Astronomy Night",
              description = "Stargazing with telescopes",
              date = now,
              location = Location("Observatory", 46.5260, 6.5680),
              tags = listOf("Science", "Astronomy"),
              public = true,
              ownerId = "user45",
              capacity = 100,
              participantIds = generateParticipants("user45", 72)),

          // Additional high-attendance events for better heatmap distribution
          Event(
              uid = "event46",
              title = "International Food Festival",
              description = "Cuisines from around the world",
              date = now,
              location = Location("Central Plaza", 46.5168, 6.5670),
              tags = listOf("Food", "Festival"),
              public = true,
              ownerId = "user46",
              capacity = 800,
              participantIds = generateParticipants("user46", 742)), // Massive event, nearly full
          Event(
              uid = "event47",
              title = "Student Party",
              description = "End of semester celebration",
              date = now,
              location = Location("Party Venue", 46.5152, 6.5690),
              tags = listOf("Party", "Social"),
              public = true,
              ownerId = "user47",
              capacity = 600,
              participantIds = generateParticipants("user47", 580)), // Huge party
          Event(
              uid = "event48",
              title = "Rock Concert",
              description = "Local bands performing",
              date = now,
              location = Location("Outdoor Stage", 46.5245, 6.5715),
              tags = listOf("Music", "Rock"),
              public = true,
              ownerId = "user48",
              capacity = 550,
              participantIds = generateParticipants("user48", 488)),
          Event(
              uid = "event49",
              title = "Marathon Training",
              description = "Group marathon preparation",
              date = now,
              location = Location("Running Track", 46.5135, 6.5645),
              tags = listOf("Sports", "Running"),
              public = true,
              ownerId = "user49",
              capacity = 140,
              participantIds = generateParticipants("user49", 105)),
          Event(
              uid = "event50",
              title = "Craft Beer Festival",
              description = "Local breweries showcase",
              date = now,
              location = Location("Beer Garden", 46.5175, 6.5615),
              tags = listOf("Food", "Festival"),
              public = true,
              ownerId = "user50",
              capacity = 450,
              participantIds = generateParticipants("user50", 398))) // Very popular
    }
  }
}
