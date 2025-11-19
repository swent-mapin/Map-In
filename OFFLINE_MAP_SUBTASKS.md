# Offline Map Implementation - Sub-Issues

Parent Issue: #271 - Offline Map Feature

This document breaks down the offline map implementation into 4 manageable sub-issues, each deliverable as an independent PR with tests achieving 80%+ coverage.

**Note:** Mapbox automatically caches recently viewed map tiles using an LRU (least-recently used) policy, so manual viewport-based caching is unnecessary. This implementation focuses on proactive downloads for saved/joined events.

---

## Sub-Issue 1: Network Connectivity Detection Service ✅ COMPLETED

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

## Sub-Issue 2: Mapbox TileStore Configuration and Setup ✅ COMPLETED

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

## Sub-Issue 3: Event-Based Offline Region Downloads

### Description
Implement automatic downloading and caching of map tiles for a 2km radius around the user's saved and joined events when the device is online. This ensures users can view event locations offline without relying solely on Mapbox's automatic viewport caching.

The system should monitor the user's saved/joined events list and proactively download map tiles covering a 2km radius around each event location. Downloads happen in the background without blocking the UI, respecting storage limits from Sub-Issue 2.

### Why This is Needed
While Mapbox automatically caches recently viewed tiles (LRU policy), users need guaranteed offline access to important event locations even if they haven't recently viewed them. Proactively downloading event regions ensures users can navigate to saved/joined events offline.

### Acceptance Criteria
- System observes user's saved and joined events (from existing event repository/ViewModel)
- For each saved/joined event, calculates a 2km radius bounding box around the event's coordinates
- When online, triggers asynchronous tile downloads for each event region via TileStore (from Sub-Issue 2)
- Downloads happen in background without blocking UI
- Connectivity state (from Sub-Issue 1) determines whether downloads should occur
- If an event is removed from saved/joined lists, its cached region can be optionally purged
- A manager class coordinates event monitoring and download operations
- Unit tests verify download triggering based on event changes, radius calculation, and TileStore integration
- Tests achieve 80%+ code coverage

### Testing Requirements
- Unit tests for event-based download manager
- Mock event repository, TileStore, and connectivity service
- Verify downloads are triggered when events are saved/joined and device is online
- Verify 2km radius bounding box calculation is correct
- Verify downloads are NOT triggered when offline
- Test handling of download failures and retries
- Test cache cleanup when events are removed

### Dependencies
- Requires: Sub-Issue 1 (connectivity detection)
- Requires: Sub-Issue 2 (TileStore setup)
- Requires: Existing event repository/ViewModel (to observe saved/joined events)

### Files Expected (~300-400 lines, 4-5 files)
- New: Event-based offline region manager
- New: Radius/bounding box utility functions
- New: Unit tests
- Update: Event ViewModel or repository (integrate download triggers)

---

## Sub-Issue 4: Offline Mode User Feedback UI

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

| Sub-Issue | Status | Lines | Files | Dependencies | Key Focus |
|-----------|--------|-------|-------|--------------|-----------|
| 1. Connectivity Detection | ✅ Merged | 150-200 | 3 | None | Foundation - network monitoring |
| 2. TileStore Setup | ✅ Merged | 200-250 | 3-4 | #1 | Infrastructure - storage config |
| 3. Event-Based Downloads | 🔄 To Do | 300-400 | 4-5 | #1, #2, Events | Core feature - event region caching |
| 4. User Feedback UI | 🔄 To Do | 170-225 | 4 | #1, #3 | UX - offline indicators |

## Implementation Order

1. ✅ **Completed**: Sub-Issue 1 (Connectivity Detection) - Foundation for everything
2. ✅ **Completed**: Sub-Issue 2 (TileStore Setup) - Storage infrastructure
3. 🔄 **Next**: Sub-Issue 3 (Event-Based Downloads) - Download 2km radius around saved/joined events
4. 🔄 **Final**: Sub-Issue 4 (User Feedback UI) - Offline mode indicators

Each PR includes its own tests with 80%+ coverage requirement.
