package com.swent.mapin.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import com.mapbox.maps.extension.compose.style.sources.generated.GeoJsonSourceState
import com.mapbox.maps.extension.compose.style.sources.generated.rememberGeoJsonSourceState
import com.mapbox.maps.extension.compose.style.standard.LightPresetValue
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardSatelliteStyle
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.maps.extension.compose.style.standard.StandardStyleState
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
import com.swent.mapin.model.event.Event
import com.swent.mapin.testing.UiTestTags
import com.swent.mapin.ui.components.BottomSheet
import com.swent.mapin.ui.components.BottomSheetConfig
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

/** Map screen that layers Mapbox content with a bottom sheet driven by MapScreenViewModel. */
@OptIn(MapboxDelicateApi::class)
@Composable
fun MapScreen(
    onEventClick: (Event) -> Unit = {},
    renderMap: Boolean = true,
    onNavigateToProfile: () -> Unit = {}
) {
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

  // Setup camera centering callback
  val screenHeightDpValue = screenHeightDp.value
  LaunchedEffect(Unit) {
    viewModel.onCenterCamera = { event, forceZoom ->
      val animationOptions = MapAnimationOptions.Builder().duration(500L).build()
      val currentZoom = mapViewportState.cameraState?.zoom ?: MapConstants.DEFAULT_ZOOM.toDouble()

      // When forceZoom is true (from search), always zoom to 17 to ensure pins are visible
      // Otherwise, use the existing logic
      val targetZoom =
          if (forceZoom) {
            17.0
          } else {
            if (currentZoom < 14.0) 15.0 else currentZoom
          }

      val offsetPixels = (screenHeightDpValue * 0.25) / 2

      mapViewportState.easeTo(
          cameraOptions {
            center(Point.fromLngLat(event.location.longitude, event.location.latitude))
            zoom(targetZoom)
            padding(com.mapbox.maps.EdgeInsets(0.0, 0.0, offsetPixels * 2, 0.0))
          },
          animationOptions = animationOptions)
    }
  }

  ObserveSheetStateForZoomUpdate(viewModel, mapViewportState)
  ObserveZoomForSheetCollapse(viewModel, mapViewportState)

  val sheetMetrics =
      rememberSheetInteractionMetrics(
          screenHeightDp = screenHeightDp, currentSheetHeight = viewModel.currentSheetHeight)

  val isDarkTheme = isSystemInDarkTheme()
  val lightPreset = if (isDarkTheme) LightPresetValue.NIGHT else LightPresetValue.DAY

  // Adjust POI labels alongside our custom annotations
  val standardStyleState = rememberStandardStyleState {
    configurationsState.apply {
      this.lightPreset = lightPreset
      showPointOfInterestLabels = BooleanValue(true) // turn off if needed
    }
  }

  // Heatmap source mirrors the ViewModel events list
  val heatmapSource =
      rememberGeoJsonSourceState(key = "events-heatmap-source") {
        data = GeoJSONData(eventsToGeoJson(viewModel.events))
      }

  LaunchedEffect(viewModel.events) {
    heatmapSource.data = GeoJSONData(eventsToGeoJson(viewModel.events))
  }

  // Fusion d'une seule racine UI box qui contient la carte, overlays et la feuille inférieure
  Box(modifier = Modifier.fillMaxSize().testTag(UiTestTags.MAP_SCREEN)) {
    // Carte Mapbox: combine les comportements précédemment séparés
    if (renderMap) {
      MapboxLayer(
          viewModel = viewModel,
          mapViewportState = mapViewportState,
          sheetMetrics = sheetMetrics,
          standardStyleState = standardStyleState,
          heatmapSource = heatmapSource,
          isDarkTheme = isDarkTheme,
          onEventClick = { event ->
            // Conserver tous les effets attendus lors d'un clic sur un pin
            viewModel.onEventPinClicked(event)
            viewModel.setBottomSheetState(BottomSheetState.MEDIUM)
            // Propager vers le handler externe
            onEventClick(event)
          })
    }

    // Overlays et contrôles au-dessus de la carte
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

    // Bloque les interactions de carte quand la feuille est pleine
    ConditionalMapBlocker(bottomSheetState = viewModel.bottomSheetState)

    // BottomSheet unique : montre soit le détail d'événement soit le contenu normal
    BottomSheet(
        config = sheetConfig,
        currentState = viewModel.bottomSheetState,
        onStateChange = { newState -> viewModel.setBottomSheetState(newState) },
        calculateTargetState = viewModel::calculateTargetState,
        stateToHeight = viewModel::getHeightForState,
        onHeightChange = { height -> viewModel.currentSheetHeight = height },
        modifier = Modifier.align(Alignment.BottomCenter).testTag("bottomSheet")) {
          AnimatedContent(
              targetState = viewModel.selectedEvent,
              transitionSpec = {
                val direction = if (targetState != null) 1 else -1
                (fadeIn(animationSpec = androidx.compose.animation.core.tween(260)) +
                        slideInVertically(
                            animationSpec = androidx.compose.animation.core.tween(260),
                            initialOffsetY = { direction * it / 6 }))
                    .togetherWith(
                        fadeOut(animationSpec = androidx.compose.animation.core.tween(200)) +
                            slideOutVertically(
                                animationSpec = androidx.compose.animation.core.tween(200),
                                targetOffsetY = { -direction * it / 6 }))
              },
              label = "eventSheetTransition") { selectedEvent ->
                if (selectedEvent != null) {
                  EventDetailSheet(
                      event = selectedEvent,
                      sheetState = viewModel.bottomSheetState,
                      isParticipating = viewModel.isUserParticipating(selectedEvent),
                      organizerName = viewModel.organizerName,
                      onJoinEvent = { viewModel.joinEvent() },
                      onUnregisterEvent = { viewModel.unregisterFromEvent() },
                      onSaveForLater = { viewModel.saveEventForLater() },
                      onClose = { viewModel.closeEventDetail() },
                      onShare = { viewModel.showShareDialog() })
                } else {
                  BottomSheetContent(
                      state = viewModel.bottomSheetState,
                      fullEntryKey = viewModel.fullEntryKey,
                      searchBarState =
                          SearchBarState(
                              query = viewModel.searchQuery,
                              shouldRequestFocus = viewModel.shouldFocusSearch,
                              onQueryChange = viewModel::onSearchQueryChange,
                              onTap = viewModel::onSearchTap,
                              onFocusHandled = viewModel::onSearchFocusHandled,
                              onClear = viewModel::onClearSearch),
                      searchResults = viewModel.searchResults,
                      isSearchMode = viewModel.isSearchMode,
                      currentScreen = viewModel.currentBottomSheetScreen,
                      availableEvents = viewModel.availableEvents,
                      topTags = viewModel.topTags,
                      selectedTags = viewModel.selectedTags,
                      onTagClick = viewModel::toggleTagSelection,
                      onEventClick = { event ->
                        // Handle event click from search - focus pin, show details, remember
                        // search mode
                        viewModel.onEventClickedFromSearch(event)
                        onEventClick(event)
                      },
                      onCreateMemoryClick = viewModel::showMemoryForm,
                      onCreateEventClick = viewModel::showAddEventForm,
                      onMemorySave = viewModel::onMemorySave,
                      onMemoryCancel = viewModel::onMemoryCancel,
                      onCreateEventDone = viewModel::onAddEventCancel,
                      onTabChange = viewModel::setBottomSheetTab,
                      joinedEvents = viewModel.joinedEvents,
                      selectedTab = viewModel.selectedBottomSheetTab,
                      onJoinedEventClick = viewModel::onJoinedEventClicked,
                      onProfileClick = onNavigateToProfile)
                }
              }
        }

    // Share dialog
    if (viewModel.showShareDialog && viewModel.selectedEvent != null) {
      ShareEventDialog(
          event = viewModel.selectedEvent!!, onDismiss = { viewModel.dismissShareDialog() })
    }

    // Indicateur de sauvegarde de mémoire
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

/**
 * Composable that renders the Mapbox map with compass, scale bar, and event annotations.
 *
 * Handles map styling (standard vs satellite), pointer input for sheet interactions, and delegates
 * the rendering of map layers to [MapLayers].
 */
@Composable
private fun MapboxLayer(
    viewModel: MapScreenViewModel,
    mapViewportState: MapViewportState,
    sheetMetrics: SheetInteractionMetrics,
    standardStyleState: StandardStyleState,
    heatmapSource: GeoJsonSourceState,
    isDarkTheme: Boolean,
    onEventClick: (Event) -> Unit
) {
  MapboxMap(
      Modifier.fillMaxSize()
          .then(
              Modifier.mapPointerInput(
                  bottomSheetState = viewModel.bottomSheetState,
                  sheetMetrics = sheetMetrics,
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
        Box(modifier = Modifier.fillMaxSize()) {
          Compass(
              modifier =
                  Modifier.align(Alignment.BottomEnd)
                      .padding(bottom = MapConstants.COLLAPSED_HEIGHT + 96.dp, end = 16.dp))
        }
      },
      scaleBar = {
        Box(modifier = Modifier.fillMaxSize()) {
          AnimatedVisibility(
              visible = viewModel.isZooming,
              enter = fadeIn(),
              exit = fadeOut(),
              modifier = Modifier.align(Alignment.TopCenter).padding(top = 60.dp)) {
                ScaleBar()
              }
        }
      }) {
        MapLayers(
            viewModel = viewModel,
            mapViewportState = mapViewportState,
            heatmapSource = heatmapSource,
            isDarkTheme = isDarkTheme,
            onEventClick = onEventClick)
      }
}

/**
 * Renders map content including location markers, clusters, and optional heatmap.
 *
 * Switches between heatmap mode (with simple annotations) and clustering mode based on
 * [MapScreenViewModel.showHeatmap].
 */
@Composable
private fun MapLayers(
    viewModel: MapScreenViewModel,
    mapViewportState: MapViewportState,
    heatmapSource: GeoJsonSourceState,
    isDarkTheme: Boolean,
    onEventClick: (Event) -> Unit
) {
  val context = LocalContext.current
  val markerBitmap = remember(context) { context.drawableToBitmap(R.drawable.ic_map_marker) }

  val annotationStyle =
      remember(isDarkTheme, markerBitmap) { createAnnotationStyle(isDarkTheme, markerBitmap) }

  val annotations =
      remember(viewModel.events, annotationStyle, viewModel.selectedEvent) {
        createEventAnnotations(viewModel.events, annotationStyle, viewModel.selectedEvent?.uid)
      }

  val clusterConfig = remember { createClusterConfig() }

  // Render heatmap layer when enabled
  if (viewModel.showHeatmap) {
    CreateHeatmapLayer(heatmapSource)
  }

  // Disable clustering when a pin is selected to prevent it from being absorbed
  val shouldCluster = !viewModel.showHeatmap && viewModel.selectedEvent == null

  // Render annotations (with or without clustering)
  if (viewModel.showHeatmap || !shouldCluster) {
    // No clustering: used for heatmap mode or when a pin is selected
    PointAnnotationGroup(annotations = annotations) {
      markerBitmap?.let { iconImage = IconImage(it) }
      iconAllowOverlap = false // Enable collision detection
      textAllowOverlap = false // Enable collision detection for text
      iconIgnorePlacement = false // Respect other symbols
      textIgnorePlacement = false // Respect other symbols
      interactionsState.onClicked { annotation ->
        findEventForAnnotation(annotation, viewModel.events)?.let { event ->
          onEventClick(event)
          true
        } ?: false
      }
    }
  } else {
    // With clustering: default behavior when no pin is selected
    PointAnnotationGroup(annotations = annotations, annotationConfig = clusterConfig) {
      markerBitmap?.let { iconImage = IconImage(it) }
      iconAllowOverlap = false // Enable collision detection
      textAllowOverlap = false // Enable collision detection for text
      iconIgnorePlacement = false // Respect other symbols
      textIgnorePlacement = false // Respect other symbols
      interactionsState
          .onClicked { annotation ->
            findEventForAnnotation(annotation, viewModel.events)?.let { event ->
              onEventClick(event)
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

/**
 * Data class holding metrics for sheet interaction calculations.
 *
 * @property densityDpi Screen density in DPI for touch proximity calculations
 * @property sheetTopPx Current top position of the bottom sheet in pixels
 */
private data class SheetInteractionMetrics(val densityDpi: Int, val sheetTopPx: Float)

/**
 * Calculates and remembers sheet interaction metrics based on screen dimensions.
 *
 * Converts dp values to pixels and computes the sheet's top position for touch detection.
 */
@Composable
private fun rememberSheetInteractionMetrics(
    screenHeightDp: Dp,
    currentSheetHeight: Dp
): SheetInteractionMetrics {
  val density = LocalDensity.current
  val densityFactor = density.density
  val densityDpi = remember(densityFactor) { (densityFactor * 160).toInt() }
  val screenHeightPx =
      remember(screenHeightDp, densityFactor) { screenHeightDp.value * densityFactor }
  val sheetTopPx =
      remember(screenHeightPx, currentSheetHeight, densityFactor) {
        screenHeightPx - (currentSheetHeight.value * densityFactor)
      }
  return remember(densityDpi, sheetTopPx) { SheetInteractionMetrics(densityDpi, sheetTopPx) }
}

/** Pointer modifier that collapses the sheet when touches originate near its top edge. */
private fun Modifier.mapPointerInput(
    bottomSheetState: BottomSheetState,
    sheetMetrics: SheetInteractionMetrics,
    onCollapseSheet: () -> Unit,
    checkTouchProximity: (Float, Float, Int) -> Boolean
) =
    this.pointerInput(bottomSheetState, sheetMetrics) {
      awaitPointerEventScope {
        while (true) {
          val event = awaitPointerEvent()
          if (event.type == PointerEventType.Move) {
            event.changes.firstOrNull()?.let { change ->
              val touchY = change.position.y
              if (checkTouchProximity(touchY, sheetMetrics.sheetTopPx, sheetMetrics.densityDpi)) {
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

/**
 * Conditionally renders a map interaction blocker when the sheet is fully expanded.
 *
 * Prevents map gestures from interfering with sheet content interaction.
 */
@Composable
private fun ConditionalMapBlocker(bottomSheetState: BottomSheetState) {
  if (bottomSheetState == BottomSheetState.FULL) {
    MapInteractionBlocker()
  }
}

/**
 * Converts a drawable resource to a Bitmap for use in map annotations.
 *
 * @param drawableResId Resource ID of the drawable to convert
 * @return Bitmap representation of the drawable, or null if conversion fails
 */
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

/**
 * Holds styling information for map annotations.
 *
 * @property textColorInt Text color for annotation labels (ARGB integer)
 * @property haloColorInt Halo color for text outline (ARGB integer)
 * @property markerBitmap Optional bitmap for the marker icon
 */
@VisibleForTesting
internal data class AnnotationStyle(
    val textColorInt: Int,
    val haloColorInt: Int,
    val markerBitmap: Bitmap?
)

/**
 * Creates annotation styling based on current theme.
 *
 * @param isDarkTheme Whether dark theme is active
 * @param markerBitmap Optional bitmap for marker icon
 * @return AnnotationStyle with theme-appropriate colors
 */
@VisibleForTesting
internal fun createAnnotationStyle(isDarkTheme: Boolean, markerBitmap: Bitmap?): AnnotationStyle {
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

/**
 * Converts a list of events to Mapbox point annotation options.
 *
 * Each annotation includes position, icon, label, and custom styling. The index is stored as data
 * for later retrieval. Selected event pins are enlarged.
 *
 * @param events List of events to convert
 * @param style Styling to apply to annotations
 * @param selectedEventId UID of the currently selected event (if any)
 * @return List of configured PointAnnotationOptions
 */
@VisibleForTesting
internal data class AnnotationVisualParameters(
    val iconSize: Double,
    val textSize: Double,
    val textOffset: List<Double>,
    val textHaloWidth: Double,
    val sortKey: Double
)

@VisibleForTesting
internal fun computeAnnotationVisualParameters(isSelected: Boolean): AnnotationVisualParameters {
  return if (isSelected) {
    AnnotationVisualParameters(
        iconSize = 1.5,
        textSize = 15.0,
        textOffset = listOf(0.0, 0.5),
        textHaloWidth = 2.0,
        sortKey = 0.0)
  } else {
    AnnotationVisualParameters(
        iconSize = 1.0,
        textSize = 12.0,
        textOffset = listOf(0.0, 0.2),
        textHaloWidth = 1.5,
        sortKey = 100.0)
  }
}

@VisibleForTesting
internal fun createEventAnnotations(
    events: List<Event>,
    style: AnnotationStyle,
    selectedEventId: String? = null
): List<PointAnnotationOptions> {
  return events.mapIndexed { index, event ->
    val isSelected = event.uid == selectedEventId
    val visual = computeAnnotationVisualParameters(isSelected)

    PointAnnotationOptions()
        .withPoint(Point.fromLngLat(event.location.longitude, event.location.latitude))
        .apply { style.markerBitmap?.let { withIconImage(it) } }
        .withIconSize(visual.iconSize)
        .withIconAnchor(IconAnchor.BOTTOM)
        .withTextAnchor(TextAnchor.TOP)
        .withTextOffset(visual.textOffset)
        .withTextSize(visual.textSize)
        .withTextColor(style.textColorInt)
        .withTextHaloColor(style.haloColorInt)
        .withTextHaloWidth(visual.textHaloWidth)
        .withTextField(event.title)
        .withData(JsonPrimitive(index))
        .withSymbolSortKey(visual.sortKey) // Ensures selected pin is prioritized for visibility
  }
}

/**
 * Creates clustering configuration for location annotations.
 *
 * Uses blue gradient colors for cluster sizes and enables touch interaction.
 *
 * @return AnnotationConfig with clustering enabled
 */
@VisibleForTesting
internal fun createClusterConfig(): AnnotationConfig {
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

/**
 * Finds the Event associated with a clicked annotation.
 *
 * First tries to match by stored index data, then falls back to coordinate comparison.
 *
 * @param annotation The clicked point annotation
 * @param events List of all events
 * @return Matching Event or null if not found
 */
@VisibleForTesting
internal fun findEventForAnnotation(
    annotation: com.mapbox.maps.plugin.annotation.generated.PointAnnotation,
    events: List<Event>
): Event? {
  val index = annotation.getData()?.takeIf { it.isJsonPrimitive }?.asInt
  return index?.let { events.getOrNull(it) }
      ?: events.firstOrNull { event ->
        val point = annotation.point
        event.location.latitude == point.latitude() && event.location.longitude == point.longitude()
      }
}

/**
 * Renders a heatmap layer showing location density.
 *
 * Uses interpolated colors, radius, and weight based on zoom level and location data.
 *
 * @param heatmapSource GeoJSON source containing location data
 */
@Composable
private fun CreateHeatmapLayer(heatmapSource: GeoJsonSourceState) {
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
