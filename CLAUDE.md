# auto format - first time setup
Run this once after cloning the repository to install git hooks: `./scripts/setup-hooks.sh`

# Testing guide

## Formatting
- `./gradlew ktfmtFormat`: Format the code using ktfmt.

## Unit tests
- `./gradlew testDebugUnitTest`

## Instrumentation tests
- Start an emulator or plug in a device.
- `./gradlew connectedDebugAndroidTest`

## Full suite
- `./gradlew testDebugUnitTest connectedDebugAndroidTest`

## Coverage report
- Run the unit + instrumentation commands above first. Then: 
- `./gradlew jacocoTestReport`

## Cleanup and verification helpers
- `./gradlew clean` : delete all build outputs.
- `./gradlew check` : run the full verification pipeline (unit tests, lint, formatting checks).

# Build APK
- `./gradlew assembleDebug`: build APK
