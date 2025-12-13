package com.swent.mapin.ui.map.components

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.annotation.VisibleForTesting
import com.swent.mapin.R
import com.swent.mapin.model.event.Event

/**
 * Represents different event pin types that map to specific icons on the map. Each type has
 * associated drawable resources for different capacity states.
 *
 * @property greenIcon Drawable resource for events with plenty of spots (>50% capacity)
 * @property orangeIcon Drawable resource for events with limited spots (10-50% capacity)
 * @property redIcon Drawable resource for events with few/no spots (<10% capacity or full)
 * @property keywords List of tag keywords that map to this pin type
 */
enum class EventPinType(
    @DrawableRes val greenIcon: Int,
    @DrawableRes val orangeIcon: Int,
    @DrawableRes val redIcon: Int,
    val keywords: List<String>
) {
  MUSIC(
      greenIcon = R.drawable.ic_music_green,
      orangeIcon = R.drawable.ic_music_orange,
      redIcon = R.drawable.ic_music_red,
      keywords = listOf("music", "concert", "festival", "band", "dj", "live", "song", "sing")),
  SPORTS(
      greenIcon = R.drawable.ic_sports_green,
      orangeIcon = R.drawable.ic_sports_orange,
      redIcon = R.drawable.ic_sports_red,
      keywords =
          listOf(
              "sport",
              "basketball",
              "football",
              "soccer",
              "volleyball",
              "running",
              "tennis",
              "swimming",
              "fitness",
              "yoga",
              "gym",
              "cycling",
              "hiking",
              "wellness",
              "run",
              "bike",
              "swim")),
  FOOD(
      greenIcon = R.drawable.ic_food_green,
      orangeIcon = R.drawable.ic_food_orange,
      redIcon = R.drawable.ic_food_red,
      keywords =
          listOf(
              "food",
              "restaurant",
              "cooking",
              "cuisine",
              "dinner",
              "lunch",
              "breakfast",
              "market",
              "farmer",
              "eat",
              "meal")),
  SCIENCE(
      greenIcon = R.drawable.ic_science_green,
      orangeIcon = R.drawable.ic_science_orange,
      redIcon = R.drawable.ic_science_red,
      keywords =
          listOf(
              "science",
              "technology",
              "tech",
              "robotic",
              "physics",
              "chemistry",
              "biology",
              "engineering",
              "coding",
              "programming",
              "conference",
              "workshop",
              "exhibition",
              "expo",
              "code",
              "lab")),
  DEFAULT(
      greenIcon = R.drawable.ic_map_marker_green,
      orangeIcon = R.drawable.ic_map_marker_orange,
      redIcon = R.drawable.ic_map_marker_red,
      keywords = emptyList());

  companion object {
    /**
     * Determines the appropriate pin type based on the event's tags. Scans all tags to find the
     * first matching keyword. Falls back to DEFAULT if no matching tag is found.
     *
     * @param event The event to determine pin type for
     * @return The matching EventPinType
     */
    @VisibleForTesting
    fun fromEvent(event: Event): EventPinType {
      if (event.tags.isEmpty()) return DEFAULT

      // Check all tags to find the first match
      for (tag in event.tags) {
        val lowerTag = tag.lowercase()
        val matchingType =
            entries.firstOrNull { pinType ->
              pinType != DEFAULT && pinType.keywords.any { keyword -> lowerTag.contains(keyword) }
            }
        if (matchingType != null) return matchingType
      }

      return DEFAULT
    }
  }
}

/** Represents the capacity state of an event, which determines the pin color. */
enum class CapacityState {
  /** Plenty of spots available (>50% capacity remaining or unlimited) */
  AVAILABLE,
  /** Limited spots available (10-50% capacity remaining) */
  LIMITED,
  /** Few or no spots available (<10% capacity remaining or full) */
  FULL
}

/**
 * Calculates the capacity state based on the event's capacity and participant count.
 *
 * @param event The event to calculate capacity state for
 * @return The CapacityState representing the event's availability
 */
@VisibleForTesting
fun calculateCapacityState(event: Event): CapacityState {
  val capacity = event.capacity ?: return CapacityState.AVAILABLE // Unlimited capacity
  if (capacity <= 0) return CapacityState.AVAILABLE // Invalid capacity, treat as unlimited

  val participantCount = event.participantIds.size
  val remainingSpots = capacity - participantCount
  val remainingPercentage = remainingSpots.toDouble() / capacity

  return when {
    remainingPercentage > 0.5 -> CapacityState.AVAILABLE
    remainingPercentage > 0.1 -> CapacityState.LIMITED
    else -> CapacityState.FULL
  }
}

/**
 * Gets the appropriate drawable resource ID for an event based on its type and capacity.
 *
 * Delegates to [getEventPinInfo] to ensure single source of truth for pin drawable logic.
 *
 * @param event The event to get the pin icon for
 * @return The drawable resource ID for the appropriate pin icon
 */
@VisibleForTesting
@DrawableRes
fun getEventPinDrawableRes(event: Event): Int {
  return getEventPinInfo(event).drawableRes
}

/**
 * Gets the bitmap for an event's pin icon.
 *
 * @param context The context to load drawable resources
 * @param event The event to get the pin bitmap for
 * @return The Bitmap for the event's pin, or null if loading fails
 */
fun getEventPinBitmap(context: Context, event: Event): Bitmap? {
  val drawableRes = getEventPinDrawableRes(event)
  return context.drawableToBitmap(drawableRes)
}

/**
 * Data class containing the pin type and capacity state for an event. Used for creating annotations
 * with the appropriate icon.
 *
 * @property pinType The type of pin (based on event tag)
 * @property capacityState The capacity state (determines color)
 * @property drawableRes The drawable resource ID for the pin icon
 */
data class EventPinInfo(
    val pinType: EventPinType,
    val capacityState: CapacityState,
    @DrawableRes val drawableRes: Int
)

/**
 * Gets complete pin information for an event.
 *
 * @param event The event to get pin info for
 * @return EventPinInfo containing type, capacity state, and drawable resource
 */
@VisibleForTesting
fun getEventPinInfo(event: Event): EventPinInfo {
  val pinType = EventPinType.fromEvent(event)
  val capacityState = calculateCapacityState(event)
  val drawableRes =
      when (capacityState) {
        CapacityState.AVAILABLE -> pinType.greenIcon
        CapacityState.LIMITED -> pinType.orangeIcon
        CapacityState.FULL -> pinType.redIcon
      }

  return EventPinInfo(pinType, capacityState, drawableRes)
}
