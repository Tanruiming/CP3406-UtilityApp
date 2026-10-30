package au.edu.jcu.assessment.utilityapp

import au.edu.jcu.assessment.utilityapp.model.City
import au.edu.jcu.assessment.utilityapp.model.ForecastHour
import au.edu.jcu.assessment.utilityapp.model.RainOutlook
import au.edu.jcu.assessment.utilityapp.model.TemperatureUnit
import au.edu.jcu.assessment.utilityapp.model.WeatherSnapshot
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class WeatherLogicTest {
    private val now = Instant.parse("2026-01-01T15:30:00Z")

    @Test fun thresholdEqualityRecommendsUmbrella() {
        val outlook = RainOutlook(listOf(10, 50, 20).mapIndexed { index, value ->
            ForecastHour(now.plusSeconds((index + 1) * 3600L), value)
        })
        assertEquals(50, outlook.maximumProbability)
        assertEquals(true, outlook.shouldCarryUmbrella(50))
        assertEquals(false, outlook.shouldCarryUmbrella(60))
    }

    @Test fun missingProbabilityIsUnknownRatherThanZero() {
        val outlook = RainOutlook(listOf(10, null, 20).mapIndexed { index, value ->
            ForecastHour(now.plusSeconds((index + 1) * 3600L), value)
        })
        assertNull(outlook.maximumProbability)
        assertNull(outlook.shouldCarryUmbrella(50))
        assertNull(RainOutlook(emptyList()).maximumProbability)
    }

    @Test fun selectsFuturePeriodEndTimesAcrossMidnight() {
        val snapshot = snapshot((0..5).map { hour ->
            ForecastHour(Instant.parse("2026-01-01T15:00:00Z").plusSeconds(hour * 3600L), hour * 10)
        })
        val upcoming = snapshot.upcomingHours(now)
        assertEquals(listOf(10, 20, 30), upcoming.map { it.rainProbability })
        assertEquals(0, upcoming.first().endsAt.atZone(snapshot.zone).hour)
        assertEquals(2, upcoming.first().endsAt.atZone(snapshot.zone).dayOfMonth)
        assertEquals(20, snapshot.upcomingHours(Instant.parse("2026-01-01T16:00:00Z")).first().rainProbability)
    }

    @Test fun temperatureConversionUsesCelsiusAsSource() {
        assertEquals(32.0, TemperatureUnit.FAHRENHEIT.convert(0.0), 0.001)
        assertEquals(86.0, TemperatureUnit.FAHRENHEIT.convert(30.0), 0.001)
        assertEquals(30.0, TemperatureUnit.CELSIUS.convert(30.0), 0.001)
    }

    @Test fun agedForecastIsMarkedStale() {
        assertFalse(snapshot(emptyList()).isStale(now.plusSeconds(600)))
        assertTrue(snapshot(emptyList()).isStale(now.plusSeconds(901)))
    }

    private fun snapshot(hours: List<ForecastHour>) = WeatherSnapshot(
        City.SINGAPORE, ZoneId.of("Asia/Singapore"), now, 29.0, 32.0, 3, hours, now
    )
}
