package com.swent.mapin.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxDelicateApi
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.IconImage
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotationGroup
import com.mapbox.maps.extension.compose.style.BooleanValue
import com.mapbox.maps.extension.compose.style.sources.GeoJSONData
import com.mapbox.maps.extension.compose.style.sources.generated.GeoJsonSourceState
import com.mapbox.maps.extension.compose.style.sources.generated.rememberGeoJsonSourceState
import com.mapbox.maps.extension.compose.style.standard.LightPresetValue
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardSatelliteStyle
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.maps.extension.compose.style.standard.StandardStyleState
import com.mapbox.maps.extension.compose.style.standard.rememberStandardStyleState
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.swent.mapin.R
import com.swent.mapin.model.LocationViewModel
import com.swent.mapin.model.PreferencesRepositoryProvider
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepositoryProvider
import com.swent.mapin.testing.UiTestTags
import com.swent.mapin.ui.chat.ChatScreenTestTags
import com.swent.mapin.ui.components.BottomSheet
import com.swent.mapin.ui.components.BottomSheetConfig
import com.swent.mapin.ui.event.EventDetailSheet
import com.swent.mapin.ui.event.EventViewModel
import com.swent.mapin.ui.event.ShareEventDialog
import com.swent.mapin.ui.map.bottomsheet.SearchBarState
import com.swent.mapin.ui.map.components.ConditionalMapBlocker
import com.swent.mapin.ui.map.components.CreateHeatmapLayer
import com.swent.mapin.ui.map.components.ObserveSheetStateForZoomUpdate
import com.swent.mapin.ui.map.components.ObserveZoomForSheetCollapse
import com.swent.mapin.ui.map.components.ScrimOverlay
import com.swent.mapin.ui.map.components.SheetInteractionMetrics
import com.swent.mapin.ui.map.components.createAnnotationStyle
import com.swent.mapin.ui.map.components.createClusterConfig
import com.swent.mapin.ui.map.components.createEventAnnotations
import com.swent.mapin.ui.map.components.drawableToBitmap
import com.swent.mapin.ui.map.components.findEventForAnnotation
import com.swent.mapin.ui.map.components.mapPointerInput
import com.swent.mapin.ui.map.components.rememberSheetInteractionMetrics
import com.swent.mapin.ui.map.directions.DirectionOverlay
import com.swent.mapin.ui.map.directions.DirectionState
import com.swent.mapin.ui.profile.ProfileViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

// Maximum zoom level when fitting camera to search results
private const val MAX_SEARCH_RESULTS_ZOOM = 17.0

/** Map screen that layers Mapbox content with a bottom sheet driven by MapScreenViewModel. */
@OptIn(MapboxDelicateApi::class)
@Composable
fun MapScreen(
    onEventClick: (Event) -> Unit = {},
    renderMap: Boolean = true,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToFriends: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
  val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
  // Bottom sheet heights scale with the current device size
  val sheetConfig =
      BottomSheetConfig(
          collapsedHeight = MapConstants.COLLAPSED_HEIGHT,
          mediumHeight = MapConstants.MEDIUM_HEIGHT,
          fullHeight = screenHeightDp * MapConstants.FULL_HEIGHT_PERCENTAGE)

  val viewModel = rememberMapScreenViewModel(sheetConfig)
  val eventViewModel =
      EventViewModel(EventRepositoryProvider.getRepository(), viewModel.eventStateController)
  val snackbarHostState = remember { SnackbarHostState() }
  val density = LocalDensity.current
  val mediumSheetBottomPaddingPx = with(density) { sheetConfig.mediumHeight.toPx() }
  val edgePaddingPx = with(density) { 24.dp.toPx() }
  val extraBottomMarginPx = with(density) { 32.dp.toPx() }
  val bottomPaddingPx = mediumSheetBottomPaddingPx + extraBottomMarginPx
  val coroutineScope = rememberCoroutineScope()

  // Location permission launcher
  val locationPermissionLauncher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationGranted =
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            if (fineLocationGranted && coarseLocationGranted) {
              viewModel.checkLocationPermission()
              viewModel.startLocationUpdates()
              viewModel.getLastKnownLocation(centerCamera = true)
            } else {
              coroutineScope.launch { snackbarHostState.showSnackbar("Location permission denied") }
            }
          }

  // Reload user profile when MapScreen is composed (e.g., returning from ProfileScreen)
  LaunchedEffect(Unit) { viewModel.loadUserProfile() }

  // Initialize TileStore for offline map caching
  LaunchedEffect(Unit) { viewModel.initializeTileStore() }

  LaunchedEffect(viewModel.errorMessage) {
    viewModel.errorMessage?.let { message ->
      snackbarHostState.showSnackbar(message)
      viewModel.clearError()
    }
  }

  // Setup location management
  LaunchedEffect(Unit) {
    viewModel.checkLocationPermission()
    if (viewModel.hasLocationPermission) {
      viewModel.startLocationUpdates()
      viewModel.getLastKnownLocation(centerCamera = true)
    }

    viewModel.onRequestLocationPermission = {
      locationPermissionLauncher.launch(
          arrayOf(
              Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
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
    viewModel.setCenterCameraCallback { event, forceZoom ->
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
            padding(EdgeInsets(0.0, 0.0, offsetPixels * 2, 0.0))
          },
          animationOptions = animationOptions)
    }

    // Setup location centering callback
    viewModel.onCenterOnUserLocation = {
      viewModel.currentLocation?.let { location ->
        val animationOptions = MapAnimationOptions.Builder().duration(500L).build()
        val collapsedPx = with(density) { sheetConfig.collapsedHeight.toPx() }
        val mediumPx = with(density) { sheetConfig.mediumHeight.toPx() }
        val sheetPx = with(density) { viewModel.currentSheetHeight.toPx() }
        val minPaddingPx = with(density) { MapConstants.LOCATION_CENTER_MIN_PADDING_DP.dp.toPx() }
        val mediumExtraPx = with(density) { MapConstants.LOCATION_CENTER_MEDIUM_EXTRA_DP.dp.toPx() }
        val locationBottomPaddingPx =
            calculateLocationPaddingPx(
                sheetHeightPx = sheetPx,
                collapsedHeightPx = collapsedPx,
                mediumHeightPx = mediumPx,
                minPaddingPx = minPaddingPx,
                mediumWeight = MapConstants.LOCATION_CENTER_MEDIUM_WEIGHT,
                mediumExtraPx = mediumExtraPx)
        viewModel.runProgrammaticCamera {
          mapViewportState.easeTo(
              cameraOptions {
                center(Point.fromLngLat(location.longitude, location.latitude))
                zoom(16.0)
                bearing(if (location.hasBearing()) location.bearing.toDouble() else 0.0)
                padding(EdgeInsets(0.0, 0.0, locationBottomPaddingPx.toDouble(), 0.0))
              },
              animationOptions = animationOptions)
        }
      }
    }
  }

  LaunchedEffect(mapViewportState, bottomPaddingPx, edgePaddingPx) {
    viewModel.setFitCameraCallback label@{ events ->
      if (events.isEmpty()) return@label

      coroutineScope.launch {
        val points =
            events.map { event ->
              Point.fromLngLat(event.location.longitude, event.location.latitude)
            }

        val padding =
            EdgeInsets(
                edgePaddingPx.toDouble(),
                edgePaddingPx.toDouble(),
                bottomPaddingPx.toDouble(),
                edgePaddingPx.toDouble())

        val camera =
            mapViewportState.cameraForCoordinates(
                coordinates = points,
                camera = cameraOptions {},
                coordinatesPadding = padding,
                maxZoom = MAX_SEARCH_RESULTS_ZOOM,
                offset = null)

        camera.let {
          mapViewportState.easeTo(it, MapAnimationOptions.Builder().duration(600L).build())
        }
      }
    }
  }

  ObserveSheetStateForZoomUpdate(viewModel, mapViewportState)
  ObserveZoomForSheetCollapse(viewModel, mapViewportState)

  val sheetMetrics =
      rememberSheetInteractionMetrics(
          screenHeightDp = screenHeightDp, currentSheetHeight = viewModel.currentSheetHeight)

  // Get map preferences and theme from PreferencesRepository
  val context = LocalContext.current
  val preferencesRepository = remember { PreferencesRepositoryProvider.getInstance(context) }
  val themeModeString by preferencesRepository.themeModeFlow.collectAsState(initial = "system")
  val showPOIs by preferencesRepository.showPOIsFlow.collectAsState(initial = true)
  val showRoadNumbers by preferencesRepository.showRoadNumbersFlow.collectAsState(initial = true)
  val showStreetNames by preferencesRepository.showStreetNamesFlow.collectAsState(initial = true)
  val enable3DView by preferencesRepository.enable3DViewFlow.collectAsState(initial = true)

  // Determine if dark theme based on app setting
  val isSystemInDark = isSystemInDarkTheme()
  val isDarkTheme =
      when (themeModeString.lowercase()) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDark // "system" or default
      }
  val lightPreset = if (isDarkTheme) LightPresetValue.NIGHT else LightPresetValue.DAY
  val view = LocalView.current

  // Force white status bar icons on light-mode satellite maps for readability
  val defaultLightStatusBars = !isDarkTheme
  val desiredLightStatusBars =
      if (defaultLightStatusBars && viewModel.useSatelliteStyle) {
        false
      } else {
        defaultLightStatusBars
      }

  DisposableEffect(view, desiredLightStatusBars, defaultLightStatusBars) {
    val window = (view.context as? Activity)?.window
    val insetsController = window?.let { WindowInsetsControllerCompat(it, view) }
    insetsController?.isAppearanceLightStatusBars = desiredLightStatusBars
    onDispose { insetsController?.isAppearanceLightStatusBars = defaultLightStatusBars }
  }

  // Initialize standard style state with light preset only
  val standardStyleState = rememberStandardStyleState {
    configurationsState.apply { this.lightPreset = lightPreset }
  }

  // Update style configuration reactively when preferences change (including theme)
  LaunchedEffect(themeModeString, showPOIs, showRoadNumbers, showStreetNames, enable3DView) {
    standardStyleState.configurationsState.apply {
      this.lightPreset = lightPreset
      showPointOfInterestLabels = BooleanValue(showPOIs)
      showRoadLabels = BooleanValue(showRoadNumbers)
      showTransitLabels = BooleanValue(showStreetNames)
      show3dObjects = BooleanValue(enable3DView)
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

  val anchoredSheetHeight =
      if (viewModel.currentSheetHeight < sheetConfig.mediumHeight) {
        viewModel.currentSheetHeight
      } else {
        sheetConfig.mediumHeight
      }
  val controlBottomPadding = anchoredSheetHeight + 24.dp
  val chatBottomPadding = anchoredSheetHeight + 16.dp

  // Fusion d'une seule racine UI box qui contient la carte, overlays et la feuille inférieure
  Box(modifier = Modifier.fillMaxSize().testTag(UiTestTags.MAP_SCREEN)) {
    // Carte Mapbox: combine les comportements précédemment séparés
    if (renderMap) {
      MapboxLayer(
          viewModel = viewModel,
          mapViewportState = mapViewportState,
          sheetMetrics = sheetMetrics,
          controlBottomPadding = controlBottomPadding,
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
    Box(
        modifier =
            Modifier.align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = chatBottomPadding)) {
          FilledIconButton(
              onClick = { onNavigateToChat() },
              shape = CircleShape,
              modifier = Modifier.size(48.dp).testTag(ChatScreenTestTags.CHAT_NAVIGATE_BUTTON),
              colors =
                  IconButtonDefaults.filledIconButtonColors(
                      containerColor = MaterialTheme.colorScheme.primary,
                      contentColor = MaterialTheme.colorScheme.onPrimary)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Go to Chats")
              }
        }

    ScrimOverlay(
        currentHeightDp = viewModel.currentSheetHeight,
        mediumHeightDp = sheetConfig.mediumHeight,
        fullHeightDp = sheetConfig.fullHeight)

    if (!renderMap) {
      Column(
          modifier =
              Modifier.align(Alignment.BottomEnd)
                  .padding(bottom = controlBottomPadding, end = 16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          horizontalAlignment = Alignment.End) {
            Box(modifier = Modifier.size(48.dp)) {
              LocationButton(
                  onClick = { viewModel.onLocationButtonClick() },
                  modifier = Modifier.fillMaxSize())
            }

            MapStyleSelector(
                selectedStyle = viewModel.mapStyle,
                onStyleSelected = { style -> viewModel.setMapStyle(style) },
                modifier = Modifier.size(48.dp))
          }
    }

    // Bloque les interactions de carte quand la feuille est pleine
    ConditionalMapBlocker(bottomSheetState = viewModel.bottomSheetState)

    // BottomSheet unique : montre soit le détail d'événement soit le contenu normal
    var modalPrevState by remember { mutableStateOf<BottomSheetState?>(null) }

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
                (fadeIn(animationSpec = tween(260)) +
                        slideInVertically(
                            animationSpec = tween(260), initialOffsetY = { direction * it / 6 }))
                    .togetherWith(
                        fadeOut(animationSpec = tween(200)) +
                            slideOutVertically(
                                animationSpec = tween(200),
                                targetOffsetY = { -direction * it / 6 }))
              },
              label = "eventSheetTransition") { selectedEvent ->
                if (selectedEvent != null) {
                  EventDetailSheet(
                      event = selectedEvent,
                      sheetState = viewModel.bottomSheetState,
                      isParticipating = viewModel.joinedEvents.any { it.uid == selectedEvent.uid },
                      // Use viewModel.savedEvents (Compose-observed state) so recomposition occurs
                      isSaved = viewModel.savedEvents.any { it.uid == selectedEvent.uid },
                      organizerName = viewModel.organizerName,
                      onJoinEvent = { viewModel.joinEvent() },
                      onUnregisterEvent = { viewModel.unregisterFromEvent() },
                      onSaveForLater = { viewModel.saveEventForLater() },
                      onUnsaveForLater = { viewModel.unsaveEventForLater() },
                      onClose = { viewModel.closeEventDetail() },
                      onShare = { viewModel.showShareDialog() },
                      onGetDirections = { viewModel.toggleDirections(selectedEvent) },
                      showDirections =
                          viewModel.directionViewModel.directionState is DirectionState.Displayed)
                } else {
                  BottomSheetContent(
                      onModalShown = { shown ->
                        if (shown) {
                          if (modalPrevState == null) modalPrevState = viewModel.bottomSheetState
                          viewModel.setBottomSheetState(BottomSheetState.COLLAPSED)
                        } else {
                          modalPrevState?.let { prev ->
                            viewModel.setBottomSheetState(prev)
                            modalPrevState = null
                          }
                        }
                      },
                      state = viewModel.bottomSheetState,
                      fullEntryKey = viewModel.fullEntryKey,
                      searchBarState =
                          SearchBarState(
                              query = viewModel.searchQuery,
                              shouldRequestFocus = viewModel.shouldFocusSearch,
                              onQueryChange = viewModel::onSearchQueryChange,
                              onTap = viewModel::onSearchTap,
                              onFocusHandled = viewModel::onSearchFocusHandled,
                              onClear = viewModel::onClearSearch,
                              onSubmit = viewModel::onSearchSubmit),
                      searchResults = viewModel.searchResults,
                      isSearchMode = viewModel.isSearchMode,
                      recentItems = viewModel.recentItems,
                      onRecentSearchClick = viewModel::applyRecentSearch,
                      onRecentEventClick = viewModel::onRecentEventClicked,
                      onClearRecentSearches = viewModel::clearRecentSearches,
                      // I will fully remove topCategories in the next PR but started
                      // with just MapScreenVm as i worked a lot on it
                      topCategories = emptyList(),
                      onCategoryClick = viewModel::applyRecentSearch,
                      currentScreen = viewModel.currentBottomSheetScreen,
                      availableEvents = viewModel.availableEvents,
                      initialMemoryEvent = viewModel.memoryFormInitialEvent,
                      onEventClick = { event ->
                        // Handle event click from search - focus pin, show details, remember
                        // search mode
                        viewModel.onEventClickedFromSearch(event)
                        onEventClick(event)
                      },
                      onCreateMemoryClick = viewModel::showMemoryFormForEvent,
                      onCreateEventClick = viewModel::showAddEventForm,
                      onNavigateToFriends = onNavigateToFriends,
                      onProfileClick = onNavigateToProfile,
                      onMemorySave = viewModel::onMemorySave,
                      onMemoryCancel = viewModel::onMemoryCancel,
                      onCreateEventDone = viewModel::onAddEventCancel,
                      onTabChange = viewModel::setBottomSheetTab,
                      joinedEvents = viewModel.joinedEvents,
                      attendedEvents = viewModel.attendedEvents,
                      savedEvents = viewModel.savedEvents,
                      ownedEvents = viewModel.ownedEvents,
                      ownedLoading = viewModel.ownedEventsLoading,
                      ownedError = viewModel.ownedEventsError,
                      onRetryOwnedEvents = viewModel::loadOwnedEvents,
                      selectedTab = viewModel.selectedBottomSheetTab,
                      onTabEventClick = viewModel::onTabEventClicked,
                      avatarUrl = viewModel.avatarUrl,
                      filterViewModel = viewModel.filterViewModel,
                      onSettingsClick = onNavigateToSettings,
                      locationViewModel = remember { LocationViewModel() },
                      profileViewModel = remember { ProfileViewModel() },
                      eventViewModel = eventViewModel)
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
    controlBottomPadding: Dp,
    standardStyleState: StandardStyleState,
    heatmapSource: GeoJsonSourceState,
    isDarkTheme: Boolean,
    onEventClick: (Event) -> Unit
) {
  LaunchedEffect(mapViewportState) {
    snapshotFlow { mapViewportState.cameraState }
        .filterNotNull()
        .collect { cameraState ->
          val center = cameraState.center
          viewModel.updateCenteredState(center.latitude(), center.longitude())
        }
  }

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
          Column(
              modifier =
                  Modifier.align(Alignment.BottomEnd)
                      .padding(bottom = controlBottomPadding, end = 16.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
              horizontalAlignment = Alignment.End) {
                Box(modifier = Modifier.size(48.dp)) { Compass() }

                Box(modifier = Modifier.size(48.dp)) {
                  LocationButton(
                      onClick = { viewModel.onLocationButtonClick() },
                      modifier = Modifier.fillMaxSize())
                }

                MapStyleSelector(
                    selectedStyle = viewModel.mapStyle,
                    onStyleSelected = { style -> viewModel.setMapStyle(style) },
                    modifier = Modifier.size(48.dp))
              }
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
 *
 * Also configures the user location puck to display the device's position and bearing on the map.
 */
@SuppressLint("VisibleForTests")
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

  if (viewModel.showHeatmap) {
    CreateHeatmapLayer(heatmapSource)
  }

  // Render direction overlay if directions are displayed
  val directionState = viewModel.directionViewModel.directionState
  if (directionState is DirectionState.Displayed) {
    DirectionOverlay(routePoints = directionState.routePoints)
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

  MapEffect(viewModel.hasLocationPermission) { mapView ->
    mapView.location.updateSettings {
      if (viewModel.hasLocationPermission) {
        locationPuck = createDefault2DPuck(withBearing = true)
        enabled = true
        pulsingEnabled = true
        puckBearingEnabled = true
        puckBearing = PuckBearing.HEADING
      } else {
        enabled = false
      }
    }
  }
}

internal fun calculateLocationPaddingPx(
    sheetHeightPx: Float,
    collapsedHeightPx: Float,
    mediumHeightPx: Float,
    minPaddingPx: Float,
    mediumWeight: Float,
    mediumExtraPx: Float
): Float {
  val clampedSheet = sheetHeightPx.coerceAtLeast(0f)
  val mediumThreshold = mediumHeightPx.coerceAtLeast(collapsedHeightPx)
  val mediumPaddingPx = clampedSheet * mediumWeight + mediumExtraPx
  return if (clampedSheet >= mediumThreshold) mediumPaddingPx else minPaddingPx
}
