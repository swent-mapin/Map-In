package com.swent.mapin.map

/*
 * This file was partially written with the assistance of AI tools (programming assistant).
 * Reviewed and adjusted by the Map'In team.
 */

import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.heatmaps.WeightedLatLng
import com.swent.mapin.model.Location
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * MapViewModel
 *
 * Responsibilities:
 * - Exposes the initial camera position for the map.
 * - Holds a list of current "events" (here modeled via Location with an attendees count) as a StateFlow.
 * - Precomputes a "historical activity" background as a heatmap-friendly point cloud made of
 *   multiple irregular blobs (cloud-like shapes) across the Lausanne area.
 *
 * Notes on the historical cloud:
 * - The cloud is synthesized deterministically (fixed seed) to provide a stable visual background
 *   during development and testing.
 * - Each blob is an oriented ellipse with a wavy boundary; the interior is filled using a regular
 *   grid with slight jitter to achieve an organic look. A radial falloff assigns higher weights
 *   toward the center and lower weights near the boundary.
 */
class MapViewModel : ViewModel() {

    /** Initial camera position (Lausanne). Used on first render. */
    val initialCamera: LatLng = LatLng(46.5197, 6.6323)

    /**
     * Sample set of "events" represented as Location items with an `attendees` count.
     * These are used both for marker display and to build a foreground heatmap layer.
     */
    private val initialEvents: List<Location> = listOf(
        Location(name = "Renens Gare", latitude = 46.5382, longitude = 6.5818, attendees = 180),
        Location(name = "UNIL Dorigny", latitude = 46.5219, longitude = 6.5794, attendees = 120),
        Location(name = "Vidy Plage", latitude = 46.5178, longitude = 6.5944, attendees = 200),
        Location(name = "Parc de Sauvabelin", latitude = 46.5363, longitude = 6.6325, attendees = 90),
        Location(name = "Pully Centre", latitude = 46.5123, longitude = 6.6609, attendees = 130),
        Location(name = "Saint-François", latitude = 46.5191, longitude = 6.6329, attendees = 250),
        Location(name = "Esplanade de Montbenon", latitude = 46.5216, longitude = 6.6307, attendees = 170),
        Location(name = "Malley", latitude = 46.5239, longitude = 6.6072, attendees = 110),
        Location(name = "Lutry Port", latitude = 46.5047, longitude = 6.6862, attendees = 80),
        Location(name = "Chavannes-près-Renens", latitude = 46.5317, longitude = 6.5706, attendees = 140),
    )

    /**
     * Reactive stream of events. UI collects this StateFlow to render markers and heatmap weights.
     */
    private val _events = MutableStateFlow(initialEvents)
    val events: StateFlow<List<Location>> = _events.asStateFlow()

    /**
     * Pre-generated "historical" cloud used as a low-contrast background heatmap layer.
     *
     * Implementation details:
     * - Multiple blobs scattered around the initial camera.
     * - Each blob is an oriented ellipse with a sinusoidal boundary (wavy edge).
     * - The area is filled using a grid in lat/lon space; each accepted point is jittered slightly.
     * - Weights are higher near the center and taper toward the boundary.
     */
    val historicalCloud: List<WeightedLatLng> =
        generateBlobbyClouds(
            center = initialCamera,
            areaRadiusMeters = 3500.0,   // How far blob centers can be from the main center.
            blobCount = 8,               // Number of distinct blobs ("small clouds").
            minBlobRadiusMeters = 400.0, // Minimum blob "radius" (size baseline).
            maxBlobRadiusMeters = 900.0, // Maximum blob "radius".
            stepMeters = 120.0,          // Grid spacing; lower -> denser point cloud.
            seed = 2025L                 // Fixed seed for determinism in tests/demos.
        )

    /**
     * Replaces the list of current events.
     * Useful when wiring to a repository or user-driven updates.
     */
    fun setEvents(newEvents: List<Location>) {
        _events.value = newEvents
    }

    /**
     * Generates a collection of weighted lat/lon points forming several irregular "cloud" blobs.
     *
     * Approach:
     * - Work in an equirectangular approximation near the given center:
     *   convert meters <-> degrees using latitude-dependent meters-per-degree factors.
     * - For each blob:
     *   1) Sample a random center within a disk of radius `areaRadiusMeters` (sqrt trick for uniform area).
     *   2) Define an oriented ellipse (axes `ax`, `ay`) with random anisotropy and rotation.
     *   3) Modulate the boundary with a combination of sinusoids to create a wavy, organic outline.
     *   4) Scan a bounding box with a regular grid; accept points whose normalized radius <= boundary.
     *   5) Add slight positional jitter to avoid visible grid patterns.
     *   6) Compute a weight with a simple center-to-edge falloff plus small randomness.
     *
     * Caveats:
     * - This is a visual heuristic, not a geodesically exact model. For small radii around a city,
     *   the equirectangular approximation is sufficient and cheaper than haversine/spherical math.
     *
     * @param center Center of the overall area (reference for the meters-to-degrees conversion).
     * @param areaRadiusMeters Max distance for blob centers from `center`.
     * @param blobCount Number of blobs to generate.
     * @param minBlobRadiusMeters Minimum baseline radius for a blob.
     * @param maxBlobRadiusMeters Maximum baseline radius for a blob.
     * @param stepMeters Grid spacing used to fill the blob interior.
     * @param seed RNG seed to keep the result deterministic.
     * @return A list of heatmap-ready weighted points.
     */
    private fun generateBlobbyClouds(
        center: LatLng,
        areaRadiusMeters: Double,
        blobCount: Int,
        minBlobRadiusMeters: Double,
        maxBlobRadiusMeters: Double,
        stepMeters: Double,
        seed: Long
    ): List<WeightedLatLng> {
        val rnd = Random(seed)

        // Reference latitude/longitude for meter<->degree conversions.
        // Equirectangular approximation near 'center':
        // - One degree of latitude ~ 111.32 km.
        // - One degree of longitude ~ 111.32 km * cos(latitude).
        val lat0 = center.latitude
        val lon0 = center.longitude
        val cosLat0 = cos(Math.toRadians(lat0))
        val metersPerDegLat = 111_320.0
        val metersPerDegLon = 111_320.0 * cosLat0

        // Grid step expressed in degrees.
        val dLat = stepMeters / metersPerDegLat
        val dLon = stepMeters / metersPerDegLon

        // Max random jitter applied to each accepted point (keeps patterns organic).
        val jitterLatMax = (stepMeters * 0.35) / metersPerDegLat
        val jitterLonMax = (stepMeters * 0.35) / metersPerDegLon

        val points = ArrayList<WeightedLatLng>()

        repeat(blobCount) {
            // 1) Sample a blob center within a disk of radius 'areaRadiusMeters'.
            // Using r = sqrt(u) for uniform area distribution.
            val thetaC = rnd.nextDouble(0.0, 2 * PI)
            val rC = sqrt(rnd.nextDouble()) * areaRadiusMeters
            val blobLat = lat0 + (rC * cos(thetaC)) / metersPerDegLat
            val blobLon = lon0 + (rC * sin(thetaC)) / metersPerDegLon

            // 2) Define an oriented ellipse via anisotropy and rotation.
            val baseR = rnd.nextDouble(minBlobRadiusMeters, maxBlobRadiusMeters)
            val anisotropy = rnd.nextDouble(0.7, 1.4) // 1.0 -> circle; otherwise ellipse.
            val ax = baseR * anisotropy   // Semi-axis along x (after rotation).
            val ay = baseR / anisotropy   // Semi-axis along y (after rotation).
            val rot = rnd.nextDouble(0.0, 2 * PI) // Random orientation.

            // 3) Wavy boundary: combine a few sinusoidal modes for an irregular outline.
            val a1 = rnd.nextDouble(0.08, 0.22)
            val a2 = rnd.nextDouble(0.05, 0.18)
            val phi1 = rnd.nextDouble(0.0, 2 * PI)
            val phi2 = rnd.nextDouble(0.0, 2 * PI)

            // Bounding box around the blob (in degrees), large enough to cover the ellipse.
            val latMin = blobLat - baseR / metersPerDegLat
            val latMax = blobLat + baseR / metersPerDegLat
            val lonMin = blobLon - baseR / metersPerDegLon
            val lonMax = blobLon + baseR / metersPerDegLon

            // 4) Scan the box with a regular grid; accept points inside the wavy boundary.
            var lat = latMin
            while (lat <= latMax) {
                var lon = lonMin
                while (lon <= lonMax) {
                    // Local coordinates (meters) relative to the blob center.
                    val dy = (lat - blobLat) * metersPerDegLat
                    val dx = (lon - blobLon) * metersPerDegLon

                    // Apply rotation to align with the ellipse axes.
                    val xr = dx * cos(rot) - dy * sin(rot)
                    val yr = dx * sin(rot) + dy * cos(rot)

                    // Normalized radius in the ellipse metric.
                    val rNorm = sqrt((xr * xr) / (ax * ax) + (yr * yr) / (ay * ay))

                    // Angular position and the wavy boundary multiplier at this angle.
                    val angle = atan2(yr, xr)
                    val boundary =
                        1.0 + a1 * sin(3.0 * angle + phi1) + a2 * sin(5.0 * angle + phi2)

                    if (rNorm <= boundary) {
                        // 5) Slight jitter to break the grid look.
                        val jLat = lat + rnd.nextDouble(-jitterLatMax, jitterLatMax)
                        val jLon = lon + rnd.nextDouble(-jitterLonMax, jitterLonMax)

                        // 6) Weight with simple center-to-edge falloff and tiny randomness.
                        val falloff = 1.0 - min(1.0, rNorm / boundary)
                        val weight =
                            (0.55 + 0.4 * falloff + rnd.nextDouble(0.0, 0.05)).coerceAtMost(1.0)

                        points.add(WeightedLatLng(LatLng(jLat, jLon), weight))
                    }
                    lon += dLon
                }
                lat += dLat
            }
        }

        return points
    }
}
