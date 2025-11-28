# Offline Map Feature – PR-by-PR Walkthrough (#278, #296, #308)

This document traces the evolution of the offline maps implementation across the three PRs you listed. It focuses on code flow/details, not just headlines, so you can reopen any file and match the behavior described here.

## PR #278 – Viewport-triggered offline downloads (initial implementation)

### Core idea
- Download tiles for whatever area the user is currently looking at, as soon as the camera settles, so that recently viewed regions remain available offline.
- Mapbox tile downloads are coordinated through a new `OfflineRegionManager`, fed with viewport bounds coming from a `ViewportBoundsCalculator`.

### Key code paths
- `app/src/main/java/com/swent/mapin/ui/map/MapScreen.kt`
  - Added a `LaunchedEffect` that watches `mapViewportState.cameraState` via `snapshotFlow`, `filterNotNull()`, and a `debounce(2000ms)`. This waits for camera idle for ~2s to avoid spamming downloads while the user pans/zooms.
  - Calculates viewport bounds via `ViewportBoundsCalculator.calculateBounds(center, zoom, screenWidthPx, screenHeightPx)`, then calls `viewModel.downloadOfflineRegion(bounds)`.
  - The Composable tracks `lastDownloadedBounds` to skip repeated downloads for the same view.
- `MapScreenViewModel` (`app/src/main/java/com/swent/mapin/ui/map/MapScreenViewModel.kt`)
  - Lazily builds an `OfflineRegionManager` using:
    - `TileStoreManagerProvider.getInstance().getTileStore()` (TileStore pre-initialized elsewhere).
    - A `connectivityFlow` from `ConnectivityServiceProvider.getInstance(applicationContext).connectivityState.map { it.isConnected }`.
  - New `downloadOfflineRegion(bounds: CoordinateBounds)` runs on `ioDispatcher`, calls `offlineRegionManager.downloadRegion(...)`, logs progress, and logs completion/failure.
  - Injects `ioDispatcher`/`mainDispatcher` (replacing hardcoded `Dispatchers.IO/Main`) to appease Sonar warnings and enable test overrides.
  - `onCleared()` cancels any active download to avoid leaks.
- `OfflineRegionManager` (`app/src/main/java/com/swent/mapin/ui/map/offline/OfflineRegionManager.kt`)
  - Entry point: `suspend fun downloadRegion(bounds, styleUri = Style.MAPBOX_STREETS, onProgress, onComplete)`.
  - Flow:
    1. Reads connectivity via the provided `Flow<Boolean>.first()`. If offline, returns a failure result immediately.
    2. Cancels any in-flight download (`currentDownload?.cancel()`) and nulls the handle to avoid stale callbacks.
    3. Builds a tileset descriptor with `OfflineManager.createTilesetDescriptor` over zoom 0–16.
    4. Converts bounds to `Polygon` via `CoordinateBounds.toPolygon()` (SW→SE→NE→NW→SW ring).
    5. Creates `TileRegionLoadOptions` with `acceptExpired(false)` and `NetworkRestriction.NONE`.
    6. Starts `tileStore.loadTileRegion(...)`, piping progress (`completedResourceCount / requiredResourceCount`) to `onProgress`, and completion/failure to `onComplete`. Clears `currentDownload` on callback.
  - `cancelActiveDownload()` cancels and clears the handle.
  - `CoordinateBounds` is defined beside the manager; `toPolygon()` trivial box polygon; tests cover geometry and hash/equals.
- `ViewportBoundsCalculator` (`app/src/main/java/com/swent/mapin/ui/map/offline/ViewportBoundsCalculator.kt`)
  - `calculateBounds(center: Point, zoom: Double, widthPx: Int, heightPx: Int)` approximates the visible bounding box:
    - Computes meters-per-pixel using Mercator (`circumference_at_lat / (256 * 2^zoom)`), then converts half-width/height in meters to lat/lng degree offsets (lng scaled by `cos(lat)`).
    - Returns `CoordinateBounds` with SW/NE points.
  - Designed for “good enough” precision to drive region download polygons.

### Tests added
- `CoordinateBoundsTest` (128 LOC): validates polygon closure, corners, negative coords, antimeridian handling, hash/equals.
- `ViewportBoundsCalculatorTest` (121 LOC): symmetry around center, aspect ratio differences, zoom scaling (higher zoom → smaller area), equator/high-lat behavior.
- Adjusted e2e: `renderMap` flag documented in `CLAUDE.md` and default kept false in tests to avoid Mapbox rendering crashes.

### Side notes
- Deleted `OFFLINE_MAP_SUBTASKS.md` (planning doc).
- Minor nav comment tweak in `AppNavHost` about `renderMap` flag semantics.

## PR #296 – Removal of viewport-based downloads (cleanup)

### Rationale
- Mapbox already auto-caches recently viewed tiles using an internal LRU. The explicit viewport download loop was considered redundant and potentially wasteful.

### What was removed
- `MapScreen.kt`: the entire `LaunchedEffect` + debounce block that tracked camera state, computed viewport bounds, and triggered `downloadOfflineRegion`.
- `MapScreenViewModel`: `downloadOfflineRegion(bounds)` removed; the class no longer exposes viewport-triggered download API.
- `ViewportBoundsCalculator.kt` and its test deleted entirely (84 + 121 LOC).

### What stayed
- `OfflineRegionManager` remained in the codebase (unused at this point) and retained `CoordinateBounds`.
- TileStore initialization infra stayed untouched.

### Behavioral net effect
- After this PR, no code path automatically downloaded tiles based on viewing the map. Offline availability relied solely on Mapbox’s implicit caching, without our own download orchestration.

## PR #308 – Event-based offline downloads (proactive caching)

### Core idea
- Instead of tracking the current viewport, proactively download a 2km radius around every saved or joined event while online. This guarantees event locations are available offline and avoids race conditions encountered with viewport-triggered cancels.

### Key code paths
- `EventBasedOfflineRegionManager` (`app/src/main/java/com/swent/mapin/ui/map/offline/EventBasedOfflineRegionManager.kt`)
  - Listens to saved/joined event streams and sequentially downloads regions.
  - `observeEvents(userId, onSavedEventsFlow, onJoinedEventsFlow)`: combines the two flows, de-duplicates by `event.uid`, checks `connectivityService.isConnected()`, then calls `downloadRegionsForEvents`.
  - `downloadRegionsForEvents(events)`: enforces `maxRegions` (default 100, well under Mapbox’s 750 tile-pack cap). Skips already-downloaded events via `downloadedEventIds`. For each event, computes bounds, then downloads **sequentially** using `downloadRegionSuspend` + `CompletableDeferred` to await completion. Sequential behavior was deliberate to avoid Mapbox canceling the previous `loadTileRegion` when a new one starts.
  - `calculateBoundsForRadius(centerLat, centerLng, radiusKm)` (public top-level helper): converts a km radius to a bounding box using degree approximations, clamps lat/lng to valid ranges, and guards `cos(lat)` near poles with `max(1e-6, cosLat)`.
  - Lifecycle: `stopObserving()` cancels flow collection and calls `offlineRegionManager.cancelActiveDownload()`. `clearDownloadedEventIds()` resets memoization to force re-downloads.
- `MapEventStateController` (`app/src/main/java/com/swent/mapin/ui/map/eventstate/MapEventStateController.kt`)
  - Added `StateFlow`s: `savedEventsFlow`, `joinedEventsFlow` that mirror the mutable lists already maintained.
  - When `loadJoinedEvents()`/`loadSavedEvents()` finish, the flows are updated, enabling the offline manager to react.
- `MapScreenViewModel`
  - New lazy `eventBasedOfflineRegionManager` (nullable) controlled by `enableEventBasedDownloads` flag (tests pass `false` to avoid side-effects).
  - `init` now calls `startEventBasedOfflineDownloads()` if enabled. It:
    - Grabs current `userId` from Firebase auth (no downloads if not logged in).
    - Subscribes to `savedEventsFlow` + `joinedEventsFlow`.
    - Logs startup or errors.
  - `onCleared()` stops observation and cancels any offline download.
  - No reintroduction of the old viewport download path; all new offline work is event-driven.
- `OfflineRegionManager` (same file as PR #278)
  - Default style switched to `Style.STANDARD` (was `MAPBOX_STREETS`) to align downloads with the map’s display style; previous mismatch caused blank offline maps despite successful downloads.
- `TileStoreManager` (`app/src/main/java/com/swent/mapin/ui/map/offline/TileStoreManager.kt`)
  - Default disk quota raised from 50MB to 2GB (`DEFAULT_DISK_QUOTA_MB = 2048`). Supports roughly 40–60 event regions before eviction; still enforced via `TileStoreOptions.DISK_QUOTA`.

### Tests added/updated
- `EventBasedOfflineRegionManagerTest` (314 LOC):
  - Verifies observation triggers downloads when online, skips when offline, combines flows without duplicates, respects `maxRegions`, skips already-downloaded events, and clears/recounts downloaded IDs.
  - Uses MockK to intercept `OfflineRegionManager.downloadRegion` callbacks and coroutine test scheduler to flush flows.
- `EventRadiusBoundsCalculatorTest` (168 LOC):
  - Validates `calculateBoundsForRadius` geometry: size scaling by radius, symmetry, equator/high-latitude behavior, clamping at poles/antimeridian, zero-radius degenerate case.
- `TileStoreManagerTest` and `TileStoreIntegrationTest` updated to expect the new 2GB quota and byte conversion.
- `MapScreenViewModelAuthListenerTest` and `MapScreenViewModelTest` pass `enableEventBasedDownloads = false` to isolate auth/search behaviors from new download side-effects.

### Behavioral flow after PR #308
1. On ViewModel init (if enabled and user is logged in), we start observing saved/joined events.
2. When either list changes and connectivity is true:
   - The combined distinct events list is truncated to `maxRegions` (default 100).
   - For each not-yet-downloaded event, we compute a 2km bounding box and call `OfflineRegionManager.downloadRegion`.
   - Downloads run **one at a time**; completion adds the event ID to `downloadedEventIds`.
3. Cleanup: navigating away/dispose triggers `stopObserving()` + `cancelActiveDownload()`.
4. Storage: TileStore now has 2GB quota; downloads target `Style.STANDARD` tiles so offline rendering matches online view.

## End state (relative to the three PRs)
- The viewport-based approach from #278 was removed in #296 and not restored.
- #308 reintroduces offline downloading but scoped to saved/joined events, with stronger correctness (sequential downloads, style alignment, larger cache).
- `OfflineRegionManager` remains the low-level downloader; now fed by the event-based orchestrator instead of the camera.
- Disk quota and style defaults were updated to better match real usage.

If you want me to cross-check any specific file or execution path, tell me which class or scenario to deep-dive next. The current branch you have checked out is `feat/offline-user-feedback-ui`; nothing was committed while writing this report.***
