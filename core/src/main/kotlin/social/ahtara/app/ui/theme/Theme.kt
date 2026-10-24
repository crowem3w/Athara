package social.ahtara.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable








private val LightColorScheme = lightColorScheme(
    primary = Teal,
    onPrimary = Background,
    primaryContainer = TealLight,
    onPrimaryContainer = TealDark,
    secondary = TextSecondary,
    onSecondary = Background,
    background = Background,
    onBackground = TextPrimary,
    surface = Background,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = TextSecondary,
    outline = Divider,
    error = Badge,
)

@Composable
fun Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content,
    )
}