package au.edu.jcu.assessment.utilityapp.data

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,apparent_temperature,weather_code",
        @Query("hourly") hourly: String = "precipitation_probability",
        @Query("forecast_days") forecastDays: Int = 2,
        @Query("timezone") timezone: String = "auto",
        @Query("timeformat") timeFormat: String = "unixtime"
    ): ForecastResponse
}

data class ForecastResponse(
    val timezone: String? = null,
    val current: CurrentResponse? = null,
    val hourly: HourlyResponse? = null
)

data class CurrentResponse(
    val time: Long? = null,
    @SerializedName("temperature_2m") val temperature: Double? = null,
    @SerializedName("apparent_temperature") val feelsLike: Double? = null,
    @SerializedName("weather_code") val weatherCode: Int? = null
)

data class HourlyResponse(
    val time: List<Long?>? = null,
    @SerializedName("precipitation_probability") val probabilities: List<Int?>? = null
)
