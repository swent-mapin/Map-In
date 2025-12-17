package com.swent.mapin.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.firestore.GeoPoint
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapboxDelicateApi
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
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
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.swent.mapin.HttpClientProvider
import com.swent.mapin.R
import com.swent.mapin.model.event.Event
import com.swent.mapin.model.event.EventRepositoryProvider
import com.swent.mapin.model.location.Location
import com.swent.mapin.model.location.LocationViewModel
import com.swent.mapin.model.location.LocationViewModelFactory
import com.swent.mapin.model.network.ConnectivityServiceProvider
import com.swent.mapin.model.preferences.PreferencesRepositoryProvider
import com.swent.mapin.testing.UiTestTags
import com.swent.mapin.ui.chat.ChatScreenTestTags
import com.swent.mapin.ui.components.BottomSheet
import com.swent.mapin.ui.components.BottomSheetConfig
import com.swent.mapin.ui.components.SheetContent
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
import com.swent.mapin.ui.map.components.createEventBitmaps
import com.swent.mapin.ui.map.components.drawableToBitmap
import com.swent.mapin.ui.map.components.findEventForAnnotation
import com.swent.mapin.ui.map.components.mapPointerInput
import com.swent.mapin.ui.map.components.rememberSheetInteractionMetrics
import com.swent.mapin.ui.map.directions.DirectionOverlay
import com.swent.mapin.ui.map.directions.DirectionState
import com.swent.mapin.ui.map.directions.RouteInfoCard
import com.swent.mapin.ui.map.offline.EventBasedOfflineRegionManager
import com.swent.mapin.ui.memory.MemoriesViewModel
import com.swent.mapin.ui.memory.MemoryDetailSheet
import com.swent.mapin.ui.profile.ProfileViewModel
import com.swent.mapin.util.EventUtils
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

// Maximum zoom level when fitting camera to search results
private const val MAX_SEARCH_RESULTS_ZOOM = 17.0

/** Map screen that layers Mapbox content with a bottom sheet driven by MapScreenViewModel. */
@OptIn(MapboxDelicateApi::class, FlowPreview::class)
@Composable
fun MapScreen(
    onEventClick: (Event) -> Unit = {},
    renderMap: Boolean = true,
    autoRequestPermissions: Boolean = true,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToFriends: () -> Unit = {},
    onNavigateToMemories: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAiAssistant: () -> Unit = {},
    deepLinkEventId: String? = null,
    onDeepLinkConsumed: () -> Unit = {},
    memoryVM: MemoriesViewModel = viewModel(),
) {
  val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
  // Bottom sheet heights scale with the current device size
  val sheetConfig =
      BottomSheetConfig(
          collapsedHeight = MapConstants.COLLAPSED_HEIGHT,
          mediumHeight = MapConstants.MEDIUM_HEIGHT,
          fullHeight = screenHeightDp * MapConstants.FULL_HEIGHT_PERCENTAGE)

  val viewModel = rememberMapScreenViewModel(sheetConfig)
  val eventViewModel = remember {
    EventViewModel(EventRepositoryProvider.getRepository(), viewModel.eventStateController)
  }
  val selectedEvent by viewModel.selectedEvent.collectAsState()

  val locationViewModel = run {
    val applicationContext = LocalContext.current.applicationContext
    val factory =
        remember(applicationContext) {
          LocationViewModelFactory(context = applicationContext, client = HttpClientProvider.client)
        }
    viewModel<LocationViewModel>(factory = factory)
  }

  val snackbarHostState = remember { SnackbarHostState() }
  val density = LocalDensity.current
  val mediumSheetBottomPaddingPx = with(density) { sheetConfig.mediumHeight.toPx() }
  val edgePaddingPx = with(density) { 24.dp.toPx() }
  val extraBottomMarginPx = with(density) { 32.dp.toPx() }
  val bottomPaddingPx = mediumSheetBottomPaddingPx + extraBottomMarginPx
  val coroutineScope = rememberCoroutineScope()
  val context = LocalContext.current

  // Track if we should request notification permission after location permission
  var shouldRequestNotificationPermission by remember { mutableStateOf(false) }

  // Notification permission launcher (Android 13+)
  val notificationPermissionLauncher =
      rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) {
          isGranted ->
        if (!isGranted) {
          coroutineScope.launch { snackbarHostState.showSnackbar("Notification permission denied") }
        }
      }

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

            // After location permission result, request notification permission if needed
            if (shouldRequestNotificationPermission) {
              shouldRequestNotificationPermission = false
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasNotificationPermission =
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED

                if (!hasNotificationPermission) {
                  notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
              }
            }
          }

  // Auto-request permissions on app launch (skip in test mode)
  LaunchedEffect(autoRequestPermissions) {
    if (autoRequestPermissions) {
      // Request location permission if not already granted
      viewModel.checkLocationPermission()
      if (!viewModel.hasLocationPermission) {
        shouldRequestNotificationPermission = true
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION))
      } else {
        // If location permission already granted, request notification permission directly
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          val hasNotificationPermission =
              ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                  PackageManager.PERMISSION_GRANTED

          if (!hasNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          }
        }
      }
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

  // Handle deep link navigation to event
  LaunchedEffect(deepLinkEventId) {
    deepLinkEventId?.let { eventId ->
      viewModel.onDeepLinkEvent(eventId)
      onDeepLinkConsumed()
    }
  }

  // Retry deep link selection once events finish loading
  LaunchedEffect(Unit) { snapshotFlow { viewModel.events }.collect { viewModel.onEventsUpdated() } }

  // Handle resolved deep link event from ViewModel (separates VM logic from UI navigation)
  LaunchedEffect(viewModel.resolvedDeepLinkEvent) {
    viewModel.resolvedDeepLinkEvent?.let { event ->
      viewModel.onEventPinClicked(event, forceZoom = true)
      viewModel.clearResolvedDeepLinkEvent()
    }
  }

  // Setup location management
  LaunchedEffect(Unit) {
    viewModel.checkLocationPermission()
    if (viewModel.hasLocationPermission) {
      viewModel.startLocationUpdates()
      viewModel.getLastKnownLocation(centerCamera = false)
    }

    viewModel.onRequestLocationPermission = {
      locationPermissionLauncher.launch(
          arrayOf(
              Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
  }

  // Get saved camera position from preferences
  val preferencesRepository = remember { PreferencesRepositoryProvider.getInstance(context) }
  val savedLatitude by preferencesRepository.cameraLatitudeFlow.collectAsState(initial = null)
  val savedLongitude by preferencesRepository.cameraLongitudeFlow.collectAsState(initial = null)
  val savedZoom by preferencesRepository.cameraZoomFlow.collectAsState(initial = null)

  // Start the camera centered on the default campus view or saved position
  val mapViewportState = rememberMapViewportState {
    setCameraOptions {
      zoom(MapConstants.DEFAULT_ZOOM.toDouble())
      center(Point.fromLngLat(MapConstants.DEFAULT_LONGITUDE, MapConstants.DEFAULT_LATITUDE))
      pitch(0.0)
      bearing(0.0)
    }
  }

  // Restore saved camera position if available
  LaunchedEffect(savedLatitude, savedLongitude, savedZoom) {
    if (savedLatitude != null && savedLongitude != null && savedZoom != null) {
      mapViewportState.easeTo(
          cameraOptions {
            center(Point.fromLngLat(savedLongitude!!, savedLatitude!!))
            zoom(savedZoom!!)
          },
          MapAnimationOptions.Builder().duration(0L).build())
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

      if (event.location.isDefined()) {
        mapViewportState.easeTo(
            cameraOptions {
              center(Point.fromLngLat(event.location.longitude!!, event.location.latitude!!))
              zoom(targetZoom)
              padding(EdgeInsets(0.0, 0.0, offsetPixels * 2, 0.0))
            },
            animationOptions = animationOptions)
      }
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
            events
                .filter { it.location.isDefined() }
                .map { event ->
                  Point.fromLngLat(event.location.longitude!!, event.location.latitude!!)
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

  // Get map preferences and theme from PreferencesRepository (already declared above)
  val themeModeString by preferencesRepository.themeModeFlow.collectAsState(initial = "system")
  val showPOIs by preferencesRepository.showPOIsFlow.collectAsState(initial = true)
  val showRoadNumbers by preferencesRepository.showRoadNumbersFlow.collectAsState(initial = true)
  val showStreetNames by preferencesRepository.showStreetNamesFlow.collectAsState(initial = true)
  val enable3DView by preferencesRepository.enable3DViewFlow.collectAsState(initial = true)

  // Monitor connectivity state for offline indicator
  val connectivityService = remember { ConnectivityServiceProvider.getInstance(context) }
  val connectivityState by
      connectivityService.connectivityState.collectAsState(
          initial = com.swent.mapin.model.network.ConnectivityState(isConnected = false))
  val isOffline = !connectivityState.isConnected

  // Track viewport center to determine if in cached region
  var viewportCenter by remember { mutableStateOf<Point?>(null) }
  LaunchedEffect(mapViewportState) {
    snapshotFlow { mapViewportState.cameraState }
        .filterNotNull()
        .debounce(300) // Debounce to avoid excessive recalculations
        .collect { cameraState -> viewportCenter = cameraState.center }
  }

  // Save camera position when it changes (debounced to avoid excessive writes)
  LaunchedEffect(mapViewportState) {
    snapshotFlow { mapViewportState.cameraState }
        .filterNotNull()
        .debounce(1000) // Wait 1 second after camera stops moving
        .collect { cameraState ->
          val center = cameraState.center
          val zoom = cameraState.zoom
          preferencesRepository.saveCameraPosition(
              latitude = center.latitude(), longitude = center.longitude(), zoom = zoom)
        }
  }

  // Calculate if viewport center is within cached event radius
  val isInCachedRegion by remember {
    androidx.compose.runtime.derivedStateOf {
      val center = viewportCenter
      if (center == null) {
        false
      } else {
        val cachedEvents = (viewModel.savedEvents + viewModel.joinedEvents).distinctBy { it.uid }
        cachedEvents
            .filter { it.location.isDefined() }
            .any { event ->
              val distance =
                  EventUtils.calculateHaversineDistance(
                      GeoPoint(center.latitude(), center.longitude()),
                      GeoPoint(event.location.latitude!!, event.location.longitude!!))
              distance <= EventBasedOfflineRegionManager.DEFAULT_RADIUS_KM
            }
      }
    }
  }

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

  LaunchedEffect(viewModel.clickedPosition) {
    if (viewModel.clickedPosition != null &&
        viewModel.mapStyle == MapScreenViewModel.MapStyle.HEATMAP) {
      viewModel.handleHeatmapClickForMemories()
    }
  }

  LaunchedEffect(viewModel.shouldNavigateToNearbyMemories) {
    if (viewModel.shouldNavigateToNearbyMemories) {
      val params = viewModel.getNearbyMemoriesParams()
      if (params != null) {
        memoryVM.loadMemoriesNearLocation(
            location = Location.from("MapClick", params.first.latitude, params.first.longitude),
            radius = params.second)
      }
      viewModel.onNearbyMemoriesNavigated()
      onNavigateToMemories()
    }
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

    // Offline indicator at top-right
    OfflineIndicator(
        isOffline = isOffline,
        isInCachedRegion = isInCachedRegion,
        modifier = Modifier.align(Alignment.TopEnd).padding(top = 60.dp, end = 16.dp))

    // Route info card when directions are displayed
    val directionState = viewModel.directionViewModel.directionState
    if (directionState is DirectionState.Displayed) {
      RouteInfoCard(
          routeInfo = directionState.routeInfo,
          modifier = Modifier.align(Alignment.TopCenter).padding(top = 100.dp))
    }

    // Overlays et contrôles au-dessus de la carte
    Box(
        modifier =
            Modifier.align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = chatBottomPadding)) {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // AI Assistant button
            FilledIconButton(
                onClick = { onNavigateToAiAssistant() },
                shape = CircleShape,
                modifier = Modifier.size(48.dp).testTag("aiAssistantButton"),
                colors =
                    IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary)) {
                  Icon(imageVector = Icons.Default.Mic, contentDescription = "AI Assistant")
                }

            // Chat button
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
          val selectedMemory by memoryVM.selectedMemory.collectAsState()

          LaunchedEffect(selectedMemory) {
            if (selectedMemory != null) {
              viewModel.setBottomSheetState(BottomSheetState.FULL)
            }
          }

          // sheetTarget : type of bottomSheetContent to show
          val sheetTarget =
              when {
                selectedMemory != null ->
                    SheetContent.Memory(selectedMemory!!) // The selected memory
                selectedEvent != null -> SheetContent.Event(selectedEvent!!) // The selected event
                else -> SheetContent.None // The bottomsheet
              }

          AnimatedContent(
              targetState = sheetTarget,
              transitionSpec = {
                val direction = if (targetState !is SheetContent.None) 1 else -1
                (fadeIn(animationSpec = tween(260)) +
                        slideInVertically(
                            animationSpec = tween(260), initialOffsetY = { direction * it / 6 }))
                    .togetherWith(
                        fadeOut(animationSpec = tween(200)) +
                            slideOutVertically(
                                animationSpec = tween(200),
                                targetOffsetY = { -direction * it / 6 }))
              },
              label = "memoryOrEventSheetTransition") { content ->
                when (content) {
                  is SheetContent.Event -> {
                    EventDetailSheet(
                        event = content.event,
                        sheetState = viewModel.bottomSheetState,
                        isParticipating =
                            viewModel.joinedEvents.any { it.uid == content.event.uid },
                        isSaved = viewModel.savedEvents.any { it.uid == content.event.uid },
                        organizerState = viewModel.organizerState,
                        onJoinEvent = { viewModel.joinEvent() },
                        onUnregisterEvent = { viewModel.unregisterFromEvent() },
                        onSaveForLater = { viewModel.saveEventForLater() },
                        onUnsaveForLater = { viewModel.unsaveEventForLater() },
                        onClose = { viewModel.closeEventDetailWithNavigation() },
                        onShare = { viewModel.showShareDialog() },
                        onGetDirections = { viewModel.toggleDirections(content.event) },
                        showDirections =
                            viewModel.directionViewModel.directionState is DirectionState.Displayed,
                        hasLocationPermission = viewModel.hasLocationPermission,
                        onOrganizerClick = { userId -> viewModel.showProfileSheet(userId) })
                  }
                  is SheetContent.Memory -> {
                    MemoryDetailSheet(
                        memory = content.memory,
                        sheetState = viewModel.bottomSheetState,
                        ownerName = memoryVM.ownerName.collectAsState().value,
                        taggedUserNames = memoryVM.taggedNames.collectAsState().value,
                        onClose = {
                          memoryVM.clearSelectedMemory()
                          viewModel.closeMemoryDetailSheet()
                        })
                  }
                  is SheetContent.None -> {
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
                        onRecentProfileClick = viewModel::onRecentProfileClicked,
                        onClearRecentSearches = viewModel::clearRecentSearches,
                        userSearchResults = viewModel.userSearchResults,
                        onUserSearchClick = { userId, userName ->
                          viewModel.saveRecentUser(userId, userName)
                          viewModel.openUserProfileSheet(userId)
                        },
                        currentScreen = viewModel.currentBottomSheetScreen,
                        availableEvents = viewModel.availableEvents,
                        initialMemoryEvent = viewModel.memoryFormInitialEvent,
                        onEventClick = { event ->
                          // Handle event click from search - focus pin, show details, remember
                          // search mode
                          viewModel.onEventClickedFromSearch(event)
                          onEventClick(event)
                        },
                        onEditEvent = { event ->
                          eventViewModel.selectEventToEdit(event)
                          viewModel.showEditEventForm()
                        },
                        onDeleteEvent = { event -> viewModel.requestDeleteEvent(event) },
                        onEditEventDone = {
                          eventViewModel.clearEventToEdit()
                          viewModel.onEditEventCancel()
                        },
                        onCreateMemoryClick = viewModel::showMemoryForm,
                        onCreateEventClick = viewModel::showAddEventForm,
                        onNavigateToFriends = onNavigateToFriends,
                        onNavigateToMemories = onNavigateToMemories,
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
                        locationViewModel = locationViewModel,
                        profileViewModel = remember { ProfileViewModel() },
                        eventViewModel = eventViewModel,
                        profileSheetUserId = viewModel.profileSheetUserId,
                        onProfileSheetClose = viewModel::hideProfileSheet,
                        onProfileSheetEventClick = viewModel::onProfileSheetEventClick)
                  }
                }
              }
        }

    // Share dialog
    if (viewModel.showShareDialog && selectedEvent != null) {
      ShareEventDialog(event = selectedEvent!!, onDismiss = { viewModel.dismissShareDialog() })
    }

    viewModel.eventPendingDeletion?.let { eventToDelete ->
      if (viewModel.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = { Text(stringResource(R.string.delete_event)) },
            text = { Text(stringResource(R.string.delete_alert_text)) },
            confirmButton = {
              TextButton(
                  onClick = {
                    eventViewModel.deleteEvent(eventToDelete.uid)
                    viewModel.cancelDelete()
                  }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                  }
            },
            dismissButton = {
              TextButton(onClick = { viewModel.cancelDelete() }) { Text("Cancel") }
            })
      }
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
        MapEffect(Unit) { mapView ->
          mapView.gestures.addOnMapClickListener { point ->
            viewModel.onMapClicked(point.latitude(), point.longitude())

            false
          }
        }
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

  val selectedEvent by viewModel.selectedEvent.collectAsState()

  val annotationStyle =
      remember(isDarkTheme, markerBitmap) { createAnnotationStyle(isDarkTheme, markerBitmap) }

  // Create event-specific pin bitmaps based on event tag and capacity
  val eventBitmaps =
      remember(context, viewModel.events.map { it.uid }) {
        createEventBitmaps(context, viewModel.events)
      }

  val annotations =
      remember(viewModel.events, annotationStyle, selectedEvent, eventBitmaps) {
        createEventAnnotations(viewModel.events, annotationStyle, selectedEvent?.uid, eventBitmaps)
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

  // Only render pins when heatmap is not shown
  if (!viewModel.showHeatmap) {
    // Disable clustering when a pin is selected to prevent it from being absorbed
    val shouldCluster = selectedEvent == null

    // Render annotations (with or without clustering)
    if (!shouldCluster) {
      // No clustering: used for heatmap mode or when a pin is selected
      PointAnnotationGroup(annotations = annotations) {
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
