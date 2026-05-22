package ai.kraftshala.attendance.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val KsColorScheme = lightColorScheme(
    primary = AccentPrimary,
    onPrimary = SurfaceWhite,
    secondary = CtaDark,
    onSecondary = SurfaceWhite,
    background = BgForm,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = InputFill,
    onSurfaceVariant = TextMuted,
    error = ErrorRed,
    onError = SurfaceWhite,
    outline = DividerColor
)

@Composable
fun KraftshalaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KsColorScheme,
        typography = KsTypography,
        content = content
    )
}
