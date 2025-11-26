package com.swent.mapin.ui.map.directions

//Assisted by AI

import android.util.Log
import com.mapbox.geojson.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Result of a directions request containing route points and information.
 */
data class DirectionsResult(
    val routePoints: List<Point>,
    val routeInfo: RouteInfo
)

/**
 * Service responsible for fetching walking directions from the Mapbox Directions API.
 *
 * This service provides a simple interface to retrieve route geometry between two points using the
 * walking profile. It handles network requests and parses the response to extract route
 * coordinates.
 *
 * @property accessToken Mapbox access token for API authentication
 */
class MapboxDirectionsService(private val accessToken: String) {

  private val client = OkHttpClient()
  private val baseUrl = "https://api.mapbox.com/directions/v5/mapbox/walking"

  /**
   * Fetches walking directions between two points.
   *
   * Makes a network call to the Mapbox Directions API and returns the route as a list of geographic
   * points. The route follows actual walkable paths and sidewalks.
   *
   * @param origin Starting point (user's current location)
   * @param destination End point (event location)
   * @return DirectionsResult with route points and info, or null if the request fails
   */
  suspend fun getDirections(origin: Point, destination: Point): DirectionsResult? =
      withContext(Dispatchers.IO) {
        try {
          val coordinates =
              "${origin.longitude()},${origin.latitude()};${destination.longitude()},${destination.latitude()}"
          val url =
              "$baseUrl/$coordinates?geometries=geojson&overview=full&access_token=$accessToken"

          val request = Request.Builder().url(url).build()

          val response = client.newCall(request).execute()
          if (!response.isSuccessful) {
            Log.e("MapboxDirections", "API request failed: ${response.code}")
            return@withContext null
          }

          val responseBody = response.body?.string() ?: return@withContext null
          parseDirectionsResponse(responseBody)
        } catch (e: Exception) {
          Log.e("MapboxDirections", "Error fetching directions", e)
          null
        }
      }

  /**
   * Parses the Mapbox Directions API response to extract route coordinates.
   *
   * @param jsonResponse JSON string response from the API
   * @return DirectionsResult with route points and info, or null if parsing fails
   */
  private fun parseDirectionsResponse(jsonResponse: String): DirectionsResult? {
    try {
      val json = JSONObject(jsonResponse)
      val routes = json.getJSONArray("routes")

      if (routes.length() == 0) {
        Log.w("MapboxDirections", "No routes found in response")
        return null
      }

      val route = routes.getJSONObject(0)
      val geometry = route.getJSONObject("geometry")
      val coordinates = geometry.getJSONArray("coordinates")

      val points = mutableListOf<Point>()
      for (i in 0 until coordinates.length()) {
        val coord = coordinates.getJSONArray(i)
        val lng = coord.getDouble(0)
        val lat = coord.getDouble(1)
        points.add(Point.fromLngLat(lng, lat))
      }

      val distance = route.optDouble("distance", 0.0)
      val duration = route.optDouble("duration", 0.0)
      val routeInfo = RouteInfo(distance = distance, duration = duration)

      return DirectionsResult(routePoints = points, routeInfo = routeInfo)
    } catch (e: Exception) {
      Log.e("MapboxDirections", "Error parsing response", e)
      return null
    }
  }
}
