# Verification guide

## Automated checks

From the project root on Windows:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

The connected tests need a running Android emulator or a USB-debugging-enabled Android device. They render deterministic test fixtures; production always uses the real weather API.

| Test group | What it checks |
| --- | --- |
| `WeatherLogicTest` | Threshold boundary, missing probabilities, period selection across midnight, unit conversion and stale forecasts. |
| `WeatherRepositoryTest` | Real Retrofit requests against MockWebServer, query parameters, JSON mapping, missing/out-of-range values and service errors. |
| `WeatherViewModelTest` | Initial failure/retry, saved data after failed refresh, overlapping city requests, and settings without unnecessary API calls. |
| `UmbrellaUiTest` | Settings-to-forecast navigation, city/unit updates, missing-data display, and retry interaction on an emulator. |

## Manual walkthrough

1. Open the app with internet access. Confirm Singapore loads live temperature and hourly probabilities.
2. Read the hero recommendation. Compare the maximum of the three hourly probabilities to the selected threshold (default 50%). Equality should recommend an umbrella.
3. In Settings, select Fahrenheit, then return to Forecast. The displayed temperature should convert without losing weather data.
4. Change the threshold to 10% or 90%. Return to Forecast and verify the rule uses the new threshold.
5. Select Townsville or Cairns. Confirm the new city loads and shows its local timezone, without retaining the previous city's forecast.
6. Turn off the test device's internet and tap Refresh. Verify an understandable error and retry button. Restore internet and retry.
7. Switch between tabs and rotate the device. Preferences should remain for the running app.
8. Check scrolling and bottom navigation in portrait, landscape, dark theme and larger font sizes.
9. Force-stop and relaunch the app. Defaults are expected to return because settings are not stored permanently.

## Verification record - 31 October 2026

- Project directory: the student's `Documents/GitHub/CP3406-UtilityApp` working copy.
- Debug APK assembled successfully.
- Unit tests: 14 passed (13 weather-specific tests plus the starter test).
- Connected tests: 4 passed (3 Compose interaction tests plus the starter instrumented test), on the Medium Phone API 37.0 Android emulator.
- Android Lint: 0 errors, 24 warnings. Warnings include existing starter dependency/target-version suggestions and unused starter resources; the project retains the lecturer's toolchain rather than undertaking an unrelated migration.
- A final palette adjustment was rebuilt successfully after the full test run.
- Live API smoke check: Singapore loaded temperature and three hourly probabilities; switching to Townsville loaded its forecast and Fahrenheit values. Changing the threshold from 50% to 90% changed the recommendation from "Take an umbrella" to "Lower rain risk" for the same 75% maximum hourly probability.
- Screenshots in `docs/screenshots/` were captured from the running app.

## Boundaries

- Weather is model-based and needs internet; the app does not claim certainty that it will or will not rain.
- The forecast is not a background notification service. It refreshes on demand and when returning to an old forecast.
- API availability, rate limits and physical-device behavior can differ from emulator tests.
