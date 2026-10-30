package au.edu.jcu.assessment.utilityapp

import au.edu.jcu.assessment.utilityapp.data.NetworkWeatherRepository
import au.edu.jcu.assessment.utilityapp.data.WeatherApi
import au.edu.jcu.assessment.utilityapp.model.City
import au.edu.jcu.assessment.utilityapp.model.RainOutlook
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

class WeatherRepositoryTest {
    private val server = MockWebServer()
    private lateinit var repository: NetworkWeatherRepository

    @Before fun setUp() {
        server.start()
        val api = Retrofit.Builder().baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create()).build().create(WeatherApi::class.java)
        repository = NetworkWeatherRepository(api, Clock.fixed(Instant.ofEpochSecond(1000), ZoneOffset.UTC))
    }

    @After fun tearDown() = server.shutdown()

    @Test fun sendsExpectedQueriesAndMapsJson() = runTest {
        server.enqueue(MockResponse().setBody(response("[10,70,40]")))
        val weather = repository.fetchWeather(City.SINGAPORE)
        assertEquals(29.2, weather.temperatureCelsius, 0.001)
        assertEquals("Asia/Singapore", weather.zone.id)
        assertEquals(70, RainOutlook(weather.hours).maximumProbability)
        val url = requireNotNull(server.takeRequest(2, TimeUnit.SECONDS)).requestUrl!!
        assertEquals("/v1/forecast", url.encodedPath)
        assertEquals("1.3521", url.queryParameter("latitude"))
        assertEquals("unixtime", url.queryParameter("timeformat"))
        assertEquals("precipitation_probability", url.queryParameter("hourly"))
        assertEquals("2", url.queryParameter("forecast_days"))
    }

    @Test fun nullAndOutOfRangeRainValuesRemainUnknown() = runTest {
        server.enqueue(MockResponse().setBody(response("[null,101,40]")))
        val weather = repository.fetchWeather(City.SINGAPORE)
        assertEquals(listOf(null, null, 40), weather.hours.map { it.rainProbability })
        assertNull(RainOutlook(weather.hours).maximumProbability)
    }

    @Test fun missingTemperatureIsRejected() = runTest {
        server.enqueue(MockResponse().setBody(response("[10,20,30]").replace("29.2", "null")))
        try {
            repository.fetchWeather(City.SINGAPORE)
            fail("Missing temperature must not become zero")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun httpFailureReachesViewModelRatherThanFakeWeather() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))
        try {
            repository.fetchWeather(City.CAIRNS)
            fail("Expected service failure")
        } catch (error: HttpException) {
            assertEquals(503, error.code())
        }
    }

    private fun response(probabilities: String) = """
        {"timezone":"Asia/Singapore",
         "current":{"time":1000,"temperature_2m":29.2,"apparent_temperature":32.1,"weather_code":3},
         "hourly":{"time":[3600,7200,10800],"precipitation_probability":$probabilities}}
    """.trimIndent()
}
