package au.edu.jcu.assessment.utilityapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import au.edu.jcu.assessment.utilityapp.data.WeatherRepository
import au.edu.jcu.assessment.utilityapp.model.City
import au.edu.jcu.assessment.utilityapp.model.Preferences
import au.edu.jcu.assessment.utilityapp.model.TemperatureUnit
import au.edu.jcu.assessment.utilityapp.model.WeatherSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant

data class WeatherUiState(
    val preferences: Preferences = Preferences(),
    val weather: WeatherSnapshot? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val now: Instant = Instant.EPOCH
)

class WeatherViewModel(
    private val repository: WeatherRepository,
    private val clock: Clock
) : ViewModel() {
    private val mutableState = MutableStateFlow(WeatherUiState(now = clock.instant()))
    val uiState = mutableState.asStateFlow()
    private var fetchJob: Job? = null
    private var requestId: Long = 0

    init {
        refresh()
        viewModelScope.launch {
            while (isActive) {
                delay(60_000)
                mutableState.update { it.copy(now = clock.instant()) }
            }
        }
    }

    fun refresh() {
        val thisRequest = ++requestId
        fetchJob?.cancel()
        val city = mutableState.value.preferences.city
        mutableState.update { it.copy(isLoading = true, errorMessage = null, now = clock.instant()) }
        fetchJob = viewModelScope.launch {
            try {
                val weather = repository.fetchWeather(city)
                if (thisRequest != requestId) return@launch
                mutableState.update {
                    it.copy(weather = weather, isLoading = false, errorMessage = null, now = clock.instant())
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (thisRequest != requestId) return@launch
                val message = when (error) {
                    is IOException -> "Couldn't connect. Check your internet connection and try again."
                    is HttpException -> "The weather service is unavailable. Please try again shortly."
                    else -> "Couldn't read the weather forecast. Please try again."
                }
                mutableState.update { it.copy(isLoading = false, errorMessage = message, now = clock.instant()) }
            }
        }
    }

    fun selectCity(city: City) {
        if (city == mutableState.value.preferences.city) return
        mutableState.update {
            it.copy(preferences = it.preferences.copy(city = city), weather = null, errorMessage = null)
        }
        refresh()
    }

    fun selectTemperatureUnit(unit: TemperatureUnit) {
        mutableState.update { it.copy(preferences = it.preferences.copy(temperatureUnit = unit)) }
    }

    fun setRainThreshold(threshold: Int) {
        mutableState.update {
            it.copy(preferences = it.preferences.copy(rainThreshold = threshold.coerceIn(10, 90)))
        }
    }

    fun onAppResumed() {
        mutableState.update { it.copy(now = clock.instant()) }
        val state = mutableState.value
        if (!state.isLoading && (state.weather == null ||
                Duration.between(state.weather.fetchedAt, state.now) >= Duration.ofMinutes(10))) {
            refresh()
        }
    }

    companion object {
        fun factory(repository: WeatherRepository, clock: Clock): ViewModelProvider.Factory =
            viewModelFactory { initializer { WeatherViewModel(repository, clock) } }
    }
}
