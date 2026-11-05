package soc.athara.app.shared.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AtharaColorScheme = lightColorScheme(
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

/**
 * Brand theme for the social/travel surfaces (home feed). Account-colour theming for
 * sign-up and setup screens stays in [soc.athara.app.shared.designsystem.AppTheme].
 */
@Composable
fun AtharaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AtharaColorScheme, typography = AtharaTypography, content = content)
}
