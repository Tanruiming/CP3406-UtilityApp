package au.edu.jcu.assessment.utilityapp.model

import java.time.Duration
import java.time.Instant
import java.time.ZoneId

enum class City(val label: String, val latitude: Double, val longitude: Double) {
    SINGAPORE("Singapore", 1.3521, 103.8198),
    TOWNSVILLE("Townsville", -19.2590, 146.8169),
    CAIRNS("Cairns", -16.9186, 145.7781)
}

enum class TemperatureUnit(val label: String) {
    CELSIUS("°C"), FAHRENHEIT("°F");

    fun convert(celsius: Double): Double =
        if (this == CELSIUS) celsius else celsius * 9 / 5 + 32
}

data class Preferences(
    val city: City = City.SINGAPORE,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val rainThreshold: Int = 50
)

// Open-Meteo's probability describes the hour PRECEDING its timestamp.
data class ForecastHour(val endsAt: Instant, val rainProbability: Int?)

data class WeatherSnapshot(
    val city: City,
    val zone: ZoneId,
    val currentTime: Instant,
    val temperatureCelsius: Double,
    val feelsLikeCelsius: Double?,
    val weatherCode: Int?,
    val hours: List<ForecastHour>,
    val fetchedAt: Instant
) {
    fun upcomingHours(now: Instant): List<ForecastHour> =
        hours.filter { it.endsAt > now }.take(3)

    fun isStale(now: Instant): Boolean =
        Duration.between(fetchedAt, now) > Duration.ofMinutes(15) ||
            Duration.between(currentTime, now) > Duration.ofMinutes(90)
}

data class RainOutlook(val hours: List<ForecastHour>) {
    // Missing values must not silently become a 0% probability.
    val maximumProbability: Int?
        get() = if (hours.size == 3 && hours.all { it.rainProbability != null }) {
            hours.maxOf { requireNotNull(it.rainProbability) }
        } else null

    fun shouldCarryUmbrella(threshold: Int): Boolean? =
        maximumProbability?.let { it >= threshold }
}

fun weatherDescription(code: Int?): String = when (code) {
    0 -> "Clear sky"
    1, 2 -> "Partly cloudy"
    3 -> "Overcast"
    45, 48 -> "Foggy"
    51, 53, 55, 56, 57 -> "Drizzle"
    61, 63, 65, 66, 67 -> "Rain"
    71, 73, 75, 77 -> "Snow"
    80, 81, 82 -> "Rain showers"
    85, 86 -> "Snow showers"
    95, 96, 99 -> "Thunderstorms"
    else -> "Current weather estimate"
}
