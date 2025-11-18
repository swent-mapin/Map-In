# Testing guide

## Formatting
- `./gradlew ktfmtFormat`: Format the code using ktfmt.

## Unit tests
- `./gradlew testDebugUnitTest`

## Instrumentation tests
- Start an emulator or plug in a device.
- `./gradlew connectedDebugAndroidTest`

### Map rendering flag in tests
- Instrumented/E2E tests run with `renderMap = false` to skip the Mapbox satellite map on CI emulators (avoids native crashes). Keep this disabled in tests; only enable when you explicitly need to exercise Mapbox rendering locally.

## Full suite
- `./gradlew testDebugUnitTest connectedDebugAndroidTest`

## Coverage report
- Run the unit + instrumentation commands above first. Then: 
- `./gradlew jacocoTestReport`
- Coverage: Run `open app/build/reports/jacoco/jacocoTestReport/html/index.html` or open the file in a browser.

## One liner for everything
- `./gradlew ktfmtFormat testDebugUnitTest connectedDebugAndroidTest jacocoTestReport && open app/build/reports/jacoco/jacocoTestReport/html/index.html`

## Cleanup and verification helpers
- `./gradlew clean` : delete all build outputs.
- `./gradlew check` : run the full verification pipeline (unit tests, lint, formatting checks).

# Build APK
- `./gradlew assembleDebug`: build APK

# Commits 

- Be concise and precise.
- Use normal commit style (Imperative, Upper case first letter)

# PRs

- Follow the pull_request_template.md
- Always push the PR in draft.
