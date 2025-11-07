package com.swent.mapin.ui.map.camera

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.swent.mapin.model.event.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Encapsulates camera-related side effects so the ViewModel only orchestrates state.
 *
 * Responsibilities:
 * - Track zoom gestures to drive UI (scale bar visibility).
 * - Run programmatic camera animations (center pin, fit list) while preventing sheet collapse.
 * - Expose callbacks for Compose to hook into Mapbox.
 */
class MapCameraController(private val scope: CoroutineScope) {

  var centerCameraCallback: ((Event, Boolean) -> Unit)? = null
  var fitCameraCallback: ((List<Event>) -> Unit)? = null

  private var hideScaleBarJob: Job? = null
  private var programmaticZoomJob: Job? = null

  private var _isZooming by mutableStateOf(false)
  val isZooming: Boolean
    get() = _isZooming

  private var _lastZoom by mutableFloatStateOf(0f)

  private var _isProgrammaticZoom by mutableStateOf(false)
  val isProgrammaticZoom: Boolean
    get() = _isProgrammaticZoom

  fun onZoomChange(newZoom: Float) {
    if (kotlin.math.abs(newZoom - _lastZoom) <= 0.01f) return
    _isZooming = true
    _lastZoom = newZoom

    hideScaleBarJob?.cancel()
    hideScaleBarJob =
        scope.launch {
          delay(300)
          _isZooming = false
        }
  }

  fun centerOnEvent(event: Event, forceZoom: Boolean) {
    _isProgrammaticZoom = true
    centerCameraCallback?.invoke(event, forceZoom)
    scheduleProgrammaticReset()
  }

  fun fitToEvents(events: List<Event>) {
    if (events.isEmpty()) return
    _isProgrammaticZoom = true
    fitCameraCallback?.invoke(events)
    scheduleProgrammaticReset()
  }

  fun clearCallbacks() {
    hideScaleBarJob?.cancel()
    programmaticZoomJob?.cancel()
    centerCameraCallback = null
    fitCameraCallback = null
  }

  private fun scheduleProgrammaticReset(delayMillis: Long = 1100) {
    programmaticZoomJob?.cancel()
    programmaticZoomJob =
        scope.launch {
          delay(delayMillis)
          _isProgrammaticZoom = false
        }
  }
}
