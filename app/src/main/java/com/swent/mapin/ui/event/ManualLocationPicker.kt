package com.swent.mapin.ui.event

import android.content.Context
import android.location.LocationManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxDelicateApi
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotationGroup
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.maps.extension.compose.style.standard.rememberStandardStyleState
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.swent.mapin.R
import com.swent.mapin.model.location.Location
import com.swent.mapin.ui.map.MapConstants
import com.swent.mapin.ui.map.components.drawableToBitmap
import java.util.Locale

internal fun formatPinnedLocationLabel(lat: Double, lng: Double): String {
  return "Pinned location (${String.format(Locale.getDefault(), "%.5f, %.5f", lat, lng)})"
}

internal fun getLastKnownUserPoint(context: Context): Point? {
  val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
  val hasPermission =
      ContextCompat.checkSelfPermission(
          context, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
          PermissionChecker.PERMISSION_GRANTED ||
          ContextCompat.checkSelfPermission(
              context, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
              PermissionChecker.PERMISSION_GRANTED
  if (!hasPermission) return null

  val providers =
      listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).mapNotNull { provider
        ->
        runCatching { lm.getLastKnownLocation(provider) }.getOrNull()
      }
  val loc = providers.maxByOrNull { it.time } ?: return null
  return Point.fromLngLat(loc.longitude, loc.latitude)
}

@OptIn(MapboxDelicateApi::class)
@Composable
internal fun ManualLocationPickerDialog(
    initialLocation: Location?,
    onDismiss: () -> Unit,
    onLocationPicked: (Location) -> Unit,
    searchResults: List<Location>,
    onSearchQuery: (String) -> Unit,
    onSearchResultSelect: (Location) -> Unit,
    recenterPoint: Point? = null,
    locationExpanded: MutableState<Boolean>? = null
) {
  val context = LocalContext.current
  val focusManager = LocalFocusManager.current
  val startPoint =
      recenterPoint
          ?: initialLocation?.let { loc ->
            if (loc.latitude != null && loc.longitude != null) {
              Point.fromLngLat(loc.longitude, loc.latitude)
            } else null
          }
          ?: searchResults.firstOrNull()?.let { loc ->
            if (loc.latitude != null && loc.longitude != null) {
              Point.fromLngLat(loc.longitude!!, loc.latitude!!)
            } else null
          }
          ?: Point.fromLngLat(MapConstants.DEFAULT_LONGITUDE, MapConstants.DEFAULT_LATITUDE)

  val initialPickedPoint =
      initialLocation?.let { loc ->
        if (loc.latitude != null && loc.longitude != null) {
          Point.fromLngLat(loc.longitude, loc.latitude)
        } else null
      } ?: recenterPoint
  var pickedPoint by remember(startPoint) { mutableStateOf<Point?>(initialPickedPoint) }
  val mapViewportState = rememberMapViewportState {
    setCameraOptions {
      center(startPoint)
      zoom(MapConstants.DEFAULT_ZOOM.toDouble())
    }
  }
  val standardStyleState = rememberStandardStyleState()
  val markerBitmap = remember(context) { context.drawableToBitmap(R.drawable.ic_map_marker) }
  val screenHeight = LocalConfiguration.current.screenHeightDp.dp
  val dialogMapHeight = (screenHeight * 0.7f).coerceAtLeast(360.dp)
  var searchQuery by remember { mutableStateOf("") }
  var showResults by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) {
    locationExpanded?.value = false
    if (initialPickedPoint == null) {
      mapViewportState.setCameraOptions {
        center(startPoint)
        zoom(14.0)
      }
    }
  }

  Dialog(
      onDismissRequest = onDismiss,
      properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 6.dp) {
          Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(text = "Select location on map", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                  OutlinedTextField(
                      value = searchQuery,
                      onValueChange = {
                        searchQuery = it
                        onSearchQuery(it)
                        showResults = it.isNotBlank()
                      },
                      modifier = Modifier.weight(1f),
                      placeholder = { Text("Search place or address") },
                      singleLine = true)
                  OutlinedButton(
                      onClick = {
                        recenterPoint?.let { pt ->
                          pickedPoint = pt
                          mapViewportState.setCameraOptions {
                            center(pt)
                            zoom(16.0)
                          }
                        }
                      },
                      enabled = recenterPoint != null) {
                        Text("My location")
                      }
                }
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(dialogMapHeight)) {
              val annotations =
                  remember(pickedPoint, markerBitmap) {
                    pickedPoint?.let { point ->
                      val opts = PointAnnotationOptions().withPoint(point)
                      markerBitmap?.let { opts.withIconImage(it) }
                      listOf(opts)
                    } ?: emptyList()
                  }
              MapboxMap(
                  modifier = Modifier.fillMaxWidth().height(dialogMapHeight),
                  mapViewportState = mapViewportState,
                  style = { MapboxStandardStyle(standardStyleState = standardStyleState) }) {
                    MapEffect(Unit) { mapView ->
                      val listener = OnMapClickListener { point ->
                        pickedPoint = point
                        mapViewportState.setCameraOptions { center(point) }
                        showResults = false
                        searchQuery = ""
                        onSearchQuery("")
                        focusManager.clearFocus()
                        true
                      }
                      mapView.gestures.addOnMapClickListener(listener)
                    }

                    if (annotations.isNotEmpty()) {
                      PointAnnotationGroup(annotations = annotations) {
                        iconAnchor = IconAnchor.BOTTOM
                      }
                    }
                  }
              if (showResults && searchResults.isNotEmpty()) {
                Column(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
                  Surface(shape = RoundedCornerShape(8.dp), tonalElevation = 4.dp) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                      searchResults.forEachIndexed { idx, loc ->
                        TextButton(
                            onClick = {
                              if (loc.latitude != null && loc.longitude != null) {
                                val point = Point.fromLngLat(loc.longitude!!, loc.latitude!!)
                                pickedPoint = point
                                mapViewportState.setCameraOptions {
                                  center(point)
                                  zoom(16.0)
                                }
                              }
                              onSearchResultSelect(loc)
                              searchQuery = ""
                              onSearchQuery("")
                              focusManager.clearFocus()
                              showResults = false
                            },
                            modifier = Modifier.fillMaxWidth()) {
                              Text(loc.name ?: "Result ${idx + 1}")
                            }
                      }
                    }
                  }
                }
              }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically) {
                  TextButton(onClick = onDismiss) { Text(text = "Cancel") }
                  Spacer(modifier = Modifier.width(8.dp))
                  Button(
                      onClick = {
                        pickedPoint?.let { point ->
                          val lat = point.latitude()
                          val lng = point.longitude()
                          val label = formatPinnedLocationLabel(lat, lng)
                          val loc = Location.from(label, lat, lng)
                          onLocationPicked(loc)
                        }
                      },
                      enabled = pickedPoint != null) {
                        Text(text = "Use this location")
                      }
                }
          }
        }
      }
}
