package com.swent.mapin.model.event

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local Room entity representing a saved event for a user. Maps to the "saved_events" table in the
 * local database. Each instance represents one saved event associated with a specific user. Fields
 * correspond to the event's properties, with userId indicating which user saved it. Implemented
 * with the help of AI.
 */
@Entity(tableName = "saved_events")
data class SavedEventEntity(
    @PrimaryKey var id: String,
    @ColumnInfo(name = "user_id") var userId: String,
    @ColumnInfo(name = "title") var title: String,
    @ColumnInfo(name = "description") var description: String,
    @ColumnInfo(name = "date_seconds") var dateSeconds: Long?,
    @ColumnInfo(name = "date_nanoseconds") var dateNanoseconds: Int?,
    @ColumnInfo(name = "end_date_seconds") var endDateSeconds: Long?,
    @ColumnInfo(name = "end_date_nanoseconds") var endDateNanoseconds: Int?,
    @ColumnInfo(name = "location_name") var locationName: String,
    @ColumnInfo(name = "location_lat") var locationLat: Double,
    @ColumnInfo(name = "location_lng") var locationLng: Double,
    @ColumnInfo(name = "tags") var tagsCsv: String,
    @ColumnInfo(name = "public") var isPublic: Boolean,
    @ColumnInfo(name = "owner_id") var ownerId: String,
    @ColumnInfo(name = "image_url") var imageUrl: String?,
    @ColumnInfo(name = "capacity") var capacity: Int?,
    @ColumnInfo(name = "participant_ids") var participantIdsCsv: String,
    @ColumnInfo(name = "saved_at") var savedAtSeconds: Long?
)
