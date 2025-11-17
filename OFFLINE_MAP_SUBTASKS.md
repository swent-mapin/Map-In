# Offline Map Implementation - Sub-Issues

Parent Issue: #271 - Offline Map Feature

This document breaks down the offline map implementation into 5 manageable sub-issues, each deliverable as an independent PR with tests achieving 80%+ coverage.

---

## Sub-Issue 1: Network Connectivity Detection Service

### Description
Implement a service to monitor the device's network connectivity state (online/offline) in real-time. This is foundational for the offline map feature, as it determines when to use cached tiles versus fetching fresh data from Mapbox servers.

The service should provide a reactive stream of connectivity states that can be observed by the map screen and other components. This enables the app to seamlessly transition between online and offline modes without user intervention.

### Why This is Needed
The parent issue (#271) requires the map to detect offline scenarios and switch to cached tiles automatically. Without connectivity detection, the app cannot determine when to use cached data versus when to fetch from the network.

### Acceptance Criteria
``- A connectivity service monitors network state using Android's `ConnectivityManager`
- The service exposes the current connectivity state as a Kotlin Flow or LiveData for reactive observation
- The service correctly detects transitions between online and offline states
- The service handles edge cases (airplane mode, WiFi without internet, mobile data toggle)
- Unit tests verify correct state emission for various network scenarios
- Tests achieve 80%+ code coverage
``
### Testing Requirements
- Unit tests for connectivity state changes
- Mock `ConnectivityManager` to simulate different network conditions
- Verify Flow emissions for online/offline transitions
- Test edge cases (no connectivity callback support on older APIs)

### Dependencies
None (foundational PR)

### Files Expected (~150-200 lines, 3 files)
- New: Connectivity service implementation
- New: Unit tests
- Update: Dependency injection setup

---

## Sub-Issue 2: Mapbox TileStore Configuration and Setup

### Description
Configure and initialize Mapbox's built-in TileStore API to enable offline tile caching. 
The TileStore is Mapbox's native mechanism for storing map tiles locally, and it 
handles the low-level details of tile storage, retrieval, and management.

This PR sets up the infrastructure for tile caching but does not yet implement automatic downloads. It ensures the TileStore is properly initialized when the map loads and is ready to store tiles.

### Why This is Needed
The parent issue (#271) requires local storage of map tiles. Mapbox's TileStore API provides a robust, tested solution for this rather than building custom tile storage. This PR establishes the foundation that subsequent PRs will build upon.

### Acceptance Criteria
- TileStore is initialized when the map screen loads
- TileStore is configured with appropriate storage limits (e.g., 50 MB as per parent issue requirements)
- A manager/wrapper class provides a clean interface to TileStore operations
- The map successfully connects to the configured TileStore
- Unit tests verify TileStore initialization and configuration
- Tests achieve 80%+ code coverage (with appropriate mocking of Mapbox SDK)

### Testing Requirements
- Unit tests for TileStore manager initialization
- Mock Mapbox TileStore SDK classes
- Verify configuration parameters (storage limits, paths)
- Test error handling for initialization failures

### Dependencies
- Requires: Sub-Issue 1 (connectivity detection, for future integration)

### Files Expected (~200-250 lines, 3-4 files)
- New: TileStore manager/wrapper class
- New: Unit tests
- Update: Map screen or ViewModel for initialization

---

## Sub-Issue 3: Offline Region Download and Caching

### Description
Implement automatic downloading and caching of map tiles for the current viewport when the device is online. This ensures that users can view previously accessed map areas when offline.

The system should intelligently download tiles for the visible map region (and potentially nearby regions) when online, respecting the storage limits established in Sub-Issue 2. Downloads should happen in the background without blocking map interactions.

### Why This is Needed
The parent issue (#271) requires that "map tiles for recently viewed map areas are stored locally." This PR implements the core caching mechanism that captures map tiles as users navigate the map while online.

### Acceptance Criteria
- When online, the system downloads and caches tiles for the current viewport
- Tile downloads happen asynchronously without blocking the UI
- Only tiles within the current or recently viewed map bounds are cached (optimization per parent issue)
- Downloaded tiles are persisted via the TileStore (from Sub-Issue 2)
- Connectivity state (from Sub-Issue 1) determines whether downloads should occur
- A repository or manager class coordinates download operations
- Unit tests verify download triggering logic and tile storage
- Tests achieve 80%+ code coverage

### Testing Requirements
- Unit tests for download manager/repository
- Mock TileStore and connectivity service
- Verify downloads are triggered when online and viewport changes
- Verify downloads are NOT triggered when offline
- Test handling of download failures and retries

### Dependencies
- Requires: Sub-Issue 1 (connectivity detection)
- Requires: Sub-Issue 2 (TileStore setup)

### Files Expected (~280-350 lines, 4-5 files)
- New: Offline region manager/download coordinator
- New: Cache repository interface
- New: Unit tests
- Update: Map ViewModel (trigger downloads on viewport changes)

---

## Sub-Issue 4: Cache Metadata and TTL-Based Invalidation

### Description
Implement a metadata system to track when map tiles were cached and automatically invalidate tiles older than 7 days (Time-To-Live). This ensures users see relatively fresh map data and don't rely on outdated tiles when online.

Use a lightweight Room database to store cache metadata (region identifiers, timestamps). When online, the system should check cached tile ages and refresh stale tiles from Mapbox servers.

### Why This is Needed
The parent issue (#271) requires: "Cached map tiles older than 7 days (TTL) are marked as stale and are not displayed unless the device is offline." This prevents users from seeing outdated map data when they have connectivity.

### Acceptance Criteria
- A Room database tracks metadata for cached map regions (region ID, timestamp, bounds)
- When tiles are cached (from Sub-Issue 3), metadata is recorded with current timestamp
- When online, the system identifies cached tiles older than 7 days
- Stale tiles are automatically refreshed from Mapbox when online
- Stale tiles are still usable when offline (degraded experience is better than no map)
- A cleanup mechanism removes metadata for deleted cache regions
- Unit tests verify TTL logic, database operations, and stale tile identification
- Tests achieve 80%+ code coverage

### Testing Requirements
- Unit tests for Room DAO operations
- Test cache metadata insertion, querying, and deletion
- Verify TTL calculation logic (7-day threshold)
- Integration tests for stale tile refresh workflow
- Test that stale tiles are served offline but refreshed when online

### Dependencies
- Requires: Sub-Issue 3 (downloads tiles that need metadata tracking)

### Files Expected (~290-340 lines, 6-7 files)
- New: Room database class
- New: Cache entity data model
- New: Cache DAO interface
- New: Unit tests for DAO and TTL logic
- Update: Offline region manager (integrate metadata tracking)

---

## Sub-Issue 5: Offline Mode User Feedback UI

### Description
Implement user-facing UI components to inform users when they are viewing cached (offline) map data versus live data. This includes a persistent banner/notification when in offline mode and error messages when no cached data is available.

The UI should be non-intrusive but clear, ensuring users understand the state of their map data without disrupting their workflow.

### Why This is Needed
The parent issue (#271) requires: "When displaying cached map data in offline mode, a UI notification informs the user that they are viewing an offline map" and "If no cached map data is available in offline mode, a message is displayed."

### Acceptance Criteria
- When offline and displaying cached tiles, a banner/toast notifies the user they are in offline mode
- The banner is non-intrusive (e.g., top or bottom of screen, dismissible or auto-hiding)
- When offline with no cached data available, a clear message explains the map cannot load
- The UI updates reactively based on connectivity state (from Sub-Issue 1)
- Offline indicator disappears when connectivity is restored
- String resources are used for all user-facing text (localization support)
- Compose UI tests verify banner visibility and state transitions
- Tests achieve 80%+ code coverage

### Testing Requirements
- Compose UI tests for offline banner visibility
- Verify banner shows when offline with cache
- Verify "no cache" message shows when offline without cache
- Verify banner hides when online
- Test banner interactions (if dismissible)

### Dependencies
- Requires: Sub-Issue 1 (connectivity state to trigger UI)
- Requires: Sub-Issue 3 (cache availability to determine which message to show)

### Files Expected (~170-225 lines, 4 files)
- New: Offline banner composable
- New: Compose UI tests
- Update: Map screen (integrate banner)
- Update: String resources

---

## Summary Table

| Sub-Issue | Lines | Files | Dependencies | Key Focus |
|-----------|-------|-------|--------------|-----------|
| 1. Connectivity Detection | 150-200 | 3 | None | Foundation - network monitoring |
| 2. TileStore Setup | 200-250 | 3-4 | #1 | Infrastructure - storage config |
| 3. Offline Region Download | 280-350 | 4-5 | #1, #2 | Core feature - tile caching |
| 4. Cache Metadata & TTL | 290-340 | 6-7 | #3 | Data freshness - invalidation |
| 5. User Feedback UI | 170-225 | 4 | #1, #3 | UX - offline indicators |

## Implementation Order

1. **First**: Sub-Issue 1 (Connectivity Detection) - No dependencies, foundation for everything
2. **Second**: Sub-Issue 2 (TileStore Setup) - Needs connectivity detection context
3. **Third**: Sub-Issue 3 (Offline Region Download) - Needs both #1 and #2
4. **Fourth**: Sub-Issue 4 (Cache Metadata & TTL) - Builds on #3
5. **Fifth**: Sub-Issue 5 (User Feedback UI) - Needs #1 and #3, polish layer

Each PR includes its own tests with 80%+ coverage requirement.
