package au.edu.jcu.assessment.utilityapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF12645D), onPrimary = Color.White,
    primaryContainer = Color(0xFFD7EEE7), onPrimaryContainer = Color(0xFF103D38),
    secondary = Color(0xFF546A63), secondaryContainer = Color(0xFFD7EEE7),
    onSecondaryContainer = Color(0xFF103D38), background = Color(0xFFF3F6F3),
    surface = Color(0xFFFCFDFB), surfaceVariant = Color(0xFFE5EBE6),
    onSurface = Color(0xFF172E2C), onSurfaceVariant = Color(0xFF536761), outline = Color(0xFF778981)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9BD4C7), onPrimary = Color(0xFF06372F),
    primaryContainer = Color(0xFF244E43), onPrimaryContainer = Color(0xFFD7EEE7),
    secondaryContainer = Color(0xFF244E43), onSecondaryContainer = Color(0xFFD7EEE7),
    background = Color(0xFF101D1B), surface = Color(0xFF172724),
    surfaceVariant = Color(0xFF2A3C36), onSurface = Color(0xFFE4EEE8), onSurfaceVariant = Color(0xFFB5C9C0)
)

@Composable
fun MobileTech_UtilityAppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography, content = content)
}
