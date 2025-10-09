package com.swent.mapin.model

/**
 * A sample repository that provides hardcoded location data for development and testing.
 * This can be replaced with a real repository implementation that fetches data from a backend.
 */
object SampleLocationRepository {

  /**
   * Returns a list of sample locations with varying attendee counts.
   * These locations are positioned around EPFL in Lausanne, Switzerland.
   */
  fun getSampleLocations(): List<Location> {
    return listOf(
      Location("Music Festival", 46.5197, 6.5668, attendees = 200),
      Location("Basketball Game", 46.5217, 6.5688, attendees = 50),
      Location("Art Exhibition", 46.5177, 6.5653, attendees = 120),
      Location("Food Market", 46.5212, 6.5658, attendees = 80),
      Location("Yoga Class", 46.5182, 6.5683, attendees = 30)
    )
  }
}

