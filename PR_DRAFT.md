## Description
This PR introduces a significant enhancement to the Map-In application: a **Manual Location Picker** for creating events.

The **Manual Location Picker** allows users to precisely select an event's location by interacting directly with the map, providing greater flexibility than just searching or using the current location.

**Related Issue:** N/A

---

## Changes

**Implementation:**
- **Manual Location Picker:**
    - Added `ManualLocationPicker.kt`: A Mapbox-based dialog allowing users to pan, zoom, and tap to select a location.
    - Integrated into `AddEventScreen.kt`: Users can now choose to "Pick on Map" when creating an event.
    - Updated `AddEventUtils.kt` and `EventDetailSheet.kt` to support manual location selection flows.
- **UI & Navigation:**
    - Updated `MapScreenViewModel.kt` and related files to support the new features.

**Tests:**
- Added `ManualLocationPickerTest.kt`: Unit/Integration tests for the new picker component.
- Updated `AddEventUtilsTest.kt`: Adjusted tests to cover new location selection logic.
- Updated `MapScreenViewModelTest.kt` and `MapEventStateControllerTest.kt` to reflect changes in state management.

---

## Testing
- [x] Unit tests pass (`./gradlew testDebugUnitTest`)
- [ ] Instrumentation tests pass (`./gradlew connectedDebugAndroidTest`)
- [x] Manual testing completed
- [ ] Coverage maintained (≥80%)

**Test summary:**
- Verified the Manual Location Picker allows selecting a point and correctly populates the address/coordinates in the "Add Event" form.
- Existing tests for Chat and Map functionality continue to pass.

---

## Screenshots/Videos
*(Placeholder: Attach screenshots of the new "Select location on map" dialog here)*

---

## Checklist
- [x] Sufficient documentation and minimal inline comments
- [x] All tests passing
- [x] No new warnings
- [x] If related issue exists: issue has all labels, fields, and milestone filled