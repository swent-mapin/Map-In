package com.swent.mapin.map

/*
 * This file was partially written with the assistance of AI tools (programming assistant).
 * Reviewed and adjusted by the Map'In team.
 */

import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng

/*
 * ViewModel for the map screen.
 * - Purpose: expose data needed by the UI to display the map.
 * - Currently: provides only the initial camera position (Lausanne).
 * - Future: add StateFlows for markers, user location, UI state, etc.
 */
class MapViewModel : ViewModel() {

    /**
     * Initial camera position (Lausanne).
     * Used to center the map on first display.
     */
    val initialCamera: LatLng = LatLng(46.5197, 6.6323)
}
