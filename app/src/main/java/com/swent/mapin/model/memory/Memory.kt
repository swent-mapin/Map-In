package com.swent.mapin.model.memory

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import com.swent.mapin.model.location.Location

/**
 * Represents a memory created by a user, optionally linked to an event.
 *
 * @property uid Unique identifier for the memory
 * @property title Optional title for the memory
 * @property description Description of what happened (mandatory field)
 * @property eventId Optional reference to an event this memory is linked to
 * @property ownerId User ID of the memory creator
 * @property isPublic Whether this memory is visible to others (on event page if linked)
 * @property createdAt Timestamp when the memory was created
 * @property mediaUrls List of photo/video URLs associated with this memory
 * @property taggedUserIds List of user IDs tagged in this memory
 */
data class Memory(
    val uid: String = "",
    val title: String = "",
    val description: String = "",
    val eventId: String? = null,
    val ownerId: String = "",
    val location: Location = Location.UNDEFINED,
    @get:PropertyName("isPublic") val isPublic: Boolean = false,
    val createdAt: Timestamp? = null,
    val mediaUrls: List<String> = emptyList(),
    val taggedUserIds: List<String> = emptyList()
)
