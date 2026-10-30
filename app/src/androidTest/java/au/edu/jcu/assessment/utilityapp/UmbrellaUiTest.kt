package au.edu.jcu.assessment.utilityapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import au.edu.jcu.assessment.utilityapp.model.City
import au.edu.jcu.assessment.utilityapp.model.ForecastHour
import au.edu.jcu.assessment.utilityapp.model.WeatherSnapshot
import au.edu.jcu.assessment.utilityapp.ui.WeatherUiState
import au.edu.jcu.assessment.utilityapp.ui.theme.MobileTech_UtilityAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class UmbrellaUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun unitAndCitySettingsUpdateForecastAndSurviveTabChanges() {
        compose.setContent {
            var state by remember { mutableStateOf(sample()) }
            MobileTech_UtilityAppTheme {
                UtilityApp(state, {},
                    onCityChange = { city -> state = state.copy(
                        preferences = state.preferences.copy(city = city), weather = state.weather?.copy(city = city)) },
                    onUnitChange = { unit -> state = state.copy(preferences = state.preferences.copy(temperatureUnit = unit)) },
                    onThresholdChange = { threshold -> state = state.copy(preferences = state.preferences.copy(rainThreshold = threshold)) })
            }
        }
        compose.onNodeWithText("Take an umbrella.").assertIsDisplayed()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Fahrenheit · °F").performScrollTo().performClick()
        compose.onNodeWithText("Townsville").performScrollTo().performClick()
        compose.onNodeWithText("Forecast").performClick()
        compose.onNodeWithText("Townsville").assertIsDisplayed()
        compose.onNodeWithText("84°F").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Fahrenheit · °F").performScrollTo().assertIsDisplayed()
    }

    @Test fun missingRainDataDoesNotRecommendLeavingUmbrella() {
        compose.setContent {
            MobileTech_UtilityAppTheme {
                UtilityApp(sample().let { state -> state.copy(weather = state.weather?.copy(
                    hours = state.weather.hours.mapIndexed { index, hour ->
                        if (index == 1) hour.copy(rainProbability = null) else hour
                    })) }, {}, {}, {}, {})
            }
        }
        compose.onNodeWithText("Forecast incomplete.").assertIsDisplayed()
    }

    @Test fun failedInitialLoadShowsRetryAndRetryCallbackRuns() {
        var retries = 0
        compose.setContent {
            MobileTech_UtilityAppTheme {
                UtilityApp(WeatherUiState(errorMessage = "Test connection unavailable"),
                    { retries++ }, {}, {}, {})
            }
        }
        compose.onNodeWithText("Weather unavailable").assertIsDisplayed()
        compose.onNodeWithText("Try again").performClick()
        assertEquals(1, retries)
    }

    private fun sample(): WeatherUiState {
        val now = Instant.parse("2026-01-01T03:30:00Z")
        return WeatherUiState(weather = WeatherSnapshot(City.SINGAPORE, ZoneId.of("Asia/Singapore"),
            now, 29.0, 32.0, 3, listOf(20, 70, 50).mapIndexed { index, value ->
                ForecastHour(Instant.parse("2026-01-01T04:00:00Z").plusSeconds(index * 3600L), value)
            }, now), now = now)
    }
}
