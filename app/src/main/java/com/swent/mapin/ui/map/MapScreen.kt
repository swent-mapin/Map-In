package com.swent.mapin.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.google.gson.JsonPrimitive
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxDelicateApi
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.IconImage
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotationGroup
import com.mapbox.maps.extension.compose.style.BooleanValue
import com.mapbox.maps.extension.compose.style.ColorValue
import com.mapbox.maps.extension.compose.style.DoubleValue
import com.mapbox.maps.extension.compose.style.LongValue
import com.mapbox.maps.extension.compose.style.layers.generated.HeatmapLayer
import com.mapbox.maps.extension.compose.style.sources.GeoJSONData
import com.mapbox.maps.extension.compose.style.sources.generated.rememberGeoJsonSourceState
import com.mapbox.maps.extension.compose.style.standard.LightPresetValue
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardSatelliteStyle
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.maps.extension.compose.style.standard.rememberStandardStyleState
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.AnnotationSourceOptions
import com.mapbox.maps.plugin.annotation.ClusterOptions
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.swent.mapin.R
import com.swent.mapin.model.Location
import com.swent.mapin.testing.UiTestTags
import com.swent.mapin.ui.components.BottomSheet
import com.swent.mapin.ui.components.BottomSheetConfig
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

/** Map screen that layers Mapbox content with a bottom sheet driven by MapScreenViewModel. */
@OptIn(MapboxDelicateApi::class)
@Composable
fun MapScreen(onLocationClick: (Location) -> Unit = {}) {
  val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
  // Bottom sheet heights scale with the current device size
  val sheetConfig =
      BottomSheetConfig(
          collapsedHeight = MapConstants.COLLAPSED_HEIGHT,
          mediumHeight = MapConstants.MEDIUM_HEIGHT,
          fullHeight = screenHeightDp * MapConstants.FULL_HEIGHT_PERCENTAGE)

  val viewModel = rememberMapScreenViewModel(sheetConfig)
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(viewModel.errorMessage) {
    viewModel.errorMessage?.let { message ->
      snackbarHostState.showSnackbar(message)
      viewModel.clearError()
    }
  }

  // Start the camera centered on the default campus view
  val mapViewportState = rememberMapViewportState {
    setCameraOptions {
      zoom(MapConstants.DEFAULT_ZOOM.toDouble())
      center(Point.fromLngLat(MapConstants.DEFAULT_LONGITUDE, MapConstants.DEFAULT_LATITUDE))
      pitch(0.0)
      bearing(0.0)
    }
  }

  ObserveSheetStateForZoomUpdate(viewModel, mapViewportState)
  ObserveZoomForSheetCollapse(viewModel, mapViewportState)

  val density = LocalDensity.current
  val densityDpi = remember(density) { (density.density * 160).toInt() }
  val screenHeightPx = remember(screenHeightDp, density) { screenHeightDp.value * density.density }
  val sheetTopPx = screenHeightPx - (viewModel.currentSheetHeight.value * density.density)

  val isDarkTheme = isSystemInDarkTheme()
  val lightPreset = if (isDarkTheme) LightPresetValue.NIGHT else LightPresetValue.DAY

  // Disable POI labels to keep our annotations front-and-center
  val standardStyleState = rememberStandardStyleState {
    configurationsState.apply {
      this.lightPreset = lightPreset
      showPointOfInterestLabels = BooleanValue(true) // turn off if needed
    }
  }

  // Heatmap source mirrors the ViewModel locations list
  val heatmapSource =
      rememberGeoJsonSourceState(key = "locations-heatmap-source") {
        data = GeoJSONData(locationsToGeoJson(viewModel.locations))
      }

  LaunchedEffect(viewModel.locations) {
    heatmapSource.data = GeoJSONData(locationsToGeoJson(viewModel.locations))
  }

  Box(modifier = Modifier.fillMaxSize().testTag(UiTestTags.MAP_SCREEN)) {
    MapboxMap(
        Modifier.fillMaxSize()
            .then(
                Modifier.mapPointerInput(
                    bottomSheetState = viewModel.bottomSheetState,
                    sheetTopPx = sheetTopPx,
                    densityDpi = densityDpi,
                    onCollapseSheet = { viewModel.setBottomSheetState(BottomSheetState.COLLAPSED) },
                    checkTouchProximity = viewModel::checkTouchProximityToSheet)),
        mapViewportState = mapViewportState,
        style = {
          if (viewModel.useSatelliteStyle) {
            MapboxStandardSatelliteStyle(styleInteractionsState = null)
          } else {
            MapboxStandardStyle(standardStyleState = standardStyleState)
          }
        },
        compass = {
          Compass(
              modifier =
                  Modifier.align(Alignment.BottomEnd)
                      .padding(bottom = MapConstants.COLLAPSED_HEIGHT + 96.dp, end = 16.dp))
        },
        scaleBar = {
          AnimatedVisibility(
              visible = viewModel.isZooming,
              enter = fadeIn(),
              exit = fadeOut(),
              modifier = Modifier.align(Alignment.TopCenter).padding(top = 60.dp)) {
                ScaleBar()
              }
        }) {
          val context = LocalContext.current
          val markerBitmap =
              remember(context) { context.drawableToBitmap(R.drawable.ic_map_marker) }
          val marker = remember(markerBitmap) { markerBitmap?.let { IconImage(it) } }

          val annotationStyle =
              remember(isDarkTheme, markerBitmap) {
                createAnnotationStyle(isDarkTheme, markerBitmap)
              }

          val annotations =
              remember(viewModel.locations, annotationStyle) {
                createLocationAnnotations(viewModel.locations, annotationStyle)
              }

          val clusterConfig = remember { createClusterConfig() }

          if (viewModel.showHeatmap) {
            CreateHeatmapLayer(heatmapSource)

            PointAnnotationGroup(annotations = annotations) {
              marker?.let { iconImage = it }
              interactionsState.onClicked { annotation ->
                findLocationForAnnotation(annotation, viewModel.locations)?.let { location ->
                  onLocationClick(location)
                  true
                } ?: false
              }
            }
          } else {
            // Enable clustering when heatmap is off to reduce pin noise
            PointAnnotationGroup(annotations = annotations, annotationConfig = clusterConfig) {
              marker?.let { iconImage = it }
              interactionsState
                  .onClicked { annotation ->
                    findLocationForAnnotation(annotation, viewModel.locations)?.let { location ->
                      onLocationClick(location)
                      true
                    } ?: false
                  }
                  .onClusterClicked { clusterFeature ->
                    val feature = clusterFeature.originalFeature
                    val center = (feature.geometry() as? Point) ?: return@onClusterClicked false
                    val currentZoom =
                        mapViewportState.cameraState?.zoom ?: MapConstants.DEFAULT_ZOOM.toDouble()
                    val animationOptions = MapAnimationOptions.Builder().duration(450L).build()

                    mapViewportState.easeTo(
                        cameraOptions =
                            cameraOptions {
                              center(center)
                              zoom((currentZoom + 2.0).coerceAtMost(18.0))
                            },
                        animationOptions = animationOptions)
                    true
                  }
            }
          }
        }

    TopGradient()

    ScrimOverlay(
        currentHeightDp = viewModel.currentSheetHeight,
        mediumHeightDp = sheetConfig.mediumHeight,
        fullHeightDp = sheetConfig.fullHeight)

    MapStyleSelector(
        selectedStyle = viewModel.mapStyle,
        onStyleSelected = { style -> viewModel.setMapStyle(style) },
        modifier =
            Modifier.align(Alignment.BottomEnd)
                .padding(bottom = sheetConfig.collapsedHeight + 24.dp, end = 16.dp))

    // Block map gestures when the sheet is in focus
    ConditionalMapBlocker(bottomSheetState = viewModel.bottomSheetState)

    BottomSheet(
        config = sheetConfig,
        currentState = viewModel.bottomSheetState,
        onStateChange = { newState -> viewModel.setBottomSheetState(newState) },
        calculateTargetState = viewModel::calculateTargetState,
        stateToHeight = viewModel::getHeightForState,
        onHeightChange = { height -> viewModel.currentSheetHeight = height },
        modifier = Modifier.align(Alignment.BottomCenter).testTag("bottomSheet")) {
          BottomSheetContent(
              state = viewModel.bottomSheetState,
              fullEntryKey = viewModel.fullEntryKey,
              searchBarState =
                  SearchBarState(
                      query = viewModel.searchQuery,
                      shouldRequestFocus = viewModel.shouldFocusSearch,
                      onQueryChange = viewModel::onSearchQueryChange,
                      onTap = viewModel::onSearchTap,
                      onFocusHandled = viewModel::onSearchFocusHandled),
              showMemoryForm = viewModel.showMemoryForm,
              availableEvents = viewModel.availableEvents,
              onCreateMemoryClick = viewModel::showMemoryForm,
              onMemorySave = viewModel::onMemorySave,
              onMemoryCancel = viewModel::onMemoryCancel)
        }

    if (viewModel.isSavingMemory) {
      Box(
          modifier =
              Modifier.fillMaxSize()
                  .background(Color.Black.copy(alpha = 0.5f))
                  .testTag("memoryLoadingIndicator"),
          contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
          }
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp))
  }
}

/** Top gradient overlay for better visibility of status bar icons on map */
@Composable
private fun TopGradient() {
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .height(100.dp)
              .background(
                  brush =
                      Brush.verticalGradient(
                          colors = listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent))))
}

/** Scrim that fades in once the sheet passes MEDIUM, dimming the map underneath. */
@Composable
private fun ScrimOverlay(currentHeightDp: Dp, mediumHeightDp: Dp, fullHeightDp: Dp) {
  val opacity =
      if (currentHeightDp >= mediumHeightDp) {
        val progress =
            ((currentHeightDp - mediumHeightDp) / (fullHeightDp - mediumHeightDp)).coerceIn(0f, 1f)
        progress * MapConstants.MAX_SCRIM_ALPHA
      } else {
        0f
      }

  Box(
      modifier =
          Modifier.fillMaxSize()
              .testTag("scrimOverlay")
              .background(Color.Black.copy(alpha = opacity)))
}

/** Transparent overlay that consumes all map gestures while the sheet is fully expanded. */
@SuppressLint("ReturnFromAwaitPointerEventScope")
@Composable
private fun MapInteractionBlocker() {
  Box(
      modifier =
          Modifier.fillMaxSize().testTag("mapInteractionBlocker").pointerInput(Unit) {
            awaitPointerEventScope {
              while (true) {
                val event = awaitPointerEvent()
                event.changes.forEach { pointerInputChange -> pointerInputChange.consume() }
              }
            }
          })
}

/** Pointer modifier that collapses the sheet when touches originate near its top edge. */
private fun Modifier.mapPointerInput(
    bottomSheetState: BottomSheetState,
    sheetTopPx: Float,
    densityDpi: Int,
    onCollapseSheet: () -> Unit,
    checkTouchProximity: (Float, Float, Int) -> Boolean
) =
    this.pointerInput(bottomSheetState, sheetTopPx) {
      awaitPointerEventScope {
        while (true) {
          val event = awaitPointerEvent()
          if (event.type == PointerEventType.Move) {
            event.changes.firstOrNull()?.let { change ->
              val touchY = change.position.y
              if (checkTouchProximity(touchY, sheetTopPx, densityDpi)) {
                onCollapseSheet()
              }
            }
          }
        }
      }
    }

/** Updates the zoom baseline whenever the sheet settles in MEDIUM. */
@Composable
private fun ObserveSheetStateForZoomUpdate(
    viewModel: MapScreenViewModel,
    mapViewportState: MapViewportState
) {
  LaunchedEffect(viewModel.bottomSheetState, mapViewportState) {
    if (viewModel.bottomSheetState == BottomSheetState.MEDIUM) {
      mapViewportState.cameraState?.let { viewModel.updateMediumReferenceZoom(it.zoom.toFloat()) }
    }
  }
}

/** Collapses the sheet after zoom interactions and keeps zoom state in sync. */
@Composable
private fun ObserveZoomForSheetCollapse(
    viewModel: MapScreenViewModel,
    mapViewportState: MapViewportState
) {
  LaunchedEffect(mapViewportState) {
    snapshotFlow { mapViewportState.cameraState?.zoom?.toFloat() ?: 0f }
        .filterNotNull()
        .distinctUntilChanged()
        .collect { z ->
          viewModel.onZoomChange(z)

          if (viewModel.checkZoomInteraction(z)) {
            viewModel.setBottomSheetState(BottomSheetState.COLLAPSED)
          }
        }
  }
}

@Composable
private fun ConditionalMapBlocker(bottomSheetState: BottomSheetState) {
  if (bottomSheetState == BottomSheetState.FULL) {
    MapInteractionBlocker()
  }
}

private fun Context.drawableToBitmap(@DrawableRes drawableResId: Int): Bitmap? {
  val drawable = AppCompatResources.getDrawable(this, drawableResId) ?: return null
  val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1
  val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1
  val bitmap = createBitmap(width, height)
  val canvas = Canvas(bitmap)
  drawable.setBounds(0, 0, canvas.width, canvas.height)
  drawable.draw(canvas)
  return bitmap
}

private data class AnnotationStyle(
    val textColorInt: Int,
    val haloColorInt: Int,
    val markerBitmap: Bitmap?
)

private fun createAnnotationStyle(isDarkTheme: Boolean, markerBitmap: Bitmap?): AnnotationStyle {
  val textColor = if (isDarkTheme) Color.White else Color.Black
  val haloColor =
      if (isDarkTheme) {
        Color.Black.copy(alpha = 0.8f)
      } else {
        Color.White.copy(alpha = 0.85f)
      }

  return AnnotationStyle(
      textColorInt = textColor.toArgb(),
      haloColorInt = haloColor.toArgb(),
      markerBitmap = markerBitmap)
}

private fun createLocationAnnotations(
    locations: List<Location>,
    style: AnnotationStyle
): List<PointAnnotationOptions> {
  return locations.mapIndexed { index, loc ->
    PointAnnotationOptions()
        .withPoint(Point.fromLngLat(loc.longitude, loc.latitude))
        .apply { style.markerBitmap?.let { withIconImage(it) } }
        .withIconAnchor(IconAnchor.BOTTOM)
        .withTextAnchor(TextAnchor.TOP)
        .withTextOffset(listOf(0.0, 0.5))
        .withTextSize(14.0)
        .withTextColor(style.textColorInt)
        .withTextHaloColor(style.haloColorInt)
        .withTextHaloWidth(1.5)
        .withTextField(loc.name)
        .withData(JsonPrimitive(index))
  }
}

private fun createClusterConfig(): AnnotationConfig {
  val clusterColorLevels =
      listOf(
          0 to Color(0xFF64B5F6).toArgb(),
          25 to Color(0xFF1E88E5).toArgb(),
          50 to Color(0xFF0D47A1).toArgb())

  return AnnotationConfig(
      annotationSourceOptions =
          AnnotationSourceOptions(
              clusterOptions =
                  ClusterOptions(
                      clusterRadius = 60L,
                      colorLevels = clusterColorLevels,
                      textColor = Color.White.toArgb(),
                      textSize = 12.0)))
}

private fun findLocationForAnnotation(
    annotation: com.mapbox.maps.plugin.annotation.generated.PointAnnotation,
    locations: List<Location>
): Location? {
  val index = annotation.getData()?.takeIf { it.isJsonPrimitive }?.asInt
  return index?.let { locations.getOrNull(it) }
      ?: locations.firstOrNull { location ->
        val point = annotation.point
        location.latitude == point.latitude() && location.longitude == point.longitude()
      }
}

@Composable
private fun CreateHeatmapLayer(
    heatmapSource: com.mapbox.maps.extension.compose.style.sources.generated.GeoJsonSourceState
) {
  HeatmapLayer(sourceState = heatmapSource, layerId = "locations-heatmap") {
    maxZoom = LongValue(18L)
    heatmapOpacity = DoubleValue(0.65)
    heatmapRadius =
        DoubleValue(
            interpolate {
              linear()
              zoom()
              stop {
                literal(0.0)
                literal(18.0)
              }
              stop {
                literal(14.0)
                literal(32.0)
              }
              stop {
                literal(22.0)
                literal(48.0)
              }
            })
    heatmapWeight =
        DoubleValue(
            interpolate {
              linear()
              get { literal("weight") }
              stop {
                literal(0.0)
                literal(0.0)
              }
              stop {
                literal(5.0)
                literal(0.4)
              }
              stop {
                literal(25.0)
                literal(0.8)
              }
              stop {
                literal(100.0)
                literal(1.0)
              }
            })
    heatmapColor =
        ColorValue(
            interpolate {
              linear()
              heatmapDensity()
              MapConstants.HeatmapColors.COLOR_STOPS.forEach { (position, color) ->
                stop {
                  literal(position)
                  if (color.a == 0.0) {
                    rgba(color.r, color.g, color.b, color.a)
                  } else {
                    rgb(color.r, color.g, color.b)
                  }
                }
              }
            })
  }
}
