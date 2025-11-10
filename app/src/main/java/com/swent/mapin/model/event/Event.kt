package com.swent.mapin.model.event

import com.google.firebase.Timestamp
import com.swent.mapin.model.Location

/**
 * @property uid event id
 * @property title event title
 * @property description event description
 * @property date event date
 * @property endDate event end date
 * @property location location of the event (name, latitude, longitude)
 * @property tags list of event types
 * @property public is event public or private
 * @property ownerId user id of the event creator
 * @property imageUrl url of the event image
 * @property capacity maximum number of attendees
 * @property participantIds list of user IDs who are participating in this event
 */
data class Event(
    val uid: String = "",
    val title: String = "",
    val url: String? = null,
    val description: String = "",
    val date: Timestamp? = null,
    val endDate: Timestamp? = null,
    val location: Location = Location("", 0.0, 0.0),
    val tags: List<String> = emptyList(),
    val public: Boolean = true,
    val ownerId: String = "",
    val imageUrl: String? = null,
    val capacity: Int? = null,
    val participantIds: List<String> = emptyList(),
    val price: Double = 0.0
) {
  fun isValidEvent(): Boolean {
    if (ownerId.isBlank() || title.isBlank() || description.isBlank() || date == null || location.name.isBlank()) {
      return false
    }
    // if endDate is provided it must be the same or after start date
    if (endDate != null) {
      if (endDate.toDate().before(date.toDate())) return false
    }
    return true
  }
}
