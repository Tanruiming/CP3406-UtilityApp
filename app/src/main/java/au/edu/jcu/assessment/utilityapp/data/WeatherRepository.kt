package au.edu.jcu.assessment.utilityapp.data

import au.edu.jcu.assessment.utilityapp.model.City
import au.edu.jcu.assessment.utilityapp.model.ForecastHour
import au.edu.jcu.assessment.utilityapp.model.WeatherSnapshot
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

interface WeatherRepository {
    suspend fun fetchWeather(city: City): WeatherSnapshot
}

class NetworkWeatherRepository(
    private val api: WeatherApi,
    private val clock: Clock
) : WeatherRepository {
    override suspend fun fetchWeather(city: City): WeatherSnapshot =
        api.forecast(city.latitude, city.longitude).toSnapshot(city, clock.instant())
}

internal fun ForecastResponse.toSnapshot(city: City, fetchedAt: Instant): WeatherSnapshot {
    val currentWeather = requireNotNull(current) { "Missing current weather" }
    val temperature = requireNotNull(currentWeather.temperature) { "Missing temperature" }
    require(temperature.isFinite()) { "Invalid temperature" }
    val timestamps = requireNotNull(hourly?.time) { "Missing forecast times" }
    val probabilities = hourly?.probabilities
    val hours = timestamps.mapIndexedNotNull { index, timestamp ->
        timestamp?.let {
            ForecastHour(
                endsAt = Instant.ofEpochSecond(it),
                rainProbability = probabilities?.getOrNull(index)?.takeIf { value -> value in 0..100 }
            )
        }
    }.distinctBy { it.endsAt }.sortedBy { it.endsAt }
    require(hours.isNotEmpty()) { "Empty hourly forecast" }
    return WeatherSnapshot(
        city = city,
        zone = ZoneId.of(requireNotNull(timezone) { "Missing timezone" }),
        currentTime = Instant.ofEpochSecond(requireNotNull(currentWeather.time)),
        temperatureCelsius = temperature,
        feelsLikeCelsius = currentWeather.feelsLike?.takeIf { it.isFinite() },
        weatherCode = currentWeather.weatherCode,
        hours = hours,
        fetchedAt = fetchedAt
    )
}
