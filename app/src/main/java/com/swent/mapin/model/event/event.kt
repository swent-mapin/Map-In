package com.swent.mapin.model.event

import com.google.firebase.Timestamp
import com.swent.mapin.model.Location

/**
 * @property uid event id
 * @property title event title
 * @property description event description
 * @property date event date
 * @property location event location
 * @property tags list of event types
 * @property public is event public or private
 * @property ownerId user id of the event creator
 * @property imageUrl url of the event image
 * @property capacity maximum number of attendees
 * @property attendeeCount current number of attendees
 */
data class Event(
    val uid: String = "",
    val title: String = "",
    val url: String = "",
    val description: String = "",
    val date: Timestamp? = null,
    val location: Location = Location("", 0.0, 0.0),
    val tags: List<String> = emptyList(),
    val public: Boolean = true,
    val ownerId: String = "",
    val imageUrl: String? = null,
    val capacity: Int? = 0,
    val attendeeCount: Int? = 0,
)
