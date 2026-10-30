package au.edu.jcu.assessment.utilityapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.edu.jcu.assessment.utilityapp.model.City
import au.edu.jcu.assessment.utilityapp.model.ForecastHour
import au.edu.jcu.assessment.utilityapp.model.Preferences
import au.edu.jcu.assessment.utilityapp.model.RainOutlook
import au.edu.jcu.assessment.utilityapp.model.TemperatureUnit
import au.edu.jcu.assessment.utilityapp.model.WeatherSnapshot
import au.edu.jcu.assessment.utilityapp.model.weatherDescription
import au.edu.jcu.assessment.utilityapp.ui.theme.MobileTech_UtilityAppTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val HeroInk = Color(0xFF103F3C)
private val HeroMint = Color(0xFFBDE7CA)
private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

@Composable
private fun PageColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp), content = content)
    }
}

@Composable
fun UtilityScreen(state: WeatherUiState, onRefresh: () -> Unit) {
    val weather = state.weather
    PageColumn {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Eyebrow("UMBRELLA BUDDY")
                Text("Before you head out.", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold)
            }
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                UmbrellaArt(Modifier.padding(8.dp).size(28.dp), MaterialTheme.colorScheme.primary)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(state.preferences.city.label, style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold)
                Text("Your local weather outlook", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = onRefresh, enabled = !state.isLoading) {
                Text(if (state.isLoading) "Updating…" else "Refresh")
            }
        }
        state.errorMessage?.let { message ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (weather == null) "Weather unavailable" else "Update failed · saved forecast",
                        fontWeight = FontWeight.Bold)
                    Text(message, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onRefresh, enabled = !state.isLoading) { Text("Try again") }
                }
            }
        }
        if (weather == null) {
            EmptyForecast(state.isLoading)
        } else {
            if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
            val hours = weather.upcomingHours(state.now)
            val outlook = RainOutlook(hours)
            val stale = weather.isStale(state.now)
            AdviceCard(outlook, state.preferences.rainThreshold, stale)
            CurrentWeatherCard(weather, state.preferences.temperatureUnit)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Upcoming hourly outlook", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                if (hours.isEmpty()) Text("No upcoming forecast periods. Refresh to get a new forecast.")
                else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    hours.forEach { hour ->
                        HourCard(hour, weather.zone, state.preferences.rainThreshold, Modifier.weight(1f))
                    }
                }
                Text("Each value is the rain chance during the displayed hour. The first period may already be underway.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${if (stale) "Saved forecast · refresh needed" else "Last fetched"} · ${time(weather.fetchedAt, weather.zone)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (stale) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Times shown in ${weather.zone.id}. Forecasts can change.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Attribution()
            }
        }
    }
}

@Composable
private fun Eyebrow(text: String, color: Color = MaterialTheme.colorScheme.primary) {
    Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
        letterSpacing = 1.8.sp, color = color)
}

@Composable
private fun EmptyForecast(loading: Boolean) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            UmbrellaArt(Modifier.size(110.dp), MaterialTheme.colorScheme.primary)
            if (loading) CircularProgressIndicator(Modifier.size(30.dp))
            Text(if (loading) "Checking the skies…" else "Let's get your forecast",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(if (loading) "Finding the latest weather for your city." else "Tap Refresh or Try again to load the weather.",
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AdviceCard(outlook: RainOutlook, threshold: Int, stale: Boolean) {
    val probability = outlook.maximumProbability
    val carry = if (stale) null else outlook.shouldCarryUmbrella(threshold)
    val heading = when {
        stale -> "Time for a refresh"
        carry == true -> "Take an umbrella."
        carry == false -> "Lower rain risk."
        else -> "Forecast incomplete."
    }
    val detail = when {
        stale -> "This forecast is getting old. Refresh before deciding what to bring."
        carry == true -> "Rain chance meets your ${threshold}% reminder threshold."
        carry == false -> "Rain chance is below your ${threshold}% reminder threshold."
        else -> "Some hourly rain data is missing. Try refreshing before heading out."
    }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
        .background(Brush.linearGradient(listOf(HeroInk, Color(0xFF205A50)))).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Eyebrow("HIGHEST HOURLY RAIN CHANCE", HeroMint)
                Text(if (probability == null) "—" else "$probability%",
                    fontSize = 54.sp, lineHeight = 60.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            UmbrellaArt(Modifier.size(68.dp), HeroMint)
        }
        Text(heading, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
        Text(detail, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFDFEDE5))
        Text("Based on the next 3 hourly forecast periods.", style = MaterialTheme.typography.bodySmall, color = HeroMint)
    }
}

@Composable
private fun CurrentWeatherCard(weather: WeatherSnapshot, unit: TemperatureUnit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Eyebrow("CURRENT ESTIMATE")
                Text(weatherDescription(weather.weatherCode), style = MaterialTheme.typography.titleMedium)
                Text("As of ${time(weather.currentTime, weather.zone)}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(temperature(weather.temperatureCelsius, unit), fontSize = 34.sp, fontWeight = FontWeight.Bold)
                weather.feelsLikeCelsius?.let {
                    Text("Feels ${temperature(it, unit)}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun HourCard(hour: ForecastHour, zone: ZoneId, threshold: Int, modifier: Modifier = Modifier) {
    val probability = hour.rainProbability
    val highlight = probability != null && probability >= threshold
    Card(modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(
        containerColor = if (highlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(time(hour.endsAt.minusSeconds(3600), zone), style = MaterialTheme.typography.labelMedium)
            Text("to ${time(hour.endsAt, zone)}", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(probability?.let { "$it%" } ?: "—", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("rain chance", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(preferences: Preferences, onCityChange: (City) -> Unit,
    onUnitChange: (TemperatureUnit) -> Unit, onThresholdChange: (Int) -> Unit) {
    PageColumn {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Eyebrow("MAKE IT YOURS")
            Text("Settings", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("A little planning. A better way out.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SettingsCard("Your city", "Choose where you're heading. No location permission needed.") {
            City.entries.forEachIndexed { index, city ->
                Row(Modifier.fillMaxWidth().selectable(selected = city == preferences.city,
                    onClick = { onCityChange(city) }, role = Role.RadioButton).padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RadioButton(selected = city == preferences.city, onClick = null)
                    Text(city.label, Modifier.padding(vertical = 12.dp))
                }
                if (index < City.entries.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
        SettingsCard("Temperature", "Choose the unit displayed on your forecast.") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TemperatureUnit.entries.forEach { unit ->
                    FilterChip(selected = unit == preferences.temperatureUnit, onClick = { onUnitChange(unit) },
                        label = { Text(if (unit == TemperatureUnit.CELSIUS) "Celsius · °C" else "Fahrenheit · °F") })
                }
            }
        }
        SettingsCard("Umbrella reminder", "Recommend an umbrella when the highest hourly rain chance reaches this threshold.") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Rain chance threshold", style = MaterialTheme.typography.labelLarge)
                Text("${preferences.rainThreshold}%", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Slider(value = preferences.rainThreshold.toFloat(),
                onValueChange = { onThresholdChange((it / 10).roundToInt() * 10) }, valueRange = 10f..90f, steps = 7)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("10% · More cautious", style = MaterialTheme.typography.labelSmall)
                Text("90% · Higher tolerance", style = MaterialTheme.typography.labelSmall)
            }
        }
        Text("Changes apply immediately. Settings stay while the app is running and reset when its process is restarted.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Attribution()
    }
}

@Composable
private fun SettingsCard(title: String, description: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun Attribution() {
    val uriHandler = LocalUriHandler.current
    TextButton(onClick = { uriHandler.openUri("https://open-meteo.com/") }) {
        Text("Weather data by Open-Meteo ↗", style = MaterialTheme.typography.labelMedium)
    }
}

// A code-drawn illustration remains crisp at different screen densities.
@Composable
private fun UmbrellaArt(modifier: Modifier, color: Color) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val canopy = Path().apply {
            moveTo(w * .1f, h * .48f)
            cubicTo(w * .16f, h * .04f, w * .84f, h * .04f, w * .9f, h * .48f)
            quadraticTo(w * .76f, h * .35f, w * .64f, h * .48f)
            quadraticTo(w * .5f, h * .35f, w * .36f, h * .48f)
            quadraticTo(w * .23f, h * .35f, w * .1f, h * .48f)
            close()
        }
        drawPath(canopy, color)
        val stem = Path().apply {
            moveTo(w * .5f, h * .44f)
            lineTo(w * .5f, h * .78f)
            cubicTo(w * .5f, h * .97f, w * .76f, h * .97f, w * .76f, h * .78f)
        }
        drawPath(stem, color, style = Stroke(width = w * .05f, cap = StrokeCap.Round))
        drawLine(color, Offset(w * .5f, h * .12f), Offset(w * .5f, h * .05f), w * .045f, StrokeCap.Round)
    }
}

private fun time(instant: Instant, zone: ZoneId): String = TimeFormat.format(instant.atZone(zone))
private fun temperature(celsius: Double, unit: TemperatureUnit): String = "${unit.convert(celsius).roundToInt()}${unit.label}"

@Preview(showBackground = true, backgroundColor = 0xFFF3F6F3, widthDp = 393, heightDp = 1000)
@Composable
private fun ForecastPreview() {
    val now = Instant.parse("2026-01-01T03:30:00Z")
    MobileTech_UtilityAppTheme(darkTheme = false) {
        UtilityScreen(WeatherUiState(weather = WeatherSnapshot(City.SINGAPORE, ZoneId.of("Asia/Singapore"), now,
            29.0, 32.0, 3, listOf(20, 70, 50).mapIndexed { index, value ->
                ForecastHour(Instant.parse("2026-01-01T04:00:00Z").plusSeconds(index * 3600L), value)
            }, now), now = now), {})
    }
}
