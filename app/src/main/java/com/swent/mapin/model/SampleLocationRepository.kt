package com.swent.mapin.model

/**
 * A sample repository that provides hardcoded location data for development and testing. This can
 * be replaced with a real repository implementation that fetches data from a backend.
 */
object SampleLocationRepository {

  /**
   * Returns a list of sample locations with varying attendee counts. These locations are positioned
   * around EPFL in Lausanne, Switzerland.
   */
  fun getSampleLocations(): List<Location> {
    return listOf(
        // Low attendance cluster near the Rolex Learning Center
        Location("Student Meetup", 46.5199, 6.5664, attendees = 4),
        Location("Coding Jam", 46.5201, 6.5666, attendees = 9),

        // Moderate attendance events around Esplanade
        Location("Farmers Market", 46.5208, 6.5655, attendees = 18),
        Location("Street Food Night", 46.5205, 6.5659, attendees = 28),
        Location("Pop-up Gallery", 46.5203, 6.5662, attendees = 36),

        // High attendance hotspots clustered near the convention centre
        Location("Music Festival", 46.5195, 6.5672, attendees = 85),
        Location("eSports Finals", 46.5192, 6.5675, attendees = 120),
        Location("Fireworks Show", 46.5190, 6.5678, attendees = 160),

        // Very high attendance to trigger red tones
        Location("National Day Concert", 46.5186, 6.5681, attendees = 240),
        Location("International Expo", 46.5183, 6.5684, attendees = 320))
  }
}
