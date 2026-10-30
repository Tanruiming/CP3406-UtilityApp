package au.edu.jcu.assessment.utilityapp

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import au.edu.jcu.assessment.utilityapp.data.WeatherRepository
import au.edu.jcu.assessment.utilityapp.model.City
import au.edu.jcu.assessment.utilityapp.model.TemperatureUnit
import au.edu.jcu.assessment.utilityapp.model.WeatherSnapshot
import au.edu.jcu.assessment.utilityapp.ui.WeatherViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val now = Instant.parse("2026-01-01T03:30:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test fun initialFailureCanBeRetried() = modelTest {
        var fail = true
        val model = model { city -> if (fail) throw IOException() else snapshot(city) }
        runCurrent()
        assertFalse(model.uiState.value.isLoading)
        assertNull(model.uiState.value.weather)
        assertNotNull(model.uiState.value.errorMessage)
        fail = false
        model.refresh()
        runCurrent()
        assertNotNull(model.uiState.value.weather)
        assertNull(model.uiState.value.errorMessage)
    }

    @Test fun failedRefreshRetainsLastSuccessfulForecastWithError() = modelTest {
        var fail = false
        val model = model { city -> if (fail) throw IOException() else snapshot(city) }
        runCurrent()
        val original = model.uiState.value.weather
        fail = true
        model.refresh()
        runCurrent()
        assertEquals(original, model.uiState.value.weather)
        assertNotNull(model.uiState.value.errorMessage)
    }

    @Test fun latestCityWinsWhenPreviousRequestIsStillPending() = modelTest {
        val first = CompletableDeferred<WeatherSnapshot>()
        val second = CompletableDeferred<WeatherSnapshot>()
        val model = model { city -> if (city == City.SINGAPORE) first.await() else second.await() }
        runCurrent()
        model.selectCity(City.TOWNSVILLE)
        runCurrent()
        second.complete(snapshot(City.TOWNSVILLE))
        runCurrent()
        first.complete(snapshot(City.SINGAPORE))
        runCurrent()
        assertEquals(City.TOWNSVILLE, model.uiState.value.weather?.city)
        assertEquals(City.TOWNSVILLE, model.uiState.value.preferences.city)
        assertFalse(model.uiState.value.isLoading)
    }

    @Test fun preferencesUpdateWithoutUnnecessaryNetworkCalls() = modelTest {
        var requests = 0
        val model = model { city -> requests++; snapshot(city) }
        runCurrent()
        model.selectTemperatureUnit(TemperatureUnit.FAHRENHEIT)
        model.setRainThreshold(70)
        model.selectCity(City.SINGAPORE)
        model.onAppResumed()
        runCurrent()
        assertEquals(1, requests)
        assertEquals(TemperatureUnit.FAHRENHEIT, model.uiState.value.preferences.temperatureUnit)
        assertEquals(70, model.uiState.value.preferences.rainThreshold)
    }

    private fun modelTest(block: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        try { block() } finally { store.clear() }
    }

    private fun model(fetch: suspend (City) -> WeatherSnapshot): WeatherViewModel {
        val repository = object : WeatherRepository {
            override suspend fun fetchWeather(city: City) = fetch(city)
        }
        return ViewModelProvider(store, WeatherViewModel.factory(repository, clock))[WeatherViewModel::class.java]
    }

    private fun snapshot(city: City) = WeatherSnapshot(city, ZoneOffset.UTC, now,
        29.0, 32.0, 3, emptyList(), now)
}
