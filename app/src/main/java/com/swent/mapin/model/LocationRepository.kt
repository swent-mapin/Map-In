package com.swent.mapin.model

/** Interface for location search repositories. */
interface LocationRepository {
  suspend fun search(query: String): List<Location>
}
